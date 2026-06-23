package com.video.avd.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.avd.util.AdBlockerHelper
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.video.avd.BuildConfig
import com.video.avd.MyApplication
import com.video.avd.constent.isSplash
import com.video.avd.ui.onbooard.OnboardingActivity
import com.video.avd.ui.onbooard.SurveyActivity
import com.video.avd.ui.player.callback.PauseVideoCallBack
import com.video.avd.ui.splash.SplashActivity
import com.video.avd.ui.splash_flow.activities.InAppActivity
import com.video.avd.utils.GlobalValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class AppOpenManager(private val myApplication: MyApplication) : LifecycleObserver,
    Application.ActivityLifecycleCallbacks {
    var appOpenAd: AppOpenAd? = null
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var loadCallback: AppOpenAd.AppOpenAdLoadCallback? = null

    init {
        Log.e("AppOpenAd", "=== AppOpenManager INIT (instance created) ===")
        Log.e("AppOpenAd", "AD_UNIT_ID: ${if (AD_UNIT_ID != null) AD_UNIT_ID else "NULL"}")
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.Companion.get().lifecycle.addObserver(this)
        Log.e("AppOpenAd", "Lifecycle callbacks registered - ON_RESUME will trigger on app resume")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onStart() {
        Log.e("AppOpenAd", "=== onStart() APP RESUMED ===")
        Log.e("AppOpenAd", "Current Activity: ${currentActivity?.javaClass?.simpleName}")
        Log.e("AppOpenAd", "isShowingAd: $isShowingAd | isAdAvailable: $isAdAvailable | isPro: ${AdBlockerHelper.isProVersion.value} | isSplash: ${isSplash} | interstitialShown: ${AdBlockerHelper.getinterstitialshown()} | is24hour: ${GlobalValues.is24hourEnabled.value}")
        Log.e("AppOpenAd", "appOpenAd: ${if (appOpenAd != null) "NOT NULL" else "NULL"} | loadTime: $loadTime | adShowCounter: ${AdsManager.adShowCounter}")
        if(currentActivity !is AdActivity && !isShowingAd && currentActivity !is SplashActivity && currentActivity !is SurveyActivity && currentActivity !is InAppActivity && currentActivity !is OnboardingActivity) {
            showAdIfAvailable()
        }
    }

    fun showAdIfAvailable() {
        Log.e("AppOpenAd", "=== showAdIfAvailable() called ===")
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        val condition1 = !isShowingAd
        val condition2 = isAdAvailable
        val condition3 = AdBlockerHelper.isProVersion.value != true
        val condition4 = !isSplash
        val condition5 = !AdBlockerHelper.getinterstitialshown()

        Log.e("AppOpenAd", "Conditions: !isShowingAd=$condition1 | isAdAvailable=$condition2 | !isPro=$condition3 | !isSplash=$condition4 | !interstitialShown=$condition5")

        if (!isShowingAd && isAdAvailable && AdBlockerHelper.isProVersion.value != true && !isSplash && !AdBlockerHelper.getinterstitialshown()) {
            Log.e("AppOpenAd", "All conditions met! Checking 24-hour status...")
            if (GlobalValues.is24hourEnabled.value == false) {
                Log.e("AppOpenAd", "24-hour ad free is disabled. Preparing to show ad...")
                val fullScreenContentCallback: FullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.e("AppOpenAd", "onAdDismissedFullScreenContent() called")
                            AdsManager.hideAppOpenWelcomeDialog()
                            // Set the reference to null so isAdAvailable() returns false.
                            appOpenAd = null
                            isShowingAd = false
                            videoCallback?.pauseVideo(false)
//                          MainActivity.Companion.setShowing(false);
//                          if (MainActivity.Companion.isShowing()) {
                            fetchAd()
//                                                        }
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e("AppOpenAd", "onAdFailedToShowFullScreenContent() - Error: ${adError.code}, Message: ${adError.message}")
                            AdsManager.hideAppOpenWelcomeDialog()
                            isShowingAd = false
                        }
                        override fun onAdShowedFullScreenContent() {
                            Log.e("AppOpenAd", "onAdShowedFullScreenContent() called - Ad is now showing")
                            isShowingAd = true
                            videoCallback?.pauseVideo(true)
                        }
                    }
                appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
                CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    Log.e("AppOpenAd", "After 500ms delay, checking currentActivity...")
//                sadaqat want to commit this
                    currentActivity?.let {
                        Log.e("AppOpenAd", "Current activity available: ${it.javaClass.simpleName}")
                        if (AdsManager.adShowCounter < 1) {
                            Log.e("AppOpenAd", "adShowCounter < 1, showing ad now...")
                            AdsManager.adShowCounter++
                            AdsManager.showAppOpenWelcomeDialog(it)
                            delay(1000)
                            appOpenAd?.show(it)
                            Log.e("AppOpenAd", "appOpenAd.show() called. adShowCounter: ${AdsManager.adShowCounter}")
                            isShowingAd = false
                        } else {
                            Log.e("AppOpenAd", "adShowCounter >= 1 (${AdsManager.adShowCounter}), resetting counter and NOT showing ad")
                            AdsManager.adShowCounter = 0
                        }
                    } ?: run {
                        Log.e("AppOpenAd", "ERROR: currentActivity is NULL! Cannot show ad.")
                    }
                }
            } else {
                Log.e("AppOpenAd", "24-hour ad free is ENABLED. Skipping ad show, fetching new ad instead.")
                fetchAd()
            }
        } else {
            Log.e("AppOpenAd", "NOT showing ad. Reason: isShowingAd=$isShowingAd | isAdAvailable=$isAdAvailable | isPro=${AdBlockerHelper.isProVersion.value} | isSplash=${isSplash} | interstitialShown=${AdBlockerHelper.getinterstitialshown()}")
            fetchAd()
        }
    }

    /**
     * Request an ad
     */
    fun fetchAd() {
        Log.e("AppOpenAd", "=== fetchAd() called ===")
        // We will implement this below.
//        if (MainActivity.Companion.isShowing()) {
//            MainActivity.Companion.setShowing(false);
//        } else {
//            return;
//        }
        if (isAdAvailable) {
            Log.e("AppOpenAd", "Ad already available, skipping fetch")
            /*if (currentActivity is SplashActivity) {
                openAdLoaded.value = 1
            }*/
            return
        }
        Log.e("AppOpenAd", "Ad not available, starting load...")
        loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
                Log.e("AppOpenAd", "=== onAdLoaded() SUCCESS ===")
                appOpenAd = ad
                loadTime = Date().time
                Log.e("AppOpenAd", "Ad loaded successfully. Load time: $loadTime")
                Log.e("AppOpenAd", "Ad will be available for 4 hours")
            }


            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                // Handle the error.
                Log.e("AppOpenAd", "=== onAdFailedToLoad() ERROR ===")
                Log.e("AppOpenAd", "Error Code: ${loadAdError.code}")
                Log.e("AppOpenAd", "Error Domain: ${loadAdError.domain}")
                Log.e("AppOpenAd", "Error Message: ${loadAdError.message}")
                Log.e("AppOpenAd", "Response Info: ${loadAdError.responseInfo}")
            }

        }
        val request = adRequest
        Log.e("AppOpenAd", "Load check: isPro=${AdBlockerHelper.isProVersion.value} | is24hour=${GlobalValues.is24hourEnabled.value} | AD_UNIT_ID=${if (AD_UNIT_ID != null) "set" else "NULL"}")

        if (AdBlockerHelper.isProVersion.value != true) {
            if (GlobalValues.is24hourEnabled.value == false) {
                if (AD_UNIT_ID != null) {
                    loadCallback?.let {
                        Log.e("AppOpenAd", "Starting AppOpenAd.load()...")
                        CoroutineScope(Dispatchers.Main).launch {
                            AppOpenAd.load(
                                myApplication,
                                AD_UNIT_ID,
                                request,
                                it
                            )
                        }
                    } ?: run {
                        Log.e("AppOpenAd", "ERROR: loadCallback is NULL, cannot load ad")
                    }
                } else {
                    Log.e("AppOpenAd", "ERROR: AD_UNIT_ID is NULL, cannot load ad")
                }
            } else {
                Log.e("AppOpenAd", "24-hour ad free is enabled, skipping ad load")
            }
        } else {
            Log.e("AppOpenAd", "Pro version is enabled, skipping ad load")
        }
    }

    /**
     * Creates and returns ad request.
     */
    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    /**
     * Utility method to check if ad was loaded more than n hours ago.
     */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    private val isAdAvailable: Boolean
        get() {
            val hasAd = appOpenAd != null
            val isTimeValid = wasLoadTimeLessThanNHoursAgo(4)
            val result = hasAd && isTimeValid
            if (!result) Log.e("AppOpenAd", "isAdAvailable=false (hasAd=$hasAd, isTimeValid=$isTimeValid)")
            if (!isTimeValid && hasAd) {
                val hoursSinceLoad = (Date().time - loadTime) / (3600000)
                Log.e("AppOpenAd", "Ad expired: loaded $hoursSinceLoad hours ago (4h limit)")
            }
            return result
        }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }
    override fun onActivityStarted(activity: Activity) {
        Log.e("AppOpenAd", "onActivityStarted: ${activity.javaClass.simpleName}")
        currentActivity = activity
//        isShowingAd= AdBlockerHelper.isDownloading
        SplashActivity.Companion.currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        Log.e("AppOpenAd", "onActivityResumed: ${activity.javaClass.simpleName}")
        currentActivity = activity
//        isShowingAd= AdBlockerHelper.isDownloading
        SplashActivity.Companion.currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
//        isShowingAd= AdBlockerHelper.isDownloading

    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
            Log.e("AppOpenAd", "onActivityDestroyed: ${activity.javaClass.simpleName} (current cleared)")
        }
    }

    companion object {
        private val AD_UNIT_ID = BuildConfig.app_open_resume
        var isShowingAd = false
        var videoCallback: PauseVideoCallBack? = null
        fun pauseVideoCallback(callback: PauseVideoCallBack) {
            videoCallback = callback
        }
    }
}