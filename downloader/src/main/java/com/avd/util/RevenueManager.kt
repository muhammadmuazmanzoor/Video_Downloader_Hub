package com.avd.util

import android.content.Context
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData

class RevenueManager private constructor(val context: Context?) : MaxAdRevenueListener {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    init {
        if (context != null) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                } else {
                    if (firebaseAnalytics == null) {
                        firebaseAnalytics = Firebase.analytics
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Singleton instance
    companion object {
        @Volatile
        private var revenueManager: RevenueManager? = null

        // Ensure only one instance is created
        fun getInstance(context: Context): RevenueManager {

            return revenueManager ?: synchronized(this) {
                revenueManager ?: RevenueManager(context).also { revenueManager = it }
            }
        }
    }

    // Example function to log user action
    override fun onAdRevenuePaid(impressionData: MaxAd) {
        context?.let {
            impressionData?.let { data ->
                firebaseAnalytics?.logEvent("ad_impression_max") {
                    param(FirebaseAnalytics.Param.AD_PLATFORM, "appLovin")
                    param(FirebaseAnalytics.Param.AD_UNIT_NAME, data.adUnitId)
                    param(FirebaseAnalytics.Param.AD_FORMAT, data.format.label)
                    param(FirebaseAnalytics.Param.AD_SOURCE, data.networkName)
                    param(FirebaseAnalytics.Param.VALUE, data.revenue)
                    param(FirebaseAnalytics.Param.CURRENCY, "USD") // Revenue is in USD
                }
                Log.e("checkfbAds","AppLovin Ad: ${data.networkName} \nAd Format : ${data.format.label}")
                val adData = SingularAdData(
                    "AppLovin Ad: ${data.networkName} Ad Format : ${data.format.label}",
                    "USD",
                    data.revenue
                )
                Singular.adRevenue(adData)
            }
        }
    }
}