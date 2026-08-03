package com.video.avd.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.findNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.avd.browserkit.BrowserKitInitializer
import com.avd.browserkit.api.BrowserKit
import com.video.avd.downloader.BrowserKitBridge
import com.avd.ui.main.progress.ProgressViewModel
import com.avd.DynamicModuleDownloader
import com.avd.ui.dialog.DownloadDialogManager
import com.avd.ui.main.downloder_queue.ui.main.DownloadListPagerAdapter
import com.avd.ui.main.home.MainViewModel
import com.avd.ui.main.home.downloadapi.ApiViewModel
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.banner_home_enable
import com.avd.util.AdBlockerHelper.fromBrowser
import com.avd.util.AdBlockerHelper.hideLoading
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.isCooldownOver
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.maxPopupAdImpressions
import com.avd.util.AdBlockerHelper.remotePopupAdImpressions
import com.avd.util.AdBlockerHelper.remotemaxAdImpressions
import com.avd.util.AdBlockerHelper.saveCurrentTime
import com.avd.util.AdBlockerHelper.showExitScreen
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.CommunicateWithActivity
import com.avd.util.RemoteConfigHelper
import com.avd.util.ads.InterstitialManagerA.loadHomeInterstitialAd
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.singular.sdk.Singular
import com.singular.sdk.SingularConfig
import com.video.avd.BuildConfig
import com.video.avd.BuildConfig.banner_home
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.ads.AdsManager.exit_interstitial
import com.video.avd.constent.backFromPlayer
import com.video.avd.constent.is2Adwatched
import com.video.avd.constent.isFirstTime
import com.video.avd.constent.isSplash
import com.video.avd.constent.isUserAdSeen
import com.video.avd.constent.isbackfromplayer
import com.video.avd.constent.singularinitialze
import com.video.avd.constent.splashAdClick
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.ActivityMainBinding
import com.video.avd.net.NetworkChangeReceiver
import com.video.avd.net.NetworkStateListener
import com.video.avd.ads.AdsManagerKit
import com.video.avd.ads.isShowingAd
import com.video.avd.ui.exitadactivity.ExitActivity
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.hideNavigationBar
import com.video.avd.utils.AppUtils.startTimeMillis
import com.video.avd.utils.AppUtils.totalForegroundTimeMillis
import com.video.avd.utils.AppUtils.wasRunning
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.GlobalValues.fromSplash
import com.video.avd.utils.GlobalValues.is24hourEnabled
import com.video.avd.utils.GlobalValues.link
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.SharedPreferencesManager
import com.video.avd.utils.SimpleDownloadDialog
import com.video.avd.utils.newvideo_receiver.VideoCheckJobService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Typeface
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NetworkStateListener, CommunicateWithActivity {

    var binding: ActivityMainBinding? = null

    val viewModel: MainActivityViewModel by viewModels()

    val mainViewModel: MainViewModel by viewModels()
    val progressViewModel: ProgressViewModel by viewModels()

    // Broadcast receiver for download completion dialog
    private var downloadCompleteReceiver: BroadcastReceiver? = null
    private var isFromHomeScreen = false
    private var lastClickTime = 0L
    private val CLICK_DELAY = 1000L
    private val apiviewModel: ApiViewModel by viewModels()
    var isAllow = true
    private lateinit var dynamicModuleDownloader: DynamicModuleDownloader
    var isPipMode = false
    private var showThemesFragment = false
    private val bottomNavItemViews = mutableMapOf<Int, LinearLayout>()
    private val bottomNavIconViews = mutableMapOf<Int, ImageView>()
    private val bottomNavLabelViews = mutableMapOf<Int, TextView>()
    private var selectedBottomNavItemId = R.id.fragmentDownloadQueue
    var navController: NavController? = null
    private var navHostFragment: NavHostFragment? = null
    private val networkChangeReceiver: NetworkChangeReceiver by lazy {
        NetworkChangeReceiver()
    }

    var lang: String? = null
    var isNavDownload = false
    var currenttheme = ""
    var sharedPreferencesManager: SharedPreferencesManager? = null
    lateinit var clipboardManager: ClipboardManager
    private var previousSelectedMenu = "downloads" //Home Video Menu
    private val reviewManager by lazy {
        ReviewManagerFactory.create(application)
    }

    fun getlantype() {
        if (AppPreference.getLanguage(this) != null) {
            lang = AppPreference.getLanguage(this).toString()
        }
    }


    private fun downloadOrLaunchDynamicModule(activity: FragmentActivity) {
        dynamicModuleDownloader =
            DynamicModuleDownloader(activity = activity, moduleName = "youtubedldynamic")
        lifecycleScope.launch(Dispatchers.IO) {
            dynamicModuleDownloader.installOrLaunchModule {
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching {
                        BrowserKitInitializer.initializeAwait(applicationContext)
                        Log.d("BrowserKitInit", "MainActivity ready=${BrowserKitInitializer.isInitialized()}")
                    }.onFailure {
                        Log.e("BrowserKitInit", "MainActivity init failed", it)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getlantype()
        AppUtils.setLocate(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        Log.d("checkBaseUrl","end point: ${RemoteConfigHelper.getSocialDownloaderEndpoint()}")
        fromSplash=false
        link=RemoteConfigHelper.getSocialDownloaderEndpoint()
        binding?.main?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        hideNavigationBar()
        loadFallbackInterstitialAd(this, BuildConfig.inter_home_high, BuildConfig.inter_home,inter_home_high,inter_home_normal,{
            interHome=it
        },{
            interHome=it
        })
        isFirstTime = false
        sharedPreferencesManager = SharedPreferencesManager(this)
        isSplash = false
        clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        AppUtils.firebaseUserAction("onCreate_MainActivity", "MainActivity")
        startTimeMillis = SystemClock.elapsedRealtime()
        showThemesFragment = intent.getBooleanExtra("showThemesFragment", false)
        AppPreference.isFromSplash = false
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHostFragment?.navController
        setupBottomNavigationBar()

        // BrowserKit is initialized from Application to match the source app flow.

        navController?.let { controller ->
          /*  controller.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.homeFragment1 -> {
                        if (!isNavDownload) {
                            interHome?.let {
                                showInterstitial(false,it,this,{
                                    hideLoading()
                                },inter_home)
                            }
                            previousSelectedMenu = "Home"
                        }
                        loadBanner()
                        isNavDownload = false
                    }
                    R.id.fragmentDownloadHistory -> {
                        if (!isNavDownload) {
                            interHome?.let {
                                showInterstitial(false,it,this,{
                                    hideLoading()
                                },inter_home)
                            }
                            loadBanner()
                            previousSelectedMenu = "history"
                        }
                        isNavDownload = false
                    }

                    R.id.mainDownloaderFragment -> {
                        fromBrowser=false
                        if (!isNavDownload) {
                            // showInterstitialHome(activity = this@MainActivity) {}
                            interHome?.let {
                                showInterstitial(false,it,this,{
                                    hideLoading()
                                },inter_home)
                            }
                            loadBanner()
                            previousSelectedMenu = "downloads"
                        }
                        isNavDownload = false
                    }

                    R.id.fragmentDownloadQueue -> {
                        fromBrowser=true
                        if (!isNavDownload) {
                            //   showInterstitialHome(activity = this@MainActivity) {}
                            interHome?.let {
                                showInterstitial(false,it,this,{
                                    hideLoading()
                                },inter_home)
                            }
                            loadBanner()
                            previousSelectedMenu = "fragmentDownloadQueue"
                        }
                        isNavDownload = false
                    }
                }
            }*/
            controller.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.homeFragment1 -> {
                        if (!isNavDownload) {
                           /* interHome?.let {
                                showInterstitial(false, it, this, {
                                    hideLoading()
                                }, inter_home)
                            }*/
                            previousSelectedMenu = "Home"
                        }
                        loadBanner()
                        isNavDownload = false
                    }
                    R.id.fragmentDownloadHistory -> {
                        if (!isNavDownload) {
                            /*interHome?.let {
                                showInterstitial(false, it, this, {
                                    hideLoading()
                                }, inter_home)
                            }*/
                            loadBanner()
                            previousSelectedMenu = "history"
                        }
                        isNavDownload = false
                    }

                    R.id.mainDownloaderFragment -> {
                        fromBrowser = false
                        if (!isNavDownload) {
                          /*  interHome?.let {
                                showInterstitial(false, it, this, {
                                    hideLoading()
                                }, inter_home)
                            }*/
                            loadBanner()
                            previousSelectedMenu = "downloads"
                        }
                        isNavDownload = false
                    }

                    R.id.fragmentDownloadQueue -> {
                        fromBrowser = true
                        if (!isNavDownload) {
                           /* interHome?.let {
                                showInterstitial(false, it, this, {
                                    hideLoading()
                                }, inter_home)
                            }*/
                            loadBanner()
                            previousSelectedMenu = "fragmentDownloadQueue"
                        }
                        isNavDownload = false
                    }
                }
                updateBottomNavigationSelection(destination.id)
            }
            updateBottomNavigationSelection(controller.currentDestination?.id ?: R.id.fragmentDownloadQueue)
        }

        downloadOrLaunchDynamicModule(this)


//      Engagement
    /*    FirebaseMessaging.getInstance().subscribeToTopic("downloader")
            .addOnCompleteListener { task ->
                var msg = "Subscription successful"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("MYTAG", msg)
            }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.e("TokenFireBase", token)
            AppUtils.registerTokenToSingular(token)
        }
        )
*/


        GlobalValues.hidePopupPlayer.observe(this) {
            if (it == true) {
                releaseMediaSession()
            }
        }

        AdBlockerHelper.isProVersion.observe(this) {
            it?.let {
                if (it) {
                    binding?.adContainer?.visibility = View.GONE
                }
                else{
                    binding?.adContainer?.visibility = View.VISIBLE
                }
            }
        }
        val notifydata = intent.getStringExtra("fragment")
        val url = intent.getStringExtra("url")
        val youtube = intent.getStringExtra("youtubelink")
        if (notifydata != null || youtube != null || url != null) {
            if (shouldProcessIntent) {
                if (intent != null) {
                    handleNotification(intent)
                }
            }
        }

        handleSharedContent(intent)

        // Schedule the video check work, but first check if it's the first launch
        if (isFirstLaunchwork()) {
            markFirstLaunchCompleted()
            scheduleVideoCheckJob(this)
        } else {
            if (isFirstRegister()) {
                markFirstRegister()
            }
        }

        // Reset download dialog session on app start
        resetDownloadDialogSession()
        // Register download complete receiver
        registerDownloadCompleteReceiver()



        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val current = navController?.currentDestination?.id
                    if (current != R.id.mainDownloaderFragment) {
                        fromBrowser=false
                        navController?.navigate(R.id.mainDownloaderFragment)
                    }
                }
            }
        )

        showExitScreen = {
            Log.d("exitTag", "onCreate: $exit_interstitial")
            val exitIntent = Intent(this@MainActivity, ExitActivity::class.java)
            if (exit_interstitial) {
                if(interHome!=null) {
                    showInterstitial(false, interHome!!, this, {
                        startActivity(exitIntent)
                    }, exit_interstitial)
                }
                else{
                    startActivity(exitIntent)
                }
            } else {
                startActivity(exitIntent)
            }
        }


        binding?.root
    }

    private fun isSafeClick(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < CLICK_DELAY) return false
        lastClickTime = now
        return true
    }
    @SuppressLint("SuspiciousIndentation")
    fun showDialogueonlink() {
        if (::clipboardManager.isInitialized) {
            val hasPrimaryClip = clipboardManager.hasPrimaryClip()
            val primaryClip = clipboardManager.primaryClip
            if (hasPrimaryClip && primaryClip != null) {
                val clipData: ClipData = primaryClip
                val item = clipData.getItemAt(0)
                val clipText = item.text
                if (clipText != null && isLikelyALink(clipText.toString())) {
                    if (viewModel.lastdetctedlink.isNotEmpty() && viewModel.lastdetctedlink == clipText.toString()) {
                        return
                    }
                    viewModel.lastdetctedlink = clipText.toString()

                    apiviewModel.texturl.value = clipText.toString()
                    sharedPreferencesManager?.setLastLink(viewModel.lastdetctedlink)
                } else {
                    if (clipText != null) {
                        apiviewModel.texturl.value = clipText.toString()
                    }
                }
            } else {
            }
        } else {
        }
    }

    private fun isLikelyALink(text: String): Boolean {
        // A basic check to see if text looks like a URL
        val regex = Regex(
            pattern = """^https?://(?:www\.)?(twitter\.com|t\.co|x\.com|facebook\.com|fb\.com|fb\.watch|m\.facebook\.com|instagram\.com|instagr\.am|tiktok\.com|vm\.tiktok\.com|vt\.tiktok\.com|dailymotion\.com|imdb\.com|vimeo\.com)(?:/.*)?$""",
            option = RegexOption.IGNORE_CASE
        )
        return regex.containsMatchIn(text.trim())
    }

    private fun releaseMediaSession() {
        try {
            binding?.bottomPopupContainer?.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        try {
            dynamicModuleDownloader.registerListener()
            if (splashAdClick) {
                splashAdClick = false
                AppUtils.firebaseUserAction("SplashAdClickResumeApp", "MainActivity")
            }
            AppUtils.setLocate(this)
            // Reset app open ad blocker flag after a delay when returning from external links
            // This allows app open ads to show again after user returns from Privacy Policy/Terms/Rate Us
            // Using a longer delay (5 seconds) to ensure the blocking period has passed and normal behavior resumes
            // Check flag at the START of onResume to prevent app open ad from showing immediately
            val wasFlagSet = AdBlockerHelper.getinterstitialshown()
            if (wasFlagSet) {
                Log.d("AppOpenAd", "App resumed with ad blocker flag set - app open ad will be blocked")
                // Reset after delay to allow normal app open ad flow to resume
                lifecycleScope.launch {
                    delay(5000) // Reset after 5 seconds to ensure normal app open ad flow resumes
                    if (AdBlockerHelper.getinterstitialshown()) {
                        AdBlockerHelper.setinterstitialshown(false)
                        Log.d("AppOpenAd", "App open ad blocker flag reset - normal behavior resumed")
                    }
                }
            }
            
            if (isbackfromplayer) {
                isbackfromplayer = false
                if (lastplayed == 0) {
                    lastplayed = 1
                    navController?.navigate(R.id.rateUs)
                    Log.e("lastplayed", "$lastplayed")
                } else if (lastplayed % 10 == 0) {
                    Log.e("lastplayed", "$lastplayed")
                    navController?.navigate(R.id.rateUs)
                }
            } else if (backFromPlayer) {
                backFromPlayer = false
                if (is2Adwatched >= 2) {
                    if (is24hourEnabled.value == false && AdBlockerHelper.isProVersion.value != true) {
                        val lastShowTime = AppPreference.get30MinutesShownTime(this)
                        val currentTime = System.currentTimeMillis()
                        if (lastShowTime != currentTime) {
                            if (currentTime - lastShowTime > 24 * 60 * 60 * 1000) {
                                navController?.navigate(R.id.action_global_halfHourAdFreeFragment)
                            }
                        } else {
                            navController?.navigate(R.id.action_global_halfHourAdFreeFragment)
                        }
                    }
                }
            }
            AppUtils.setLocate(this)
            if (wasRunning) {
                val resumeTime = SystemClock.elapsedRealtime()
                totalForegroundTimeMillis += resumeTime - startTimeMillis
            }
            if (isAllow) {
                isShowingAd=true
                if (intent.action != Intent.ACTION_SEND) {
                    window.decorView.postDelayed({
                        hideNavigationBar()
                        viewModel.lastdetctedlink =
                            sharedPreferencesManager?.getLastDetected().toString()
                        showDialogueonlink()
                    }, 500) // Delay 500ms
                }
                lifecycleScope.launch(Dispatchers.Main){
                    delay(1500)
                    isShowingAd=false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun loadBanner() {
        if(banner_home_enable) {
            binding?.bottomAd?.visibility=View.VISIBLE
            binding?.adContainer?.let { container ->
                /*binding?.adViewLayout?.let {
                AdsManager.loadBanner(this, it, container)
            }*/
                AdsManagerKit.loadBanner(
                    activity = this,
                    highFloorAdId = BuildConfig.banner_home_high,
                    normalAdId = BuildConfig.banner_home,
                    showHighFloor = banner_home_enable,
                    showNormalFloor = banner_home_enable,
                    onLoaded = {},
                    adContainer = container
                )

            }
        }
        else{
           binding?.bottomAd?.visibility=View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleSharedContent(intent)
            handleNotification(intent)
        }
    }

    fun playVideowithurl(url: String) {
        var urlne = convertHttpToHttps(url)
        val result = Bundle()
        result.putString("id", "1")
        result.putBoolean("isliveuri", true)
        result.putString("uri", urlne)
        result.putBoolean("isYouTube", false)
        result.putString("fragmentName", "Streaming")
        val intent = Intent(this, PlayerVideoActivity::class.java)
        intent.putExtras(result)
        startActivity(intent)
    }

    fun convertHttpToHttps(url: String): String {
        if (url.startsWith("http://")) {
            return url.replaceFirst("http://", "https://")
        }
        return url
    }

    fun isValidExoPlayerUrl(url: String): Boolean {
        val pattern = "^https?://(?:[a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}(?:/[^\\s]*)?$"
        val regex = Regex(pattern)
        return regex.matches(url)
    }

    private fun handleNotification(intent: Intent) {
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val scheme: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (isValidHttpsUrl(scheme)) {
                    // This is a link intended for browsing
//                    try {
//                        val intent = Intent(this, MainActivityDownloader::class.java)
//                        intent.putExtra("linkbrowse", scheme.toString())
//                        startActivity(intent)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
                }
            }
        }
        val url = intent.getStringExtra("url")
        if (url != null) {
            Log.e("Notifiiim", "$url")
            playVideowithurl(url)
        }
        when (intent.getStringExtra("fragment")) {
            "home" -> displayHomeFragment()
            "music" -> displayMusicFragment()
            "download" -> displayDownloadFragment()
            "status_saver" -> displayStatusSaverFragment()
            else -> {
                intent.getStringExtra("youtubelink")?.let {
                    openYouTubeLink(it)
                    Log.d("youtubelink", it.toString())
                }
            }
        }
    }

    private fun displayHomeFragment() {
        // logic to display Home Fragment
        AppUtils.firebaseUserAction("home_Notification", "MainActivity")
    }

    private fun displayMusicFragment() {
//        AppUtils.firebaseUserAction("music_Notification", "MainActivity")
//        binding?.bottomNavigation?.selectedItemId = R.id.homeAudioFragment
    }

    private fun displayDownloadFragment() {
//        AppUtils.firebaseUserAction("download_Notification", "MainActivity")
//        val intent = Intent(this, MainActivityDownloader::class.java)
//        startActivity(intent)
    }

    private fun displayStatusSaverFragment() {
        // logic to display Status Saver Fragment
        AppUtils.firebaseUserAction("statussaver_Notification", "MainActivity")
        navHostFragment?.findNavController()?.navigate(R.id.action_global_statusSaverFragment)
    }

    private fun openYouTubeLink(url: String) {
//        try {
//            AppUtils.firebaseUserAction("youtube_Notification", "MainActivity")
//            AppUtils.getMain(this).hideBannerAd()
//            val intent = Intent(this, MainActivityDownloader::class.java)
//            intent.putExtra("url", url)
//            intent.putExtra("isLink", true)
//            startActivity(intent)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    fun isValidHttpsUrl(url: String?): Boolean {
        return try {
            val uri = URI(url)
            "https" == uri.scheme && uri.host != null
        } catch (e: URISyntaxException) {
            false
        }
    }

    companion object {
        var shouldProcessIntent = true
        var propanelbottom = false
        var lastplayed = 0
    }

    private data class BottomNavItem(
        val id: Int,
        val iconRes: Int,
        val titleRes: Int
    )

    private val bottomNavItems = listOf(
        BottomNavItem(R.id.fragmentDownloadQueue, R.drawable.bottom_web, R.string.bottom_browser),
        BottomNavItem(R.id.mainDownloaderFragment, com.avd.R.drawable.ic_socialapps, R.string.home),
        BottomNavItem(R.id.fragmentDownloadHistory, R.drawable.bottom_progress, R.string.progress),
        BottomNavItem(R.id.homeFragment1, R.drawable.bottom_video, R.string.bottom_video_player)
    )

    private fun setupBottomNavigationBar() {
        val bottomNavigation = binding?.bottomNavigation ?: return
        bottomNavItemViews.clear()
        bottomNavIconViews.clear()
        bottomNavLabelViews.clear()

        bindBottomNavItem(
            R.id.fragmentDownloadQueue,
            bottomNavigation.findViewById(R.id.fragmentDownloadQueue),
            bottomNavigation.findViewById(R.id.ivBottomWeb),
            bottomNavigation.findViewById(R.id.tvBottomWeb)
        )
        bindBottomNavItem(
            R.id.mainDownloaderFragment,
            bottomNavigation.findViewById(R.id.mainDownloaderFragment),
            bottomNavigation.findViewById(R.id.ivBottomHome),
            bottomNavigation.findViewById(R.id.tvBottomHome)
        )
        bindBottomNavItem(
            R.id.fragmentDownloadHistory,
            bottomNavigation.findViewById(R.id.fragmentDownloadHistory),
            bottomNavigation.findViewById(R.id.ivBottomProgress),
            bottomNavigation.findViewById(R.id.tvBottomProgress)
        )
        bindBottomNavItem(
            R.id.homeFragment1,
            bottomNavigation.findViewById(R.id.homeFragment1),
            bottomNavigation.findViewById(R.id.ivBottomPlayer),
            bottomNavigation.findViewById(R.id.tvBottomPlayer)
        )
        updateBottomNavigationSelection(selectedBottomNavItemId)
    }

    private fun bindBottomNavItem(
        itemId: Int,
        itemView: LinearLayout?,
        icon: ImageView?,
        label: TextView?
    ) {
        if (itemView == null || icon == null || label == null) return
        val item = bottomNavItems.firstOrNull { it.id == itemId } ?: return
        itemView.gravity = Gravity.CENTER
        itemView.orientation = LinearLayout.VERTICAL
        itemView.setBaselineAligned(false)
        itemView.contentDescription = getString(item.titleRes)
        itemView.setOnClickListener { handleBottomNavigationItemClick(itemId) }
        icon.setImageDrawable(tintedBottomNavIcon(item.iconRes, false))
        label.typeface = ResourcesCompat.getFont(this, R.font.poppins_light) ?: Typeface.DEFAULT
        label.visibility = View.VISIBLE
        label.alpha = 1f
        bottomNavItemViews[itemId] = itemView
        bottomNavIconViews[itemId] = icon
        bottomNavLabelViews[itemId] = label
    }

    private fun handleBottomNavigationItemClick(itemId: Int): Boolean {
        if (navController?.currentDestination?.id == itemId) {
            updateBottomNavigationSelection(itemId)
            return true
        }

        if (!isSafeClick()) {
            Log.d("safeClick", "blocked")
            return false
        }
        updateBottomNavigationSelection(itemId)
        navigateToBottomDestination(itemId)
        when (itemId) {
            R.id.homeFragment1,
            R.id.fragmentDownloadHistory,
            R.id.mainDownloaderFragment,
            R.id.fragmentDownloadQueue -> {
                if (interHome != null) {
                    showInterstitial(false, interHome!!, this, {
                        hideLoading()
                    }, inter_home)
                }
            }
        }
        return true
    }

    private fun navigateToBottomDestination(itemId: Int) {
        val controller = navController ?: return
        val options = navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(controller.graph.findStartDestination().id) {
                saveState = true
            }
        }
        controller.navigate(itemId, null, options)
    }

    private fun updateBottomNavigationSelection(itemId: Int) {
        if (bottomNavItems.none { it.id == itemId }) return
        selectedBottomNavItemId = itemId
        bottomNavItems.forEach { item ->
            val isSelected = item.id == itemId
            val itemView = bottomNavItemViews[item.id] ?: return@forEach
            val icon = bottomNavIconViews[item.id] ?: return@forEach
            val label = bottomNavLabelViews[item.id] ?: return@forEach
            itemView.isSelected = isSelected
            itemView.background = null
            icon.setImageDrawable(tintedBottomNavIcon(item.iconRes, isSelected))
            label.setTextColor(bottomNavColor(isSelected))
            label.visibility = View.VISIBLE
            label.animate().cancel()
            label.alpha = 1f
            label.isEnabled = true
        }
    }

    private fun selectBottomNavigationItem(itemId: Int) {
        handleBottomNavigationItemClick(itemId)
    }

    private fun tintedBottomNavIcon(iconRes: Int, isSelected: Boolean) =
        AppCompatResources.getDrawable(this, iconRes)?.mutate()?.let { drawable ->
            DrawableCompat.wrap(drawable).also {
                DrawableCompat.setTint(it, bottomNavColor(isSelected))
            }
        }

    private fun bottomNavColor(isSelected: Boolean): Int {
        val colorRes = if (isSelected) R.color.bottom_nav_selected else R.color.nonSelectedColor
        return ContextCompat.getColor(this, colorRes)
    }

    fun hidebottombar() {
        binding?.bottomNavigation?.visibility = View.GONE
    }

    fun showbottombar() {
        binding?.bottomNavigation?.visibility = View.VISIBLE
    }

    override fun onNetworkStateChanged(isOnline: Boolean) {
        if (isOnline) {
            loadBanner()
            loadHomeInterstitialAd(this)
        }
        if (!singularinitialze) {
            initsingularsdk()
        }
    }

    private fun initsingularsdk() {
        if (NetworkUtils.isOnline(this)) {
            singularinitialze = true
            CoroutineScope(Dispatchers.IO).launch {
                // Create a configuration object
                val config = SingularConfig(
                    "terafort_new_94135a02",
                    "387f4c401b63a8ba72835bdedeb6a91b"
                ).withLoggingEnabled().withLogLevel(1)
                // Set up a deep links handler
                config.withSingularLink(
                    intent
                ) { params ->
                    val deeplink = params.deeplink
                    val passthrough = params.passthrough
                    val isDeferred = params.isDeferred
                    // Add deep link handling code here
                }
                Singular.init(this@MainActivity, config)
                Singular.event("Main Activity called")
            }
        }
    }

    fun hideBannerAd() {
        binding?.adContainer?.visibility = View.GONE
    }

    fun showBannerAd() {
        if (AdBlockerHelper.isProVersion.value != true) {
            if (is24hourEnabled.value == false) {
                binding?.adContainer?.visibility = View.VISIBLE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isAllow = true
        dynamicModuleDownloader.unregisterListener()
        if (AdsManager.adView != null) {
            AdsManager.adView?.pause()
        }
        // If flag is already set (from opening external links), ensure it persists through pause/resume
        // This prevents app open ads when returning from external apps like Play Store, browser, etc.
        if (AdBlockerHelper.getinterstitialshown()) {
            // Flag is already set, it will persist through onResume
            Log.d("AppOpenAd", "App paused with ad blocker flag set - will prevent app open ad on resume")
        }
    }

    override fun onStart() {
        super.onStart()
        networkChangeReceiver.setNetworkStateListener(this)
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.registerReceiver(networkChangeReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(networkChangeReceiver, intentFilter)
        }
    }

    /**
     * Register broadcast receiver for download completion dialog
     */
    private fun registerDownloadCompleteReceiver() {
        Log.d("TESTdialogue", "Registering download complete receiver...")
        downloadCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("TESTdialogue", "Broadcast received: ${intent.action}")
                if (intent.action == "DOWNLOAD_COMPLETE") {
                    Log.d("TESTdialogue", "DOWNLOAD_COMPLETE broadcast received!")
                    Log.d(
                        "TESTdialogue",
                        "Activity state - isFinishing: $isFinishing, isDestroyed: $isDestroyed"
                    )

                    // Show dialog if app is running (anywhere in downloader)
                    if (!isFinishing && !isDestroyed) {
                        val completionInfo = com.avd.ui.dialog.DownloadCompletionInfo.fromIntentExtras(intent.extras)
                        Log.d("TESTdialogue", "Showing download completion dialog for: ${completionInfo.videoTitle}")

                        SimpleDownloadDialog.showOnDownloadComplete(
                            this@MainActivity,
                            completionInfo,
                            isFromHomeScreen,
                            onDownloadMore = {
                                isNavDownload = true
                                if (maxPopupAdImpressions < remotePopupAdImpressions) {
                                    showInterstitialHome(
                                        forFragment = true,
                                        activity = this@MainActivity
                                    ) {
                                        maxPopupAdImpressions++
                                        val currentDest = navController?.currentDestination?.id
                                        if (currentDest != R.id.mainDownloaderFragment) {
                                            fromBrowser = false
                                            navController?.navigate(R.id.mainDownloaderFragment)
                                        }
                                    }
                                } else {
                                    val currentDest = navController?.currentDestination?.id
                                    if (currentDest != R.id.mainDownloaderFragment) {
                                        fromBrowser = false
                                        navController?.navigate(R.id.mainDownloaderFragment)
                                    }
                                }

                            },
                            onGoToDownloads = {
                                isNavDownload = true
                                if (maxPopupAdImpressions < remotePopupAdImpressions) {
                                    showInterstitialHome(
                                        forFragment = true,
                                        activity = this@MainActivity
                                    ) {
                                        maxPopupAdImpressions++
                                        DownloadDialogManager.defaultTabPos =
                                            DownloadListPagerAdapter.COMPLETED_FRAG_POS
                                        selectBottomNavigationItem(R.id.fragmentDownloadHistory)
                                        Log.d(
                                            "FragmentDownloadQueue",
                                            "Resumed → switched to tab: ${DownloadDialogManager.defaultTabPos}"
                                        )
                                    }
                                } else {
                                    DownloadDialogManager.defaultTabPos =
                                        DownloadListPagerAdapter.COMPLETED_FRAG_POS
                                    selectBottomNavigationItem(R.id.fragmentDownloadHistory)
                                }
                            }

                        )
                    } else {
                        Log.d(
                            "TESTdialogue",
                            "Dialog not shown - activity is finishing or destroyed"
                        )
                    }
                }
            }
        }

        // Register the receiver with RECEIVER_NOT_EXPORTED for security
        val filter = IntentFilter("DOWNLOAD_COMPLETE")
        // Use ContextCompat for API level compatibility
        ContextCompat.registerReceiver(
            this,
            downloadCompleteReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d("TESTdialogue", "Receiver registered with RECEIVER_NOT_EXPORTED (compatible)")
    }

    /**
     * Reset dialog session - call this when app starts or when you want to allow dialog again
     */
    fun resetDownloadDialogSession() {
        SimpleDownloadDialog.resetDialogSession(this)
        isFromHomeScreen = false
        Log.d("TESTdialogue", "Download dialog session reset")
    }

    /**
     * Unregister broadcast receiver
     */
    private fun unregisterDownloadCompleteReceiver() {
        downloadCompleteReceiver?.let {
            unregisterReceiver(it)
            downloadCompleteReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the NetworkChangeReceiver when the activity is destroyed
        AdsManager.adView?.destroy()
        isUserAdSeen = 0
        is2Adwatched = 0
        wasRunning = true
        startTimeMillis = SystemClock.elapsedRealtime()
        videolistglobal = emptyList()
        unregisterReceiver(networkChangeReceiver)
        AdBlockerHelper.maxAdImpressions = 0
        GlobalValues.newProType = false

        // Unregister download complete receiver
        unregisterDownloadCompleteReceiver()
    }


    fun scheduleVideoCheckJob(context: Context) {
        // Creating a PeriodicWorkRequest for every 5 minutes
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        saveLastCheckTime(applicationContext, System.currentTimeMillis() / 1000)

        val videoCheckRequest = PeriodicWorkRequestBuilder<VideoCheckJobService>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Enqueue the work as unique. This way, even if this method is called multiple times, it won't schedule more than one instance of the work.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "videoCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,  // KEEP existing periodic work, REPLACE to cancel and reschedule
            videoCheckRequest
        )
    }

    // Utility function to check if it's the app's first launch
    fun isFirstLaunchwork(): Boolean {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("FirstLaunchwork", true)
    }

    fun isFirstRegister(): Boolean {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("FirstRegister", true)
    }

    fun markFirstRegister() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("FirstRegister", false)
            apply()
        }
    }

    // Utility function to mark first launch logic as completed
    fun markFirstLaunchCompleted() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("FirstLaunchwork", false)
            apply()
        }
    }

    fun saveLastCheckTime(context: Context, time: Long) {
        val sharedPreferences =
            context.getSharedPreferences("VideoCheckPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("LastCheckTime", time)
            apply()
        }
    }

    override fun hideBottomBar() {
        hidebottombar()
    }

    override fun showBottomBar() {
        showbottombar()
    }

    override fun setbottomseelection() {
        selectBottomNavigationItem(R.id.fragmentDownloadHistory)
    }

    override fun showBrowser() {
        selectBottomNavigationItem(R.id.fragmentDownloadQueue)
    }

    override fun showHome() {
        selectBottomNavigationItem(R.id.mainDownloaderFragment)
    }

    private fun handleSharedContent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            Log.d("ShowDialogueonlink", "hasPrimaryClip: from share url")
            // Check both possible extra keys
            isShowingAd=true
            lifecycleScope.launch(Dispatchers.Main){
                delay(1500)
                isShowingAd=false
            }
            isAllow = false
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra("android.intent.extra.TEXT")
            sharedText?.let { text ->
                val url = extractUrlFromText(text)
                if (viewModel.lastdetctedlink == url) {
                    return
                } else {
                }

                // Extract URL using more robust method
                if (isLikelyALink(url.toString())) {
                    if (url != null) {
                        /*if (isTikTokUrl(url)){
                            apiviewModel.downloadMediaTiktok(url)
                        }else {
                            apiviewModel.downloadMedia(url)
                        }*/
                        fromBrowser = false
                        navController?.navigate(R.id.mainDownloaderFragment)
                        selectBottomNavigationItem(R.id.mainDownloaderFragment)
                        viewModel.lastdetctedlink = url
                        apiviewModel.texturl.value = url.toString()
                        setIntent(Intent())
                        sharedPreferencesManager?.setLastLink(viewModel.lastdetctedlink)
                    } else {
                        Log.e("IntentHandler", "No URL found in shared text: $text")
                    }
                } else {
                    apiviewModel.texturl.value = url.toString()
                }
            } ?: run {
                Log.e("IntentHandler", "No text found in shared intent")
            }

        } else {
            Log.d("ShowDialogueonlink", "hasPrimaryClip: from clipboard url")
            window.decorView.postDelayed({
                hideNavigationBar()
                isShowingAd=true
                lifecycleScope.launch(Dispatchers.Main){
                    delay(1500)
                    isShowingAd=false
                }
                viewModel.lastdetctedlink = sharedPreferencesManager?.getLastDetected().toString()
                showDialogueonlink()
            }, 500) // Delay 500ms
        }
    }

    // Improved URL extraction
    private fun extractUrlFromText(text: String): String? {
        val urlPattern =
            Pattern.compile("(https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*)")
        val matcher = urlPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group()
        }
        // Second try: Look for common video domains
        val domains = listOf("tiktok.com", "youtube.com", "instagram.com", "twitter.com")
        for (domain in domains) {
            val index = text.indexOf(domain)
            if (index != -1) {
                // Extract from start of domain to next whitespace
                val start = text.lastIndexOf(' ', index).takeIf { it != -1 }?.plus(1) ?: 0
                val end = text.indexOf(' ', index).takeIf { it != -1 } ?: text.length
                return text.substring(start, end)
            }
        }
        return null
    }


}
