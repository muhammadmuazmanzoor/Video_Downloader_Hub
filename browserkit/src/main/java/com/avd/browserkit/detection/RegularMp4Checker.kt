package com.avd.browserkit.detection

import android.net.Uri
import android.webkit.CookieManager
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.InstagramUrlUtils
import com.avd.browserkit.util.VideoUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Xilli [DetectedVideosTabViewModel.propagateCheckJobInternal] port.
 * Accept only Content-Type contains "video" AND Content-Length > [THRESHOLD] (default 5MB).
 * That filters FB CMAF moof stubs (~50–100KB) that look like video/mp4.
 */
object RegularMp4Checker {

    /** Xilli SharedPrefHelper videoDetectionTreshold default. */
    const val THRESHOLD_BYTES: Long = 5L * 1024 * 1024

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Xilli extension deny-list (cleared URL without query). */
    private val excludedExt = Regex(
        """\.(apk|html?|xml|ico|css|js|png|gif|json|jpe?g|svg|woff2?|ts|php|ttf|otf|eot|""" +
            """cur|webp|bmp|tif|tiff|psd|ai|eps|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|md|rtf|""" +
            """vtt|srt|swf|jar|log|txt|m4s)$""",
        RegexOption.IGNORE_CASE,
    )

    data class Result(
        val finalUrl: String,
        val contentLength: Long,
        val contentType: String,
        val headers: Map<String, String>,
    )

    fun shouldSkipUrl(url: String): Boolean {
        if (!url.startsWith("http")) return true
        val cleared = url.substringBefore('?').trim()
        return excludedExt.containsMatchIn(cleared)
    }

    /**
     * HEAD (then GET if needed). Returns non-null only when type=video and length > threshold
     * (or [forceStream] accepts unknown/short length like Super isForceStreamDetection).
     */
    fun check(
        url: String,
        requestHeaders: Map<String, String>,
        threshold: Long = THRESHOLD_BYTES,
        forceStream: Boolean = false,
    ): Result? {
        if (shouldSkipUrl(url)) return null

        val redirected = getFinalRedirectUrl(url, requestHeaders.toMutableMap()) ?: return null
        val finalUrl = redirected.first
        val headers = redirected.second.toMutableMap()

        // Fresh cookies for final host (Xilli).
        runCatching {
            val cookies = CookieManager.getInstance().getCookie(finalUrl)
                ?: CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) headers["Cookie"] = cookies
        }

        var response: okhttp3.Response? = null
        try {
            val headReq = Request.Builder().url(finalUrl).apply {
                headers.forEach { (k, v) -> if (v.isNotBlank()) header(k, v) }
            }.head().build()
            response = client.newCall(headReq).execute()

            var length = response.header("Content-Length")?.toLongOrNull()
            var typeStr = response.header("Content-Type").orEmpty()

            if (length == null && response.code == 200) {
                response.close()
                response = null
                val getReq = Request.Builder().url(finalUrl).apply {
                    headers.forEach { (k, v) -> if (v.isNotBlank()) header(k, v) }
                }.get().build()
                response = client.newCall(getReq).execute()
                length = response.body?.contentLength()?.takeIf { it >= 0 }
                    ?: response.header("Content-Length")?.toLongOrNull()
                typeStr = response.body?.contentType()?.toString()
                    ?: response.header("Content-Type").orEmpty()
            }

            val code = response?.code ?: 0
            BrowserKitLog.d(
                "RegularMp4",
                "code=$code type=$typeStr len=$length thresh=$threshold " +
                    "url=${BrowserKitLog.shortUrl(finalUrl)}",
            )

            // 401/403 retry without headers (Xilli)
            if (code == 403 || code == 401) {
                response?.close()
                response = null
                val emptyRedirect = getFinalRedirectUrl(url, mutableMapOf())
                if (emptyRedirect != null && length != null && length > threshold) {
                    val emptyGet = Request.Builder().url(emptyRedirect.first).get().build()
                    client.newCall(emptyGet).execute().use { emptyRes ->
                        val emptyType = emptyRes.header("Content-Type")
                            ?: emptyRes.body?.contentType()?.toString().orEmpty()
                        if (emptyType.contains("video", ignoreCase = true)) {
                            return Result(
                                finalUrl = emptyRedirect.first,
                                contentLength = length,
                                contentType = emptyType,
                                headers = emptyMap(),
                            )
                        }
                    }
                }
                return null
            }

            val isTikTok = VideoUtils.isTikTokMediaUrl(url) || finalUrl.contains(".tiktok.com/", ignoreCase = true)
            val isIg = InstagramUrlUtils.isInstagramMediaUrl(url) ||
                url.contains("cdninstagram", ignoreCase = true)
            val tikTokMin = 1024L * 1024 / 3
            // IG reels often 0.5–4MB progressive — Super-style lower floor than FB CMAF gate.
            val igMin = 512L * 1024
            val isVideo = typeStr.contains("video", ignoreCase = true)
            val sizeOk = when {
                length == null || length < 0L -> forceStream
                length > threshold -> true
                isTikTok && length > tikTokMin -> true
                isIg && length > igMin -> true
                forceStream -> true
                else -> false
            }
            if (isVideo && sizeOk) {
                return Result(
                    finalUrl = finalUrl,
                    contentLength = length ?: -1L,
                    contentType = typeStr,
                    headers = headers,
                )
            }
            return null
        } catch (t: Throwable) {
            BrowserKitLog.w("RegularMp4", "check fail ${t.message}")
            return null
        } finally {
            response?.close()
        }
    }

    /** Xilli CookieUtils.getFinalRedirectURL (max 5). */
    private fun getFinalRedirectUrl(
        url: String,
        headers: MutableMap<String, String>,
        depth: Int = 0,
    ): Pair<String, Map<String, String>>? {
        if (depth >= 5) return url to headers
        var con: HttpURLConnection? = null
        return try {
            con = URL(url).openConnection() as HttpURLConnection
            con.instanceFollowRedirects = false
            con.connectTimeout = 10_000
            con.readTimeout = 10_000
            headers.forEach { (k, v) -> con.setRequestProperty(k, v) }
            runCatching { con.connect() }
            val code = con.responseCode
            if (code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == 307 || code == 308
            ) {
                var location = con.getHeaderField("Location")
                val origin = con.getHeaderField("Access-Control-Allow-Origin")
                con.disconnect()
                con = null
                if (location.isNullOrBlank()) return url to headers
                if (!origin.isNullOrBlank()) headers["Referer"] = origin
                if (location.startsWith("//")) {
                    location = "https:$location"
                } else if (location.startsWith("/")) {
                    val u = Uri.parse(url)
                    location = "${u.scheme}://${u.host}$location"
                }
                return getFinalRedirectUrl(location, headers, depth + 1)
            }
            url to headers
        } catch (_: Exception) {
            url to headers
        } finally {
            con?.disconnect()
        }
    }
}
