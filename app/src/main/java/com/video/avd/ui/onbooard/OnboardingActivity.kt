package com.video.avd.ui.onbooard

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.avd.util.DataStoreManager
import com.avd.util.Prefs
import com.video.avd.R
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.loadWithFallback
import com.video.avd.ads.AdsHelper.native_ob4
import com.video.avd.ads.AdsHelper.obInterstitialEnabled
import com.video.avd.ads.AdsHelper.obInterstitialHighEnabled
import com.video.avd.ads.loadInterOb
import com.video.avd.ads.loadInterObHigh
import com.video.avd.databinding.ActivityOnboardingBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ui.splash_flow.activities.LanguageActivity
import com.video.avd.ui.splash_flow.utils.AppUtils.OB_SESSION
import com.video.avd.ui.splash_flow.utils.AppUtils.hideNavigationBar
import com.video.avd.ui.splash_flow.utils.AppUtils.shouldNavigateToSurvey
import com.video.avd.utils.AppUtils.IS_LANGUAGE
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity(), PagerNav {
    private var binding: ActivityOnboardingBinding? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private val pagerAdapter by lazy { OnboardingViewPager(this) }

    /*  companion object {
          var selectedPosition = MutableLiveData(0)
      }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocate(this)
        //   enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()

        try {
          /*  binding?.main?.let {
                ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }*/

        } catch (e: Exception) {
            e.printStackTrace()
        }
        handleBackPress()
        loadOB4ds()
        setAdapter()
    }

    fun setAdapter() {
        binding?.vpOnboard?.adapter = pagerAdapter
        AdsHelper.obFull1Loaded.observe(this) { if (it) pagerAdapter.refresh() }
        AdsHelper.obFull2Loaded.observe(this) { if (it) pagerAdapter.refresh() }

    }

    fun setLocate(activity: Activity) {
        var lang = Locale.getDefault().language //System Default Language
        dataStoreManager.readDataStoreValue(IS_LANGUAGE, "") {
            Log.e("Languageset", this.toString())
            //  val langnew = this
            val langnew = LanguageActivity.mSelectedLanguage
            if (langnew == "") {
                val supportedLangs = listOf(
                    "ja",
                    "es",
                    "in",
                    "hi",
                )

                // Check if the system language is in the list of supported languages, else default to English
                var lange = if (lang in supportedLangs) lang else "en"
                lang = lange
            } else {
                lang = langnew
            }
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            activity.baseContext.resources.updateConfiguration(
                config,
                activity.baseContext.resources.displayMetrics
            )
        }
    }

    fun loadOB4ds() {

        if (AdsHelper.obFourthEnable) {
            if (AdsHelper.obNativeAdHigh4 == null) {
                loadWithFallback(
                    activity = this,
                    highFloorAdId = getString(R.string.native_ob4_high),
                    normalAdId = getString(R.string.native_ob4),
                    showHighfloor = AdsHelper.obNativeHigh4Enabled,
                    showNormalfloor = AdsHelper.obNative4Enabled,
                    onAdLoadedHigh = { AdsHelper.obNativeAdHigh4 = it
                        native_ob4.postValue(true)},
                    onAdLoadedNormal = { AdsHelper.obNativeAd4 = it
                        native_ob4.postValue(true)
                                       },
                    onAdFailed = {
                        native_ob4.postValue(false)

                    }
                )
            }
        }

        if (obInterstitialHighEnabled && obInterstitialEnabled){
            loadInterObHigh(this)
        }else if (obInterstitialEnabled){
            loadInterOb(this)
        }

    }

    override fun goNext() {
        val next = binding?.vpOnboard?.currentItem?.plus(1)
        val total = pagerAdapter.itemCount

        if (next != null) {
            if (next < total) {
                binding?.vpOnboard?.setCurrentItem(next, true)
            } else {
                navigateNext()
            }
        }
    }

    private fun handleBackPress() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isFinishing && !isDestroyed) {
                    navigateNext()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    fun navigateNext(){
        Prefs[OB_SESSION] = 1
        val nextActivity = if (shouldNavigateToSurvey()) {
            SurveyActivity::class.java
        } else {
            MainActivity::class.java
        }
        startActivity(Intent(this, nextActivity))
        finish()
    }


    override fun pageCount(): Int = pagerAdapter.itemCount
}
