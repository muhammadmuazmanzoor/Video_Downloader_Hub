package com.video.avd.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.banner_home_enable
import com.avd.util.AdBlockerHelper.browser_native
import com.avd.util.AdBlockerHelper.exitTimer
import com.avd.util.AdBlockerHelper.exit_native
import com.avd.util.AdBlockerHelper.home_native
import com.avd.util.AdBlockerHelper.inter_browser
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.inter_videos
import com.avd.util.AdBlockerHelper.isProVersion
import com.avd.util.AdBlockerHelper.proCrossTimer
import com.avd.util.AdBlockerHelper.recycler_native
import com.avd.util.AppConstant
import com.avd.util.ads.InterstitialManagerA
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData
import com.video.avd.MyApplication
import com.video.avd.R
import com.video.avd.utils.GlobalValues

@SuppressLint("StaticFieldLeak")
object AdsManagerKit {





    fun init(remoteConfig: FirebaseRemoteConfig) {
        try {
            if (remoteConfig == null)
                return
            //Splash Screen Banner Visibility
            val splashJson = remoteConfig.getString("splash_ads")
            RemoteJsonConvertor.splashJsonConvertor(splashJson)
            val langJson = remoteConfig.getString("language_ads")
            RemoteJsonConvertor.langJsonConvertor(langJson)
            val onboardingJson = remoteConfig.getString("onboarding_ads")
            RemoteJsonConvertor.obJsonConvertor(onboardingJson)
            AdsManager.appOpenAdRemote = remoteConfig.getBoolean("appOpenAd")
            AdsManager.exit_native = remoteConfig.getBoolean("exit_native")
            AdsManager.exit_interstitial = remoteConfig.getBoolean("exit_interstitial")
            InterstitialManagerA.interstitialAdCounter = remoteConfig.getLong("interstitialAdCounter").toInt().coerceAtLeast(3)
            AdsManager.recyclerNative = remoteConfig.getBoolean("recyclerNative")
            recycler_native = remoteConfig.getBoolean("recycler_native")
            home_native = remoteConfig.getBoolean("home_native")
            browser_native = remoteConfig.getBoolean("browser_native")
            proCrossTimer = remoteConfig.getLong("pro_cross_timer").toInt()
            exitTimer = remoteConfig.getLong("exit_timer").toInt()
            exit_native = remoteConfig.getBoolean("exit_native")
            inter_home_high = remoteConfig.getBoolean("inter_home_high")
            inter_home = remoteConfig.getBoolean("inter_home")
            inter_browser = remoteConfig.getBoolean("inter_browser")
            inter_videos = remoteConfig.getBoolean("inter_videos")
            inter_home_normal = remoteConfig.getBoolean("inter_home_normal")
            banner_home_enable = remoteConfig.getBoolean("banner_home")
            AdsManager.ob_inter = remoteConfig.getString("ob_inter")
            MyApplication.Companion.isShowPermission = remoteConfig.getBoolean("show_permission_screen")
            AdBlockerHelper.cooldownValue = remoteConfig.getLong("cooldown_seconds")
            GlobalValues.newProType = !remoteConfig.getBoolean("newprotype")
            AdBlockerHelper.remotePopupAdImpressions = remoteConfig.getLong("Download_pop_up_inter").toInt()
            AdBlockerHelper.remotemaxAdImpressions = remoteConfig.getLong("maxAdImpressions").toInt()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun identifyAd(adId: String): AdInfo {
        AdPlacement.entries.forEach { placement ->
            when (adId) {
                placement.highFloorAdId -> return AdInfo(placement, "Highfloor")
                placement.normalAdId -> return AdInfo(placement, "Normal")
            }
        }
        return AdInfo(null, "Unknown")
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
        if (isProVersion.value!=true) {
            try {
                if (showHighfloor) {
                    val highFloorLoader = AdLoader.Builder(activity, highFloorAdId)
                        .forNativeAd { ad ->
                            onAdLoadedHigh(ad)
                            ad.setOnPaidEventListener { adValue ->
                                AdBlockerHelper.trackAdjustAdRevenue(
                                    adUnitId = highFloorAdId,
                                    revenue = adValue.valueMicros / 1_000_000.0,
                                    currency = adValue.currencyCode,
                                    token = AppConstant.AD_IMPRESSION_TOKEN,
                                    appContext = MyApplication.Companion.context
                                )
                            }
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
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
        if (isProVersion.value!=true) {
            if (!showAd)
                return
            val normalLoader = AdLoader.Builder(activity, adId)
                .forNativeAd { ad ->
                    onAdLoaded(ad)
                    ad.setOnPaidEventListener { adValue ->
                        AdBlockerHelper.trackAdjustAdRevenue(
                            adUnitId = adId,
                            revenue = adValue.valueMicros / 1_000_000.0,
                            currency = adValue.currencyCode,
                            token = AppConstant.AD_IMPRESSION_TOKEN,
                            appContext = MyApplication.Companion.context
                        )
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
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
        onLoaded: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null,
        adContainer: FrameLayout
    ) {
        if (isProVersion.value==true) return
        // Try to load the high banner first

        if(showNormalFloor){
            if (showHighFloor) {
                loadBannerAd(
                    activity,
                    adContainer,
                    highFloorAdId,
                    onLoaded = onLoaded,
                    onFailure = {
                        if (showNormalFloor) {
                            loadBannerAd(activity, adContainer, normalAdId, onFailure = onAdFailed,
                                onLoaded = onLoaded
                            )
                        }
                    }
                )
            } else if (showNormalFloor) {
                loadBannerAd(activity, adContainer, normalAdId, onFailure = onAdFailed,
                    onLoaded = onLoaded
                )
            }
        }
    }

    fun loadBannerAd(
        activity: Activity,
        container: FrameLayout,
        adId: String,
        onLoaded: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        // Clear container safely
        try {
            container.removeAllViews()

            // Inflate shimmer
            val shimmerView = LayoutInflater.from(activity)
                .inflate(R.layout.load_fb_banner, container, false)

            val shimmer =
                shimmerView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)

            shimmer?.startShimmer()
            container.addView(shimmerView)

            // Create AdView
            val adView = AdView(activity).apply {
                adUnitId = adId
                setAdSize(
                    getAdaptiveAdSize(
                        activity,
                        container
                    )
                )

                // Revenue tracking (set ONCE)
                onPaidEventListener = OnPaidEventListener { adValue ->
                    val revenue = adValue.valueMicros / 1_000_000.0

                    Singular.adRevenue(
                        SingularAdData(
                            "AdMob",
                            adValue.currencyCode,
                            revenue
                        )
                    )

                    AdBlockerHelper.trackAdjustAdRevenue(
                        adUnitId = adId,
                        revenue = revenue,
                        currency = adValue.currencyCode,
                        token = AppConstant.AD_IMPRESSION_TOKEN,
                        appContext = MyApplication.Companion.context
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
                    Log.e("AdBanner", "Banner failed → ${error.message}")
                }
            }

            adView.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
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
}