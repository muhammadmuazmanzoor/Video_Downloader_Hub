package com.avd.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.view.View
import androidx.annotation.FloatRange
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object Memory {

    fun calcCacheSize(context: Context, @FloatRange(from = 0.01, to = 1.0) size: Float): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val largeHeap = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
        val memoryClass = if (largeHeap) am.largeMemoryClass else am.memoryClass
        return (memoryClass * 1024L * 1024L * size).toLong()
    }

    fun FragmentActivity.hideNavigationBar(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }else{
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    fun changeStatusBarColor(color: Int, context: FragmentActivity?, dark: Boolean = false) {
        context?.let { activity ->
            val window = activity.window
            window.statusBarColor = ContextCompat.getColor(activity, color)

            // Preserve the current system UI visibility flags
            val currentFlags = window.decorView.systemUiVisibility

            window.decorView.systemUiVisibility = if (dark) {
                // Add LIGHT_STATUS_BAR flag to make status bar text/icons dark
                currentFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                // Remove LIGHT_STATUS_BAR flag for light text/icons
                currentFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }



}