package com.avd.ui.main.home.downloadapi

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class VideoResponse(
    @SerializedName("type") val type: String = "",
    @SerializedName("version") val version: Version = Version(),
    @SerializedName("formats") val formats: List<VideoFormat> = emptyList(),
    @SerializedName("acodec") val acodec: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artists") val artists: List<String>? = null,
    @SerializedName("aspect_ratio") val aspectRatio: Double? = null,
    @SerializedName("audio_ext") val audioExt: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("channel_id") val channelId: String? = null,
    @SerializedName("channel_url") val channelUrl: String? = null,
    @SerializedName("comment_count") val commentCount: Int? = null,
    @SerializedName("cookies") val cookies: String? = null,
    @SerializedName("displayId") val displayId: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("duration_string") val durationString: String? = null,
    @SerializedName("dynamic_range") val dynamicRange: String? = null,
    @SerializedName("epoch") val epoch: Long? = null,
    @SerializedName("ext") val ext: String? = null,
    @SerializedName("extractor") val extractor: String? = null,
    @SerializedName("extractor_key") val extractorKey: String? = null,
    @SerializedName("filesize") val filesize: Int? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("format_id") val formatId: String? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("fulltitle") val title :String? = null,
)

@Serializable
data class Version(
    val releaseGitHead: String? = null,
    val repository: String? = null,
    val version: String? = null
)

@Serializable
data class VideoFormat(
    val acodec: String? = null,
    val aspect_ratio: Double? = null,
    val audio_ext: String? = null,
    val ext: String? = null,
    val filesize_approx: Int? = null,
    val format: String? = null,
    val format_id: String? = null,
    val height: Int? = null,
    val extractor_key : String? = null,
    val protocol: String? = null,
    val quality: Int? = null,
    val resolution: String? = null,
    val tbr: Double? = null,
    val url: String? = null,
    val vcodec: String? = null,
    val video_ext: String? = null,
    val width: Int? = null,
    val cookies: String?=null,
    val http_headers:HttpHeaders?=null,
    val qualit:String?="Normal"
)

@Serializable
data class HttpHeaders(
    val Accept: String? = null,
    val `Accept-Language`: String? = null,
    val Referer: String? = null,
    val `Sec-Fetch-Mode`: String? = null,
    val `User-Agent`: String? = null
)

@Serializable
data class Thumbnail(
    val id: String,
    val preference: Int,
    val url: String
)
