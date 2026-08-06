package com.video.avd.ui.status_saver

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.ui.status_saver.model.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import java.util.Arrays
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor() : ViewModel() {
    private companion object {
        const val TAG = "StatusDebug"
    }
    private val _videoList = MutableLiveData<List<Status>>()
    var videoList: LiveData<List<Status>> = _videoList
    var status: Status? = null
    private var statusLoadJob: Job? = null

    private val _hasData = MutableLiveData<Boolean>()
    var hasData: LiveData<Boolean> = _hasData

    init {
        videoList=_videoList
    }

        fun getStatus(context: Context, isBusiness: Boolean) {
        Log.d(TAG, "getStatus: sdk=${Build.VERSION.SDK_INT}, business=$isBusiness")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isBusiness) {
                executeNew(context,true)
            }
           else {
               executeNew(context, false)
            }
        } else if (isBusiness)
        {
            if (CommonStatusUtils.STATUS_DIRECTORY_BUSINESS.exists()) {
            executeOld(context,isBusiness)
        }
        }
        else if (CommonStatusUtils.STATUS_DIRECTORY.exists()) {
            executeOld(context,isBusiness)
        } else {
            // Handle the case when neither condition is met
        }
    }

    private fun executeOld(context: Context,isBusiness:Boolean) {
        Log.d(TAG, "executeOld: business=$isBusiness")
        statusLoadJob?.cancel()
        statusLoadJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                //    val statusFiles = CommonStatusUtils.STATUS_DIRECTORY.listFiles()
                val statusFiles = if (isBusiness) CommonStatusUtils.STATUS_DIRECTORY_BUSINESS.listFiles() else CommonStatusUtils.STATUS_DIRECTORY.listFiles()
                Log.d(TAG, "executeOld: directory=${if (isBusiness) CommonStatusUtils.STATUS_DIRECTORY_BUSINESS else CommonStatusUtils.STATUS_DIRECTORY}, entries=${statusFiles?.size ?: -1}")
                val newVideoList = mutableListOf<Status>()
                if (statusFiles != null && statusFiles.isNotEmpty()) {
                    Arrays.sort(statusFiles)
                    for (file in statusFiles) {
                        val status = Status(file, file.name, file.absolutePath,isBusiness)
                        if (isSupportedStatus(file.name)) {
                            newVideoList.add(status)
                        }
                    }
                }
                _videoList.postValue(newVideoList)
                Log.d(TAG, "executeOld: posting ${newVideoList.size} videos")
            }
        }
    }

    private fun executeNew(context: Context, isBusiness: Boolean) {
        statusLoadJob?.cancel()
        statusLoadJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = context.contentResolver.persistedUriPermissions
                try {
                    Log.d(TAG, "executeNew: business=$isBusiness, persistedGrants=${list.size}")
                    list.forEachIndexed { index, permission ->
                        Log.d(
                            TAG,
                            "grant[$index]: uri=${permission.uri}, read=${permission.isReadPermission}, write=${permission.isWritePermission}, matches=${uriMatchesApp(permission.uri.toString(), isBusiness)}"
                        )
                    }

                    val matchedPermission = list.asReversed().firstOrNull { permission ->
                        permission.isReadPermission && uriMatchesApp(permission.uri.toString(), isBusiness)
                    }
                    Log.d(TAG, "executeNew: matchedGrant=${matchedPermission?.uri}")

                    val root = matchedPermission?.let {
                        DocumentFile.fromTreeUri(context, it.uri)
                    }
                    Log.d(TAG, "executeNew: treeRoot=${root?.uri}, name=${root?.name}, exists=${root?.exists()}, canRead=${root?.canRead()}")

                    val file = root?.let(::findStatusesDirectory)

                    if (file == null) {
                        Log.e(TAG, "executeNew: .Statuses directory not resolved; business=$isBusiness")
                    } else {
                        Log.d(TAG, "executeNew: resolvedStatuses=${file.uri}, name=${file.name}, canRead=${file.canRead()}")
                    }

                    val newVideoList = mutableListOf<Status>()
                    val statusFiles = runCatching { file?.listFiles().orEmpty() }
                        .onFailure { Log.e("StatusViewModel", "Unable to list .Statuses", it) }
                        .getOrDefault(emptyArray())

                    for (documentFile in statusFiles) {
                        // A directory can contain .nomedia, folders, or provider rows
                        // with no display name. One such row must not abort the whole load.
                        val video = isVideoStatus(documentFile)
                        val supported = video || isImageStatus(documentFile)
                        Log.d(
                            TAG,
                            "entry: name=${documentFile.name}, uri=${documentFile.uri}, file=${documentFile.isFile}, type=${documentFile.type}, length=${documentFile.length()}, video=$video, supported=$supported"
                        )
                        if (!documentFile.isFile || !supported) continue
                        runCatching {
                            Status(documentFile, isBusiness)
                        }.onSuccess(newVideoList::add)
                            .onFailure {
                                Log.w("StatusViewModel", "Skipping unreadable status: ${documentFile.uri}", it)
                            }
                    }
                    Log.d(
                        TAG,
                        "Loaded ${newVideoList.size} statuses from ${file?.uri}; business=$isBusiness"
                    )
                    _videoList.postValue(newVideoList)
                } catch (e: Exception) {
                    Log.e(TAG, "executeNew: failed; business=$isBusiness", e)
                    _videoList.postValue(emptyList())
                }
            }
        }
    }

    fun hasStatusFolderPermission(context: Context, isBusiness: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        val result = context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && uriMatchesApp(permission.uri.toString(), isBusiness)
        }
        Log.d(TAG, "hasStatusFolderPermission: business=$isBusiness, result=$result")
        return result
    }

    private fun uriMatchesApp(uriString: String, isBusiness: Boolean): Boolean {
        val normalized = uriString.lowercase()
        return if (isBusiness) {
            normalized.contains("com.whatsapp.w4b")
        } else {
            normalized.contains("com.whatsapp") && !normalized.contains("com.whatsapp.w4b")
        }
    }

    private fun isVideoStatus(file: DocumentFile): Boolean {
        val name = file.name.orEmpty()
        val mimeType = file.type.orEmpty()
        return name.endsWith(".mp4", ignoreCase = true) ||
            mimeType.startsWith("video/", ignoreCase = true)
    }

    private fun isImageStatus(file: DocumentFile): Boolean {
        val name = file.name.orEmpty()
        val mimeType = file.type.orEmpty()
        return name.endsWith(".jpg", ignoreCase = true) ||
            name.endsWith(".jpeg", ignoreCase = true) ||
            name.endsWith(".png", ignoreCase = true) ||
            name.endsWith(".webp", ignoreCase = true) ||
            mimeType.startsWith("image/", ignoreCase = true)
    }

    private fun isSupportedStatus(name: String): Boolean {
        return name.endsWith(".mp4", ignoreCase = true) ||
            name.endsWith(".jpg", ignoreCase = true) ||
            name.endsWith(".jpeg", ignoreCase = true) ||
            name.endsWith(".png", ignoreCase = true) ||
            name.endsWith(".webp", ignoreCase = true)
    }

    /**
     * The system picker may return .Statuses itself or one of its parents (Media,
     * WhatsApp, or the package directory). Resolve the actual hidden status folder
     * instead of assuming the granted tree already points directly at it.
     */
    private fun findStatusesDirectory(root: DocumentFile): DocumentFile? {
        Log.d(TAG, "findStatusesDirectory: root=${root.uri}, name=${root.name}")
        if (root.name.equals(".Statuses", ignoreCase = true)) {
            Log.d(TAG, "findStatusesDirectory: granted root is .Statuses")
            return root
        }

        var level = listOf(root)
        repeat(5) {
            val next = mutableListOf<DocumentFile>()
            level.forEach { directory ->
                val children = runCatching { directory.listFiles().toList() }
                    .onFailure { Log.e(TAG, "findStatusesDirectory: cannot list ${directory.uri}", it) }
                    .getOrDefault(emptyList())
                Log.d(TAG, "findStatusesDirectory: scanning=${directory.name}, children=${children.map { it.name }}")
                children.firstOrNull {
                    it.isDirectory && it.name.equals(".Statuses", ignoreCase = true)
                }?.let {
                    Log.d(TAG, "findStatusesDirectory: found=${it.uri}")
                    return it
                }
                next += children.filter { it.isDirectory }
            }
            if (next.isEmpty()) return null
            level = next
        }
        return null
    }

    private fun executeNew_(context:Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = context.contentResolver?.persistedUriPermissions
                try {
                    val file = list?.get(0)
                        ?.let { DocumentFile.fromTreeUri(context, it.uri) }
                    val newVideoList = mutableListOf<Status>()
                    val statusFiles = file?.listFiles()
                    if (statusFiles != null) {
                        for (documentFile in statusFiles) {
                            val status = Status(documentFile, false)
                            if (status.isVideo) {
                                newVideoList.add(status)
                            }
                        }
                    }
                    _videoList.postValue(newVideoList)
                } catch (e: Exception) {
                    // Handle the exception
                }
            }
        }
    }
    private fun executeNewBusiness_(context:Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = context.contentResolver?.persistedUriPermissions
                try {
                    val file = list?.get(0)
                        ?.let { DocumentFile.fromTreeUri(context, it.uri) }
                    val newVideoList = mutableListOf<Status>()
                    val statusFiles = file?.listFiles()
                    if (statusFiles != null) {
                        for (documentFile in statusFiles) {
                            val status = Status(documentFile, true)
                            if (status.isVideo) {
                                newVideoList.add(status)
                            }
                        }
                    }
                    _videoList.postValue(newVideoList)
                } catch (e: Exception) {
                    // Handle the exception
                }
            }
        }
    }



    fun updateHasData(hasData:Boolean){
        _hasData.value = hasData
    }
    fun clearData(){
        _videoList.value= emptyList()
    }
}
