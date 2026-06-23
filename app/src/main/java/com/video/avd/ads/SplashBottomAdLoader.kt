package com.video.avd.ads

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper.isProVersion
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.ui.splash_flow.utils.AppUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashBottomAdLoader(
    private val activity: FragmentActivity,
    private val adContainer: FrameLayout
) {

    private var shimmer: ShimmerFrameLayout? = null

    init {
        loadBottomAd()
    }


    fun loadBottomAd() {
        if (isProVersion.value==true) return
        adContainer.removeAllViews()


        when {
            AdsHelper.splashBannerHighEnabled || AdsHelper.splashBannerEnabled -> {
                activity.lifecycleScope.launch {
                    delay(1000)
                    AdsHelper.loadBanner(
                        activity = activity,
                        highFloorAdId = BuildConfig.banner_home_high,
                        normalAdId = BuildConfig.banner_home,
                        showHighFloor = AdsHelper.splashBannerHighEnabled,
                        showNormalFloor = AdsHelper.splashBannerEnabled,
                        onLoaded = {},
                        onAdFailed = { hideShimmer() },
                        adContainer = adContainer
                    )
                }
            }

            AdsHelper.splashNativeHighEnabled || AdsHelper.splashNativeEnabled -> {
                showShimmer()
                AdsHelper.loadWithFallback(
                    activity = activity,
                    highFloorAdId = BuildConfig.native_splash_high,
                    normalAdId = BuildConfig.native_splash,
                    showHighfloor = AdsHelper.splashNativeHighEnabled,
                    showNormalfloor = AdsHelper.splashNativeEnabled,
                    onAdLoadedHigh = ::displayNative,
                    onAdLoadedNormal = ::displayNative,
                    onAdFailed = { hideShimmer() }
                )
            }

            else -> {
                hideShimmer()
                Log.e("SplashBottomAd", "No splash ad enabled")
            }
        }
    }


    private fun displayNative(nativeAd: NativeAd?) {
        nativeAd ?: return
        if (activity.isFinishing || activity.isDestroyed) return // <- SAFETY CHECK
        hideShimmer()

        try {
            val layoutResId = when (AppUtils.getMediationInfo(nativeAd)) {
                "meta" -> R.layout.layout_native_ads_meta
                else -> when (AdsHelper.splashNativeFormat) {
                    1 -> R.layout.layout_native_ads_without_mediaview
                    2 -> R.layout.layout_native_ads
                    3 -> R.layout.layout_native_ads_ctr_up
                    else -> R.layout.layout_native_ads_without_mediaview
                }
            }

            val adView = LayoutInflater.from(activity)
                .inflate(layoutResId, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            adContainer.apply {
                removeAllViews()
                addView(adView)
                visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------------- SHIMMER -------------------------
    private fun showShimmer() {
        val shimmerLayout = when {
            AdsHelper.splashNativeFormat == 1 -> R.layout.layout_loading_ads_native_small
            else -> R.layout.layout_loading_ads_native_large
        }

        val shimmerView = LayoutInflater.from(activity)
            .inflate(shimmerLayout, null)

        shimmer = shimmerView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
            ?.apply { startShimmer() }
        adContainer.addView(shimmerView)
    }

    private fun hideShimmer() {
        shimmer?.apply {
            stopShimmer()
            visibility = View.GONE
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.apply {
            headlineView = findViewById(R.id.ad_headline)
            bodyView = findViewById(R.id.ad_body)
            callToActionView = findViewById(R.id.ad_call_to_action)
            mediaView = findViewById(R.id.ad_media)

            (headlineView as? TextView)?.text = nativeAd.headline
            (bodyView as? TextView)?.text = nativeAd.body
            (callToActionView as? AppCompatButton)?.apply {
                text = nativeAd.callToAction

                backgroundTintList = ColorStateList.valueOf(Color.parseColor(AdsHelper.splashCtaColor))
                typeface = if (AdsHelper.splashCtaTextStyle.equals("bold", ignoreCase = true))
                    ResourcesCompat.getFont(context, R.font.poppins_bold)
                else
                    ResourcesCompat.getFont(context, R.font.poppins_regular)
                setTextColor(
                    Color.parseColor(AdsHelper.splashCtaTextColor)   // e.g. "#FFFFFF"
                )
            }
            mediaView?.mediaContent = nativeAd.mediaContent
            setNativeAd(nativeAd)
        }
    }

    fun cleanup() {
        adContainer.removeAllViews()
        shimmer?.stopShimmer()
        shimmer = null
    }
}