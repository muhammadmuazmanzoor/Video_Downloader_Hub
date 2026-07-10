package com.video.avd.ui.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.airbnb.lottie.FontAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.TextDelegate
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.hideLoading
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.inter_videos
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.AdBlockerHelper.showInterstitialPlayer
import com.bumptech.glide.Glide
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.ads.AppLovinAdUtils
import com.video.avd.constent.VIDEO_PLAYER_ORDER_TYPE
import com.video.avd.constent.backFromPlayer
import com.video.avd.constent.isCastingForStreaming
import com.video.avd.constent.isSplash
import com.video.avd.constent.isbackfromplayer
import com.video.avd.constent.videoListLocal
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentPlayerVideoBinding
import com.video.avd.ui.equalizer.video.EqualizerFragmentVideo
import com.video.avd.ui.playbackspeed.BottomSheetPlaybackSpeed
import com.video.avd.ui.player.ChromeCastDelegate.Companion.mChromecastConnection
import com.video.avd.ui.player.audiotrack.AudioTrack
import com.video.avd.ui.player.audiotrack.AudioTrackImpl
import com.video.avd.ui.player.bookmark.BookmarkRemoveListener
import com.video.avd.ui.player.bookmark.VideoBookmarkDialogFragment
import com.video.avd.ui.player.callback.PauseVideoCallBack
import com.video.avd.ui.player.model.FragmentEvent
import com.video.avd.ui.player.playersettingdelegate.PlayerSettingDelegate
import com.video.avd.ui.player.playersettingdelegate.PlayerSettingDelegateImpl
import com.video.avd.ui.player.playlist.PlaylistAdapter
import com.video.avd.ui.player.playlist.PlaylistBottomSheetFragment
import com.video.avd.ui.player.subtitle.SubtitleDelegate
import com.video.avd.ui.player.subtitle.SubtitleDelegateImpl
import com.video.avd.ui.player.subtitle.SubtitleState
import com.video.avd.ui.status_saver.StatusViewModel
import com.video.avd.ui.videos.model.Video
import com.video.avd.ads.AppOpenManager.Companion.isShowingAd
import com.video.avd.ads.AppOpenManager.Companion.pauseVideoCallback
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppPreference.isFirstPlayLaunch
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.getWindow
import com.video.avd.utils.AppUtils.saveScreenshotToExternalStorage
import com.video.avd.utils.AppUtils.shareVideo
import com.video.avd.utils.AppUtils.stringForTime
import com.video.avd.utils.AppUtils.takeScreenshotWithPixelCopy
import com.video.avd.utils.CustomTimeBar
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.SharedPreferencesManager
import com.google.android.gms.cast.framework.CastContext
import com.video.avd.BuildConfig
import com.video.avd.ui.splash_flow.activities.InAppActivity
import com.video.avd.utils.chromecast.ChromecastConnection
import com.video.avd.utils.chromecast.ToastUtils
import com.video.avd.utils.chromecast.constent.CastConstant
import com.video.avd.utils.ifAdDisplayed
import com.video.avd.utils.isRendererAvailable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@AndroidEntryPoint
class PlayerVideoActivity : AppCompatActivity(), RewardAdDismissListener,
    PlayerDelegate by PlayerDelegateImpl(), ChromeCastDelegate by ChromeCastDelegateImp(),
    AudioTrack by AudioTrackImpl(), PauseVideoCallBack, SubtitleDelegate by SubtitleDelegateImpl(),
    BottomSheetPlaybackSpeed.PlaybackSpeedListener,
    PlayerSettingDelegate by PlayerSettingDelegateImpl(), OnUserEarnedRewardListener {
    private var isPlayerBackHandling = false
    val tHRESHOLD = 80
    var binding: FragmentPlayerVideoBinding? = null
    private val viewModel: PlayerViewModel by viewModels()
    private val statusViewModel: StatusViewModel by viewModels()
    private var debounceJob: Job? = null
    private var actualIsPlaying: Boolean = false
    private var debounceJobPlay: Job? = null
    var ispause = false
    private lateinit var youtubeUrl: String
    private var isYoutubeLink = false
    private var isOnlineStreaming = false
    private var isHistory = false
    private var isPlaybackCount = false
    var job: Job? = null
    var isLock = false
    private var hideViewJob: Job? = null
    var subtitleToggle: Boolean = false
    var uriList = arrayListOf<Uri>()
    var position = 0
    private var sharedPreferences: SharedPreferencesManager? = null
    var isliveuri = false
    var alreadyAdShown = false
    var isVault = false
    var isFromTrimmer = false
    var startX = 0.0f
    var startY = 0.0f
    var mScreenWidth = 0
    var mScreenHeight = 0
    var currentPosition: Int? = 0
    var mChangeVolume = false
    var mChangePosition = false
    var mChangeBrightness = false
    var mGestureDownVolume = 0
    var mGestureDownBrightness = 0f
    var trackname = ""
    var fragmentclose: Fragment? = null
    private val longClickDuration = 600L
    private var isLongPress = false
    private var isMovementActived = false
    private var isPinchedZoomActived = false
    private var isLongPressedActived = false
    val handler = Handler(Looper.getMainLooper())
    private var lastVideoDuration = 0L
    var dontplaybackuntilkill = false
    private var isFromLaunchers = false

    // Define the current resize mode index
    private var currentResizeModeIndex = 0
    var listvideos = listOf<Video>()
    var dark = false
    var sleepTimer: CountDownTimer? = null
    var favoriteLocalClick = false
    var playSingleVideo = false
    private var isFlipped = false
    private var prevousFragment = ""
    private var brightness = 0
    var videooutside = false
    private val doubleTapTimeout by lazy {
        300
    }
    var mask = false

    // Adjust this as needed
    // Define the resize modes to cycle through
    var mDownX: Float = 0f
    var mDownY = 0f
    private val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    )
    private lateinit var lottieAnimationView: LottieAnimationView

    // private val animationList = listOf(R.raw.volume_new,R.raw.brightness_new,R.raw.zoom_new)
    private var currentAnimationIndex = 0
    var lastTapTime: Long = 0
    var tapCount: Int = 0
    var mGestureDownPosition: Long? = 0
    var mSeekTimePosition: Long = 0

    var mAudioManager: AudioManager? = null
    private var volumeObserver: ContentObserver? = null

    private var fromOpenWithOption = false

    //required to check when applying subtitle
    private var isPlayerInitialized = MutableLiveData(false)

    /////////////////// audio
    var trackselector: DefaultTrackSelector? = null

    ////// action Zoom
    private var scaleFactor = 1.0f

    // Add this variable to keep track of initial distance between fingers for pinch zoom
    private var initialDistance = 0f

    // Add this variable to keep track of initial scale factor for pinch zoom
    private var initialScaleFactor = 1.0f

    //// pip close open new video same video play issue variable
    var wasInPictureInPictureMode: Boolean = false

    // Define minimum and maximum scale factors
    val MIN_SCALE = 0.7f  // No zoom out beyond 100%
    val MAX_SCALE = 5.0f  // Limit zoom in to 500%
    var isFirst = false
    private lateinit var renderersFactory: DefaultRenderersFactory
    private var updateJob: Job? = null

    //////////////// current item for saving playback
    var itemsaving: Video? = null

    //check if previous fragment is from playlist or not.
    private var isFromPlaylist = false

    private var playListBottomSheetFragment: PlaylistBottomSheetFragment? = null

    private var playerMode = "auto"


    private var exoBar: CustomTimeBar? = null
    private var bookmarkDialogShown = false

    private var isHDREnabled = false
    var latestUriList: List<Uri>? = listOf()
    private var hdrOptionDialog: Dialog? = null

    ///// dubel click block btn_play
    private var mLastClickTime: Long = 0
    private var lastClickTime: Long = 0
    val CLICK_COOLDOWN: Long = 1000

    /// BG not Allowed
    var isBgNotAllowed = false

    ////jobs
    var addtorecentjob: Job? = null

    // Create a Job for managing the coroutine scope
    private val jobnew = Job()

    // Create a CoroutineScope with the Main dispatcher and the job
    private val scope = CoroutineScope(Dispatchers.Main + jobnew)

    var bookmarkDialog: VideoBookmarkDialogFragment? = null


    //////
    private val TAG = "TouchDebug"
    private var longPressRunnable: Runnable? = null


    ////////////////////////////// testing for delay player
    //////////////////////////////////////
    ///////////////////////////////////////////////
