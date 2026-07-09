package com.avd.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebResourceResponse
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent
import com.applovin.mediation.MaxAd
import com.avd.BuildConfig
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.avd.R
import com.avd.data.local.model.LocalVideo
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream


object AdBlockerHelper {

    const val TAG = "loading_dialog"
    var cooldownValue: Long = 20L
    private lateinit var sharedPref: SharedPreferences
    private const val LAST_TIME_KEY = "ad_last_saved_time"
    var showExitScreen: (() -> Unit)? = null
    var resetAppOpenShownAd: (() -> Unit)? = null
    var adChoice: String? = ""
    var isPro: Boolean = false
    var isProVersion = MutableLiveData(false)
    var interstitalShown: Boolean = false
    var maxAdImpressions: Int = 0
    var remotePopupAdImpressions: Int = 3
    var maxPopupAdImpressions: Int = 0
    var downloaderShown: Boolean = false
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var loadingDialog: Dialog? = null
    var remotemaxAdImpressions: Int = 6
    var proCrossTimer: Int = 3
    var exitTimer: Int = 3
    var fromBrowser=false
    var isAdShowing=false
    var isDownloading=false
    var is24hour: Boolean? = false
    var localeLangauge: String? = "en"

    var nativeAdNow: NativeAd? = null
    var nativeAd: MaxAd? = null
    var home_native = true
    var native_home_variation = 2L
    var browser_native = true
    var rewardedDownload: RewardedAd? = null
    var isLoading: Boolean = false
    var recycler_native = true
    var exit_native = true
    var inter_home_high = true
    var inter_home = true
    var inter_browser = true
    var inter_videos = true
    var inter_home_normal = true
    var banner_home_enable = true
    var banner_home = true
    var interHome: InterstitialAd? = null
    var cachedVideosList: ObservableField<MutableList<LocalVideo>> = ObservableField(mutableListOf())
    var fromVideo:Boolean=false

    var isinter = false
    var isIapEnableAfterSplash: Boolean=true

    private val adShownMap = mutableMapOf<ScreenName, Boolean>()

    fun isAdShown(screenName: ScreenName): Boolean {
        return adShownMap[screenName] ?: false
    }

    fun setAdShown(screenName: ScreenName, shown: Boolean) {
        adShownMap[screenName] = shown
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }

    fun loadRewardedAd(context: Context,highId:String="ca-app-pub-5972202469838280/3070132069",lowId:String="ca-app-pub-5972202469838280/7009377071",highRequest:Boolean=true,lowRequest:Boolean=false) {
       /* if (isProVersion.value != true) {
            val requestId=if(highRequest) highId else lowId
            if (!isLoading && rewardedDownload == null) {
                isLoading = true
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context,
                    if(BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else requestId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            // Handle the error.
                            rewardedDownload = null
                            isLoading = false
                            if(lowRequest==false) {
                                loadRewardedAd(context, highId, lowId, false, true)
                            }
                        }

                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            rewardedDownload = rewardedAd
                            isLoading = false
                            rewardedAd.setOnPaidEventListener { adValue ->
                                trackAdjustAdRevenue(
                                    adUnitId = requestId,
                                    revenue = adValue.valueMicros / 1_000_000.0,
                                    currency = adValue.currencyCode,
                                    token = AppConstant.AD_IMPRESSION_TOKEN,
                                    appContext = context
                                )
                            }
                        }
                    })
            }
        } else {
            rewardedDownload = null
        }*/
    }


    fun showRewarded(
        context: Context,
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        onDismissed: (() -> Unit)? = null,
    ) {
        // Show loader immediately
        showLoading(activity, "Loading Rewarded Ad...")
        val scope = CoroutineScope(Dispatchers.Main)
        var elapsedSeconds = 0
        val maxWaitSeconds = 5
        scope.launch {
            // Check every 1 sec up to 5 sec
            while (elapsedSeconds < maxWaitSeconds) {
                delay(1_000L)
                elapsedSeconds++
                Log.d("RewardedAd", "Checking ad... attempt $elapsedSeconds")

                if (rewardedDownload != null) {
                    // Ad loaded — hide loader and show ad
                    hideLoading()
                    displayAd(activity, onUserEarnedRewardListener, onDismissed, context)
                    return@launch
                }
            }

            // 5 sec passed and ad still not loaded
            hideLoading()
            Toast.makeText(activity, "Ad not loaded yet! try again", Toast.LENGTH_SHORT).show()
            loadRewardedAd(context)
        }
    }

