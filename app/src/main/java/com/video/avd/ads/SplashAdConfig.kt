package com.video.avd.ads

import androidx.annotation.Keep

@Keep
data class SplashAdConfig(
    val Interstitialhigh: Int = 1,
    val NativeFullHigh : Int = 1,
    val InterstitialMedium: Int = 1,
    val Interstitialall: Int = 1,
    val Appopenhigh: Int = 0,
    val Appopen: Int = 0,
    val Bannerhigh: Int = 1,
    val Banner: Int = 1,
    val Nativehigh: Int = 0,
    val Native: Int = 0,
    val NativeFormat:Int = 1,
    val Ctacolor: String = "#F75655",
    val CtatextColor: String = "#000000",
    val CtatextStyle: String = "bold",
    val NativeFullCrossDelay : Int = 3,
)