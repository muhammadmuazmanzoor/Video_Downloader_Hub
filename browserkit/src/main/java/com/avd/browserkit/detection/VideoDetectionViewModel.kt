package com.avd.browserkit.detection

import android.webkit.CookieManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avd.browserkit.BrowserKitInitializer
import com.avd.browserkit.api.BrowserKit
import com.avd.browserkit.util.BrowserDetectionPrefs
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.CookieUtils
import com.avd.browserkit.util.DailymotionUrlUtils
import com.avd.browserkit.util.FacebookUrlUtils
import com.avd.browserkit.util.InstagramUrlUtils
import com.avd.browserkit.util.VideoUtils
import com.avd.browserkit.ytdlp.YtDlpExtractor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Xilli DetectedVideosTabViewModel (regular path) + page yt-dlp.
 * Progressive videos must pass [RegularMp4Checker] (video + >5MB).
 */
class VideoDetectionViewModel : ViewModel() {
    private val _buttonState = MutableLiveData<DownloadButtonState>(DownloadButtonState.CannotDownload)
    val buttonState: LiveData<DownloadButtonState> = _buttonState

    private var interceptJob: Job? = null
    private var ytdlpJob: Job? = null
    private var lastVerifiedUrl: String = ""
    private var pageTitle: String = ""
    private var userAgent: String = ""
    private var currentPageUrl: String = ""
    private var currentDetectedInfo: DetectedVideoInfo? = null

    /** Xilli verified regular videos (url → result), largest wins for FB green btn. */
    private val verifiedRegular = ConcurrentHashMap<String, RegularMp4Checker.Result>()
    private val checkSemaphore = Semaphore(2)

    fun setUserAgent(ua: String) {
        userAgent = ua
    }

    fun onPageStarted(pageUrl: String) {
        currentPageUrl = pageUrl
        lastVerifiedUrl = ""
        pageTitle = ""
        currentDetectedInfo = null
        interceptJob?.cancel()
        ytdlpJob?.cancel()
        verifiedRegular.clear()
        _buttonState.value = DownloadButtonState.CannotDownload
        BrowserKitLog.i("Detect", "pageStarted page=${BrowserKitLog.shortUrl(pageUrl)}")
    }

    fun onPageTitle(title: String?) {
        if (!title.isNullOrBlank()) pageTitle = title
    }

    /** Best Xilli checkRegularMp4 hit (largest Content-Length). */
    fun bestVerifiedRegular(): RegularMp4Checker.Result? {
        return verifiedRegular.values.maxByOrNull { it.contentLength }
    }

    fun currentDownloadInfo(): DetectedVideoInfo? {
        return currentDetectedInfo ?: (_buttonState.value as? DownloadButtonState.CanDownload)?.info
    }

    private fun publishDetectedInfo(info: DetectedVideoInfo, verifiedUrl: String) {
        currentDetectedInfo = info
        lastVerifiedUrl = verifiedUrl
        interceptJob?.cancel()
        ytdlpJob?.cancel()
        _buttonState.value = DownloadButtonState.CanDownload(info)
    }

