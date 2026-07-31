package com.avd.browserkit.detection

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.avd.browserkit.util.BrowserKitLog

class VideoDetectorJsBridge(
    private val onMediaUrl: (String) -> Unit,
    private val onVideoClicked: ((String) -> Unit)? = null,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastClickAtMs: Long = 0L
    private var lastClickUrl: String = ""

    @JavascriptInterface
    fun onMediaUrl(url: String?) {
        val cleaned = url?.trim().orEmpty()
        if (cleaned.isBlank() || cleaned.startsWith("blob:")) {
            BrowserKitLog.d("JS", "onMediaUrl ignored blank/blob")
            return
        }
        BrowserKitLog.i("JS", "onMediaUrl ${BrowserKitLog.shortUrl(cleaned)}")
        mainHandler.post { onMediaUrl(cleaned) }
    }

    /** Debug breadcrumb from FaceBookScript click path. */
    @JavascriptInterface
    fun onFbDebug(message: String?) {
        BrowserKitLog.i("JS", "onFbDebug ${message.orEmpty()}")
    }

    @JavascriptInterface
    fun onVideoClicked(url: String?) {
        // Always forward — empty / fbvid:id means Kotlin uses CDN or page fallback.
        val cleaned = url?.trim().orEmpty()
        val now = System.currentTimeMillis()
        // Same object exposed as BrowserKitDetector + AndroidInterface — block double call.
        if (cleaned == lastClickUrl && now - lastClickAtMs < 1_200L) {
            BrowserKitLog.w("JS", "onVideoClicked debounced")
            return
        }
        lastClickUrl = cleaned
        lastClickAtMs = now
        val logUrl = when {
            cleaned.isBlank() -> "(empty)"
            cleaned.startsWith("fbvid:") -> cleaned
            else -> BrowserKitLog.shortUrl(cleaned)
        }
        BrowserKitLog.i("JS", "onVideoClicked len=${cleaned.length} url=$logUrl")
        if (cleaned.startsWith("blob:")) {
            BrowserKitLog.w("JS", "onVideoClicked blob — forward empty for CDN fallback")
            mainHandler.post { onVideoClicked?.invoke("") }
            return
        }
        mainHandler.post { onVideoClicked?.invoke(cleaned) }
    }
}
