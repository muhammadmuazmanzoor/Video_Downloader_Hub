package com.video.avd

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.avd.util.AdBlockerHelper.browser_native
import com.avd.util.AdBlockerHelper.home_native
import com.avd.util.AdBlockerHelper.initialize
import com.avd.util.AdBlockerHelper.isProVersion
import com.avd.util.AdBlockerHelper.native_home_variation
import com.avd.util.AdBlockerHelper.recycler_native
import com.avd.util.AdBlockerHelper.resetAppOpenShownAd
import com.facebook.ads.AudienceNetworkAds
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import com.avd.util.AppConstant.ADJUST_TOKEN
import com.avd.util.ContextUtils
import com.avd.util.RevenueManager
import com.avd.util.YoutubeDlUtils
import com.avd.util.ads.InterstitialManagerA
import com.squareup.picasso.Picasso
import com.video.avd.ads.AdsManager
import com.video.avd.ads.AdsManager.appOpenAdRemote
import com.video.avd.ads.AdsManagerKit
import com.avd.util.Prefs
import com.avd.util.RemoteConfigHelper
import com.video.avd.ads.AppOpenManager
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.hasNotifiedThisSession
import com.video.avd.utils.GlobalValues
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : MultiDexApplication(), Configuration.Provider , DefaultLifecycleObserver {

    @SuppressLint("StaticFieldLeak")
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Inject lateinit var workerFactory: HiltWorkerFactory
    var appOpenManager: AppOpenManager? = null
    var remoteConfig: FirebaseRemoteConfig? = null
        private set
    @SuppressLint("StaticFieldLeak")
    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
        private lateinit var revenueManager: RevenueManager
        var isShowPermission = true


        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: MyApplication
        fun getInstance(): MyApplication {
            return instance
        }
    }

    val environment = if (BuildConfig.DEBUG) {
        AdjustConfig.ENVIRONMENT_SANDBOX   // For debug builds
    } else {
        AdjustConfig.ENVIRONMENT_PRODUCTION // For release builds
    }


    override fun onCreate() {
        super<MultiDexApplication>.onCreate()
        context = applicationContext
        YoutubeDlUtils.application = this
        ContextUtils.initApplicationContext(applicationContext)
        MultiDex.install(this)
        AdsManager.adSdkChoice ="admob"
        initialize(applicationContext)
        resetAppOpenShownAd = {
            AppOpenManager.isShowingAd = false
        }
        RemoteConfigHelper.init()
        // Initialize and fetch remote config
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Prefs.init(this)
        
        // Initialize Picasso lazily on background thread to prevent ANR
        // PicassoProvider ContentProvider is disabled in AndroidManifest.xml
        initializePicassoLazily()

        if (com.video.avd.ui.splash_flow.utils.AppUtils.isOnline(applicationContext)) {
            applicationScope.launch {
                try {
                    Log.e("AppOpenAd", "[MyApplication] Early AdsManagerKit.init() - online path")
                    val config = com.video.avd.ui.splash_flow.utils.AppUtils.getFirebaseRemoteConfig()
                    AdsManagerKit.init(config)
                    Log.e("AppOpenAd", "[MyApplication] AdsManagerKit.init() completed, appOpenAdRemote=$appOpenAdRemote")
                    MobileAds.initialize(applicationContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("AppOpenAd", "[MyApplication] Early AdsManagerKit.init() failed", e)
                    Log.e("RemoteConfig", "Initialization failed: ${e.message}")
                }
            }
        } else {
            // If no network, still get cached config
            try {
                Log.e("AppOpenAd", "[MyApplication] Early AdsManagerKit.init() - offline path")
                val config = com.google.firebase.Firebase.remoteConfig
                AdsManagerKit.init(config)
                Log.e("AppOpenAd", "[MyApplication] AdsManagerKit.init() completed (offline), appOpenAdRemote=$appOpenAdRemote")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AppOpenAd", "[MyApplication] Early AdsManagerKit.init() failed (offline)", e)
            }
        }
        // Firebase initializes automatically via FirebaseInitProvider ContentProvider
        // Do not call FirebaseApp.initializeApp() here to prevent ANR from class loading
        // The initialization happens automatically before Application.onCreate()
        try {
            // Wait for Firebase to be initialized (it happens automatically via ContentProvider)
            // Use a background thread to avoid blocking main thread during startup
            applicationScope.launch {
                try {
                    // Give FirebaseInitProvider time to initialize Firebase
                    // Check if Firebase is initialized, if not wait briefly
                    var attempts = 0
                    while (FirebaseApp.getApps(applicationContext).isEmpty() && attempts < 10) {
                        delay(50) // Wait 50ms between checks
                        attempts++
                    }
                    
                    // Now safely access Firebase Remote Config
                    if (FirebaseApp.getApps(applicationContext).isNotEmpty()) {
                        Log.e("AppOpenAd", "[MyApplication] Firebase initialized, setting up Remote Config...")
                        val config = Firebase.remoteConfig
                        remoteConfig = config
                        // Apply config settings (minimum fetch interval)
                        val configSettings = remoteConfigSettings {
                            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 0
                        }
                        config.setConfigSettingsAsync(configSettings)
                        // Set defaults immediately
                        config.setDefaultsAsync(R.xml.remote_config_defaults)
                        Log.e("AppOpenAd", "[MyApplication] Remote Config setup complete, calling fetchRemoteConfigInBackground()...")
                        // Start fetching in background without waiting
                        fetchRemoteConfigInBackground()
                    } else {
                        Log.e("AppOpenAd", "[MyApplication] Firebase NOT initialized after $attempts attempts - AppOpenManager will NOT be created")
                    }
                } catch (e: Exception) {
                    Log.e("AppOpenAd", "[MyApplication] Firebase initialization error - AppOpenManager will NOT be created", e)
                    Log.e("MyApplication", "Firebase initialization error", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "Error setting up Firebase Remote Config", e)
        }
        instance = this
        AudienceNetworkAds.initialize(this)
        initializeAdjust()
    }

    private fun initializeAdjust() {
        val config = AdjustConfig(this, ADJUST_TOKEN, environment)

        // 🔸 Recommended: verbose logs for debugging
        config.setLogLevel(LogLevel.VERBOSE)

        // Optional listeners for debugging / analytics
        config.setOnEventTrackingSucceededListener {
            Log.d("ADJUST_Config", "✅ Event tracked: $it")
            // printDebugLog("✅ Event tracked: $it")
        }
        config.setOnSessionTrackingSucceededListener {
            Log.d("ADJUST_Config","✅ Session tracked: $it")
        }
        config.setOnEventTrackingFailedListener {
            Log.d("ADJUST_Config", "❌ Event failed: $it")
        }
// Allow to send in the background.
        config.enableSendingInBackground()
        // Initialize Adjust SDK
        Adjust.initSdk(config)
        // Register lifecycle callbacks so Adjust handles onResume/onPause automatically
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = Adjust.onResume()
            override fun onActivityPaused(activity: Activity) = Adjust.onPause()
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
        Log.d("ADJUST_Config","🔥 Adjust initialized with token: $ADJUST_TOKEN")


    }

    private fun fetchRemoteConfigInBackground() {
        Log.e("AppOpenAd", "[MyApplication] fetchRemoteConfigInBackground() started")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure remoteConfig is initialized before fetching
                val config = remoteConfig ?: run {
                    Log.e("AppOpenAd", "[MyApplication] remoteConfig is NULL - cannot fetch, AppOpenManager will NOT be created")
                    return@launch
                }
                Log.e("AppOpenAd", "[MyApplication] Calling fetchAndActivate()...")
                val result = config.fetchAndActivate().await()
                Log.e("AppOpenAd", "[MyApplication] fetchAndActivate result=$result")
                
                // Re-read appOpenAdRemote AFTER fetchAndActivate to get latest value
                val appOpenAdRemoteValue = config.getBoolean("appOpenAd")
                home_native = config.getBoolean("home_native")
                native_home_variation = config.getLong("home_native_variation")
                browser_native = config.getBoolean("browser_native")
                recycler_native = config.getBoolean("recycler_native")
                Log.e("AppOpenAd", "[MyApplication] appOpenAdRemote from config=$appOpenAdRemoteValue (old value was $appOpenAdRemote)")
                appOpenAdRemote = appOpenAdRemoteValue
                
                Log.e("AppOpenAd", "[MyApplication] Conditions: appOpenAdRemote=$appOpenAdRemote, isPro=${isProVersion.value}, is24hour=${GlobalValues.is24hourEnabled.value}, adSdkChoice=${AdsManager.adSdkChoice}")
                
                if (result) {
                    Log.e("AppOpenAd", "[MyApplication] fetchAndActivate succeeded - fetched new config")
                } else {
                    Log.e("AppOpenAd", "[MyApplication] fetchAndActivate returned false - using cached config (this is OK)")
                }
                
                // Initialize Ads if conditions are met (regardless of fetch result - cached config is fine)
                try {
                    if (appOpenAdRemote &&
                        isProVersion.value != true &&
                        GlobalValues.is24hourEnabled.value == false
                    ) {
                        withContext(Dispatchers.Main){
                            when (AdsManager.adSdkChoice) {
                                "admob" -> {
                                    Log.e("AppOpenAd", "[MyApplication] Creating AppOpenManager (admob)...")
                                    MobileAds.initialize(this@MyApplication)
                                    appOpenManager = AppOpenManager(this@MyApplication)
                                    Log.e("AppOpenAd", "[MyApplication] AppOpenManager created successfully")
                                }

                                "applovin" -> {
                                    Log.e("AppOpenAd", "[MyApplication] Using applovin, not creating AppOpenManager")
                                    revenueManager = RevenueManager.getInstance(this@MyApplication)
                                }
                                else -> {
                                    Log.e("AppOpenAd", "[MyApplication] adSdkChoice='${AdsManager.adSdkChoice}' - AppOpenManager NOT created")
                                }
                            }
                        }
                    } else {
                        Log.e("AppOpenAd", "[MyApplication] AppOpenManager NOT created: appOpenAdRemote=$appOpenAdRemote, isPro=${isProVersion.value}, is24hour=${GlobalValues.is24hourEnabled.value}")
                    }
                } catch (e: Exception) {
                    Log.e("AppOpenAd", "[MyApplication] Exception creating AppOpenManager", e)
                    e.printStackTrace()
                }

                Log.d("RemoteConfig", "Fetch success: $result")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AppOpenAd", "[MyApplication] fetchRemoteConfigInBackground failed", e)
                Log.e("RemoteConfig", "Fetch failed: ${e.message}")
            }
        }
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onStart(owner: LifecycleOwner) {
        // App just came to foreground – reset “hasNotified” flag
        Log.e("AppOpenAd", "[MyApplication] App entered FOREGROUND. appOpenManager=${appOpenManager != null}")
        Log.d("AppLife", ">>> FOREGROUND")
        hasNotifiedThisSession = false
    }

    override fun onStop(owner: LifecycleOwner) {
        // All activities are now stopped → app in background
        if (!hasNotifiedThisSession) context?.let { AppUtils.maybeShowNotification(it) }
    }

    /**
     * Initialize Picasso lazily on a background thread to prevent ANR.
     * PicassoProvider ContentProvider is disabled in AndroidManifest.xml to avoid
     * blocking initialization during app startup.
     */
    private fun initializePicassoLazily() {
        applicationScope.launch {
            try {
                // Initialize Picasso on background thread to avoid blocking main thread
                // This prevents ANR from PicassoProvider ContentProvider initialization
                val picasso = Picasso.Builder(applicationContext)
                    .build()
                
                // Set singleton instance only if not already set
                // This ensures we don't conflict with any other initialization
                try {
                    Picasso.setSingletonInstance(picasso)
                } catch (e: IllegalStateException) {
                    // Picasso singleton already set, which is fine
                    Log.d("MyApplication", "Picasso singleton already initialized")
                }
            } catch (e: Exception) {
                Log.e("MyApplication", "Failed to initialize Picasso lazily", e)
                // Picasso will initialize on first use if this fails
            }
        }
    }

}
