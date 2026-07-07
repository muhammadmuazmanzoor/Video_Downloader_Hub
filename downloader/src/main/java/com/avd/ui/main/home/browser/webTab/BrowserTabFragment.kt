package com.avd.ui.main.home.browser.webTab

import ViewPagerAdapter
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.avd.R
import com.avd.data.local.model.Suggestion
import com.avd.data.local.model.VideoInfoWrapper
import com.avd.data.local.room.entity.PageInfo
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.remote.sealed.ApiState
import com.avd.data.remote.service.VideoServiceLocal.Companion.COOKIE_HEADER
import com.avd.data.remote.service.VideoServiceLocal.Companion.MP4_EXT
import com.avd.databinding.DialogFetchingVideoBinding
import com.avd.databinding.FragmentBrowserTabBinding
import com.avd.ui.component.adapter.RecentVideosAdapter
import com.avd.ui.component.adapter.SuggestionAdapter
import com.avd.ui.component.adapter.SuggestionListener
import com.avd.ui.main.downloder_queue.utils.PermissionManagerNew
import com.avd.ui.main.home.CustomImageView
import com.avd.ui.main.home.MainViewModel
import com.avd.ui.main.home.bottomsheet.DefaultBrowserDialogFragment
import com.avd.ui.main.home.bottomsheet.NotificationDialogFragment
import com.avd.ui.main.home.browser.BaseWebTabFragment
import com.avd.ui.main.home.browser.BrowserListener
import com.avd.ui.main.home.browser.TabManagerProvider
import com.avd.ui.main.home.browser.homeTab.BrowserHomeViewModel
import com.avd.ui.main.home.browser.homeTab.OverlayPermissionDialog
import com.avd.ui.main.home.browser.homeTab.TiktokDownloadFeatureDialogFragment
import com.avd.ui.main.home.browser.homeTab.adapter.IconVideosAdapter
import com.avd.ui.main.home.browser.homeTab.adapter.MovieItemAdapter
import com.avd.ui.main.home.browser.homeTab.adapter.SocialAdapter
import com.avd.ui.main.home.browser.homeTab.enginedialogue.SearchEngineDialogFragment
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.IconItem
import com.avd.ui.main.home.downloadapi.ApiViewModel
import com.avd.ui.main.home.downloadapi.SocialDownloaderResponse
import com.avd.ui.main.home.downloadapi.VideoItem
import com.avd.ui.main.home.downloadapi.adapter.SocialFormatAdapter
import com.avd.ui.main.progress.ProgressViewModel
import com.avd.ui.main.video.VideoViewModel
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.refreshAd
import com.avd.util.AdBlockerHelper.setinterstitialshown
import com.avd.util.AdBlockerHelper.showExitScreen
import com.avd.util.AppUtil
import com.avd.util.CommunicateWithActivity
import com.avd.util.CookieUtils
import com.avd.util.CookieUtils.WIDGET_SHOW
import com.avd.util.DataStoreManager
import com.avd.util.DownloaderModuleNavigator
import com.avd.util.FirebaseEvents
import com.avd.util.FloatingBallView.floatingView
import com.avd.util.Prefs
import com.avd.util.RemoteConfigHelper
import com.avd.util.ScreenName
import com.avd.util.SharedPrefHelper
import com.avd.util.YoutubeDlUtils
import com.avd.youtubedl.VideoFormat
import com.avd.ui.main.widgetactivity.FloatingBallActivity
import com.avd.util.AdBlockerHelper.browser_native
import com.avd.util.AdBlockerHelper.cachedVideosList
import com.avd.util.AdBlockerHelper.home_native
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_browser
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.filter

interface BrowserHomeListener : BrowserListener {
    override fun onBrowserReloadClicked() {
    }

    override fun onTabCloseClicked() {
    }

    override fun onBrowserStopClicked() {
    }

    override fun onBrowserBackClicked() {
    }

    override fun onBrowserForwardClicked() {
    }
}

