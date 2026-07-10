package com.avd.ui.main.home.browser.webTab

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.avd.R
import com.avd.data.local.model.VideoInfoWrapper
import com.avd.data.local.room.entity.HistoryItem
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.databinding.FragmentWebTabBinding
import com.avd.ui.component.adapter.SuggestionTabListener
import com.avd.ui.component.adapter.TabSuggestionAdapter
import com.avd.ui.component.dialog.DownloadTabListener
import com.avd.ui.main.home.MainViewModel
import com.avd.ui.main.home.browser.BaseWebTabFragment
import com.avd.ui.main.home.browser.BrowserFragment
import com.avd.ui.main.home.browser.BrowserListener
import com.avd.ui.main.home.browser.BrowserViewModel
import com.avd.ui.main.home.browser.CurrentTabIndexProvider
import com.avd.ui.main.home.browser.CustomWebChromeClient
import com.avd.ui.main.home.browser.CustomWebViewClient
import com.avd.ui.main.home.browser.DownloadButtonStateCanDownload
import com.avd.ui.main.home.browser.DownloadButtonStateCanNotDownload
import com.avd.ui.main.home.browser.DownloadButtonStateLoading
import com.avd.ui.main.home.browser.HOME_TAB_INDEX
import com.avd.ui.main.home.browser.HistoryProvider
import com.avd.ui.main.home.browser.PageTabProvider
import com.avd.ui.main.home.browser.TAB_INDEX_KEY
import com.avd.ui.main.home.browser.TabManagerProvider
import com.avd.ui.main.home.browser.WorkerEventProvider
import com.avd.ui.main.home.browser.detectedVideos.DetectedVideosTabFragment
import com.avd.ui.main.home.browser.detectedVideos.DetectedVideosTabViewModel
import com.avd.ui.main.home.browser.homeTab.BrowserHomeFragment.Companion.BASEURL
import com.avd.ui.main.progress.ProgressFragment
import com.avd.ui.main.progress.ProgressViewModel
import com.avd.ui.main.video.VideoFragment
import com.avd.util.AdBlockerHelper
import com.avd.util.AppLogger
import com.avd.util.AppUtil
import com.avd.util.CommunicateWithActivity
import com.avd.util.ContextUtils
import com.avd.util.DownloaderModuleNavigator
import com.avd.util.FileNameCleaner
import com.avd.util.NotificationsHelper
import com.avd.util.NetworkUtils
import com.avd.util.SharedPrefHelper
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.avd.util.downloaders.youtubedl_downloader.YoutubeDlDownloader
import com.avd.util.proxy_utils.CustomProxyController
import com.avd.util.proxy_utils.OkHttpProxyClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebTabFragment : BaseWebTabFragment(), ButtonVisibilityYoutube {

    companion object {
        fun newInstance() = WebTabFragment()
    }

    private var wifiOnly: Boolean = false
    private lateinit var suggestionAdapter: TabSuggestionAdapter
    private var host: CommunicateWithActivity? = null
    private val mainViewModel: MainViewModel by activityViewModels()

    private val progressViewModel: ProgressViewModel by activityViewModels()

    @Inject
    lateinit var appUtil: AppUtil

    @Inject
    lateinit var proxyController: CustomProxyController

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    private lateinit var dataBinding: FragmentWebTabBinding

    private lateinit var tabManagerProvider: TabManagerProvider

    private lateinit var pageTabProvider: PageTabProvider

    private lateinit var historyProvider: HistoryProvider

    private lateinit var workerEventProvider: WorkerEventProvider
    private lateinit var openPageIProvider: TabManagerProvider
    private lateinit var currentTabIndexProvider: CurrentTabIndexProvider
    private  val browserViewModel: BrowserViewModel by activityViewModels()
    private val tabViewModel: WebTabViewModel by viewModels()

    private val videoDetectionTabViewModel: DetectedVideosTabViewModel by activityViewModels()

    private var webTab: WebTab? = null
    private var videoToast: Toast? = null

    private var floatingLoadingResetJob: Job? = null

    private var canGoCounter = 0

    private var isFaceBook = false

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleOnBackPress()
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("FragmentLifecycle", "onAttach called")
        try {
            host = context as? CommunicateWithActivity ?: error("Activity must implement HostActions")
        } catch (e: Exception) {
            Log.e("FragmentLifecycle", "Error in onAttach: ${e.localizedMessage}", e)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDownloadViewModel(requireActivity())
    }

    private fun initDownloadViewModel(it: FragmentActivity) {
//        val initParams = AddInitParams()
//        viewModel.fillInitParams(initParams, it)
//        viewModel.initParams(initParams)
    }
    private fun openNewTab(input: String) {
        if (input.isNotEmpty()) {
            openPageIProvider.getOpenTabEvent().value =
                WebTabFactory.createWebTabFromInput(input, BASEURL)
        }
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        try {
            val thisTabIndex = requireArguments().getInt(TAB_INDEX_KEY)
            tabManagerProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            pageTabProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            historyProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            workerEventProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            currentTabIndexProvider = DownloaderModuleNavigator.mainViewModel?.browserServicesProvider!!
            videoDetectionTabViewModel.settingsModel = DownloaderModuleNavigator.settingsViewModel!!
            videoDetectionTabViewModel.webTabModel = tabViewModel
            tabViewModel.openPageEvent = tabManagerProvider.getOpenTabEvent()
            tabViewModel.closePageEvent = tabManagerProvider.getCloseTabEvent()
            tabViewModel.thisTabIndex.set(thisTabIndex)
            webTab = pageTabProvider.getPageTab(thisTabIndex)
            recreateWebView(savedInstanceState)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dataBinding = FragmentWebTabBinding.inflate(inflater, container, false).apply {
            buildWebTabMenu(this.imgSetting, true)
            viewModel = tabViewModel
            browserMenuListener = tabListener
            settingsViewModel = DownloaderModuleNavigator.settingsViewModel
            videoTabVModel = videoDetectionTabViewModel
            suggestionAdapter =
                TabSuggestionAdapter(requireContext(), mutableListOf(), suggestionListener)
            search.setAdapter(suggestionAdapter)
            search.addTextChangedListener(onInputTabChangeListener)
            this.search.imeOptions = EditorInfo.IME_ACTION_DONE
            this.search.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    this.search.clearFocus()
                    viewModel?.viewModelScope?.launch {
                        delay(400)
                        tabViewModel.loadPage((this@apply.search as EditText).text.toString())
                    }
                    false
                } else false
            }
            forward.clipToOutline = true
            back3.clipToOutline = true
            refresh.clipToOutline = true
            constraintLayout4.bringToFront()
            back3.setOnClickListener { tabListener.onBrowserBackClicked() }
            forward.setOnClickListener { tabListener.onBrowserForwardClicked() }
            Glide.with(this@WebTabFragment).asGif().load(R.drawable.loading_float).into(loadingWavy)
            loadingWavy.clipToOutline = true
            configureWebView(this)
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
        addChangeRouteCallBack()
        tabViewModel.userAgent.set(
            webTab?.getWebView()?.settings?.userAgentString ?: BrowserFragment.MOBILE_USER_AGENT
        )
        val message = webTab?.getMessage()
        if (message != null) {
            message.sendToTarget()
            webTab?.flushMessage()
        } else {
            webTab?.getUrl()?.let { tabViewModel.loadPage(it) }
        }

        wifiOnly = sharedPrefHelper.getDownloadWifi()

        return dataBinding.root
    }

    override fun shareWebLink() {
        val link = webTab?.getWebView()?.url
        if (link != null) {
            shareLink(link)
        }
    }

    override fun setIsDesktop(isDesktop: Boolean) {
        super.setIsDesktop(isDesktop)
        setUserAgentIsDesktop(isDesktop)
        webTab?.getWebView()?.reload()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!outState.isEmpty) {
            webTab?.getWebView()?.saveState(outState)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null && !savedInstanceState.isEmpty) {
            webTab?.getWebView()?.restoreState(savedInstanceState)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            handleIndexChangeEvent()
            handleLoadPageEvent()
            handleChangeTabFocusEvent()
            handleWorkerEvent()
            handleOpenDetectedVideos()
            handleVideoPushed()
            handleDownloadVideoEvent()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            tabViewModel.start()
            videoDetectionTabViewModel.start()
            dataBinding.tabCount.setOnClickListener {
                DownloaderModuleNavigator.mainViewModel?.openNavDrawerEvent?.call()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        hideBottomBarIfActiveWebTab()
    }

    override fun onPause() {
        super.onPause()
        onWebViewPause()
        backPressedCallback.remove()
    }

    override fun onResume() {
        super.onResume()
        onWebViewResume()
        hideBottomBarIfActiveWebTab()
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onDestroyView() {
        floatingLoadingResetJob?.cancel()
        floatingLoadingResetJob = null
        if (::dataBinding.isInitialized) {
            dataBinding.webviewContainer.removeView(webTab?.getWebView())
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingLoadingResetJob?.cancel()
        webTab?.let { tab ->
            AppLogger.d("onDestroy Webview::::::::: ${tab.getUrl()}")
            tab.getWebView()?.let { destroyWebView(it) }
            tab.setWebView(null)
        }

        webTab = null

        tabViewModel.stop()
        videoDetectionTabViewModel.stop()

        if (::tabManagerProvider.isInitialized) {
            tabManagerProvider
                .getTabsListChangeEvent()
                .removeOnPropertyChangedCallback(tabsListChangeListener)
        }
    }

    private fun handleOpenDetectedVideos() {
        videoDetectionTabViewModel.showDetectedVideosEvent.observe(viewLifecycleOwner) {
            navigateToDownloads()
        }
    }

    private fun handleVideoPushed() {
        videoDetectionTabViewModel.videoPushedEvent.observe(viewLifecycleOwner) {
            onVideoPushed()
        }
    }

    private fun onVideoPushed() {
//        showToastVideoFound()
//        val isDownloadsVisible = isDetectedVideosTabFragmentVisible()
//        val isCond = !tabViewModel.isDownloadDialogShown.get() && !isDownloadsVisible

//        if (context != null && mainActivity.settingsViewModel.getVideoAlertState().get() && isCond) {
//            lifecycleScope.launch(Dispatchers.Main) {
//                showAlertVideoFound()
//            }
//        }

    }

    private fun onVideoPreviewPropagate(
        videoInfo: VideoInfo, format: String, isForce: Boolean
    ) {
        AppLogger.d(
            "onPreviewVideo: ${videoInfo.formats}  $format"
        )
        val currFormat = videoInfo.formats.formats.filter {
            it.format?.contains(
                format
            ) ?: false
        }
        var intent: Intent? = null
        try {
            intent = Intent(
                requireContext(),
                Class.forName("com.video.avd.ui.player.PlayerVideoActivity")
            )
            var bundle = Bundle()
            bundle.putBoolean("isliveuri", true)
            bundle.putBoolean("isBgNotAllowed", true)
            bundle.putString("uri", currFormat.first().url.toString())
            intent.putExtras(bundle)
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun onVideoDownloadPropagate(videoInfo: VideoInfo, videoTitle: String, format: String) {
        val info = videoInfo.copy(
            title = FileNameCleaner.cleanFileName(videoTitle),
            formats = VideFormatEntityList(videoInfo.formats.formats.filter {
                it.format?.contains(
                    format
                ) ?: false
            })
        )

        DownloaderModuleNavigator.mainViewModel?.downloadVideoEvent?.value = info
//        if (info.isRegularDownload){
////            viewModel.addDownloadWithParams(info.title,info.formats.formats[0].url)
//            context?.let { CustomRegularDownloader.addDownload(it, info) }
//        }else{
//            Log.d("Tiktok_URL","${info.downloadUrls}")
//            context?.let { YoutubeDlDownloader.startDownload(it, info) }
//        }
//        mainActivity.mainViewModel.downloadVideoEvent.value = info
        context?.let {
            Toast.makeText(
                it,
                it.getString(R.string.download_started),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab?.getMessage() == null || webTab?.getWebView() == null) {
            webTab?.setWebView(WebView(requireContext()))
        }

        if (savedInstanceState != null) {
            webTab?.getWebView()?.restoreState(savedInstanceState)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(fragmentWebTabBinding: FragmentWebTabBinding) {
        try {
            val currentWebView = this.webTab?.getWebView()

            val webViewClient = DownloaderModuleNavigator.settingsViewModel?.let {
                CustomWebViewClient(
                    tabViewModel,
                    it,
                    videoDetectionTabViewModel,
                    historyProvider.getHistoryVModel(),
                    proxyController,
                    okHttpProxyClient,
                    tabManagerProvider.getUpdateTabEvent(),
                    pageTabProvider,
                    this,
                    { updateNextAndForward() },
                    { scheduleFloatingLoadingReset() }
                )
            }

            val chromeClient = DownloaderModuleNavigator.settingsViewModel?.let {
                CustomWebChromeClient(
                    tabViewModel,
                    it,
                    tabManagerProvider.getUpdateTabEvent(),
                    pageTabProvider,
                    fragmentWebTabBinding,
                    appUtil,
                    requireActivity()
                )
            }

            currentWebView?.webChromeClient = chromeClient
            if (webViewClient != null) {
                currentWebView?.webViewClient = webViewClient
            }

            val webSettings = webTab?.getWebView()?.settings
            val webView = webTab?.getWebView()

            // Use hardware acceleration for better GPU performance
            webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webView?.isScrollbarFadingEnabled = true

            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            webSettings?.apply {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(true)
                setSupportMultipleWindows(true)
                setGeolocationEnabled(false)
                allowContentAccess = true
                allowFileAccess = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Disable offscreen pre-rasterization to reduce GPU load and prevent rendering ANRs
                    offscreenPreRaster = false
                }
                displayZoomControls = false
                builtInZoomControls = true
                loadWithOverviewMode = true
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                useWideViewPort = true
                domStorageEnabled = true
                javaScriptEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                javaScriptCanOpenWindowsAutomatically = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
                mediaPlaybackRequiresUserGesture = false
                if (DownloaderModuleNavigator.settingsViewModel?.isDesktopMode?.get() == true) {
                    userAgentString = BrowserFragment.DESKTOP_USER_AGENT
                }
                // Enable hardware acceleration for better rendering performance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    setRenderPriority(WebSettings.RenderPriority.HIGH)
                }
                webView?.addJavascriptInterface(WebAppInterface(), "AndroidInterface")
            }
            fragmentWebTabBinding.webviewContainer.addView(
                webTab?.getWebView(),
                LinearLayout.LayoutParams(-1, -1)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onInputTabChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            val input = s.toString()

            tabViewModel.showTabSuggestions()
            tabViewModel.tabPublishSubject.onNext(input)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private val suggestionListener = object : SuggestionTabListener {
        override fun onItemClicked(suggestion: HistoryItem) {
            tabViewModel.loadPage(suggestion.url)
        }
    }

    private fun handleChangeTabFocusEvent() {
        var value = -1
        tabViewModel.changeTabFocusEvent.observe(viewLifecycleOwner) { isFocus ->
            isFocus.let {
                if (it) {
                    val oldValue = value
                    val start = dataBinding.search.selectionStart
                    val end = dataBinding.search.selectionEnd
                    value = (start + end) / 2
                    if (oldValue == value) {
                        dataBinding.search.selectAll()

                    }
                    tabViewModel.isTabInputFocused.set(true)
                    appUtil.showSoftKeyboard(dataBinding.search)
                } else {
                    tabViewModel.isTabInputFocused.set(false)
                    appUtil.hideSoftKeyboard(
                        dataBinding.search
                    )
                }
            }
        }
    }

    private fun handleLoadPageEvent() {
        try {
            tabViewModel.loadPageEvent.observe(viewLifecycleOwner) { tab ->
                if (tab.getUrl().startsWith("http")) {
                    webTab?.getWebView()?.stopLoading()
                    webTab?.getWebView()?.loadUrl(tab.getUrl())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleWorkerEvent() {
        try {
            workerEventProvider.getWorkerM3u8MpdEvent().observe(viewLifecycleOwner) { state ->
                if (state is DownloadButtonStateCanDownload && state.info?.id?.isNotEmpty() == true) {
                    videoDetectionTabViewModel.pushNewVideoInfoToAll(state.info)
                    val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
                    loadings?.remove("m3u8")
                    videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
                }
                if (state is DownloadButtonStateLoading) {
                    val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
                    loadings?.add("m3u8")
                    videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
                    videoDetectionTabViewModel.setButtonState(DownloadButtonStateLoading())
                }
                if (state is DownloadButtonStateCanNotDownload) {
                    val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
                    loadings?.remove("m3u8")
                    videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
                    videoDetectionTabViewModel.setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        } catch (e: Exception) {
           e.printStackTrace()
        }
    }

    private fun scheduleFloatingLoadingReset() {
        if (!isViewReadyForUiUpdate()) return
        floatingLoadingResetJob?.cancel()
        val owner = viewLifecycleOwner
        floatingLoadingResetJob = owner.lifecycleScope.launch {
            delay(1200L)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                videoDetectionTabViewModel.clearStaleLoadingIfNoVideos()
            }
        }
    }

    private fun isViewReadyForUiUpdate(): Boolean {
        return view != null &&
                viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)
    }

    private fun handleIndexChangeEvent() {
        try {
            tabManagerProvider.getTabsListChangeEvent()
                .addOnPropertyChangedCallback(tabsListChangeListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val tabsListChangeListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val tabs = tabManagerProvider.getTabsListChangeEvent().get()
            val webTab = tabs?.find { it.id == webTab?.id }
            val index = tabs?.indexOf(webTab)
            if (index != null && index in tabs.indices) {
                tabViewModel.thisTabIndex.set(index)
            }
        }
    }

    private fun onWebViewPause() {
        webTab?.getWebView()?.onPause()
    }

    private fun onWebViewResume() {
        webTab?.getWebView()?.onResume()
    }

    private fun hideBottomBarIfActiveWebTab() {
        if (isActiveBrowserWebTab()) {
            host?.hideBottomBar()
        }
    }

    private fun isActiveBrowserWebTab(): Boolean {
        return try {
            if (!::currentTabIndexProvider.isInitialized) return false

            val currentRoute = DownloaderModuleNavigator.mainViewModel?.currentItem?.get()
            val currentTabIndex = currentTabIndexProvider.getCurrentTabIndex().get()
            val thisTabIndex = arguments?.getInt(TAB_INDEX_KEY) ?: return false

            currentRoute == HOME_TAB_INDEX &&
                    currentTabIndex != HOME_TAB_INDEX &&
                    currentTabIndex == thisTabIndex
        } catch (e: Exception) {
            false
        }
    }

    private val tabListener = object : BrowserListener {

        override fun onBrowserMenuClicked() {
            buildWebTabMenu(dataBinding.imgSetting, false)
            showPopupMenu()
        }

        override fun onBrowserReloadClicked() {
            var url = webTab?.getWebView()?.url
            var urlWasChange = false

            if (url?.contains("m.facebook") == true) {
                url = url.replace("m.facebook", "www.facebook")
                urlWasChange = true
                val isDesktop = DownloaderModuleNavigator.settingsViewModel?.isDesktopMode!!.get()
                if (!isDesktop) {
                    DownloaderModuleNavigator.settingsViewModel?.setIsDesktopMode(true)
                }
            }

            val userAgent =
                webTab?.getWebView()?.settings?.userAgentString ?: tabViewModel.userAgent.get()
                ?: BrowserFragment.MOBILE_USER_AGENT
            if (url != null) {
                videoDetectionTabViewModel.viewModelScope.launch(videoDetectionTabViewModel.executorReload) {
                    videoDetectionTabViewModel.onStartPage(url, userAgent)
                }
                if (url.contains("www.facebook") && urlWasChange) {
                    tabViewModel.openPage(url)
                    webTab?.let { tabViewModel.closeTab(it) }
                } else {
                    tabViewModel.onPageReload(webTab?.getWebView())
                }
            }
        }

        override fun onTabCloseClicked() {
            webTab?.let { tabViewModel.closeTab(it) }
            videoDetectionTabViewModel.cancelAllCheckJobs()
        }

        override fun onBrowserStopClicked() {
            tabViewModel.onPageStop(webTab?.getWebView())
        }

        override fun onBrowserBackClicked() {
            val webView = getActiveWebView()
            val canGoBack = webView?.canGoBack()
            if (canGoBack == true) {
                tabViewModel.onGoBack(webView)
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }
            updateNextAndForward()
          /*  else {
                webTab?.let { tabViewModel.closeTab(it) }
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }*/
        }

        override fun onBrowserHomeClicked() {
            browserViewModel.selectWebTabEvent.value = WebTab("","Home Tab",null,id="home")
            try {
                host?.showBottomBar()
                host = null
            } catch (e: Exception) {
                Log.e("FragmentLifecycle", "Error in onDestroy: ${e.localizedMessage}", e)
            }
        }

        override fun onBrowserForwardClicked() {
            val webView = getActiveWebView()
            val canGoForward = webView?.canGoForward()
            if (canGoForward == true) {
                tabViewModel.onGoForward(webView)
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }
            updateNextAndForward()
        }
    }

    private fun getActiveWebView(): WebView? {
        return try {
            val currentIndex = tabViewModel.thisTabIndex.get()
            if (::pageTabProvider.isInitialized && currentIndex >= 0) {
                pageTabProvider.getPageTab(currentIndex).getWebView() ?: webTab?.getWebView()
            } else {
                webTab?.getWebView()
            }
        } catch (e: Exception) {
            webTab?.getWebView()
        }
    }

    // Replace your existing updateNextAndForward() function with this improved version

    fun updateNextAndForward() {
        val webView = getActiveWebView()
        val canGoForward = webView?.canGoForward() ?: false
        val canGoBack = webView?.canGoBack() ?: false

        Log.d("NavigationButtons", "canGoBack=$canGoBack, canGoForward=$canGoForward")

        updateBrowserNavigationButton(dataBinding.forward, canGoForward)
        updateBrowserNavigationButton(dataBinding.back3, canGoBack)
    }

    private fun updateBrowserNavigationButton(button: AppCompatImageView, isAvailable: Boolean) {
        val iconColor = if (isAvailable) {
            R.color.browser_nav_icon_enabled
        } else {
            R.color.browser_nav_icon_disabled
        }

        button.setColorFilter(
            ContextCompat.getColor(requireContext(), iconColor),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        button.alpha = 1f
        button.isEnabled = isAvailable
        button.isClickable = isAvailable
        button.isFocusable = isAvailable
    }
    @SuppressLint("WebViewApiAvailability")
    private fun showAlertVideoFound() {
        if (!tabViewModel.isDownloadDialogShown.get()) {
            tabViewModel.isDownloadDialogShown.set(true)
            val client = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webTab?.getWebView()?.webViewClient as CustomWebViewClient?
            } else {
                TODO("VERSION.SDK_INT < O")
            }

            client?.videoAlert =
                AlertDialog.Builder(requireContext()) // Changed to AlertDialog.Builder
                    .setTitle(R.string.video_found)
                    .setMessage(R.string.whatshould)
                    .setPositiveButton(R.string.view) { dialog, _ ->
                        navigateToDownloads()
                        tabViewModel.isDownloadDialogShown.set(false)
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.dontshow) { dialog, _ ->
                        DownloaderModuleNavigator.settingsViewModel?.setShowVideoAlertOff()
                        tabViewModel.isDownloadDialogShown.set(false)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.all_text_cancel) { dialog, _ ->
                        tabViewModel.isDownloadDialogShown.set(false)
                        dialog.dismiss()
                    }
                    .setOnDismissListener {
                        client?.videoAlert = null
                    }
                    .create() // Create the AlertDialog instance

            client?.videoAlert?.show()
        }
    }

    private fun handleOnBackPress() {
        val isBrowserRoute = DownloaderModuleNavigator.mainViewModel?.currentItem?.get() == 0
        val isCurrentTabSelected =
            currentTabIndexProvider.getCurrentTabIndex().get() == requireArguments().getInt(
                TAB_INDEX_KEY
            )
        val isStateResumed = viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED

        if (isStateResumed && isBrowserRoute && isCurrentTabSelected && isVisible && webTab?.getWebView()
                ?.canGoBack() == true
        ) {
            webTab?.getWebView()?.goBack()
        } else {
            webTab?.let { tabViewModel.closeTab(it) }
            videoDetectionTabViewModel.cancelAllCheckJobs()
        }
        updateNextAndForward()
    }

    private fun setUserAgentIsDesktop(isDesktop: Boolean) {
        val settings = webTab?.getWebView()?.settings
        if (isDesktop) {
            settings?.userAgentString = BrowserFragment.DESKTOP_USER_AGENT
        } else {
            settings?.userAgentString = null
        }
    }

    private fun addChangeRouteCallBack() {
        DownloaderModuleNavigator.mainViewModel?.currentItem?.removeOnPropertyChangedCallback(
            changeRouteCallBack
        )
        DownloaderModuleNavigator.mainViewModel?.currentItem?.addOnPropertyChangedCallback(
            changeRouteCallBack
        )
    }

    private val changeRouteCallBack = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val indexRoute = DownloaderModuleNavigator.mainViewModel?.currentItem?.get()
            val currentTabIndexSelected = currentTabIndexProvider.getCurrentTabIndex().get()
            val isCurrentTabSelected =
                currentTabIndexSelected == requireArguments().getInt(TAB_INDEX_KEY)
            val isBrowserRoute = indexRoute == 0
            val isNotHomeTabSelected = currentTabIndexSelected != HOME_TAB_INDEX
            val isVisible = this@WebTabFragment.isVisible
            if (isBrowserRoute && isNotHomeTabSelected && isCurrentTabSelected && isVisible) {
                activity?.onBackPressedDispatcher?.addCallback(
                    viewLifecycleOwner, backPressedCallback
                )
            } else {
                backPressedCallback.remove()
            }
        }
    }

    private fun showToastVideoFound() {
        val context = context

        if (context != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                videoToast?.cancel()
                videoToast = Toast.makeText(
                    context, context.getString(R.string.video_found), Toast.LENGTH_SHORT
                )
                videoToast?.show()
            }, 1)
        }
    }

    private fun destroyWebView(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        webTab?.setWebView(null)
    }

    private fun navigateToDownloads() {
        activity?.let { safeActivity ->
            /*if (AdBlockerHelper.mInterstitialAd != null) {
                AdBlockerHelper.showAppInterstitialAd(safeActivity, "DOWNLOAD_VIDEO") {
                    lifecycleScope.launch {
                        delay(300)
                        if (isAdded) {
                            showBottomSheet()
                        }

                    }
                }
            } else {
                AdBlockerHelper.showAppInterstitialAdNormal(safeActivity, "DOWNLOAD_VIDEO") {
                    lifecycleScope.launch {
                        delay(300)
                        if (isAdded) {
                            showBottomSheet()
                        }
                    }
                }
            }*/
            activity?.let { showInterstitialHome(activity = it){
                lifecycleScope.launch {
                    delay(300)
                    if (isAdded) {
                        showBottomSheet()
                    }
                }
            } }
        }
    }

    fun showBottomSheet() {
        try {
            val fragment = DetectedVideosTabFragment.newInstance()
            fragment.detectedVideosTabViewModel = videoDetectionTabViewModel
            fragment.candidateFormatListener = downloadListener
            fragment.show(requireActivity().supportFragmentManager, "DOWNLOADS_TAB")
        } catch (e: ClassCastException) {
            AppLogger.e("Can't get the fragment manager with this: ${e.message}")
        }
    }

    private fun isDetectedVideosTabFragmentVisible(): Boolean {
        val fragmentManager = requireActivity().supportFragmentManager
        val fragment =
            fragmentManager.findFragmentByTag("DOWNLOADS_TAB") as? DetectedVideosTabFragment
        return fragment != null && fragment.isAdded && fragment.isVisible && fragment.isResumed
    }

    private val downloadListener = object : DownloadTabListener {
        override fun onCancel() {
            requireActivity().supportFragmentManager.popBackStack()
        }

        override fun onPreviewVideo(
            videoInfo: VideoInfo, format: String, isForce: Boolean
        ) {
            onVideoPreviewPropagate(videoInfo, format, isForce)
        }

        override fun onDownloadVideo(
            videoInfo: VideoInfo, format: String, videoTitle: String
        ) {

            val ctx = context ?: return
            if (!isAdded) return

            if (!NetworkUtils.isOnline(ctx)) {
                Toast.makeText(ctx, getString(R.string.waiting_for_network), Toast.LENGTH_SHORT)
                    .show()
                return
            }
            if (wifiOnly) {
                if (ctx.isWifiConnected()) {
                    onVideoDownloadPropagate(videoInfo, videoTitle, format)
                } else {
                    Toast.makeText(ctx, "Wi-Fi required for downloads", Toast.LENGTH_SHORT).show()
                }
            } else {
                onVideoDownloadPropagate(videoInfo, videoTitle, format)
            }
        }

        override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
            val formats =
                videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
            formats[videoInfo.id] = format
            videoDetectionTabViewModel.selectedFormats.set(formats)
        }

    }

    private fun showProgressFragment() {
        dataBinding.fullscreenContainer.visibility = View.VISIBLE
        val fragmentManager = activity?.supportFragmentManager
        val fragmentTransaction = fragmentManager?.beginTransaction()

        // Create a new instance of the ProgressFragment and add it to the FrameLayout.
        val progressFragment = ProgressFragment()
        fragmentTransaction?.replace(R.id.fullscreen_container, progressFragment)
        fragmentTransaction?.addToBackStack(null)  // Optional, to allow back navigation.
        fragmentTransaction?.commit()
    }


    private fun showVidFragment() {
        dataBinding.fullscreenContainer.visibility = View.VISIBLE
        val fragmentManager = activity?.supportFragmentManager
        val fragmentTransaction = fragmentManager?.beginTransaction()
        // Create a new instance of the ProgressFragment and add it to the FrameLayout.
        val progressFragment = VideoFragment()
        fragmentTransaction?.replace(R.id.fullscreen_container, progressFragment)
        fragmentTransaction?.addToBackStack(null)  // Optional, to allow back navigation.
        fragmentTransaction?.commit()
    }

    private fun handleDownloadVideoEvent() {
        mainViewModel.downloadVideoEvent.observe(viewLifecycleOwner) { videoInfo ->
            progressViewModel.start()
            val currentOriginal = videoInfo.originalUrl
            mainViewModel.currentOriginal.set(currentOriginal)
            progressViewModel.downloadVideo(videoInfo)
        }
    }
// Removed Dialog for YouTube detection as per your request. If you want to show a dialog, you can uncomment the code below.
    fun showYouTubeDialog() {
       /* AlertDialog.Builder(requireContext())
            .setTitle("YouTube Detected")
            .setMessage("You are viewing a YouTube page. Certain features like downloading are not available.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()*/
    }

    var isshown = false

    override fun isYoutubeOpen(visibility: Boolean) {
        if (!isViewReadyForUiUpdate()) return
        if (visibility) {
            if (!isshown) {
                isshown = true
                showYouTubeDialog()
                Log.d("btnGone", "YoutubeOPenbtnGone")
                dataBinding.floatingContainer.visibility = View.GONE
            }
        } else {
            if (isshown) {
                isshown = false
                Log.d("btnGone", "YoutubeOPenbtnvisible")
                dataBinding.floatingContainer.visibility = View.VISIBLE
            }
        }
    }

    fun isYouTubeVideoLink(url: String): Boolean {
        return url.contains("youtu.be", ignoreCase = true)
    }

    override fun isFaceBookOpen(visibility: Boolean) {
        if (!isViewReadyForUiUpdate()) return
        if (visibility) {
            //  if (!YoutubeDlDownloader.isFaceBook) {
            YoutubeDlDownloader.isFaceBook = true
            Log.d("btnGone", "Preparing to execute Facebook JS")
            dataBinding.floatingContainer.visibility = View.GONE
            Log.d("btnGone", "facebookOPenbtnGone")
            //   }
        } else {
            if (YoutubeDlDownloader.isFaceBook) {
                YoutubeDlDownloader.isFaceBook = false
                Log.d("btnGone", "facebookOPenbtnVisible")
                dataBinding.floatingContainer.visibility = View.VISIBLE
            }
        }
    }

    inner class WebAppInterface() {
        @JavascriptInterface
        fun onVideoClicked(url: String) {
            try {
                progressViewModel.start()
                val currentOriginal = url
                NotificationsHelper.pendingurl = url
                mainViewModel.currentOriginal.set(currentOriginal)
                val video = VideoInfoWrapper(
                    VideoInfo(
                        title = "face_book",
                        ext = "mp4",
                        originalUrl = url,
                        formats = VideFormatEntityList(
                            mutableListOf(
                                VideoFormatEntity(
                                    formatId = "0",
                                    format = ContextUtils.getApplicationContext()
                                        .getString(R.string.player_resolution),
                                    ext = "mp4",
                                    url = url,
                                    fileSize = 0
                                )
                            )
                        ),
                        isRegularDownload = false
                    )
                )
                if (wifiOnly) {
                    if (requireContext().isWifiConnected()) {
                        progressViewModel.downloadVideo(video.videoInfo)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Wi-Fi required for downloads",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    progressViewModel.downloadVideo(video.videoInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun Context.isWifiConnected(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

}
