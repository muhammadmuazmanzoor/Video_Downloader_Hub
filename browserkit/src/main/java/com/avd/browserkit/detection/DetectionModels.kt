package com.avd.browserkit.detection

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ContentType {
    M3U8,
    MPD,
    VIDEO,
    AUDIO,
    OTHER,
}

enum class StreamType {
    PROGRESSIVE_MP4,
    HLS_M3U8,
    DASH_MPD,
    AUDIO,
    /** Xilli-style: yt-dlp download of FB CDN / data-video-url (skip -f / recode). */
    FACEBOOK_YTDLP,
    UNKNOWN,
}

@Parcelize
data class StreamFormat(
    val url: String,
    val label: String,
    val ext: String,
    val streamType: StreamType,
    val headers: Map<String, String> = emptyMap(),
    /** Super Avd formatId e.g. hls-720p-2500000 */
    val formatId: String = "",
    /** Master / MPD manifest URL when [url] is a media playlist. */
    val manifestUrl: String = "",
    /** Separate audio media playlist (HLS). */
    val audioUrl: String? = null,
) : Parcelable

@Parcelize
data class DetectedVideoInfo(
    val pageUrl: String,
    val title: String,
    val formats: List<StreamFormat>,
    val headers: Map<String, String> = emptyMap(),
    val isLive: Boolean = false,
    /**
     * Xilli [VideoInfo.isRegularDownload]:
     * true → CustomRegular (OkHttp/chunks + headers)
     * false → yt-dlp (HLS/DASH/Facebook/page extract) unless [isDetectedByAvd]
     */
    val isRegularDownload: Boolean = true,
    /**
     * Super Avd (ex-SuperX): HLS/MPD parsed in-app → Avd segment downloader.
     */
    val isDetectedByAvd: Boolean = false,
) : Parcelable

sealed class DownloadButtonState {
    data object CannotDownload : DownloadButtonState()
    data object Loading : DownloadButtonState()
    data class CanDownload(val info: DetectedVideoInfo) : DownloadButtonState()
}
