package com.video.avd.ui.splash_flow.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.avd.util.Prefs
import com.video.avd.ads.AdsHelper.langSessionRemote
import com.video.avd.ads.AdsHelper.obEnable
import com.video.avd.ads.AdsHelper.obFirstEnable
import com.video.avd.ads.AdsHelper.obFourthEnable
import com.video.avd.ads.AdsHelper.obSecondEnable
import com.video.avd.ads.AdsHelper.obThirdEnable
import com.video.avd.ads.AdsHelper.surveyEnable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume



object AppUtils {
    const val ONBOARDING = "onboarding_screen"

    const val FIRST_TIME_KEY = "is_first_time"

    const val LANG_SESSION = "lang_session"
    const val OB_SESSION = "ob_session"
    const val SURVEY_SESSION = "survey_session"
    const val PREF_KEY = "ProjectPrefs"

    var selectedTags: ArrayList<String> = arrayListOf()

    var isTagItemSelected = MutableLiveData(false)

    val remoteConfigStatus = MutableLiveData<Boolean>()

    private val DEFAULTS: HashMap<String, Any> =
        hashMapOf(
//            SPLASH_AD to true,
        )

    fun shouldNavigateToLanguage(): Boolean {
        if (langSessionRemote == 0) return false
        return Prefs[LANG_SESSION, 0] < 1
    }

    fun shouldNavigateToOnboarding(): Boolean {
        return obEnable && Prefs[OB_SESSION, 0] < 1
    }

    fun shouldNavigateToSurvey(): Boolean {
        return surveyEnable && Prefs[SURVEY_SESSION, 0] < 1
    }

    fun shouldAllObDisable(): Boolean{
        return !obFirstEnable && !obSecondEnable &&
                !obThirdEnable && !obFourthEnable

    }


    fun Activity.hideNavigationBar(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.navigationBars())
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }else{
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        }
    }

    fun isOnline(context: Context): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = connectivityManager.activeNetwork
                    if (network != null) {
                        val nc = connectivityManager.getNetworkCapabilities(network)
                        return nc?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ||
                                nc?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    }
                } else {
                    val networkInfos = connectivityManager.allNetworkInfo
                    for (tempNetworkInfo in networkInfos) {
                        if (tempNetworkInfo.isConnected) {
                            return true
                        }
                    }
                }
            }
        } catch (e: NullPointerException) {
            // Handle the NullPointerException gracefully
            e.printStackTrace()
        }
        return false
    }
    fun getMediationInfo(nativeAd: NativeAd) : String {
        val responseInfo = nativeAd.responseInfo
        val mediationAdapterClassName = responseInfo?.mediationAdapterClassName
        when {
            mediationAdapterClassName?.contains("facebook", ignoreCase = true) == true -> {
                return "meta"
            }else ->{
            return "other"

        }
        }
    }
    /*@OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return withContext(Dispatchers.IO) {
            val remoteConfig = Firebase.remoteConfig

            try {
                // Always apply config settings
                val configSettings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds =  0
                }
                remoteConfig.setConfigSettingsAsync(configSettings).await()

                // Reset defaults and reapply them
                remoteConfig.setDefaultsAsync(DEFAULTS).await()

                // Fetch and activate safely
                val fetchTask = remoteConfig.fetchAndActivate()

                suspendCancellableCoroutine<Unit> { cont ->
                    fetchTask.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("RemoteConfig", "Fetch successful")
                        } else {
                            Log.e("RemoteConfig", "Fetch failed: ${task.exception}")
                        }
                        cont.resume(Unit)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RemoteConfig", "Outer Exception: ${e.message}")
            }

            return@withContext remoteConfig
        }
    }*/

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return withContext(Dispatchers.IO) {
            val remoteConfig = Firebase.remoteConfig

            try {
                val configSettings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 0
                }
                remoteConfig.setConfigSettingsAsync(configSettings).await()
                remoteConfig.setDefaultsAsync(DEFAULTS).await()

                val fetchTask = remoteConfig.fetchAndActivate()

                suspendCancellableCoroutine<Unit> { cont ->
                    fetchTask.addOnCompleteListener { task ->

                        if (task.isSuccessful) {
                            Log.d("RemoteConfig", "Fetch successful")
                            remoteConfigStatus.postValue(true)
                        } else {
                            Log.e("RemoteConfig", "Fetch failed: ${task.exception}")
                            remoteConfigStatus.postValue(false)
                        }

                        cont.resume(Unit)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RemoteConfig", "Outer Exception: ${e.message}")
                remoteConfigStatus.postValue(false)
            }

            remoteConfig
        }
    }

}
