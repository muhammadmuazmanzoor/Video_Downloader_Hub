package com.video.avd.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.avd.util.AdBlockerHelper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.avd.util.AdBlockerHelper.trackAdjustAdRevenue
import com.avd.util.AppConstant
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData
import com.video.avd.MyApplication
import com.video.avd.R
import com.video.avd.ads.AppLovinAdUtils.mRewardedAd
import com.video.avd.constent.isSplash
import com.video.avd.ui.player.RewardAdDismissListener
import com.video.avd.utils.AdDismissedListener
import com.video.avd.ads.AppOpenManager.Companion.isShowingAd
import com.video.avd.utils.GlobalValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@SuppressLint("StaticFieldLeak")
object AdsManager {
    var adSdkChoice: String? = "admob"
    var maxAdImpressions: Int = 0
    var adView: AdView? = null
    const val TAG = "AdsManager"
    var adShowCounter = 0

    var nativeAdNow: NativeAd? = null
    var nativeAdhome: NativeAd? = null
    var nativeAd: NativeAd? = null
    var nativeAdhigh: NativeAd? = null

    var exitAdCount = 0

    var rewardedAd: RewardedAd? = null
    var nativeAdLarge: NativeAd? = null
    var nativeAFrameLayout: FrameLayout? = null
    private var interstitialAdCounter = 0
    var timerTextView: TextView? = null
    private var adclose: ImageView? = null
    var exit_native: Boolean = true
    var exit_interstitial: Boolean = true

    var ob_inter = "inmobi"
    var appOpenAdRemote: Boolean = true
    var recyclerNative: Boolean = false
    private var appOpenWelcomeDialog: Dialog? = null

    ///remote configs
    private var firebaseAnalytics: FirebaseAnalytics? = null

   // var mInterstitialAd: InterstitialAd? = null
   // var mInterstitialAdHigh: InterstitialAd? = null
    private val adShownMap = mutableMapOf<ScreenName, Boolean>()

    private var isBannerLoading = false