//    private val resourceMonitorHandler = Handler(Looper.getMainLooper())
//    private val resourceMonitorRunnable = object : Runnable {
//        override fun run() {
//            logMemoryInfo()
//            monitorPlayerResources()
//            resourceMonitorHandler.postDelayed(this, 60000) // Monitor every minute
//        }
//    }
//
//    private fun logMemoryInfo() {
//        val runtime = Runtime.getRuntime()
//        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
//        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
//        val availableHeapSizeInMB = maxHeapSizeInMB - usedMemInMB
//
//
//        Used Memory: $usedMemInMB MB
//        Max Heap Size: $maxHeapSizeInMB MB
//        Available Heap: $availableHeapSizeInMB MB
//    """.trimIndent())
//    }
//
//    private fun monitorPlayerResources() {
//        player?.let { exoPlayer ->
//
//            Active Renderers: ${exoPlayer.rendererCount}
//            Current Timeline: ${exoPlayer.currentTimeline.windowCount}
//            Buffered Position: ${exoPlayer.bufferedPosition}
//            Total Buffered: ${exoPlayer.totalBufferedDuration}
//        """.trimIndent())
//        }
//    }
    /////////////////////////////////////////////////////
    /////////////////////////////

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "UseCompatLoadingForDrawables")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.setLocate(this)
        overridePendingTransition(R.anim.player_enter_anim, R.anim.player_stay)
        try {
            binding = FragmentPlayerVideoBinding.inflate(layoutInflater)
            setContentView(binding?.root)
            
            // Optimize rendering to reduce GPU load and prevent ANRs
            optimizeRenderingPerformance()
            
            player?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            listvideos = videolistglobal
            lifecycleScope.launch {
                setupController()
            }
            setInstance(this@PlayerVideoActivity)
            AppUtils.firebaseUserAction("onCreateView_PlayerVideoFragment", "PlayerVideoFragment")
            //data collect
            position = intent.getStringExtra("id")?.toInt() ?: 0
            getIntentExtras()
            viewModel.savedorientation = rememberorientation(this)
            viewModel.savedbrightness = rememberbrightness(this)
            viewModel.savedSpeed = rememberspeed(this)
            GlobalValues.hidePopupPlayer.postValue(true)
            if (!viewModel.savedorientation) {
                binding?.playbacklayout?.playbackmain?.let { setBottomMargin(it, 130f, this) }
            }


            binding?.icRotation?.setOnClickListener {
                binding?.videoEqualizerContainer?.visibility = View.GONE
                rotateScreen()
            }

            binding?.icTimer?.setOnClickListener {
                try {
                    binding?.bottomView?.visibility = View.GONE
                    binding?.timerView?.visibility = View.VISIBLE
                    binding?.timerView?.let { it1 ->
                        binding?.playerViewContainer?.let { it2 ->
                            viewModel.setSleepTimerViewWidthAndHeight(this, timerView = it1, it2)
                        }
                    }
                    AppUtils.firebaseUserAction(
                        "sleepBtnClicked_PlayerVideoFragment",
                        "PlayerVideoFragment"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            binding?.icRepeat?.setOnClickListener {
                binding?.videoEqualizerContainer?.visibility = View.GONE
                if (viewModel.icRepeat) {
                    binding?.icRepeat?.setImageDrawable(resources.getDrawable(R.drawable.ic_repeat))
                } else {
                    binding?.icRepeat?.setImageDrawable(resources.getDrawable(R.drawable.ic_repeat_on))
                }
                viewModel.repeatMode(this@PlayerVideoActivity)
            }


            binding?.icScreenshot?.setOnClickListener {
                binding?.videoEqualizerContainer?.visibility = View.GONE
                captureScreen()
            }


            /////init prefs
            viewModel.sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            sharedPreferences = SharedPreferencesManager(this)
            isFirstPlayLaunch = isFirstPlayLaunch(this@PlayerVideoActivity)
            viewModel.orientationMode = viewModel.getorientation()!!
            if (!isFirstPlayLaunch) {
                requestedOrientation =
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    else ActivityInfo.SCREEN_ORIENTATION_SENSOR
                isFirst = true
            } else {
                if (viewModel.savedorientation) {
                    when (viewModel.orientationMode) {
                        0 -> {
                            binding?.playbacklayout?.playbackmain?.let {
                                setBottomMargin(
                                    it, 130f, this
                                )
                            }
                            requestedOrientation =
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                else ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        }

                        1 -> {
                            requestedOrientation =
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            binding?.playbacklayout?.playbackmain?.let {
                                setBottomMargin(
                                    it, 65f, this
                                )
                            }
                        }

                        2 -> {
                            binding?.playbacklayout?.playbackmain?.let {
                                setBottomMargin(
                                    it, 130f, this
                                )
                            }
                            requestedOrientation =
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                else {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                        }

                        else -> {
                            requestedOrientation =
                                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                else ActivityInfo.SCREEN_ORIENTATION_SENSOR
                        }
                    }
                } else {
                    requestedOrientation = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    }
                }

            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    binding?.root?.findViewById<ImageView>(R.id.pip_button)?.visibility =
                        View.INVISIBLE
                } else {
                    // Register the broadcast receiver
                    this.registerReceiver(
                        playPauseReceiver,
                        IntentFilter("PIP_PLAY_PAUSE_PLAYER"), RECEIVER_EXPORTED
                    )

                    binding?.volumeBar?.let { viewModel.changeVolume(it, this) }
                }
            } else {
                binding?.root?.findViewById<ImageView>(R.id.pip_button)?.visibility = View.INVISIBLE
            }
            isPipMode = false
            startObserver()
            //update UI
            if (isVault) {
                binding?.menuButton?.visibility = View.INVISIBLE
                binding?.cast?.visibility = View.INVISIBLE
                binding?.icShare?.visibility = View.INVISIBLE
                binding?.tvShare?.visibility = View.INVISIBLE
            } else if (fromOpenWithOption) {
                binding?.cast?.visibility = View.GONE
            } else if (isFromTrimmer) {
                binding?.cast?.visibility = View.GONE
            } else if (!prevousFragment.isNullOrBlank() && (prevousFragment == "Status" || prevousFragment == "downloads_")) {
                binding?.icDelete?.visibility = View.INVISIBLE
                binding?.tvDelete?.visibility = View.INVISIBLE
                binding?.cast?.visibility = View.GONE
            } else {
                binding?.cast?.visibility = View.VISIBLE
            }

            if (!viewModel.savedbrightness) {
                brightness =
                    Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                binding?.brightnessBar?.progress = 50
                viewModel.changeBrightness(0.5f, this)
            } else {
                brightness = viewModel.getbrightness()
                binding?.brightnessBar?.progress = brightness
                if (brightness.toInt() >= 100) {
                    viewModel.changeBrightness(1.0f, this)
                } else {
                    viewModel.changeBrightness(brightness.toFloat(), this)
                }
            }
            mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            binding?.root?.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_prev)
                ?.setOnClickListener {
                    player?.seekToPrevious()
                }

            viewModel.speedfeatureon = speed2xHandle(this)
            viewModel.fastforwardfeature = fastForward(this)
            viewModel.autoplayfeature = autoplay(this)

            binding?.menuButton?.setOnClickListener {
                AppUtils.firebaseUserAction("menuBtnClicked_videoPlayer", "PlayerVideoActivity")
                binding?.videoEqualizerContainer?.visibility = View.GONE
                binding?.bottomView?.visibility = View.VISIBLE
                setBottomViewWidthAndHeight()
                buttonClickListeners()
                AppUtils.hideFragment(this@PlayerVideoActivity, "videoEqualizer")
                binding?.videoView?.hideController()
                lifecycleScope.launch {
                    hideSystemUI()
                }
                binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
            }

            binding?.hdrquality?.setOnClickListener {
                binding?.videoEqualizerContainer?.visibility = View.GONE
                var getPlayCountHDR = sharedPreferences?.getPlayCountHDR() ?: 1
                if (AdBlockerHelper.isProVersion.value != true && getPlayCountHDR > 2) {
                    if (isHDREnabled) {
                        isHDREnabled = false
                        removeHDRFilter(binding?.videoView?.videoSurfaceView as TextureView)
                        Toast.makeText(this@PlayerVideoActivity, "HDR Disabled", Toast.LENGTH_SHORT)
                            .show()
                        binding?.hdrquality?.let { imageView ->
                            imageView.setImageResource(R.drawable.ic_hdr_unselected)
                        }
                    } else {
                        showHdrOptionDialog()
                    }
                } else {
                    if (!isHDREnabled) {
                        getPlayCountHDR = getPlayCountHDR.plus(1) ?: 1
                        sharedPreferences?.savePlayCountHDR(getPlayCountHDR ?: 1)
                        applyHDREffect()
                        // applyHDRFilter(binding?.videoView?.videoSurfaceView as TextureView)
                        //  Toast.makeText(this@PlayerVideoActivity, "HDR Enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@PlayerVideoActivity, "HDR Disabled", Toast.LENGTH_SHORT)
                            .show()
                        isHDREnabled = false
                        removeHDRFilter(binding?.videoView?.videoSurfaceView as TextureView)
                        binding?.hdrquality?.let { imageView ->
                            imageView.setImageResource(R.drawable.ic_hdr_unselected)
                        }
                    }
                }
            }

            pauseVideoCallback(this)/*---------------------------------------Observers ------------------------ */

            binding?.root?.findViewById<ImageView>(R.id.viewlock)?.setOnClickListener {
                AppUtils.firebaseUserAction("lockBtnClicked_videoPlayer", "PlayerVideoActivity")
                isLock = !isLock
                if (!isLock) {
                    // change lock icon here
                    binding?.root?.findViewById<ImageView>(R.id.viewlock)
                        ?.setImageDrawable(resources.getDrawable(R.drawable.ic_lock_player_bg))
                } else {
                    // change lock icon here
                    binding?.root?.findViewById<ImageView>(R.id.viewlock)
                        ?.setImageDrawable(resources.getDrawable(R.drawable.ic_unlock))
                }
                lockScreen(isLock)
            }

            binding?.viewUnlock?.setOnClickListener {
                isLock = !isLock
                lockScreen(isLock)
            }

            binding?.viewUnlock2?.setOnClickListener {
                isLock = !isLock
                lockScreen(isLock)
            }

            videoListLocal.observe(this) { list ->
                try {
                    if (!list.isNullOrEmpty()) {
                        updateJob?.cancel()
                        // Start a new job with a short delay to debounce
                        updateJob = lifecycleScope.launch {
                            delay(300) // Debounce for 300ms to allow for stabilization during drag or item changes

                            // Preserve the current playing URI and playback position
                            val currentUri = player?.currentMediaItem?.localConfiguration?.uri
                            val currentPlaybackPosition = player?.currentPosition ?: 0L
                            val currentPositionIndex = player?.currentWindowIndex ?: 0

                            // Update the list of videos
                            listvideos = list
                            if (listvideos.isNotEmpty()) {
                                try {
                                    player?.currentMediaItemIndex?.let { index ->
                                        viewModel.currentVideo = listvideos[index]
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            videolistglobal = list
                            latestUriList = listvideos.mapNotNull { video ->
                                video.contentUri?.let {
                                    Uri.parse(it)
                                }
                            }.toCollection(ArrayList())
                            // Find the new index of the current playing video in the updated list
                            val newCurrentIndex = latestUriList?.indexOf(currentUri) ?: 0
                            latestUriList?.let {

                                viewModel.updatePlayerMediaItems(
                                    it,
                                    newCurrentIndex,
                                    currentPlaybackPosition
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (listvideos.isNotEmpty()) {
                player?.currentMediaItemIndex?.let { index ->
                    viewModel.currentVideo = listvideos[index]
                }
            }

            binding?.root?.findViewById<AppCompatImageView>(R.id.prevplay)
                ?.setOnClickListener { button ->
                    AppUtils.firebaseUserAction(
                        "previousSongBtnClicked_videoPlayer", "PlayerVideoActivity"
                    )
                    button.isClickable = false
                    lifecycleScope.launch {
                        try {
                            updatePlayerList()
                            val currentPosition = player?.currentPosition ?: 0L
                            val totalDuration = player?.duration ?: 0L
                            if (totalDuration > 0) {
                                val playedPercentage =
                                    (currentPosition.toDouble() / totalDuration.toDouble()) * 100
                                if (playedPercentage >= 90) {

                                    // Handle the case where 90% or more of the video has been played
                                } else {

                                    // Handle the case where less than 90% of the video has been played
                                }
                            }
                            if (isPlaybackCount) {
                                if (isFromPlaylist) {
                                    val id = listvideos.getOrNull(position)?.id ?: 0
                                    val lastPlayed = player?.currentPosition
                                    if (lastPlayed != null) {

                                    }

                                    val vid = listvideos.getOrNull(position)
                                    vid?.let {
                                        it.isRecent = true
                                        viewModel.updateUserData(it)
                                    }

                                } else {
                                    itemsaving = position?.let {
                                        listvideos.getOrNull(
                                            it
                                        )
                                    }
                                    itemsaving?.lastPlayed = player?.currentPosition!!
                                    itemsaving?.let {
                                        it.isRecent = true
                                        viewModel.updateUserData(it)
                                    }
                                }
                            } else {
                                val vid = listvideos.getOrNull(position)
                                vid?.let {
                                    it.isRecent = true
                                    viewModel.updateUserData(it)
                                }
                            }
                            if (player?.hasPreviousMediaItem() == true) {
                                player?.seekToPreviousMediaItem()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            button.isClickable = true
                        }
                    }
                }

            binding?.root?.findViewById<AppCompatImageView>(R.id.nextplay)
                ?.setOnClickListener { button ->
                    AppUtils.firebaseUserAction(
                        "nextVideoBtnClicked_videoPlayer",
                        "PlayerVideoActivity"
                    )
                    button.isClickable = false
                    lifecycleScope.launch {
                        try {
                            updatePlayerList()
                            val currentPosition = player?.currentPosition ?: 0L
                            val totalDuration = player?.duration ?: 0L
                            if (totalDuration > 0) {
                                val playedPercentage =
                                    (currentPosition.toDouble() / totalDuration.toDouble()) * 100
                                if (playedPercentage >= 90) {

                                    itemsaving = listvideos.getOrNull(position)
                                    itemsaving?.playedOver90Percent = true
                                    itemsaving?.playedPercentage = playedPercentage.toInt()
                                    itemsaving?.let {
                                        it.isRecent = true
                                        viewModel.updateUserData(it)
                                    }
                                    // Handle the case where 90% or more of the video has been played
                                } else {

                                    // Handle the case where less than 90% of the video has been played
                                }
                            }
                            if (isPlaybackCount) {
                                if (isFromPlaylist) {
                                    val id = listvideos.getOrNull(position)?.id ?: 0
                                    val lastPlayed = player?.currentPosition
                                    if (lastPlayed != null) {

                                    }
                                    val vid = listvideos.getOrNull(position)
                                    vid?.let {
                                        it.isRecent = true
                                        viewModel.updateUserData(it)
                                    }
                                } else {
                                    itemsaving = listvideos.getOrNull(position)
                                    itemsaving?.lastPlayed = player?.currentPosition!!
                                    itemsaving?.let {
                                        it.isRecent = true
                                        viewModel.updateUserData(it)
                                    }
                                }
                            } else {

                            }

                            if (player?.hasNextMediaItem() == true) {
                                player?.seekToNextMediaItem()
                                val vid = listvideos.getOrNull(position)
                                vid?.let {
                                    it.isRecent = true
                                    viewModel.updateUserData(it)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            button.isClickable = true
                        }
                    }
                }

            binding?.root?.findViewById<ImageView>(R.id.stretchButton)?.setOnClickListener {
                resetPlayerView()
                AppUtils.firebaseUserAction("stretchBtnClicked_videoPlayer", "PlayerVideoActivity")
                currentResizeModeIndex = (currentResizeModeIndex + 1) % resizeModes.size
                binding?.videoView?.resizeMode = resizeModes[currentResizeModeIndex]
                when (currentResizeModeIndex) {
                    0 -> {
                        binding?.root?.findViewById<ImageView>(R.id.stretchButton)
                            ?.setImageDrawable(resources.getDrawable(R.drawable.ic_mode_fill))
                        binding?.sizeText?.let { it1 -> displayTextForTwoSeconds(it1, "FILL_MODE") }
                    }

                    1 -> {
                        binding?.root?.findViewById<ImageView>(R.id.stretchButton)
                            ?.setImageDrawable(resources.getDrawable(R.drawable.ic_mode_fit))
                        binding?.sizeText?.let { it1 -> displayTextForTwoSeconds(it1, "FIT_MODE") }
                    }

                    2 -> {
                        binding?.root?.findViewById<ImageView>(R.id.stretchButton)
                            ?.setImageDrawable(resources.getDrawable(R.drawable.ic_mode_fill))
                        binding?.sizeText?.let { it1 -> displayTextForTwoSeconds(it1, "ZOOM_MODE") }
                    }

                    3 -> {
                        binding?.root?.findViewById<ImageView>(R.id.stretchButton)
                            ?.setImageDrawable(resources.getDrawable(R.drawable.ic_mode_zoom))
                        binding?.sizeText?.let { it1 -> displayTextForTwoSeconds(it1, "16:9") }
                    }

                    4 -> {
                        binding?.root?.findViewById<ImageView>(R.id.stretchButton)
                            ?.setImageDrawable(resources.getDrawable(R.drawable.ic_mode_fixed_width_new))
                        binding?.sizeText?.let { it1 -> displayTextForTwoSeconds(it1, "9:16") }
                    }
                }
            }

            binding?.backButtom?.setOnClickListener {
                try {
                    player?.stop()
                    backfunctionality()
                } catch (e: Exception) {
                   e.printStackTrace()
                }
            }

            binding?.root?.findViewById<ImageView>(R.id.pip_button)?.setOnClickListener {
                AppUtils.firebaseUserAction("pipBtnClicked_videoPlayer", "PlayerVideoActivity")
                try {
                    binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
                    binding?.root?.findViewById<ConstraintLayout>(R.id.main_controller)?.visibility =
                        View.GONE
                    binding?.playbacklayout?.playbackmain?.visibility = View.GONE
                    initPip(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@PlayerVideoActivity,
                        "This device doesn't support Pip Mode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding?.root?.findViewById<ImageView>(R.id.btn_play)?.setOnClickListener {

                binding?.videoEqualizerContainer?.visibility = View.GONE

                val currentTime: Long = SystemClock.elapsedRealtime()
                // If the time difference between the current click and the last click
                // is less than our cooldown, ignore this click

                if (currentTime - mLastClickTime < CLICK_COOLDOWN) {
                    return@setOnClickListener  // Ignoreubl the "doe tap"
                }

                // Update the last click time
                mLastClickTime = currentTime
                AppUtils.firebaseUserAction("playlistBtnClicked_videoPlayer", "PlayerVideoActivity")
                binding?.bottomView?.visibility = View.GONE
                playListBottomSheetFragment = PlaylistBottomSheetFragment()

                playListBottomSheetFragment?.let {
                    val bundle = Bundle()
                    val list = arrayListOf<Video>()
                    list.clear()
                    list.addAll(listvideos)
                    PlaylistBottomSheetFragment.listvideos = list
                    bundle.putString("previousFragment", prevousFragment)
                    bundle.putInt("position", position)
                    bundle.putBoolean("isFullScreen", viewModel.isFullScreen)
                    bundle.putInt("video_order_type", VIDEO_PLAYER_ORDER_TYPE.value ?: 0)
                    it.arguments = bundle
                    it.show(supportFragmentManager, "")
                    it.setOrderTypeChangeListner(object :
                        OnVideoPlayerPlaylistOrderTypeChangeListner {
                        override fun onVideoOrderChanged(orderType: Int) {
                            VIDEO_PLAYER_ORDER_TYPE.value = orderType
                            AppPreference.saveVideoPlaylistOrderType(
                                this@PlayerVideoActivity, orderType
                            )
                            when (orderType) {
                                0 -> {
                                    viewModel.icRepeat = false
                                    player?.repeatMode = Player.REPEAT_MODE_OFF
                                }

                                1 -> {
                                    viewModel.icRepeat = false
                                    player?.repeatMode = Player.REPEAT_MODE_ALL
                                }

                                2 -> {
                                    viewModel.icRepeat = false
                                    player?.shuffleModeEnabled = true

                                }

                                3 -> {
                                    viewModel.icRepeat = true
                                    player?.repeatMode = Player.REPEAT_MODE_ONE
                                }
                            }
                        }
                    })
                    it.setPlayItemClickListener(object : PlaylistAdapter.PlayListItemClickListener {
                        override fun onItemClick(position: Int, list: List<Video>) {
                            updatePlayerList()
                            hideNativeAd()
                            addtorecentjob = CoroutineScope(Dispatchers.IO).launch {
                                viewModel.addToRecents(position.toString(), ArrayList(list)) {}
                            }
                            it.dismiss()
                            this@PlayerVideoActivity.position = position
                            viewModel.playbackPosition = 0
                            player?.seekTo(position, 0)
                        }

                        override fun onPlayingItemRemoved(video: Video) {
                            hideNativeAd()
//                            Toast.makeText(this@PlayerVideoActivity, "${video.title}\nis currently playing, can't removed", Toast.LENGTH_SHORT).show()
                        }

                        override fun onbackpresscalled() {
                            it.dismiss()
                        }
                    })
                }
            }

            binding?.icSubtitle?.setOnClickListener {
                AppUtils.firebaseUserAction("subtitleBtnClicked_videoPlayer", "PlayerVideoActivity")
                binding?.videoEqualizerContainer?.visibility = View.GONE
                player?.currentPosition?.let {
                    viewModel.newPos = it
                }
                binding?.bottomView?.visibility = View.GONE
                playListBottomSheetFragment?.dismiss()
                currentVideoTitle = binding?.title?.text.toString()
                binding?.videoView?.subtitleView?.let { subtitleView ->
                    viewModel.currentVideoHasSubtitle.value?.let { it1 ->
                        if (binding?.videoView?.subtitleView?.visibility == View.VISIBLE && it1 == true) {
                            showSubtitleDialog(
                                listvideos,
                                this,
                                subtitleView,
                                viewModel,
                                it1,
                                true
                            )
                        } else {
                            showSubtitleDialog(
                                listvideos,
                                this,
                                subtitleView,
                                viewModel,
                                subtitleToggle,
                                subtitleToggle
                            )

                        }
                    }
                }
            }

            binding?.root?.findViewById<AppCompatImageView>(R.id.back_seconds)?.setOnClickListener {
                val currentPosition = player?.currentPosition
                if (currentPosition != null && currentPosition > 10000) {
                    player?.seekTo(currentPosition - 10000)
                } // 10,000 milliseconds (10 seconds) forward
            }

            binding?.root?.findViewById<AppCompatImageView>(R.id.forward_seconds)
                ?.setOnClickListener {
                    val currentPosition = player?.currentPosition
                    if (currentPosition != null) {
                        player?.seekTo(currentPosition + 10000)
                    } // 10,000 milliseconds (10 seconds) forward
                }

            onScreenTouch()

            binding?.videoView?.setOnClickListener {
                if (isLock) {
                    binding?.viewUnlock?.let { toggleViewVisibility(it, 2000) }
                }
            }

            playSpeedClickListeners()

            exoBar = binding?.root?.findViewById(R.id.exo_progress)

            exoBar?.setOnMarkerClickListener { pos ->
                player?.seekTo(pos)
            }

//            val handler = Handler(Looper.getMainLooper())
            volumeObserver = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    // Get the current volume
                    val currentVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)
                    // Update your UI
                    // updateVolumeUI(currentVolume)
                    binding?.volumeBar?.let {
                        it.progress = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)!!
                    }
                    if (currentVolume == 0) {
                        PlayerDelegateImpl.isSoundMuted = true
                    } else {
                        PlayerDelegateImpl.isSoundMuted = false
                    }
                }
            }

            // Register the observer to listen to changes in the audio volume
            contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                volumeObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Defer Chromecast setup: get CastContext on background thread to avoid ANR from Binder in getSharedInstance
        setupChromecastConnectionAsync()
        viewModel.currentVideoHasSubtitle.observe(this) {
            if (it) {
                binding?.videoView?.subtitleView?.let { subtitleView ->
                    viewModel.currentVideoSubtitlePath.observe(this) { path ->
                        if (path.isNotEmpty()) {
                            setSubTitle(listvideos, path, subtitleView, viewModel.newPos)
                            dismissAllDialogs(this)
                        }
                    }

                }

            }
        }
        viewModel.showSubtitleView.observe(this) {
            if (it == true) {
                binding?.videoView?.subtitleView?.visibility = View.VISIBLE
                subtitleToggle = true
            } else {
                subtitleToggle = false
                binding?.videoView?.subtitleView?.visibility = View.GONE
            }
        }
        lifecycleScope.launch {
            delay(3000)
            viewModel.getSubtitleFromDB()
        }
        ifAdDisplayed?.removeObservers(this) // ✅ Remove any existing observer
        // Observe LiveData only once during the activity lifecycle
        ifAdDisplayed?.observe(this, Observer { isAdDisplayed ->
            if (isAdDisplayed == false) {
                player?.play()
            } else {
                player?.pause()
            }
        })
    }

    private fun resetPlayerView() {
        binding?.videoView?.findViewById<ConstraintLayout>(R.id.zoom_player)?.apply {
            scaleX = 1.0f
            scaleY = 1.0f
            translationX = 0f
            translationY = 0f
        }
        isPinchedZoomActived = false // Reset the zoom state
    }

    private fun updatePlayerList() {
        CoroutineScope(Dispatchers.IO).launch {
            // Create a copy of the list to avoid ConcurrentModificationException
            val videosCopy = listvideos.toList()
            val uriList = videosCopy.mapNotNull { video ->
                video.contentUri?.let { Uri.parse(it) }
            }
            // Only post to LiveData if the new uriList is different from the current one
            val currentUris = viewModel.videosUrlList.value
            if (uriList != currentUris) {
                viewModel.videosUrlList.postValue(uriList)
            }
        }
    }

    private fun openSpeed() {
        AppUtils.firebaseUserAction("playbackSpeedBtnClicked_videoplayer", "PlayerVideoActivity")
        binding?.bottomView?.visibility = View.GONE
        viewModel.isSheetbakpress = true
        viewModel.openEqualizerEvent.value = FragmentEvent(BottomSheetPlaybackSpeed())
    }

    private fun bgPlayClick() {
        if (!isBgNotAllowed) {
//            try {
//                val currentmediaitem = player?.currentMediaItem
//                val uri = currentmediaitem?.localConfiguration?.uri
//                uri?.let {
//                    viewModel.icBgAudioClicked = true
//                    viewModel.createAudioList(uriList, listvideos, this, position, playerListener)
//                    AppUtils.firebaseUserAction(
//                        "bgAudioBtnClicked_PlayerVideoFragment",
//                        "PlayerVideoFragment"
//                    )
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
        } else {
            Toast.makeText(this, "Bg play not allowed here", Toast.LENGTH_SHORT).show()
        }
    }


    private fun rotateScreen() {
        AppUtils.firebaseUserAction("fullScreenBtnClicked_videoPlayer", "PlayerVideoActivity")
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            playerMode = when (playerMode) {
                "auto" -> {

                    "landscape"
                }

                "landscape" -> {
                    "auto"
                }

                "portrait" -> {

                    "auto"
                }

                else -> "null"
            }
        } else {
            playerMode = when (playerMode) {
                "auto" -> {

                    "landscape"
                }

                "landscape" -> {

                    "portrait"
                }

                "portrait" -> {

                    "landscape"
                }

                else -> "null"
            }
        }
        Toast.makeText(this, playerMode, Toast.LENGTH_SHORT).show()
        if (!viewModel.isFullScreen) {
            binding?.sizeText?.let { it1 ->
                displayTextForTwoSeconds(
                    it1, "LANDSCAPE_MODE"
                )
            }
            viewModel.saveOrientationPrefs(1)
            binding?.root?.findViewById<AppCompatImageView>(R.id.back_seconds)?.visibility =
                View.VISIBLE
            binding?.root?.findViewById<AppCompatImageView>(R.id.forward_seconds)?.visibility =
                View.VISIBLE
            requestedOrientation = if (playerMode != "auto") {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else {
                    viewModel.isFullScreen = !viewModel.isFullScreen
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            } else {
                viewModel.saveOrientationPrefs(0)
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
        } else {
            binding?.sizeText?.let { it1 -> displayTextForTwoSeconds(it1, "POTRAIT_MODE") }
            viewModel.saveOrientationPrefs(2)
            binding?.root?.findViewById<AppCompatImageView>(R.id.back_seconds)?.visibility =
                View.GONE
            binding?.root?.findViewById<AppCompatImageView>(R.id.forward_seconds)?.visibility =
                View.GONE
            requestedOrientation = if (playerMode != "landscape") {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else {
                    viewModel.isFullScreen = !viewModel.isFullScreen
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            } else {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    fun captureScreen() {
        AppUtils.firebaseUserAction("screenshotBtnClicked_videoPlayer", "PlayerVideoActivity")
        screenShort()
    }

    private fun activateNightMode() {
        try {
            if (!dark) {
                binding?.nightMode?.visibility = View.VISIBLE
                binding?.icNightMode?.setImageResource(R.drawable.ic_night_mode_selector)
                binding?.tvNightMode?.setTextColor(
                    ContextCompat.getColor(
                        this@PlayerVideoActivity,
                        R.color.gSelector
                    )
                )
                dark = true
            } else {
                binding?.nightMode?.visibility = View.GONE
                binding?.icNightMode?.setImageResource(R.drawable.ic_nightmode)
                binding?.tvNightMode?.setTextColor(
                    ContextCompat.getColor(
                        this@PlayerVideoActivity,
                        R.color.brand_text_primary
                    )
                )
                dark = false
            }
            AppUtils.firebaseUserAction(
                "nightModeBtnClicked_PlayerVideoFragment",
                "PlayerVideoFragment"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideNativeAd()
        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                binding?.playbacklayout?.playbackmain?.let { setBottomMargin(it, 130f, this) }
                if (playerMode == "auto") {
                    requestedOrientation =
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        else ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
                binding?.root?.findViewById<AppCompatImageView>(R.id.back_seconds)?.visibility =
                    View.GONE
                binding?.root?.findViewById<AppCompatImageView>(R.id.forward_seconds)?.visibility =
                    View.GONE
                isFlipped = true
                binding?.icMirror?.setImageResource(R.drawable.ic_mirror)
                binding?.tvMirror?.setTextColor(
                    ContextCompat.getColor(
                        this@PlayerVideoActivity, R.color.brand_text_primary
                    )
                )
                flipVideo()
                if (!viewModel.savedorientation) {

                }
                viewModel.isFullScreen = false
            }

            Configuration.ORIENTATION_LANDSCAPE -> {
                binding?.playbacklayout?.playbackmain?.let { setBottomMargin(it, 65f, this) }
                isFlipped = true
                binding?.icMirror?.setImageResource(R.drawable.ic_mirror)
                binding?.tvMirror?.setTextColor(
                    ContextCompat.getColor(
                        this@PlayerVideoActivity, R.color.brand_text_primary
                    )
                )
                flipVideo()
                binding?.root?.findViewById<AppCompatImageView>(R.id.back_seconds)?.visibility =
                    View.VISIBLE
                binding?.root?.findViewById<AppCompatImageView>(R.id.forward_seconds)?.visibility =
                    View.VISIBLE
                if (!viewModel.savedorientation) {
//                    if (playerMode == "auto") {
//                        adapterPlayerTopFeatures?.updateTiltIconDrawable(
//                            R.drawable.ic_auto_rotate,
//                            "Auto Rotate"
//                        )
//                    } else {
//                        adapterPlayerTopFeatures?.updateTiltIconDrawable(
//                            R.drawable.ic_landscape,
//                            "Landscape"
//                        )
//                    }
                }
                viewModel.isFullScreen = true
            }
        }
        val params =
            binding?.videoPlayerFeaturesIconsLayout?.layoutParams as ConstraintLayout.LayoutParams
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                params.setMargins(30, 0, 55, 0)
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                params.setMargins(0, 0, 0, 0)
            }
            binding?.videoPlayerFeaturesIconsLayout?.layoutParams = params
        }
        binding?.bottomView?.visibility = View.GONE
        binding?.timerView?.visibility = View.GONE
    }

    private fun setupController() {
        hideSystemUI()
        binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
        binding?.videoView?.hideController()
        binding?.icRotation?.visibility = View.GONE
        binding?.icRepeat?.visibility = View.GONE
        binding?.icTimer?.visibility = View.GONE
        binding?.icScreenshot?.visibility = View.GONE
        binding?.videoView?.controllerShowTimeoutMs = 4000
        binding?.videoView?.setControllerVisibilityListener(ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                // controller is visible
                try {
                    if (!isLock && !isPipMode) {
                        if (!bookmarkDialogShown) {
                            showSystemUI()
                            binding?.icRotation?.visibility = View.VISIBLE
                            binding?.icRepeat?.visibility = View.VISIBLE
                            binding?.icTimer?.visibility = View.VISIBLE
                            binding?.icScreenshot?.visibility = View.VISIBLE
                            binding?.videoPlayerFeaturesIconsLayout?.visibility = View.VISIBLE
                            binding?.root?.findViewById<ConstraintLayout>(R.id.main_controller)?.visibility =
                                View.VISIBLE
                        }

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // controller is not visible
                if (!ispause) {
                    hideSystemUI()
                    binding?.icRotation?.visibility = View.GONE
                    binding?.icRepeat?.visibility = View.GONE
                    binding?.icTimer?.visibility = View.GONE
                    binding?.icScreenshot?.visibility = View.GONE
                    binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
                }
            }
        })
        try {
            updateViewByInsets(findViewById<ConstraintLayout>(R.id.main_controller))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateViewByInsets(viewToUpdate: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewToUpdate) { view, windowInsetsCompat ->
            val insets: Insets = if (Build.VERSION.SDK_INT >= 30) {
                windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
            } else {
                Insets.of(
                    windowInsetsCompat.systemGestureInsets.left,
                    0,
                    windowInsetsCompat.systemGestureInsets.right,
                    windowInsetsCompat.systemGestureInsets.bottom
                )
            }
            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun getIntentExtras() {
        try {
            val intent = intent
            if (intent != null) {
                when (intent.action) {
                    Intent.ACTION_VIEW -> handleActionView(intent)
                    Intent.ACTION_SEND -> handleActionSend(intent)
                }

                // Add to recents if the video list is not empty
                if (videolistglobal.isNotEmpty()) {
                    lifecycleScope.launch {
                        viewModel.addToRecents(position.toString(), ArrayList(videolistglobal)) {}
                    }
                }

                // Additional intent extras
                isVault = intent.getBooleanExtra("isValut", false)
                isFromTrimmer = intent.getBooleanExtra("isFromTrimmer", false)
                prevousFragment = intent.getStringExtra("fragmentName") ?: ""
                viewModel.playbackPosition = intent.getLongExtra("playbackposition", 0L)
                isFromPlaylist = intent.getBooleanExtra("isFromPlaylist", false)
                isBgNotAllowed = intent.getBooleanExtra("isBgNotAllowed", false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handles the `Intent.ACTION_VIEW` action.
     */

    private fun handleActionView(intent: Intent) {
        val uri = intent.data
        if (uri != null && videolistglobal.isEmpty()) {
            initializeVideoList(uri)
            fromOpenWithOption = true
            videooutside = true
            isFromLaunchers = true
        }
    }

    /**
     * Handles the `Intent.ACTION_SEND` action.
     */

    private fun handleActionSend(intent: Intent) {
        if (intent.type?.startsWith("video/") == true) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri != null && videolistglobal.isEmpty()) {
                initializeVideoList(uri)
                fromOpenWithOption = true
                videooutside = true
                isFromLaunchers = true
            }
        }
    }

    /**
     * Initializes the video list with the given URI.
     */

    private fun initializeVideoList(uri: Uri) {
        videolistglobal = emptyList()
        videolistglobal = listOf(Video(contentUri = uri.toString()))
        viewModel.singleList.clear()
        player?.clearMediaItems()
        viewModel.singleList.add(uri)
        isShowingAd = true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onScreenTouch() {
        lifecycleScope.launch(Dispatchers.Main) {
            mScreenWidth = resources.displayMetrics.widthPixels
            mScreenHeight = resources.displayMetrics.heightPixels
            binding?.customTouchView?.setOnTouchListener(@SuppressLint("ClickableViewAccessibility") object :
                OnTouchListener {
                var startY = 0f

                @SuppressLint("UseCompatLoadingForDrawables")
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    val x = event.x
                    val y = event.y

                    when (event.action and MotionEvent.ACTION_MASK) {
                        MotionEvent.ACTION_POINTER_DOWN -> {
                            // This is the case when second finger touches the screen
                            if (!isLongPressedActived && !isMovementActived) {
                                mask = true
                                handler.removeCallbacksAndMessages(null)
                                initialDistance = calculateFingerDistance(event)
                                initialScaleFactor = scaleFactor
                            }
                        }

                        MotionEvent.ACTION_POINTER_UP -> {
                            // This is the case when second finger leaves the screen
                            initialDistance = 0f
                            mask = false
                        }

                        MotionEvent.ACTION_MOVE -> {
                            if (event.pointerCount == 2 && initialDistance > 0) {
                                val newDistance = calculateFingerDistance(event)
                                scaleFactor = max(
                                    MIN_SCALE,
                                    min(
                                        initialScaleFactor * (newDistance / initialDistance),
                                        MAX_SCALE
                                    )
                                )
                                // You can use the 'scaleFactor' to zoom in/out
                                // For example, you can apply this scale factor to your View or VideoPlayer
                                // Make sure to handle any constraints or limits on zooming
                                // Additionally, you can update UI elements to show current zoom level if needed
                                // Apply zoom to PlayerView
                                if (!scaleFactor.isNaN() && scaleFactor > 0.0f && scaleFactor < 10.0f) {
                                    binding?.videoView?.findViewById<ConstraintLayout>(R.id.zoom_player)?.scaleX =
                                        scaleFactor
                                    binding?.videoView?.findViewById<ConstraintLayout>(R.id.zoom_player)?.scaleY =
                                        scaleFactor
                                    isPinchedZoomActived = true
                                } else {
                                    // Handle the case where scaleFactor is not valid (e.g., show an error message, use a default value, or take appropriate action)
                                }
                            }
                        }
                    }

                    // Calculate the dimensions for the excluded top and bottom regions

                    if (!mask) {
                        when (event.action) {

                            MotionEvent.ACTION_DOWN -> {
//                                isLongPress         = true
                                isMovementActived = false
                                isPinchedZoomActived = false
//                                longPressRunnable = Runnable {
//                                    if (isLongPress && !isMovementActived && !isPinchedZoomActived) {
//                                        if (viewModel.speedfeatureon) {
//                                            player?.setPlaybackSpeed(2.0f)
//                                            isLongPressedActived = true
//                                            if (!ispause) {
//                                                binding?.speedAmin?.visibility = View.VISIBLE
//                                                binding?.speedAmin?.playAnimation()
//                                            }
//                                        }
//                                    } else {
//                                        Log.d(TAG, "Runnable ignored (flags changed).")
//                                    }
//                                }
//                                handler.postDelayed(longPressRunnable!!, longClickDuration)

                                /* ----- existing double‑tap bookkeeping ----- */
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastTapTime <= doubleTapTimeout) tapCount++ else tapCount =
                                    1
                                lastTapTime = currentTime

                                mDownX = event.x
                                mDownY = event.y
                                mChangeVolume = false
                                mChangePosition = false
                                mChangeBrightness = false
                            }

                            MotionEvent.ACTION_MOVE -> {
                                // Calculate the change in X and Y coordinates
                                if (!isLock) {
                                    val deltaX: Float = x - mDownX
                                    var deltaY: Float = y - mDownY
                                    val absDeltaX = abs(deltaX)
                                    val absDeltaY = abs(deltaY)
                                    // Check the current screen mode
                                    if (!mChangePosition && !mChangeVolume && !mChangeBrightness && !isLongPressedActived && !isPinchedZoomActived) {
                                        // Check if the user's touch movement exceeds a threshold
                                        if (absDeltaX > tHRESHOLD || absDeltaY > tHRESHOLD) {
                                            // Cancel any ongoing progress timer
                                            if (absDeltaX >= tHRESHOLD) {
                                                // Handle seeking within the video
                                                // If the current state is not an error state, allow seeking
                                                mChangePosition = true
                                                isMovementActived = true
                                                isLongPressedActived = false
                                                isPinchedZoomActived = false
                                                mGestureDownPosition =
                                                    getCurrentPositionWhenPlaying()
                                            } else {
                                                // Handle changing volume and brightness
                                                if (!viewModel.isFullScreen) {
                                                    if (mDownX < mScreenWidth * 0.5f) {
                                                        // Left side of the screen: Change brightness
                                                        isMovementActived = true
                                                        isLongPressedActived = false
                                                        isPinchedZoomActived = false
                                                        mChangeBrightness = true
                                                        mChangeVolume = false
                                                        // Get the current brightness settings
                                                        // Handle both system brightness and activity brightness
                                                        val lp: WindowManager.LayoutParams? =
                                                            getWindow(this@PlayerVideoActivity)?.attributes
                                                        lp?.let {
                                                            if (it.screenBrightness < 0) {
                                                                try {
                                                                    mGestureDownBrightness =
                                                                        Settings.System.getInt(
                                                                            this@PlayerVideoActivity.contentResolver,
                                                                            Settings.System.SCREEN_BRIGHTNESS
                                                                        ).toFloat()
                                                                } catch (e: SettingNotFoundException) {
                                                                    e.printStackTrace()
                                                                }
                                                            } else {
                                                                mGestureDownBrightness =
                                                                    it.screenBrightness.times(
                                                                        255
                                                                    )
                                                            }
                                                        }
                                                    } else {
                                                        // Right side of the screen: Change volume
                                                        isMovementActived = true
                                                        isLongPressedActived = false
                                                        isPinchedZoomActived = false
                                                        mChangeVolume = true
                                                        mChangeBrightness = false
                                                        mAudioManager?.let {
                                                            mGestureDownVolume =
                                                                it.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                        }
                                                        PlayerDelegateImpl.isSoundMuted = false
                                                    }
                                                } else if (mDownX < mScreenWidth * 0.9f) {
                                                    // Left side of the screen (in landscape mode): Change brightness
                                                    // Get the current brightness settings
                                                    //左侧改变亮度
                                                    mChangeBrightness = true
                                                    mChangeVolume = false
                                                    isMovementActived = true
                                                    isLongPressedActived = false
                                                    isPinchedZoomActived = false
                                                    val lp: WindowManager.LayoutParams? =
                                                        getWindow(this@PlayerVideoActivity)?.attributes
                                                    lp?.let {
                                                        if (it.screenBrightness < 0) {
                                                            try {
                                                                mGestureDownBrightness =
                                                                    Settings.System.getInt(
                                                                        this@PlayerVideoActivity.contentResolver,
                                                                        Settings.System.SCREEN_BRIGHTNESS
                                                                    ).toFloat()
                                                            } catch (e: SettingNotFoundException) {
                                                                e.printStackTrace()
                                                            }
                                                        } else {
                                                            mGestureDownBrightness =
                                                                it.screenBrightness.times(255)
                                                        }
                                                    }
                                                } else {
                                                    // Right side of the screen (in landscape mode): Change volume
                                                    // Get the current volume level
                                                    mChangeVolume = true
                                                    mChangeBrightness = false
                                                    mAudioManager?.let {
                                                        mGestureDownVolume =
                                                            it.getStreamVolume(AudioManager.STREAM_MUSIC)

                                                        isMovementActived = true
                                                        isLongPressedActived = false
                                                        isPinchedZoomActived = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Handle different touch interactions based on the detected changes
                                    if (mChangePosition) {
                                        // Handle seeking within the video
                                        if (player?.duration != null) {
                                            val totalTimeDuration: Long = player?.duration!!
                                            mSeekTimePosition =
                                                (mGestureDownPosition?.plus(deltaX * totalTimeDuration / mScreenWidth))?.toLong()
                                                    ?: 0L
                                            if (mSeekTimePosition > totalTimeDuration) mSeekTimePosition =
                                                totalTimeDuration
                                            val seekTime: String? = stringForTime(mSeekTimePosition)
                                            val totalTime: String? =
                                                stringForTime(totalTimeDuration)
                                            if (seekTime != null) {
                                                if (totalTime != null) {
                                                    showProgressDialog(
                                                        deltaX,
                                                        seekTime,
                                                        mSeekTimePosition,
                                                        totalTime,
                                                        totalTimeDuration
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (mChangeVolume) {
                                        // Handle changing volume
                                        // Calculate the volume change based on touch movement
                                        // Set the new volume level and show a volume dialog
                                        try {
                                            deltaY = -deltaY
                                            val max =
                                                mAudioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                            max?.let {
                                                val deltaV =
                                                    (it * deltaY * 3 / mScreenHeight).toInt()
                                                mAudioManager?.setStreamVolume(
                                                    AudioManager.STREAM_MUSIC,
                                                    mGestureDownVolume + deltaV,
                                                    0
                                                )
                                                val volumePercent =
                                                    (mGestureDownVolume * 100 / it + deltaY * 3 * 100 / mScreenHeight).toInt()
                                                showVolumeDialog(-deltaY, volumePercent)
                                            }
                                        } catch (e: ArithmeticException) {
                                            e.printStackTrace()
                                        }
                                    }
                                    if (mChangeBrightness) {
                                        try {
                                            // Handle changing brightness
                                            // Calculate the brightness change based on touch movement
                                            // Adjust the screen brightness and show a brightness dialog
                                            deltaY = -deltaY
                                            val deltaV = (255 * deltaY * 3 / mScreenHeight).toInt()
                                            val params: WindowManager.LayoutParams? =
                                                getWindow(this@PlayerVideoActivity)?.attributes
                                            params?.let {
                                                if ((mGestureDownBrightness + deltaV) / 255 >= 1) { //这和声音有区别，必须自己过滤一下负值
                                                    it.screenBrightness = 1f
                                                } else if ((mGestureDownBrightness + deltaV) / 255 <= 0) {
                                                    it.screenBrightness = 0.01f
                                                } else {
                                                    it.screenBrightness =
                                                        (mGestureDownBrightness + deltaV) / 255
                                                }
                                                getWindow(this@PlayerVideoActivity)?.attributes = it
                                                val brightnessPercent =
                                                    (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight).toInt()
                                                showBrightnessDialog(brightnessPercent)
                                            }
                                        } catch (e: ArithmeticException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            MotionEvent.ACTION_UP -> {
//                                backToNormalSpeed()
                                try {
                                    // Determine if it's a single tap or a double tap
                                    binding?.progressduration?.visibility = View.GONE
                                    if (mChangePosition) {
                                        player?.seekTo(mSeekTimePosition)
                                        player?.contentBufferedPosition
                                    } else if (mChangeBrightness) {
                                        binding?.brightProgress?.visibility = View.GONE
                                        binding?.vlumeandbright?.visibility = View.GONE
                                    } else if (mChangeVolume) {
                                        binding?.volumeProgress?.visibility = View.GONE
                                        binding?.vlumeandbright?.visibility = View.GONE
                                    } else {
                                        lifecycleScope.launch {
                                            delay(500)
                                            if (tapCount > 1) {
                                                // Handle double tap action
                                                // Show the PlayerControlView when touched
                                                performDoubleTapAction(v, event)
                                            } else {
                                                // Handle single tap action
                                                startY = y
                                                startX = x
                                                //hide the PlayerControlView
                                                binding?.timerView?.visibility = View.GONE
                                                binding?.bottomView?.visibility = View.GONE
                                                if (fragmentclose != null) {
                                                    if (fragmentclose?.isVisible == true) {
                                                        if (binding?.videoEqualizerContainer?.isShown == true) {
                                                            onBackPressed()
                                                            binding?.videoEqualizerContainer?.visibility =
                                                                View.GONE
                                                        }
                                                    }
                                                }
                                                mChangeVolume = false
                                                mChangePosition = false
                                                mChangeBrightness = false
                                                // Reset the delay for hiding the PlayerControlView
                                                when (binding?.videoView?.isControllerFullyVisible) {
                                                    true -> {
                                                        if (!ispause) binding?.videoView?.hideController()
                                                    }

                                                    false -> {
                                                        binding?.videoView?.showController()
                                                    }

                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                    binding?.speedAmin?.cancelAnimation()
                                    binding?.speedAmin?.visibility = View.GONE
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            else -> {}
                        }
                    }
                    return true
                }
            })
        }
    }

    private fun toggleViewVisibility(view: View, delayMillis: Long) {
        if (view.visibility == View.GONE) {
            view.visibility = View.VISIBLE
            binding?.viewUnlock2?.visibility = View.VISIBLE
            scheduleHideView(view, delayMillis)
        } else {
            view.visibility = View.GONE
            binding?.viewUnlock2?.visibility = View.GONE
            hideViewJob?.cancel() // Cancel any pending hide task
        }
    }

    private fun scheduleHideView(view: View, delayMillis: Long) {
        jobnew.cancel() // Cancel previous job if it exists
        scope.launch {
            delay(delayMillis)
            view.visibility = View.GONE
            binding?.viewUnlock2?.visibility = View.GONE
        }
    }

    // Helper method to calculate distance between two fingers
    private fun calculateFingerDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun backToNormalSpeed() {
        try {
            if (isLongPress) {
                val initial = "1"
                val speedWithoutX = initial.replace("x", "1")
                val doubleIntital = (speedWithoutX.toFloat() ?: 1.0f)
                player?.setPlaybackSpeed(doubleIntital)
                isLongPress = false
                isLongPressedActived = false
            }
            handler.removeCallbacksAndMessages(null)
            binding?.sizeText?.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        lifecycleScope.coroutineContext.cancelChildren()  // Cancel all running coroutines
        debounceJob?.cancel()
        debounceJobPlay?.cancel()
        updateJob?.cancel()
        addtorecentjob?.cancel()
        scope.cancel()  // If using a separate CoroutineScope, cancel it
        volumeObserver?.let { contentResolver.unregisterContentObserver(it) }  // Unregister content observer
        handler.removeCallbacksAndMessages(null)  // Remove pending UI updates
        videoListLocal.value = null
        isFromLaunchers = false
        if (isPlaybackCount) {
            if (isFromPlaylist) {
                if (position in videolistglobal.indices) {
                    val id = videolistglobal.getOrNull(position)?.id ?: 0
                    val lastPlayed = viewModel.lastplaybackforsave
//                    viewModel.updateVideoEntityPlaylist(lastPlayed, id)
                }

                if (position in videolistglobal.indices) {
                    val vid = videolistglobal.getOrNull(position)
                    vid?.let {
                        it.isRecent = true
                        viewModel.updateUserData(it)
                    }
                }

            } else {
                if (position in videolistglobal.indices) {
                    itemsaving = videolistglobal.getOrNull(position)
                    itemsaving?.lastPlayed = viewModel.lastplaybackforsave
                    itemsaving?.let {
                        it.isRecent = true
                        viewModel.updateUserData(it)
                    }
                }
            }
        } else {
            val vid = videolistglobal.getOrNull(position)
            vid?.let {
                it.isRecent = true
                viewModel.updateUserData(it)
            }
        }
        ChromeCastDelegate.mPreparingConnectionDialog?.dismiss()
        ChromeCastDelegate.mPrepareServerDialog?.dismiss()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                unregisterReceiver(playPauseReceiver)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Clear the binding to avoid memory leaks
        binding = null
        playWhenReady = true
        dontplaybackuntilkill = false
        subTitleUri.value = ""
        if (EqualizerFragmentVideo.mEqualizer != null) {
            EqualizerFragmentVideo.mEqualizer?.release()
            EqualizerFragmentVideo.mEqualizer = null
        }
        if (EqualizerFragmentVideo.bassBoost != null) {
            EqualizerFragmentVideo.bassBoost?.release()
            EqualizerFragmentVideo.bassBoost = null
        }
        if (EqualizerFragmentVideo.presetReverb != null) {
            EqualizerFragmentVideo.presetReverb?.release()
            EqualizerFragmentVideo.presetReverb = null
        }
        viewModel.playbackPosition = 0L
        hideViewJob?.cancel()
        job?.cancel()
        addtorecentjob?.cancel()
        jobnew.cancel()
        mAudioManager = null
        exoBar = null
        super.onDestroy()
    }

    private fun showVolumeDialog(fl: Float, volumePercent: Int) {
        try {
            binding?.vlumeandbright?.visibility = View.VISIBLE
            binding?.volumeProgress?.visibility = View.VISIBLE
            binding?.volumeText?.text = "Volume"

            binding?.icRepeat?.visibility = View.GONE
            binding?.icScreenshot?.visibility = View.GONE
            binding?.icRotation?.visibility = View.GONE
            binding?.icTimer?.visibility = View.GONE
            // Ensure volumePercent is within the valid range [0, 100]
            var value = when {
                volumePercent > 100 -> 100
                volumePercent < 0 -> 0
                else -> volumePercent
            }

            // Display volume percent with leading zero if less than 10
            val formattedVolumeText = if (value < 10 && value != 0) "0$value" else "$value"

            binding?.progressBar?.progress = value
            binding?.brightnessText?.text = formattedVolumeText

            if (value == 0) {
                PlayerDelegateImpl.isSoundMuted = true
                binding?.volumeIcon?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this, R.drawable.ic_volumeoff_bar
                    )
                )
            } else {
                PlayerDelegateImpl.isSoundMuted = false
                binding?.volumeIcon?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this, R.drawable.ic_volumeon_bar
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showBrightnessDialog(brightnessPercent: Int) {
        try {
            binding?.brightProgress?.visibility = View.VISIBLE
            binding?.vlumeandbright?.visibility = View.VISIBLE
            binding?.volumeText?.text = "Brightness"

            binding?.icRepeat?.visibility = View.GONE
            binding?.icScreenshot?.visibility = View.GONE
            binding?.icRotation?.visibility = View.GONE
            binding?.icTimer?.visibility = View.GONE

            // Ensure brightnessPercent is within the valid range [0, 100]
            var value = when {
                brightnessPercent > 100 -> 100
                brightnessPercent < 0 -> 0
                else -> brightnessPercent
            }

            // Display brightness percent with leading zero if less than 10
            val formattedBrightnessText = if (value < 10 && value != 0) "0$value" else "$value"

            viewModel.savebrightnessPrefs(value)
            binding?.progressBarBright?.progress = value
            binding?.brightnessText?.text = formattedBrightnessText
            binding?.brightnessBar?.progress = value
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun getCurrentPositionWhenPlaying(): Long? {
        return player?.currentPosition
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        hideNativeAd()
        try {
            if (this.isInPictureInPictureMode) {
                binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
                binding?.root?.findViewById<ConstraintLayout>(R.id.main_controller)?.visibility =
                    View.GONE
                wasInPictureInPictureMode = true
            } else {
                isPipMode = false
                wasInPictureInPictureMode = false
                //   binding?.videoPlayerFeaturesIconsLayout?.visibility = View.VISIBLE
                // binding?.icCutter?.visibility = View.VISIBLE
                //   binding?.topfeaturesRecycler?.visibility = View.VISIBLE
//                    binding?.root?.findViewById<ConstraintLayout>(R.id.main_controller)?.visibility = View.VISIBLE
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        binding?.root?.findViewById<ImageView>(R.id.pip_button)?.visibility = View.VISIBLE
//                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun showProgressDialog(
        deltaX: Float,
        seekTime: String,
        seekTimePosition: Long,
        totalTime: String,
        totalTimeDuration: Long
    ) {
        try {
            // Inflate and find views if not done already
            binding?.progressduration?.visibility = View.VISIBLE
            val progress =
                (seekTimePosition * 100 / (if (totalTimeDuration == 0L) 1 else totalTimeDuration)).toInt()
            binding?.progressBarDuration?.progress = progress
            binding?.progressText?.text = "$seekTime / $totalTime"
            debounceJob?.cancel()
            actualIsPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun lockScreen(lock: Boolean) {
        try {
            if (lock) {
                binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
                binding?.root?.findViewById<ConstraintLayout>(R.id.videoControllerIconsLayout)?.visibility =
                    View.GONE
                binding?.root?.findViewById<LinearLayout>(R.id.videoProgressLayout)?.visibility =
                    View.GONE
                binding?.customTouchView?.visibility = View.GONE
                binding?.viewUnlock?.visibility = View.VISIBLE
                binding?.viewUnlock2?.visibility = View.VISIBLE
                binding?.icRepeat?.visibility = View.GONE
                binding?.icScreenshot?.visibility = View.GONE
                binding?.icRotation?.visibility = View.GONE
                binding?.icTimer?.visibility = View.GONE
            } else {
                binding?.videoPlayerFeaturesIconsLayout?.visibility = View.VISIBLE
                binding?.root?.findViewById<ConstraintLayout>(R.id.videoControllerIconsLayout)?.visibility =
                    View.VISIBLE
                binding?.root?.findViewById<LinearLayout>(R.id.videoProgressLayout)?.visibility =
                    View.VISIBLE
                binding?.customTouchView?.visibility = View.VISIBLE
                binding?.videoView?.showController()
                binding?.viewUnlock?.visibility = View.GONE
                binding?.viewUnlock2?.visibility = View.GONE
                binding?.icRepeat?.visibility = View.VISIBLE
                binding?.icScreenshot?.visibility = View.VISIBLE
                binding?.icRotation?.visibility = View.VISIBLE
                binding?.icTimer?.visibility = View.VISIBLE
                showSystemUI()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        AppUtils.setLocate(this)
        if (isShowFileChooser) {
            Log.e("PlayerActivity", "File chooser is shown, exiting onResume()")
            return
        }
        try {
            Log.e("PlayerActivity", "Loading rewarded ad...")
            AdsManager.loadRewardedAd(this)
            if (!isSplash) {
                Log.e("PlayerActivity", "Setting playWhenReady = true")
                player?.playWhenReady = true
            }
            isliveuri = intent.getBooleanExtra("isliveuri", false)
            alreadyAdShown = intent.getBooleanExtra("alreadyAdShown", false)
            isYoutubeLink = intent.getBooleanExtra("isYouTube", false)
            isPlaybackCount = intent.getBooleanExtra("isPlaybackCount", false)
            isOnlineStreaming = intent.getBooleanExtra("isOnlineStreaming", false)
            isHistory = intent.getBooleanExtra("ishistory", false)
            Log.e(
                "PlayerActivity",
                "Intent Data - isliveuri: $isliveuri, isYoutubeLink: $isYoutubeLink, isPlaybackCount: $isPlaybackCount, isOnlineStreaming: $isOnlineStreaming, isHistory: $isHistory"
            )
            if (!viewModel.icBgAudioClicked) {
                Log.e("PlayerActivity", "Releasing player (background audio not clicked)")
//                viewModel.releasePlayer(playerListener,"onresume")
            } else {
                Log.e("PlayerActivity", "icBgAudioClicked was true, resetting flag")
                viewModel.icBgAudioClicked = false
            }
            if (player == null) {
                Log.e("PlayerActivity", "Player is NULL or SDK version <= 23, initializing...")
                if (isliveuri || isYoutubeLink) {
                    Log.e(
                        "PlayerActivity",
                        "Live URI or YouTube link detected, skipping video list setup"
                    )
                    listvideos = emptyList()
                    binding?.icAudioTrack?.visibility = View.GONE
                    binding?.tvAudioTrack?.visibility = View.GONE
                    binding?.icSubtitle?.visibility = View.GONE
                    binding?.btnPlay?.visibility = View.GONE
                    if (isYoutubeLink) {
                        binding?.cast?.visibility = View.GONE
                        Log.e("PlayerActivity", "YouTube link detected, notifying user")
                        viewModel.notifyUser(this)
                    } else {
                        if (isOnlineStreaming) {
                            binding?.cast?.visibility = View.VISIBLE
                        }
                        Log.e(
                            "PlayerActivity",
                            "Initializing live player with URI: ${intent.getStringExtra("uri")}"
                        )
                        initializePlayerwithlive(Uri.parse(intent.getStringExtra("uri") ?: ""), 0)
                    }
                } else {
                    Log.e("PlayerActivity", "Not a live URI or YouTube link, fetching video list")
                    if (!isFromLaunchers) {
                        viewModel.geturiListinBackground(listvideos)
                    } else {
                        viewModel.videosUrlList.value = viewModel.singleList

                    }
                }
            }
            Log.e("PlayerActivity", "Setting up ad observer")
            Log.e("PlayerActivity", "Starting 25-second delay for ad handling")
            lifecycleScope.launch {
                delay(25000)
                isShowingAd = false
                Log.e("PlayerActivity", "isShowingAd set to false after delay")
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Exception in onResume()", e)
            e.printStackTrace()
        }
        if (viewModel.savedSpeed) {
            player?.setPlaybackSpeed(viewModel.getspeed())
            Log.e("PlayerActivity", "Setting saved playback speed: ${viewModel.getspeed()}")
            handlePlaySpeedSelectedButtonColor(viewModel.getspeed().toString())
        } else {
            Log.e("PlayerActivity", "Setting default playback speed: 1.0")
            player?.setPlaybackSpeed(1.0f)
            handlePlaySpeedSelectedButtonColor("1.0")
        }
    }


    @SuppressLint("SuspiciousIndentation")
    private fun startObserver() {
        //// OLD Code
        viewModel.videosUrlList.observe(this) {
            if (it.isEmpty()) {
                return@observe // Early return if the list is empty
            }
            if (!dontplaybackuntilkill && isPlaybackCount) {
                dontplaybackuntilkill = true
                // Get the video at the current position, if available
                val video = videolistglobal.getOrNull(position)

                val playbackPosition = video?.lastPlayed ?: 0
                viewModel.playbackPosition = playbackPosition

                video?.let { v ->
                    if (v.playedCompletely) v.playedCompletely = false
                    if (playbackPosition > 0L) {
                        // Get the user's choice for playback
                        val option = showplaybackposition(this)

                        // Handle the option, defaulting to reinitialize the player
                        when (option) {
                            1 -> {
                                showPlaybacklayout(it, position)
                                initializePlayer(it, position)
                            }

                            2 -> initializePlayer(it, position)
                            3 -> {
                                viewModel.playbackPosition = 0
                                initializePlayer(it, position)
                            }

                            else -> {
                                showPlaybacklayout(it, position)
                                initializePlayer(it, position)
                            }
                        }
                    } else {
                        initializePlayer(it, position)
                    }
                } ?: initializePlayer(it, 0)  // Fallback if video is null, start at position 0
            } else {
                initializePlayer(it, position)
            }
        }

        //This observer will work for android 31 and above
        subTitleUri.observe(this) {
            try {
                if (it.isNotEmpty()) {
//                    if(isValidSubtitleFile(Uri.parse(it))==true){
                    isPlayerInitialized.observe(this) { isInitialized ->
                        if (it.isNotEmpty() && isInitialized) {
                            binding?.videoView?.subtitleView?.let { it1 ->
                                setSubTitle(
                                    listvideos,
                                    it, it1, viewModel.newPos
                                )
                            }
                            val videoID = listvideos[player?.currentMediaItemIndex ?: 0]
                            val subtitleState = SubtitleState(
                                videoId = videoID.id,
                                subtitlePath = it,
                                hasSubtitle = true,
                                toggle = true
                            )
                            viewModel.insertSubtitleWithVideoId(subtitleState)
                            viewModel.checkIFSubtitleTurnOn()
                            //                        dismissAllDialogs(this)
                        }
                    }
                    /*}
                    else{
                        Toast.makeText(this, "Invalid Subtitle", Toast.LENGTH_SHORT).show()
                    }*/
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModel.openEqualizerEvent.observe(this) { event ->
            event.let { equalizerEvent -> // Use the correct type
                addFragment(equalizerEvent.fragment)
            }
        }

    }


    //// old player
//    private fun initializePlayer(videoUris: List<Uri>, startPositionIndex: Int = 0) {
//
//        try {
//            setupRenderersFactory()
//            uriList.clear()
//            uriList.addAll(videoUris)
//            // Configure the load control with buffering strategy for smoother seeking
//            val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
//                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
//                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
//                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
//                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
//                ).build()
//
//            val extractorsFactory = DefaultExtractorsFactory().setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS).setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
//
//            val mediaItems = videoUris.map { MediaItem.fromUri(it) }
//
//            // Initialize the track selector
//            trackselector = DefaultTrackSelector(applicationContext)
//            // Build the player with the specified load control and other configurations
//            player = ExoPlayer.Builder(this@PlayerVideoActivity)
//                .setTrackSelector(trackselector!!)
//                .setLoadControl(loadControl) // Set load control here
//                .setHandleAudioBecomingNoisy(true)
//                .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
//                .setRenderersFactory(renderersFactory)
//                .build().also { exoPlayer ->
//                    exoPlayer.setMediaItems(mediaItems)
//                    exoPlayer.setAudioAttributes(getAudioAttributes(), true)
//                    exoPlayer.seekTo(startPositionIndex, viewModel.playbackPosition)
//                    exoPlayer.addListener(playerListener)
//                    exoPlayer.prepare()
//
//                    // Set the player to the UI view
//                    binding?.videoView?.player = exoPlayer
//                    exoPlayer.playWhenReady = playWhenReady // Start playing the video immediately
//
//                    // Set playback speed
//                    adapterPlayerTopFeatures?.updateSpeed("${BottomSheetPlaybackSpeed.playBackSpeed}" + "x")
//                    exoPlayer.setPlaybackSpeed(BottomSheetPlaybackSpeed.playBackSpeed)
//                    binding?.videoView?.hideController()
//                    viewModel.newPos = viewModel.playbackPosition
//                }
//
//            // Mark the player as initialized
//            isPlayerInitialized.value = true
//            currentPosition = player?.currentWindowIndex
//
//            // Set repeat mode if required
//            if (viewModel.icRepeat) {
//                player?.repeatMode = Player.REPEAT_MODE_ONE
//            }
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    private fun initializePlayer(videoUris: List<Uri>, startPositionIndex: Int = 0) {
        try {
            Log.e("PlayerActivity", "initializePlayer called")
            setupRenderersFactory()
            uriList.clear()
            uriList.addAll(videoUris)
            val mediaItems = videoUris.map { MediaItem.fromUri(it) }
            if (player == null) {
                // Configure the load control with buffering strategy for smoother seeking
                val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                ).build()
                val extractorsFactory = DefaultExtractorsFactory()
                    .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
                    .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
                // Initialize the track selector
                trackselector = DefaultTrackSelector(applicationContext)
                // Build the player with the specified load control and other configurations
                player = ExoPlayer.Builder(this@PlayerVideoActivity)
                    .setTrackSelector(trackselector!!)
                    .setLoadControl(loadControl)
                    .setHandleAudioBecomingNoisy(true)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
                    .setRenderersFactory(renderersFactory)
                    .build()
            }
            player?.let { exoPlayer ->
                exoPlayer.setMediaItems(mediaItems)
                exoPlayer.setAudioAttributes(getAudioAttributes(), true)
                exoPlayer.seekTo(startPositionIndex, viewModel.playbackPosition)
                exoPlayer.addListener(playerListener)
                exoPlayer.prepare()
                // Set the player to the UI view
                binding?.videoView?.player = exoPlayer
                exoPlayer.playWhenReady = playWhenReady // Start playing the video immediately
                // Set playback speed
                exoPlayer.setPlaybackSpeed(BottomSheetPlaybackSpeed.playBackSpeed)
                binding?.videoView?.hideController()
                viewModel.newPos = viewModel.playbackPosition
            }
            if (ifAdDisplayed?.value == true) {
                player?.pause()
            }
            // Mark the player as initialized
            isPlayerInitialized.value = true
            currentPosition = player?.currentWindowIndex
            // Set repeat mode if required
            if (viewModel.icRepeat) {
                player?.repeatMode = Player.REPEAT_MODE_ONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializePlayerwithlive(videoUris: Uri, startPositionIndex: Int = 0) {
        try {
            uriList.clear()
            uriList.add(videoUris)
            if (player == null) {
                player =
                    ExoPlayer.Builder(this@PlayerVideoActivity).setHandleAudioBecomingNoisy(true)
                        .build()
            }
            player?.let { exoPlayer ->
                val mediaItem = MediaItem.fromUri(videoUris)
                binding?.videoView?.player = exoPlayer
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady // start playing the video immediately
                exoPlayer.seekTo(startPositionIndex, viewModel.playbackPosition)
                exoPlayer.addListener(playerListener)
                exoPlayer.prepare()
                binding?.title?.text = mediaItem.mediaMetadata.title
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onPause() {
        super.onPause()
        player?.currentPosition?.let {
            viewModel.lastplaybackforsave = it
        }
        if (isShowFileChooser) return
        try {
            if (Util.SDK_INT <= 23) {
                viewModel.releasePlayer(playerListener, "onpause")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            viewModel.releasePlayer(playerListener, "onstop")
            isPlayerInitialized.value = false
        }
        if (wasInPictureInPictureMode) {
            finishAndRemoveTask()
        }
        if (!isPipMode) {
            videolistglobal = emptyList()
        }
    }

    fun updateButtonStates() {
        try {
            val currentIndex = player?.currentMediaItemIndex
            val itemCount = player?.mediaItemCount
            val prevButton = binding?.root?.findViewById<AppCompatImageView>(R.id.prevplay)
            val nextButton = binding?.root?.findViewById<AppCompatImageView>(R.id.nextplay)

            if (currentIndex != null && itemCount != null) {
                // Determine the enabled state
                val isPrevEnabled = currentIndex > 0
                val isNextEnabled = currentIndex < itemCount - 1

                // Set enabled state
                prevButton?.isEnabled = isPrevEnabled
                nextButton?.isEnabled = isNextEnabled

                // Set color based on enabled/disabled state
                val enabledColor = ContextCompat.getColor(binding?.root?.context!!, R.color.white)
                val disabledColor = ContextCompat.getColor(binding?.root?.context!!, R.color.grey01)

                prevButton?.setColorFilter(if (isPrevEnabled) enabledColor else disabledColor)
                nextButton?.setColorFilter(if (isNextEnabled) enabledColor else disabledColor)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateButtonStates()
            if (playbackState == Player.STATE_ENDED) {
                if (position in videolistglobal.indices) {
                    itemsaving = videolistglobal.getOrNull(position)
                    itemsaving?.playedCompletely = true
                    itemsaving?.let {
                        viewModel.updateUserData(it)
                    }
                }
            }
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }

            if (playbackState == Player.STATE_BUFFERING && isliveuri) {
                binding?.ytProgress?.visibility = View.VISIBLE
            } else if (playbackState == Player.STATE_IDLE && isliveuri) {
                binding?.ytProgress?.visibility = View.VISIBLE
            } else {
                binding?.ytProgress?.visibility = View.INVISIBLE
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            updateButtonStates()
        }

        @SuppressLint("SwitchIntDef")
        @Deprecated("Deprecated in Java")
        override fun onPositionDiscontinuity(reason: Int) {
            when (reason) {
                Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> {
                    if (!viewModel.autoplayfeature) {
                        player?.stop()
                        onBackPressed()
                    }
                    if (isPlaybackCount) {
                        if (isFromPlaylist) {
                            if (position in videolistglobal.indices) {
                                val id = videolistglobal.getOrNull(position)?.id ?: 0
                                val lastPlayed = 0L
//                                viewModel.updateVideoEntityPlaylist(lastPlayed, id)
                            }
                        } else {
                            if (position in videolistglobal.indices) {
                                itemsaving = videolistglobal.getOrNull(position)
                                itemsaving?.lastPlayed = 0L
                                itemsaving?.playedCompletely = true
                                itemsaving?.let {
                                    viewModel.updateUserData(it)
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            try {
                bookmarkDialog?.dismiss()
                bookmarkDialogShown = false
                // Check if we need to handle the back press
                if (playSingleVideo) {
                    onBackPressed()
                    backFromPlayer = true
                    return // Early return after handling back press
                }

                if (listvideos.isNotEmpty()) {
                    if (!intent.getBooleanExtra("isliveuri", false)) {
                        player?.currentMediaItemIndex?.let { index ->
                            if (index >= 0 && index < listvideos.size) {
                                if (!viewModel.icBgAudioClicked) {
                                    binding?.title?.text = listvideos[index].title
                                }

                                trackname = listvideos[index].title.toString()
//                                position = index
                                val isalreadyWacthed = listvideos[index].playedCompletely
                                if (isalreadyWacthed) {
                                    listvideos[index].playedCompletely = false
                                }
                            }
                            viewModel.currentVideo = listvideos[index]
//                           viewModel.checkIFSubtitleTurnOn()
                            getVideoBookmarks()
                            lifecycleScope.launch {
                                viewModel.addToRecents(
                                    position.toString(),
                                    ArrayList(listvideos)
                                ) {}
                            }
                        }
                    } else {
                        trackname =
                            viewModel.getVideoNameFromUrl(intent.getStringExtra("uri") ?: "")
                        if (!viewModel.icBgAudioClicked) {
                            binding?.title?.text = trackname
                        }
                    }
                } else {
                    trackname = viewModel.getVideoNameFromUrl(intent.getStringExtra("uri") ?: "")
                    if (!viewModel.icBgAudioClicked) {
                        binding?.title?.text = trackname
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            // check if the reason for the position discontinuity is due to the player moving to a new media item
            if (reason == Player.EVENT_MEDIA_ITEM_TRANSITION) {
                // get the index of the current media item
                player?.currentWindowIndex?.let {
                    position = it
                }
                viewModel.playbackPosition = 0L
            } else if (reason == Player.EVENT_TIMELINE_CHANGED) {
                // get the index of the current media item
                player?.currentWindowIndex?.let {
                    position = it
                }
            }

        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            viewModel.notifyUser(this@PlayerVideoActivity)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)
            if (!isPipMode) {
                if (isFirstPlayLaunch) {
                    ispause = !playWhenReady
                }
            }
        }

    }

    private val playPauseReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                lifecycleScope.launch {
                    // Handle the play/pause action here
                    if (player?.isPlaying == true) {
                        player?.pause()
                    } else {
                        player?.play()
                    }
                    val pictureInPictureParams = player?.isPlaying?.let {
                        buildPIPParams(this@PlayerVideoActivity, it)
                    }
                    if (pictureInPictureParams != null) {
                        setPictureInPictureParams(pictureInPictureParams)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        var player: ExoPlayer? = null
        var instance: WeakReference<PlayerVideoActivity>? = null
        var isPipMode = false
        var playWhenReady = true
        var soundVolume = 0
        var isFirstPlayLaunch = false

        @JvmField
        var subTitleUri = MutableLiveData("")
        var isShowFileChooser = false

        fun getInstance(): PlayerVideoActivity? {
            return instance?.get()
        }

        fun setInstance(activity: PlayerVideoActivity) {
            instance?.clear()
            instance = null
            instance = WeakReference(activity)
        }
    }

    fun displayTextForTwoSeconds(textView: TextView, text: String) {
        jobnew.cancel()
        textView.text = ""
        scope.launch {
            textView.text = text
            delay(2000)
            textView.text = ""
        }
    }

    // Example usage
    fun screenShort() {
        try {
            val weakPlayer = WeakReference(player)
            weakPlayer.get()?.let {
                val videViewSurface = binding?.videoView?.videoSurfaceView as TextureView
                takeScreenshotWithPixelCopy(videViewSurface) {
                    if (it != null) {
                        saveScreenshotToExternalStorage(it) { isSaved ->
                            // Handle the result of the saving operation
                            if (isSaved) {
                                Toast.makeText(
                                    this@PlayerVideoActivity,
                                    "Screenshot saved",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@PlayerVideoActivity,
                                    "Something went wrong",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addFragment(fragment: Fragment) {
        lifecycleScope.launch {
            try {
                // Get the screen height
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenHeight = displayMetrics.heightPixels
                val screenWidth = displayMetrics.widthPixels
                val percentage = 0.6f // 60% as a decimal value
                val desiredHeight =
                    if (!viewModel.isFullScreen) (screenHeight * percentage).toInt() else screenHeight
                val desiredWidth = if (viewModel.isFullScreen) screenWidth / 2 else screenWidth
                // Set the calculated height to the ConstraintLayout
                val layoutParams =
                    binding?.videoEqualizerContainer?.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.height = desiredHeight
                layoutParams.width = desiredWidth
                binding?.videoEqualizerContainer?.layoutParams = layoutParams
                if (viewModel.isFullScreen) {
                    try {
                        // Align view to right side
                        // Get the parent ConstraintLayout
                        val parentConstraintLayout = binding?.playerViewContainer
                        // Get the child ConstraintLayout
                        val childConstraintLayout = binding?.videoEqualizerContainer
                        // Set the width of the child ConstraintLayout to 50% of the parent ConstraintLayout's width
                        val layoutParams =
                            childConstraintLayout?.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams.width = (parentConstraintLayout?.width!! * 0.5).toInt()
                        childConstraintLayout.layoutParams = layoutParams
                        layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                        layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                        childConstraintLayout.layoutParams = layoutParams
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                binding?.videoView?.hideController()
                lifecycleScope.launch {
                    hideSystemUI()
                }
                binding?.videoPlayerFeaturesIconsLayout?.visibility = View.GONE
                fragmentclose = fragment
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(
                    R.id.videoEqualizerContainer, fragment, "videoEqualizer"
                )
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
                delay(200)
                binding?.videoEqualizerContainer?.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buttonClickListeners() {
        binding?.icEqualizer?.setOnClickListener {
            viewModel.isSheetbakpress = true
            try {
                AppUtils.firebaseUserAction(
                    "equalizerBtnClicked_PlayerVideoFragment",
                    "PlayerVideoFragment"
                )
                binding?.bottomView?.visibility = View.GONE
                val sessionID = player?.audioSessionId
                if (sessionID != null && sessionID != 0) {
                    com.video.avd.ui.equalizer.Settings.isEditing = false
                    val equalizerFragment: EqualizerFragmentVideo =
                        EqualizerFragmentVideo.newBuilder().setAudioSessionId(sessionID).build()
                    viewModel.openEqualizerEvent.value = FragmentEvent(equalizerFragment)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding?.icNightMode?.setOnClickListener {
            activateNightMode()
        }
        // Initialize and update the decoder status
        updateDecoderStatus()
        // Set up click listener to toggle decoder preference
        binding?.icDecoder?.setOnClickListener {
            toggleDecoderPreference()
        }

        binding?.icMirror?.setOnClickListener {
            AppUtils.firebaseUserAction("mirrorBtnClicked_videoPlayer", "PlayerVideoActivity")
            try {
                flipVideo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding?.icAudioTrack?.setOnClickListener {
            AppUtils.firebaseUserAction("audioTrackBtnClicked_videoPlayer", "PlayerVideoActivity")
            binding?.bottomView?.visibility = View.GONE
            val mappedTrackInfo = trackselector?.currentMappedTrackInfo ?: return@setOnClickListener
            if (!mappedTrackInfo.isRendererAvailable(C.TRACK_TYPE_AUDIO)) return@setOnClickListener
            val audioTrack = ArrayList<String>()
            val audioList = ArrayList<String>()
            player?.currentTracks?.groups?.let {
                for (group in it) {
                    if (group.mediaTrackGroup.type == C.TRACK_TYPE_AUDIO) {
                        val groupinfo = group.mediaTrackGroup
                        for (i in 0 until groupinfo.length) {
                            audioTrack.add(groupinfo.getFormat(i).language.toString())
                            audioList.add(
                                "${audioList.size + 1}. " + Locale(groupinfo.getFormat(i).language.toString()).displayLanguage + "(${
                                    trackname.substring(
                                        0, minOf(12, trackname.length)
                                    )
                                })"
                            )
                        }
                    }
                }
            }
            val tempTracks = audioList.toArray(arrayOfNulls<CharSequence>(audioList.size))
            AlertDialog.Builder(this, R.style.CustomMaterialDialog)
                .setTitle(getString(R.string.select_language))
                .setOnCancelListener {
                    // Handle cancel action if needed
                }
                .setItems(tempTracks) { _, position ->
                    trackselector?.buildUponParameters()
                        ?.setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
                        ?.setPreferredAudioLanguages(audioTrack[position])
                        ?.let { it1 -> trackselector?.setParameters(it1) }
                }
                .create()
                .show()
        }

        binding?.icSleep?.setOnClickListener {
            try {
                binding?.bottomView?.visibility = View.GONE
                binding?.timerView?.visibility = View.VISIBLE
                binding?.timerView?.let { it1 ->
                    binding?.playerViewContainer?.let { it2 ->
                        viewModel.setSleepTimerViewWidthAndHeight(
                            this, timerView = it1, it2
                        )
                    }
                }
                AppUtils.firebaseUserAction(
                    "sleepBtnClicked_PlayerVideoFragment", "PlayerVideoFragment"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding?.icShare?.setOnClickListener {
            try {
                if (isliveuri || isYoutubeLink) {
                    val link = intent.getStringExtra("uri") ?: ""
                    shareLiveLink(this, link)
                } else {
                    if (!prevousFragment.isNullOrBlank() && prevousFragment == "Status") {
                        shareStatus(this, statusViewModel)
                    } else {
                        if (intent.getBooleanExtra("isliveuri", false)) {
                            shareVideo(this, File(intent.getStringExtra("uri") ?: ""))
                        } else {
                            if (position < listvideos.size) {
                                listvideos[position].contentUri?.let {
                                    AppUtils.getFilePathFromContentUri(
                                        Uri.parse(it), this@PlayerVideoActivity
                                    )?.let { it1 ->
                                        File(it1)
                                    }?.let { it2 ->
                                        binding?.icShare?.isEnabled = false
                                        shareVideo(this, it2)
                                        binding?.icShare?.postDelayed({
                                            binding?.icShare?.isEnabled = true
                                        }, 2000)

                                    }
                                }
                            } else {
                                //
                            }
                        }
                    }
                }
                AppUtils.firebaseUserAction(
                    "shareBtnClicked_PlayerVideoFragment", "PlayerVideoFragment"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding?.icBookmark?.setOnClickListener {
            binding?.bottomView?.visibility = View.GONE
            bookmarkDialog = VideoBookmarkDialogFragment()
            bookmarkDialog?.show(supportFragmentManager, "")
            bookmarkDialogShown = true
            bookmarkDialog?.setBookmarkRemoveListener(object : BookmarkRemoveListener {
                override fun onBookmarkRemove() {

                    lifecycleScope.launch {
                        // Fetch updated bookmarks and refresh the CustomTimeBar
                        val updatedBookmarks = viewModel.getVideoBookmarksByUri(
                            player?.currentMediaItem?.localConfiguration?.uri.toString()
                        )?.firstOrNull() ?: emptyList()

                        val duration = player?.duration
                        exoBar?.setBookmarkPositions(
                            updatedBookmarks.map { it.position },
                            duration ?: 1000
                        )

                    }

                }

                override fun onDialogDismiss() {
                    bookmarkDialogShown = false
                    binding?.videoPlayerFeaturesIconsLayout?.visibility = View.VISIBLE
                    binding?.videoView?.showController()
                    showSystemUI()
                }

            })
        }

        binding?.brightnessBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightnessLevel = progress.toFloat() / binding?.brightnessBar?.max?.toFloat()!!
                viewModel.changeBrightness(brightnessLevel, this@PlayerVideoActivity)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        stopTimerClickListeners()


    }

    private fun setupRenderersFactory() {
        val isUsingSWDecoder = AppPreference.isUsingSWDecoder(this)
        renderersFactory = DefaultRenderersFactory(this).apply {
            if (isUsingSWDecoder) {
                // Use software decoding
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                // Force software decoders
                setEnableDecoderFallback(false)
            } else {
                // Prefer hardware decoders but fallback to software if necessary
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                setEnableDecoderFallback(true)
            }
        }
    }

    private fun updateDecoderStatus() {
        // Since this is a simple read operation, it can safely run on the main thread
        val isUsingSWDecoder = AppPreference.isUsingSWDecoder(this)
        val decoderText = if (isUsingSWDecoder) {
            getString(R.string.decoderSW)
        } else {
            getString(R.string.decoderHW)
        }
        val icon = if (isUsingSWDecoder) {
            R.drawable.ic_decoder_sw
        } else {
            R.drawable.ic_decoder_hw
        }
        binding?.tvDecoder?.text = decoderText
        binding?.icDecoder?.let { Glide.with(this).load(icon).into(it) }
    }

    private fun toggleDecoderPreferenceOld() {
        // Launch a coroutine to handle preference saving in a background thread
        jobnew.cancel()
        scope.launch {
            val isUsingSWDecoder = AppPreference.isUsingSWDecoder(this@PlayerVideoActivity)
            val newPreference = !isUsingSWDecoder
            // Save the new preference on a background thread
            withContext(Dispatchers.IO) {
                AppPreference.saveSWDecoderPreference(this@PlayerVideoActivity, newPreference)
            }
            // Update the UI on the main thread after the preference is saved
            updateDecoderStatus()
            reInitializePlayer()
        }
    }

    private fun toggleDecoderPreference() {
        // Launch a coroutine to handle preference saving in a background thread
        CoroutineScope(Dispatchers.Main).launch {
            val isUsingSWDecoder = AppPreference.isUsingSWDecoder(this@PlayerVideoActivity)
            val newPreference = !isUsingSWDecoder

            // Save the new preference on a background thread
            withContext(Dispatchers.IO) {
                AppPreference.saveSWDecoderPreference(this@PlayerVideoActivity, newPreference)
            }

            // Update the UI on the main thread after the preference is saved
            updateDecoderStatus()
            reInitializePlayer()
        }
    }


    private fun reInitializePlayer() {
        viewModel.releasePlayer(playerListener, "reinit")
        viewModel.videosUrlList.observe(this) {
            if (it.isNotEmpty()) {
                initializePlayer(it, viewModel.currentItem)
            }
        }
    }

    private fun handleTimerSelectedButtonColor(which: String) {
        try {
            when (which) {
                "off" -> {
                    binding?.tvTimerOff?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                    binding?.tvTimer10mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer30mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer60mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimerToEnd?.setBackgroundResource(R.drawable.bg_rounded_textview)
                }

                "10" -> {
                    binding?.tvTimerOff?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer10mnt?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                    binding?.tvTimer30mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer60mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimerToEnd?.setBackgroundResource(R.drawable.bg_rounded_textview)
                }

                "30" -> {
                    binding?.tvTimerOff?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer10mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer30mnt?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                    binding?.tvTimer60mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimerToEnd?.setBackgroundResource(R.drawable.bg_rounded_textview)
                }

                "60" -> {
                    binding?.tvTimerOff?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer10mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer30mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer60mnt?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                    binding?.tvTimerToEnd?.setBackgroundResource(R.drawable.bg_rounded_textview)
                }

                "end" -> {
                    binding?.tvTimerOff?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer10mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer30mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimer60mnt?.setBackgroundResource(R.drawable.bg_rounded_textview)
                    binding?.tvTimerToEnd?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                }

                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopTimerClickListeners() {
        try {
            binding?.tvTimer10mnt?.setOnClickListener {
                lifecycleScope.launch {
                    handleTimerSelectedButtonColor("10")
                    playSingleVideo = false
                    binding?.icSleep?.setImageResource(R.drawable.ic_stop_timer_selected)
                    binding?.tvSleep?.setTextColor(
                        ContextCompat.getColor(
                            this@PlayerVideoActivity, R.color.dark_mode_green
                        )
                    )
                    sleepTimer =
                        viewModel.setSleepTimer(duration = 10, activity = this@PlayerVideoActivity)
                }
            }
            binding?.tvTimer30mnt?.setOnClickListener {
                lifecycleScope.launch {
                    handleTimerSelectedButtonColor("30")
                    playSingleVideo = false
                    binding?.icSleep?.setImageResource(R.drawable.ic_stop_timer_selected)
                    binding?.tvSleep?.setTextColor(
                        ContextCompat.getColor(
                            this@PlayerVideoActivity, R.color.dark_mode_green
                        )
                    )
                    sleepTimer =
                        viewModel.setSleepTimer(duration = 30, activity = this@PlayerVideoActivity)
                }
            }
            binding?.tvTimer60mnt?.setOnClickListener {
                lifecycleScope.launch {
                    handleTimerSelectedButtonColor("60")
                    playSingleVideo = false

                    sleepTimer =
                        viewModel.setSleepTimer(duration = 60, activity = this@PlayerVideoActivity)
                    binding?.icSleep?.setImageResource(R.drawable.ic_stop_timer_selected)
                    binding?.tvSleep?.setTextColor(
                        ContextCompat.getColor(
                            this@PlayerVideoActivity, R.color.dark_mode_green
                        )
                    )
                }
            }

            binding?.tvTimerOff?.setOnClickListener {
                lifecycleScope.launch {
                    handleTimerSelectedButtonColor("off")
                    binding?.icSleep?.setImageResource(R.drawable.ic_stop_timer)
                    binding?.tvSleep?.setTextColor(
                        ContextCompat.getColor(
                            this@PlayerVideoActivity, R.color.brand_text_primary
                        )
                    )
                    playSingleVideo = false
                    if (sleepTimer != null) {
                        sleepTimer?.cancel()
                        sleepTimer = null
                    }
                }
                Toast.makeText(this@PlayerVideoActivity, "Timer is turned off", Toast.LENGTH_SHORT)
                    .show()
            }

            binding?.tvTimerToEnd?.setOnClickListener {
                lifecycleScope.launch {
                    handleTimerSelectedButtonColor("end")
                    playSingleVideo = true
                    binding?.icSleep?.setImageResource(R.drawable.ic_stop_timer_selected)
                    binding?.tvSleep?.setTextColor(
                        ContextCompat.getColor(
                            this@PlayerVideoActivity, R.color.dark_mode_green
                        )
                    )
                }
                Toast.makeText(this@PlayerVideoActivity, "Timer set until end", Toast.LENGTH_SHORT)
                    .show()
            }

            binding?.icTimerBack?.setOnClickListener {
                binding?.bottomView?.visibility = View.VISIBLE
                binding?.timerView?.visibility = View.GONE
            }
            binding?.timerView?.setOnClickListener {

            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAnimations() {
        try {
            AppPreference.setFirstPlayLaunch(this, true)
            /*         binding?.animationLayout?.visibility = View.VISIBLE
                     lottieAnimationView = binding?.lottieAnimationView!!
                     lottieAnimationView.setAnimation(animationList[currentAnimationIndex])
                     currentAnimationIndex++
                     binding?.animationLayout?.setOnClickListener {
                     }
                     val nextButton = binding?.nextButton
                     var btnclick = 1
                     nextButton?.setOnClickListener {
                         btnclick++
                         if (btnclick == 4) {
                             AppUtils.firebaseUserAction("nextBtnClicked_videoPlayer", "PlayerVideoActivity")
                             binding?.animationLayout?.visibility = View.GONE
                             requestedOrientation =
                                 if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                 else ActivityInfo.SCREEN_ORIENTATION_SENSOR
                         } else {
                             loadNextAnimation()
                         }
                     }
                     binding?.skip?.setOnClickListener {
                         AppUtils.firebaseUserAction("skipBtnClicked_videoPlayer", "PlayerVideoActivity")
                         binding?.animationLayout?.visibility = View.GONE
                         requestedOrientation =
                             if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                             else ActivityInfo.SCREEN_ORIENTATION_SENSOR
                     }*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadNextAnimation() {
        try {
//            if (currentAnimationIndex < animationList.size) {
//                lottieAnimationView.setAnimation(animationList[currentAnimationIndex])
//                lottieAnimationView.playAnimation()
//                currentAnimationIndex = (currentAnimationIndex + 1) % animationList.size
//            } else {
//                binding?.animationLayout?.visibility = View.GONE
//                requestedOrientation =
//                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//                    else ActivityInfo.SCREEN_ORIENTATION_SENSOR
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var cumulativeSkipSeconds = 0
    private var cumulativeSkipSecondsbaxk = 0
    private var resetJob: Job? = null
    private var resetJobback: Job? = null

    private fun performDoubleTapAction(view: View, event: MotionEvent) {
        if (!isLock) {
            debounceJob?.cancel()
            actualIsPlaying = true
            if (viewModel.fastforwardfeature) {
                try {
                    val screenWidth = view.width
                    val x = event.x
                    // Get the duration of the video
                    val totalDuration = player?.duration ?: 0
                    // Detect which side of the view was double tapped
                    // Calculate the seek position based on the tap position
                    if (x > screenWidth / 2) {
                        // Right side double tap (fast-forward)
                        val currentPosition = player?.currentPosition ?: 0
                        val newSeekPosition =
                            currentPosition + 5000 // Fast-forward by 10 seconds (10000 milliseconds)
                        if (newSeekPosition > totalDuration) totalDuration else newSeekPosition
                        // Right side double-tap (Fast-forward)
                        binding?.fastforwardanim?.let { updateLottieAnimation(it) }
                        player?.seekTo(newSeekPosition)
                    } else {
                        // Left side double tap (rewind)
                        binding?.rewindanim?.visibility = View.VISIBLE
                        binding?.rewindanim?.playAnimation()
                        val currentPosition = player?.currentPosition ?: 0
                        val newSeekPosition =
                            currentPosition - 5000 // Rewind by 10 seconds (10000 milliseconds)
                        if (newSeekPosition < 0) 0 else newSeekPosition
                        // Left side double-tap (Rewind)
                        binding?.rewindanim?.let { updateLottieAnimationback(it) }
                        player?.seekTo(newSeekPosition)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateLottieAnimation(lottieView: LottieAnimationView) {
        // Update cumulative skip seconds
        val skip = 10000
        cumulativeSkipSeconds += skip


        // Ensure the cumulative skip seconds do not go below zero

        cumulativeSkipSeconds = cumulativeSkipSeconds.coerceAtLeast(0)

        // Create and set the TextDelegate
        val textDelegate = TextDelegate(lottieView)
        lottieView.setTextDelegate(textDelegate)

        // Set the FontAssetDelegate to use the default typeface
        lottieView.setFontAssetDelegate(object : FontAssetDelegate() {
            override fun fetchFont(fontFamily: String?): Typeface {
                return Typeface.DEFAULT
            }
        })

        // Set the text for the Lottie animation dynamically
        val skipText = "+${cumulativeSkipSeconds / 1000}s"
        val layerName = "+10s"
        textDelegate.setText(layerName, skipText)

        // Play the Lottie animation
        lottieView.visibility = View.VISIBLE
        lottieView.playAnimation()

        resetJob?.cancel()

        // Reset the cumulative skip seconds after a delay
        resetJob = lifecycleScope.launch {
            delay(1500)
            cumulativeSkipSeconds = 0 // Reset the counter
            lottieView.cancelAnimation()
            lottieView.visibility = View.GONE
        }
    }

    private fun updateLottieAnimationback(lottieView: LottieAnimationView) {
        // Update cumulative skip seconds
        val skip = 10000
        cumulativeSkipSecondsbaxk -= skip

        // Create and set the TextDelegate
        val textDelegate = TextDelegate(lottieView)
        lottieView.setTextDelegate(textDelegate)

        // Set the FontAssetDelegate to use the default typeface
        lottieView.setFontAssetDelegate(object : FontAssetDelegate() {
            override fun fetchFont(fontFamily: String?): Typeface {
                return Typeface.DEFAULT
            }
        })

        // Set the text for the Lottie animation dynamically
        val skipText = "${cumulativeSkipSecondsbaxk / 1000}s"
        val layerName = "-10s"
        textDelegate.setText(layerName, skipText)

        // Play the Lottie animation
        lottieView.visibility = View.VISIBLE
        lottieView.playAnimation()

        resetJobback?.cancel()

        // Reset the cumulative skip seconds after a delay
        resetJobback = lifecycleScope.launch {
            delay(1500)
            cumulativeSkipSecondsbaxk = 0 // Reset the counter
            lottieView.cancelAnimation()
            lottieView.visibility = View.GONE
        }
    }

    override fun pauseVideo(ispause: Boolean) {
        try {
            if (ispause) {
                player?.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initPip(activity: Activity) {
        if (!isPipMode) {
            isShowingAd = true
            createPIPMode(activity)
            isPipMode = true
        } else {
            createPIPMode(activity)
        }
    }

    override fun onPlaybackSpeedChange(playbackSpeedValue: Float) {
        player?.setPlaybackSpeed(playbackSpeedValue)
        viewModel.saveSpeedValueToPrefs(playbackSpeedValue)
    }

    private fun setBottomViewWidthAndHeight() {
        binding?.volumeBar?.let { viewModel.changeVolume(it, this) }
        // Get the screen height
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        val percentage = 0.6f // 60% as a decimal value
        val desiredHeight =
            if (!viewModel.isFullScreen) (screenHeight * percentage).toInt() else screenHeight
        val desiredWidth = if (viewModel.isFullScreen) screenWidth / 2 else screenWidth
        // Set the calculated height to the ConstraintLayout
        val layoutParams = binding?.bottomView?.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.height = desiredHeight
        layoutParams.width = desiredWidth
        binding?.bottomView?.layoutParams = layoutParams
        //  binding?.bottomView?.visibility = View.VISIBLE
        if (viewModel.isFullScreen) {
            try {
                // Align view to right side
                // Get the parent ConstraintLayout
                val parentConstraintLayout = binding?.playerViewContainer
                // Get the child ConstraintLayout
                val childConstraintLayout = binding?.bottomView
                // Set the width of the child ConstraintLayout to 50% of the parent ConstraintLayout's width
                val layoutParams =
                    childConstraintLayout?.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.width = (parentConstraintLayout?.width!! * 0.5).toInt()
                childConstraintLayout.layoutParams = layoutParams
                layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                childConstraintLayout.layoutParams = layoutParams
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handlePlaySpeedSelectedButtonColor(which: String) {
        when (which) {
            "025" -> {
                AppUtils.firebaseUserAction(
                    "0.25_SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "05" -> {
                AppUtils.firebaseUserAction(
                    "0.5SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "1" -> {
                AppUtils.firebaseUserAction("1_SpeedBtn_videoPlayer", "PlayerVideoActivity")
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "125" -> {
                AppUtils.firebaseUserAction(
                    "1.25SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "15" -> {
                AppUtils.firebaseUserAction(
                    "1.5SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "20" -> {
                AppUtils.firebaseUserAction(
                    "2.0SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "30" -> {
                AppUtils.firebaseUserAction(
                    "3.0SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview)
            }

            "40" -> {
                AppUtils.firebaseUserAction(
                    "4.0SpeedBtn_videoPlayer", "PlayerVideoActivity"
                )
                binding?.tv025?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv05?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv1?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv125?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv15?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv20?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv30?.setBackgroundResource(R.drawable.bg_rounded_textview)
                binding?.tv40?.setBackgroundResource(R.drawable.bg_rounded_textview_selected)
            }

            else -> {}
        }
    }

    private fun playSpeedClickListeners() {

        binding?.tv025?.setOnClickListener {
            player?.setPlaybackSpeed(0.25f)
            handlePlaySpeedSelectedButtonColor("025")
        }

        binding?.tv05?.setOnClickListener {
            player?.setPlaybackSpeed(0.5f)
            handlePlaySpeedSelectedButtonColor("05")
        }

        binding?.tv1?.setOnClickListener {
            player?.setPlaybackSpeed(1f)
            handlePlaySpeedSelectedButtonColor("1")
        }

        binding?.tv125?.setOnClickListener {
            player?.setPlaybackSpeed(1.25f)
            handlePlaySpeedSelectedButtonColor("125")
        }

        binding?.tv15?.setOnClickListener {
            player?.setPlaybackSpeed(1.5f)
            handlePlaySpeedSelectedButtonColor("15")
        }

        binding?.tv20?.setOnClickListener {
            player?.setPlaybackSpeed(2.0f)
            handlePlaySpeedSelectedButtonColor("20")
        }

        binding?.tv30?.setOnClickListener {
            player?.setPlaybackSpeed(3.0f)
            handlePlaySpeedSelectedButtonColor("30")
        }

        binding?.tv40?.setOnClickListener {
            player?.setPlaybackSpeed(4.0f)
            handlePlaySpeedSelectedButtonColor("40")
        }

    }

    fun flipVideo() {
        try {
            // Get the TextureView from the PlayerView
            val textureView = binding?.videoView?.videoSurfaceView as TextureView
            // Create a new transformation matrix
            val matrix = Matrix()
            // Get the current matrix from the TextureView
            textureView.let {
                it.getTransform(matrix)
                // Apply a horizontal flip transformation
                if (isFlipped) {
                    // If already flipped, flip back to original state
                    binding?.icMirror?.setImageResource(R.drawable.ic_mirror)
                    binding?.tvMirror?.setTextColor(
                        ContextCompat.getColor(
                            this@PlayerVideoActivity, R.color.brand_text_primary
                        )
                    )
                    matrix.setScale(1f, 1f, it.width / 2f, it.height / 2f)
                } else {
                    // If not flipped, apply the flip transformation
                    matrix.setScale(-1f, 1f, it.width / 2f, it.height / 2f)
                    binding?.icMirror?.setImageResource(R.drawable.ic_mirror_selected)
                    binding?.tvMirror?.setTextColor(resources.getColor(R.color.gSelector))
                }
                // Set the new matrix to the TextureView
                it.setTransform(matrix)
                isFlipped = !isFlipped
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads CastContext on a background thread (to avoid Binder/ANR on main thread), then creates
     * ChromecastConnection and completes setup on the main thread.
     */
    private fun setupChromecastConnectionAsync() {
        mDefaultCastStateListener = object : ChromecastConnection.CastStateUpdateListener() {
            override fun onReceiverAvailableUpdate(available: Boolean) {}
        }

        mDefaultCastStateListener?.setCastDrawable(
            AppCompatResources.getDrawable(this, R.drawable.ic_cast_player_selected),
            AppCompatResources.getDrawable(this, R.drawable.ic_cast_player),
            AppCompatResources.getDrawable(this, R.drawable.ic_cast_player_not_available)
        )

        mDefaultCastStateListener?.setCastIcon(binding?.cast)

        lifecycleScope.launch {
            val castContext = withContext(Dispatchers.IO) {
                try {
                    CastContext.getSharedInstance(this@PlayerVideoActivity)
                } catch (e: Exception) {
                    Log.e("PlayerVideoActivity", "CastContext init failed", e)
                    null
                }
            }
            if (castContext != null && isFinishing.not()) {
                mChromecastConnection = ChromecastConnection(
                    this@PlayerVideoActivity,
                    mDefaultCastStateListener!!,
                    castContext
                )
                mChromecastConnection?.initialize(CastConstant.CAST_APPLICATION_ID)
            }
            setupChromecastClickListeners()
        }
    }

    private fun setupChromecastClickListeners() {
        binding?.cast?.setOnClickListener {
            AppUtils.firebaseUserAction("castBtnClicked_videoPlayer", "PlayerVideoActivity")
            binding?.videoEqualizerContainer?.visibility = View.GONE
            ChromeCastDelegate.mPreparingConnectionDialog = null
            if (mChromecastConnection?.isChromeCastConnect == true) {
                mChromecastConnection?.requestEndSession(object :
                    ChromecastConnection.RequestEndSessionCallback() {
                    override fun onSuccess() {
                        ToastUtils.showMessageLong(
                            applicationContext,
                            getString(R.string.cast_stop_casting_success)
                        )
                        ChromeCastDelegate.mPreparingConnectionDialog = null
                        updateSelectedPosition(-1)
                        mChromecastConnection?.stopMediaIfPlaying()
                    }

                    override fun onCancel() {
                        // do nothing
                        Toast.makeText(this@PlayerVideoActivity, "Canceled", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            } else {
                // mChromecastConnection?.requestStartSession(mDefaultRequestSessionCallbackFun(this))
            }
            if (isliveuri && !isYoutubeLink) {
                val link = intent.getStringExtra("uri") ?: ""
                if (link.isNotEmpty()) {
                    startChromeCastConnectionForLiveVideos(this, link)
                    isCastingForStreaming = true
                }
            } else if (!videolistglobal.isNullOrEmpty()) {
                ChromecastConnection.listofvideos = videolistglobal
                ChromecastConnection.position = position
                startChromeCastConnection(ChromecastConnection.listofvideos, this, position)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CastConstant.START_MEDIA_SERVICE) {
            when (resultCode) {
                CastConstant.CONNECT_SUCCESS_MESSAGE -> {
                    try {
                        ToastUtils.showMessageShort(
                            this, getString(R.string.cast_start_casting_success)
                        )
                        hidePrepareServerDialog()
                        if (isCastingForStreaming) {
                            loadRemoteMediaForStreaming(this, intent.getStringExtra("uri") ?: "")
                            isCastingForStreaming = false
                        } else {
                            loadRemoteMedia(this)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                CastConstant.CONNECT_ERROR_MESSAGE -> {
                    ToastUtils.showMessageLong(this, getString(R.string.cast_start_casting_error))
                    updateSelectedPosition(-1)
                    hidePrepareServerDialog()
                    mChromecastConnection?.stopMediaIfPlaying()
                }

                CastConstant.CONNECT_DESTROY_MESSAGE -> {
                    ToastUtils.showMessageShort(
                        this,
                        getString(R.string.cast_start_casting_playing_fail)
                    )
                    updateSelectedPosition(-1)
                    hidePrepareServerDialog()
                    mChromecastConnection?.stopMediaIfPlaying()
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
       /* try {
            if (fragmentclose == null) {
                player?.stop()
            }
        } catch (e: Exception) {
           e.printStackTrace()
        }*/
        backfunctionality()
    }

    fun showPlaybacklayout(videoUris: List<Uri>, startPositionIndex: Int = 0) {
        lifecycleScope.launch {
            // Show the playback layout for 7 seconds, then hide it
            binding?.playbacklayout?.playbackmain?.visibility = View.VISIBLE
            delay(7000)
            binding?.playbacklayout?.playbackmain?.visibility = View.GONE
        }

        // Handle the start button click - reset playback position and initialize player
        binding?.playbacklayout?.starttext?.setOnClickListener {
            binding?.playbacklayout?.playbackmain?.visibility = View.GONE

            // Reset playback position and last playback data
            viewModel.playbackPosition = 0L
            viewModel.lastplaybackforsave = 0L

            // Update video entity (save the playback position and other data)
            if (position in videolistglobal.indices) {
                itemsaving = videolistglobal[position]
            }
            itemsaving?.lastPlayed = viewModel.lastplaybackforsave

            // Update database based on playlist or non-playlist context
            if (isFromPlaylist) {
//                val id = videolistglobal[position].id
//                val lastPlayed = viewModel.lastplaybackforsave
//                viewModel.updateVideoEntityPlaylist(lastPlayed, id)
            } else {
                itemsaving?.let { viewModel.updateUserData(it) }
            }

            // Release the player and re-initialize only if necessary
            viewModel.releasePlayerfromstart(playerListener)

            // Check if the player needs to be reinitialized based on media URIs or position
            if (player == null || player?.currentMediaItem?.mediaId != videoUris.first()
                    .toString()
            ) {
                initializePlayer(videoUris, startPositionIndex)
            } else {
                player?.seekTo(startPositionIndex, viewModel.playbackPosition)
            }
        }

        // Handle the cancel button click - simply hide the playback layout
        binding?.playbacklayout?.cancel?.setOnClickListener {
            binding?.playbacklayout?.playbackmain?.visibility = View.GONE
        }
    }

    private fun continueOnBackPressed() {
        super.onBackPressed()
        backFromPlayer = true
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null) {
            val uri = intent.action
            if (uri != null) {
                videolistglobal = emptyList()
                handleActionSend(intent)
                isFromLaunchers = true
            } else {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri != null) {
                    videolistglobal = emptyList()
                    handleActionSend(intent)
                    isFromLaunchers = true
                }
            }
        }
    }

    fun backfunctionality() {
        if (isPlayerBackHandling) return

        if (isLock) {
            com.video.avd.utils.ToastUtils.showToast(
                this@PlayerVideoActivity,
                getString(R.string.player_is_currently_in_locked_mode)
            )
            return
        }

        isPlayerBackHandling = true

        val ad = interHome

        if (ad != null) {
            showInterstitialPlayer(
                forFragment = false,
                interstitialAd = ad,
                activity = this,
                onDismissed = {
                    performPlayerBackActionSafely()
                },
                enable = inter_videos
            )
        } else {
            loadFallbackInterstitialAd(
                this,
                BuildConfig.inter_home_high,
                BuildConfig.inter_home,
                inter_home_high,
                inter_home_normal,
                {
                    interHome = it
                },
                {
                    interHome = it
                }
            )

            performPlayerBackActionSafely()
        }
    }

    private fun performPlayerBackActionSafely() {
        try {
            performPlayerBackAction()
        } finally {
            isPlayerBackHandling = false
        }
    }

    private fun performPlayerBackAction() {
        when {
            hdrOptionDialog?.isShowing == true -> {
                hdrOptionDialog?.dismiss()
            }

            viewModel.isSheetbakpress -> {
                viewModel.isSheetbakpress = false
                continueOnBackPressed()
            }

            viewModel.icBgAudioClicked -> {
                continueOnBackPressed()
            }

            isFirst -> {
                isFirst = false
                isbackfromplayer = true
                continueOnBackPressed()
            }

            else -> {
                backFromPlayer = true
                continueOnBackPressed()
            }
        }
    }

    fun setBottomMargin(view: View, bottomDp: Float, context: Context) {
        val params = view.layoutParams as? MarginLayoutParams
        params?.let {
            val bottomPx = (bottomDp * context.resources.displayMetrics.density).toInt()
            it.bottomMargin = bottomPx
            view.layoutParams = it
        }
    }

    fun shownativeAd() {
        binding?.nativeClose?.setOnClickListener {
            hideNativeAd()
            binding?.nativeClose?.visibility = View.GONE
        }
        try {
            this@PlayerVideoActivity.let {
                if (!it.isFinishing) {
                    binding?.root?.let { it1 ->
                        when (AdsManager.adSdkChoice) {
                            "admob" -> {
                                AdsManager.refreshAdVideo(
                                    it1, it, true, this@PlayerVideoActivity
                                )
                            }

                            "applovin" -> {
                                binding?.flAdplace?.let { it2 ->
                                    AppLovinAdUtils.loadNativeAd(
                                        this@PlayerVideoActivity,
                                        it2, binding?.nativeClose
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideNativeAd() {
        binding?.flAdplace?.visibility = View.GONE
        binding?.nativeClose?.visibility = View.GONE
    }

    fun getVideoBookmarks() {
        lifecycleScope.launch {
            val bookmarkPositions = ArrayList<Long>()
            viewModel.getVideoBookmarksByUri(player?.currentMediaItem?.localConfiguration?.uri.toString())
                ?.collectLatest { list ->
                    bookmarkPositions.clear()
                    list.forEach {
                        bookmarkPositions.add(it.position)
                    }
                    val duration = player?.duration
                    exoBar?.setBookmarkPositions(bookmarkPositions, duration ?: 1000)
                }
        }
    }

    private fun removeHDRFilter(textureView: TextureView) {
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun showHdrOptionDialog() {
        if (hdrOptionDialog == null) {
            hdrOptionDialog = viewModel.getDialogue(this)
        }
        hdrOptionDialog?.window?.setDimAmount(0.8f)
        val closeBtn = hdrOptionDialog?.findViewById<ImageView>(R.id.close_dg)
        val watchVideo = hdrOptionDialog?.findViewById<ImageView>(R.id.watch_button)
        val goPremium = hdrOptionDialog?.findViewById<TextView>(R.id.goPro)
        closeBtn?.setOnClickListener {
            hdrOptionDialog?.dismiss()
        }
        watchVideo?.setOnClickListener {
            when (AdsManager.adSdkChoice) {
                "admob" -> {
                    AdsManager.showRewardedVideo(
                        this,
                        this,
                        this, this
                    )
                    hdrOptionDialog?.dismiss()
                }

                "applovin" -> {
                    AdsManager.showRewardedVideoAppLovin(
                        context = this,
                        activity = this,
                        onUserEarnedRewardListener = {
                            // Reward the user
                            Log.d("AppLovin", "User earned reward!")
                        },
                        rewardAdDismissListener = {
                            // Handle ad dismissal
                            Log.d("AppLovin", "Ad dismissed!")
                        }
                    )
                    hdrOptionDialog?.dismiss()
                }
            }

        }
        goPremium?.setOnClickListener {
            startActivity(Intent(this@PlayerVideoActivity, InAppActivity::class.java))
            // hdrOptionDialog?.dismiss()
        }
        hdrOptionDialog?.show()

    }

    override fun onUserEarnedReward(p0: RewardItem) {

    }

    override fun onDismissRewardAd() {
        applyHDREffect()
    }

    /**
     * Optimizes rendering performance to reduce GPU load and prevent rendering ANRs.
     * This function applies various optimizations to reduce the rendering bottleneck.
     */
    private fun optimizeRenderingPerformance() {
        try {
            // Optimize PlayerView rendering
            binding?.videoView?.let { playerView ->
                // Enable hardware acceleration if not already enabled
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    playerView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }
                
                // Optimize video surface view if it's a TextureView
                playerView.videoSurfaceView?.let { surfaceView ->
                    if (surfaceView is TextureView) {
                        // Set surface texture listener to optimize rendering
                        surfaceView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                // Surface is ready, rendering can proceed
                            }
                            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                // Handle size changes efficiently
                            }
                            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                                // Return true to release the surface texture
                                return true
                            }
                            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                                // Called every frame - keep this lightweight
                            }
                        }
                    }
                }
            }
            
            // Optimize root view to reduce overdraw
            binding?.root?.let { rootView ->
                // Enable clipping to prevent drawing outside bounds
                rootView.clipToPadding = true
                rootView.clipChildren = true
            }
            
            // Optimize controller views
            binding?.videoPlayerFeaturesIconsLayout?.let { layout ->
                // Use hardware layer for complex layouts that don't change often
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    layout.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }
            }
            
        } catch (e: Exception) {
            Log.e("PlayerVideoActivity", "Error optimizing rendering performance", e)
        }
    }

    private fun applyHDREffect() {
        binding?.hdrAnim?.visibility = View.VISIBLE
        binding?.hdrAnim?.playAnimation()
        lifecycleScope.launch {
            delay(2000)
            isHDREnabled = true
            viewModel.applyHDRFilter(binding?.videoView?.videoSurfaceView as TextureView)
            Toast.makeText(this@PlayerVideoActivity, "HDR Enabled", Toast.LENGTH_SHORT).show()
            binding?.hdrquality?.let { imageView ->
                imageView.setImageResource(R.drawable.ic_hdr_selected)
            }
            withContext(Dispatchers.Main) {
                binding?.hdrAnim?.visibility = View.GONE
                binding?.hdrAnim?.cancelAnimation()
            }
        }
    }

}

private fun dismissAllDialogs(context: Context) {
    try {
        // Ensure the context is a FragmentActivity to access the FragmentManager
        if (context is FragmentActivity) {
            val fragmentManager = context.supportFragmentManager

            // Iterate through all fragments
            for (fragment in fragmentManager.fragments) {
                if (fragment is DialogFragment && fragment.isVisible()) {
                    (fragment as DialogFragment).dismissAllowingStateLoss()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

interface RewardAdDismissListener {
    fun onDismissRewardAd()
}

