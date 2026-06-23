package com.video.avd.ui.splash

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Trace.isEnabled
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.hideLoading
import com.avd.util.AdBlockerHelper.isAdShowing
import com.avd.util.AdBlockerHelper.isProVersion
import com.avd.util.AdBlockerHelper.showLoading
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.video.avd.BuildConfig
import com.video.avd.MyApplication
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.attachRevenueListener
import com.video.avd.ads.AdsHelper.langNativeAd1
import com.video.avd.ads.AdsHelper.langNativeAdHigh1
import com.video.avd.ads.AdsHelper.loadWithFallback
import com.video.avd.ads.AdsHelper.native_language
import com.video.avd.ads.AdsHelper.splashInterstitialEnabled
import com.video.avd.ads.AdsHelper.splashInterstitialHighEnabled
import com.video.avd.ads.ConsentCallback
import com.video.avd.ads.ConsentController
import com.video.avd.ads.SplashBottomAdLoader
import com.video.avd.ads.isShowingAd
import com.video.avd.constent.isDataInitialized
import com.video.avd.constent.isinternal
import com.video.avd.constent.isnotification
import com.video.avd.databinding.ActivitySplashBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ui.splash_flow.activities.InAppActivity
import com.video.avd.ui.splash_flow.activities.LanguageActivity
import com.video.avd.ui.splash_flow.utils.AppUtils.isOnline
import com.video.avd.ui.splash_flow.utils.AppUtils.remoteConfigStatus
import com.video.avd.ui.splash_flow.utils.AppUtils.shouldNavigateToLanguage
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.changeStatusBarColor
import com.video.avd.utils.AppUtils.hideNavigationBar
import com.video.avd.utils.GlobalLoader
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.GlobalValues.fromSplash
import com.video.avd.utils.InAppPurchases
import com.video.avd.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    var binding: ActivitySplashBinding? = null
    var splashBottomAdLoader: SplashBottomAdLoader? = null

    @Inject
    lateinit var inAppSubscription: InAppPurchases

    private var interstitialSplash: InterstitialAd? = null
    companion object{
        var show:Boolean=true
        var alreadyRequested:Boolean=false
        var fromNoti=false
        var currentActivity: Activity? = null
    }
    private var splashTimeoutJob: Job? = null

    fun restartApp(activity: Activity) {
        val intent = activity.packageManager
            .getLaunchIntentForPackage(activity.packageName)

        intent?.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        )

        activity.startActivity(intent)
        activity.finish()
        Runtime.getRuntime().exit(0)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreference.saveSystemDefaultLanguage(this)
        AppUtils.setLocate(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        fromSplash=true
        tracknotification(intent)
        Log.e("Notifiiis", "internalnotification data: ${intent.data}")
        changeStatusBarColor(com.avd.R.color.primary_bg, this@SplashActivity, false)
        handleBackPress()
        binding?.btnTryAgain?.setOnClickListener {
            if (isOnline(this)) {
                restartApp(this)
            }
        }
        AppUtils.fbEvents("splash_view", "SplashActivity",this)
        binding?.main?.let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        getP()
        isDataInitialized.observe(this){
            if(it==true){
                isProVersion.observe(this) { value ->
                    if (value == true) {
                        lifecycleScope.launch {
                            delay(2000)
                            navigateToNext()
                        }

                    }
                    binding?.adText?.visibility = if (value == true) {
                        View.INVISIBLE
                    } else {
                        lifecycleScope.launch {
                            startSplashTimeout()
                            //  requestNotificationPermission()
                            if (isOnline(this@SplashActivity)) {
                                initConsent()
                            }
                        }
                        View.VISIBLE
                    }
                }

            }
            else{
                    lifecycleScope.launch {
                        startSplashTimeout()
                        //  requestNotificationPermission()
                        getP()
                        if (isOnline(this@SplashActivity)) {
                            initConsent()
                        }
                    }
                    View.VISIBLE
            }
        }

        if (!isOnline(this)) {
            binding?.flNoAd?.visibility = View.VISIBLE
            return
        } else {
            binding?.flNoAd?.visibility = View.GONE
        }

    }

    private fun initConsent() {
        ConsentController(this).apply {
            initConsent("5FE6905BB78D8DE6E5B883EAF56F2B4D", object : ConsentCallback {
                override fun onAdsLoad(canRequestAd: Boolean) {
                    loadAds()
                }

                override fun onConsentFormLoaded() {
                    this@apply.showConsentForm()
                }

                override fun onConsentFormDismissed() {
                }

                override fun onPolicyStatus(required: Boolean) {
                }
            })
        }
    }

    fun loadAds() {
        remoteConfigStatus.observeForever { success ->
            if (success) {
                lifecycleScope.launch {
                    delay(1_000)
                    if(!fromNoti) {
                        requestingAllAds()
                    }
                }
            } else {
                lifecycleScope.launch {
                    delay(15_000)
                    navigateToNext()
                }
                Log.e("RemoteConfig", "Fetch failed11")
            }
        }
    }

    fun navigateToNext() {
        splashTimeoutJob?.cancel()
        if(show) {
            if (shouldNavigateToLanguage() && NetworkUtils.isOnline(this)) {
                if (isProVersion.value != true) {
                    startActivity(Intent(this@SplashActivity, InAppActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this@SplashActivity, LanguageActivity::class.java))
                    finish()
                }
            }
            else if (NetworkUtils.isOnline(this)) {
                if (isProVersion.value != true) {
                    startActivity(Intent(this@SplashActivity, InAppActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

            }
            if(!fromNoti){
            show=false
                }
        }
    }


    private fun startSplashTimeout() {
        splashTimeoutJob?.cancel()

        splashTimeoutJob = lifecycleScope.launch {
            delay(240_000L) // ⏱️ 4 minutes max

            if (!isFinishing && !isDestroyed) {
                AppUtils.fbEvents("splash_timeout", "SplashActivity",this@SplashActivity)
//                Log.e("SplashTimeout", "Ad stuck or not shown → forcing navigation")
                navigateToNext()
            }
        }
    }


    private fun handleBackPress() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isFinishing && !isDestroyed) {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun requestingAllAds() {
        // Setup bottom banner loader for splash
        binding?.splashAdContainer?.let {
            splashBottomAdLoader = SplashBottomAdLoader(
                this,
                it
            )
        }


        if (splashInterstitialHighEnabled) {
            loadInterSplashHigh(this)
            loadProgress(15_000L, false)
        } else if (splashInterstitialEnabled) {
            loadInterSplashNormal(this)
            loadProgress(15_000L, false)
        } else {
            loadProgress(5_000L, true)
        }

        if (shouldNavigateToLanguage()) {

            // Language Native Ad #1
            if (langNativeAdHigh1 == null) {
                loadWithFallback(
                    activity = this,
                    highFloorAdId = BuildConfig.native_language_high,
                    normalAdId = BuildConfig.native_language,
                    showHighfloor = AdsHelper.langNativeHigh1Enabled,
                    showNormalfloor = AdsHelper.langNative1Enabled,
                    onAdLoadedHigh = { langNativeAdHigh1 = it
                        native_language.postValue(true)
                                     },
                    onAdLoadedNormal = { langNativeAd1 = it
                        native_language.postValue(true)
                                       },
                    onAdFailed = {
                        native_language.postValue(false)
                    }
                )
            }
        }
    }

    private fun loadProgress(duration: Long, move: Boolean) {
        binding?.progressBar?.progress = 0
        lifecycleScope.launch {
            if (AdBlockerHelper.isProVersion.value != true) {
                val steps = 100
                val delayPerStep = duration.toDouble() / steps

                for (i in 1..steps) {
                    binding?.progressBar?.progress = i
                    delay(delayPerStep.toLong())
                }
            }

            if (move) {
                navigateToNext()
            }
        }
    }


    fun loadInterSplashHigh(
        context: Context,
    ) {
        if (AdBlockerHelper.isProVersion.value == true && !fromSplash) {
            Log.w("checkInterAd", "No request, its from notification")
            binding?.progressBar?.progress = 0
            lifecycleScope.launch {
                val totalDuration = 5_000L
                val steps = 100
                val delayPerStep = totalDuration.toDouble() / steps

                for (i in 1..steps) {
                    binding?.progressBar?.progress = i
                    delay(delayPerStep.toLong())
                }
                navigateToNext()
            }

            return
        }
        if(!alreadyRequested) {
            Log.d("checkInterAd", "request Inter High")
            val adRequest = AdRequest.Builder().build()
            val adUnitId = BuildConfig.inter_splash_high
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialSplash = ad
                        Log.d("checkInterAd", "Loaded Inter High")
                        attachRevenueListener(ad)
                        if (!fromNoti) {
                            showInterSplash(this@SplashActivity)
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialSplash = null
                        alreadyRequested=false
                        Log.e("checkInterAd", "Failed Inter High")
                        if (splashInterstitialEnabled) {
                            loadInterSplashNormal(this@SplashActivity)
                        }

                    }
                })
            alreadyRequested=true
        }
    }


    fun loadInterSplashNormal(
        context: Context,
    ) {
        if (isProVersion.value == true && !fromSplash) {
            binding?.progressBar?.progress = 0
            Log.w("checkInterAd", "No Request, its from notification")
            lifecycleScope.launch {
                val totalDuration = 5_000L
                val steps = 100
                val delayPerStep = totalDuration.toDouble() / steps

                for (i in 1..steps) {
                    binding?.progressBar?.progress = i
                    delay(delayPerStep.toLong())
                }
                navigateToNext()
            }

            return
        }
        if(!alreadyRequested) {
            Log.d("checkInterAd", "request Inter Normal")
            val adRequest = AdRequest.Builder().build()
            val adUnitId = BuildConfig.inter_splash
            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialSplash = ad
                        Log.d("checkInterAd", "Loaded Inter Normal")
                        attachRevenueListener(ad)
                        if (!fromNoti) {
                            showInterSplash(this@SplashActivity)
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("checkInterAd", "Failed Inter Normal")
                        interstitialSplash = null
                        navigateToNext()
                    }
                })
            alreadyRequested=true
        }
    }


    fun showInterSplash(
        currentActivitys: FragmentActivity,
    ) {

        CoroutineScope(Dispatchers.Main).launch {
            splashTimeoutJob?.cancel()
            try {
                if (isProVersion.value != true) {

                    if (interstitialSplash != null) {
                        isAdShowing=true
                        showLoading(currentActivitys,"Loading Ad...")
                        delay(1000)
                        interstitialSplash?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isShowingAd = true
                                  /*  currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                    }*/
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                    GlobalLoader.hide(currentActivity)
                                    interstitialSplash = null
                                    hideLoading()
                                }

                                override fun onAdDismissedFullScreenContent() {
//                                    GlobalLoader.hide(currentActivity)
                                    interstitialSplash = null
                                    isShowingAd = false
                                    hideLoading()
                                    navigateToNext()
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialSplash = null
                                }
                            }
                        navigateToNext()
                        delay(300)
                            currentActivity?.let {  interstitialSplash?.show(it)}
                        Log.d("checkInterAd", "Showing Inter")
                            interstitialSplash = null
                        currentActivitys.onBackPressedDispatcher.addCallback(currentActivitys) {
                            if (isAdShowing) {
                                // Block back press while ad is showing
                                // Optionally show a toast
                                isEnabled = false
                                Toast.makeText(currentActivitys, "Please wait for the ad to finish", Toast.LENGTH_SHORT).show()
                            } else {
                                isEnabled = true
                            }
                        }

                    } else {
                        Log.e("checkInterAd", "interstitialSplash Null")
                        interstitialSplash = null
                        navigateToNext()
                    }

                } else {
                    navigateToNext()

                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("checkInterAd", "interstitialSplash Exception:$e")
                navigateToNext()
            }

        }

    }

    override fun onDestroy() {
        splashBottomAdLoader?.cleanup()
        splashTimeoutJob?.cancel()
        super.onDestroy()
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private var permissionResultCallback: ((Boolean) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionResultCallback?.invoke(granted)
            permissionResultCallback = null
        }

    private suspend fun requestNotificationPermission(): Boolean {
        if (hasNotificationPermission(this)) return true

        return suspendCancellableCoroutine { continuation ->
            permissionResultCallback = { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun getP() {
        if (isProVersion.value != true && InAppPurchases.Companion.billingClient == null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    inAppSubscription.setBillingClient()
                    val value = inAppSubscription.hasEverSubscribed(this@SplashActivity)
                    inAppSubscription.saveEverSubscribed(this@SplashActivity, value)
                    inAppSubscription.getPrice(application.applicationContext)
                    inAppSubscription.checkSubscription()
                    inAppSubscription.getSubscriptionPref(application.applicationContext)
                    Log.e("livedata", "true")
                } catch (e: Exception) {
                    Log.e("mTag", "error $e")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppUtils.setLocate(this)
    }


    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tracknotification(intent)
        Log.e("Notifiiis", "internalnotification data: ${intent.data}")
    }


    fun tracknotification(intent: Intent) {
        val type = intent.getStringExtra("type")
        val youtube = intent.getStringExtra("youtubelink")
        val playerurl = intent.getStringExtra("url")

        Log.e("Notifiiis", "${playerurl}" + "$type")

        val data = intent.getStringExtra("internalnotification")
        Log.e("Notifiiis", "internalnotification data: $data")
        if (data == "internalnotification") {
            Log.d("Notifiiis", "internalnotification true")
            isnotification = false
            fromNoti=true
            show=false
            fromSplash=false
            isinternal = true
            lifecycleScope.launch(Dispatchers.Main){
                delay(1000)
                    if (isProVersion.value != true) {
                        startActivity(Intent(this@SplashActivity, InAppActivity::class.java))
                        finish()
                    } else {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                    }
            }
        } else {
            Log.e("Notifiiis", "internalnotification data: $data")

            isnotification = true
            isinternal = false
        }

        if (type != null) {
            val intentnew = Intent(this, MainActivity::class.java)
            when (type) {
                "1" -> intentnew.putExtra("fragment", "home")
                "2" -> intentnew.putExtra("fragment", "music")
                "3" -> intentnew.putExtra("fragment", "download")
                "4" -> intentnew.putExtra("youtubelink", youtube)
                "5" -> intentnew.putExtra("fragment", "status_saver")
                "6" -> intentnew.putExtra("url", playerurl)
                else -> intentnew.putExtra("fragment", "home")
            }
            intentnew.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            /*lifecycleScope.launch {
                            if (!tryToShowAdFirstTime(intentnew)) {
                                // Fallback to starting the activity directly if ad fails to show
                                startActivity(intentnew)
                            }
                        }*/
            isShowingAd=true

        }
        getIntent().replaceExtras(null)
        setIntent(Intent())
    }

}
