package com.avd.data.remote.service

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.common.net.InternetDomainName
import com.avd.data.local.model.Proxy
import com.avd.data.local.model.VideoInfoWrapper
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.util.AppLogger
import com.avd.util.CookieUtils
import com.avd.util.YoutubeDlUtils
import com.avd.util.proxy_utils.CustomProxyController
import com.avd.util.proxy_utils.OkHttpProxyClient
import com.avd.youtubedl.VideoFormat
import com.avd.youtubedl.YoutubeDLRequest
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.Locale
import javax.inject.Inject

interface VideoService {
    fun getVideoInfo(url: Request): VideoInfoWrapper?
}

open class VideoServiceLocal(
    private val proxyController: CustomProxyController, private val helper: YoutubedlHelper
) : VideoService {
    companion object {
        const val MP4_EXT = "mp4"
        private const val M3U8_EXT = ".m3u8"
        private const val TXT_EXT = ".txt"
        private const val MPD_EXT = ".mpd"
        private const val FACEBOOK_HOST = ".facebook."
        const val COOKIE_HEADER = "Cookie"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getVideoInfo(url: Request): VideoInfoWrapper? {
        AppLogger.d("Getting info url...:  $url  ${url.headers["Cookie"]}")

        var result: VideoInfoWrapper? = null

        try {
            val isM3u8 = url.url.toString().contains(M3U8_EXT, true)
            val isHentaiHavenM3u8 = url.url.toString().contains(TXT_EXT, true)
            val isMpd = url.url.toString().contains(MPD_EXT, true)
            result = if (isM3u8 || isHentaiHavenM3u8 || isMpd) {
              val t =   handleYoutubeDlUrl(url, true)
                AppLogger.d("YoutubeDL videoInfo: ${t.videoInfo}")
                t
            } else {
                handleYoutubeDlUrl(url)
            }
        } catch (e: Throwable) {
            AppLogger.d("YoutubeDL Error: $e")
            Log.e("mTag", "Error while fetching video info: ", e)
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleYoutubeDlUrl(url: Request, isM3u8OrMpd: Boolean = false): VideoInfoWrapper {
        if (!isM3u8OrMpd && !isYotubeDlSupportedHost(url.url.host)) {
            throw Throwable("host not in supported list")
        }
        val request = YoutubeDlUtils.getMappedYoutubeDLRequest(url.url.toString())
        url.headers.names().forEach {
            if (it != COOKIE_HEADER) {
                request?.addOption("--add-header", "$it:${url.headers[it]}")
            }
        }
        val currentProxy = proxyController.getCurrentRunningProxy()
        if (currentProxy != Proxy.noProxy()) {
            attachProxyToRequest(request, currentProxy)
        }
        val tmpCookieFile = CookieUtils.addCookiesToRequest(url.url.toString(), request)
        try {
            val instance = YoutubeDlUtils.youtubeDl
            val info = YoutubeDlUtils.getYtdlInfo(instance, request)
            val formats = info?.formats?.map {
                videoEntityFromFormat(it)
            }
            val filtered = arrayListOf<VideoFormatEntity>()
            if (url.url.toString().contains(FACEBOOK_HOST)) {
                if (formats != null) {
                    filtered.addAll(formats.filter {
                        it.formatId?.lowercase(Locale.ROOT)?.contains(Regex("hd|sd")) == true
                    })
                }
            }
            val listFormats = VideFormatEntityList(filtered.ifEmpty { formats?.filter { !(it.acodec != "none" && it.vcodec == "none") } } ?: emptyList())
            if (listFormats.formats.isEmpty()) throw Exception("Audio Only Detected")
            return VideoInfoWrapper(VideoInfo(title = info?.title ?: "no title").also { videoInfo ->
                videoInfo.ext = info?.ext ?: MP4_EXT
                videoInfo.thumbnail = info?.thumbnail ?: ""
                videoInfo.duration = info?.duration?.toLong()!!
                videoInfo.originalUrl = url.url.toString()
                videoInfo.downloadUrls = if (isM3u8OrMpd) emptyList() else listOf(url)
                videoInfo.formats = listFormats
                videoInfo.isRegularDownload = false
            })
        } catch (e: Throwable) {
            throw e
        } finally {
            tmpCookieFile.delete()
        }
    }


    private fun attachProxyToRequest(request: YoutubeDLRequest?, currentProxy: Proxy) {
        val user = proxyController.getProxyCredentials().first
        val password = proxyController.getProxyCredentials().second
        if (user.isNotEmpty() && password.isNotEmpty()) {
            request?.addOption(
                "--proxy", "https://${user}:${password}@${currentProxy.host}:${currentProxy.port}"
            )
        } else {
            request?.addOption(
                "--proxy", "${currentProxy.host}:${currentProxy.port}"
            )
        }
    }

    private fun isYotubeDlSupportedHost(host: String): Boolean {
        return helper.isHostSupported(host)
    }

    private fun videoEntityFromFormat(videoFormat: VideoFormat): VideoFormatEntity {
        return VideoFormatEntity(
            asr = videoFormat.asr,
            tbr = videoFormat.tbr,
            abr = videoFormat.abr,
            format = videoFormat.format,
            formatId = videoFormat.formatId,
            formatNote = videoFormat.formatNote,
            ext = videoFormat.ext,
            preference = videoFormat.preference,
            vcodec = videoFormat.vcodec,
            acodec = videoFormat.acodec,
            width = videoFormat.width,
            height = videoFormat.height,
            fileSize = videoFormat.fileSize,
            fileSizeApproximate = videoFormat.fileSizeApproximate,
            fps = videoFormat.fps,
            url = videoFormat.url,
            manifestUrl = videoFormat.manifestUrl,
            httpHeaders = videoFormat.httpHeaders
        )
    }
}

class YoutubedlHelper @Inject constructor(private val okHttpProxyClient: OkHttpProxyClient) {
    companion object {
        private const val SUPPORTED_SITES_URL =
            "https://ytb-dl.github.io/ytb-dl/supportedsites.html"
    }

    private val sites: HashSet<String> = HashSet()
    private var isLoading = false

    fun isHostSupported(host: String): Boolean {
        if (sites.isEmpty() || isLoading) {
            try {
                loadFromAssets()
            } catch (e: Throwable) {
                e.printStackTrace()
                isLoading = false

                return true
            }

            return true
        }

        return try {
            val domainName: InternetDomainName = InternetDomainName.from(host).topPrivateDomain()
            val fixedName = domainName.toString().replace(Regex("\\.\\w{2,}$"), "")

            sites.contains(fixedName) || sites.contains("${fixedName}.com")
        } catch (e: Exception) {
            true
        }
    }

    private fun loadFromAssets() {
        if (!isLoading) {
            isLoading = true

            val response = okHttpProxyClient.getProxyOkHttpClient().newCall(
                Request.Builder().url(SUPPORTED_SITES_URL).build()
            ).execute()
            val doc = response.body?.string()?.let { Jsoup.parse(it) }
            val sitesB = doc?.select("li > b")

            if (sitesB != null) {
                for (b in sitesB) {
                    val value =
                        b.text().trim().split(":").first().trim().lowercase().replace("- **", "")
                            .replace("**", "").trim()
                    sites.add(value)
                }
            }
            isLoading = false
        }
    }
}
