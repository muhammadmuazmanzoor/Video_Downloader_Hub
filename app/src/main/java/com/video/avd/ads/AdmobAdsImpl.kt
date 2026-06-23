package com.video.avd.ads

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.isProVersion
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.video.avd.BuildConfig

var bannerAdRequest: AdRequest? = null

var isShowingAd = false

var interstitialOb: InterstitialAd? = null
var interstitialHome: InterstitialAd? = null

var nativeLanguage: NativeAd? = null

var nativeLanguageAlt: NativeAd? = null


fun loadNativeLanguageHigh(activity: FragmentActivity) {
    if (AdBlockerHelper.isProVersion.value != true) {
        try {

            val adUnitId = BuildConfig.native_language_high
            val adLoader =
                AdLoader.Builder(activity, adUnitId) // ✅ use Activity context
                    .forNativeAd { nativeAd ->
                        nativeLanguage = nativeAd
                        nativeAd?.setOnPaidEventListener { adValue ->
                            adjustRevenueMMP(
                                adUnitId,
                                adValue.valueMicros / 1_000_000.0,
                                adValue.currencyCode,
                                "",
                                "Native"
                            )
                        }
                        //   showNativeLanguage()
                        LogUtils.printLog("language native hf loaded", BuildConfig.native_language_high)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d("nativeAd", "not loaded language $adError")
                            loadNativeLanguageNormal(activity)
                            LogUtils.printLog("language native hf failed", BuildConfig.native_language_high)
                        }
                    })
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}

fun loadNativeLanguageNormal(activity: FragmentActivity) {
    if (AdBlockerHelper.isProVersion.value != true) {
        try {
            val adUnitId = BuildConfig.native_language
            val adLoader =
                AdLoader.Builder(activity, adUnitId) // ✅ use Activity context
                    .forNativeAd { nativeAd ->
                        Log.d("nativeAd", "loaded language")
                        nativeLanguage = nativeAd
                        nativeAd?.setOnPaidEventListener { adValue ->
                            adjustRevenueMMP(
                                adUnitId,
                                adValue.valueMicros / 1_000_000.0,
                                adValue.currencyCode,
                                "",
                                "Native"
                            )
                        }
                        // showNativeLanguage()
                        LogUtils.printLog("language native  loaded", BuildConfig.native_language)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.d("nativeAd", "not loaded language $adError")
                            LogUtils.printLog("language native  failed", BuildConfig.native_language)
                        }
                    })
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}


fun loadNativeLanguageAltHigh(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    if (AdBlockerHelper.isProVersion.value != true) {
        try {
            val adUnitId = BuildConfig.native_language_alt_high
            val adLoader =
                AdLoader.Builder(activity, adUnitId) // ✅ use Activity context
                    .forNativeAd { nativeAd ->
                        nativeLanguageAlt = nativeAd
                        nativeAd?.setOnPaidEventListener { adValue ->
                            adjustRevenueMMP(
                                adUnitId,
                                adValue.valueMicros / 1_000_000.0,
                                adValue.currencyCode,
                                "",
                                "Native"
                            )
                        }
                        onResult(true)
                        // showNativeLanguageAlt()
                        LogUtils.printLog("language native alt hf loaded", BuildConfig.native_language_alt_high)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            onResult(false)
                            LogUtils.printLog("language native alt hf faliled", BuildConfig.native_language_alt_high)
                        }

                    })
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}

fun loadNativeLanguageAltNormal(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    if (isProVersion.value != true) {
        try {
            val adUnitId = BuildConfig.native_language_alt
            val adLoader =
                AdLoader.Builder(activity, adUnitId) // ✅ use Activity context
                    .forNativeAd { nativeAd ->
                        Log.d("nativeAd", "loaded language")
                        nativeLanguageAlt = nativeAd
                        nativeAd?.setOnPaidEventListener { adValue ->
                            adjustRevenueMMP(
                                adUnitId,
                                adValue.valueMicros / 1_000_000.0,
                                adValue.currencyCode,
                                "",
                                "Native"
                            )
                        }
                        onResult(true)
                        //  showNativeLanguageAlt()
                        LogUtils.printLog("language native alt loaded", BuildConfig.native_language_alt)
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            onResult(false)
                            LogUtils.printLog("language native alt failed", BuildConfig.native_language_alt)
                        }
                    })
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}

