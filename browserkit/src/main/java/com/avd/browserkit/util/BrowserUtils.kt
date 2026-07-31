package com.avd.browserkit.util

import com.avd.browserkit.detection.ContentType
import com.avd.browserkit.detection.StreamType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object VideoUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val skipProbeRegex = Regex("\\.(js|css|m4s|ts|vtt|jpg|jpeg|png|gif|webp|svg|woff2?|ico)(\\?|$)", RegexOption.IGNORE_CASE)
    private val mediaUrlRegex = Regex(
        """\.m3u8|\.mpd|\.mp4|\.webm|\.m4v|\.mov|/hls/|/dash/|videoplayback|googlevideo|mime=video|mime%3Dvideo|master\.txt|index\.m3u8|playlist\.m3u8|manifest|cdninstagram|scontent.*fbcdn|/o1/v/t16/|/v/t16/|/video/tos/|playwm|playaddr|aweme/v1/play|dmcdn\.net.*(\.m3u8|\.mpd|\.mp4|manifest|/hls/|mpegurl|sec2\()""",
        RegexOption.IGNORE_CASE,
    )

    fun isTikTokMediaUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        return lower.contains("tiktokcdn") ||
            lower.contains("tiktokv") ||
            lower.contains("muscdn") ||
            lower.contains("byteoversea") ||
            lower.contains("ibyteimg") ||
            lower.contains("ibytedtos") ||
            lower.contains("aweme/v1/play") ||
            lower.contains("playwm") ||
            lower.contains("playaddr") ||
            lower.contains("/video/tos/") ||
            lower.contains("/o1/v/") ||
            lower.contains("/v/t16/")
    }

    fun looksLikeMediaUrl(url: String): Boolean {
        if (url.isBlank() || url.startsWith("blob:") || url.startsWith("data:")) return false
        if (skipProbeRegex.containsMatchIn(url)) return false
        if (isTikTokMediaUrl(url)) return true
        if (InstagramUrlUtils.isInstagramMediaUrl(url)) return true
        if (DailymotionUrlUtils.isDailymotionMediaUrl(url) &&
            (url.contains(".m3u8", true) || url.contains(".mpd", true) ||
                url.contains(".mp4", true) || url.contains("manifest", true) ||
                url.contains("/hls/", true) || url.contains("mpegurl", true))
        ) {
            return true
        }
        return mediaUrlRegex.containsMatchIn(url)
    }

    fun streamTypeFromUrl(url: String): StreamType? {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("mpegurl") -> StreamType.HLS_M3U8
            lower.contains(".mpd") || lower.contains("/dash/") -> StreamType.DASH_MPD
            lower.contains(".mp3") || lower.contains(".m4a") -> StreamType.AUDIO
            lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".m4v") ||
                lower.contains("videoplayback") || lower.contains("googlevideo") -> StreamType.PROGRESSIVE_MP4
            isTikTokMediaUrl(url) -> StreamType.PROGRESSIVE_MP4
            // Instagram CDN often omits file extension
            InstagramUrlUtils.isInstagramMediaUrl(url) -> StreamType.PROGRESSIVE_MP4
            else -> null
        }
    }

    fun getContentTypeByUrl(
        url: String,
        headers: Map<String, String>?,
        pageUrl: String? = null,
        userAgent: String? = null,
    ): ContentType {
        if (skipProbeRegex.containsMatchIn(url)) return ContentType.OTHER
        streamTypeFromUrl(url)?.let { streamType ->
            return when (streamType) {
                StreamType.HLS_M3U8 -> ContentType.M3U8
                StreamType.DASH_MPD -> ContentType.MPD
                StreamType.AUDIO -> ContentType.AUDIO
                StreamType.PROGRESSIVE_MP4 -> ContentType.VIDEO
                else -> ContentType.OTHER
            }
        }

        return runCatching {
            val builder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1")
                .get()
            headers?.forEach { (k, v) -> builder.addHeader(k, v) }
            if (!userAgent.isNullOrBlank()) builder.header("User-Agent", userAgent)
            if (!pageUrl.isNullOrBlank()) builder.header("Referer", pageUrl)
            client.newCall(builder.build()).execute().use { response ->
                val contentTypeStr = response.header("Content-Type").orEmpty().lowercase()
                when {
                    contentTypeStr.contains("mpegurl") || url.contains(".m3u8", true) -> ContentType.M3U8
                    contentTypeStr.contains("dash") || url.contains(".mpd", true) -> ContentType.MPD
                    contentTypeStr.contains("video") -> ContentType.VIDEO
                    contentTypeStr.contains("audio") -> ContentType.AUDIO
                    contentTypeStr.contains("application/octet-stream") ||
                        contentTypeStr.contains("application/vnd.apple.mpegurl") -> {
                        val peek = response.body?.string()?.take(64).orEmpty()
                        when {
                            peek.startsWith("#EXTM3U") -> ContentType.M3U8
                            peek.contains("<MPD") -> ContentType.MPD
                            else -> ContentType.OTHER
                        }
                    }
                    else -> ContentType.OTHER
                }
            }
        }.getOrDefault(ContentType.OTHER)
    }
}

object CookieUtils {
    fun headersFromWebView(url: String, userAgent: String? = null, referer: String? = null): Map<String, String> {
        val headers = linkedMapOf<String, String>()
        val cookie = android.webkit.CookieManager.getInstance().getCookie(url).orEmpty()
        if (cookie.isNotBlank()) headers["Cookie"] = cookie
        if (!userAgent.isNullOrBlank()) headers["User-Agent"] = userAgent
        if (!referer.isNullOrBlank()) headers["Referer"] = referer
        return headers
    }

    fun mergeHeaders(vararg maps: Map<String, String>): Map<String, String> {
        val merged = linkedMapOf<String, String>()
        maps.forEach { map -> merged.putAll(map) }
        return merged
    }
}

object UrlUtils {
    fun normalizeInput(raw: String, searchTemplate: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "about:blank"
        if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            return trimmed
        }
        if (trimmed.contains('.') && !trimmed.contains(' ')) {
            return "https://$trimmed"
        }
        return searchTemplate.replace("{query}", java.net.URLEncoder.encode(trimmed, "UTF-8"))
    }

    fun isHttpUrl(url: String): Boolean {
        return url.startsWith("http://", true) || url.startsWith("https://", true)
    }
}
