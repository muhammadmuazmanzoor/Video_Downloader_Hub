package com.avd.util.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.avd.R
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.cooldownValue
import com.avd.util.AdBlockerHelper.firebaseUserAction
import com.avd.util.AdBlockerHelper.isPro
import com.avd.util.AdBlockerHelper.maxAdImpressions
import com.avd.util.AdBlockerHelper.remotemaxAdImpressions
import com.avd.util.AdBlockerHelper.setinterstitialshown
import com.avd.util.AdBlockerHelper.showLoading
import com.avd.util.AdBlockerHelper.trackAdjustAdRevenue
import com.avd.util.AppConstant
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object InterstitialManagerA {
    private const val TAG = "InterstitialManagerA"
    private var loadingDialog: Dialog? = null
    private var isHighLoading = false
    private var isAllLoading = false
    var homeInterstitialHigh: InterstitialAd? = null
    var homeInterstitialAll: InterstitialAd? = null
    private var highRetryCount = 0
    private const val MAX_HIGH_RETRIES = 2
    var interstitialAdCounter = 3


    // Call this once during app startup, for example in Application class

    // Load high interstitial
    private fun loadHomeInterstitialHigh(
        activity: Activity,
        onFailed: (error: LoadAdError) -> Unit
    ) {
        val adUnitId = activity.getString(R.string.Interstitial_Home_ID_High)

        Log.d(
            TAG,
            "loadHigh → called | premium=$isPro | loading=$isHighLoading | hasAd=${homeInterstitialHigh != null}"
        )

        if (isPro || homeInterstitialHigh != null || isHighLoading) {
            Log.d(TAG, "loadHigh → skipped")
            return
        }

        isHighLoading = true
        Log.d(TAG, "loadHigh → start loading")

        InterstitialAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    isHighLoading = false
                    homeInterstitialHigh = ad
                    highRetryCount = 0

                    Log.d(TAG, "loadHigh → SUCCESS")
                    firebaseUserAction("interstitial_highB_loaded", "InterstitialManager")
                    setupAdListeners(ad, adUnitId,activity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isHighLoading = false
                    highRetryCount++

                    Log.e(
                        TAG,
                        "loadHigh → FAILED | retry=$highRetryCount | error=${loadAdError.message}"
                    )

                    firebaseUserAction("interstitial_highB_failed", "InterstitialManager")

                    if (highRetryCount < MAX_HIGH_RETRIES) {
                        Log.d(TAG, "loadHigh → retrying")
//                        loadHomeInterstitialHigh(activity, onFailed)
                    } else {
                        Log.e(TAG, "loadHigh → max retries reached")
                        onFailed(loadAdError)
                    }
                }
            }
        )
    }

    // Load all interstitial
    private fun loadHomeInterstitialAll(
        activity: Activity,
    ) {
        val adUnitId = activity.getString(R.string.Interstitial_Home_ID)

        Log.d(
            TAG,
            "loadAll → called | premium=$isPro | loading=$isAllLoading | hasAd=${homeInterstitialAll != null}"
        )

        if (isPro || homeInterstitialAll != null || isAllLoading) {
            Log.d(TAG, "loadAll → skipped")
            return
        }

        isAllLoading = true
        Log.d(TAG, "loadAll → start loading")

        InterstitialAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(ad: InterstitialAd) {
                    isAllLoading = false
                    homeInterstitialAll = ad

                    Log.d(TAG, "loadAll → SUCCESS")
                    firebaseUserAction("interstitial_allB_loaded", "InterstitialManager")
                    setupAdListeners(ad, adUnitId,activity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAllLoading = false

                    Log.e(TAG, "loadAll → FAILED | error=${loadAdError.message}")
                    firebaseUserAction("interstitial_allB_failed", "InterstitialManager")
                }
            }
        )
    }

    // Show interstitial: first high, then all
    fun showInterstitialHome(
        forFragment: Boolean = false,
        activity: FragmentActivity,
        onDismissed: (() -> Unit)? = null
    ) {
        onDismissed?.invoke()
   /*     Log.d(
            TAG,
            "showInterstitial → called | premium=$isPro  | highReady=${homeInterstitialHigh != null} | allReady=${homeInterstitialAll != null}"
        )

        if (isPro) {
            Log.d(TAG, "showInterstitial → skipped")
            onDismissed?.invoke()
            return
        }
        clickCount++
        Log.d("ADs_key", "init: interstitialAdCounter:$interstitialAdCounter   clickCount:$clickCount")
        val shouldShowAd =
            // (clickCount == 1 || (clickCount - 1) % interstitialAdCounter == 0)
            (clickCount  % interstitialAdCounter == 0)
        if ( shouldShowAd && maxAdImpressions < remotemaxAdImpressions) {

            activity.lifecycleScope.launch {
                try {
                    val adToShow = homeInterstitialHigh ?: homeInterstitialAll
                    val adType = if (adToShow == homeInterstitialHigh) "high" else "all"


                    adToShow?.let { ad ->
                        showLoading(activity)
                        delay(1500)
                        Log.d(TAG, "showInterstitial → showing $adType")

                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {

                            override fun onAdShowedFullScreenContent() {
                                maxAdImpressions++
                                setinterstitialshown(true)
                                Log.d(TAG, "ad $adType → SHOWN")
                                firebaseUserAction(
                                    "interstitial_shown_$adType B",
                                    "InterstitialManager"
                                )

                                if (adType == "high") homeInterstitialHigh = null
                                else homeInterstitialAll = null
                            }

                            override fun onAdDismissedFullScreenContent() {
                                setinterstitialshown(false)
                                AdBlockerHelper.hideLoading()
                                Log.d(TAG, "ad $adType → DISMISSED")
                                firebaseUserAction(
                                    "interstitial_dismissed_$adType B",
                                    "InterstitialManager"
                                )
                                saveCurrentTime()

                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(cooldownValue * 1000)
                                    loadHomeInterstitialAd(activity)
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                setinterstitialshown(false)

                                Log.e(TAG, "ad $adType → FAILED TO SHOW | ${adError.message}")
                                firebaseUserAction(
                                    "interstitial_failed_to_show_$adType B",
                                    "InterstitialManager"
                                )

                                loadHomeInterstitialAd(activity)
                            }
                        }

                        if (forFragment) {
                            ad.show(activity)
                          //  delay(100)

                            delay(400)
                            onDismissed?.invoke()
                        } else {
                            onDismissed?.invoke()
                            delay(50)
                            ad.show(activity)
                        }
                        Log.w(TAG, "showInterstitial → for activity")



                    } ?: run {
                        Log.w(TAG, "showInterstitial → no ad available, forcing reload x2")
                        loadHomeInterstitialAd(activity)
                        loadHomeInterstitialAd(activity)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error showing interstitial ad", e)
                    onDismissed?.invoke()
                } finally {
                    if (forFragment){
                        AdBlockerHelper.hideLoading()
                    }
                }
            }
        }else{
            onDismissed?.invoke()
        }*/
    }

    // Helper method to reload ads after shown or failed
    fun loadHomeInterstitialAd(activity: Activity) {
        if (maxAdImpressions < remotemaxAdImpressions) {
            Log.d(TAG, "loadInterstitialAd → triggered")
         /*   loadHomeInterstitialHigh(activity) { }
            loadHomeInterstitialAll(activity)*/
        }
    }

    // Revenue listener
    private fun setupAdListeners(ad: InterstitialAd, adUnitId: String,activity: Activity?=null) {
        ad.onPaidEventListener = OnPaidEventListener { adValue ->
            val revenue = adValue.valueMicros / 1_000_000.0

            Log.d(
                TAG,
                "onPaidEvent → unit=$adUnitId | revenue=$revenue ${adValue.currencyCode}"
            )

            val data = SingularAdData(
                "AdMob",
                adValue.currencyCode,
                revenue
            )
            Singular.adRevenue(data)

            trackAdjustAdRevenue(
                adUnitId = adUnitId,
                revenue = revenue,
                currency = adValue.currencyCode,
                token = AppConstant.AD_IMPRESSION_TOKEN,
                appContext = activity as Context
            )
        }
    }


}