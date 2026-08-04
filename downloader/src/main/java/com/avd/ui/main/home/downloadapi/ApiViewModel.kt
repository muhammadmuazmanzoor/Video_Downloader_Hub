package com.avd.ui.main.home.downloadapi

import android.util.Log
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avd.data.remote.sealed.ApiState
import com.avd.data.remote.service.ApiService
import com.avd.data.remote.service.ApiService2
import com.avd.data.remote.service.SocialDownloaderService
import com.avd.util.AdBlockerHelper.isDownloading
import com.avd.util.ContextUtils
import com.avd.util.RemoteConfigHelper
import com.avd.util.SocialDownloaderIdentity
import com.avd.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ApiViewModel @Inject constructor(
    private val apiService: ApiService,
    private val apiService2: ApiService2,
    private val socialDownloaderService: SocialDownloaderService
) : ViewModel() {

    private val _socialDownloadState = MutableStateFlow<ApiState<SocialDownloaderResponse>>(ApiState.Idle)
    val socialDownloadState: StateFlow<ApiState<SocialDownloaderResponse>> get() = _socialDownloadState

    val texturl = SingleLiveEvent<String>()
    private var activeRequestToken: String? = null

    fun socialDownloader(url: String) {
        val logTag = "SocialDownloaderAPI"
        val normalizedUrl = normalizeSocialDownloadUrl(url)
        Log.d(logTag, "URL: $url normalizedUrl: $normalizedUrl")
        if (normalizedUrl == null) {
            _socialDownloadState.value = ApiState.Error("Invalid URL format: ${url.trim().take(120)}")
            return
        }
        isDownloading=true
        val requestToken = UUID.randomUUID().toString()
        activeRequestToken = requestToken
        viewModelScope.launch {
            try {
                delay(250)
                _socialDownloadState.value = ApiState.Loading

                val endpoint = RemoteConfigHelper.getSocialDownloaderEndpoint()
                val cacheBust = System.currentTimeMillis()
                val appContext = ContextUtils.getApplicationContext()
                val installationId = appContext?.let { SocialDownloaderIdentity.getInstallationId(it) }
                    ?: UUID.randomUUID().toString()
                Log.d(logTag, "Endpoint: $endpoint requestToken=$requestToken install=${installationId.take(8)}")
                val response = socialDownloaderService.downloadVideo(
                    endpoint,
                    normalizedUrl,
                    cacheBust,
                    requestToken,
                    installationId
                )
                if (activeRequestToken != requestToken) {
                    Log.w(logTag, "Ignoring stale response for requestToken=$requestToken")
                    return@launch
                }
                Log.d(logTag, "Response received: ${response.isSuccessful}")
                Log.d(logTag, "Response code: ${response.code()}")
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
                    val errorBody = response.errorBody()?.string().orEmpty()
                    Log.e(logTag, "API call failed: ${response.code()} - ${response.message()}")
                    Log.e(logTag, "Error body: ${errorBody.ifBlank { "<empty>" }}")
                    _socialDownloadState.value = ApiState.Error(
                        buildApiErrorMessage(response.code(), response.message(), errorBody)
                    )
                }
            } catch (e: Exception) {
                if (activeRequestToken != requestToken) {
                    Log.w(logTag, "Ignoring stale exception for requestToken=$requestToken")
                    return@launch
                }
                Log.e(logTag, "Exception during API call: ${e.localizedMessage}", e)
                _socialDownloadState.value = ApiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun clearDownloadState() {
        activeRequestToken = null
        _socialDownloadState.value = ApiState.Idle
    }

    companion object {
        private val urlPattern = Regex("""(?i)\bhttps?://[^\s<>"']+""")
        private val repeatedSchemePattern = Regex("""(?i)^(https?://)(?:https?://)+""")

        fun normalizeSocialDownloadUrl(rawUrl: String?): String? {
            val trimmed = rawUrl
                ?.trim()
                ?.trim('\u200B', '\u200C', '\u200D', '\uFEFF')
                .orEmpty()
            if (trimmed.isBlank()) return null

            val extracted = urlPattern.find(trimmed)?.value ?: trimmed
            val cleaned = extracted
                .trim()
                .trimEnd('.', ',', ';', ')', ']', '}')
                .replace(repeatedSchemePattern, "\$1")
            val candidate = if (cleaned.startsWith("http://", true) || cleaned.startsWith("https://", true)) {
                cleaned
            } else {
                "https://$cleaned"
            }

            return try {
                val uri = Uri.parse(candidate)
                if ((uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()) {
                    candidate
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun buildApiErrorMessage(code: Int, message: String, errorBody: String): String {
            val bodyMessage = errorBody.trim().take(180)
            return when {
                bodyMessage.isNotBlank() -> "Download API error $code: $bodyMessage"
                message.isNotBlank() -> "Download API error $code: $message"
                else -> "Download API error $code"
            }
        }
    }

}
