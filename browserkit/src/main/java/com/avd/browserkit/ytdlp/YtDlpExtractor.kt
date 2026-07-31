package com.avd.browserkit.ytdlp

import com.avd.browserkit.BrowserKitInitializer
import com.avd.browserkit.detection.DetectedVideoInfo
import com.avd.browserkit.detection.StreamFormat
import com.avd.browserkit.detection.StreamType
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.DailymotionUrlUtils
import com.avd.browserkit.util.InstagramUrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YtDlpExtractor {
    suspend fun extract(url: String, cookies: String?, userAgent: String? = null): DetectedVideoInfo? =
        withContext(Dispatchers.IO) {
            if (!isExtractableUrl(url)) {
                BrowserKitLog.w("YtDlp", "extract skip unsupported ${BrowserKitLog.shortUrl(url)}")
                return@withContext null
            }
            if (!BrowserKitInitializer.isInitialized()) {
                BrowserKitLog.w("YtDlp", "extract abort: BrowserKitInitializer not ready")
                return@withContext null
            }
            val engine = YoutubeDlBridge.engineOrNull()
            if (engine == null) {
                BrowserKitLog.w("YtDlp", "extract abort: YoutubeDlEngine null (DFM missing?)")
                return@withContext null
            }
            val headers = buildMap {
                if (!cookies.isNullOrBlank()) put("Cookie", cookies)
                if (!userAgent.isNullOrBlank()) put("User-Agent", userAgent)
                // Xilli SocialDownloadUtils: IG needs Referer or CDN/extract fails
                if (InstagramUrlUtils.isInstagramUrl(url)) {
                    put("Referer", InstagramUrlUtils.REFERER)
                }
            }
            BrowserKitLog.i(
                "YtDlp",
                "getInfo url=${BrowserKitLog.shortUrl(url)} headerKeys=${headers.keys}",
            )
            runCatching {
                val info = engine.getInfo(url, headers)
                if (info == null) {
                    BrowserKitLog.w("YtDlp", "getInfo returned null")
                    return@runCatching null
                }
                val parsed = parseInfo(url, info)
                BrowserKitLog.i(
                    "YtDlp",
                    "parsed formats=${parsed?.formats?.size ?: 0} title=${parsed?.title}",
                )
                parsed
            }.onFailure {
                BrowserKitLog.e("YtDlp", "getInfo exception", it)
            }.getOrNull()
        }

    private fun parseInfo(pageUrl: String, info: YtDlpRawInfo): DetectedVideoInfo? {
        val title = info.title?.ifBlank { pageUrl } ?: pageUrl
        val formats = info.formats.mapNotNull { it.toStreamFormat() }.distinctBy { it.url }
        val resolved = if (formats.isNotEmpty()) {
            formats
        } else {
            val directUrl = info.url.orEmpty()
            if (directUrl.isBlank()) return null
            listOf(
                StreamFormat(
                    url = directUrl,
                    label = "Best",
                    ext = info.ext ?: "mp4",
                    streamType = streamTypeForUrl(directUrl, info.ext, info.manifestUrl),
                ),
            )
        }
        return DetectedVideoInfo(
            pageUrl = pageUrl,
            title = title,
            formats = resolved,
            isLive = info.duration == 0 && resolved.any {
                it.streamType == StreamType.HLS_M3U8 || it.streamType == StreamType.DASH_MPD
            },
        )
    }

    private fun YtDlpRawFormat.toStreamFormat(): StreamFormat? {
        if (url.isBlank()) return null
        val extValue = ext ?: "mp4"
        val label = buildString {
            if (height > 0) append("${height}p")
            if (!formatNote.isNullOrBlank()) {
                if (isNotEmpty()) append(" ")
                append(formatNote)
            }
            if (extValue.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(extValue)
            }
        }.ifBlank { formatId ?: "format" }
        return StreamFormat(
            url = url,
            label = label,
            ext = extValue,
            streamType = streamTypeForUrl(url, extValue, manifestUrl),
        )
    }

    /** Drop Google telemetry / search / enablejs — not video pages. */
    private fun isExtractableUrl(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false
        val lower = url.lowercase()
        val host = runCatching { android.net.Uri.parse(url).host.orEmpty().lowercase() }
            .getOrDefault("")
        if (host.isBlank()) return false
        if (host == "google.com" || host == "www.google.com" ||
            host.endsWith(".google.com") && !host.contains("googlevideo")
        ) {
            return false
        }
        if (lower.contains("httpservice/retry") || lower.contains("enablejs")) return false
        // Dailymotion: only /video/… (and playlist) pages — not home / locale / channel.
        if (DailymotionUrlUtils.isDailymotionUrl(url) &&
            !DailymotionUrlUtils.isDailymotionVideoPage(url)
        ) {
            return false
        }
        return true
    }

    private fun streamTypeForUrl(url: String, ext: String?, manifestUrl: String?): StreamType {
        val lowerUrl = url.lowercase()
        val lowerManifest = manifestUrl.orEmpty().lowercase()
        return when {
            lowerUrl.contains(".m3u8") || lowerManifest.contains(".m3u8") -> StreamType.HLS_M3U8
            lowerUrl.contains(".mpd") || lowerManifest.contains(".mpd") -> StreamType.DASH_MPD
            ext == "mp3" || ext == "m4a" -> StreamType.AUDIO
            else -> StreamType.PROGRESSIVE_MP4
        }
    }
}
