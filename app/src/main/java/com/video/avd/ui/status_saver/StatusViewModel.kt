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
import java.util.Arrays
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor() : ViewModel() {
    private val _videoList = MutableLiveData<List<Status>>()
    var videoList: LiveData<List<Status>> = _videoList
    var status: Status? = null

    private val _hasData = MutableLiveData<Boolean>()
    var hasData: LiveData<Boolean> = _hasData

    init {
        videoList=_videoList
    }

        fun getStatus(context: Context, isBusiness: Boolean) {
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
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                //    val statusFiles = CommonStatusUtils.STATUS_DIRECTORY.listFiles()
                val statusFiles = if (isBusiness) CommonStatusUtils.STATUS_DIRECTORY_BUSINESS.listFiles() else CommonStatusUtils.STATUS_DIRECTORY.listFiles()
                val newVideoList = mutableListOf<Status>()
                if (statusFiles != null && statusFiles.isNotEmpty()) {
                    Arrays.sort(statusFiles)
                    for (file in statusFiles) {
                        val status = Status(file, file.name, file.absolutePath,isBusiness)
                        if (status.isVideo) {
                            newVideoList.add(status)
                        }
                    }
                }
                _videoList.postValue(newVideoList)
            }
        }
    }

    private fun executeNew(context: Context, isBusiness: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = context.contentResolver.persistedUriPermissions
                try {
                    // Debug: Log persisted URIs
                    list.forEach {
                        Log.d("URI Permission ss", "URI: ${it.uri}")
                    }

                    val file = list.asReversed().firstOrNull { permission ->
                        val uriString = permission.uri.toString()
                        permission.isReadPermission && if (isBusiness) {
                            uriString.contains("com.whatsapp.w4b")
                        } else {
                            uriString.contains("com.whatsapp") && !uriString.contains("com.whatsapp.w4b")
                        }
                    }?.let {
                        DocumentFile.fromTreeUri(context, it.uri)
                    }?.let(::findStatusesDirectory)

                    if (file == null) {
                        Log.d("URI Permission", "No matching URI found for isBusiness = $isBusiness")
                    }

                    val newVideoList = mutableListOf<Status>()
                    val statusFiles = file?.listFiles()
                    if (statusFiles != null) {
                        for (documentFile in statusFiles) {
                            val status = Status(documentFile, isBusiness)
                            if (status.isVideo) {
                                newVideoList.add(status)
                            }
                        }
                    }
                    _videoList.postValue(newVideoList)
                } catch (e: Exception) {
                    Log.e("executeNew", "Error fetching status videos", e)
                    // Handle the exception
                }
            }
        }
    }

    /**
     * The system picker may return .Statuses itself or one of its parents (Media,
     * WhatsApp, or the package directory). Resolve the actual hidden status folder
     * instead of assuming the granted tree already points directly at it.
     */
    private fun findStatusesDirectory(root: DocumentFile): DocumentFile? {
        if (root.name.equals(".Statuses", ignoreCase = true)) return root

        var level = listOf(root)
        repeat(5) {
            val next = mutableListOf<DocumentFile>()
            level.forEach { directory ->
                val children = runCatching { directory.listFiles().toList() }
                    .getOrDefault(emptyList())
                children.firstOrNull {
                    it.isDirectory && it.name.equals(".Statuses", ignoreCase = true)
                }?.let { return it }
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
