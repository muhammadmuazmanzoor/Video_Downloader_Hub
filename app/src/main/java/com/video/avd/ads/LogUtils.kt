package com.video.avd.ads

import android.util.Log

object LogUtils {

    private var tag = "AD-DEBUGGER"
    fun printLog(which : String, message : String){
        Log.d("$tag $which", message)
    }
}