    fun onMediaCandidate(requestUrl: String, pageUrl: String) {
        if (!requestUrl.startsWith("http")) return

        val igMedia = InstagramUrlUtils.isInstagramMediaUrl(requestUrl)
        val streamTypeHint = VideoUtils.streamTypeFromUrl(requestUrl)
        val looksMedia = VideoUtils.looksLikeMediaUrl(requestUrl)
        val tikTokMedia = VideoUtils.isTikTokMediaUrl(requestUrl)
        val fbCdn = requestUrl.contains("fbcdn", true) || requestUrl.contains("fbsbx", true)
        val dmCdn = DailymotionUrlUtils.isDailymotionMediaUrl(requestUrl)

        // HLS/DASH → AvdManifestDetector (must run before RegularMp4 deny-list)
        if (streamTypeHint == StreamType.HLS_M3U8 || streamTypeHint == StreamType.DASH_MPD) {
            handleManifestCandidate(requestUrl, pageUrl)
            return
        }

        if (RegularMp4Checker.shouldSkipUrl(requestUrl)) return

        // Xilli checkRegularMp4: progressive / extensionless CDN (FB, IG, DM, …)
        if (!igMedia && !tikTokMedia && !looksMedia && !fbCdn && !dmCdn && streamTypeHint == null) return

        val activePage = pageUrl.ifBlank { currentPageUrl }
        BrowserKitLog.i(
            "Detect",
            "candidate regularCheck ig=$igMedia fbCdn=$fbCdn dmCdn=$dmCdn " +
                "url=${BrowserKitLog.shortUrl(requestUrl)}",
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (!checkSemaphore.tryAcquire()) return@launch
            try {
                val headers = CookieUtils.headersFromWebView(
                    cookieUrlFor(activePage, requestUrl),
                    userAgent,
                    refererFor(activePage, requestUrl),
                )
                val forceStream = BrowserKit.getAppContext()?.let {
                    BrowserDetectionPrefs.isForceStreamDetection(it)
                } == true
                val result = RegularMp4Checker.check(
                    requestUrl,
                    headers,
                    forceStream = forceStream,
                )
                if (result == null) {
                    BrowserKitLog.d(
                        "Detect",
                        "RegularMp4 reject (need video+>${RegularMp4Checker.THRESHOLD_BYTES}) " +
                            BrowserKitLog.shortUrl(requestUrl),
                    )
                    return@launch
                }
                verifiedRegular[result.finalUrl] = result
                BrowserKitLog.i(
                    "Detect",
                    "RegularMp4 OK len=${result.contentLength} " +
                        "url=${BrowserKitLog.shortUrl(result.finalUrl)}",
                )
                val detected = buildRegularDetected(result, activePage)
                withContext(Dispatchers.Main) {
                    publishDetectedInfo(detected, result.finalUrl)
                }
            } finally {
                checkSemaphore.release()
            }
        }
    }

    private fun handleManifestCandidate(requestUrl: String, pageUrl: String) {
        if (requestUrl == lastVerifiedUrl) return
        val activePage = pageUrl.ifBlank { currentPageUrl }
        interceptJob?.cancel()
        interceptJob = viewModelScope.launch {
            if (currentDownloadInfo() != null) return@launch
            if (_buttonState.value !is DownloadButtonState.CanDownload) {
                _buttonState.value = DownloadButtonState.Loading
            }
            delay(200)
            if (currentDownloadInfo() != null) return@launch
            val headers = CookieUtils.headersFromWebView(
                cookieUrlFor(activePage, requestUrl),
                userAgent,
                refererFor(activePage, requestUrl),
            )
            val streamType = VideoUtils.streamTypeFromUrl(requestUrl) ?: return@launch
            val title = pageTitle.ifBlank { pageTitleOrUrl(activePage) }
            val legacy = BrowserKit.getAppContext()?.let {
                BrowserDetectionPrefs.isUseLegacyM3u8Detection(it)
            } == true

            val detected = if (!legacy) {
                withContext(Dispatchers.IO) {
                    AvdManifestDetector.detect(
                        manifestUrl = requestUrl,
                        headers = headers,
                        pageUrl = activePage.ifBlank { requestUrl },
                        titleHint = title,
                        isM3u8 = streamType == StreamType.HLS_M3U8,
                        isMpd = streamType == StreamType.DASH_MPD,
                    )
                }
            } else {
                null
            } ?: DetectedVideoInfo(
                pageUrl = activePage.ifBlank { requestUrl },
                title = title,
                formats = listOf(
                    StreamFormat(
                        url = requestUrl,
                        label = if (streamType == StreamType.HLS_M3U8) "HLS" else "DASH",
                        ext = if (streamType == StreamType.HLS_M3U8) "m3u8" else "mpd",
                        streamType = streamType,
                        headers = headers,
                        manifestUrl = requestUrl,
                    ),
                ),
                headers = headers,
                isRegularDownload = false,
                isDetectedByAvd = false,
            )
            lastVerifiedUrl = requestUrl
            BrowserKitLog.i(
                "Detect",
                "manifest Avd=${detected.isDetectedByAvd} formats=${detected.formats.size} " +
                    "type=$streamType url=${BrowserKitLog.shortUrl(requestUrl)}",
            )
            publishDetectedInfo(detected, requestUrl)
        }
    }