    private fun displayAd(
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        onDismissed: (() -> Unit)? = null,
        context: Context
    ) {
        rewardedDownload?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isAdShowing = true
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedDownload = null
                loadRewardedAd(context)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedDownload = null
                isAdShowing = false
                onDismissed?.invoke()
                loadRewardedAd(context)
            }
        }
        rewardedDownload?.show(activity, onUserEarnedRewardListener)
    }
    fun firebaseUserAction(action: String, activityName: String) {
       /* CoroutineScope(Dispatchers.IO).launch {
            Singular.event(action)
            ContextUtils.getApplicationContext()?.let {
                try {
                    if (FirebaseApp.getApps(it).isEmpty()) {
                        FirebaseApp.initializeApp(it)
                    } else {
                        if (firebaseAnalytics == null) {
                            firebaseAnalytics = Firebase.analytics
                        }
                        firebaseAnalytics?.let { analytics ->
                            analytics.logEvent(action) {
                                param("Screen_Name", activityName)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }*/
    }

    fun onInterstitialImpressionSuccess(context: Context) {
       /* CoroutineScope(Dispatchers.IO).launch {
            context?.let {
                try {
                    if (FirebaseApp.getApps(it).isEmpty()) {
                        FirebaseApp.initializeApp(it)
                    } else {
                        if (firebaseAnalytics == null) {
                            firebaseAnalytics = Firebase.analytics
                        }
                        firebaseAnalytics?.let { analytics ->
                            analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION) {
                                param(
                                    FirebaseAnalytics.Param.AD_UNIT_NAME,
                                    "Interstitial ${maxAdImpressions}"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }*/
    }

    /**
     * Helper function to safely check if an activity is valid for showing dialogs
     * @param activity The activity to check
     * @return true if the activity can safely show a dialog, false otherwise
     */
    private fun isActivityValidForDialog(activity: Activity?): Boolean {
        if (activity == null) return false
        return try {
            !activity.isFinishing && !activity.isDestroyed
        } catch (e: Exception) {
            Log.e(TAG, "Error checking activity state", e)
            false
        }
    }

    fun showLoading(activity: Activity, message: String = "Loading Ad...") {
        // Always dismiss any existing dialog first (to handle case when new activity is passed)
        hideLoading()
        // Check if the new activity is valid for showing dialogs
        if (!isActivityValidForDialog(activity)) {
            Log.d(TAG, "Activity is not valid for showing dialog, skipping")
            return
        }
        // Always create a new Dialog instance for the new activity (don't reuse old dialog)
        loadingDialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.ad_dialog_new)

            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            findViewById<TextView>(R.id.loadingText)?.text = message

            setCanceledOnTouchOutside(false)
            setCancelable(false)

            setOnDismissListener {
                loadingDialog = null
            }
        }

        // Safe show with try-catch
        try {
            if (isActivityValidForDialog(activity)) {
                loadingDialog?.show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading dialog", e)
            loadingDialog = null
        }
    }

    fun hideLoading() {
        try {
            val dialog = loadingDialog
            if (dialog != null) {
                // Check if dialog's context (activity) is still valid before dismissing
                val context = dialog.context
                if (context is Activity) {
                    if (!isActivityValidForDialog(context)) {
                        // Activity is finishing/destroyed, just nullify the dialog reference
                        dialog.dismiss()
                        loadingDialog = null
                        return
                    }
                }

                try {
                    dialog.dismiss()
                    loadingDialog = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading dialog", e)
            loadingDialog = null
        }
    }

    /*fun showLoading(context: Activity, message: String = "Loading Ad...") {
        if (loadingDialog?.isShowing == true) return

        loadingDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.ad_dialog_new)
            window?.apply {
                // Make background fully transparent
                setBackgroundDrawable(ColorDrawable(Color.WHITE))

                // Remove all margins → truly fullscreen
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }


            // Set loading text
            findViewById<TextView>(R.id.loadingText)?.text = message

            // Dialog properties
            setCanceledOnTouchOutside(false)
            setCancelable(false)

            setOnDismissListener { loadingDialog = null }
        }

        try {
            loadingDialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
                loadingDialog = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

    fun parseAdsLine(line: String?): String {
        val a = line.toString().replace("^$\\third-party", "")
            .replace("0.0.0.0", "")
            .replace(":::::", "")
            .replace(":", "")
            .replace("127.0.0.1", "")
            .replace("255.255.255.255", "")
            .replace("localhost", "")
            .trim()
            .lowercase().replace(Regex(" \\.{1,2} "), "")
        a.replace("www.", "").replace(".m", "").trim()
            .lowercase()
            .trim()
            .let {
                if (it.startsWith(".") || it.startsWith("ip6-")) {
                    return ""
                }
                return it
            }
    }
    fun loadFallbackInterstitialAd(
        activity: Activity,
        highFloorAdId: String,
        normalAdId: String,
        loadHighFloor: Boolean = true,
        loadNormalFloor: Boolean = true,
        onAdLoadedHigh: (interstitialAd: InterstitialAd) -> Unit,
        onAdLoadedNormal: (interstitialAd: InterstitialAd) -> Unit,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (isProVersion.value == true) {
            return
        }
        if(interHome!=null){
            return
        }
        Log.d("checkAd","loadFallbackInterstitialAd")
        if (loadHighFloor) {
            loadInterstitialAd(
                activity, highFloorAdId,
                onLoaded = { ad ->
                    onAdLoadedHigh(ad)
                },
                onFailed = { error ->
                    loadInterstitialAd(
                        activity, normalAdId,
                        onLoaded = { ad ->
                            onAdLoadedNormal(ad)
                        },
                        onFailed = { error ->
                            onAdFailed?.invoke()
                        })
                })
        } else if (loadNormalFloor) {
            loadInterstitialAd(
                activity, normalAdId,
                onLoaded = { ad ->
                    onAdLoadedNormal(ad)
                },
                onFailed = { error ->
                    onAdFailed?.invoke()
                })
        }
    }


    private fun loadInterstitialAd(
        activity: Activity,
        adUnitId: String,
        onLoaded: (interstitialAd: InterstitialAd) -> Unit,
        onFailed: (error: LoadAdError) -> Unit
    ) {
        if (isProVersion.value == true) {
            return
        }
        if(interHome!=null){
            return
        }
        Log.d("checkAd","request InterstitialAd")
        InterstitialAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onLoaded(ad)
                    Log.d("checkAd","onAdLoaded")
                    ad.onPaidEventListener = OnPaidEventListener { adValue ->
                        trackAdjustAdRevenue(
                            adUnitId = adUnitId,
                            revenue = adValue.valueMicros / 1_000_000.0,
                            currency = adValue.currencyCode,
                            token = AppConstant.AD_IMPRESSION_TOKEN,
                            appContext = activity
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onFailed(loadAdError)
                    Log.e("checkAd","onAdLoadFailed")
                }
            })
    }

    // Call this once during app startup, for example in Application class
    fun initialize(context: Context) {
        try {
            sharedPref = context.getSharedPreferences("ad_manager_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun saveCurrentTime() {
        try {
            val currentTime = System.currentTimeMillis()
            sharedPref.edit().putLong(LAST_TIME_KEY, currentTime).apply()
//            Log.e("checkCoolDown","saveCurrentTime: $currentTime")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isCooldownOver(): Boolean {
        try {
            val lastSavedTime = sharedPref.getLong(LAST_TIME_KEY, 0L)
            val currentTimeMillis = System.currentTimeMillis()
            val elapsedMillis = currentTimeMillis - lastSavedTime
            return elapsedMillis >= (cooldownValue * 1000L)
        } catch (e: Exception) {
            Log.e("checkCoolDown", "exception: False")
            return false
        }
    }
    fun showInterstitial(
        forFragment: Boolean = false,
        interstitialAd: InterstitialAd,
        activity: FragmentActivity,
        onDismissed: (() -> Unit)? = null,
        enable: Boolean = true
    ) {
        if(!enable) {
            onDismissed?.invoke()
            return
        }
        if(isProVersion.value==true){
            onDismissed?.invoke()
            return
        }
        if (isCooldownOver()) {
            activity.lifecycleScope.launch {
                try {
                    isAdShowing=true
                    showLoading(activity,"Loading Ad...")
//                com.video.avd.ads.AdsHelper.showLoading(activity)
                    delay(1000)
                    interstitialAd.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
//                            Log.d(com.video.avd.ads.AdsHelper.TAG, "show Interstitial → SHOW ✔")
//                            isShowingAd = true
                            }

                            override fun onAdDismissedFullScreenContent() {
//                            Log.d(com.video.avd.ads.AdsHelper.TAG, "show Interstitial → DISMISSED ✖")
//                            isShowingAd = false
//                            nullifyUsedAd(interstitialAd)
                                interHome=null
                                saveCurrentTime()
                                loadFallbackInterstitialAd(activity, activity.resources.getString(R.string.Interstitial_Home_ID_High), activity.resources.getString(R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                                    interHome=it
                                },{
                                    interHome=it
                                })
                                if (!forFragment) {
                                    hideLoading()
                                }
                                else{
                                    showLoading(activity,"")
                                    hideLoading()
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                            Log.e(com.video.avd.ads.AdsHelper.TAG, "show Interstitial SHOW FAILED ❌: ${adError.message}")
//                            isShowingAd = false
                                interHome=null
                                loadFallbackInterstitialAd(activity, activity.resources.getString(R.string.Interstitial_Home_ID_High), activity.resources.getString(R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                                    interHome=it
                                },{
                                    interHome=it
                                })
                                showLoading(activity,"")
                                hideLoading()

                            }
                        }

                    activity.lifecycleScope.launch {
                        if (forFragment) {
                            interstitialAd.show(activity)
                            delay(400)
                            hideLoading()
                            showLoading(activity,"")
                            hideLoading()
                            onDismissed?.invoke()
                            isAdShowing=false
                        } else {
                            onDismissed?.invoke()
                            hideLoading()
                            showLoading(activity,"")
                            hideLoading()
                            interstitialAd.show(activity)
                            isAdShowing=false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdsManager", "Error showing interstitial ad", e)
                    onDismissed?.invoke()
                    hideLoading()
                } finally {
                }
            }
            try {
                if (forFragment) {
                    activity.onBackPressedDispatcher.addCallback(activity) {
                        if (isAdShowing) {
                            // Block back press
                            isEnabled = false
                        } else {
                            isEnabled = true
                        }
                    }
                } else {
                    // For Activity
                    activity.onBackPressedDispatcher.addCallback(activity) {
                        if (isAdShowing) {
                            // Block back press while ad is showing
                            // Optionally show a toast
                            isEnabled = false
                            Toast.makeText(activity, "Please wait for the ad to finish", Toast.LENGTH_SHORT).show()
                        } else {
                            isEnabled = true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        else{
            hideLoading()
            onDismissed?.invoke()
        }

    }
    fun showInterstitialPlayer(
        forFragment: Boolean = false,
        interstitialAd: InterstitialAd,
        activity: FragmentActivity,
        onDismissed: (() -> Unit)? = null,
        enable: Boolean = true,
        runBackAfterLoading: Boolean = false
    ) {
        if (!enable) {
            onDismissed?.invoke()
            return
        }

        if (isProVersion.value == true) {
            onDismissed?.invoke()
            return
        }

        if (!isCooldownOver()) {
            onDismissed?.invoke()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
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
                isAdShowing = true
                showLoading(activity, "Loading Ad...")

                delay(1000L)

                if (activity.isFinishing || activity.isDestroyed) {
                    hideLoading()
                    isAdShowing = false
                    continueOnce()
                    return@launch
                }

                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {

                    override fun onAdShowedFullScreenContent() {
                        isAdShowing = true
                    }

                    override fun onAdDismissedFullScreenContent() {
                        interHome = null
                        saveCurrentTime()
                        hideLoading()
                        isAdShowing = false

                        loadFallbackInterstitialAd(
                            activity,
                            activity.resources.getString(R.string.Interstitial_Home_ID_High),
                            activity.resources.getString(R.string.Interstitial_Home_ID),
                            inter_home_high,
                            inter_home_normal,
                            { interHome = it },
                            { interHome = it }
                        )

                        if (!runBackAfterLoading) {
                            continueOnce()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interHome = null
                        hideLoading()
                        isAdShowing = false

                        loadFallbackInterstitialAd(
                            activity,
                            activity.resources.getString(R.string.Interstitial_Home_ID_High),
                            activity.resources.getString(R.string.Interstitial_Home_ID),
                            inter_home_high,
                            inter_home_normal,
                            { interHome = it },
                            { interHome = it }
                        )

                        continueOnce()
                    }
                }

                hideLoading()

                /*
                 * This replaces your old activity.onBackPressed().
                 * It runs your final back action after loading dialog.
                 * IMPORTANT: onDismissed should NOT call backfunctionality() again.
                 * It should call continueOnBackPressed() / finish / final back logic.
                 */
                if (runBackAfterLoading) {
                    continueOnce()
                }

                interstitialAd.show(activity)

            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                hideLoading()
                isAdShowing = false
                continueOnce()
            }
        }
    }
    /** Remote config `native_home_variation`: 1=download, 2=small, 3=without media. */
    fun getNativeHomeVariation(): Int = when (native_home_variation.toInt()) {
        1, 2, 3 -> native_home_variation.toInt()
        else -> 2
    }

    fun getNativeLayoutRes(variation: Int = getNativeHomeVariation()): Int = when (variation) {
        1 -> R.layout.native_ad_download
        2 -> R.layout.native_ad_small
        3 -> R.layout.native_ad_without_mediaview
        else -> R.layout.native_ad_small
    }

    fun getShimmerContentRes(variation: Int = getNativeHomeVariation()): Int = when (variation) {
        1 -> R.layout.shimmer_content_native_download
        2 -> R.layout.shimmer_content_native_small
        3 -> R.layout.shimmer_content_native_without_media
        else -> R.layout.shimmer_content_native_small
    }

    fun inflateNativeAdView(inflater: LayoutInflater, variation: Int = getNativeHomeVariation()): NativeAdView {
        return inflater.inflate(getNativeLayoutRes(variation), null) as NativeAdView
    }

    fun setupNativeShimmer(adContainer: FrameLayout?, inflater: LayoutInflater) {
        adContainer ?: return
        val variation = getNativeHomeVariation()
        var shimmer = adContainer.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        if (shimmer == null) {
            shimmer = ShimmerFrameLayout(adContainer.context).apply {
                id = R.id.shimmer_view_container
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            adContainer.addView(shimmer, 0)
        }
        shimmer.removeAllViews()
        inflater.inflate(getShimmerContentRes(variation), shimmer, true)
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
    }

    fun hideNativeShimmer(adContainer: FrameLayout?) {
        adContainer?.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)?.let { shimmer ->
            shimmer.stopShimmer()
            shimmer.visibility = View.INVISIBLE
        }
    }

    private fun getNativeAdInflater(
        fragment: Fragment,
        context: Context,
        nativeAFrameLayout: FrameLayout?
    ): LayoutInflater? {
        if (!fragment.isAdded) {
            nativeAFrameLayout?.visibility = View.GONE
            hideNativeShimmer(nativeAFrameLayout)
            return null
        }
        return LayoutInflater.from(fragment.context ?: context)
    }

    private fun displayNativeAdInContainer(
        adView: NativeAdView,
        nativeAFrameLayout: FrameLayout?
    ) {
        nativeAFrameLayout ?: return
        hideNativeShimmer(nativeAFrameLayout)
        for (i in nativeAFrameLayout.childCount - 1 downTo 0) {
            if (nativeAFrameLayout.getChildAt(i) !is ShimmerFrameLayout) {
                nativeAFrameLayout.removeViewAt(i)
            }
        }
        nativeAFrameLayout.addView(adView)
        nativeAFrameLayout.visibility = View.VISIBLE
    }

    private fun requestNativeAd(
        fragment: Fragment,
        context: Context,
        nativeAFrameLayout: FrameLayout?,
        adUnitId: String,
        isHighFloor: Boolean
    ) {
        val inflater = getNativeAdInflater(fragment, context, nativeAFrameLayout) ?: return
        val variation = getNativeHomeVariation()
        val adView = inflateNativeAdView(inflater, variation)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val builder = AdLoader.Builder(context, adUnitId)
                builder.forNativeAd { nativeAd ->
                    try {
                        nativeAdNow?.destroy()
                        nativeAdNow = nativeAd
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                populateNativeAdViewInBackground(nativeAd, adView)
                                displayNativeAdInContainer(adView, nativeAFrameLayout)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        nativeAd.setOnPaidEventListener { adValue ->
                            trackAdjustAdRevenue(
                                adUnitId = adUnitId,
                                revenue = adValue.valueMicros / 1_000_000.0,
                                currency = adValue.currencyCode,
                                token = AppConstant.AD_IMPRESSION_TOKEN,
                                appContext = context
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val videoOptions = VideoOptions.Builder().build()
                val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
                builder.withNativeAdOptions(adOptions)

                val adLoader = builder.withAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Log.d("NativeHome", if (isHighFloor) "loaded high" else "loaded normal")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(
                            "NativeHome",
                            if (isHighFloor) "nativeAdNow: high failed" else "nativeAdNow: normal failed"
                        )
                        if (isHighFloor) {
                            if (fragment.isAdded) {
                                refreshAdNorm(fragment, context, false, nativeAFrameLayout)
                            } else {
                                nativeAFrameLayout?.visibility = View.GONE
                                hideNativeShimmer(nativeAFrameLayout)
                            }
                        } else {
                            nativeAFrameLayout?.visibility = View.GONE
                            hideNativeShimmer(nativeAFrameLayout)
                        }
                    }
                }).build()

                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshAd(
        fragment: Fragment,
        context: Context,
        isDetached: Boolean,
        nativeAFrameLayout: FrameLayout? = null,
        enable: Boolean = true
    ) {
        if (!enable) {
            nativeAFrameLayout?.visibility = View.GONE
            hideNativeShimmer(nativeAFrameLayout)
            return
        }
        if (isDetached) {
            nativeAdNow = null
        }
        Log.d("NativeHome", "isPro: ${isProVersion.value}, variation: ${getNativeHomeVariation()}")
        if (isProVersion.value == true) return

        val inflater = getNativeAdInflater(fragment, context, nativeAFrameLayout) ?: return
        setupNativeShimmer(nativeAFrameLayout, inflater)
        nativeAFrameLayout?.visibility = View.VISIBLE

        if (nativeAdNow == null) {
            Log.d("NativeHome", "nativeAdNow: request high")
            val adUnitId = if (!BuildConfig.DEBUG) {
                "ca-app-pub-5972202469838280/2142408210"
            } else {
                "ca-app-pub-3940256099942544/2247696110"
            }
            requestNativeAd(fragment, context, nativeAFrameLayout, adUnitId, isHighFloor = true)
        } else {
            Log.d("NativeHome", "nativeAdNow: cached $nativeAdNow")
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val cachedInflater = getNativeAdInflater(fragment, context, nativeAFrameLayout)
                        ?: return@launch
                    val adView = inflateNativeAdView(cachedInflater)
                    nativeAdNow?.let { populateNativeAdViewInBackground(it, adView) }
                    displayNativeAdInContainer(adView, nativeAFrameLayout)
                } catch (e: Exception) {
                    Log.d("NativeHome", "exception: $e")
                    e.printStackTrace()
                }
            }
        }
    }

    fun refreshAdNorm(
        fragment: Fragment,
        context: Context,
        isDetached: Boolean,
        nativeAFrameLayout: FrameLayout? = null
    ) {
        if (isProVersion.value == true) {
            Log.d("NativeHome", "User is Pro, No Ad Loaded")
            return
        }
        if (isDetached) {
            nativeAdNow = null
        }

        val inflater = getNativeAdInflater(fragment, context, nativeAFrameLayout) ?: return
        setupNativeShimmer(nativeAFrameLayout, inflater)
        nativeAFrameLayout?.visibility = View.VISIBLE

        if (nativeAdNow == null) {
            val adUnitId = if (!BuildConfig.DEBUG) {
                "ca-app-pub-5972202469838280/1862283176"
            } else {
                "ca-app-pub-3940256099942544/2247696110"
            }
            requestNativeAd(fragment, context, nativeAFrameLayout, adUnitId, isHighFloor = false)
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val cachedInflater = getNativeAdInflater(fragment, context, nativeAFrameLayout)
                        ?: return@launch
                    val adView = inflateNativeAdView(cachedInflater)
                    nativeAdNow?.let { populateNativeAdViewInBackground(it, adView) }
                    displayNativeAdInContainer(adView, nativeAFrameLayout)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun populateNativeAdViewInBackground(nativeAd: NativeAd, adView: NativeAdView) =
        withContext(Dispatchers.Main) {
            adView.findViewById<MediaView>(R.id.ad_media)?.let { mediaView ->
                adView.mediaView = mediaView
                mediaView.mediaContent = nativeAd.mediaContent
            }

            adView.findViewById<TextView>(R.id.ad_headline)?.let { headline ->
                adView.headlineView = headline
                headline.text = nativeAd.headline
            }

            adView.findViewById<TextView>(R.id.ad_body)?.let { body ->
                adView.bodyView = body
                if (nativeAd.body.isNullOrBlank()) {
                    body.visibility = View.GONE
                } else {
                    body.visibility = View.VISIBLE
                    body.text = nativeAd.body
                }
            }

            adView.findViewById<ImageView>(R.id.ad_app_icon)?.let { icon ->
                if (nativeAd.icon == null) {
                    icon.visibility = View.GONE
                } else {
                    icon.visibility = View.VISIBLE
                    icon.setImageDrawable(nativeAd.icon?.drawable)
                }
            }

            adView.findViewById<View>(R.id.ad_call_to_action)?.let { cta ->
                adView.callToActionView = cta
                when (cta) {
                    is Button -> cta.text = nativeAd.callToAction
                    is AppCompatButton -> cta.text = nativeAd.callToAction
                    is TextView -> cta.text = nativeAd.callToAction
                }
            }

            adView.setNativeAd(nativeAd)

            nativeAd.mediaContent?.videoController?.let { vc ->
                if (vc.hasVideoContent()) {
                    vc.videoLifecycleCallbacks =
                        object : VideoController.VideoLifecycleCallbacks() {
                            override fun onVideoEnd() {
                                super.onVideoEnd()
                            }
                        }
                }
            }
        }


    fun getinterstitialshown(): Boolean {
        return isinter
    }

    fun setinterstitialshown(value: Boolean) {
        isinter = value
    }

    fun trackAdjustAdRevenue(
        adUnitId: String?,
        revenue: Double = 0.00,
        currency: String = "USD",
        token: String = "admob_sdk",
        appContext:Context?=null
    ) {
        try {
            //Adjust Events
            val event = AdjustEvent(AppConstant.AD_IMPRESSION_TOKEN)
            // Assign custom identifier to event which will be reported in success/failure callbacks.
            event.addCallbackParameter("ad_unit_id", adUnitId)
            event.setRevenue(revenue, currency)
            Adjust.trackEvent(event)

            //Meta Events
            val metaParams = Bundle().apply {
                putString("ad_unit_id", adUnitId ?: "unknown")
                putString("currency", currency)
            }
            if (appContext != null) {
                val appEventsLogger = AppEventsLogger.newLogger(appContext)
                appEventsLogger.logEvent("ad_impression", metaParams)
                appEventsLogger.logEvent("ad_revenue", revenue, metaParams)
            }
            // printDebugLog("Revenue: $revenue Currency:$currency AdUnitId:$adUnitId")
        } catch (e: Exception) {
            // printDebugLog(" Failed to send revenue event: ${e.message}")
        }
    }


}
