package com.avd.ui.main.video

//import com.allVideoDownloaderXmaster.OpenForTesting
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.avd.data.local.model.LocalVideo
import com.avd.ui.main.base.BaseViewModel
import com.avd.util.AdBlockerHelper.cachedVideosList
import com.avd.util.AdBlockerHelper.fromVideo
import com.avd.util.ContextUtils
import com.avd.util.FileUtil
import com.avd.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val fileUtil: FileUtil,
) : BaseViewModel() {

    companion object {
        const val FILE_EXIST_ERROR_CODE = 1
        const val FILE_INVALID_ERROR_CODE = 2
        private const val COMPLETION_REFRESH_ATTEMPTS = 10
        private const val COMPLETION_REFRESH_RETRY_DELAY_MS = 500L
    }

    var localVideos: ObservableField<MutableList<LocalVideo>> = ObservableField(mutableListOf())
    val isLoadingVideos = ObservableField(true)

    val renameErrorEvent = SingleLiveEvent<Int>()
    val shareEvent = SingleLiveEvent<Uri>()

    private val _permissionNeededForDeleteVideo = MutableLiveData<IntentSender?>()
    val permissionNeededForDeleteVideo: LiveData<IntentSender?> = _permissionNeededForDeleteVideo

    private var pendingDeleteVideo: LocalVideo? = null

    // Cache to avoid repeated file system operations and prevent deadlocks
    var cachedFilesList: List<LocalVideo> = emptyList()
    var showLatestDownloadsFirst: Boolean = false
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION_MS = 5000L // Cache for 5 seconds instead of polling every second

   /* override fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val newList = getFilesList().sortedByDescending { it.time }
                    localVideos.set(newList.toMutableList())
                    if(fromVideo) {
                        // Use longer delay to reduce binder IPC calls and prevent deadlocks
                        delay(5000) // Changed from 1000ms to 5000ms to reduce file system operations
                    }
                    else{
                        delay(500)
                    }
                    // Check if cache is still valid
                    val currentTime = System.currentTimeMillis()
                    val sortedList = if (cachedFilesList != null && (currentTime - lastCacheTime) < CACHE_DURATION_MS) {
                        // Use cached result
                        cachedFilesList!!
                    } else {
                        // Refresh cache
                        val newList = getFilesList().sortedByDescending { it.time }
                        cachedFilesList = newList
                        cachedVideosList?.set(newList.toMutableList())
                        lastCacheTime = currentTime
                        newList
                    }
                    
                    val rev = sortedList.reversed()
                    localVideos.set(rev.toMutableList())
                } catch (e: Exception) {
                    // Log error but continue the loop to prevent crashes
                    Log.e("VideoViewModel", "Error refreshing file list", e)
                    delay(5000) // Wait before retrying
                }
            }
        }
    }*/
    private var refreshJob: Job? = null
    private var completionRefreshJob: Job? = null

    override fun start() {
        if (refreshJob?.isActive == true) return
        isLoadingVideos.set(true)

        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val currentTime = System.currentTimeMillis()

                    // 1. Show cached data first if available
                    cachedFilesList?.let { cachedList ->
                        if (localVideos.get().isNullOrEmpty()) {
                            localVideos.set(orderForCurrentView(cachedList).toMutableList())
                            if (cachedList.isNotEmpty()) {
                                isLoadingVideos.set(false)
                            }
                        }
                    }

                    // 2. Fetch fresh data only when cache expired or no cache
                    val shouldFetchFresh =
                        cachedFilesList.isEmpty() ||
                                (currentTime - lastCacheTime) >= CACHE_DURATION_MS

                    if (!shouldFetchFresh) {
                        delay(5000L)
                        continue
                    }

                    val freshList = orderForCache(getFilesList())
                    val displayList = orderForCurrentView(freshList)

                    val oldList = cachedFilesList.orEmpty()

                    // 4. Only refresh if data changed
                    if (!areVideoListsSame(oldList, freshList)) {
                        cachedFilesList = freshList
                        cachedVideosList?.set(freshList.toMutableList())
                        localVideos.set(displayList.toMutableList())
                        lastCacheTime = System.currentTimeMillis()
                    } else {
                        // No new data, only update cache time
                        lastCacheTime = System.currentTimeMillis()
                    }
                    isLoadingVideos.set(false)
                    delay(5000L)

                } catch (e: Exception) {
                    Log.e("VideoViewModel", "Error refreshing file list", e)
                    isLoadingVideos.set(false)
                    delay(5000L)
                }
            }
        }
    }
    private fun areVideoListsSame(
        oldList: List<LocalVideo>,
        newList: List<LocalVideo>
    ): Boolean {
        if (oldList.size != newList.size) return false

        return oldList.zip(newList).all { (old, new) ->
            old.uri == new.uri &&
                    old.time == new.time &&
                    old.size == new.size
        }
    }

    private fun orderForCache(videos: List<LocalVideo>): List<LocalVideo> {
        // Keep cached order consistent and make intent explicit: newest first.
        // Previously this used `sortedByDescending { it.time }.reversed()` which
        // effectively produced oldest-first and was confusing. Return newest-first
        // directly so callers that expect recent items at the top get a correct
        // ordering.
        return videos.sortedByDescending { it.time }
    }

    private fun orderForCurrentView(videos: List<LocalVideo>): List<LocalVideo> {
        return if (showLatestDownloadsFirst) {
            videos.sortedByDescending { it.time }
        } else {
            orderForCache(videos)
        }
    }

    override fun stop() {
        refreshJob?.cancel()
        refreshJob = null
    }

    /**
     * Bypass both VideoViewModel and FileUtil caches when a download finishes.
     * MediaStore publication can trail the SUCCESS state slightly, so retry briefly
     * before notifying the queue screen that the Completed tab is ready.
     */
    fun refreshCompletedDownloads(onRefreshed: () -> Unit = {}) {
        // SUCCESS may be persisted before MediaStore exposes the finished file. A
        // completion broadcast can also arrive while a progress-triggered refresh is
        // retrying, so cancel the older refresh to prevent stale results winning.
        completionRefreshJob?.cancel()
        completionRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            val previousUris = localVideos.get().orEmpty().map { it.uri }.toSet()
            var freshList: List<LocalVideo> = emptyList()

            for (attempt in 0 until COMPLETION_REFRESH_ATTEMPTS) {
                fileUtil.invalidateListFilesCache()
                freshList = orderForCache(getFilesList())
                val containsNewVideo = freshList.any { it.uri !in previousUris }
                if (containsNewVideo || attempt == COMPLETION_REFRESH_ATTEMPTS - 1) break
                delay(COMPLETION_REFRESH_RETRY_DELAY_MS)
            }

            withContext(Dispatchers.Main.immediate) {
                cachedFilesList = freshList
                cachedVideosList?.set(freshList.toMutableList())
                localVideos.set(orderForCurrentView(freshList).toMutableList())
                lastCacheTime = System.currentTimeMillis()
                isLoadingVideos.set(false)
                onRefreshed()
            }
        }
    }


    private fun getFilesListOld(): List<LocalVideo> {
        val listVideos: MutableList<LocalVideo> = mutableListOf()
        fileUtil.listFiles.forEach { entry ->
            val fileUri = entry.value.second
            val fileSize = fileUtil.getContentLength(ContextUtils.getApplicationContext(), fileUri)
            val readableSize = FileUtil.getFileSizeReadable(fileSize.toDouble())
            val video = LocalVideo(
                entry.value.first,
                fileUri,
                entry.key
            )
            video.size = readableSize
            listVideos.add(video)
        }

        return listVideos.toList()
    }
    private fun getFilesList(): List<LocalVideo> {
        val listVideos: MutableList<LocalVideo> = mutableListOf()
        fileUtil.listFiles.forEach { entry ->
            val fileId = entry.value.first
            val fileUri = entry.value.second
            val timestamp = entry.value.third // Access the timestamp here
            val fileSize = fileUtil.getContentLength(ContextUtils.getApplicationContext(), fileUri)
            val readableSize = FileUtil.getFileSizeReadable(fileSize.toDouble())
            val video = LocalVideo(
                id = fileId,
                uri = fileUri,
                name = entry.key,
                time = timestamp,


            )
            video.size = readableSize
            listVideos.add(video)
        }

        return listVideos.toList()
    }


    fun deleteVideo(context: Context, video: LocalVideo) {
        pendingDeleteVideo = video
        val currentList = localVideos.get().orEmpty()
        val updatedList = currentList.filterNot { it.uri.toString() == video.uri.toString() }

        localVideos.set(updatedList.toMutableList())
        val cacheList = orderForCache(updatedList)
        cachedFilesList = cacheList
        cachedVideosList?.set(cacheList.toMutableList())
        lastCacheTime = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deleted = fileUtil.deleteMediaOrThrow(context.applicationContext, video.uri)

                if (!deleted || fileUtil.isUriExists(context.applicationContext, video.uri)) {
                    refreshVideosAfterDelete()
                }
            } catch (securityException: SecurityException) {
                val intentSender = getDeletePermissionIntentSender(context, video.uri, securityException)
                if (intentSender != null) {
                    withContext(Dispatchers.Main.immediate) {
                        _permissionNeededForDeleteVideo.value = intentSender
                    }
                } else {
                    Log.e("VideoViewModel", "Delete permission denied without recoverable action", securityException)
                    refreshVideosAfterDelete()
                }
            } catch (e: Throwable) {
                Log.e("VideoViewModel", "Unable to delete video", e)
                refreshVideosAfterDelete()
            }
        }
    }

    /**
     * Re-apply current ordering to the visible list immediately.
     * This is used when the ordering preference (showLatestDownloadsFirst)
     * changes after the refresh job has already started so UI updates
     * immediately without restarting the background job.
     */
    fun applyCurrentOrdering() {
        try {
            val displayList = orderForCurrentView(cachedFilesList)
            localVideos.set(displayList.toMutableList())
        } catch (e: Exception) {
            Log.e("VideoViewModel", "Error applying current ordering", e)
        }
    }

    fun onDeletePermissionResult(context: Context, granted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (granted) {
                fileUtil.invalidateListFilesCache()
            }

            refreshVideosAfterDelete()

            withContext(Dispatchers.Main.immediate) {
                pendingDeleteVideo = null
                _permissionNeededForDeleteVideo.value = null
            }
        }
    }

    private fun getDeletePermissionIntentSender(
        context: Context,
        uri: Uri,
        securityException: SecurityException
    ): IntentSender? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                (securityException as? RecoverableSecurityException)
                    ?.userAction
                    ?.actionIntent
                    ?.intentSender
            }
            else -> null
        }
    }

    private suspend fun refreshVideosAfterDelete() {
        val freshList = orderForCache(getFilesList())
        val displayList = orderForCurrentView(freshList)
        withContext(Dispatchers.Main.immediate) {
            cachedFilesList = freshList
            cachedVideosList?.set(freshList.toMutableList())
            localVideos.set(displayList.toMutableList())
            lastCacheTime = System.currentTimeMillis()
        }
    }


    fun renameVideo(context: Context, uri: Uri, newName: String) {
        if (newName.isNotEmpty()) {
            val exists = fileUtil.isUriExists(context, uri)
            if (exists) {
                val isFileWithNameNotExists =
                    fileUtil.isFileWithNameNotExists(context, uri, newName)
                if (isFileWithNameNotExists) {
                    val newMediaNameUri = fileUtil.renameMedia(context, uri, newName)
                    if (newMediaNameUri != null) {
                        localVideos.get()?.find { it.uri.toString() == uri.toString() }?.let {
                            it.uri = newMediaNameUri.second
                            it.name = newMediaNameUri.first

                            localVideos.get().let { list ->
                                list?.set(list.indexOf(it), it)
                            }
                        }
                        return
                    }
                }

                renameErrorEvent.value = FILE_EXIST_ERROR_CODE
                return
            }
        }

        renameErrorEvent.value = FILE_INVALID_ERROR_CODE
    }

    fun findVideoByName(downloadFilename: String?): Observable<LocalVideo> {
        return Observable.create { emitter ->
            val videos = getFilesList()
            val found =
                videos.find { it.name.contains(File(downloadFilename.toString()).name) }
            if (found != null) {
                emitter.onNext(found)
                emitter.onComplete()
            }
        }
    }


    fun getPathFromUri(context: Context, uri: Uri): String? {
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                val path = cursor.getString(columnIndex)
                cursor.close()
                return path
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