    fun verifyPageWithYtDlp(pageUrl: String) {
        if (!pageUrl.startsWith("http")) return
        val host = runCatching { android.net.Uri.parse(pageUrl).host.orEmpty().lowercase() }
            .getOrDefault("")
        if (host.contains("google.") || host == "bing.com" || host.endsWith(".bing.com") ||
            host.contains("duckduckgo.") || host.contains("yahoo.")
        ) {
            BrowserKitLog.d("YtDlp", "skip page extract: search host=$host")
            return
        }
        // FB: skip page extract — green btn + RegularMp4 only (Xilli)
        if (FacebookUrlUtils.isFacebookUrl(pageUrl)) {
            BrowserKitLog.d("YtDlp", "skip page extract on Facebook (use RegularMp4 / green btn)")
            return
        }
        // DM home / locale / channel pages are not extractable (yt-dlp: Unsupported URL / Invalid channel).
        if (DailymotionUrlUtils.isDailymotionUrl(pageUrl) &&
            !DailymotionUrlUtils.isDailymotionVideoPage(pageUrl)
        ) {
            BrowserKitLog.d(
                "YtDlp",
                "skip page extract: DM non-video ${BrowserKitLog.shortUrl(pageUrl)}",
            )
            return
        }
        ytdlpJob?.cancel()
        ytdlpJob = viewModelScope.launch {
            if (currentDownloadInfo() != null) return@launch
            _buttonState.value = DownloadButtonState.Loading

            // Await DFM / engine — cold browser open often races module install.
            val ctx = BrowserKit.getAppContext()
            if (ctx != null && !BrowserKitInitializer.isInitialized()) {
                BrowserKitLog.i("YtDlp", "awaiting YoutubeDlEngine init…")
                var attempt = 0
                while (isActive && !BrowserKitInitializer.isInitialized() && attempt < 10) {
                    runCatching { BrowserKitInitializer.initializeAwait(ctx) }
                    if (BrowserKitInitializer.isInitialized()) break
                    attempt++
                    delay(1500L)
                }
            }
            if (!BrowserKitInitializer.isInitialized()) {
                BrowserKitLog.w("YtDlp", "extract abort: engine still not ready")
                if (_buttonState.value !is DownloadButtonState.CanDownload) {
                    _buttonState.value = DownloadButtonState.CannotDownload
                }
                return@launch
            }

            // Retry extract — DM / CDN can flake on first getInfo.
            val maxExtractAttempts = if (DailymotionUrlUtils.isDailymotionUrl(pageUrl)) 3 else 2
            var info: DetectedVideoInfo? = null
            for (attempt in 0 until maxExtractAttempts) {
                if (!isActive) return@launch
                if (currentDownloadInfo() != null) return@launch
                val cookies = CookieManager.getInstance().getCookie(pageUrl)
                BrowserKitLog.i(
                    "YtDlp",
                    "extract attempt=${attempt + 1}/$maxExtractAttempts " +
                        "url=${BrowserKitLog.shortUrl(pageUrl)}",
                )
                info = YtDlpExtractor.extract(pageUrl, cookies, userAgent)
                if (info != null && info.formats.isNotEmpty()) break
                if (attempt < maxExtractAttempts - 1) delay(2000L)
            }

            val extracted = info
            if (extracted != null && extracted.formats.isNotEmpty()) {
                val withHeaders = when {
                    InstagramUrlUtils.isInstagramUrl(pageUrl) -> {
                        val igHeaders = CookieUtils.headersFromWebView(
                            pageUrl, userAgent, InstagramUrlUtils.REFERER,
                        )
                        extracted.copy(
                            title = pageTitle.ifBlank { extracted.title },
                            headers = extracted.headers + igHeaders,
                            formats = extracted.formats.map { f ->
                                f.copy(headers = f.headers + igHeaders)
                            },
                            isRegularDownload = false,
                        )
                    }
                    DailymotionUrlUtils.isDailymotionUrl(pageUrl) -> {
                        val dmHeaders = CookieUtils.headersFromWebView(
                            pageUrl, userAgent, DailymotionUrlUtils.REFERER,
                        )
                        extracted.copy(
                            title = pageTitle.ifBlank { extracted.title },
                            headers = extracted.headers + dmHeaders,
                            formats = extracted.formats.map { f ->
                                f.copy(headers = f.headers + dmHeaders)
                            },
                            isRegularDownload = false,
                        )
                    }
                    else -> extracted.copy(
                        title = pageTitle.ifBlank { extracted.title },
                        isRegularDownload = false,
                    )
                }
                publishDetectedInfo(withHeaders, pageUrl)
            } else if (currentDownloadInfo() == null && _buttonState.value !is DownloadButtonState.CanDownload) {
                _buttonState.value = DownloadButtonState.CannotDownload
            }
        }
    }

