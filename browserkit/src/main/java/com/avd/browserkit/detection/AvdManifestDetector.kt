package com.avd.browserkit.detection

import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.hls_parser.HlsPlaylistParser
import com.avd.browserkit.util.hls_parser.MpdPlaylistParser
import java.time.Duration
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Super VideoServiceAvd port: parse HLS/MPD manifests in-app (Media3) → [DetectedVideoInfo]
 * with [DetectedVideoInfo.isDetectedByAvd]=true for Avd segment download routing.
 */
object AvdManifestDetector {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun detect(
        manifestUrl: String,
        headers: Map<String, String>,
        pageUrl: String,
        titleHint: String,
        isM3u8: Boolean,
        isMpd: Boolean,
    ): DetectedVideoInfo? {
        if (!(isM3u8 || isMpd) || !manifestUrl.startsWith("http")) return null
        return try {
            val req = Request.Builder().url(manifestUrl).apply {
                headers.forEach { (k, v) -> if (v.isNotBlank()) header(k, v) }
            }.get().build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isEmpty()) {
                    BrowserKitLog.w(
                        "AvdDetect",
                        "manifest HTTP ${response.code} empty=${body.isEmpty()} " +
                            BrowserKitLog.shortUrl(manifestUrl),
                    )
                    return null
                }
                if (isM3u8) {
                    parseHls(HlsPlaylistParser.parse(body, manifestUrl), headers, pageUrl, titleHint)
                } else {
                    parseMpd(MpdPlaylistParser.parse(body, manifestUrl), headers, pageUrl, titleHint)
                }
            }
        } catch (t: Throwable) {
            BrowserKitLog.w("AvdDetect", "parse fail ${t.message}")
            null
        }
    }

    private fun parseHls(
        manifest: HlsPlaylistParser.HlsPlaylist,
        headers: Map<String, String>,
        pageUrl: String,
        titleHint: String,
    ): DetectedVideoInfo? {
        val formats = mutableListOf<StreamFormat>()
        var isLive = false

        when (manifest) {
            is HlsPlaylistParser.MasterPlaylist -> {
                val audioByGroup = manifest.alternateRenditions
                    .filter { it.type == HlsPlaylistParser.RenditionType.AUDIO && it.url != null }
                    .groupBy { it.groupId }

                for (variant in manifest.variants) {
                    val height = variant.height
                    val width = variant.resolution?.split("x")?.getOrNull(0)?.toIntOrNull() ?: 0
                    if (width <= 0 && height <= 0) continue
                    val h = if (height > 0) height else 0
                    val audioUrl = audioByGroup[variant.audioGroupId]?.firstOrNull()?.url
                    formats.add(
                        StreamFormat(
                            url = variant.url.ifBlank { manifest.baseUri },
                            label = if (h > 0) "HLS ${h}p" else "HLS",
                            ext = "mp4",
                            streamType = StreamType.HLS_M3U8,
                            headers = headers,
                            formatId = "hls-${h}p-${variant.bandwidth}",
                            manifestUrl = manifest.baseUri,
                            audioUrl = audioUrl,
                        ),
                    )
                }
                // Probe first media playlist for live flag (best-effort).
                val first = formats.firstOrNull()?.url
                if (!first.isNullOrBlank()) {
                    isLive = runCatching { probeHlsLive(first, headers) }.getOrDefault(false)
                }
            }

            is HlsPlaylistParser.MediaPlaylist -> {
                isLive = !manifest.hasEndList
                val inferred = manifest.baseUri.substringAfterLast('-')
                    .substringBefore('.').toIntOrNull()
                formats.add(
                    StreamFormat(
                        url = manifest.baseUri,
                        label = if (inferred != null) "HLS ${inferred}p" else "HLS",
                        ext = "mp4",
                        streamType = StreamType.HLS_M3U8,
                        headers = headers,
                        formatId = "hls-media",
                        manifestUrl = manifest.baseUri,
                    ),
                )
            }
        }

        if (formats.isEmpty()) return null
        val sorted = formats.sortedByDescending {
            it.formatId.substringAfterLast('-').toLongOrNull() ?: 0L
        }
        BrowserKitLog.i(
            "AvdDetect",
            "HLS formats=${sorted.size} live=$isLive url=${BrowserKitLog.shortUrl(manifest.baseUri)}",
        )
        return DetectedVideoInfo(
            pageUrl = pageUrl.ifBlank { manifest.baseUri },
            title = titleHint.ifBlank { "HLS Stream" },
            formats = sorted,
            headers = headers,
            isLive = isLive,
            isRegularDownload = false,
            isDetectedByAvd = true,
        )
    }

    private fun parseMpd(
        manifest: MpdPlaylistParser.MpdManifest,
        headers: Map<String, String>,
        pageUrl: String,
        titleHint: String,
    ): DetectedVideoInfo? {
        val isLive = manifest.type.equals("dynamic", ignoreCase = true)
        val formats = manifest.periods.flatMap { it.adaptationSets }
            .filter { it.mimeType?.startsWith("video/") == true }
            .flatMap { it.representations }
            .mapNotNull { rep ->
                if (rep.height <= 0 || rep.width <= 0) return@mapNotNull null
                StreamFormat(
                    url = manifest.baseUri,
                    label = "DASH ${rep.height}p",
                    ext = "mp4",
                    streamType = StreamType.DASH_MPD,
                    headers = headers,
                    formatId = "mpd-${rep.height}p-${rep.bandwidth}",
                    manifestUrl = manifest.baseUri,
                )
            }
            .sortedByDescending { it.label.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

        if (formats.isEmpty()) return null
        BrowserKitLog.i(
            "AvdDetect",
            "MPD formats=${formats.size} live=$isLive url=${BrowserKitLog.shortUrl(manifest.baseUri)}",
        )
        return DetectedVideoInfo(
            pageUrl = pageUrl.ifBlank { manifest.baseUri },
            title = titleHint.ifBlank { "MPEG-DASH Stream" },
            formats = formats,
            headers = headers,
            isLive = isLive,
            isRegularDownload = false,
            isDetectedByAvd = true,
        )
    }

    private fun probeHlsLive(mediaPlaylistUrl: String, headers: Map<String, String>): Boolean {
        val req = Request.Builder().url(mediaPlaylistUrl).apply {
            headers.forEach { (k, v) -> if (v.isNotBlank()) header(k, v) }
        }.get().build()
        client.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful || body.isEmpty()) return false
            val parsed = HlsPlaylistParser.parse(body, mediaPlaylistUrl)
            return parsed is HlsPlaylistParser.MediaPlaylist && !parsed.hasEndList
        }
    }

    @Suppress("unused")
    private fun parseIso8601Duration(duration: String?): Long {
        if (duration.isNullOrBlank()) return 0L
        try {
            return Duration.parse(duration).toMillis()
        } catch (_: Throwable) {
        }
        val pattern =
            Regex("PT(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?")
        val match = pattern.matchEntire(duration) ?: return 0L
        val hours = match.groupValues[1].ifEmpty { "0" }.toDouble()
        val minutes = match.groupValues[2].ifEmpty { "0" }.toDouble()
        val seconds = match.groupValues[3].ifEmpty { "0" }.toDouble()
        return ((hours * 3600 + minutes * 60 + seconds) * 1000).toLong()
    }
}
