package com.video.avd.ads

import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.video.avd.ads.AdsHelper.splashInterstitialEnabled
import com.video.avd.ads.AdsHelper.splashInterstitialHighEnabled
import com.video.avd.ads.AdsHelper.surveyInterstitialHighEnabled
import com.video.avd.ads.AdsHelper.surveyNativeHighEnable


@Keep
object RemoteJsonConvertor {

    fun splashJsonConvertor(splashJson: String) {
        try {
            val config = Gson().fromJson(splashJson, SplashAdConfig::class.java)

            // Interstitial
            splashInterstitialHighEnabled = config.Interstitialhigh == 1
            splashInterstitialEnabled = config.Interstitialall == 1


            // Banner
            AdsHelper.splashBannerEnabled = config.Banner == 1
            AdsHelper.splashBannerHighEnabled = config.Bannerhigh == 1

            // Native
            AdsHelper.splashNativeEnabled = config.Native == 1
            AdsHelper.splashNativeHighEnabled = config.Nativehigh == 1

            AdsHelper.splashNativeFormat = config.NativeFormat

            // CTA
            AdsHelper.splashCtaColor = config.Ctacolor
            AdsHelper.splashCtaTextColor = config.CtatextColor
            AdsHelper.splashCtaTextStyle = config.CtatextStyle
            Log.e("SplashInterstitial", "Remote config loaded: $config")
            Log.d("SplashInterstitial", "requestingAllAds: $splashInterstitialHighEnabled   $splashInterstitialEnabled")
        } catch (e: Exception) {
            Log.e("AdsManager", "JSON parse error", e)
        }
    }

    fun langJsonConvertor(splashJson: String) {
        try {
            val config = Gson().fromJson(splashJson, LangAdConfig::class.java)

            AdsHelper.langSessionRemote = config.SessionCount

            AdsHelper.languageButtonDelay = config.LanguageButtonDelay
            AdsHelper.languageButtonStyle = config.LanguageButton

            /*// Interstitial
            AdsHelper.langInterstitialHighEnabled = config.InterstitialHigh == 1
            AdsHelper.langInterstitialEnabled = config.InterstitialAll == 1*/


            // Native
            AdsHelper.langNative1Enabled = config.NativeAll1 == 1
            AdsHelper.langNativeHigh1Enabled = config.NativeHigh1 == 1
            AdsHelper.langNative2Enabled = config.NativeAll2 == 1
            AdsHelper.langNativeHigh2Enabled = config.NativeHigh2 == 1

            AdsHelper.langNativeFormat = config.NativeFormat

            // CTA
            AdsHelper.langCtaColor = config.AdCtaColor
            AdsHelper.langCtaTextColor = config.AdCtaTextColor
            AdsHelper.langCtaTextStyle = config.CtaTextStyle


        } catch (e: Exception) {
            Log.e("AdsManager", "JSON parse error", e)
        }
    }

    fun obJsonConvertor(splashJson: String) {
        try {
            val config = Gson().fromJson(splashJson, OnboardingAdsConfig::class.java)


            /* AdsHelper.obFirstEnable = config.Onboarding1 == 1
             AdsHelper.obSecondEnable = config.Onboarding2 == 1
             AdsHelper.obThirdEnable = config.Onboarding3 == 1*/

            AdsHelper.surveyEnable = config.surveyEnable == 1

            AdsHelper.surveyNativeHighEnable = config.surveyNativeHigh == 1
            AdsHelper.surveyNativeEnable = config.surveyNative == 1
            Log.e("checkSurvey","native high:$surveyNativeHighEnable")


            AdsHelper.surveyInterstitialHighEnabled = config.surveyInterstitialHigh == 1
            AdsHelper.surveyInterstitialEnabled = config.surveyInterstitialAll == 1
            Log.e("checkSurvey","inter high: $surveyInterstitialHighEnabled")
            AdsHelper.obEnable = config.ObEnable == 1


            AdsHelper.obNative1Enabled = config.ObNativeAll1 == 1
            AdsHelper.obNative3Enabled = config.ObNativeAll3 == 1
            AdsHelper.obNative4Enabled = config.ObNativeAll4 == 1

            AdsHelper.obNativeHigh1Enabled = config.ObNativeHigh1 == 1
            AdsHelper.obNativeHigh3Enabled = config.ObNativeHigh3 == 1
            AdsHelper.obNativeHigh4Enabled = config.ObNativeHigh4 == 1

            AdsHelper.obNativeHighFullScr1Enabled = config.FullScreenNativeHigh1 == 1
            AdsHelper.obNativeHighFullScr2Enabled = config.FullScreenNativeHigh2 == 1


            AdsHelper.obNativeFullScr1Enabled = config.FullScreenNativeAll1 == 1
            AdsHelper.obNativeFullScr2Enabled = config.FullScreenNativeAll2 == 1

            AdsHelper.obInterstitialHighEnabled = config.ObInterstitialHigh == 1
            AdsHelper.obInterstitialEnabled = config.ObInterstitialAll == 1


            AdsHelper.featureNative1Enabled = config.FeatureNativeAll1 == 1
            AdsHelper.featureNativeHigh1Enabled = config.FeatureNativeHigh1 == 1



            AdsHelper.obNativeFormat = config.NativeFormat
            // CTA
            AdsHelper.obCtaColor = config.AdCtaColor
            AdsHelper.obCtaTextColor = config.AdCtaTextColor
            AdsHelper.obCtaTextStyle = config.CtaTextStyle
            AdsHelper.isShowNativeFullCross = config.ShowNativeFullCross
            AdsHelper.nativeFullCrossDelay = config.NativeFullCrossDelay

            Log.e("ObInterstitial", "Remote config loaded: $config")
        } catch (e: Exception) {
            Log.e("AdsManager", "JSON parse error", e)
        }
    }

}