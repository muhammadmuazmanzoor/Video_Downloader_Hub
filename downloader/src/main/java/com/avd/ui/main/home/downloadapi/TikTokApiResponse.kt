package com.avd.ui.main.home.downloadapi

data class TikTokApiResponse(
    val code: Int,
    val msg: String,
    val processed_time: Double,
    val data: TikTokData
)
