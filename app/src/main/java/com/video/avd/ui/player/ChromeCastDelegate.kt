package com.video.avd.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.chromecast.ChromecastConnection
import java.util.ArrayList

interface ChromeCastDelegate {
    var mDefaultCastStateListener: ChromecastConnection.CastStateUpdateListener?

    var mSelectedMedia: ArrayList<Video>?
     var mPosition : Int


    companion object {
        @SuppressLint("StaticFieldLeak")
        var mChromecastConnection: ChromecastConnection? = null

        @SuppressLint("StaticFieldLeak")
        var mPreparingConnectionDialog: SweetAlertDialog? = null

        @SuppressLint("StaticFieldLeak")
        var mPrepareServerDialog: SweetAlertDialog? = null
    }

    fun mDefaultRequestSessionCallbackFun(activity: Activity): ChromecastConnection.RequestSessionCallback
    fun startChromeCastConnection(fileData: List<Video>, activity: Activity,position: Int)
    fun startChromeCastConnectionForLiveVideos(activity: Activity, parse: String)
    fun loadRemoteMediaForStreaming(activity: Activity,parse: String)
    fun startChromeCastConnectionfromList(fileData: List<Video>, activity: Activity,position: Int)
    fun startWebServerFromHome(activity: Activity)
    fun loadNextVideo(activity: Activity)



    fun prepareSimpleWebServer(
        activity: Activity,
        primaryCallback: Runnable,
        successCallback: Runnable
    )


    fun updateSelectedPosition(position: Int)

    fun updatePosition(position: Int)


    fun loadRemoteMedia(activity: Activity)
    fun loadRemoteMediaFromPlaylist(activity: Activity)


    fun hidePrepareServerDialog()

    fun showPrepareConnectionDialog(activity: Activity)
    fun showPrepareConnectionDialogFromHome(activity: Activity)

}