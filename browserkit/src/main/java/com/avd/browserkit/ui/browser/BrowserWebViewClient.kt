package com.avd.browserkit.ui.browser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.SafeBrowsingResponseCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.avd.browserkit.util.BrowserKitLog

class BrowserWebViewClient(
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String, String?) -> Unit,
    private val onRequestIntercepted: (String, String) -> Unit,
    private val onNavigate: (String) -> Boolean,
    private val currentPageUrl: () -> String,
    private val onHistoryChanged: (String) -> Unit = {},
) : WebViewClientCompat() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        val pageUrl = url.orEmpty()
        // Do NOT call onNavigate here. Returning true + stopLoading() races with
        // loadUrl() inside onNavigate and aborts Facebook (WebView falls back to Google).
        // Navigation overrides belong only in shouldOverrideUrlLoading.
        if (pageUrl.isNotBlank()) onPageStarted(pageUrl)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        onPageFinished(url.orEmpty(), view?.title)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        // IG/FB Reels SPA often skip onPageFinished — reinject UI on history URL change.
        onHistoryChanged(url ?: view?.url.orEmpty())
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val requestUrl = request?.url?.toString().orEmpty()
        // Xilli: check every non-excluded request; RegularMp4Checker filters by size.
        if (requestUrl.startsWith("http") && shouldProbeAsMedia(requestUrl)) {
            if (com.avd.browserkit.util.BrowserAdvancedFeatures.shouldBlockUrl(requestUrl)) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }
            onRequestIntercepted(requestUrl, currentPageUrl())
        }
        return super.shouldInterceptRequest(view, request)
    }

    /**
     * Xilli deny-list; probe fbcdn/extensionless CDN (accept = video + >5MB later).
     */
    private fun shouldProbeAsMedia(url: String): Boolean {
        val cleared = url.substringBefore('?').trim()
        if (EXCLUDED_EXT.containsMatchIn(cleared)) return false
        if (com.avd.browserkit.util.VideoUtils.streamTypeFromUrl(url) != null) return true
        if (com.avd.browserkit.util.VideoUtils.looksLikeMediaUrl(url)) return true
        if (com.avd.browserkit.util.InstagramUrlUtils.isInstagramMediaUrl(url)) return true
        val lower = cleared.lowercase()
        val dmMedia = (lower.contains("dmcdn") || lower.contains("dailymotion")) &&
            (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains(".mp4") ||
                lower.contains("manifest") || lower.contains("/hls/") ||
                lower.contains("mpegurl") || lower.contains("sec2("))
        return lower.contains("fbcdn") || lower.contains("fbsbx") ||
            lower.contains("cdninstagram") || lower.contains("video.twimg") ||
            lower.contains("tiktok") || lower.contains("/o1/v/") ||
            dmMedia ||
            (lower.contains("video") && lower.contains("cdn"))
    }

    companion object {
        // m3u8/mpd NOT excluded — AvdManifestDetector needs intercept (Super CustomWebViewClient).
        private val EXCLUDED_EXT = Regex(
            """\.(apk|html?|xml|ico|css|js|png|gif|json|jpe?g|svg|woff2?|ts|php|ttf|otf|eot|""" +
                """cur|webp|bmp|tif|tiff|psd|ai|eps|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|md|rtf|""" +
                """vtt|srt|swf|jar|log|txt|m4s)$""",
            RegexOption.IGNORE_CASE,
        )
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString().orEmpty()
        if (url.isBlank()) return false
        return onNavigate(url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val target = url.orEmpty()
        if (target.isBlank()) return false
        return onNavigate(target)
    }

    /**
     * Google Safe Browsing hit — malware / phishing / unwanted software (not adult content).
     * Always go back to safety so unsafe pages do not load.
     */
    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponseCompat,
    ) {
        BrowserKitLog.w(
            "SafeBrowsing",
            "threatType=$threatType url=${BrowserKitLog.shortUrl(request.url?.toString().orEmpty())}",
        )
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY)) {
            callback.backToSafety(/* report */ true)
        } else {
            super.onSafeBrowsingHit(view, request, threatType, callback)
        }
    }
}
