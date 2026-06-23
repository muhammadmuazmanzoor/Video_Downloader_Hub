package com.video.avd.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.video.avd.BuildConfig

const val TAG = "ConsentTag"

class ConsentController(private val activity: Activity) {

    private var consentInformation: ConsentInformation? = null
    private var consentCallback: ConsentCallback? = null
    private var consentForm: ConsentForm? = null

    val canRequestAds: Boolean get() = consentInformation?.canRequestAds() == true

    fun initConsent(
        @Debug("Device Id is only use for DEBUG") deviceId: String,
        callback: ConsentCallback?
    ) {
        this.consentCallback = callback

        val isDebug = BuildConfig.DEBUG

        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(deviceId)
            .build()

        val params = when (isDebug) {
            true -> ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build()
            false -> ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
        }

        consentInformation = UserMessagingPlatform.getConsentInformation(activity).also {
//            if (isDebug) {
//                Log.d(TAG, "Consent Form reset() in Debug")
//                it.reset()
//            }

            Log.d(TAG, "Consent ready for initialization")
            it.requestConsentInfoUpdate(
                activity,
                params,
                {
                    Log.d(TAG, "Consent successfully initialized \n Is Consent Form Available: ${it.isConsentFormAvailable}")

                    val status = consentInformation?.consentStatus
                    val isPolicyRequired =
                        consentInformation?.privacyOptionsRequirementStatus ==
                                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                    // Call policy callback
                    consentCallback?.onPolicyStatus(isPolicyRequired)

                    when (status) {
                        ConsentInformation.ConsentStatus.REQUIRED -> {
                            // Only load the form if REQUIRED
                            loadConsentForm()
                        }
                        ConsentInformation.ConsentStatus.OBTAINED,
                        ConsentInformation.ConsentStatus.NOT_REQUIRED -> {
                            // Already handled or not needed → skip form
                            consentCallback?.onAdsLoad(canRequestAds)
                        }
                        else -> {
                            // UNKNOWN or null → continue anyway
                            consentCallback?.onAdsLoad(canRequestAds)
                        }
                    }
                },
                { error ->
                    Log.e(TAG, "initializationError: ${error.message}")
                    consentCallback?.onAdsLoad(canRequestAds)
                }
            )
        }
    }

    private fun loadConsentForm() {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { consentForm ->
                Log.d(TAG, "Consent Form Load Successfully")
                this.consentForm = consentForm
                consentCallback?.onConsentFormLoaded()
            },
            { formError ->
                Log.e(TAG, "Consent Form Load Fail: ${formError.message}")
                consentCallback?.onAdsLoad(canRequestAds)
            }
        )
    }

    fun showConsentForm() {
        if (consentForm == null) {
            Log.e(TAG, "Consent form is null, skipping show")
            consentCallback?.onAdsLoad(canRequestAds)
            return
        }

        Log.i(TAG, "Consent form is showing")
        consentForm?.show(activity) { formError ->
            Log.i(TAG, "Consent Form Dismissed")

            consentCallback?.onConsentFormDismissed()
            consentCallback?.onAdsLoad(canRequestAds)

            if (formError == null) {
                checkConsentAndPrivacyStatus()
            } else {
                Log.e(TAG, "Consent Form Show fail: ${formError.message}")
            }
        }
    }

    private fun checkConsentAndPrivacyStatus() {
        Log.d(TAG, "Check Consent And Privacy Status After Form Dismissed")

        when (consentInformation?.consentStatus) {
            ConsentInformation.ConsentStatus.REQUIRED -> Log.d(TAG, "consentStatus: REQUIRED")
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> Log.d(TAG, "consentStatus: NOT_REQUIRED")
            ConsentInformation.ConsentStatus.OBTAINED -> Log.d(TAG, "consentStatus: OBTAINED")
            ConsentInformation.ConsentStatus.UNKNOWN -> Log.d(TAG, "consentStatus: UNKNOWN")
            null -> Log.d(TAG, "Consent Information is null")
        }
    }

    annotation class Debug(val message: String = "For Debug Feature")
}
