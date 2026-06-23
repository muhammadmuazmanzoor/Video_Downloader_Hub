package com.video.avd.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import kotlin.let

object FirebaseLogUtils {
    private var fbAnalytics: FirebaseAnalytics? = null
    fun initFirebaseAnalytics(context: Context) {
        fbAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logEvent(eventName: String, logEvent: String) {
       // Singular.event(eventName)
       /* val param = Bundle()
        param.putString(eventName, logEvent)
        fbAnalytics?.let { analytics ->
            analytics.logEvent(eventName, param)
        }*/
    }

}