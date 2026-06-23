package com.avd.util

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.singular.sdk.Singular
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseEvents{

    var firebaseAnalytics: FirebaseAnalytics? = null
    fun firebaseUserAction(action: String, activityName: String, context: Context) {
    /*    CoroutineScope(Dispatchers.IO).launch {
            Singular.event(action)
            context.let {
                try {
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
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }*/
    }
    fun fbEvents(action: String, activityName: String, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }

                if (firebaseAnalytics == null) {
                    firebaseAnalytics = Firebase.analytics
                }

                firebaseAnalytics?.logEvent(action) {
                    param("Screen_Name", activityName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}