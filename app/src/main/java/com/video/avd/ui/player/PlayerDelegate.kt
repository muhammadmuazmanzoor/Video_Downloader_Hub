package com.video.avd.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import com.video.avd.ui.status_saver.StatusViewModel

interface PlayerDelegate {

    fun shareStatus(activity: Activity, statusViewModel: StatusViewModel)

    fun startPictureInPictureWithRatio(activity: Activity)

    fun createPIPMode(activity: Activity)

    fun isPipModeEnable(activity: Activity)

    fun isPipModeCheck(activity: Activity): Boolean

    fun buildPIPParams(
        context: Context?,
        nowPlaying: Boolean
    ): PictureInPictureParams




    fun unmuteSound(context: Context)


    fun muteSound(context: Context)

    fun shareLiveLink(context: Context, link : String)



}