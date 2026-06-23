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

                    val file = list.firstOrNull { permission ->
                        val uriString = permission.uri.toString()
                        if (isBusiness) {
                            uriString.contains("com.whatsapp.w4b")
                        } else {
                            uriString.contains("com.whatsapp") && !uriString.contains("com.whatsapp.w4b")
                        }
                    }?.let {
                        DocumentFile.fromTreeUri(context, it.uri)
                    }

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
