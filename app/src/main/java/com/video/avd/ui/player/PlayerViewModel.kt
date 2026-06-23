package com.video.avd.ui.player

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.CountDownTimer
import android.util.DisplayMetrics
import android.view.TextureView
import android.view.View
import android.view.Window
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import com.video.avd.R
import com.video.avd.constent.VIDEO_PLAYER_ORDER_TYPE
import com.video.avd.repo.PlayerRepository
import com.video.avd.ui.equalizer.video.EqualizerFragmentVideo
import com.video.avd.ui.player.PlayerVideoActivity.Companion.playWhenReady
import com.video.avd.ui.player.PlayerVideoActivity.Companion.player
import com.video.avd.ui.player.bookmark.VideoBookmark
import com.video.avd.ui.player.model.FragmentEvent
import com.video.avd.ui.player.subtitle.SubtitleState
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(private val repository: PlayerRepository) : ViewModel() {
    var isFullScreen = false
    var icBgAudioClicked = false
    var icRepeat = false
    var videosUrlList = MutableLiveData<List<Uri>>()
    var playbackPosition = 0L
    var newPos = 0L
    var currentItem = 0
    var iTag = 134
    var isYtQualityChanged = false
    var sharedPreferences: SharedPreferences? = null
    var lastplaybackforsave: Long = 0L
    val singleList = arrayListOf<Uri>()
    var speedfeatureon = true
    var fastforwardfeature = true
    var autoplayfeature = true
    var savedorientation = false
    var savedbrightness = true
    var savedSpeed = false
    var orientationMode = 0
    var urilistjob : Job? = null
    //Subtitle current video properties
    var currentVideo  : Video? = null
    var currentVideoHasSubtitle = MutableLiveData<Boolean>(false)
    var currentVideoSubtitleTurnOn = false
    var showSubtitleView = MutableLiveData(false)
    var currentVideoSubtitlePath = MutableLiveData<String>("")
    var isSheetbakpress = false

     val openEqualizerEvent = SingleLiveEvent<FragmentEvent>()




    /*------------------------------------------------------New-----------------------------------------------------------*/
    fun setSleepTimer(duration: Int, activity: Activity): CountDownTimer {
        val sleepTimerDuration = duration * 60 * 1000L
        val sleepTimer = object : CountDownTimer(sleepTimerDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Optional: Update UI to show remaining time if desired
            }
            override fun onFinish() {
                try {
                    // Timer expired, close the video
                    if (isFullScreen) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    Toast.makeText(activity, "Timer ended", Toast.LENGTH_SHORT).show()
                    activity.onBackPressed()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
        sleepTimer.start()
        Toast.makeText(activity, "Timer set for $duration minutes", Toast.LENGTH_SHORT).show()
        return sleepTimer
    }

    fun changeVolume(volumeBar: SeekBar, activity: Activity) {
        try {
            val audioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            volumeBar.max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)!!
            volumeBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            PlayerVideoActivity.soundVolume =
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            saveSoundValueToPrefs(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
            volumeBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onStopTrackingTouch(arg0: SeekBar) {}
                override fun onStartTrackingTouch(arg0: SeekBar) {}
                override fun onProgressChanged(arg0: SeekBar, progress: Int, arg2: Boolean) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        progress, 0
                    )
                    //                    saveSoundValueToPrefs(progress)
                }
            })
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun saveSoundValueToPrefs(soundValue: Int) {
        val editor = sharedPreferences?.edit()
        editor?.putInt("system_sound_value", soundValue)
        editor?.apply()
    }

    fun saveSpeedValueToPrefs(soundValue: Float) {
        val editor = sharedPreferences?.edit()
        editor?.putFloat("playerSpeed", soundValue)
        editor?.apply()
    }

    fun getspeed(): Float {
        return sharedPreferences?.getFloat("playerSpeed", 0.0f) ?: 0.0f
    }

    fun saveOrientationPrefs(orientation: Int) {
        val editor = sharedPreferences?.edit()
        editor?.putInt("orientationMode", orientation)
        editor?.apply()
    }

    fun getorientation(): Int? {
        return sharedPreferences?.getInt("orientationMode", 0)
    }

    fun savebrightnessPrefs(orientation: Int) {
        val editor = sharedPreferences?.edit()
        editor?.putInt("brightnessvalue", orientation)
        editor?.apply()
    }

    fun getbrightness(): Int {
        return sharedPreferences?.getInt("brightnessvalue", 0) ?: 50
    }

    fun changeBrightness(brightnessLevel: Float, activity: Activity) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = brightnessLevel
        activity.window.attributes = layoutParams
    }





    fun setSleepTimerViewWidthAndHeight(activity: Activity, timerView: ConstraintLayout, playerViewContainer: ConstraintLayout) {
        try {
            // Get the screen height
            val displayMetrics = DisplayMetrics()
            activity.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels
            val percentage = 0.5f // 50% as a decimal value
            val desiredHeight =
                if (!isFullScreen) (screenHeight * percentage).toInt() else screenHeight
            val desiredWidth = if (isFullScreen) screenWidth / 2 else screenWidth
            // Set the calculated height to the ConstraintLayout
            val layoutParams = timerView.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.height = desiredHeight
            layoutParams.width = desiredWidth
            timerView.layoutParams = layoutParams
            //timerView.visibility = View.VISIBLE
            if (isFullScreen) {
                try {
                    val layoutParams =
                        timerView.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.width = (playerViewContainer.width * 0.5).toInt()
                    timerView.layoutParams = layoutParams
                    layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                    timerView.layoutParams = layoutParams
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        } catch (e: Exception) {

        }
    }

    fun notifyUser(activity: Activity) {
        viewModelScope.launch(Dispatchers.Main) {
            val builder = AlertDialog.Builder(activity).apply {
                // Optional: Prevent dismissal with back button
                setCancelable(false)
            }
            builder.setMessage(activity.getString(R.string.video_not_playable))
                .setPositiveButton(activity.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                    activity.finish()
                }

            val dialog = builder.create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(activity, R.color.brand_text_primary))
            }

            dialog.show()
        }
    }

    fun getVideoNameFromUrl(url: String): String {
        val uri = Uri.parse(url)
        val lastPathSegment = uri.lastPathSegment
        return lastPathSegment ?: "Unknown"
    }

    fun geturiListinBackground(listvideo: List<Video>) {
        try {
            urilistjob=  CoroutineScope(Dispatchers.IO).launch {
                val list = arrayListOf<Uri>()
                for (i in listvideo.orEmpty()) {
                    i.contentUri?.let { url ->
                        list.add(Uri.parse(url))
                    }
                }
                // Mutable live data
                videosUrlList.postValue(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun releasePlayer(playerlistner: Listener,name:String) {
        try {
            if (player != null) {
                player?.let { exoPlayer ->
                    playbackPosition = exoPlayer.currentPosition
                    currentItem = exoPlayer.currentMediaItemIndex
                    playWhenReady = exoPlayer.playWhenReady
                    exoPlayer.removeListener(playerlistner)
                    exoPlayer.stop()
                    exoPlayer.release()
                    player = null
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun releasePlayerfromstart(playerlistner: Listener) {
        try {
            if (player != null) {
               player?.let { exoPlayer ->
                    exoPlayer.stop()
                    currentItem = exoPlayer.currentMediaItemIndex
                    playWhenReady = exoPlayer.playWhenReady
                    exoPlayer.removeListener(playerlistner)
                    exoPlayer.release()
                }
                player = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addToRecents(id: String, list: ArrayList<Video>, addedToRecents: (Boolean) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                try {

                    if (list.isNotEmpty() && id.toInt() >= 0 && id.toInt() < list.size) {
                        val video = list[id.toInt()]
                        video.updatedTimeStump = System.currentTimeMillis()
                        video.timeStump = System.currentTimeMillis()
                        video.isRecent = true
                        video.isNew = false
                        updateUserData(video)
                        addedToRecents(true)
                    }
                } catch (e: NullPointerException) {
                    if (list.isNotEmpty() && id.toInt() >= 0 && id.toInt() < list.size) {
                        val video = list[id.toInt()]
                        video.updatedTimeStump = System.currentTimeMillis()
                        video.timeStump = System.currentTimeMillis()
                        video.isRecent = true
                        video.isNew = false
                        updateUserData(video)
                        addedToRecents(true)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun updateUserData(userEntities: Video){
        repository.updateUserData(userEntities)
    }



   fun insertSubtitleWithVideoId(subtitle : SubtitleState){
       repository.insertSubtitleWithVideoId(subtitle)
   }


    fun getSubtitleFromDB(){
        viewModelScope.launch {
            repository.getVideoWithSubtitle(currentVideo?.id?.toLong() ?: 0L).let {
                if(it!=null){
                    currentVideoHasSubtitle.value = it?.hasSubtitle==true
                    currentVideoSubtitleTurnOn=it?.toggle==true
                    showSubtitleView.postValue(true)
                    currentVideoSubtitlePath.value=it?.subtitlePath ?: ""
                }

            }
        }
    }

    fun checkIFSubtitleTurnOn(){
        viewModelScope.launch {
            repository.getVideoWithSubtitle(currentVideo?.id?.toLong() ?: 0L).let {
                currentVideoSubtitleTurnOn=it?.toggle==true
                showSubtitleView.postValue(true)
                currentVideoHasSubtitle.value = it?.hasSubtitle==true

            }
        }
    }

    fun updateSubtitleState(videoId : Long, toggle : Boolean){
        repository.updateSubtitleState(videoId,toggle)
    }



     fun addVideoBookmark( bookmark : VideoBookmark){
       repository.addVideoBookmark(bookmark)
    }

    suspend fun getVideoBookmarksByUri(uri : String): Flow<List<VideoBookmark>>? {
        return repository.getVideoBookmarksByUri(uri)
    }

   suspend fun deleteVideoBookmark(uri : String, timeStamp : Long){
        repository.deleteVideoBookmark(uri,timeStamp)
    }

     fun renameBookmark(uri : String, timeStamp : Long,name : String){
        repository.renameBookmark(uri,timeStamp,name)
    }

     fun updatePlayerMediaItems(newList: List<Uri>, startPositionIndex: Int = 0, playbackPosition: Long = 0L) {
        val mediaItems = newList.map { MediaItem.fromUri(it) }
        player?.let { exoPlayer ->
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.seekTo(startPositionIndex, playbackPosition) // Seek to the start position and current playback position in the updated list
            exoPlayer.prepare()
            exoPlayer.play() // Start playing from the saved position
        }
    }

     fun repeatMode(activity: Activity) {
        AppUtils.firebaseUserAction("repeatBtnClicked_videoPlayer", "PlayerVideoActivity")
        if (icRepeat) {
            // repeat = false
            icRepeat = false
            VIDEO_PLAYER_ORDER_TYPE.value = 0
            AppPreference.saveVideoPlaylistOrderType(activity, 0)
            player?.repeatMode = Player.REPEAT_MODE_OFF
        } else {
            //   repeat = true
            icRepeat = true
            player?.repeatMode = Player.REPEAT_MODE_ONE
            VIDEO_PLAYER_ORDER_TYPE.value = 3
            AppPreference.saveVideoPlaylistOrderType(activity, 3)
        }
    }

     fun openEqualizer() {
        isSheetbakpress = true
        try {
            AppUtils.firebaseUserAction("equalizerBtnClicked_PlayerVideoFragment", "PlayerVideoFragment")
            val sessionID = player?.audioSessionId
            if (sessionID != null && sessionID != 0) {
                com.video.avd.ui.equalizer.Settings.isEditing = false
                val equalizerFragment = EqualizerFragmentVideo.newBuilder().setAudioSessionId(sessionID).build()
                equalizerFragment.let { openEqualizerEvent.value= FragmentEvent(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

     fun applyHDRFilter(textureView: TextureView) {
        val colorMatrix = ColorMatrix()
        // Adjust brightness (setScale values are now closer to 1 for subtle changes)
        colorMatrix.setScale(1.05f, 1.05f, 1.05f, 1.0f) // Lower brightness from 1.2f to 1.05f
        // Adjust contrast (reduce contrast slightly)
        val contrast = 1.15f // Lower contrast from 1.5f to 1.15f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        colorMatrix.postConcat(ColorMatrix(contrastMatrix))
        // Optionally adjust saturation (to boost color without over-saturation)
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(1.1f) // Slightly boost saturation
        colorMatrix.postConcat(saturationMatrix)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }

     fun getDialogue(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_hdr_show_option)
        return dialog
    }

    override fun onCleared() {
        super.onCleared()
        // Clear any viewModel resources
        viewModelScope.cancel()
        urilistjob?.cancel()
        // Clear any other resources
    }

}