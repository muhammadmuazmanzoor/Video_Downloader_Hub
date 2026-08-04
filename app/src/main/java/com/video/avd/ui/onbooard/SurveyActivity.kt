package com.video.avd.ui.onbooard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.util.Prefs
import com.avd.util.AdBlockerHelper.isAdShowing
import com.avd.util.AdBlockerHelper.isProVersion
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.interstitialSurvey
import com.video.avd.ads.AdsHelper.obEnable
import com.video.avd.ads.AdsHelper.obInterstitialEnabled
import com.video.avd.ads.AdsHelper.obInterstitialHighEnabled
import com.video.avd.ads.AdsHelper.obNative4Enabled
import com.video.avd.ads.AdsHelper.obNativeHigh4Enabled
import com.video.avd.ads.LogUtils
import com.video.avd.ads.adjustRevenueMMP
import com.video.avd.databinding.ActivitySurveyBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ads.AppOpenManager.Companion.isShowingAd
import com.video.avd.ui.splash_flow.utils.AppUtils.SURVEY_SESSION
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.changeStatusBarColor
import com.video.avd.utils.AppUtils.hideNavigationBar
import com.video.avd.utils.AppUtils.setLocate
import com.video.avd.utils.FirebaseLogUtils
import com.video.avd.utils.GlobalLoader
import com.video.avd.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class SurveyActivity  : AppCompatActivity()  {


    private  var binding: ActivitySurveyBinding?= null

    private var nativeSurvey: NativeAd? = null

    private var isSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setLocate(this)
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding?.btnNext?.isEnabled=false
        changeStatusBarColor(R.color.black,this, false)
//        FirebaseLogUtils.logEvent("survey_view", "")
        AppUtils.fbEvents("survey_scr_view", "Survey",this)

        if (AdsHelper.surveyNativeHighEnable && AdsHelper.surveyNativeEnable){
            loadNativeSurveyHf()
        }else if (AdsHelper.surveyNativeEnable){
            loadNativeSurvey()
        }else{
            binding?.clbottom?.visibility = View.GONE
        }

       /* if (AdsHelper.surveyInterstitialHighEnabled && AdsHelper.surveyInterstitialEnabled) {
            loadInterSurveyHigh(this)
        } else if (AdsHelper.surveyInterstitialEnabled) {
            loadInterSurvey(this)
        }*/

        binding?.btnNext?.setOnClickListener {
            showInterSurvey(this)
        }
        binding?.clGallery?.setOnClickListener {
            binding?.clGallery?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clEditPhoto?.setOnClickListener {
            binding?.clEditPhoto?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clBodyMaker?.setOnClickListener {
            binding?.clBodyMaker?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
        binding?.clCollage?.setOnClickListener {
            binding?.clCollage?.foreground = ContextCompat.getDrawable(this, R.drawable.bg_rectangled_cl_bordered)
            setSelected()
        }
    }


    private fun setSelected(){
        if(binding?.btnNext?.isEnabled==false) {
            lifecycleScope.launch(Dispatchers.Main) {
                binding?.progress?.visibility=View.VISIBLE
                delay(3000)
                binding?.btnNext?.isEnabled=true
                binding?.progress?.visibility=View.GONE
                binding?.btnNext?.background = resources.getDrawable(R.drawable.bg_gradient_button)
            }
        }

    }

    fun showInterSurvey(
        currentActivity: FragmentActivity,
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value != true) {

                    if (interstitialSurvey != null) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        interstitialSurvey?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isShowingAd = true
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_survey shown",
                                            interstitialSurvey?.adUnitId.toString()
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialSurvey = null

                                    LogUtils.printLog(
                                        "inter_survey failed to shown",
                                        interstitialSurvey?.adUnitId.toString()
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialSurvey = null
                                    isShowingAd = false

                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialSurvey = null

                                }
                            }
                        currentActivity.onBackPressedDispatcher.addCallback(currentActivity) {
                            if (isAdShowing) {
                                // Block back press while ad is showing
                                // Optionally show a toast
                                isEnabled = false
                                Toast.makeText(currentActivity, "Please wait for the ad to finish", Toast.LENGTH_SHORT).show()
                            } else {
                                isEnabled = true
                            }
                        }
                        navigateNext()

                        if (interstitialSurvey != null) {
                            interstitialSurvey?.show(currentActivity)
                        } else {
                            GlobalLoader.hide(currentActivity)

                        }
                        interstitialSurvey = null
                    } else {
                        interstitialSurvey = null

                        navigateNext()

                    }


                } else {

                    navigateNext()

                }
            } catch (e: Exception) {
                navigateNext()
                e.printStackTrace()
            }

        }

    }

    private fun navigateNext(){
        AppUtils.fbEvents("survey_scr_next", "Survey",this)
        Prefs[SURVEY_SESSION] = 1
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }



    private fun loadNativeSurveyHf() {
        if (isProVersion.value != true && NetworkUtils.isOnline(this)) {
            try {
                binding?.clbottom?.visibility = View.VISIBLE
                binding?.shimmer?.visibility = View.VISIBLE
                val adUnitId = BuildConfig.native_survey_hf
                val adLoader =
                    AdLoader.Builder(this@SurveyActivity, adUnitId)
                        .forNativeAd { nativeAd ->
                            nativeSurvey?.destroy()
                            nativeSurvey = nativeAd
                            showNativeSplash()
                            //  nativeSplash = null
                            LogUtils.printLog(
                                "native survey hf  loaded",
                                BuildConfig.native_survey_hf
                            )
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                loadNativeSurvey()
                                LogUtils.printLog(
                                    "native survey hf failed to loaded",
                                    BuildConfig.native_survey_hf
                                )
                            }

                        })
                        .build()

                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else binding?.clbottom?.visibility = View.GONE
    }

    private fun loadNativeSurvey() {
        if (isProVersion.value != true && NetworkUtils.isOnline(this@SurveyActivity)) {
            try {
                binding?.clbottom?.visibility = View.VISIBLE
                val adUnitId = BuildConfig.native_survey
                val adLoader =
                    AdLoader.Builder(this@SurveyActivity, adUnitId) // ✅ use Activity context
                        .forNativeAd { nativeAd ->
                            Log.d("nativeAd", "loaded survey")

                            nativeSurvey?.destroy()
                            nativeSurvey = nativeAd
                            showNativeSplash()
                            //    nativeSplash = null
                            LogUtils.printLog("survey native   loaded", BuildConfig.native_survey)
                        }
                        .withAdListener(object : AdListener() {
                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                binding?.clbottom?.visibility = View.GONE
                                LogUtils.printLog(
                                    "survey native failed to load",
                                    BuildConfig.native_survey
                                )
                            }

                        })
                        .build()

                adLoader.loadAd(AdRequest.Builder().build())
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else binding?.clbottom?.visibility = View.GONE
    }

    private fun showNativeSplash() {
        try {

            nativeSurvey?.let {

                val layoutResId = R.layout.layout_native_ads
                val adView = LayoutInflater.from(this@SurveyActivity)
                    .inflate(layoutResId, null) as NativeAdView

                populateNativeAdView(it, adView)

                binding?.nativeAdView?.removeAllViews()
                binding?.nativeAdView?.addView(adView)
                //  binding?.shimmerContainer?.flShimemr?.shimmerContainerBanner.stopShimmer()
                binding?.shimmer?.visibility = View.GONE
                binding?.nativeAdView?.visibility = View.VISIBLE
                //    nativeBannerSplash = null
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
    ) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        // adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? AppCompatButton)?.text = nativeAd.callToAction
        adView.mediaView?.mediaContent = nativeAd.mediaContent


        adView.setNativeAd(nativeAd)
    }
}
