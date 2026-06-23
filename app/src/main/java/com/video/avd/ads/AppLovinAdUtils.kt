package com.video.avd.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView


object AppLovinAdUtils {

    var isInterstitialAdLoading = false
    private var isStaticInterstitialAdLoading = false
    private var isRewardedAdLoading = false
    var mInterstitialAd: MaxInterstitialAd? = null
    private var mStaticInterstitialAd: MaxInterstitialAd? = null
     var mRewardedAd: MaxRewardedAd? = null
    var nativeAdViewApplovin: MaxNativeAdView?=null
    var nativeAdViewApplovinSmall: MaxNativeAdView?=null
    var nativeAdLoader: MaxNativeAdLoader?=null
    var nativeAdLoadersmall: MaxNativeAdLoader?=null
    var nativeAd: MaxAd? = null
    var nativeAdsmall: MaxAd? = null

    private var onInterstitialAdClosed: (() -> Unit)? = null
    private var onUserRewardedForAd: (() -> Unit)? = null

    fun loadInterstitialAd(context: Activity) {
//        if (!isInterstitialAdLoading && mInterstitialAd == null && maxAdImpressions<remotemaxAdImpressions &&GlobalValues.AdBlockerHelper.isProVersion.value != true && GlobalValues.is24hourEnabled.value==false) {
//            isInterstitialAdLoading = true
//            Log.e("AdsManagerInter", "loadInterstitialAd")
//            mInterstitialAd = MaxInterstitialAd(context.resources.getString(R.string.Interstitial_ID_AppLovin), context)
//            context?.let {contex->
//                mInterstitialAd?.setRevenueListener(RevenueManager.getInstance(contex))
//            }
//            mInterstitialAd?.loadAd()
//
//        }

    }

 fun loadNativeAd(activity: Activity, nativeAdContainer: FrameLayout? = null,goneView:View?=null,timer:View?=null) {
//     if(GlobalValues.AdBlockerHelper.isProVersion.value != true && GlobalValues.is24hourEnabled.value==false){
//         if (nativeAdViewApplovin != null) {
//             // Ad is already loaded, reuse the existing one
//             Log.d("AppLovinNative", "Reusing already loaded native ad")
//             displayNativeAd(nativeAdContainer,goneView,activity,timer)
//             return
//         }
//
//         val adUnitId = activity.resources.getString(R.string.Native_ID_AppLovin)
//         Log.d("AppLovinNative", "Initializing native ad loader")
//         nativeAdLoader = MaxNativeAdLoader(adUnitId, activity)
//
//         val nativeAdView = createNativeAdView(activity) // Call the function here
//
//     /*    nativeAdLoader?.setRevenueListener { p0 -> // AppLovin returns revenue in USD directly as a Double
//             val revenueUsd = p0.revenue
//             // Construct your SingularAdData using "AppLovin" as the network and "USD" as the currency
//             val data = SingularAdData(
//                 "AppLovin",   // Network name
//                 "USD",        // Currency code
//                 revenueUsd    // Revenue amount in USD
//             )
//             // Send the revenue event to Singular
//             Singular.adRevenue(data)
//         }*/
//         nativeAdLoader?.setNativeAdListener(object : MaxNativeAdListener() {
//             override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
//                 Log.d("AppLovinNative", "Native Ad Loaded Successfully")
//                 nativeAdViewApplovin = nativeAdView
//                 if (nativeAdView != null) {
//                     AdBlockerHelper.setNative(
//                         nativeAdView
//                     )
//                 }
//                 nativeAd = ad
//                 displayNativeAd(nativeAdContainer,goneView,activity)
//
//             }
//
//             override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
//                 Log.e("AppLovinNative", "Native Ad failed to load: ${error.message}")
//                 nativeAdViewApplovin = null
//                 goneView?.visibility=View.GONE
//           /*      Handler(Looper.getMainLooper()).postDelayed({
//                     loadNativeAd(activity,nativeAdContainer)
//                 },2500)*/
//
//             }
//
//             override fun onNativeAdClicked(ad: MaxAd) {
//                 Log.d("AppLovinNative", "Native Ad clicked")
//             }
//
//
//         })
//
//         // Load the native ad with the custom native ad view
//         nativeAdLoader?.loadAd(nativeAdView)
//     }

 }
 fun loadNativeAdMusic(activity: Activity, nativeAdContainer: FrameLayout? = null,goneView:View?=null) {
//     if(GlobalValues.AdBlockerHelper.isProVersion.value != true){
//         if (nativeAdViewApplovin != null) {
//             // Ad is already loaded, reuse the existing one
//             Log.d("AppLovinNative", "Reusing already loaded native ad")
//             displayNativeAd(nativeAdContainer,goneView)
//             return
//         }
//
//         val adUnitId = activity.resources.getString(R.string.Native_ID_AppLovin)
//         Log.d("AppLovinNative", "Initializing native ad loader")
//         nativeAdLoader = MaxNativeAdLoader(adUnitId, activity)
//
//         val nativeAdView = createNativeAdView(activity) // Call the function here
//
////         nativeAdLoader?.setRevenueListener { p0 -> // AppLovin returns revenue in USD directly as a Double
////             val revenueUsd = p0.revenue
////             // Construct your SingularAdData using "AppLovin" as the network and "USD" as the currency
////             val data = SingularAdData(
////                 "AppLovin",   // Network name
////                 "USD",        // Currency code
////                 revenueUsd    // Revenue amount in USD
////             )
////             // Send the revenue event to Singular
////             Singular.adRevenue(data)
////         }
//
//         nativeAdLoader?.setNativeAdListener(object : MaxNativeAdListener() {
//             override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
//                 Log.d("AppLovinNative", "Native Ad Loaded Successfully")
//                 nativeAdViewApplovin = nativeAdView
//                 nativeAd = ad
//                 displayNativeAd(nativeAdContainer,goneView)
//
//             }
//
//             override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
//                 Log.e("AppLovinNative", "Native Ad failed to load: ${error.message}")
//                 nativeAdViewApplovin = null
//                 goneView?.visibility=View.GONE
//                 loadNativeAd(activity,nativeAdContainer)
//             }
//
//             override fun onNativeAdClicked(ad: MaxAd) {
//                 Log.d("AppLovinNative", "Native Ad clicked")
//             }
//
//
//         })
//
//         // Load the native ad with the custom native ad view
//         nativeAdLoader?.loadAd(nativeAdView)
//     }

 }

