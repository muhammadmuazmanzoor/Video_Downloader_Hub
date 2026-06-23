package com.video.avd.ui.player.playersettingdelegate

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class PlayerSettingDelegateImpl : PlayerSettingDelegate {

    override fun showplaybackposition(mActivity: Activity): Int {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getInt("SelectedOptionPosition", 1)
    }

    override fun speed2xHandle(mActivity: Activity): Boolean {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getBoolean("2xSpeedFeatureEnabled", true)
    }

    override fun fastForward(mActivity: Activity): Boolean {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getBoolean("FastForward", true)
    }

    override fun autoplay(mActivity: Activity): Boolean {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getBoolean("autoplay", true)
    }

    override fun rememberorientation(mActivity: Activity): Boolean {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getBoolean("orientationlock", false)
    }

    override fun rememberbrightness(mActivity: Activity): Boolean {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getBoolean("brightnesslock", false)
    }

    override fun rememberspeed(mActivity: Activity): Boolean {
        val sharedPreferences = getPreferences(mActivity)
        return sharedPreferences.getBoolean("speedlock", false)
    }


    private fun getPreferences(activity: Activity): SharedPreferences {
        return activity.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
    }


}