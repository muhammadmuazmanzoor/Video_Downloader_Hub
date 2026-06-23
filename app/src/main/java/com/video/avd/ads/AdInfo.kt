package com.video.avd.ads

import com.video.avd.ads.AdPlacement

data class AdInfo(
    val placement: AdPlacement?,
    val type: String // "Highfloor", "Normal", or "Unknown"
)