    private fun displayNativeAd(nativeAdContainer: FrameLayout?,goneView:View?=null,activity: Activity?=null,timer:View?=null) {
        if (nativeAdViewApplovin == null) {
            goneView?.visibility=View.INVISIBLE
//            timer?.visibility=View.INVISIBLE
            Log.e("AppLovinNative", "Native Ad View is null, cannot display")
            return
        }
//        activity?.let {contex->
//            nativeAdLoader?.setRevenueListener(RevenueManager.getInstance(contex))
//        }
        // Detach the nativeAdViewApplovin from its previous parent, if any
        val parent = nativeAdViewApplovin?.parent as? ViewGroup
        parent?.removeView(nativeAdViewApplovin)
        // Add the nativeAdViewApplovin to the new container
        nativeAdContainer?.removeAllViews()
        nativeAdContainer?.addView(nativeAdViewApplovin)
        nativeAdContainer?.visibility = View.VISIBLE
        goneView?.visibility=View.INVISIBLE
//        timer?.visibility = View.VISIBLE
        if(nativeAdContainer?.visibility==View.VISIBLE) {
            goneView?.visibility = View.VISIBLE
//            timer?.visibility=View.VISIBLE
        }

    }

    fun destroyNativeAd() {
//        nativeAd?.let { nativeAdLoader?.destroy(it) }
        nativeAdViewApplovin = null
//        nativeAd = null
//        nativeAdLoader = null
    }
    fun destroyNativeAdsmall() {
//        nativeAdsmall?.let { nativeAdLoadersmall?.destroy(it) }
//        nativeAdViewApplovinSmall = null
//        nativeAdsmall= null
//        nativeAdLoadersmall = null
    }