    fun isAdShown(screenName: ScreenName): Boolean {
        return try {
            adShownMap[screenName] ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun setAdShown(screenName: ScreenName, shown: Boolean) {
        try {
            adShownMap[screenName] = shown
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeAdShown(screenName: ScreenName, shown: Boolean) {
        try {
            adShownMap[screenName] = shown
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

  /*  fun loadBanner(activity: Activity, adViewLayout: FrameLayout, adContainer: RelativeLayout) {
        if (isBannerLoading) return
        isBannerLoading = true

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000)
            isBannerLoading = false
        }

        CoroutineScope(Dispatchers.Main).launch {
            if (GlobalValues.AdBlockerHelper.isProVersion.value != true && GlobalValues.is24hourEnabled.value != true) {
                if (adView != null) {
                    adView?.destroy()
                    adView = null
                }
                // Wait for layout pass to ensure we have valid width
                adContainer.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        adContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        val adSize = getAdaptiveAdSize(activity, adContainer)
                        val adUnitId = activity.getString(R.string.largeBanner)
                        adView = AdView(activity)
                        adView?.adUnitId = adUnitId
                        adView?.setAdSize(adSize)


                        val adRequest = AdRequest.Builder().build()
                        adView?.loadAd(adRequest)

                        adView?.adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("Banner", "AdMob banner loaded")
                                adContainer.setBackgroundColor(
                                    ContextCompat.getColor(activity, R.color.adaptive_ad_bg)
                                )
                                adContainer.removeAllViews()
                                adContainer.addView(adView)
                                adView?.onPaidEventListener = OnPaidEventListener { adValue ->
                                    val data = SingularAdData(
                                        "AdMob",
                                        adValue.currencyCode,
                                        adValue.valueMicros / 1_000_000.0
                                    )
                                    Singular.adRevenue(data)
                                    trackAdjustAdRevenue(
                                        adUnitId = adUnitId,
                                        revenue = adValue.valueMicros / 1_000_000.0,
                                        currency = adValue.currencyCode,
                                        token = AppConstant.AD_IMPRESSION_TOKEN
                                    )
                                }
                            }

                            override fun onAdFailedToLoad(p0: LoadAdError) {
                                Log.d("Banner", "$p0")
                            }
                        }
                    }
                })
            }
        }
    }

    fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        val containerWidth = if (container.width > 0) container.width else outMetrics.widthPixels
        val adWidth = (containerWidth / density).toInt().coerceAtLeast(320) // Minimum 320dp

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
*/

/*    fun loadAppInterstitialAd(activity: Activity) {
        if (mInterstitialAd == null && maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
            val adUnitId =
                activity.resources.getString(R.string.Interstitial_Home_ID) // Replace with your actual Ad Unit ID
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                activity,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("AdsManager", "Interstitial Ad Loaded")
                        mInterstitialAd = interstitialAd
                        mInterstitialAd?.onPaidEventListener = OnPaidEventListener { adValue ->
                            val impressionData: AdValue = adValue
                            val data = SingularAdData(
                                "AdMob",
                                impressionData.currencyCode,
                                impressionData.valueMicros / 1000000.0
                            )
                            Singular.adRevenue(data)
                            trackAdjustAdRevenue(
                                adUnitId = adUnitId,
                                revenue = adValue.valueMicros / 1_000_000.0,
                                currency = adValue.currencyCode,
                                token = AppConstant.AD_IMPRESSION_TOKEN
                            )
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(
                            "AdsManager",
                            "Failed to load Interstitial Ad: ${loadAdError.message}"
                        )
                        mInterstitialAd = null
                    }
                }
            )
        }
    }


    fun loadAppInterstitialAdHigh(activity: Activity) {
        if (mInterstitialAdHigh == null && maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
            val adUnitId =
                activity.resources.getString(R.string.Interstitial_Home_ID_High) // Replace with your actual Ad Unit ID
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                activity,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("AdsManager", "Interstitial Ad Loaded")
                        mInterstitialAdHigh = interstitialAd
                        mInterstitialAdHigh?.onPaidEventListener = OnPaidEventListener { adValue ->
                            val impressionData: AdValue = adValue
                            val data = SingularAdData(
                                "AdMob",
                                impressionData.currencyCode,
                                impressionData.valueMicros / 1000000.0
                            )
                            Singular.adRevenue(data)
                            trackAdjustAdRevenue(
                                adUnitId = adUnitId,
                                revenue = adValue.valueMicros / 1_000_000.0,
                                currency = adValue.currencyCode,
                                token = AppConstant.AD_IMPRESSION_TOKEN
                            )
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(
                            "AdsManager",
                            "Failed to load Interstitial Ad: ${loadAdError.message}"
                        )
                        mInterstitialAdHigh = null
                    }
                }
            )
        }
    }

    fun showAppInterstitialAd(
        currentActivity: Activity,
        screenName: String,
        playerActivity: Boolean = false,
        onAdClosed: () -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!isAdShown(ScreenName.valueOf(screenName)) && maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
                    Log.e("High/Low", "low Ads Shown")
                    if (mInterstitialAd != null) {
                        showLoading(currentActivity)
                        delay(1000)
                        mInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isSplash = true
                                    onInterstitialImpressionSuccess()
                                    maxAdImpressions++
                                    setAdShown(ScreenName.valueOf(screenName), true)
                                    AppUtils.firebaseUserAction("home_ad_shown", "home_ad_shown")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialAd = null
                                    Log.e("AdsManager", "Splash Ad failed: ${adError.message}")
                                    loadAppInterstitialAd(currentActivity)
                                    AppUtils.firebaseUserAction(
                                        "home_ad_shown_failed",
                                        "home_ad_shown_failed"
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    isSplash = false
                                    mInterstitialAd = null
                                    saveCurrentTime()
                                    loadAppInterstitialAd(currentActivity)
                                    onAdClosed.invoke()
                                }
                            }
                        AppUtils.firebaseUserAction("inter_home", "inter_home")
                        mInterstitialAd?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        onAdClosed.invoke()
                    }
                } else {
                    if (isAdShown(ScreenName.valueOf(screenName))) {
                        removeAdShown(ScreenName.valueOf(screenName), false)
                    }
                    Log.e("AdsManager", "invoked without ad")
                    onAdClosed.invoke()
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                onAdClosed.invoke()
            } finally {
                hideLoading()
            }
        }
    }

    fun showNavInterstitialAd(currentActivity: Activity, onAdClosed: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
                    Log.e("High/Low", "low Ads Shown")
                    if (mInterstitialAd != null) {
                        showLoading(currentActivity)
                        delay(1000)
                        mInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isSplash = true
                                    onInterstitialImpressionSuccess()
                                    maxAdImpressions++
                                    AppUtils.firebaseUserAction("home_ad_shown", "home_ad_shown")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialAd = null
                                    Log.e("AdsManager", "Splash Ad failed: ${adError.message}")
                                    loadAppInterstitialAd(currentActivity)
                                    AppUtils.firebaseUserAction(
                                        "home_ad_shown_failed",
                                        "home_ad_shown_failed"
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    isSplash = false
                                    mInterstitialAd = null
                                    saveCurrentTime()
                                    loadAppInterstitialAd(currentActivity)
                                    onAdClosed.invoke()
                                }
                            }
                        AppUtils.firebaseUserAction("inter_home", "inter_home")
                        mInterstitialAd?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        onAdClosed.invoke()
                    }
                } else {
                    onAdClosed.invoke()
                }
            } catch (e: Exception) {
                onAdClosed.invoke()
            } finally {
                hideLoading()
            }
        }
    }

    fun showAppInterstitialAdHigh(
        currentActivity: Activity,
        screenName: String,
        playerActivity: Boolean = false,
        onAdClosed: () -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!isAdShown(ScreenName.valueOf(screenName)) && maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
                    Log.e("High/Low", "High Ads Shown")
                    if (mInterstitialAdHigh != null) {
                        showLoading(currentActivity)
                        delay(1000)
                        mInterstitialAdHigh?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isSplash = true
                                    onInterstitialImpressionSuccess()
                                    maxAdImpressions++
                                    setAdShown(ScreenName.valueOf(screenName), true)
                                    AppUtils.firebaseUserAction("home_ad_shown", "home_ad_shown")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialAdHigh = null
                                    Log.e("AdsManager", "Splash Ad failed: ${adError.message}")
                                    loadAppInterstitialAdHigh(currentActivity)
                                    onAdClosed.invoke()
                                    AppUtils.firebaseUserAction(
                                        "home_ad_shown_failed",
                                        "home_ad_shown_failed"
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    isSplash = false
                                    mInterstitialAdHigh = null
                                    saveCurrentTime()
                                    loadAppInterstitialAdHigh(currentActivity)
                                    saveCurrentTime()
                                    onAdClosed.invoke()
                                }
                            }
                        AppUtils.firebaseUserAction("inter_home_high", "inter_home_high")
                        mInterstitialAdHigh?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        onAdClosed.invoke()
                    }
                } else {
                    if (isAdShown(ScreenName.valueOf(screenName))) {
                        removeAdShown(ScreenName.valueOf(screenName), false)
                    }
                    Log.e("AdsManager", "invoked without ad")
                    onAdClosed.invoke()
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                onAdClosed.invoke()
            } finally {
                hideLoading()
            }
        }
    }*/


    fun showAppOpenWelcomeDialog(activity: Activity) {
        hideAppOpenWelcomeDialog()
        if (activity.isFinishing || activity.isDestroyed) return

        appOpenWelcomeDialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_app_open_welcome)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            setOnDismissListener {
                appOpenWelcomeDialog = null
            }
        }

        try {
            if (!activity.isFinishing && !activity.isDestroyed) {
                appOpenWelcomeDialog?.show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing app open welcome dialog", e)
            appOpenWelcomeDialog = null
        }
    }

    fun hideAppOpenWelcomeDialog() {
        try {
            val dialog = appOpenWelcomeDialog ?: return
            val context = dialog.context
            if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                appOpenWelcomeDialog = null
                return
            }
            if (dialog.isShowing) {
                dialog.dismiss()
            }
            appOpenWelcomeDialog = null
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding app open welcome dialog", e)
            appOpenWelcomeDialog = null
        }
    }

    fun onInterstitialImpressionSuccess() {
        CoroutineScope(Dispatchers.IO).launch {
            MyApplication.context?.let {
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
                                    "Interstitial ${interstitialAdCounter.plus(1)}"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        exitAdCount++
    }


    fun refreshAdVideo(
        view: View,
        context: Context,
        isDetached: Boolean,
        fragment: Activity,
    ) {
        if (AdBlockerHelper.isProVersion.value != true) {
            if (nativeAdNow == null) {
                val adUnitId = context.resources.getString(R.string.Native_ID)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val builder =
                            AdLoader.Builder(
                                context,
                                adUnitId
                            )
                        // OnLoadedListener implementation.
                        builder.forNativeAd { nativeAd: NativeAd ->
                            // If this callback occurs after the activity is destroyed, you must call
                            // destroy and return or you may get a memory leak.
                            try {
                                val isDestroyed: Boolean = isDetached
                                if (isDestroyed) {
                                    if (nativeAdNow != null) {
                                        nativeAdNow?.destroy()
                                        return@forNativeAd
                                    }
                                }
                                // You must call destroy on old ads when you are done with them,
                                // otherwise you will have a memory leak.
                                if (nativeAdNow != null) {
                                    nativeAdNow?.destroy()
                                }
                                nativeAdNow = nativeAd
                                CoroutineScope(Dispatchers.Main).launch {
                                    nativeAFrameLayout = view.findViewById(R.id.fl_adplace)
                                    try {

                                        val adView = fragment?.layoutInflater?.inflate(
                                            R.layout.native_ad_music_player,
                                            null
                                        ) as NativeAdView
                                        timerTextView =
                                            adView?.findViewById<TextView>(R.id.tvRemainTime)
                                        timerTextView?.visibility = View.GONE
                                        adclose = adView?.findViewById(R.id.adclose)
                                        adclose?.visibility = View.VISIBLE
                                        nativeAdNow?.let {
                                            populateNativeAdViewInBackground(it, adView)
                                        }
                                        nativeAFrameLayout?.removeAllViews()
                                        nativeAFrameLayout?.addView(adView)
                                        nativeAFrameLayout?.visibility = View.VISIBLE
                                    } catch (e: Exception) {

                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "refreshAd: $e")
                            } catch (e: IllegalStateException) {
                                Log.e(TAG, "refreshAd illegal: $e")
                            }
                            nativeAd.setOnPaidEventListener { adValue ->
                                trackAdjustAdRevenue(
                                    adUnitId = adUnitId,
                                    revenue = adValue.valueMicros / 1_000_000.0,
                                    currency = adValue.currencyCode,
                                    token = AppConstant.AD_IMPRESSION_TOKEN,
                                    appContext = MyApplication.context
                                )
                            }
                        }
                        val videoOptions = VideoOptions.Builder().build()
                        val adOptions =
                            NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
                        builder.withNativeAdOptions(adOptions)
                        val adLoader = builder.withAdListener(object : AdListener() {
                            //                               override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                adclose?.setOnClickListener {
                                    nativeAFrameLayout?.visibility = View.GONE
                                }
                                nativeAdNow?.setOnPaidEventListener {
                                    val impressionData: AdValue = it
                                    val data = SingularAdData(
                                        "AdMob",
                                        impressionData.currencyCode,
                                        impressionData.valueMicros / 1000000.0
                                    )
                                    Singular.adRevenue(data)
                                }
                            }
                        }).build()
                        adLoader.loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    nativeAFrameLayout = view.findViewById(R.id.fl_adplace)
                    val adView = fragment?.layoutInflater?.inflate(
                        R.layout.native_ad_music_player,
                        null
                    ) as NativeAdView
                    timerTextView = adView.findViewById<TextView>(R.id.tvRemainTime)
                    timerTextView?.visibility = View.GONE
                    adclose = adView.findViewById(R.id.adclose)
                    adclose?.visibility = View.VISIBLE
                    nativeAdNow?.let {
                        populateNativeAdViewInBackground(it, adView)
                    }
                    nativeAFrameLayout?.removeAllViews()
                    nativeAFrameLayout?.addView(adView)
                    adclose?.setOnClickListener {
                        nativeAFrameLayout?.visibility = View.GONE
                    }
                }
            }

        }
    }


    fun refreshAd(
        view: View,
        goneView: View?,
        context: Context,
        isDetached: Boolean,
        fragment: Fragment,
        frameLayout: FrameLayout? = null
    ) {
        Log.d("refreshAdDebug", "refreshAd() called")
        if (AdBlockerHelper.isProVersion.value != true) {
            Log.d("refreshAdDebug", "Not Pro Version, proceeding")
            if (nativeAdNow == null) {
                val adUnitId = context.resources.getString(R.string.Native_static)
                Log.d("refreshAdDebug", "nativeAdNow null, proceeding")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val builder = AdLoader.Builder(
                            context,
                            adUnitId
                        )
                        // OnLoadedListener implementation.
                        builder.forNativeAd { nativeAd: NativeAd ->
                            // If this callback occurs after the activity is destroyed, you must call
                            // destroy and return or you may get a memory leak.
                            try {
                                val isDestroyed: Boolean = isDetached
                                if (isDestroyed) {
                                    if (nativeAdNow != null) {
                                        nativeAdNow?.destroy()
                                        return@forNativeAd
                                    }
                                }
                                // You must call destroy on old ads when you are done with them,
                                // otherwise you will have a memory leak.
                                if (nativeAdNow != null) {
                                    nativeAdNow?.destroy()
                                }
                                nativeAdNow = nativeAd
                                CoroutineScope(Dispatchers.Main).launch {
                                    nativeAFrameLayout = view.findViewById(R.id.fl_adplace)
                                    try {
                                        val responseInfo = nativeAd.responseInfo
                                        val mediationAdapterClassName =
                                            responseInfo?.mediationAdapterClassName
                                        val isFromMeta = mediationAdapterClassName?.contains(
                                            "facebook",
                                            ignoreCase = true
                                        ) == true
                                        Log.d("refreshAdDebug", "$mediationAdapterClassName")
                                        val adView = fragment.layoutInflater.inflate(
                                            R.layout.native_ad,
                                            null
                                        ) as NativeAdView
                                        nativeAdNow?.let {
                                            populateNativeAdViewInBackground(it, adView)
                                        }
                                        nativeAFrameLayout?.removeAllViews()
                                        nativeAFrameLayout?.addView(adView)
                                        nativeAFrameLayout?.visibility = View.VISIBLE
//                                                }
                                    } catch (e: Exception) {
                                        Log.d("refreshAdDebug", "$e")
                                        e.printStackTrace()
                                    }
                                }
                                nativeAd.setOnPaidEventListener { adValue ->
                                    trackAdjustAdRevenue(
                                        adUnitId = adUnitId,
                                        revenue = adValue.valueMicros / 1_000_000.0,
                                        currency = adValue.currencyCode,
                                        token = AppConstant.AD_IMPRESSION_TOKEN,
                                        appContext = MyApplication.context
                                    )
                                }
                            } catch (e: Exception) {
                                Log.d("refreshAdDebug", "$e")
                                Log.e("refreshAdDebug", "refreshAdDebug: $e")
                            } catch (e: IllegalStateException) {
                                Log.e("refreshAdDebug", "refreshAdDebug: $e")
                            }
                        }
                        val videoOptions = VideoOptions.Builder().build()
                        val adOptions =
                            NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
                        builder.withNativeAdOptions(adOptions)
                        val adLoader = builder.withAdListener(object : AdListener() {
                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                try {
                                    goneView?.let {
                                        it.isVisible = false
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "refreshAdDebug $e")
                                }
                                nativeAdNow?.setOnPaidEventListener {
                                    val impressionData: AdValue = it
                                    val data = SingularAdData(
                                        "AdMob",
                                        impressionData.currencyCode,
                                        impressionData.valueMicros / 1000000.0
                                    )
                                    Singular.adRevenue(data)
                                }
                            }
                        }).build()
                        adLoader.loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.d("refreshAdDebug", "$e")
                        e.printStackTrace()
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    nativeAFrameLayout = view.findViewById(R.id.fl_adplace)
                    val responseInfo = nativeAd?.responseInfo
                    val mediationAdapterClassName = responseInfo?.mediationAdapterClassName
                    val isFromMeta =
                        mediationAdapterClassName?.contains("facebook", ignoreCase = true) == true
                    Log.d("refreshAdDebug", "$mediationAdapterClassName")

                    val adView =
                        fragment.layoutInflater.inflate(R.layout.native_ad, null) as NativeAdView
                    nativeAdNow?.let {
                        populateNativeAdViewInBackground(it, adView)
                    }
                    nativeAFrameLayout?.removeAllViews()
                    nativeAFrameLayout?.addView(adView)
                    nativeAFrameLayout?.visibility = View.VISIBLE
//                            }
                    try {
                        goneView?.let {
                            it.isVisible = false
                        }
                    } catch (e: Exception) {
                        Log.d("refreshAdDebug", "$e")
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    fun loadRewardedAd(context: Context) {
//        if (GlobalValues.AdBlockerHelper.isProVersion.value != true) {
//            if (!AdsManager.isLoading && rewardedAd == null) {
//                AdsManager.isLoading = true
//                val adRequest = AdRequest.Builder().build()
//                RewardedAd.load(
//                    context,
//                    context.resources.getString(R.string.rewardedVideo),
//                    adRequest,
//                    object : RewardedAdLoadCallback() {
//                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                            // Handle the error.
//                            rewardedAd = null
//                            AdsManager.isLoading = false
//                        }
//
//                        override fun onAdLoaded(rewardedAd: RewardedAd) {
//                            AdsManager.rewardedAd = rewardedAd
//                            AdsManager.isLoading = false
//                            AdsManager.rewardedAd?.setOnPaidEventListener {
//                                val impressionData: AdValue = it
//                                val data = SingularAdData(
//                                    "AdMob",
//                                    impressionData.currencyCode,
//                                    impressionData.valueMicros / 1000000.0)
//                                Singular.adRevenue(data)
//                            }
//                        }
//                    })
//            }
//        } else {
//            rewardedAd = null
//        }
    }


    fun showRewardedVideoforMp3(
        context: Context,
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        adDismissedListener: AdDismissedListener
    ) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    isSplash = true
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewardedAd(context)
                }

                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    adDismissedListener.onAdDismissed()
                    loadRewardedAd(context)
                }
            }
            rewardedAd?.show(activity, onUserEarnedRewardListener)
        } else {
            Toast.makeText(activity, "Ad not loaded yet! try again", Toast.LENGTH_SHORT).show()
            loadRewardedAd(context)
        }
    }


    fun showRewardedVideoforMp3AppLovin(
        context: Context,
        activity: Activity,
        onUserEarnedRewardListener: () -> Unit, // Listener for earning rewards
        adDismissedListener: () -> Unit        // Listener for ad dismissed
    ) {
        if (mRewardedAd?.isReady == true) {
            mRewardedAd?.setListener(object : MaxRewardedAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    // Ad is loaded, ready to show
                    Toast.makeText(context, "Ad loaded and ready!", Toast.LENGTH_SHORT).show()
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    // Ad is displayed
                    isSplash = true
                }

                override fun onAdHidden(ad: MaxAd) {
                    // Ad dismissed by the user
                    isShowingAd = false
                    Toast.makeText(context, "Ad dismissed", Toast.LENGTH_SHORT).show()
                    adDismissedListener()
                    AppLovinAdUtils.loadRewardedAd(context)
                }

                override fun onAdClicked(ad: MaxAd) {
                    // Ad clicked
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    // Ad failed to load
                    isShowingAd = false
                    Toast.makeText(
                        context,
                        "Failed to load ad: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    AppLovinAdUtils.loadRewardedAd(context)
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    // Ad failed to display
                    isShowingAd = true
                    Toast.makeText(
                        context,
                        "Ad failed to display: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    AppLovinAdUtils.loadRewardedAd(context)
                }

                override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
                    // User earned reward
                    onUserEarnedRewardListener()
                }
            })
            mRewardedAd?.showAd()
        } else {
            Toast.makeText(context, "Ad not loaded yet! Please try again.", Toast.LENGTH_SHORT)
                .show()
            AppLovinAdUtils.loadRewardedAd(context)
        }
    }


    fun showRewardedVideo(
        context: Context,
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        rewardAdDismissListener: RewardAdDismissListener? = null
    ) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    isShowingAd = true
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    isShowingAd = false
                    AdsManager.loadRewardedAd(context)
                }

