package com.video.avd.ui.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.video.avd.R
import com.video.avd.constent.isClickedForCasting
import com.video.avd.constent.isExpendedRunning
import com.video.avd.data.DataManager
import com.video.avd.ui.player.ChromeCastDelegate.Companion.mChromecastConnection
import com.video.avd.ui.player.ChromeCastDelegate.Companion.mPreparingConnectionDialog
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.convertToMilliseconds
import com.video.avd.utils.ServiceUtils
import com.video.avd.utils.chromecast.ChromecastConnection
import com.video.avd.utils.chromecast.DialogFactory
import com.video.avd.utils.chromecast.ExpandedCControls
import com.video.avd.utils.chromecast.IpUtils
import com.video.avd.utils.chromecast.MediaWebService
import com.video.avd.utils.chromecast.ToastUtils
import com.video.avd.utils.chromecast.constent.CastConstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChromeCastDelegateImp : ChromeCastDelegate {
    private var mPrepareServerDialog: SweetAlertDialog? = null
    override var mSelectedMedia: ArrayList<Video>? = null
    private var mSelectedPosition = -1
    var mIsPlayingByChromeCast = false
    var mTimePlayingByChromeCast: Long = 0
    var mIsUsedToBeFailed = false
//    var mServer: LocalFileStreamingServer? = null

    //cast
//    override var mChromecastConnection: ChromecastConnection? = null
    override var mDefaultCastStateListener: ChromecastConnection.CastStateUpdateListener? = null


    override fun mDefaultRequestSessionCallbackFun(activity: Activity): ChromecastConnection.RequestSessionCallback {
        val mDefaultRequestSessionCallback: ChromecastConnection.RequestSessionCallback =
            object : ChromecastConnection.RequestSessionCallback() {
                override fun onError(errorCode: Int) {
                    hidePrepareConnectionDialog()
                    ToastUtils.showMessageLong(
                        activity,
                        activity.getString(R.string.cast_start_casting_error)
                    )
                }

                override fun onCancel() {
                    Toast.makeText(activity, "Canceled", Toast.LENGTH_SHORT)
                        .show()
//                    ToastUtils.showMessageLong(
//                        activity,
//                        activity.getString(R.string.cast_start_casting_error)
//                    )
                }

                override fun onDialogShow() {
                }

                override fun onItemClick() {
                    showPrepareConnectionDialog(activity)
                }

                override fun onDialogCanNotShow() {
                    hidePrepareConnectionDialog()
                    ToastUtils.showMessageLong(
                        activity,
                        activity.getString(R.string.cast_start_casting_error)
                    )
                }

                override fun onJoinedSuccess() {
                    ToastUtils.showMessageLong(
                        activity,
                        activity.getString(R.string.cast_start_casting_success)
                    )
                }
            }

        return mDefaultRequestSessionCallback
    }


    override fun startChromeCastConnectionForLiveVideos(activity: Activity, parse: String) {
        if (mChromecastConnection?.isChromeCastConnect == true) {
            startMediaServiceFromYoutube(activity, parse)
        } else if (mChromecastConnection != null) {
            requestStartSessionWithCallback(activity, mChromecastConnection!!,
                {
                    startMediaServiceFromYoutube(activity, parse)
                }) {}
        }
    }

    override fun startChromeCastConnection(
        fileData: List<Video>,
        activity: Activity,
        position: Int
    ) {
        if (mChromecastConnection?.isChromeCastConnect == true) {
            startMediaService(fileData, activity, position)
        } else {
            if (mChromecastConnection != null) {
                requestStartSessionWithCallback(
                    activity,
                    mChromecastConnection!!,
                    {
                        startMediaService(fileData, activity, position)
                    }) {
                }
            }
        }
    }

    override fun startChromeCastConnectionfromList(
        fileData: List<Video>,
        activity: Activity,
        position: Int
    ) {
        startMediaServiceFromHome(fileData, activity, position)
    }


    override fun prepareSimpleWebServer(
        activity: Activity,
        primaryCallback: Runnable,
        successCallback: Runnable
    ) {
//        mPrepareServerDialog =
//            DialogFactory.getDialogProgress(
//                activity,
//                activity.getString(R.string.cast_start_casting_message)
//            )
//        mPrepareServerDialog?.setCancelable(false)
//        mPrepareServerDialog?.show()

        val deviceIpAddress: String? = IpUtils.findIPAddress(activity.applicationContext)
        if (deviceIpAddress == null) {
            ToastUtils.showMessageLong(
                activity,
                activity.getString(R.string.cast_start_casting_error)
            )
            hidePrepareServerDialog()
            return
        }
        if (ServiceUtils.isMyServiceRunning(
                MediaWebService::class.java,
                activity
            ) && deviceIpAddress == DataManager.getInstance(activity).lastIPAddress
        ) {
            primaryCallback.run()
            successCallback.run()
            hidePrepareServerDialog()
        } else {
            primaryCallback.run()
            val pendingResult = activity.createPendingResult(
                CastConstant.START_MEDIA_SERVICE,
                Intent(),
                0
            )
            val serviceIntent = Intent(activity, MediaWebService::class.java)
            val extras = Bundle()
            extras.putString(CastConstant.IP_LINK_KEY, deviceIpAddress)
            serviceIntent.putExtra(CastConstant.PENDING_INTENT_SERVICE, pendingResult)
            serviceIntent.putExtras(extras)
            activity.startService(serviceIntent)

        }
    }

    override fun updateSelectedPosition(position: Int) {
        mSelectedMedia?.let {
            if (position >= 0 && position < it.size) {
                //  mSelectedMedia = mListFile as ArrayList
                mSelectedPosition = position
//            mVideoListAdapter.setCurrentItem(position)
            } else {
                mSelectedPosition = -1
                //  mSelectedMedia = null
//            mVideoListAdapter.setCurrentItem(-1)
            }
        }
    }

    override fun updatePosition(position: Int) {
        ChromecastConnection.listofvideos?.let { list ->
            if (position >= 0 && position < list.size) {
                mSelectedPosition = position
            } else {
                mSelectedPosition = -1
            }
        }

    }

    override fun loadRemoteMedia(activity: Activity) {
        if (mChromecastConnection?.isChromeCastConnect == false) {
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_error)
            )
            updateSelectedPosition(-1)
            return
        }
        val remoteMediaClient = mChromecastConnection?.session?.remoteMediaClient
        if (remoteMediaClient == null) {
            mIsUsedToBeFailed = true
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_error)
            )
            updateSelectedPosition(-1)
            return
        }
        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                if (remoteMediaClient.isPlaying || remoteMediaClient.isPaused || remoteMediaClient.isBuffering) {
                    mIsUsedToBeFailed = false
                    mIsPlayingByChromeCast = true
                    mTimePlayingByChromeCast = System.currentTimeMillis()
                    val intent = Intent(activity, ExpandedCControls::class.java)
                    activity.startActivity(intent)
                    remoteMediaClient.unregisterCallback(this)
                }

            }

            override fun onMediaError(mediaError: MediaError) {
                super.onMediaError(mediaError)
                ToastUtils.showMessageLong(
                    activity.applicationContext,
                    activity.getString(R.string.cast_start_casting_playing_fail)
                )
                updateSelectedPosition(-1)
            }
        })
        activity.runOnUiThread {
            try {
                if (remoteMediaClient.isPlaying) {
                    remoteMediaClient.stop()
                }
                val queueItems = mSelectedMedia?.map { video ->
                    val mediaInfo =
                        buildMediaInfo(activity, video) // Method to create MediaInfo from Video

                    mediaInfo?.let {
                        MediaQueueItem.Builder(it).build()
                    }

                }
                remoteMediaClient.let { client ->
                    if (client.isPlaying) {
                        client.stop()
                    }
                    queueItems?.toTypedArray()?.let {
                        client.queueLoad(
                            it,
                            mSelectedPosition,
                            MediaStatus.REPEAT_MODE_REPEAT_OFF,
                            0,
                            JSONObject()
                        )
                    }
                }
//                    getViewModel().saveHistory(mSelectedMedia)
            } catch (e: java.lang.Exception) {
                mIsUsedToBeFailed = true
                Log.d("errorMessage", "${e.message}")
                ToastUtils.showMessageLong(
                    activity.applicationContext,
                    activity.getString(R.string.cast_start_casting_playing_fail)
                )
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(2000L)
            if (activity is PlayerVideoActivity) {
                activity.finish()
            } else {
                Log.d("Lopig", "Main")
            }
        }
    }


    override fun loadNextVideo(activity: Activity) {
        val remoteMediaClient = mChromecastConnection?.session?.remoteMediaClient
        remoteMediaClient?.let { remoteMediaClient ->
            remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
                val status = remoteMediaClient.mediaStatus
                override fun onStatusUpdated() {
                    super.onStatusUpdated()
                    if (status?.playerState == MediaStatus.PLAYER_STATE_IDLE && status?.idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                        // Video has finished playing
                        // Handle this event here (e.g., load next video, update UI)
                        Log.d("statusOfCast", "before  ${ChromecastConnection.position}")

                        Log.d("sssss", "got calledd from inside")
                        var isMp4: Boolean

                        ChromecastConnection.position++
                        var newPosition = ChromecastConnection.position
                        mSelectedMedia?.let {
                            while (newPosition < it.size) {
                                val currentUri = it[newPosition].contentUri
                                isMp4 = currentUri?.let { uri ->
                                    AppUtils.isSupportedVideoFile(activity, Uri.parse(uri))
                                } ?: false
                                if (isMp4) {
                                    break // Found an MP4 file, exit the loop
                                }
                                newPosition++
                                ChromecastConnection.position++
                            }
                        }
                        updateSelectedPosition(newPosition)
                        val item = mSelectedMedia?.get(mSelectedPosition)
                        val mediaInfo: MediaInfo? =
                            item?.let { buildMediaInfo(activity, video = it) }
                        if (mediaInfo != null) {
                            activity.runOnUiThread {
                                try {
                                    remoteMediaClient.load(
                                        MediaLoadRequestData.Builder()
                                            .setMediaInfo(mediaInfo)
                                            .setAutoplay(true).build()
                                    )
                                } catch (e: java.lang.Exception) {
                                    mIsUsedToBeFailed = true
                                    Log.d("errorMessage", "${e.message}")
                                    ToastUtils.showMessageLong(
                                        activity.applicationContext,
                                        activity.getString(R.string.cast_start_casting_playing_fail)
                                    )
                                }
                            }
                        }
                        Log.d("statusOfCast", "afterr  ${mSelectedPosition}")

                    }
                }
            })
        }

    }

    override fun loadRemoteMediaForStreaming(activity: Activity, parse: String) {
        if (mChromecastConnection?.isChromeCastConnect == false) {
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_error)
            )
            return
        }
        val remoteMediaClient = mChromecastConnection?.session?.remoteMediaClient
        if (remoteMediaClient == null) {
            mIsUsedToBeFailed = true
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_error)
            )
            return
        }
        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                if (remoteMediaClient.isPlaying || remoteMediaClient.isPaused || remoteMediaClient.isBuffering) {
                    mIsUsedToBeFailed = false
                    isClickedForCasting.value = false
                    mIsPlayingByChromeCast = true
                    mTimePlayingByChromeCast = System.currentTimeMillis()
                    val intent = Intent(activity, ExpandedCControls::class.java)
                    activity.startActivity(intent)
                    remoteMediaClient.unregisterCallback(this)
                }
            }

            override fun onMediaError(mediaError: MediaError) {
                super.onMediaError(mediaError)
                ToastUtils.showMessageLong(
                    activity.applicationContext,
                    activity.getString(R.string.cast_start_casting_playing_fail)
                )
                updateSelectedPosition(-1)
            }
        })

        val mediaInfo: MediaInfo? = buildMediaInfoForStreaming(parse)
        if (mediaInfo == null) {
            mIsUsedToBeFailed = true
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_playing_fail)
            )
        } else {
            activity.runOnUiThread {
                try {
                    remoteMediaClient.load(
                        MediaLoadRequestData.Builder()
                            .setMediaInfo(mediaInfo)
                            .setAutoplay(true).build()
                    )
                } catch (e: java.lang.Exception) {
                    mIsUsedToBeFailed = true
                    ToastUtils.showMessageLong(
                        activity.applicationContext,
                        activity.getString(R.string.cast_start_casting_playing_fail)
                    )
                }
            }
        }
       CoroutineScope(Dispatchers.Main).launch {
            delay(5000L)
            if (activity is PlayerVideoActivity) {
                activity.finish()
            }
        }
    }


    override fun loadRemoteMediaFromPlaylist(activity: Activity) {
        if (mChromecastConnection?.isChromeCastConnect == false) {
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_error)
            )
            updateSelectedPosition(-1)
            return
        }
        val remoteMediaClient = mChromecastConnection?.session?.remoteMediaClient
        if (remoteMediaClient == null) {
            mIsUsedToBeFailed = true
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_error)
            )
            updateSelectedPosition(-1)
            return
        }
        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val status = remoteMediaClient.mediaStatus
                Log.d("expandeddstatusfff", "status updated")
                if (remoteMediaClient.isPlaying || remoteMediaClient.isPaused || remoteMediaClient.isBuffering) {
                    mIsUsedToBeFailed = false
                    isClickedForCasting.value = false
                    mIsPlayingByChromeCast = true
                    mTimePlayingByChromeCast = System.currentTimeMillis()
                    Log.d("expandeddstatusfff", "status updated in if condition ")
                    Log.d("expandeddd", "arrived outside")
                    Log.d("expandeddd", "arrived inside")
                    isExpendedRunning = true
                    val intent = Intent(activity, ExpandedCControls::class.java)
                    activity.startActivity(intent)
                    remoteMediaClient.unregisterCallback(this)
                }


                /*     if (status?.playerState == MediaStatus.PLAYER_STATE_IDLE && status?.idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                         var isMp4: Boolean
                         Log.d("expandeddstatusfff", "status updated in if dasda ")
                         ChromecastConnection.position++
                         var newPosition = ChromecastConnection.position
                         mSelectedMedia?.let {
                             while (newPosition < it.size) {
                                 val currentUri = it[newPosition].contentUri
                                 isMp4 = currentUri?.let { uri ->
                                     AppUtils.isSupportedVideoFile(activity, uri)
                                 } ?: false
                                 if (isMp4) {
                                     break // Found an MP4 file, exit the loop
                                 }
                                 newPosition++
                                 ChromecastConnection.position++
                             }
                         }
                         updateSelectedPosition(newPosition)
                         loadRemoteMediaFromPlaylist(activity)
                         *//*  val item = mSelectedMedia?.get(mSelectedPosition)
                      val mediaInfo: MediaInfo? = item?.let { buildMediaInfo(activity, video = it) }
                      if (mediaInfo != null) {
                          activity.runOnUiThread {
                              try {
                                  remoteMediaClient.load(
                                      MediaLoadRequestData.Builder()
                                          .setMediaInfo(mediaInfo)
                                          .setAutoplay(true).build()
                                  )
                              } catch (e: java.lang.Exception) {
                                  mIsUsedToBeFailed = true
                                  Log.d("errorMessage", "${e.message}")
                                  ToastUtils.showMessageLong(
                                      activity.applicationContext,
                                      activity.getString(R.string.cast_start_casting_playing_fail)
                                  )
                              }
                          }
                      }
*//*
                }*/
            }

            override fun onMediaError(mediaError: MediaError) {
                super.onMediaError(mediaError)
                ToastUtils.showMessageLong(
                    activity.applicationContext,
                    activity.getString(R.string.cast_start_casting_playing_fail)
                )
                isClickedForCasting.value = false
                updateSelectedPosition(-1)
            }
        })

        val item = mSelectedMedia?.get(mSelectedPosition)
        val mediaInfo: MediaInfo? = item?.let { buildMediaInfo(activity, video = it) }
        Log.d("mediaInfo",mediaInfo.toString())
        if (mediaInfo == null) {
            mIsUsedToBeFailed = true
            ToastUtils.showMessageLong(
                activity.applicationContext,
                activity.getString(R.string.cast_start_casting_playing_fail)
            )
            updateSelectedPosition(-1)
        } else {
            activity.runOnUiThread {
                try {
                    remoteMediaClient.load(
                        MediaLoadRequestData.Builder()
                            .setMediaInfo(mediaInfo)
                            .setAutoplay(true).build()
                    )
                } catch (e: java.lang.Exception) {
                    mIsUsedToBeFailed = true
                    Log.d("errorMessage", "${e.message}")
                    ToastUtils.showMessageLong(
                        activity.applicationContext,
                        activity.getString(R.string.cast_start_casting_playing_fail)
                    )
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                delay(5000L)
                if (activity is PlayerVideoActivity) {
                    activity.finish()
                } else {
                    Log.d("Lopig", "Main")
                }
            }
        }
    }

    override fun hidePrepareServerDialog() {
        try {
            if (mPrepareServerDialog != null && mPrepareServerDialog?.isShowing == true) {
                mPrepareServerDialog?.dismiss()
            }
            if (mPreparingConnectionDialog != null && mPreparingConnectionDialog?.isShowing == true) {
                mPreparingConnectionDialog?.dismiss()
            }
        } catch (ignored: java.lang.Exception) {
            Log.d("statusOFCast", "${ignored.message}")
        }
    }

    override fun showPrepareConnectionDialog(activity: Activity) {
        try {
            if (mPreparingConnectionDialog == null) {
                mPreparingConnectionDialog =
                    DialogFactory.getDialogProgress(
                        activity,
                        activity.getString(R.string.prepare_connection)
                    )
            }
            mPreparingConnectionDialog?.setCancelable(false)
            mPreparingConnectionDialog?.show()
        } catch (ignored: java.lang.Exception) {
        }
    }

    override fun showPrepareConnectionDialogFromHome(activity: Activity) {
        try {
            if (mPreparingConnectionDialog == null) {
                mPreparingConnectionDialog =
                    DialogFactory.getDialogProgress(
                        activity,
                        activity.getString(R.string.prepare_connection)
                    )
            }
            mPreparingConnectionDialog?.setCancelable(false)
            mPreparingConnectionDialog?.show()
        } catch (ignored: java.lang.Exception) {
        }
    }

    private fun startMediaServiceFromHome(
        fileData: List<Video>,
        activity: Activity,
        position: Int
    ) {
        mSelectedMedia = if (!fileData.isNullOrEmpty()) {
            ArrayList(fileData)
        } else {
            null
        }
        updateSelectedPosition(position)
        this.loadRemoteMediaFromPlaylist(activity)
    }


    private fun startMediaServiceFromYoutube(activity: Activity, uri: String) {
        prepareSimpleWebServer(
            activity,
            {
                Log.d("successfully", "Done")
            }) {
            loadRemoteMediaForStreaming(activity, uri)
        }
    }

    private fun startMediaService(fileData: List<Video>, activity: Activity, position: Int) {
        prepareSimpleWebServer(
            activity,
            {
                mSelectedMedia = if (!fileData.isNullOrEmpty()) {
                    ArrayList(fileData)
                } else {
                    null
                }
                updateSelectedPosition(position)
            }
        )
        {
            this.loadRemoteMediaFromPlaylist(activity)

        }
    }

    override fun startWebServerFromHome(activity: Activity) {
        prepareSimpleWebServer(
            activity,
            {
                Log.d("successfully", "Done")
            },
            {
                //  this.loadRemoteMedia(activity)
            })
    }


    private fun requestStartSessionWithCallback(
        activity: Activity,
        chromecastConnection: ChromecastConnection,
        successCallback: Runnable,
        failCallback: Runnable
    ) {
        chromecastConnection.requestStartSession(object :
            ChromecastConnection.RequestSessionCallback() {
            override fun onError(errorCode: Int) {
                hidePrepareConnectionDialog()
                failCallback.run()
            }

            override fun onCancel() {
//                ToastUtils.showMessageLong(
//                    activity.applicationContext,
//                    activity.getString(R.string.cancelled)
//                )
            }

            override fun onDialogShow() {

            }

            override fun onItemClick() {
                showPrepareConnectionDialog(activity)
            }

            override fun onDialogCanNotShow() {

            }

            override fun onJoinedSuccess() {
                successCallback.run()
            }
        })
    }


    private fun buildMediaInfoForStreaming(videoUrl: String): MediaInfo? {
        if (videoUrl.isEmpty()) {
            return null
        }

        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, "")

        movieMetadata.putString(MediaMetadata.KEY_TITLE, "live streaming")

        // Build the MediaInfo object
        return MediaInfo.Builder(videoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4") // Use appropriate content type based on video format
            .setMetadata(movieMetadata)
            .build()
    }


    private fun buildMediaInfo(activity: Activity, video: Video): MediaInfo? {
        if (mSelectedMedia == null || Uri.parse(video.contentUri)?.path == null) {
            return null
        } else {
            val rootLink = Environment.getExternalStorageDirectory().absolutePath
            val ipAddress = DataManager.getInstance(activity).lastIPAddress
            var mediaLink = ""
            val path =
                AppUtils.getPathFromUri(activity, Uri.parse(video.contentUri.toString()))
            mediaLink =
                if (video != null && path != null && path.contains(rootLink)) {
                    path.replace(rootLink, "http://$ipAddress:8080")
                } else {
                    ""
                }
            val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, "")
            video?.let { movieMetadata.putString(MediaMetadata.KEY_TITLE, it.title ?: "") }
            return MediaInfo.Builder(mediaLink)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("")
                .setMetadata(movieMetadata)
                .setStreamDuration(convertToMilliseconds(video.duration))
                .build()
        }
    }


    private fun hidePrepareConnectionDialog() {
        try {
            if (mPreparingConnectionDialog != null && mPreparingConnectionDialog?.isShowing == true) {
                mPreparingConnectionDialog?.dismiss()
            }
        } catch (ignored: java.lang.Exception) {
        }
    }

    override var mPosition: Int
        get() = 0
        set(value) {}


}