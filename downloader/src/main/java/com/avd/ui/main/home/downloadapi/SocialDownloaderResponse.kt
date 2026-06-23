package com.avd.ui.main.home.downloadapi

import kotlinx.serialization.Serializable

@Serializable
data class SocialDownloaderResponse(
    val success: Boolean? = null,
    val platform: String? = null,
    val apiUsed: String? = null,
    val thumbnail: String? = null,
    val videos: List<VideoItem> = emptyList()
)

@Serializable
data class VideoItem(
    val url: String? = null,
    val format: String? = null
)
