package com.example.ammar.youtubedldynamic

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import com.avd.browserkit.ytdlp.YoutubeDlEngine
import com.avd.browserkit.ytdlp.YtDlpRawFormat
import com.avd.browserkit.ytdlp.YtDlpRawInfo
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

@Keep
class YoutubeDlEngineImpl : YoutubeDlEngine {

    @Volatile
    private var initialized = false

    @Volatile
    private var appContext: Context? = null

    override fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            YoutubeDL.getInstance().init(context.applicationContext)
            FFmpeg.getInstance().init(context.applicationContext)
            initialized = true
            Log.d(TAG, "YoutubeDL + FFmpeg initialized in dynamic module for BrowserKit")
            val app = context.applicationContext
            Thread({
                runCatching {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(
                        app,
                        YoutubeDL.UpdateChannel.STABLE,
                    )
                    Log.i(TAG, "yt-dlp update status=$status")
                }.onFailure {
                    Log.w(TAG, "yt-dlp update skipped: ${it.message}")
                }
            }, "ytdlp-update-bk").start()
        }
    }

    override fun getInfo(url: String, headers: Map<String, String>): YtDlpRawInfo? {
        ensureInit()
        val request = YoutubeDLRequest(url)
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        val cookieFile = applyHeadersAndCookies(request, url, headers, facebookMode = false)
        return try {
            val info = YoutubeDL.getInstance().getInfo(request)
            YtDlpRawInfo(
                title = info.title,
                url = info.url,
                ext = info.ext,
                duration = info.duration,
                manifestUrl = info.manifestUrl,
                formats = info.formats.orEmpty().mapNotNull { format ->
                    val formatUrl = format.url.orEmpty()
                    if (formatUrl.isBlank()) return@mapNotNull null
                    YtDlpRawFormat(
                        url = formatUrl,
                        height = format.height,
                        formatNote = format.formatNote,
                        ext = format.ext,
                        formatId = format.formatId,
                        manifestUrl = format.manifestUrl,
                    )
                },
            )
        } finally {
            cookieFile?.delete()
        }
    }

    override fun execute(
        url: String,
        outputPath: String,
        headers: Map<String, String>,
        facebookMode: Boolean,
        onProgress: (Float) -> Unit,
    ) {
        ensureInit()
        val request = YoutubeDLRequest(url)
        request.addOption("-o", outputPath)
        request.addOption("--no-playlist")
        request.addOption("--progress")
        request.addOption("-N", "4")

        val isDirectMedia = url.contains(".mp4", ignoreCase = true) ||
            url.contains(".m3u8", ignoreCase = true) ||
            url.contains("video.twimg.com", ignoreCase = true)

        if (!isDirectMedia && !facebookMode) {
            request.addOption("--recode-video", "mp4")
        }
        if (!isDirectMedia) {
            request.addOption("--merge-output-format", "mp4")
        }
        if (isDirectMedia) {
            request.addOption("--no-part")
        }
        request.addOption("--hls-prefer-native")
        request.addOption("--hls-use-mpegts")

        val cookieFile = applyHeadersAndCookies(request, url, headers, facebookMode)
        try {
            YoutubeDL.getInstance().execute(request, null) { progress, _, _ ->
                onProgress(progress)
            }
        } finally {
            cookieFile?.delete()
        }
    }

    private fun applyHeadersAndCookies(
        request: YoutubeDLRequest,
        url: String,
        headers: Map<String, String>,
        facebookMode: Boolean,
    ): File? {
        var cookieFile: File? = null
        headers.forEach { (k, v) ->
            if (k.equals("Cookie", ignoreCase = true)) {
                if (v.isNotBlank()) {
                    cookieFile = writeNetscapeCookieFile(url, v, facebookMode)
                    cookieFile?.let {
                        request.addOption("--cookies", it.absolutePath)
                        Log.i(TAG, "cookies file=${it.name} bytes=${it.length()}")
                    }
                }
                return@forEach
            }
            if (v.isNotBlank()) {
                request.addOption("--add-header", "$k:$v")
            }
        }
        return cookieFile
    }

    private fun writeNetscapeCookieFile(
        url: String,
        cookieHeader: String,
        facebookMode: Boolean,
    ): File? {
        val ctx = appContext ?: return null
        val host = Uri.parse(url).host.orEmpty()
        val domains = linkedSetOf<String>()
        when {
            facebookMode ||
                host.contains("facebook.com", ignoreCase = true) ||
                host.contains("fbcdn.net", ignoreCase = true) ||
                host.contains("fbsbx.com", ignoreCase = true) ||
                host.contains("fb.watch", ignoreCase = true) -> {
                domains.add(".facebook.com")
                if (host.contains("fbcdn", ignoreCase = true) ||
                    host.contains("fbsbx", ignoreCase = true)
                ) {
                    domains.add(".fbcdn.net")
                }
            }
            host.contains("instagram.com", ignoreCase = true) ||
                host.contains("cdninstagram.com", ignoreCase = true) -> {
                domains.add(".instagram.com")
            }
            host.isNotBlank() -> {
                val parts = host.split('.').filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    domains.add(".${parts.takeLast(2).joinToString(".")}")
                }
            }
            else -> domains.add(".facebook.com")
        }
        val dir = File(ctx.cacheDir, "ytdlp_cookies_bk").apply { mkdirs() }
        val file = File(dir, "c_${url.hashCode()}_${System.nanoTime()}.txt")
        val sb = StringBuilder("# Netscape HTTP Cookie File\n")
        val pairs = cookieHeader.split(';')
            .map { it.trim() }
            .filter { it.contains('=') }
        for (domain in domains) {
            val domainField = if (domain.startsWith(".")) domain else ".$domain"
            for (pair in pairs) {
                val eq = pair.indexOf('=')
                val name = pair.substring(0, eq).trim()
                val value = pair.substring(eq + 1).trim()
                if (name.isEmpty()) continue
                sb.append(domainField)
                    .append("\tTRUE\t/\tTRUE\t0\t")
                    .append(name)
                    .append('\t')
                    .append(value)
                    .append('\n')
            }
        }
        file.writeText(sb.toString())
        return file
    }

    private fun ensureInit() {
        check(initialized) { "YoutubeDlEngineImpl not initialized" }
    }

    companion object {
        private const val TAG = "YoutubeDlEngineImplBK"
    }
}
