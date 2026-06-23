package com.video.avd.ui.splash_flow.utils

import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseEvents {
    var firebaseAnalytics: FirebaseAnalytics? = null
    fun firebaseUserActionNew(activityName: String, actionName: String) {
        val action=formatString(actionName)
        CoroutineScope(Dispatchers.IO).launch {
       /*     context?.let {
                if (FirebaseApp.getApps(it).isEmpty()) {
                    FirebaseApp.initializeApp(it)
                } else {
                    if (firebaseAnalytics == null) {
                        firebaseAnalytics = Firebase.analytics
                    }
                    firebaseAnalytics?.let { analytics ->
                        analytics.logEvent(action) {
                            param("Screen_Name", activityName)
                        }
                    }
                    Singular.event(action)
                }
            }*/
        }
    }


    fun formatString(input: String): String {
        try {
            return input
                .replace(" ", "_")                  // Replace spaces with underscores
                .take(40)
        } catch (e: Exception) {
            return input
        }                           // Trim to max 40 characters
    }
    fun extractMediatedNetworkName(className: String?): String {
        return className
            ?.substringAfter("mediation.")       // "applovin.AppLovinMediationAdapter"
            ?.substringBefore('.')               // "applovin"
            ?.lowercase() ?: "unknown"
    }

}