@AndroidEntryPoint
class BrowserTabFragment : BaseWebTabFragment(), ViewPagerAdapter.onClickListener,
    NotificationDialogFragment.NotificationCallback,
    DefaultBrowserDialogFragment.DefaultBrowserCallback {

    private val viewModel: ApiViewModel by activityViewModels()

    private lateinit var binding: FragmentBrowserTabBinding

    //private val binding get() = _binding!!
    private var fetchingDialog: Dialog? = null
    private var copiedLinkDialog: AlertDialog? = null

    companion object {
        private const val TAG = "WidgetDebug"

        fun newInstance() = BrowserTabFragment()
        var BASEURL = "google"
        private const val REQUEST_CODE_DEFAULT_BROWSER = 1001

        // Weak reference to avoid memory leaks
        private var weakActivity: WeakReference<BrowserTabFragment>? = null

        // Accessor for ViewModel (use with caution!)
        val viewModelInstance: ApiViewModel?
            get() = weakActivity?.get()?.viewModel

    }

    val progressViewModel: ProgressViewModel by viewModels()

    @Inject
    lateinit var appUtil: AppUtil

    private var host: CommunicateWithActivity? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private var permissionManager: PermissionManagerNew? = null
    private lateinit var openPageIProvider: TabManagerProvider

    private val homeViewModel: BrowserHomeViewModel by viewModels()

    private val mainViewModel: MainViewModel by activityViewModels()

    lateinit var list: List<PageInfo>

    private lateinit var suggestionAdapter: SuggestionAdapter

    private var hasFocus = false

    private lateinit var recentVideosAdapter: RecentVideosAdapter

    private val videoViewModel: VideoViewModel by viewModels()

    var isUrlReceived = false

//    private val moviesWebList = listOf(
//        IconItem(R.drawable.plex_movie_icon, "Plex",R.drawable.plex_thumb),
//        IconItem(R.drawable.movie_123, "123Movies",R.drawable.tubi_thumb),
//        IconItem(R.drawable.justwatch, "JustWatch",R.drawable.justwatch_thumb),
//        IconItem(R.drawable.moviebox, "MovieBox",R.drawable.moviebox_thumb),
//    )
//
//    private val dramasWebList = listOf(
//        IconItem(R.drawable.good_short_icon, "GoodShorts",R.drawable.goodshorts_thumb),
//        IconItem(R.drawable.stardust_icon, "Stardust",R.drawable.shortmax_thumb),
//        IconItem(R.drawable.reel_short_icon, "ReelShorts",R.drawable.topshorts_thumb),
//        IconItem(R.drawable.net_short_icon, "NetShorts",R.drawable.dramabox_thumb),
//    )


    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            host =
                context as? CommunicateWithActivity ?: error("Activity must implement HostActions")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (host != null) {
            host?.showBottomBar()
            Log.d("HostCheck", "showBottomBar")
        } else {
            Log.d("HostCheck", "null")
        }
        permissionManager = PermissionManagerNew(
            requireContext(), requireActivity(),
            object : PermissionManagerNew.Callback {
                override fun onStorageResult(isGranted: Boolean) {
                    Log.d("Permissions", "Storage granted=$isGranted")
                }

                override fun onNotificationResult(isGranted: Boolean) {
                    Log.d("Permissions", "Notification granted=$isGranted")
                }

                override fun onForegroundServiceResult(isGranted: Boolean) {
                    Log.d("Permissions", "Foreground Service granted=$isGranted")
                }
            }
        )

/*// Request permissions
        if (!permissionManager?.areAllPermissionsGranted()!!) {
            permissionManager?.requestAllPermissions(this)
        }*/
    }

    fun showFetchingVideoDialog(
    ): Dialog {
        val binding = DialogFetchingVideoBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = Dialog(requireContext())

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        //  dialog.show()

        return dialog
    }


    // In Fragment, override onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManagerNew.PERMISSION_REQUEST_CODE) {
            permissionManager?.handlePermissionsResult(this, permissions, grantResults)
        }
    }



    private fun observer() {

        binding.progressloading.setOnClickListener {}

/*        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.socialDownloadState.collectLatest { state ->
                when (state) {
                    is ApiState.Loading -> {
                        // Show progress bar
                        binding.progressloading.visibility = View.VISIBLE
                        binding.videoProgressBar.visibility = View.VISIBLE
                    }

                    is ApiState.Success -> {
                        // Hide progress bar & show data
                        binding.progressloading.visibility = View.GONE
                        binding.videoProgressBar.visibility = View.GONE
                        if (isAdded && view != null && isVisible) {
                            showSocialDownloadOptions(state.data)
                        }
                        // Update UI with videoData
                    }

                    is ApiState.Error -> {
                        // Hide progress bar & show error
                        binding.progressloading.visibility = View.GONE
                        binding.videoProgressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Some Thing Went Wrong! try again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is ApiState.Idle -> {
                        binding.progressloading.visibility = View.GONE
                        binding.videoProgressBar.visibility = View.GONE
                    }

                    else -> {
                        binding.progressloading.visibility = View.GONE
                        binding.videoProgressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Some Thing Went Wrong! try again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }*/

        viewModel.texturl.observe(viewLifecycleOwner) {
            if (isValidUrl(it)) {
                homeViewModel.searchTextInput.set(it)
            } else {
                homeViewModel.searchTextInput.set("")
            }
        }
    }

    private var activeBottomSheet1: BottomSheetDialog? = null
    private var activeBottomSheet2: BottomSheetDialog? = null

    private fun showSocialDownloadOptions(videosResponse: SocialDownloaderResponse) {
        var selectedFormatlocal: VideoItem? = null
        var position: Int = 0
        activeBottomSheet1?.dismiss()
        activeBottomSheet1 = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_download, null)
        val recyclerView = sheetView.findViewById<RecyclerView>(R.id.rvFormats)
        val thumbnail = sheetView.findViewById<CustomImageView>(R.id.thumbnail)
        val title = sheetView.findViewById<TextView>(R.id.title)
        val download = sheetView.findViewById<ConstraintLayout>(R.id.cl_download)
        val watch = sheetView.findViewById<ConstraintLayout>(R.id.cl_watch)
        sheetView.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            activeBottomSheet1?.dismiss()
        }
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        if (videosResponse.videos.isNotEmpty()) {
            selectedFormatlocal = videosResponse.videos[0]
            recyclerView.adapter =
                SocialFormatAdapter(videosResponse.videos) { selectedFormat, pos ->
                    selectedFormatlocal = selectedFormat
                    position = pos
                }
        }

        val randomNumber = (100000..999999).random()
        val vTitle = "${videosResponse.platform}_$randomNumber"
        title.text = vTitle

        if (videosResponse.thumbnail?.isNotEmpty() == true) {
            Glide.with(thumbnail.context)
                .load(videosResponse.thumbnail)
                .error(R.drawable.ic_vid_thumb)
                .into(thumbnail)
            thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            thumbnail.setImageResource(R.drawable.ic_vid_thumb)
        }

        watch.setOnClickListener {
            try {
                val intent = Intent(
                    requireContext(),
                    Class.forName("com.video.avd.ui.player.PlayerVideoActivity")
                )
                val bundle = Bundle()
                bundle.putBoolean("isliveuri", true)
                bundle.putBoolean("alreadyAdShown", false)
                bundle.putString("uri", selectedFormatlocal?.url)
                intent.putExtras(bundle)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModel.clearDownloadState()

        download.setOnClickListener {
            val outputDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            selectedFormatlocal?.url?.let {
                if (outputDir != null) {
                    downloadVideo(
                        url = it,
                        title = vTitle,
                        videosResponse = videosResponse,
                        pos = position
                    )
                }
            }
            viewModel.texturl.value = ""
            isUrlReceived = false
            activeBottomSheet1?.dismiss()
        }

        activeBottomSheet1?.setContentView(sheetView)
        activeBottomSheet1?.show()

        // ✅ Clear the wrapper background completely
        val bottomSheet = activeBottomSheet1?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) as? FrameLayout
        bottomSheet?.apply {
            background = null
            setBackgroundColor(Color.TRANSPARENT)
        }

        // ✅ Apply your rounded bg to sheetView directly
        sheetView.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
    }

    fun isValidUrl(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        return try {
            val uri = Uri.parse(text)
            uri.scheme == "http" || uri.scheme == "https"
        } catch (e: Exception) {
            false
        }
    }

    fun downloadVideo(
        url: String,
        title: String,
        videosResponse: SocialDownloaderResponse,
        pos: Int
    ) {
        val isTwitter = videosResponse.platform.equals("twitter", ignoreCase = true)
        if (isTwitter) fetchingDialog?.show()
        lifecycleScope.launch {
            if (isTwitter) {
                val videoInfo = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url.trim()).build()
                    val handledInfo = handleYoutubeDlUrl(request)
                    delay(500) // only if necessary — consider removing if not needed
                    handledInfo.videoInfo
                }
                fetchingDialog?.dismiss()
                progressViewModel.downloadVideo(videoInfo, false, pos)
            } else {
                progressViewModel.downloadVideo(videosResponse.toVideoInfo(title), true, pos)
            }
        }
    }

    private fun handleYoutubeDlUrl(url: Request): VideoInfoWrapper {

        val request = YoutubeDlUtils.getMappedYoutubeDLRequest(url.url.toString())
        url.headers.names().forEach {
            if (it != COOKIE_HEADER) {
                request?.addOption("--add-header", "$it:${url.headers[it]}")
            }
        }

        val tmpCookieFile = CookieUtils.addCookiesToRequest(url.url.toString(), request)
        try {
            val instance = YoutubeDlUtils.youtubeDl
            val info = YoutubeDlUtils.getYtdlInfo(instance, request)
            val formats = info?.formats?.map {
                videoEntityFromFormat(it)
            }
            val filtered = arrayListOf<VideoFormatEntity>()

            val listFormats =
                VideFormatEntityList(filtered.ifEmpty { formats?.filter { !(it.acodec != "none" && it.vcodec == "none") } }
                    ?: emptyList())
            if (listFormats.formats.isEmpty()) throw Exception("Audio Only Detected")
            return VideoInfoWrapper(VideoInfo(title = info?.title ?: "no title").also { videoInfo ->
                videoInfo.ext = info?.ext ?: MP4_EXT
                videoInfo.thumbnail = info?.thumbnail ?: ""
                videoInfo.duration = info?.duration?.toLong()!!
                videoInfo.originalUrl = url.url.toString()
                videoInfo.downloadUrls = emptyList()
                videoInfo.formats = listFormats
                videoInfo.isRegularDownload = false
            })
        } catch (e: Throwable) {
            throw e
        } finally {
            tmpCookieFile.delete()
        }
    }

    private fun videoEntityFromFormat(videoFormat: VideoFormat): VideoFormatEntity {
        return VideoFormatEntity(
            asr = videoFormat.asr,
            tbr = videoFormat.tbr,
            abr = videoFormat.abr,
            format = videoFormat.format,
            formatId = videoFormat.formatId,
            formatNote = videoFormat.formatNote,
            ext = videoFormat.ext,
            preference = videoFormat.preference,
            vcodec = videoFormat.vcodec,
            acodec = videoFormat.acodec,
            width = videoFormat.width,
            height = videoFormat.height,
            fileSize = videoFormat.fileSize,
            fileSizeApproximate = videoFormat.fileSizeApproximate,
            fps = videoFormat.fps,
            url = videoFormat.url,
            manifestUrl = videoFormat.manifestUrl,
            httpHeaders = videoFormat.httpHeaders
        )
    }

    fun SocialDownloaderResponse.toVideoInfo(title: String): VideoInfo {
        val video = videos.firstOrNull() ?: throw IllegalStateException("No videos available")
        val socialHeaders = RemoteConfigHelper.getSocialDownloaderHeaders()
        val requestBuilder = Request.Builder()
            .url(video.url.toString())
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Referer", "https://www.${platform}.com/")
        socialHeaders.forEach { (name, value) -> requestBuilder.addHeader(name, value) }
        val downloadHeaders = mutableMapOf(
            "User-Agent" to "Mozilla/5.0",
            "Referer" to "https://www.${platform}.com/"
        )
        downloadHeaders.putAll(socialHeaders)
        Log.d(
            "SocialDownloaderDownload",
            "Prepared web regular download url=${video.url} platform=$platform headers=${downloadHeaders.keys}"
        )

        return VideoInfo(
            id = UUID.randomUUID().toString(),
            downloadUrls = listOf(
                requestBuilder.build()
            ),
            title = title,
            ext = "mp4",
            thumbnail = thumbnail ?: "",
            duration = 0L,
            originalUrl = "",
            formats = VideFormatEntityList(
                formats = listOf(
                    VideoFormatEntity(
                        formatId = "unified",
                        url = video.url,
                        ext = "mp4",
                        vcodec = "",
                        acodec = "",
                         width = 0,
                        height = 0,
                        tbr = 0,
                        fileSize = 0L,
                        httpHeaders = downloadHeaders
                    )
                )
            ),
            isRegularDownload = true
        )
    }

    override fun onResume() {
        super.onResume()
        // Load switch state from preferences when returning to screen
        // This respects user's preference and checks permission status
        loadSwitchStateFromPreferences()
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        try {
            if (host != null) {
                host?.showBottomBar()
                Log.d("HostCheck", "showBottomBar")
            } else {
                Log.d("HostCheck", "null")
            }
            if (permissionManager?.areAllPermissionsGranted() == false) {
                if (activity?.let { isPermissionGranted(it) } == false) {
                    permissionManager?.requestAllPermissions(this)
                }
            }
        } catch (e: Exception) {
            Log.d("HostCheck", "${e.printStackTrace()}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        // Dismiss the dialog to prevent duplicates
        val existingDialog =
            parentFragmentManager.findFragmentByTag("NotificationDialogFragment") as? NotificationDialogFragment
        existingDialog?.dismiss()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            openPageIProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            suggestionAdapter = SuggestionAdapter(requireContext(), emptyList(), suggestionListener)
            dataStoreManager = DataStoreManager(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*try {
            openPageIProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            openNewTab("google")
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
        binding = FragmentBrowserTabBinding.inflate(inflater, container, false).apply {
            buildWebTabMenu(this.imgMenu, false)
            this.viewModel = homeViewModel
            this.mainVModel = mainViewModel
            this.browserMenuListener = menuListener
            this.homeEtSearch.setAdapter(suggestionAdapter)
            this.homeEtSearch.addTextChangedListener(onInputHomeSearchChangeListener)
            this.homeEtSearch.imeOptions = EditorInfo.IME_ACTION_SEARCH
            this.homeEtSearch.bringToFront()
            this.homeEtSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    this.homeEtSearch.clearFocus()
                    val text = (this@apply.homeEtSearch as EditText).text.toString()
                    if (!isUrlReceived) {
                        if (text.isNotEmpty()) {
                            viewModel?.viewModelScope?.launch {
                                delay(400)
                                openNewTab((this@apply.homeEtSearch as EditText).text.toString())
                                this@apply.homeEtSearch.text.clear()
                            }
                        }
                    } else {

                    }
                    false
                } else false
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FirebaseEvents.firebaseUserAction(
            "onViewCreated_downloaderHomeFragment",
            "BrowserHomeFragment",
            requireActivity()
        )
//        moviesItems()
//        dramaItems()
        // Start both view models
        videoViewModel.start()
        progressViewModel.start()
        Log.d("RecentVideos", "Both ViewModels started")

        AdBlockerHelper.isProVersion.observe(viewLifecycleOwner, Observer { it ->
            if (it == true) {
                binding.clPremimumNew.visibility = View.GONE
                binding.flAdplace.visibility = View.GONE
                showShimmer(false)
            } else {
                binding.clPremimumNew.visibility = View.VISIBLE
                binding.flAdplace.visibility = View.VISIBLE
                showShimmer(true) // Show shimmer before loading ad
            }
        })
        AdBlockerHelper.setAdShown(ScreenName.valueOf("DOWNLOAD_VIDEO"), false)
        refreshAd(this@BrowserTabFragment, requireContext(), false, binding.flAdplace,browser_native)
        DownloaderModuleNavigator.settingsViewModel?.isDesktopMode?.get()?.let { setIsDesktop(it) }
        homeViewModel.start()

        val openingUrl = mainViewModel.openedUrl.get()

        val openingText = mainViewModel.openedText.get()

        if (openingUrl != null) {
            showCopiedLinkOpenDialog(openingUrl)
            mainViewModel.openedUrl.set(null)
        }

        if (openingText != null) {
            showCopiedLinkOpenDialog(openingText)
            mainViewModel.openedText.set(null)
        }

        binding.tabCount.setOnClickListener {
            mainViewModel.openNavDrawerEvent.call()
        }

        setupRecyclerView()
        observer()
        fetchingDialog = showFetchingVideoDialog()
        binding.icSearch.setOnClickListener {
//            if (!isUrlReceived) {
                val text = (binding.homeEtSearch as EditText).text.toString()
                if (text.isNotEmpty() && !isSupportedSocialMediaUrl(text)) {
                    hideKeyboard(requireActivity())
                    openNewTab((binding.homeEtSearch as EditText).text.toString())
                    binding.homeEtSearch.text.clear()
                } else {
                    if (text.isNotEmpty()) {
                        viewModel.socialDownloader(binding.homeEtSearch.text.toString())
                    }
                }
           /* } else {
                if (isSupportedSocialMediaUrl(binding.homeEtSearch.text.toString())) {
                    if (binding.homeEtSearch.text.toString().isNotEmpty()) {
                        viewModel.socialDownloader(binding.homeEtSearch.text.toString())
                    }
                } else {
                    hideKeyboard(requireActivity())
                    openNewTab((binding.homeEtSearch as EditText).text.toString())
                    binding.homeEtSearch.text.clear()
                }

            }*/
        }

        binding.homeEtSearch.setOnFocusChangeListener { view, hasFocus ->
            this.hasFocus = hasFocus
        }

        binding.next.setOnClickListener {
            host?.setbottomseelection()
        }

        binding.clPremimumNew.setOnClickListener {
            try {
                val activityClass =
                    Class.forName("com.video.avd.ui.splash_flow.activities.InAppActivity")
                val intent = Intent(requireContext(), activityClass)
                intent.putExtra("where", "propanel")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        binding.ivProNew.setOnClickListener {
            try {
                val activityClass =
                    Class.forName("com.video.avd.ui.splash_flow.activities.InAppActivity")
                val intent = Intent(requireContext(), activityClass)
                intent.putExtra("where", "propanel")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        binding.clHowDownload.setOnClickListener {
            try {
                val activityClass =
                    Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                val intent = Intent(requireContext(), activityClass)
                intent.putExtra("where", "download")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // Setup recent videos RecyclerView
        setupRecentVideosRecyclerView()

        binding.clSetting.setOnClickListener {
            navigateToSettings()
        }

        // Setup TikTok download switch
        setupTiktokDownloadSwitch()

    }

    private fun setupTiktokDownloadSwitch() {
        Log.d(TAG, "setupTiktokDownloadSwitch: initializing switch")
        // Remove listener temporarily to avoid triggering when setting initial state
        binding.switchTiktokDownload?.setOnCheckedChangeListener(null)
        
        // Load initial state from preferences
        loadSwitchStateFromPreferences()
        
        // Set up listener
        binding.switchTiktokDownload?.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "setupTiktokDownloadSwitch: switch changed, isChecked=$isChecked")
            if (isChecked) {
                // User is trying to turn the switch ON
                val isPro = AdBlockerHelper.isProVersion.value == true || AdBlockerHelper.isPro
                val hasOverlayPermission = Settings.canDrawOverlays(requireContext())
                Log.d(TAG, "setupTiktokDownloadSwitch: isPro=$isPro, hasOverlayPermission=$hasOverlayPermission")

                if (hasOverlayPermission && isPro) {
                    // Permission already granted and user is pro - just enable switch
                    Log.d(TAG, "setupTiktokDownloadSwitch: permission already granted, enabling and showing widget")
                    sharedPrefHelper.setTiktokDownloadEnabled(true)
                    showFloatingWidget()
                    return@setOnCheckedChangeListener
                }

                // Permission not granted - proceed with dialog flow
                if (isPro) {
                    // Pro user - show feature dialog only once
                    val isFeatureDialogShown = sharedPrefHelper.isTiktokFeatureDialogShown()
                    Log.d(TAG, "setupTiktokDownloadSwitch: pro user, isFeatureDialogShown=$isFeatureDialogShown")

                    if (!isFeatureDialogShown) {
                        // First time - show feature dialog
                        Log.d(TAG, "setupTiktokDownloadSwitch: showing feature dialog (first time)")
                        showTiktokDownloadFeatureDialog()
                        binding.switchTiktokDownload?.isChecked = false
                    } else {
                        // Feature dialog already shown - directly show permission dialog
                        Log.d(TAG, "setupTiktokDownloadSwitch: showing overlay permission dialog")
                        handleTryNowClick()
                        binding.switchTiktokDownload?.isChecked = false
                    }
                    // Try to show widget (will no-op if permission not granted; widget shows after user returns from Settings)
                    showFloatingWidget()
                } else {
                    Log.d(TAG, "setupTiktokDownloadSwitch: non-pro user, showing feature dialog")
                    // Non-pro user - always show feature dialog
                    showTiktokDownloadFeatureDialog()
                    binding.switchTiktokDownload?.isChecked = false
                }
            } else {
                Log.d(TAG, "setupTiktokDownloadSwitch: switch OFF, saving preference and removing widget")
                sharedPrefHelper.setTiktokDownloadEnabled(false)
                removeFloatingWidget()
            }
        }
    }

    private var dismissIndicator: ImageView? = null
    var showWidget = false

    /** Removes the floating widget and dismiss indicator from the window. Call when switch is turned OFF. */
    private fun removeFloatingWidget() {
        Log.d(TAG, "removeFloatingWidget: entered, showWidget=$showWidget")
        val act = activity ?: return
        val windowManager = act.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        try {
            floatingView?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                    Log.d(TAG, "removeFloatingWidget: floating view removed")
                }
            }
            dismissIndicator?.let { view ->
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                    Log.d(TAG, "removeFloatingWidget: dismiss indicator removed")
                }
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "removeFloatingWidget: view not attached", e)
        }
        floatingView = null
        dismissIndicator = null
        showWidget = false
        Prefs[WIDGET_SHOW] = false
        Log.d(TAG, "removeFloatingWidget: done")
    }

    private fun showFloatingWidget() {
            Log.d(TAG, "showFloatingWidget: entered, showWidget=$showWidget")
            if (showWidget && floatingView?.isAttachedToWindow == true) {
                Log.d(TAG, "showFloatingWidget: widget already visible, skipping")
                return
            }
            val ctx = context ?: run {
                Log.e(TAG, "showFloatingWidget: context is null, aborting")
                return
            }
            val act = activity
            if (act == null) {
                Log.e(TAG, "showFloatingWidget: activity is null, aborting")
                return
            }
            val hasOverlay = Settings.canDrawOverlays(ctx)
            if (!hasOverlay) {
                Log.w(TAG, "showFloatingWidget: overlay permission not granted - widget will show after user grants permission and returns")
                return
            }

            Log.d(TAG, "showFloatingWidget: permission OK, creating widget")
            Prefs[WIDGET_SHOW] = true
            val windowManager = act.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) {
                Log.e(TAG, "showFloatingWidget: WindowManager is null, aborting")
                return
            }

            val layoutParams =
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
        val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels


            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = 0 //screenWidth / 2
            layoutParams.y = (screenHeight / 3.5).toInt()

            val inflater = LayoutInflater.from(ctx)
            floatingView = inflater.inflate(R.layout.floating_ball_layout, null)
            if (floatingView == null) {
                Log.e(TAG, "showFloatingWidget: failed to inflate floating_ball_layout")
                return
            }
            Log.d(TAG, "showFloatingWidget: inflated floating view")


            val shortButton: ImageView = floatingView!!.findViewById(R.id.fabDownload)

            val minAlpha = 0.2f
            val maxAlpha = 1.0f

            val alphaValue = minAlpha + (50 / 100f) * (maxAlpha - minAlpha)
            // shortButton.alpha = alphaValue

            try {
                windowManager.addView(floatingView, layoutParams)
                Log.d(TAG, "showFloatingWidget: widget view added to WindowManager successfully")
            } catch (e: Exception) {
                Log.e(TAG, "showFloatingWidget: addView failed", e)
                return
            }

            floatingView?.postDelayed({
                val targetX = screenWidth
                val animator = ValueAnimator.ofInt(layoutParams.x, targetX)
                animator.duration = 600
                animator.addUpdateListener { animation ->
                    if (floatingView?.isAttachedToWindow != true) {
                        animator.cancel()
                        return@addUpdateListener
                    }
                    layoutParams.x = animation.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(floatingView, layoutParams)
                    } catch (e: IllegalArgumentException) {
                        animator.cancel()
                    }
                }
                animator.start()

                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {

                        if (floatingView != null){
                            blinkWidget(floatingView!!)
                        }
                    }
                })
            }, 1000)

            var isDragging = false
            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f
            val dragThreshold = 10
            val dismissThreshold = screenHeight * 0.85f

            // Dismiss indicator is only added when user starts dragging (so only one overlay view by default)
            val widthInDp = 55
            val heightInDp = 55
            val density = Resources.getSystem().displayMetrics.density
            val dismissParams = WindowManager.LayoutParams(
                (widthInDp * density).toInt(),
                (heightInDp * density).toInt(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                     WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = (screenHeight * 0.05).toInt()
            }

            fun addDismissIndicatorIfNeeded() {
                if (dismissIndicator != null && dismissIndicator?.isAttachedToWindow == true) return
                val view = LayoutInflater.from(act).inflate(R.layout.widget_close, null) as? ImageView
                    ?: return
                view.alpha = 0f
                try {
                    windowManager.addView(view, dismissParams)
                    view.animate().alpha(1f).setDuration(100).start()
                    dismissIndicator = view
                } catch (e: Exception) {
                    Log.w(TAG, "showFloatingWidget: failed to add dismiss indicator", e)
                }
            }

            fun removeDismissIndicatorFromWindow() {
                dismissIndicator?.let { view ->
                    if (view.isAttachedToWindow) {
                        try {
                            windowManager.removeView(view)
                        } catch (e: IllegalArgumentException) { }
                    }
                    dismissIndicator = null
                }
            }

            showWidget = true

            shortButton.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isDragging = false
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - touchX).toInt()
                        val deltaY = (event.rawY - touchY).toInt()

                        if (Math.abs(deltaX) > dragThreshold || Math.abs(deltaY) > dragThreshold) {
                            if (!isDragging) {
                                isDragging = true
                                addDismissIndicatorIfNeeded()
                            }

                            layoutParams.x = initialX + deltaX
                            layoutParams.y = initialY + deltaY
                            if (floatingView?.isAttachedToWindow == true) {
                                try {
                                    windowManager.updateViewLayout(floatingView, layoutParams)
                                    floatingView?.alpha =
                                        if (event.rawY >= screenHeight / 2f) 0.5f else 1.0f
                                } catch (e: IllegalArgumentException) {
                                    // View no longer attached to window manager (e.g. dismissed or activity destroyed)
                                }
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        floatingView?.alpha = 1.0f
                        removeDismissIndicatorFromWindow()

                        if (isDragging) {
                            if (event.rawY >= dismissThreshold) {
                                // Dismiss the view
                                //(getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(50)

                                val animator = ObjectAnimator.ofFloat(
                                    floatingView,
                                    "translationY",
                                    floatingView?.translationY ?: 0f,
                                    screenHeight.toFloat()
                                )
                                animator.duration = 200
                                animator.interpolator = AccelerateInterpolator()
                                animator.addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        try {
                                            floatingView?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
                                            dismissIndicator?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
                                        } catch (e: IllegalArgumentException) {
                                            // View not attached to window manager
                                        }
                                        floatingView = null
                                        dismissIndicator = null
                                        showWidget = false
                                        Prefs[WIDGET_SHOW] = false
                                        sharedPrefHelper.setTiktokDownloadEnabled(false)
                                        binding?.switchTiktokDownload?.isChecked = false
                                    }
                                })
                                animator.start()
                            }
                        } else {
                            showAlertDialog()
                        }
                        true
                    }

                    else -> false
                }
            }
    }


    private fun showAlertDialog() {
            val intent = Intent(activity, FloatingBallActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
    }


    private fun blinkWidget(view: View) {
        val blinkDuration = 100L
        val totalBlinks = 3

        val handler = Handler(Looper.getMainLooper())
        var blinkCount = 0
        val blinkRunnable = object : Runnable {
            override fun run() {
                if (blinkCount < totalBlinks) {
                    view.alpha = if (view.alpha == 1.0f) 0.0f else 1.0f
                    blinkCount++
                    handler.postDelayed(this, blinkDuration)
                } else {
                    view.alpha = 1.0f
                }
            }
        }

        handler.post(blinkRunnable)
    }

    private fun showTiktokDownloadFeatureDialog() {
        val dialog = TiktokDownloadFeatureDialogFragment {
            // "Try Now" clicked callback - mark feature dialog as shown
            sharedPrefHelper.setTiktokFeatureDialogShown(true)
            proceedWithPermissionOrProPanel()
        }
        dialog.show(parentFragmentManager, "TiktokDownloadFeatureDialog")
    }

    private fun handleTryNowClick() {
        // This is called when feature dialog was already shown
        // Just proceed with permission/pro panel flow
        proceedWithPermissionOrProPanel()
    }

    private fun proceedWithPermissionOrProPanel() {
        val isPro = AdBlockerHelper.isProVersion.value == true || AdBlockerHelper.isPro
        
        if (isPro) {
            // Check if permission is already granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val hasOverlayPermission = Settings.canDrawOverlays(requireContext())
                if (hasOverlayPermission) {
                    // Permission already granted - enable switch and save preference
                    binding.switchTiktokDownload?.isChecked = true
                    sharedPrefHelper.setTiktokDownloadEnabled(true)
                    return
                }
            }
            // Permission not granted - show permission dialog
            showOverlayPermissionDialog()
        } else {
            // Navigate to pro panel for non-pro users
            navigateToProPanel()
        }
    }

    private fun showOverlayPermissionDialog() {
        val dialog = OverlayPermissionDialog(
            requireContext(),
            onPermissionGranted = null,
            onOpenOverlaySettings = { sharedPrefHelper.setOverlayPermissionPending(true) }
        )
        dialog.show()
    }

    private fun navigateToProPanel() {
        try {
            val activityClass =
                Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
            val intent = Intent(requireContext(), activityClass)
            intent.putExtra("where", "propanel")
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        copiedLinkDialog?.dismiss()
        copiedLinkDialog = null
        super.onDestroyView()
        showShimmer(false) // Stop shimmer when fragment is destroyed
        progressViewModel.stop()
    }

    private fun showCopiedLinkOpenDialog(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        if (!isValidUrl(trimmed)) {
            openNewTab(trimmed)
            return
        }
        if (!isAdded || copiedLinkDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_copied_link_detected, null)
        val title = dialogView.findViewById<TextView>(R.id.tvTitle)
        val copiedLink = dialogView.findViewById<TextView>(R.id.tvCopiedLink)
        val cancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val open = dialogView.findViewById<TextView>(R.id.btnDownload)

        title.text = "Copied link detected"
        copiedLink.text = trimmed
        open.text = "OPEN"

        copiedLinkDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        copiedLinkDialog?.setOnDismissListener {
            copiedLinkDialog = null
        }

        cancel.setOnClickListener {
            copiedLinkDialog?.dismiss()
        }

        open.setOnClickListener {
            copiedLinkDialog?.dismiss()
            openNewTab(trimmed)
        }

        copiedLinkDialog?.show()
        copiedLinkDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun checkOverlayPermissionAndUpdateSwitch() {
        // This method is called when user returns from Settings after granting permission
        // If permission is granted and user is pro, enable switch automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasOverlayPermission = Settings.canDrawOverlays(requireContext())
            val isPro = AdBlockerHelper.isProVersion.value == true || AdBlockerHelper.isPro
            
            if (hasOverlayPermission && isPro) {
                // Permission granted and user is pro - enable switch
                binding.switchTiktokDownload?.isChecked = true
                sharedPrefHelper.setTiktokDownloadEnabled(true)
            } else {
                // Permission not granted or not pro - turn switch OFF
                binding.switchTiktokDownload?.isChecked = false
                sharedPrefHelper.setTiktokDownloadEnabled(false)
            }
        }
    }

    private fun loadSwitchStateFromPreferences() {
        // Always respect saved preference: if user turned switch OFF, keep it OFF
        val savedState = sharedPrefHelper.isTiktokDownloadEnabled()
        Log.d(TAG, "loadSwitchStateFromPreferences: savedState=$savedState")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasOverlayPermission = Settings.canDrawOverlays(requireContext())
            val isPro = AdBlockerHelper.isProVersion.value == true || AdBlockerHelper.isPro
            val overlayPending = sharedPrefHelper.isOverlayPermissionPending()
            Log.d(TAG, "loadSwitchStateFromPreferences: hasOverlayPermission=$hasOverlayPermission, isPro=$isPro, overlayPending=$overlayPending")

            if (hasOverlayPermission && isPro) {
                // Only auto-enable when user just came back from overlay Settings (pending flag set)
                if (overlayPending) {
                    Log.d(TAG, "loadSwitchStateFromPreferences: user returned from overlay settings, enabling switch and showing widget")
                    sharedPrefHelper.setOverlayPermissionPending(false)
                    binding.switchTiktokDownload?.isChecked = true
                    sharedPrefHelper.setTiktokDownloadEnabled(true)
                    showFloatingWidget()
                } else {
                    // Respect saved preference
                    binding.switchTiktokDownload?.isChecked = savedState
                    if (savedState && !showWidget) {
                        Log.d(TAG, "loadSwitchStateFromPreferences: savedState ON, showing widget")
                        showFloatingWidget()
                    } else if (!savedState) {
                        removeFloatingWidget()
                    }
                }
            } else {
                binding.switchTiktokDownload?.isChecked = false
                if (savedState) {
                    sharedPrefHelper.setTiktokDownloadEnabled(false)
                }
                removeFloatingWidget()
            }
        } else {
            binding.switchTiktokDownload?.isChecked = savedState
            if (!savedState) removeFloatingWidget()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Handle the back button event
            Log.d("exitTag", "onCreate: 1")
            try {
                val indexRoute = DownloaderModuleNavigator.mainViewModel?.currentItem?.get()
                if (indexRoute == 0) {
                    Log.d("exitTag", "onCreate: 2")
                    showExitScreen?.invoke()
                   // requireActivity().finish()
                } else {
                    mainViewModel.currentItem.set((mainViewModel.currentItem.get() ?: 0) - 1)
                }

            } catch (e: Exception) {
                val indexRoute = DownloaderModuleNavigator.mainViewModel?.currentItem?.get()
                if (indexRoute == 0) {
                    showExitScreen?.invoke()
                    Log.d("exitTag", "onCreate: 3")
                   // requireActivity().finish()
                } else {
                    mainViewModel.currentItem.set((mainViewModel.currentItem.get() ?: 0) - 1)
                }
                e.printStackTrace()
            }
        }
    }

