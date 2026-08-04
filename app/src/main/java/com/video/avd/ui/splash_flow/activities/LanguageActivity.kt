package com.video.avd.ui.splash_flow.activities

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.avd.util.Prefs
import com.avd.util.AdBlockerHelper.isProVersion
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.BuildConfig
import com.video.avd.MyApplication.Companion.context
import com.video.avd.R
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.interstitialSurvey
import com.video.avd.ads.AdsHelper.langCtaColor
import com.video.avd.ads.AdsHelper.langCtaTextColor
import com.video.avd.ads.AdsHelper.langCtaTextStyle
import com.video.avd.ads.AdsHelper.langNative1Enabled
import com.video.avd.ads.AdsHelper.langNative2Enabled
import com.video.avd.ads.AdsHelper.langNativeAd1
import com.video.avd.ads.AdsHelper.langNativeAd2
import com.video.avd.ads.AdsHelper.langNativeAdHigh1
import com.video.avd.ads.AdsHelper.langNativeAdHigh2
import com.video.avd.ads.AdsHelper.langNativeFormat
import com.video.avd.ads.AdsHelper.langNativeHigh1Enabled
import com.video.avd.ads.AdsHelper.langNativeHigh2Enabled
import com.video.avd.ads.AdsHelper.languageButtonDelay
import com.video.avd.ads.AdsHelper.languageButtonStyle
import com.video.avd.ads.AdsHelper.loadWithFallback
import com.video.avd.ads.AdsHelper.native_language
import com.video.avd.ads.AdsHelper.native_ob1
import com.video.avd.ads.LogUtils
import com.video.avd.ads.adjustRevenueMMP
import com.video.avd.databinding.ActivityLanguageBinding
import com.video.avd.ui.languages.LanguageSelectionAdapter
import com.video.avd.ui.languages.LanguageSelectionModel
import com.video.avd.ui.onbooard.OnboardingActivity
import com.video.avd.ui.onbooard.SurveyActivity
import com.video.avd.ui.MainActivity
import com.video.avd.ui.splash_flow.utils.AppUtils.getMediationInfo
import com.video.avd.ui.splash_flow.utils.AppUtils.hideNavigationBar
import com.video.avd.ui.splash_flow.utils.AppUtils.LANG_SESSION
import com.video.avd.ui.splash_flow.utils.AppUtils.shouldNavigateToOnboarding
import com.video.avd.ui.splash_flow.utils.AppUtils.shouldNavigateToSurvey
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.setLocate
import com.video.avd.utils.GlobalValues.fromSplash
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity(),
    LanguageSelectionAdapter.LanguageSelectionClickListener {
    private var binding: ActivityLanguageBinding? = null
    private var selectedLanguage = "none"
    private var isShow = false
    private var isSettingsFlow = false
    private var languageAdapter: LanguageSelectionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocate(this)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        AppUtils.changeStatusBarColor(R.color.black, this@LanguageActivity, true)
        binding?.main?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        binding?.lottieAnimationView?.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER
        ) {
            PorterDuffColorFilter(
                ContextCompat.getColor(this, R.color.bottom_nav_selected),
                PorterDuff.Mode.SRC_ATOP
            )
        }
        isSettingsFlow = intent.getBooleanExtra(EXTRA_OPENED_FROM_SETTINGS, false) || !fromSplash
        setupHeader()
        AppUtils.fbEvents("language_view", "Language",this)
        doneButtonDisableStyle()
        setDataAndAdapter()
        langAltAds()
        if (isProVersion.value==true) {
            binding?.clbottom?.visibility = View.GONE
        } else {
            binding?.clbottom?.visibility = View.VISIBLE
        }

        handleBackPress()
        binding?.btnDone?.setOnClickListener {
            AppUtils.fbEvents("language_click_next", "Language",this)
            doneAndNavigate()
        }

        binding?.icBtnDone?.setOnClickListener {
            AppUtils.fbEvents("language_click_next", "Language",this)
            doneAndNavigate()
        }
        try {
            native_language.observe(this){
                if(it==true){
                    if(langNative1Enabled!=false) {
                        if (langNativeAdHigh1 != null) {
                            displayNative(langNativeAdHigh1)
                        } else if (langNativeAd1 != null) {
                            displayNative(langNativeAd1)
                        }
                        else {
                            binding?.shimmer?.visibility = View.GONE
                        }
                    }
                }
                else if(it==false){
                    binding?.shimmer?.visibility = View.GONE
                }
            }
        }
        catch (e: Exception) {
            if(langNative1Enabled!=false) {
                if (langNativeAdHigh1 != null) {
                    displayNative(langNativeAdHigh1)
                } else if (langNativeAd1 != null) {
                    displayNative(langNativeAd1)
                }
                else {
                    binding?.shimmer?.visibility = View.GONE
                }
            }
            e.printStackTrace()
        }
        binding?.langLoading?.visibility = View.VISIBLE
        if (isProVersion.value!=true) {
            lifecycleScope.launch {
                delay(3500)
                binding?.langLoading?.visibility = View.GONE
            }

        } else {
            binding?.langLoading?.visibility = View.GONE
            binding?.shimmer?.visibility = View.GONE
        }
    }

    private fun setupHeader() {
        binding?.tvTitle?.text = getString(
            if (isSettingsFlow) R.string.change_language else R.string.select_language
        )
        binding?.ivBack?.visibility = if (isSettingsFlow) View.VISIBLE else View.GONE
        binding?.ivBack?.setOnClickListener {
            finish()
        }
    }

    private fun setDataAndAdapter() {
        val list = arrayListOf<LanguageSelectionModel>()
        list.add(LanguageSelectionModel("ar", "العربية", false, resources.getDrawable(R.drawable.ic_saudia)))
        list.add(LanguageSelectionModel("ko", "한국인", false,resources.getDrawable(R.drawable.ic_korea)))
        list.add(LanguageSelectionModel("en", "English", false, resources.getDrawable(R.drawable.ic_us)))
        list.add(LanguageSelectionModel("ja", "日本語", false,resources.getDrawable(R.drawable.ic_japan)))
        list.add(LanguageSelectionModel("es", "Español", false,resources.getDrawable(R.drawable.ic_spain)))
        list.add(LanguageSelectionModel("in", "Indonesian", false,resources.getDrawable(R.drawable.ic_indonesia)))
        list.add(LanguageSelectionModel("pt", "Portuguese", false,resources.getDrawable(R.drawable.ic_portugal)))
        list.add(LanguageSelectionModel("fr", "Français", false,resources.getDrawable(R.drawable.ic_france)))
        list.add(LanguageSelectionModel("vi", "Tiếng Việt", false,resources.getDrawable(R.drawable.ic_vietnam)))
        list.add(LanguageSelectionModel("ru", "Русский", false,resources.getDrawable(R.drawable.ic_russia)))
        list.add(LanguageSelectionModel("tr", "Türkçe", false,resources.getDrawable(R.drawable.ic_turkey)))
        list.add(LanguageSelectionModel("ms", "Melayu", false,resources.getDrawable(R.drawable.ic_malaysia)))
        list.add(LanguageSelectionModel("th", "แบบไทย", false,resources.getDrawable(R.drawable.ic_thailand)))
        list.add(LanguageSelectionModel("pl", "Polski", false,resources.getDrawable(R.drawable.ic_poland)))
        for (item in list) {
            if (item.lang == getAutoLanguage(this)) {
                item.name += " ( Auto )"
                break
            }
        }
        val savedLanguage = AppPreference.getLanguage(this) ?: Locale.getDefault().language
        selectedLanguage = savedLanguage
        val appLanguage = Locale.getDefault().language
        var itemToInsert: LanguageSelectionModel? = null
        for (item in list) {
            if (item.lang == appLanguage) {
                item.isSelected = false
                itemToInsert = item
                break
            }
        }
        if (itemToInsert != null) {
            list.remove(itemToInsert)
            list.add(2, itemToInsert)
        }

        list.firstOrNull { it.lang == savedLanguage }?.isSelected = true


        val adapter = LanguageSelectionAdapter(list, this)
        languageAdapter = adapter
        binding?.rvLanguage?.layoutManager = LinearLayoutManager(this)
        binding?.rvLanguage?.adapter = adapter
    }

    private fun getAutoLanguage(activity: FragmentActivity): String {
        val systemLan = AppPreference.getSystemDefaultLanguage(activity)
        val supportedLangs =
            listOf("en","ar", "ko", "ja", "es", "in", "pt", "fr", "vi", "ru", "tr", "ms", "th", "pl")
        var lange = if (systemLan in supportedLangs) systemLan else "en"
        return lange ?: "en"
    }

    fun doneAndNavigate() {
        if (selectedLanguage == "none") {
            Toast.makeText(
                this,
                "Please Select language",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        AppUtils.firebaseUserAction("Langu_done_click", "languagescreen")
        navToNext()
    }

    fun navToNext() {
        AppPreference.saveLanguage(this, selectedLanguage)
        Prefs[LANG_SESSION] = 1
        if (isSettingsFlow) {
            finish()
            return
        }
        val nextActivity = when {
            shouldNavigateToOnboarding() -> OnboardingActivity::class.java
            shouldNavigateToSurvey() -> SurveyActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, nextActivity))
        finish()
    }
    fun loadInterSurveyHigh(
        context: Context
    ) {
        if (isProVersion.value ==true || interstitialSurvey!=null) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, BuildConfig.inter_survey_high, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialSurvey = ad
                ad?.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        adjustRevenueMMP(
                            ad?.adUnitId,
                            adValue.valueMicros / 1_000_000.0,
                            adValue.currencyCode,
                            "",
                            "Interstitial"
                        )
                    }
                LogUtils.printLog("home_inter hf loaded", BuildConfig.inter_survey_high)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadInterSurvey(context)
                interstitialSurvey = null
                LogUtils.printLog("home_inter hf failed", BuildConfig.inter_survey_high)
            }
        })
    }

    fun loadInterSurvey(
        context: Context
    ) {
        if (isProVersion.value == true || interstitialSurvey!=null) return
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, BuildConfig.inter_survey, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialSurvey = ad
                ad?.onPaidEventListener =
                    OnPaidEventListener { adValue ->
                        adjustRevenueMMP(
                            ad?.adUnitId,
                            adValue.valueMicros / 1_000_000.0,
                            adValue.currencyCode,
                            "",
                            "Interstitial"
                        )
                    }
                //  showInterSplash(context,false)
                LogUtils.printLog("surveyInter  loaded", BuildConfig.inter_survey)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialSurvey = null
                LogUtils.printLog("surveyInter  failed", BuildConfig.inter_survey)
            }
        })
    }
    fun doneButtonDisableStyle() {
        when (languageButtonStyle) {
            1 -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.round_onboarding_button
                    )
                    backgroundTintList = null
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.brand_text_primary))

                    visibility = View.VISIBLE
                }
            }

            2 -> {
                binding?.icBtnDone?.apply {
                    setImageDrawable(
                        ContextCompat.getDrawable(
                            this@LanguageActivity,
                            R.drawable.ic_done_disable
                        )
                    )
                    visibility = View.VISIBLE
                }
            }

            3 -> {
                binding?.btnDone?.apply {
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.bottom_nav_selected))
                    visibility = View.VISIBLE
                }
            }

            else -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.round_onboarding_button
                    )
                    backgroundTintList = null
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.brand_text_primary))
                    visibility = View.VISIBLE
                }
            }
        }
    }

    fun doneButtonStyle() {
        binding?.progressBar?.visibility = View.GONE
        when (languageButtonStyle) {
            1 -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.round_onboarding_button2
                    )
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@LanguageActivity, R.color.bottom_nav_selected)
                    )
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.white))

                    visibility = View.VISIBLE
                }
            }

            2 -> {
                binding?.icBtnDone?.apply {
                    setImageDrawable(
                        ContextCompat.getDrawable(
                            this@LanguageActivity,
                            R.drawable.ic_done
                        )
                    )
                    visibility = View.VISIBLE
                }
            }

            3 -> {
                binding?.btnDone?.apply {
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.bottom_nav_selected))
                    visibility = View.VISIBLE
                }
            }

            else -> {
                binding?.btnDone?.apply {
                    background = ContextCompat.getDrawable(
                        this@LanguageActivity,
                        R.drawable.round_onboarding_button2
                    )
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this@LanguageActivity, R.color.bottom_nav_selected)
                    )
                    setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.white))
                    visibility = View.VISIBLE
                }
            }
        }
    }


    override fun onLanguageClick(language: LanguageSelectionModel?) {
        mSelectedLanguage = language?.lang ?: "en"
        selectedLanguage = language?.lang ?: "en"
        languageAdapter?.stopStartupHighlight()

        if (!isShow) {
            loadOB1Ads()
            isShow = true
            showLangNative2()

            binding?.progressBar?.visibility = View.VISIBLE
            binding?.btnDone?.visibility = View.INVISIBLE
            binding?.icBtnDone?.visibility = View.INVISIBLE

            lifecycleScope.launch {
                delay(1000 * languageButtonDelay.toLong())
                doneButtonStyle()
            }
        } else {
            doneButtonStyle()
        }
    }

    private fun handleBackPress() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSettingsFlow) {
                    finish()
                } else if (!isFinishing && !isDestroyed) {
                    doneAndNavigate()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    fun showLangNative2() {
        if (langNativeAdHigh2 != null && langNativeHigh2Enabled) {
            displayNative(langNativeAdHigh2)
        } else if (langNativeAd2 != null && langNative2Enabled) {
            displayNative(langNativeAd2)
        } else {
            binding?.apply {
                shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.INVISIBLE
                }
                shimmer.visibility = View.GONE
            }
        }
    }

    fun langAltAds() {
        // Language Native Ad #2
        if (langNativeAdHigh2 == null) {
            loadWithFallback(
                activity = this,
                highFloorAdId = BuildConfig.native_language_alt_high,
                normalAdId = BuildConfig.native_language_alt,
                showHighfloor = langNativeHigh2Enabled,
                showNormalfloor = langNative2Enabled,
                onAdLoadedHigh = {
                    langNativeAdHigh2 = it
                   /* if (isShow) {
                        showLangNative2()
                    }*/
                },
                onAdLoadedNormal = {
                    langNativeAd2 = it
                    /*if (isShow) {
                        showLangNative2()
                    }*/
                },
                onAdFailed = {}
            )
        }
        if (AdsHelper.surveyEnable){
            loadInterSurveyHigh(this)}
    }

    fun loadOB1Ads() {
        // ONBOARDING 1
        if (AdsHelper.obFirstEnable && fromSplash) {

            // Native 1
            if (AdsHelper.obNativeAdHigh1 == null) {
                loadWithFallback(
                    activity = this,
                    highFloorAdId = resources.getString(R.string.native_ob1_high),
                    normalAdId = resources.getString(R.string.native_ob1),
                    showHighfloor = AdsHelper.obNativeHigh1Enabled,
                    showNormalfloor = AdsHelper.obNative1Enabled,
                    onAdLoadedHigh = { AdsHelper.obNativeAdHigh1 = it
                        native_ob1.postValue(true)},
                    onAdLoadedNormal = { AdsHelper.obNativeAd1 = it
                        native_ob1.postValue(true)},
                    onAdFailed = {
                        native_ob1.postValue(false)
                    }
                )
            }

            // Fullscreen 1
            loadWithFallback(
                activity = this,
                highFloorAdId = resources.getString(R.string.native_full_ob1_high),
                normalAdId = resources.getString(R.string.native_full_ob1),
                showHighfloor = AdsHelper.obNativeHighFullScr1Enabled,
                showNormalfloor = AdsHelper.obNativeFullScr1Enabled,
                onAdLoadedHigh = {
                    AdsHelper.obNativeAdHighFullScr1 = it
                    AdsHelper.obFull1Ready()
                },
                onAdLoadedNormal = {
                    AdsHelper.obNativeAdFullScr1 = it
                    AdsHelper.obFull1Ready()
                },
                onAdFailed = {}
            )
            // Fullscreen 2
            loadWithFallback(
                activity = this,
                highFloorAdId = resources.getString(R.string.native_full_ob2_high),
                normalAdId = resources.getString(R.string.native_full_ob2),
                showHighfloor = AdsHelper.obNativeHighFullScr2Enabled,
                showNormalfloor = AdsHelper.obNativeFullScr2Enabled,
                onAdLoadedHigh = {
                    AdsHelper.obNativeAdHighFullScr2 = it
                    AdsHelper.obFull1Ready()
                },
                onAdLoadedNormal = {
                    AdsHelper.obNativeAdFullScr2 = it
                    AdsHelper.obFull1Ready()
                },
                onAdFailed = {}
            )
        }
    }


    private fun displayNative(nativeAd: NativeAd?) {
        nativeAd ?: return
        try {
            val layoutResId = when (getMediationInfo(nativeAd)) {
                "meta" -> R.layout.layout_native_ads_meta
                else -> when (langNativeFormat) {
                    1 -> R.layout.layout_native_ads_without_mediaview
                    2 -> R.layout.layout_native_ads
                    3 -> R.layout.layout_native_ads_ctr_up
                    else -> R.layout.layout_native_ads_without_mediaview
                }
            }

            val adView = LayoutInflater.from(this)
                .inflate(layoutResId, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            binding?.nativeAdView?.apply {
                removeAllViews()
                addView(adView)
                visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            e.printStackTrace()
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

                backgroundTintList = ColorStateList.valueOf(Color.parseColor(langCtaColor))
                typeface = if (langCtaTextStyle.equals("bold", ignoreCase = true))
                    ResourcesCompat.getFont(context, R.font.poppins_bold)
                else
                    ResourcesCompat.getFont(context, R.font.poppins_regular)
                setTextColor(
                    Color.parseColor(langCtaTextColor)   // e.g. "#FFFFFF"
                )
            }
            mediaView?.mediaContent = nativeAd.mediaContent
            setNativeAd(nativeAd)
            binding?.apply {
                shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                    // When data is loaded (e.g., ad is ready), stop shimmer
                    shimmerLayout.stopShimmer()
                    shimmerLayout.visibility = View.GONE
                    binding?.nativeAdView?.visibility = View.VISIBLE  // Optional: hide shimmer
                }
                shimmer.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val EXTRA_OPENED_FROM_SETTINGS = "opened_from_settings"
        var mSelectedLanguage = "none"

        fun createIntent(context: Context, openedFromSettings: Boolean): Intent {
            return Intent(context, LanguageActivity::class.java).apply {
                putExtra(EXTRA_OPENED_FROM_SETTINGS, openedFromSettings)
            }
        }
    }
}



