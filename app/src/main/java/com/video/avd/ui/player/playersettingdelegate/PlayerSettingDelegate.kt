package com.video.avd.ui.player.playersettingdelegate

import android.app.Activity

interface PlayerSettingDelegate {
    fun showplaybackposition(mActivity: Activity) : Int?

    fun speed2xHandle(mActivity: Activity) : Boolean

    fun fastForward(mActivity: Activity):Boolean

    fun autoplay(mActivity: Activity):Boolean

    fun rememberorientation(mActivity: Activity):Boolean

    fun rememberbrightness(mActivity: Activity):Boolean

    fun rememberspeed(mActivity: Activity):Boolean
}