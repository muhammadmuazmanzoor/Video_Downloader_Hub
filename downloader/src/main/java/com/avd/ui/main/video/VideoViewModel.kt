package com.avd.ui.main.video

//import com.allVideoDownloaderXmaster.OpenForTesting
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.databinding.ObservableField
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
    }

    var localVideos: ObservableField<MutableList<LocalVideo>> = ObservableField(mutableListOf())
    val isLoadingVideos = ObservableField(true)

    val renameErrorEvent = SingleLiveEvent<Int>()
    val shareEvent = SingleLiveEvent<Uri>()

    // Cache to avoid repeated file system operations and prevent deadlocks
    var cachedFilesList: List<LocalVideo> = emptyList()
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
                            localVideos.set(cachedList.toMutableList())
                            if (cachedList.isNotEmpty()) {
                                isLoadingVideos.set(false)
                            }
                        }
                    }

                    // 2. Decide delay
                    delay(5000L)

                    // 3. Fetch fresh data only when cache expired or no cache
                    val shouldFetchFresh =
                        cachedFilesList == null ||
                                (currentTime - lastCacheTime) >= CACHE_DURATION_MS

                    if (!shouldFetchFresh) {
                        continue
                    }

                    val freshList = getFilesList()
                        .sortedByDescending { it.time }

                    val oldList = cachedFilesList.orEmpty()

                    // 4. Only refresh if data changed
                    if (!areVideoListsSame(oldList, freshList)) {
                        cachedFilesList = freshList.reversed()
                        cachedVideosList?.set(freshList.reversed().toMutableList())
                        localVideos.set(freshList.reversed().toMutableList())
                        lastCacheTime = System.currentTimeMillis()
                    } else {
                        // No new data, only update cache time
                        lastCacheTime = System.currentTimeMillis()
                    }
                    isLoadingVideos.set(false)

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

    override fun stop() {
        refreshJob?.cancel()
        refreshJob = null
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
        val currentList = localVideos.get().orEmpty()
        val updatedList = currentList.filterNot { it.uri.toString() == video.uri.toString() }

        localVideos.set(updatedList.toMutableList())
        cachedFilesList = updatedList
        cachedVideosList?.set(updatedList.toMutableList())
        lastCacheTime = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            val deleted = fileUtil.deleteMedia(context.applicationContext, video.uri)

            if (!deleted || fileUtil.isUriExists(context.applicationContext, video.uri)) {
                val freshList = getFilesList().sortedByDescending { it.time }.reversed()
                withContext(Dispatchers.Main.immediate) {
                    cachedFilesList = freshList
                    cachedVideosList?.set(freshList.toMutableList())
                    localVideos.set(freshList.toMutableList())
                    lastCacheTime = System.currentTimeMillis()
                }
            }
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
