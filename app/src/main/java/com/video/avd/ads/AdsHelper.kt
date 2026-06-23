package com.video.avd.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustEvent
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.isProVersion
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.MyApplication
import com.video.avd.R

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object AdsHelper {
    //InApp Purchases

    private var loadingDialog: Dialog? = null

    //===================Splash=================//
    private const val TAG = "SplashInterstitial"


    //===================Language=================//

    var langNativeAd1: NativeAd? = null
    var langNativeAdHigh1: NativeAd? = null
    var langNativeAd2: NativeAd? = null
    var langNativeAdHigh2: NativeAd? = null

    var languageButtonDelay: Int = 3
    var languageButtonStyle: Int = 1
    var langSessionRemote = 3
    var langNative1Enabled: Boolean = true
    var langNativeHigh1Enabled: Boolean = true

    var langNative2Enabled: Boolean = true
    var langNativeHigh2Enabled: Boolean = true
    var langNativeFormat: Int = 2
    var langCtaColor: String = "#8A38F5"
    var langCtaTextColor: String = "#000000"
    var langCtaTextStyle: String = "bold"

    //===================Onboarding=================//
    var obNativeAd1: NativeAd? = null
    var obNativeAdHigh1: NativeAd? = null

    var obNativeAd3: NativeAd? = null
    var obNativeAdHigh3: NativeAd? = null

    var obNativeAd4: NativeAd? = null
    var obNativeAdHigh4: NativeAd? = null

    var obNativeAdFullScr1: NativeAd? = null
    var obNativeAdHighFullScr1: NativeAd? = null

    var obNativeAdFullScr2: NativeAd? = null
    var obNativeAdHighFullScr2: NativeAd? = null
    var interstitialSurvey: InterstitialAd? = null
    var surveyEnable = true
    var surveyNativeHighEnable = true
    var surveyNativeEnable = true
    var surveyInterstitialEnabled: Boolean = true
    var surveyInterstitialHighEnabled: Boolean = true
    var obEnable: Boolean = true


    var obInterstitialEnabled: Boolean = true
    var obInterstitialHighEnabled: Boolean = true

    var obFirstEnable: Boolean = true
    var obSecondEnable: Boolean = true
    var obThirdEnable: Boolean = true
    var obFourthEnable: Boolean = true

    var obNative1Enabled: Boolean = true
    var obNative3Enabled: Boolean = true
    var obNative4Enabled: Boolean = true

    var obNativeHigh1Enabled: Boolean = true
    var obNativeHigh3Enabled: Boolean = true
    var obNativeHigh4Enabled: Boolean = true

    var obNativeHighFullScr1Enabled: Boolean = false
    var obNativeHighFullScr2Enabled: Boolean = false
    var obNativeFullScr1Enabled: Boolean = false
    var obNativeFullScr2Enabled: Boolean = false

    var featureNative1Enabled: Boolean = true
    var featureNativeHigh1Enabled: Boolean = true

    var obNativeFormat: Int = 2
    var obCtaColor: String = "#8A38F5"
    var obCtaTextColor: String = "#000000"
    var obCtaTextStyle: String = "bold"

    var isShowNativeFullCross = true
    var nativeFullCrossDelay = 3



    //===================Splash=================//

    var splashInterstitialEnabled: Boolean = true
    var splashInterstitialHighEnabled: Boolean = true

    var splashBannerEnabled: Boolean = true
    var splashBannerHighEnabled: Boolean = true

    var splashNativeEnabled: Boolean = false
    var splashNativeHighEnabled: Boolean = false
    var splashNativeFormat: Int = 1

    var splashCtaColor: String = "#F75655"
    var splashCtaTextColor: String = "#000000"
    var splashCtaTextStyle: String = "bold"

    var splashNativeFullCrossDelay = 3


    private val _obFull1Loaded = MutableLiveData(false)
    var native_language = MutableLiveData<Boolean>()
    var native_ob1 = MutableLiveData<Boolean>()
    var native_ob4 = MutableLiveData<Boolean>()
    val obFull1Loaded: LiveData<Boolean> get() = _obFull1Loaded

    private val _obFull2Loaded = MutableLiveData(false)
    val obFull2Loaded: LiveData<Boolean> get() = _obFull2Loaded


    fun obFull1Ready() {
        if (_obFull1Loaded.value != true) _obFull1Loaded.postValue(true)
    }

    fun obFull2Ready() {
        if (_obFull2Loaded.value != true) _obFull2Loaded.postValue(true)
    }


    fun loadWithFallback(
        activity: Activity,
        highFloorAdId: String,
        normalAdId: String,
        showHighfloor: Boolean = true,
        showNormalfloor: Boolean = true,
        onAdLoadedHigh: (nativeAd: NativeAd) -> Unit,
        onAdLoadedNormal: (nativeAd: NativeAd) -> Unit,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (AdBlockerHelper.isProVersion.value != true) {
            try {
                if (showHighfloor) {
                    val highFloorLoader = AdLoader.Builder(activity, highFloorAdId)
                        .forNativeAd { ad ->

                            Log.d(
                                "nativeAdFlow",
                                "loaded ✅ $highFloorAdId"
                            )

                            onAdLoadedHigh(ad)
                            ad.setOnPaidEventListener { adValue ->
                                trackAdjustAdRevenue(
                                    adUnitId = highFloorAdId,
                                    revenue = adValue.valueMicros / 1_000_000.0,
                                    currency = adValue.currencyCode,
                                    source = "Native"
                                )
                            }
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {

                                Log.w(
                                    "nativeAdFlow",
                                    "Ad failed ❌ Requesting Normal ${error}\n  $highFloorAdId"
                                )
                                // Fallback → Normal Ad
                                loadNormalAd(
                                    activity,
                                    normalAdId,
                                    showNormalfloor,
                                    onAdLoadedNormal,
                                    onAdFailed
                                )
                            }
                        })
                        .build()

                    highFloorLoader.loadAd(AdRequest.Builder().build())
                } else {
                    // Fallback → Normal Ad
                    loadNormalAd(
                        activity,
                        normalAdId,
                        showNormalfloor,
                        onAdLoadedNormal,
                        onAdFailed
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                onAdFailed?.invoke()
            }
        }

    }

    private fun loadNormalAd(
        activity: Activity,
        adId: String,
        showAd: Boolean = true,
        onAdLoaded: (nativeAd: NativeAd) -> Unit,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (isProVersion.value != true) {
            if (!showAd)
                return
            val normalLoader = AdLoader.Builder(activity, adId)
                .forNativeAd { ad ->
                    Log.d(
                        "nativeAdFlow",
                        "loaded ✅ $adId"
                    )
                    onAdLoaded(ad)
                    ad.setOnPaidEventListener { adValue ->
                        trackAdjustAdRevenue(
                            adUnitId = adId,
                            revenue = adValue.valueMicros / 1_000_000.0,
                            currency = adValue.currencyCode,
                            source = "Native"
                        )
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(
                            "nativeAdFlow",
                            "Ad failed ❌ Requesting Normal ${error}\n  $adId"
                        )
                        onAdFailed?.invoke()
                    }
                })
                .build()

            normalLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun loadBanner(
        activity: Activity,
        highFloorAdId: String,
        normalAdId: String,
        showHighFloor: Boolean = true,
        showNormalFloor: Boolean = true,
        isCollapsable: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null,
        adContainer: FrameLayout
    ) {

        if (AdBlockerHelper.isProVersion.value == true) return
        // Try to load the high banner first
        if (showHighFloor) {
            loadBannerAd(
                activity = activity,
                container = adContainer,
                isCollapsable = isCollapsable,
                adId = highFloorAdId,
                onLoaded = {},
                onFailure = {
                    // On failure → load normal banner as fallback
                    if (showNormalFloor) {
                        loadBannerAd(
                            activity = activity,
                            container = adContainer,
                            isCollapsable = isCollapsable,
                            adId = normalAdId,
                            onFailure = onAdFailed,
                            onLoaded = onLoaded
                        )
                    }
                }
            )
        } else if (showNormalFloor) {
            loadBannerAd(
                activity = activity,
                container = adContainer,
                isCollapsable = isCollapsable,
                adId = highFloorAdId,
                onFailure = onAdFailed,
                onLoaded = onLoaded
            )
        }

    }

    fun loadBannerAd(
        activity: Activity,
        container: FrameLayout,
        adId: String,
        isCollapsable: Boolean = false,
        onLoaded: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        // Clear container safely
        container.removeAllViews()

        // Inflate shimmer
        val shimmerView = LayoutInflater.from(activity)
            .inflate(R.layout.load_fb_banner, container, false)

        val shimmer =
            shimmerView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)

        shimmer?.startShimmer()
        container.addView(shimmerView)
        val extras = Bundle().apply {
            putString("collapsible", "bottom") // or "top"
        }

        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                extras
            )
            .build()
        // Create AdView
        val adView = AdView(activity).apply {
            adUnitId = adId
            setAdSize(getAdaptiveAdSize(activity, container))

            // Revenue tracking (set ONCE)
            onPaidEventListener = OnPaidEventListener { adValue ->
                val revenue = adValue.valueMicros / 1_000_000.0
                trackAdjustAdRevenue(
                    adUnitId = adId,
                    revenue = revenue,
                    currency = adValue.currencyCode,
                    source = "Banner"
                )
            }
        }

        container.addView(adView)

        adView.adListener = object : AdListener() {

            override fun onAdLoaded() {
                shimmer?.apply {
                    stopShimmer()
                    visibility = View.GONE
                }
                onLoaded?.invoke()
                Log.d("AdBanner", "Banner loaded → $adId")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                shimmer?.stopShimmer()
                container.removeAllViews()
                onFailure?.invoke()
                Log.e("AdBanner", "Banner failed →$adId,   ${error.message}")
            }
        }

        if (isCollapsable) {
            adView.loadAd(adRequest)
        } else {
            adView.loadAd(AdRequest.Builder().build())
        }
    }


    fun getAdaptiveAdSize(activity: Activity, adContainer: FrameLayout): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        val adWidthPixels = adContainer.width.takeIf { it > 0 }
            ?: outMetrics.widthPixels
        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    // ---------------------------------------------------------
    // Revenue listener helper
    // ---------------------------------------------------------

    fun attachRevenueListener(interstitialAd: InterstitialAd) {
        interstitialAd.onPaidEventListener = OnPaidEventListener { value ->
            val revenue = value.valueMicros / 1_000_000.0
            Log.d(TAG, "PaidEvent → rev: $revenue ${value.currencyCode}")
            trackAdjustAdRevenue(
                adUnitId = interstitialAd.adUnitId,
                revenue = value.valueMicros / 1_000_000.0,
                currency = value.currencyCode,
                source = "Interstitial"
            )
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
        if (AdBlockerHelper.isProVersion.value == true) {
            return
        }

        InterstitialAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onLoaded(ad)
                    ad.onPaidEventListener = OnPaidEventListener { adValue ->
                        trackAdjustAdRevenue(
                            adUnitId = adUnitId,
                            revenue = adValue.valueMicros / 1_000_000.0,
                            currency = adValue.currencyCode,
                            source = "Interstitial"
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onFailed(loadAdError)
                }
            })
    }

    fun showInterstitial(
        forFragment: Boolean = false,
        interstitialAd: InterstitialAd,
        activity: FragmentActivity,
        onDismissed: (() -> Unit)? = null,
        eventName: String = ""
    ) {
        activity.lifecycleScope.launch {
            try {
                showLoading(activity)
                delay(1000)
                interstitialAd.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "show Interstitial → SHOW ✔")
                            isShowingAd = true
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "show Interstitial → DISMISSED ✖")
                            isShowingAd = false
                            nullifyUsedAd(interstitialAd)
                            if (!forFragment) {
                                hideLoading()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "show Interstitial SHOW FAILED ❌: ${adError.message}")
                            isShowingAd = false
                        }
                    }

                activity.lifecycleScope.launch {
                    if (forFragment) {
                        interstitialAd.show(activity)
                        delay(400)
                        onDismissed?.invoke()
                        hideLoading()
                    } else {
                        onDismissed?.invoke()
                        interstitialAd.show(activity)
                    }
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                onDismissed?.invoke()
            } finally {
            }
        }
    }

    private fun nullifyUsedAd(interAd: InterstitialAd) {
        when (interAd) {
//            obInterstitialHigh -> obInterstitialHigh = null
//            obInterstitialAll -> obInterstitialAll = null
        }
    }

    fun showLoading(context: Context) {
        if (loadingDialog?.isShowing == true) return

        loadingDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.ad_dialog)
            window?.apply {
                // Make background fully transparent
                setBackgroundDrawable(ColorDrawable(Color.WHITE))

                // Remove all margins → truly fullscreen
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val img = findViewById<ImageView>(R.id.progressImage)
            img?.startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate))

            // Dialog properties
            setCanceledOnTouchOutside(false)
            setCancelable(false)

            setOnDismissListener {
                loadingDialog = null
            }
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
    }


    fun trackAdjustAdRevenue(
        adUnitId: String?,
        revenue: Double = 0.00,
        currency: String = "USD",
        source: String
    ) {
        try {
            /* val event = AdjustEvent(AdjustConstant.AD_IMPRESSION_TOKEN)
             // Assign custom identifier to event which will be reported in success/failure callbacks.
             event.addCallbackParameter("ad_unit_id", adUnitId)
             event.setRevenue(revenue, currency)
             Adjust.trackEvent(event)*/
            val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
            adjustAdRevenue.setRevenue(revenue, currency)  // ✅ REQUIRED
            adjustAdRevenue.addPartnerParameter("ad_format", source)
            adjustAdRevenue.addPartnerParameter("ad_unit_id", adUnitId)
            Adjust.trackAdRevenue(adjustAdRevenue)


            val logger = AppEventsLogger.newLogger(MyApplication.getInstance())
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currency)
            }
            logger.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, revenue, params)
        } catch (_: Exception) {
        }
    }

    fun getMediationInfo(nativeAd: NativeAd): String {
        val responseInfo = nativeAd.responseInfo
        val mediationAdapterClassName = responseInfo?.mediationAdapterClassName
        when {
            mediationAdapterClassName?.contains("facebook", ignoreCase = true) == true -> {
                return "meta"
            }

            else -> {
                return "other"

            }
        }
    }

    fun displayNative(
        nativeAd: NativeAd?,
        adBinding: FrameLayout?,
        activity: FragmentActivity?,
        shimmer: ShimmerFrameLayout
    ) {
        try {

            nativeAd?.let {
                val layoutResId = when (getMediationInfo(nativeAd)) {
                    "meta" -> R.layout.layout_native_ads_meta
                    else -> when (obNativeFormat) {
                        1 -> R.layout.layout_native_ads_without_mediaview
                        2 -> R.layout.layout_native_ads
                        3 -> R.layout.layout_native_ads_ctr_up
                        else -> R.layout.layout_native_ads_without_mediaview
                    }
                }

                val adView = LayoutInflater.from(activity)
                    .inflate(layoutResId, null) as NativeAdView
                activity?.let { it1 -> populateNativeAdView(it, adView, it1) }
                adBinding?.removeAllViews()
                adBinding?.addView(adView)
                adBinding?.visibility = View.VISIBLE
            }


            shimmer.let { shimmerLayout ->
                // When data is loaded (e.g., ad is ready), stop shimmer
                shimmerLayout.stopShimmer()
                shimmerLayout.visibility = View.GONE
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        activity: FragmentActivity
    ) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? AppCompatButton)?.text = nativeAd.callToAction
        adView.mediaView?.mediaContent = nativeAd.mediaContent

        (adView.callToActionView as? AppCompatButton)?.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(obCtaColor))

        val typeface = if (obCtaTextStyle.equals(
                "bold",
                ignoreCase = true
            )
        ) ResourcesCompat.getFont(activity, R.font.poppins_bold)
        else ResourcesCompat.getFont(activity, R.font.poppins_regular)
        (adView.callToActionView as? AppCompatButton)?.typeface = typeface
        (adView.callToActionView as? AppCompatButton)?.setTextColor(
            Color.parseColor(obCtaTextColor)
        )
        adView.setNativeAd(nativeAd)
    }
}