//    private fun moviesItems() {
//        val recyclerView = binding.recyclerViewMovies
//        recyclerView.layoutManager =
//            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//        recyclerView.adapter = MovieItemAdapter(moviesWebList) { clickedItem ->
//            FirebaseEvents.firebaseUserAction(
//                "Brows_${clickedItem.title}_clicked",
//                "BrowserHomeFragment",
//                requireActivity()
//            )
//            handleMovieClick(clickedItem)
//        }
//    }
//
//    private fun dramaItems() {
//        val recyclerView = binding.recyclerViewDrama
//        recyclerView.layoutManager =
//            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
//        recyclerView.adapter = MovieItemAdapter(dramasWebList) { clickedItem ->
//            FirebaseEvents.firebaseUserAction(
//                "Brows_${clickedItem.title}_clicked",
//                "BrowserHomeFragment",
//                requireActivity()
//            )
////            host?.hideBottomBar()
//            handleDramaClick(clickedItem)
//        }
//    }
//
//    private fun handleMovieClick(item: IconItem) {
//        when (item.title) {
//            "123Movies" -> openNewTab("https://ww20.0123movie.net/home.html")
//            "MovieBox" -> openNewTab("https://themoviebox.xyz/")
//            "JustWatch" -> openNewTab("https://www.justwatch.com/")
//            "Plex" -> openNewTab("https://watch.plex.tv/me")
//            else -> Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT)
//                .show()
//        }
//    }
//
//    private fun handleDramaClick(item: IconItem) {
//        when (item.title) {
//            "Stardust" -> openNewTab("https://www.stardusttv.net/")
//            "ReelShorts" -> openNewTab("https://www.reelshort.com")
//            "GoodShorts" -> openNewTab("https://www.goodshort.com/")
//            "NetShorts" -> openNewTab("https://netshort.com/")
//            else -> Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT)
//                .show()
//        }
//    }
    private fun handleSocialClick(pageInfo: PageInfo) {
        when (pageInfo.link) {
            "https://www.Status.com" -> {
                try {
                    val activityClass =
                        Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                    val intent = Intent(requireContext(), activityClass)
                    intent.putExtra("where", "whatsapp")
                    startActivity(intent)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            else -> {
                try {
                    if (host != null) {
//                        host?.hideBottomBar()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                openNewTab(pageInfo.link)
            }
        }
    }

    private val suggestionListener = object : SuggestionListener {
        override fun onItemClicked(suggestion: Suggestion) {
//            host?.hideBottomBar()
            openNewTab(suggestion.content)
        }
    }

    private fun openNewTab(input: String) {
        if (input.isNotEmpty()) {
            openPageIProvider.getOpenTabEvent().value =
                WebTabFactory.createWebTabFromInput(input, BASEURL)
        }
    }

    private val onInputHomeSearchChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            val input = s.toString()
            if (input.isEmpty()) {
                isUrlReceived = false
            }
            homeViewModel.searchTextInput.set(input)
            if (!(input.startsWith("http://") || input.startsWith("https://"))) {
                homeViewModel.showSuggestions()
            }
            homeViewModel.homePublishSubject.onNext(input)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val input = s.toString()
            if (input.isEmpty()) {
                isUrlReceived = false
            }
        }
    }

    private val menuListener = object : BrowserHomeListener {
        override fun onBrowserMenuClicked() {
            buildWebTabMenu(binding.imgMenu, false)
            showPopupMenu()
        }
        override fun onBrowserBackClicked() {
            super.onBrowserBackClicked()
            activity?.onBackPressed()
        }

        override fun onBrowserHomeClicked() {
            activity?.onBackPressed()
            host?.showHome()
            host?.showBottomBar()
        }
    }

    fun setupRecyclerView() {
        homeViewModel.lisofpages.observe(viewLifecycleOwner) { listOfPages ->
            listOfPages?.let {
                list = browserShortcutPages()
                val recyclerView = binding.recyclerViewSocial
                recyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                recyclerView.adapter = SocialAdapter(list) { clickedItem ->
                    FirebaseEvents.firebaseUserAction(
                        "Brows_${clickedItem.name}_clicked",
                        "BrowserHomeFragment",
                        requireActivity()
                    )
                    handleSocialClick(clickedItem)
                }
            }
        }
    }

    private fun browserShortcutPages(): List<PageInfo> {
        return listOf(
            PageInfo(name = "facebook", link = "https://www.facebook.com/").apply {
                drawableResId = R.drawable.icon_facebook
            },
            PageInfo(name = "bing.com", link = "https://www.bing.com").apply {
                drawableResId = R.drawable.ic_bing
            },
            PageInfo(name = "duckduckgo.com", link = "https://duckduckgo.com").apply {
                drawableResId = R.drawable.ic_duck_duck
            },
            PageInfo(name = "google", link = "https://www.google.com").apply {
                drawableResId = R.drawable.ic_google
            }
        )
    }

    override fun shareWebLink() {}

    override fun onClicklistner(pageInfo: PageInfo) {
        val name = getDomainName(pageInfo.name)
        FirebaseEvents.firebaseUserAction(
            "Brows_${name}_clicked",
            "BrowserHomeFragment",
            requireActivity()
        )
        when (pageInfo.link) {
            "https://www.Status.com" -> {
                if(interHome!=null) {
                    interHome?.let {
                        showInterstitial(false, it, requireActivity(), {
                            try {
                                val activityClass =
                                    Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                                val intent = Intent(requireContext(), activityClass)
                                intent.putExtra("where", "whatsapp")
                                startActivity(intent)
                            } catch (e: ClassNotFoundException) {
                                e.printStackTrace()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },inter_browser)
                    }

                }
                else{
                    try {
                        val activityClass =
                            Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                        val intent = Intent(requireContext(), activityClass)
                        intent.putExtra("where", "whatsapp")
                        startActivity(intent)
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            else -> {
                if(interHome!=null) {
                    interHome?.let {
                        showInterstitial(true, it, requireActivity(), {
                            try {
                                if (host != null) {
//                        host?.hideBottomBar()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            openNewTab(pageInfo.link)
                        },inter_browser)
                    }

                }
                else{
                    loadFallbackInterstitialAd(requireActivity(), requireActivity().resources.getString(R.string.Interstitial_Home_ID_High), requireActivity().resources.getString(R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                        interHome=it
                    },{
                        interHome=it
                    })
                    try {
                        if (host != null) {
//                        host?.hideBottomBar()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    openNewTab(pageInfo.link)
                }

            }
        }
    }

    fun getDomainName(url: String): String {
        return try {
            val regex = """www\.(\w+)(?=\.com)""".toRegex()
            regex.find(url)?.groupValues?.get(1) ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun showSearchEngineDialog() {
        val dialog = SearchEngineDialogFragment { selectedEngine, icon ->
            // Handle the selected search engine, e.g., initiate a search
            BASEURL = selectedEngine
            binding.searchEngine.setImageResource(icon)
        }
        activity?.supportFragmentManager?.let { dialog.show(it, "SearchEngineDialogFragment") }
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onButtonClicked(result: Boolean) {
        if (result) {
            lifecycleScope.launch {
                setinterstitialshown(true)
                delay(15000)
                setinterstitialshown(false)
            }
            if (!permissionManager!!.areAllPermissionsGranted()) {
                if (!activity?.let { isPermissionGranted(it) }!!) {
                    permissionManager!!.requestAllPermissions(this)
                }
            }
        }
    }

    override fun onBrowserButtonClicked(result: Boolean) {
        if (result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestDefaultBrowserRole()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestDefaultBrowserRole() {
        val roleManager = requireContext().getSystemService(RoleManager::class.java)
        // Create an intent to request the default browser role
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
        startActivityForResult(intent, REQUEST_CODE_DEFAULT_BROWSER)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DEFAULT_BROWSER) {
            if (resultCode == RESULT_OK) {
                // The user set your app as the default browser
            } else {
                // The user did not set your app as the default browser
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Clear focus from EditText to prevent keyboard from reopening
        binding.homeEtSearch.clearFocus()
        activeBottomSheet1?.dismiss()
        activeBottomSheet2?.dismiss()
        // Force-hide the keyboard
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().window.decorView.windowToken, 0)
    }

    private fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val videoPermission = Manifest.permission.READ_MEDIA_VIDEO
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            ContextCompat.checkSelfPermission(
                context,
                videoPermission
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        notificationPermission
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                storagePermission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showShimmer(show: Boolean) {
        if (show) {
            AdBlockerHelper.setupNativeShimmer(binding.flAdplace, layoutInflater)
        } else {
            AdBlockerHelper.hideNativeShimmer(binding.flAdplace)
        }
    }

    private fun setupRecentVideosRecyclerView() {

        recentVideosAdapter = RecentVideosAdapter { videoInfo ->
            try {
                var intent: Intent? = null
                intent = Intent(
                    requireContext(),
                    Class.forName("com.video.avd.ui.player.PlayerVideoActivity")
                )
                val bundle = Bundle()
                bundle.putBoolean("isliveuri", true)
                bundle.putBoolean("alreadyAdShown", false)
                bundle.putString("uri", videoInfo.originalUrl)
                intent.putExtras(bundle)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.rvRecentVideos.apply {
            adapter = recentVideosAdapter
            // Use a custom layout manager that handles immediate updates better
            layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
                    // Enable predictive animations for smoother updates
                    isItemPrefetchEnabled = true
                    initialPrefetchItemCount = 4
                }
            // Enable item animations for smoother updates
            itemAnimator = null // Disable default animations which can cause delays
            // Set fixed size for better performance
            setHasFixedSize(true)
            // Enable view cache for smoother scrolling
            setItemViewCacheSize(20)
        }

        var previousVideoCount = 0

        // Observe downloaded videos
        lifecycleScope.launch(Dispatchers.Main.immediate) { // Use immediate dispatcher for faster updates
            val videos = cachedVideosList.get()
            Log.d("RecentVideos", "Cache videos found: ${videos?.size}")
            videos?.let { videoList ->
                val status = recentVideosAdapter.updateDownloadedVideos(videoList)
                if (status) {
                    binding.rvRecentVideos.post {
                        binding.rvRecentVideos.scrollToPosition(0)
                    }
                }
            }
        }

        videoViewModel.localVideos.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main.immediate) { // Use immediate dispatcher for faster updates
                    val videos = videoViewModel.localVideos.get()
                    Log.d("RecentVideos", "Local videos found: ${videos?.size}")
                    videos?.let { videoList ->
                        val currentCount = videoList.size
                        val isNewItemInserted = currentCount > previousVideoCount
                        val status = recentVideosAdapter.updateDownloadedVideos(videoList)
                        if (status && isNewItemInserted) {
                            binding.rvRecentVideos.post {
                                binding.rvRecentVideos.scrollToPosition(0)
                            }
                        }
                        previousVideoCount = currentCount
                    }
                }
            }
        })

//      Observe downloading videos
        progressViewModel.progressInfos.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main.immediate) { // Use immediate dispatcher for faster updates
                    val progressInfos = progressViewModel.progressInfos.get()
                    // Update downloading videos
                    val downloadingVideos = progressInfos?.filter { it.progress in 0..99 }
                        ?.map { it.videoInfo } ?: emptyList()
                    val inserted = recentVideosAdapter.updateDownloadingVideos(downloadingVideos)
                    if (inserted) {
                        Log.d("newitem", "Inserted new item at top")
                        binding.rvRecentVideos.post {
                            binding.rvRecentVideos.scrollToPosition(0)
                        }
                    }
                    // Update progress for each downloading video
                    progressInfos?.forEach { progressInfo ->
                        if (progressInfo.progress in 0..99) {
                            recentVideosAdapter.updateDownloadProgress(
                                progressInfo.videoInfo.id,
                                progressInfo.progress
                            )
                        } else if (progressInfo.progress == 100) {
                            recentVideosAdapter.removeDownloadProgress(progressInfo.videoInfo.id)
                        }
                    }
                }
            }
        })

    }


    fun isSupportedSocialMediaUrl(text: String): Boolean {
        val regex = Regex(
            pattern = """^https?://(?:www\.)?(twitter\.com|t\.co|x\.com|facebook\.com|fb\.com|fb\.watch|m\.facebook\.com|instagram\.com|instagr\.am|tiktok\.com|vm\.tiktok\.com|vt\.tiktok\.com)(?:/.*)?$""",
            option = RegexOption.IGNORE_CASE
        )
        return regex.containsMatchIn(text.trim())
    }

}
