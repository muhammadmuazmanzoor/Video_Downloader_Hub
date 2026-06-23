package com.video.avd.ads

import androidx.annotation.Keep

@Keep
data class LangAdConfig(
    val SessionCount: Int = 0,

    // Language screen
    val LanguageButton: Int = 0,
    val LanguageButtonDelay: Int = 3,

    // Interstitial ads
    val InterstitialHigh: Int = 1,
    val InterstitialAll: Int = 1,

    // Native ads
    val NativeHigh1: Int = 1,
    val NativeAll1: Int = 1,
    val NativeHigh2: Int = 1,
    val NativeAll2: Int  = 1,
    val NativeFormat: Int  = 1,

    // Ad CTA
    val AdCtaColor: String = "#0AC43E",
    val AdCtaTextColor: String = "#000000",
    val CtaTextStyle: String =  "bold"
)