    fun loadNativeAdSmall(
        activity: Activity,
        nativeAdContainer: FrameLayout? = null,
        goneView: View? = null
        ,timer:View?=null
    ) {
//        if(GlobalValues.AdBlockerHelper.isProVersion.value != true && GlobalValues.is24hourEnabled.value==false){
//            // Check if an ad is already loaded to avoid reloading
//            if (nativeAdViewApplovinSmall != null) {
//                Log.d("AppLovinNative", "Reusing already loaded native ad")
//                displayNativeAdSmall(nativeAdContainer, goneView,activity,timer)
//                return
//            }
//
//            // Replace with your AppLovin Native Ad Unit ID
//            val adUnitId = activity.resources.getString(R.string.Native_ID_AppLovin)
//            Log.d("AppLovinNative", "Initializing native ad loader")
//
//            nativeAdLoadersmall = MaxNativeAdLoader(adUnitId, activity)
//
////            nativeAdLoadersmall?.setRevenueListener { p0 ->
////                val revenueUsd = p0.revenue
////                // Construct your SingularAdData using "AppLovin" as the network and "USD" as the currency
////                val data = SingularAdData(
////                    "AppLovin",   // Network name
////                    "USD",        // Currency code
////                    revenueUsd    // Revenue amount in USD
////                )
////                // Send the revenue event to Singular
////                Singular.adRevenue(data)
////            }
//
//            nativeAdLoadersmall?.setNativeAdListener(object : MaxNativeAdListener() {
//                override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
//                    Log.d("AppLovinNative", "Native Ad Loaded Successfully")
//                    // Remove previous native ad, if any
//                    nativeAdViewApplovinSmall = nativeAdView
//                    nativeAdsmall?.let { nativeAdLoadersmall?.destroy(it) }
//                    nativeAdsmall = ad
//
//                    // Display the ad in the container
//                    displayNativeAdSmall(nativeAdContainer, goneView,activity)
//                }
//
//                override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
//                    nativeAdViewApplovinSmall = null
//                    Log.e("AppLovinNative", "Native Ad failed to load: ${error.message}")
//                    goneView?.visibility = View.VISIBLE // Make the alternative view visible
//                 /*   Handler(Looper.getMainLooper()).postDelayed({
//                        loadNativeAdSmall(activity,nativeAdContainer)
//                    },2500)*/
//
//                }
//
//                override fun onNativeAdClicked(ad: MaxAd) {
//                    Log.d("AppLovinNative", "Native Ad clicked")
//                }
//            })
//
//            // Load the native ad with the custom layout
//            val nativeAdView = createNativeAdViewSmall(activity)
//            nativeAdLoadersmall?.loadAd(nativeAdView)
//        }

    }
//
//    private fun displayNativeAdSmall(
//        nativeAdContainer: FrameLayout?,
//        goneView: View?,
//        activity: Activity?=null,
//        timer:View?=null
//    ) {
//        if (nativeAdViewApplovinSmall == null) {
//            Log.e("AppLovinNative", "Native Ad View is null, cannot display")
//            goneView?.visibility = View.VISIBLE // Make the alternative view visible
//            timer?.visibility=View.INVISIBLE
//            return
//        }
//        activity?.let {contex->
//            nativeAdLoadersmall?.setRevenueListener(RevenueManager.getInstance(contex))
//        }
//        // Detach the nativeAdViewApplovinSmall from its previous parent, if any
//        val parent = nativeAdViewApplovinSmall?.parent as? ViewGroup
//        parent?.removeView(nativeAdViewApplovinSmall)
//
//        // Add the nativeAdViewApplovinSmall to the new container
//        nativeAdContainer?.removeAllViews()
//        nativeAdContainer?.addView(nativeAdViewApplovinSmall)
//        nativeAdContainer?.visibility = View.VISIBLE
//        timer?.visibility = View.VISIBLE
//        goneView?.visibility = View.INVISIBLE
//    }
//
//     fun createNativeAdView(activity: Activity): MaxNativeAdView {
//         val binder = MaxNativeAdViewBinder.Builder(R.layout.native_ad_applovinnew)
//             .setTitleTextViewId(R.id.title_text_view)
////             .setBodyTextViewId(R.id.body_text_view)
////             .setStarRatingContentViewGroupId(R.id.star_rating_view)
//             .setAdvertiserTextViewId(R.id.advertiser_textView)
//             .setIconImageViewId(R.id.icon_image_view)
//             .setMediaContentViewGroupId(R.id.media_view_container)
////             .setOptionsContentViewGroupId(R.id.ad_options_view)
//             .setCallToActionButtonId(R.id.cta_button)
//             .build()
//         return MaxNativeAdView(binder, activity)
//    }
//
//    fun createNativeAdViewshorts(activity: Activity): MaxNativeAdView {
//        val binder = MaxNativeAdViewBinder.Builder(R.layout.native_ad_short_applovin)
//            .setTitleTextViewId(R.id.title_text_view)
////            .setBodyTextViewId(R.id.body_text_view)
////            .setStarRatingContentViewGroupId(R.id.star_rating_view)
//            .setAdvertiserTextViewId(R.id.advertiser_textView)
//            .setIconImageViewId(R.id.icon_image_view)
//            .setMediaContentViewGroupId(R.id.media_view_container)
////            .setOptionsContentViewGroupId(R.id.ad_options_view)
//            .setCallToActionButtonId(R.id.cta_button)
//            .build()
//        return MaxNativeAdView(binder, activity)
//    }
//
//     fun createNativeAdViewSmall(activity: Activity): MaxNativeAdView {
//        val binder = MaxNativeAdViewBinder.Builder(R.layout.native_ad_applovinsmall_new)
//            .setTitleTextViewId(R.id.title_text_view)
////            .setBodyTextViewId(R.id.body_text_view)
////            .setStarRatingContentViewGroupId(R.id.star_rating_view)
//            .setAdvertiserTextViewId(R.id.advertiser_textView)
//            .setIconImageViewId(R.id.icon_image_view)
//            .setMediaContentViewGroupId(R.id.media_view_container)
////            .setOptionsContentViewGroupId(R.id.ad_options_view)
//            .setCallToActionButtonId(R.id.cta_button)
//            .build()
//
//        return MaxNativeAdView(binder, activity)
//    }
    fun loadRewardedAd(context: Context) {
//        if (!isRewardedAdLoading && mRewardedAd == null && GlobalValues.is24hourEnabled.value==false) {
//            if(GlobalValues.AdBlockerHelper.isProVersion.value != true){
//                isRewardedAdLoading = true
//                mRewardedAd = MaxRewardedAd.getInstance(context.resources.getString(R.string.rewardedVideo_AppLovin), context as Activity).apply {
//                    setListener(rewardedAdListener)
//                    setRevenueListener(adRevenueListener)
//                    loadAd()
//                }
//                context?.let {contex->
//                    mRewardedAd?.setRevenueListener(RevenueManager.getInstance(contex))
//                }
//            }
//        }
    }

//    fun loadBannerAd(adView: MaxAdView) {
//        if(GlobalValues.AdBlockerHelper.isProVersion.value != true) {
//            adView.setListener(bannerAdListener(adView))
//            adView.setRevenueListener(adRevenueListener)
//            adView.loadAd()
//            adView.visibility = View.GONE  // Hide the ad view initially
//        }
//    }
//
//    fun dpToPx(context: Activity, dp: Float): Int {
//        val density = context.resources.displayMetrics.density
//        return (dp * density + 0.5f).toInt()
//    }
//
//    fun loadBannerAd(context: Activity, frameLayout: FrameLayout) {
////        if(GlobalValues.AdBlockerHelper.isProVersion.value != true && GlobalValues.is24hourEnabled.value==false){
////            try {
////                // Create MaxAdView instance with your Banner Ad Unit ID
////                val adView = MaxAdView(context.resources.getString(R.string.BannerAppLovin), context)
////                context?.let {contex->
////                    adView?.setRevenueListener(RevenueManager.getInstance(contex))
////                }
////                // Set layout parameters for banner size (320x50dp is standard for banners)
////                val bannerWidth = FrameLayout.LayoutParams.MATCH_PARENT
////                val bannerHeight = dpToPx(context,50f)
////
////                adView.layoutParams = FrameLayout.LayoutParams(
////                    bannerWidth,
////                    bannerHeight
////                )
////                // Set listener to track events
////                adView.setListener(bannerAdListener(adView))
////
////                // Add the banner ad view to the FrameLayout
////                frameLayout.addView(adView)
////
////                // Optional: Set background color
////                adView.setBackgroundColor(ContextCompat.getColor(context, R.color.adaptive_ad_bg))
////
////                // Load the ad
////                adView.loadAd()
////
////                // Optionally show the ad view when ready
////                adView.visibility = View.GONE
////
////            } catch (e: Exception) {
////                e.printStackTrace()
////            }
////        }
//
//    }
//
//
//    fun showNormalInterstitialAd(screenName: String,onAdClosed: () -> Unit) {
//        onInterstitialAdClosed = onAdClosed
//        if (mInterstitialAd?.isReady == true && maxAdImpressions <remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true && GlobalValues.is24hourEnabled.value==false) {
//            mInterstitialAd?.showAd()
//            setAdShown(ScreenName.valueOf(screenName), true)
//            splashAdShown=true
//            maxAdImpressions++
//            hideLoading()
//            onInterstitialImpressionSuccess()
//        } else {
//            onInterstitialAdClosed?.invoke()
//        }
//    }
//
//    fun showStaticInterstitialAd(onAdClosed: () -> Unit) {
//        onInterstitialAdClosed = onAdClosed
//        if (mStaticInterstitialAd?.isReady == true && maxAdImpressions<remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
//            mStaticInterstitialAd?.showAd()
//        } else {
//            onInterstitialAdClosed?.invoke()
//        }
//    }
//
//    fun showRewardedAd(onUserRewarded: () -> Unit) {
//        if(GlobalValues.isProVersion.value != true){
//            onUserRewardedForAd = onUserRewarded
//            if (mRewardedAd?.isReady == true) {
//                mRewardedAd?.showAd()
//            } else {
//                onUserRewardedForAd?.invoke()
//            }
//        }
//
//    }
//
//    val interstitialAdListener = object : MaxAdListener {
//        override fun onAdLoaded(ad: MaxAd) {
//            isInterstitialAdLoading = false
//            Log.e("AdsManager", "onAdLoaded")
//        }
//
//        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//            isInterstitialAdLoading = false
//            Log.e("AdsManager", "onAdLoadFailed")
//        }
//
//        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
//            isInterstitialAdLoading = false
//            mInterstitialAd?.loadAd()
//            Log.e("AdsManager", "onAdDisplayFailed")
//        }
//
//        override fun onAdDisplayed(ad: MaxAd) {
//            hideLoading()
//        }
//
//        override fun onAdClicked(ad: MaxAd) {}
//
//        override fun onAdHidden(ad: MaxAd) {
//            Log.e("AdsManager", "onAdHidden")
//            mInterstitialAd?.loadAd()
////            onInterstitialAdClosed?.invoke()  // Call the custom function when interstitial ad is closed
//        }
//    }
//
//    private val staticInterstitialAdListener = object : MaxAdListener {
//        override fun onAdLoaded(ad: MaxAd) {
//            isStaticInterstitialAdLoading = false
//        }
//
//        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//            isStaticInterstitialAdLoading = false
//        }
//
//        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
//            isStaticInterstitialAdLoading = false
//            mStaticInterstitialAd?.loadAd()
//        }
//
//        override fun onAdDisplayed(ad: MaxAd) {
//            hideLoading()
//        }
//
//        override fun onAdClicked(ad: MaxAd) {}
//
//        override fun onAdHidden(ad: MaxAd) {
//            mStaticInterstitialAd?.loadAd()
////            onInterstitialAdClosed?.invoke()  // Call the custom function when static interstitial ad is closed
//        }
//    }
//
//    private val rewardedAdListener = object : MaxRewardedAdListener {
//        override fun onAdLoaded(ad: MaxAd) {
//            isRewardedAdLoading = false
//        }
//
//        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//            isRewardedAdLoading = false
//        }
//
//        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
//            isRewardedAdLoading = false
//            mRewardedAd?.loadAd()
//        }
//
//        override fun onAdDisplayed(ad: MaxAd) {}
//
//        override fun onAdClicked(ad: MaxAd) {}
//
//        override fun onAdHidden(ad: MaxAd) {
//            mRewardedAd?.loadAd()
//        }
//
//        override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
//            onUserRewardedForAd?.invoke()  // Call the custom function when the user is rewarded
//        }
//
//         override fun onRewardedVideoStarted(p0: MaxAd) {
//         }
//
//         override fun onRewardedVideoCompleted(p0: MaxAd) {
//         }
//    }

//    private fun bannerAdListener(adView: MaxAdView) = object : MaxAdViewAdListener {
//        override fun onAdLoaded(ad: MaxAd) {
//            adView.visibility = View.VISIBLE  // Show the ad view when the ad is loaded
//            Log.e("AdsManager", "Banner onAdLoaded")
//        }
//
//        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
//            adView.visibility = View.GONE  // Hide the ad view if the ad fails to load
//            Log.e("AdsManager", "Banner onAdLoadFailed")
//        }
//
//        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {}
//
//        override fun onAdDisplayed(ad: MaxAd) {
//            Log.e("AdsManager", "Banner onAdDisplayed")
//        }
//
//        override fun onAdClicked(ad: MaxAd) {}
//
//        override fun onAdExpanded(ad: MaxAd) {}
//
//        override fun onAdCollapsed(ad: MaxAd) {}
//
//        override fun onAdHidden(ad: MaxAd) {}
//    }
//
//    private val adRevenueListener = MaxAdRevenueListener { ad ->
//    }
}
