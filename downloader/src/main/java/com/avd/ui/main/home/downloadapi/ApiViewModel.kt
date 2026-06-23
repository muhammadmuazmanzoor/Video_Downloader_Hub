package com.avd.ui.main.home.downloadapi

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avd.data.remote.sealed.ApiState
import com.avd.data.remote.service.ApiService
import com.avd.data.remote.service.ApiService2
import com.avd.data.remote.service.SocialDownloaderService
import com.avd.util.AdBlockerHelper.isDownloading
import com.avd.util.RemoteConfigHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiViewModel @Inject constructor(
    private val apiService: ApiService,
    private val apiService2: ApiService2,
    private val socialDownloaderService: SocialDownloaderService
) : ViewModel() {

    private val _socialDownloadState = MutableStateFlow<ApiState<SocialDownloaderResponse>>(ApiState.Idle)
    val socialDownloadState: StateFlow<ApiState<SocialDownloaderResponse>> get() = _socialDownloadState


    val texturl = MutableLiveData<String>()

    fun socialDownloader(url: String) {
        val logTag = "SocialDownloaderAPI"
        Log.d(logTag, "URL: $url")
        isDownloading=true
        viewModelScope.launch {
            try {
                delay(250)
                _socialDownloadState.value = ApiState.Loading
                texturl.value = url

//                Log.d("checkBaseUrl","end point: ${RemoteConfigHelper.getSocialDownloaderEndpoint()}")
                val response = socialDownloaderService.downloadVideo(RemoteConfigHelper.getSocialDownloaderEndpoint(),url)
                Log.d(logTag, "Response received: ${response.isSuccessful}")
                Log.d(logTag, "Response message: ${response.message()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(logTag, "Platform: ${body.platform}, API Used: ${body}")

                        // ✅ Smart filter and sorting
                        val filteredVideos = body.videos
                            .filter { !it.url.isNullOrEmpty() }
                            .sortedByDescending { video -> video.format }
                            .take(3)

                        val filteredBody = body.copy(videos = filteredVideos)
                        Log.d(logTag, "Filtered videos count: ${filteredVideos.size}")

                        if (filteredVideos.isEmpty()) {
                            Log.e(logTag, "Empty video list after filtering.")
                            _socialDownloadState.value = ApiState.Error("No videos found.")
                        } else {
                            _socialDownloadState.value = ApiState.Success(filteredBody)
                            Log.d(logTag, "Success: Found ${filteredVideos.size} videos.")
                        }
                    } else {
                        Log.e(logTag, "Response body is null.")
                        _socialDownloadState.value = ApiState.Error("Empty response body.")
                    }
                } else {
                    Log.e(logTag, "API call failed: ${response.code()} - ${response.message()}")
                    _socialDownloadState.value = ApiState.Error(response.message())
                }
            } catch (e: Exception) {
                Log.e(logTag, "Exception during API call: ${e.localizedMessage}")
                _socialDownloadState.value = ApiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun clearDownloadState() {
        _socialDownloadState.value = ApiState.Idle
    }

}