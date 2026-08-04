package com.avd.util.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.R
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.cooldownValue
import com.avd.util.AdBlockerHelper.firebaseUserAction
import com.avd.util.AdBlockerHelper.isCooldownOver
import com.avd.util.AdBlockerHelper.isPro
import com.avd.util.AdBlockerHelper.maxAdImpressions
import com.avd.util.AdBlockerHelper.remotemaxAdImpressions
import com.avd.util.AdBlockerHelper.saveCurrentTime
import com.avd.util.AdBlockerHelper.setinterstitialshown
import com.avd.util.AdBlockerHelper.showLoading
import com.avd.util.AdBlockerHelper.trackAdjustAdRevenue
import com.avd.util.AppConstant
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object InterstitialManagerA {
    private const val TAG = "InterstitialManagerA"
    private var isHighLoading = false
    private var isAllLoading = false
    var homeInterstitialHigh: InterstitialAd? = null
    var homeInterstitialAll: InterstitialAd? = null
    private var highRetryCount = 0
    private const val MAX_HIGH_RETRIES = 2
    var interstitialAdCounter = 3
    private var clickCount = 0

    private fun loadHomeInterstitialHigh(
        activity: Activity,
        onFailed: (error: LoadAdError) -> Unit
    ) {
        val adUnitId = activity.getString(R.string.Interstitial_Home_ID_High)
        if (isPro || homeInterstitialHigh != null || isHighLoading) return

        isHighLoading = true
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isHighLoading = false
                    homeInterstitialHigh = ad
                    highRetryCount = 0
                    firebaseUserAction("interstitial_highB_loaded", "InterstitialManager")
                    setupAdListeners(ad, adUnitId, activity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isHighLoading = false
                    highRetryCount++
                    firebaseUserAction("interstitial_highB_failed", "InterstitialManager")
                    if (highRetryCount >= MAX_HIGH_RETRIES) {
                        onFailed(loadAdError)
                    }
                }
            }
        )
    }

    private fun loadHomeInterstitialAll(activity: Activity) {
        val adUnitId = activity.getString(R.string.Interstitial_Home_ID)
        if (isPro || homeInterstitialAll != null || isAllLoading) return

        isAllLoading = true
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isAllLoading = false
                    homeInterstitialAll = ad
                    firebaseUserAction("interstitial_allB_loaded", "InterstitialManager")
                    setupAdListeners(ad, adUnitId, activity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAllLoading = false
                    firebaseUserAction("interstitial_allB_failed", "InterstitialManager")
                }
            }
        )
    }

    fun showInterstitialHome(
        forFragment: Boolean = false,
        activity: FragmentActivity,
        onDismissed: (() -> Unit)? = null
    ) {
        Log.d(
            TAG,
            "showInterstitialHome called premium=$isPro highReady=${homeInterstitialHigh != null} allReady=${homeInterstitialAll != null}"
        )

        if (isPro || !isCooldownOver()) {
            onDismissed?.invoke()
            return
        }

        clickCount++
        val shouldShowAd = clickCount % interstitialAdCounter == 0
        if (!shouldShowAd || maxAdImpressions >= remotemaxAdImpressions) {
            onDismissed?.invoke()
            return
        }

        activity.lifecycleScope.launch {
            var callbackCalled = false
            fun continueOnce() {
                if (!callbackCalled) {
                    callbackCalled = true
                    onDismissed?.invoke()
                }
            }

            try {
                val adToShow = homeInterstitialHigh ?: homeInterstitialAll
                val isHigh = adToShow === homeInterstitialHigh

                if (adToShow == null) {
                    loadHomeInterstitialAd(activity)
                    continueOnce()
                    return@launch
                }

                showLoading(activity)
                delay(1000)

                adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        maxAdImpressions++
                        setinterstitialshown(true)
                        if (isHigh) homeInterstitialHigh = null else homeInterstitialAll = null
                    }

                    override fun onAdDismissedFullScreenContent() {
                        setinterstitialshown(false)
                        AdBlockerHelper.hideLoading()
                        saveCurrentTime()
                        continueOnce()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(cooldownValue * 1000)
                            loadHomeInterstitialAd(activity)
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Failed to show interstitial: ${adError.message}")
                        setinterstitialshown(false)
                        AdBlockerHelper.hideLoading()
                        if (isHigh) homeInterstitialHigh = null else homeInterstitialAll = null
                        continueOnce()
                        loadHomeInterstitialAd(activity)
                    }
                }

                adToShow.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing interstitial ad", e)
                AdBlockerHelper.hideLoading()
                onDismissed?.invoke()
            }
        }
    }

    fun loadHomeInterstitialAd(activity: Activity) {
        if (maxAdImpressions < remotemaxAdImpressions) {
            loadHomeInterstitialHigh(activity) { }
            loadHomeInterstitialAll(activity)
        }
    }

    private fun setupAdListeners(ad: InterstitialAd, adUnitId: String, activity: Activity? = null) {
        ad.onPaidEventListener = OnPaidEventListener { adValue ->
            val revenue = adValue.valueMicros / 1_000_000.0
            val data = SingularAdData("AdMob", adValue.currencyCode, revenue)
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