    private fun buildRegularDetected(
        result: RegularMp4Checker.Result,
        pageUrl: String,
    ): DetectedVideoInfo {
        val headers = result.headers.ifEmpty {
            CookieUtils.headersFromWebView(
                cookieUrlFor(pageUrl, result.finalUrl),
                userAgent,
                refererFor(pageUrl, result.finalUrl),
            )
        }
        // Xilli setVideoInfoWrapperFromUrl: isRegularDownload = true
        return DetectedVideoInfo(
            pageUrl = pageUrl.ifBlank { result.finalUrl },
            title = pageTitle.ifBlank { pageTitleOrUrl(pageUrl) },
            formats = listOf(
                StreamFormat(
                    url = result.finalUrl,
                    label = "Direct",
                    ext = "mp4",
                    streamType = StreamType.PROGRESSIVE_MP4,
                    headers = headers,
                ),
            ),
            headers = headers,
            isRegularDownload = true,
        )
    }

    private fun refererFor(pageUrl: String, mediaUrl: String = ""): String {
        return when {
            InstagramUrlUtils.isInstagramUrl(pageUrl) ||
                InstagramUrlUtils.isInstagramMediaUrl(mediaUrl) -> InstagramUrlUtils.REFERER
            VideoUtils.isTikTokMediaUrl(mediaUrl) ||
                pageUrl.contains("tiktok.com", true) -> "https://www.tiktok.com/"
            FacebookUrlUtils.isFacebookUrl(pageUrl) ||
                mediaUrl.contains("fbcdn", true) -> "https://www.facebook.com/"
            DailymotionUrlUtils.isDailymotionUrl(pageUrl) ||
                DailymotionUrlUtils.isDailymotionMediaUrl(mediaUrl) -> DailymotionUrlUtils.REFERER
            else -> pageUrl
        }
    }

    private fun cookieUrlFor(pageUrl: String, mediaUrl: String): String {
        return when {
            pageUrl.startsWith("http") -> pageUrl
            VideoUtils.isTikTokMediaUrl(mediaUrl) -> "https://www.tiktok.com/"
            InstagramUrlUtils.isInstagramMediaUrl(mediaUrl) -> InstagramUrlUtils.REFERER
            else -> mediaUrl
        }
    }

    private fun pageTitleOrUrl(pageUrl: String): String {
        return runCatching {
            android.net.Uri.parse(pageUrl).host?.replace("www.", "").orEmpty()
        }.getOrDefault(pageUrl).ifBlank { pageUrl }
    }
}