                override fun onAdDismissedFullScreenContent() {

                    rewardedAd = null
                    isShowingAd = false
                    rewardAdDismissListener?.onDismissRewardAd()
                    AdsManager.loadRewardedAd(context)
                }
            }
            rewardedAd?.show(activity, onUserEarnedRewardListener)
        } else {
            AdsManager.loadRewardedAd(context)
        }
    }


    fun showRewardedVideoAppLovin(
        context: Context,
        activity: Activity,
        onUserEarnedRewardListener: () -> Unit,
        rewardAdDismissListener: (() -> Unit)? = null
    ) {
        if (mRewardedAd?.isReady == true) {
            mRewardedAd?.setListener(object : MaxRewardedAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    // Ad has been loaded and is ready to show
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    // Called when the ad is displayed
                    isShowingAd = true
                }

                override fun onAdHidden(ad: MaxAd) {
                    // Called when the ad is dismissed
                    AppLovinAdUtils.loadRewardedAd(context)
                    isShowingAd = false
                    mRewardedAd = null
                    onUserEarnedRewardListener.invoke()
//                    rewardAdDismissListener?.invoke() // Notify the dismissal callback

                }

                override fun onAdClicked(ad: MaxAd) {
                    // Called when the ad is clicked
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    // Called when the ad fails to load
                    rewardAdDismissListener?.invoke()
                    Toast.makeText(
                        context,
                        "Failed to load ad: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    AppLovinAdUtils.loadRewardedAd(context)
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    // Called when the ad fails to display
                    isShowingAd = false
                    mRewardedAd = null
                    rewardAdDismissListener?.invoke()
                    Toast.makeText(
                        context,
                        "Failed to display ad: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    AppLovinAdUtils.loadRewardedAd(context)
                }


                override fun onUserRewarded(ad: MaxAd, reward: com.applovin.mediation.MaxReward) {
                    // Called when the user earns a reward
//                    onUserEarnedRewardListener()
                }
            })

            mRewardedAd?.showAd() // Show the rewarded ad
        } else {
            Toast.makeText(activity, "Ad not loaded yet! Try again.", Toast.LENGTH_SHORT).show()
            AppLovinAdUtils.loadRewardedAd(context)
        }
    }


    suspend fun populateNativeAdViewInBackground(nativeAd: NativeAd, adView: NativeAdView) =
        withContext(Dispatchers.Main) {
            adView.mediaView = adView.findViewById(com.avd.R.id.ad_media)
            adView.headlineView = adView.findViewById(com.avd.R.id.ad_headline)
            adView.callToActionView =
                adView.findViewById(com.avd.R.id.ad_call_to_action)

            (adView.headlineView as TextView).text = nativeAd.headline
            (adView.callToActionView as Button).text = nativeAd.callToAction
            adView.mediaView?.mediaContent = nativeAd.mediaContent

            // Finally: must call this
            adView.setNativeAd(nativeAd)

            // Optional: handle video
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

}