fun loadInterObHigh(
    context: Context
) {
    if (isProVersion.value ==true || interstitialOb!=null) return
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(context, BuildConfig.inter_ob_high, adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialOb = ad
            ad?.onPaidEventListener =
                OnPaidEventListener { adValue ->
                    adjustRevenueMMP(
                        ad?.adUnitId,
                        adValue.valueMicros / 1_000_000.0,
                        adValue.currencyCode,
                        "",
                        "Interstitial"
                    )
                }
            LogUtils.printLog("home_ob hf loaded", BuildConfig.inter_ob_high)
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            interstitialOb = null
            LogUtils.printLog("home_ob hf failed", BuildConfig.inter_ob_high)
        }
    })
}


fun loadInterOb(
    context: Context
) {
    if (isProVersion.value ==true || interstitialOb!=null) return
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(context, BuildConfig.inter_ob, adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            interstitialOb = ad
            ad?.onPaidEventListener =
                OnPaidEventListener { adValue ->
                    adjustRevenueMMP(
                        ad?.adUnitId,
                        adValue.valueMicros / 1_000_000.0,
                        adValue.currencyCode,
                        "",
                        "Interstitial"
                    )
                }
            LogUtils.printLog("home_ob  loaded", BuildConfig.inter_ob)
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            interstitialOb = null
            LogUtils.printLog("home_ob  failed", BuildConfig.inter_ob)
        }
    })
}

fun loadInterHomeHigh(
    context: Context, onResult: (Boolean) -> Unit
) {
    if (isProVersion.value ==true || interstitialHome!=null) return
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(context, BuildConfig.inter_home_high, adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            onResult(true)
            interstitialHome = ad
            ad?.onPaidEventListener =
                OnPaidEventListener { adValue ->
                    adjustRevenueMMP(
                        ad?.adUnitId,
                        adValue.valueMicros / 1_000_000.0,
                        adValue.currencyCode,
                        "",
                        "Interstitial"
                    )
                }
            LogUtils.printLog("home_inter hf loaded", BuildConfig.inter_home_high)
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            onResult(false)
            interstitialHome = null
            LogUtils.printLog("home_inter hf failed", BuildConfig.inter_home_high)
        }
    })
}

fun loadInterHome(
    context: Context, onResult: (Boolean) -> Unit
) {
    if (isProVersion.value == true || interstitialHome!=null) return
    val adRequest = AdRequest.Builder().build()

    InterstitialAd.load(context, BuildConfig.inter_home, adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            onResult(true)
            interstitialHome = ad
            ad?.onPaidEventListener =
                OnPaidEventListener { adValue ->
                    adjustRevenueMMP(
                        ad?.adUnitId,
                        adValue.valueMicros / 1_000_000.0,
                        adValue.currencyCode,
                        "",
                        "Interstitial"
                    )
                }
            //  showInterSplash(context,false)
            LogUtils.printLog("home_inter  loaded", BuildConfig.inter_home)
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            onResult(false)
            interstitialHome = null
            LogUtils.printLog("home_inter  failed", BuildConfig.inter_home)
        }
    })
}

fun adjustRevenueMMP(adUnitId: String?, adRevenue:Double=0.00, currency:String="USD", event_token:String="b0syy4",source : String){
    try {
        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
        adjustAdRevenue.setRevenue(adRevenue, currency)  // ✅ REQUIRED
        adjustAdRevenue.addPartnerParameter("ad_format", source)
        adjustAdRevenue.addPartnerParameter("ad_unit_id", adUnitId)
        Adjust.trackAdRevenue(adjustAdRevenue)

    } catch (e: Exception) {

    }
}


fun ViewGroup.getAdaptiveAdSize(activity: FragmentActivity): AdSize {
    val display = activity.windowManager.defaultDisplay
    val outMetrics = DisplayMetrics()
    display.getMetrics(outMetrics)

    val density = outMetrics.density
    val adWidthPixels = width.takeIf { it > 0 } ?: outMetrics.widthPixels
    val adWidth = (adWidthPixels / density).toInt()

    return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
}