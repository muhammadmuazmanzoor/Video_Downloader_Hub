//package com.xilliapps.hdvideoplayer.ads
//
//import android.app.Activity
//import android.os.Bundle
//import android.util.Log
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleObserver
//import androidx.lifecycle.OnLifecycleEvent
//import androidx.lifecycle.ProcessLifecycleOwner
//import com.applovin.mediation.MaxAd
//import com.applovin.mediation.MaxAdListener
//import com.applovin.mediation.MaxError
//import com.applovin.mediation.ads.MaxAppOpenAd
//import com.xilliapps.hdvideoplayer.MyApplication
//import com.xilliapps.hdvideoplayer.MyApplication.Companion.context
//import com.xilliapps.hdvideoplayer.R
//import com.xilliapps.hdvideoplayer.ads.AdsManager.appOpenAdRemote
//import com.xilliapps.hdvideoplayer.constent.isSplash
//import com.xilliapps.hdvideoplayer.ui.basefragment.BaseVideoFragment.Companion.isRationaleDialogShown
//import com.xilliapps.hdvideoplayer.ui.player.callback.PauseVideoCallBack
//import com.xilliapps.hdvideoplayer.utils.AppOpenManager
//import com.xilliapps.hdvideoplayer.utils.GlobalValues
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.util.*
//
//class AppOpenManagerAppLovin(private val myApplication: MyApplication) : LifecycleObserver,
//    android.app.Application.ActivityLifecycleCallbacks {
//
//    private var appOpenAd: MaxAppOpenAd? = null
//    private var loadTime: Long = 0
//    private var currentActivity: Activity? = null
//    private var isAdLoading = false
//
//    init {
//        myApplication.registerActivityLifecycleCallbacks(this)
//        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//        if(GlobalValues.AdBlockerHelper.isProVersion.value==false && GlobalValues.is24hourEnabled.value==false && appOpenAdRemote) {
//                initializeAppOpenAd()
//        }
//    }
//
//    /**
//     * LifecycleObserver methods
//     */
//    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//    fun onStart() {
//        if(appOpenAdRemote) {
//            showAdIfAvailable()
//        }
//    }
//
//    /**
//     * Initialize AppLovin App Open Ad
//     */
//    private fun initializeAppOpenAd() {
//       /* val adUnitId = context?.getString(R.string.App_Open_Ad_AppLovin) // Replace with your AppLovin Ad Unit ID
//        appOpenAd = adUnitId?.let { MaxAppOpenAd(it, myApplication) }
//        context?.let {
//            appOpenAd?.setRevenueListener(RevenueManager.getInstance(it))
//        }
//        appOpenAd?.setListener(object : MaxAdListener {
//            override fun onAdLoaded(ad: MaxAd) {
//                Log.d("AppOpenManager", "App Open Ad Loaded")
//                loadTime = Date().time
//                isAdLoading = false
//            }
//
//            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//                Log.e("AppOpenManager", "Failed to load App Open Ad: ${error.message}")
//                isAdLoading = false
//            }
//
//            override fun onAdDisplayed(ad: MaxAd) {
//                Log.d("AppOpenManager", "App Open Ad Displayed")
//                AppOpenManager.isShowingAd = true
//                videoCallback?.pauseVideo(true)
//            }
//
//            override fun onAdHidden(ad: MaxAd) {
//                Log.d("AppOpenManager", "App Open Ad Hidden")
//                AppOpenManager.isShowingAd = false
//                videoCallback?.pauseVideo(false)
//                fetchAd() // Reload the ad after it's shown
//            }
//
//            override fun onAdClicked(ad: MaxAd) {
//                Log.d("AppOpenManager", "App Open Ad Clicked")
//            }
//
//            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
//                Log.e("AppOpenManager", "Failed to display App Open Ad: ${error.message}")
//                AppOpenManager.isShowingAd = false
//                fetchAd() // Retry loading the ad
//            }
//        })*/
//    }
//
//    /**
//     * Show App Open Ad if available
//     */
//    fun showAdIfAvailable() {
//        Log.e("checkIsShowing","isShowingAd: ${AppOpenManager.isShowingAd }")
//        if (!AppOpenManager.isShowingAd  && isAdAvailable() && !isAdLoading && !isSplash && GlobalValues.AdBlockerHelper.isProVersion.value==false && GlobalValues.is24hourEnabled.value==false) {
//            currentActivity?.let { activity ->
//                appOpenAd?.showAd()
//            }
//        } else {
//            if(GlobalValues.AdBlockerHelper.isProVersion.value==false && GlobalValues.is24hourEnabled.value==false){
//                fetchAd()
//            }
//        }
//    }
//
//    /**
//     * Fetch a new ad if none is available
//     */
//    fun fetchAd() {
//
//        if (isAdAvailable() || isAdLoading) return
//
//        isAdLoading = true
//        CoroutineScope(Dispatchers.Main).launch {
//            appOpenAd?.loadAd()
//        }
//    }
//
//    /**
//     * Check if the ad is still valid (less than 4 hours old)
//     */
//    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
//        val dateDifference = Date().time - loadTime
//        val numMilliSecondsPerHour: Long = 3600000
//        return dateDifference < numMilliSecondsPerHour * numHours
//    }
//
//    /**
//     * Check if ad is available to show
//     */
//    private fun isAdAvailable(): Boolean {
//        return appOpenAd?.isReady == true && wasLoadTimeLessThanNHoursAgo(4)
//    }
//
//    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
//    override fun onActivityStarted(activity: Activity) {
//        currentActivity = activity
//    }
//
//    override fun onActivityResumed(activity: Activity) {
//        currentActivity = activity
//    }
//
//    override fun onActivityPaused(activity: Activity) {}
//    override fun onActivityStopped(activity: Activity) {}
//    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
//    override fun onActivityDestroyed(activity: Activity) {
//        if (currentActivity == activity) {
//            currentActivity = null
//        }
//    }
//
//    companion object {
////        var isShowingAd = false
//        var videoCallback: PauseVideoCallBack? = null
//        fun pauseVideoCallback(callback: PauseVideoCallBack) {
//            videoCallback = callback
//        }
//    }
//}
