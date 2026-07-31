package com.avd.browserkit.ui.browser

import android.app.Activity
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import android.view.inputmethod.EditorInfo
import kotlin.math.abs
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.avd.browserkit.R
import com.avd.browserkit.api.BrowserKit
import com.avd.browserkit.data.BrowserRepository
import com.avd.browserkit.databinding.BkFragmentWebTabBinding
import com.avd.browserkit.detection.DownloadButtonState
import com.avd.browserkit.detection.VideoDetectionViewModel
import com.avd.browserkit.detection.VideoDetectorJsBridge
import com.avd.browserkit.download.BrowserDownloadManager
import com.avd.browserkit.ui.dialog.BrowserDownloadsDialog
import com.avd.browserkit.ui.dialog.BrowserMenuBottomSheet
import com.avd.browserkit.ui.dialog.BrowserMenuListener
import com.avd.browserkit.ui.dialog.FindInPageBottomSheet
import com.avd.browserkit.ui.dialog.FindInPageListener
import com.avd.browserkit.ui.dialog.FormatPickerDialog
import com.avd.browserkit.ui.history.BrowserHistoryActivity
import com.avd.browserkit.ui.dialog.AdultBlockedDialog
import com.avd.browserkit.ui.dialog.YoutubeBlockedDialog
import com.avd.browserkit.util.AdultSiteBlocker
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.BrowserSiteUtils
import com.avd.browserkit.util.CookieUtils
import com.avd.browserkit.util.DailymotionUrlUtils
import com.avd.browserkit.util.FacebookUrlUtils
import com.avd.browserkit.util.InstagramUrlUtils
import com.avd.browserkit.util.UrlUtils
import com.avd.browserkit.util.YoutubeUrlUtils
import com.avd.browserkit.util.VideoUtils
import com.avd.browserkit.detection.DetectedVideoInfo
import com.avd.browserkit.detection.StreamFormat
import com.avd.browserkit.detection.StreamType
import com.avd.browserkit.ui.dialog.BrowserDialogBuilders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WebTabFragment : Fragment() {

    private var _binding: BkFragmentWebTabBinding? = null
    private val binding get() = _binding!!

    private val browserViewModel: BrowserViewModel by activityViewModels()
    private val detectionViewModel: VideoDetectionViewModel by viewModels()

    private var tabIndex: Int = 0
    private var currentPageUrl: String = "about:blank"
    private var addressEditing = false
    private var desktopMode = BrowserKit.getConfig().useDesktopMode
    private var isFacebookPage = false
    private var isInstagramPage = false
    private var isYouTubePage = false
    /** Debounce YouTube warning dialog (SPA URL churn). */
    private var lastYouTubeWarningAtMs: Long = 0L
    /** Last host logged for browser_site_view (avoid SPA spam). */
    private var lastAnalyticsSiteHost: String = ""
    private var facebookRescanRunnable: Runnable? = null
    private var instagramRescanRunnable: Runnable? = null
    /** Delayed page-finished work — must be cancelled in [onDestroyView]. */
    private var pendingScanRunnable: Runnable? = null
    private var pendingPreviewRunnable: Runnable? = null
    private val pendingDmRescanRunnables = ArrayList<Runnable>(4)
    private val pendingFbInjectRunnables = ArrayList<Runnable>(2)
    private val pendingIgInjectRunnables = ArrayList<Runnable>(2)
    /** True after desktop UA applied for Facebook (no stopLoading reload). */
    private var facebookDesktopUaApplied = false
    /** Guard against multi-fire JS bridge (same tap → many enqueues). */
    private var lastFacebookEnqueueAtMs: Long = 0L
    private var lastFacebookEnqueueUrl: String = ""
    /** Recent FB CDN media URLs from network intercept — fallback when click gets page/blob URL. */
    private val recentFacebookMediaUrls = ArrayDeque<String>(24)
    private val recentInstagramMediaUrls = ArrayDeque<String>(24)
    private lateinit var repository: BrowserRepository

    private val historyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val url = result.data?.getStringExtra(BrowserHistoryActivity.EXTRA_RESULT_URL).orEmpty()
        if (url.isNotBlank()) loadUrl(url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabIndex = requireArguments().getInt(ARG_TAB_INDEX, 0)
        repository = BrowserRepository(requireContext())
        BrowserDownloadManager.init(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val themedInflater = inflater.cloneInContext(ContextThemeWrapper(requireContext(), R.style.Theme_BrowserKit))
        _binding = BkFragmentWebTabBinding.inflate(themedInflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detectionViewModel.setUserAgent(userAgent())
        setupServiceWorkerInterceptor()
        setupWebView()
        setupControls()
        observeDetection()

        val initialUrl = browserViewModel.tabs.value?.getOrNull(tabIndex)?.url ?: "about:blank"
        if (initialUrl != "about:blank") {
            loadUrl(initialUrl)
        }

        browserViewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            val url = tabs.getOrNull(tabIndex)?.url ?: return@observe
            if (url != "about:blank" && (currentPageUrl == "about:blank" || binding.webView.url == "about:blank")) {
                loadUrl(url)
            }
        }
        browserViewModel.tabsSwitcherVisible.observe(viewLifecycleOwner) {
            refreshChrome()
        }
    }

    private fun logBrowserSiteViewIfNeeded(pageUrl: String) {
        val site = BrowserSiteUtils.siteNameFromUrl(pageUrl)
        if (site.isBlank() || site == "unknown") return
        if (site == lastAnalyticsSiteHost) return
        lastAnalyticsSiteHost = site
        BrowserKit.analytics()?.onBrowserSiteView(site = site, pageUrl = pageUrl)
    }

    /**
     * Google Safe Browsing — malware / phishing / unwanted software.
     * Does not block adult sites; keeps Google's threat interstitial / back-to-safety path.
     */
    private fun enableSafeBrowsing(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, true)
            BrowserKitLog.i("SafeBrowsing", "enabled on WebView settings")
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(requireContext().applicationContext) { success ->
                BrowserKitLog.i("SafeBrowsing", "init success=$success")
            }
        }
    }

    private fun setupServiceWorkerInterceptor() {
        runCatching {
            val controller = ServiceWorkerController.getInstance()
            controller.setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): android.webkit.WebResourceResponse? {
                    val url = request.url?.toString().orEmpty()
                    val page = currentPageUrl
                    // Browse-only: skip download detection on FB / IG / YouTube.
                    if (FacebookUrlUtils.isFacebookUrl(page) ||
                        InstagramUrlUtils.isInstagramUrl(page) ||
                        YoutubeUrlUtils.isYouTubeUrl(page)
                    ) {
                        return super.shouldInterceptRequest(request)
                    }
                    if (url.startsWith("http") &&
                        (
                            VideoUtils.streamTypeFromUrl(url) != null ||
                                VideoUtils.looksLikeMediaUrl(url)
                            )
                    ) {
                        detectionViewModel.onMediaCandidate(url, page)
                    }
                    return super.shouldInterceptRequest(request)
                }
            })
            controller.serviceWorkerWebSettings.allowContentAccess = true
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgent()
        }
        enableSafeBrowsing(binding.webView)
        // Persist site logins (FB etc.) across app restarts via CookieManager disk store.
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)
        val jsBridge = VideoDetectorJsBridge(
            onMediaUrl = { mediaUrl ->
                if (InstagramUrlUtils.isInstagramMediaUrl(mediaUrl)) {
                    rememberInstagramMediaUrl(mediaUrl)
                }
                detectionViewModel.onMediaCandidate(mediaUrl, currentPageUrl)
            },
            onVideoClicked = { videoUrl -> onBrowserVideoClicked(videoUrl) },
        )
        // Xilli uses AndroidInterface; we keep both names.
        binding.webView.addJavascriptInterface(jsBridge, "BrowserKitDetector")
        binding.webView.addJavascriptInterface(jsBridge, "AndroidInterface")
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.pageProgress.isVisible = newProgress in 1..99
                binding.pageProgress.progress = newProgress
                if (newProgress >= 100) binding.pageProgress.isVisible = false
                updateNavButtons()
            }
        }
        binding.webView.webViewClient = BrowserWebViewClient(
            onPageStarted = pageStarted@{ url ->
                if (AdultSiteBlocker.isBlocked(url)) {
                    BrowserKitLog.w("Adult", "pageStarted blocked ${BrowserKitLog.shortUrl(url)}")
                    leaveBlockedAdultPage(url)
                    return@pageStarted
                }
                val normalized = maybeRewriteFacebookUrl(url)
                if (normalized != null && normalized != url) {
                    // Rewrite m. → www without stopLoading (abort → bounce back to Google).
                    BrowserKitLog.i("FB", "rewrite ${BrowserKitLog.shortUrl(url)} → ${BrowserKitLog.shortUrl(normalized)}")
                    binding.webView.loadUrl(normalized)
                    return@pageStarted
                }
                currentPageUrl = url
                detectionViewModel.onPageStarted(url)
                binding.pageProgress.isVisible = true
                if (!addressEditing) showAddress(url)
                updateNavButtons()
                applyFacebookPageState(url)
                applyInstagramPageState(url)
                applyYouTubePageState(url)
            },
            onPageFinished = { url, title ->
                currentPageUrl = url
                detectionViewModel.onPageTitle(title)
                if (!addressEditing) showAddress(url)
                browserViewModel.updateTab(tabIndex, title.orEmpty(), url)
                binding.pageProgress.isVisible = false
                updateNavButtons()
                scheduleCapturePreview()
                applyFacebookPageState(url)
                applyInstagramPageState(url)
                applyYouTubePageState(url)
                BrowserKitLog.i(
                    "Page",
                    "finished fb=$isFacebookPage ig=$isInstagramPage yt=$isYouTubePage " +
                        "desktop=$desktopMode url=${BrowserKitLog.shortUrl(url)}",
                )
                if (UrlUtils.isHttpUrl(url)) {
                    logBrowserSiteViewIfNeeded(url)
                    lifecycleScope.launch { repository.addHistory(title.orEmpty(), url) }
                    when {
                        isFacebookPage -> {
                            // Browse only — no download buttons / FAB / detect.
                            BrowserKitLog.d("Page", "FB browse-only (download UI off)")
                            stopInstagramRescan()
                            stopFacebookRescan()
                            clearPendingFbInjects()
                            clearPendingIgInjects()
                            clearPendingPageScan()
                            clearPendingDmRescans()
                            ensureFacebookDesktopUserAgent(url)
                            hideDownloadFab()
                        }
                        isInstagramPage -> {
                            BrowserKitLog.d("Page", "IG browse-only (download UI off)")
                            stopFacebookRescan()
                            stopInstagramRescan()
                            clearPendingFbInjects()
                            clearPendingIgInjects()
                            clearPendingPageScan()
                            clearPendingDmRescans()
                            hideDownloadFab()
                        }
                        isYouTubePage -> {
                            BrowserKitLog.d("Page", "YT browse-only (download UI off)")
                            stopFacebookRescan()
                            stopInstagramRescan()
                            clearPendingFbInjects()
                            clearPendingIgInjects()
                            clearPendingPageScan()
                            clearPendingDmRescans()
                            hideDownloadFab()
                            maybeShowYouTubeWarning()
                        }
                        else -> {
                            stopFacebookRescan()
                            stopInstagramRescan()
                            clearPendingFbInjects()
                            clearPendingIgInjects()
                            scanPageForVideos()
                            scheduleDelayedPageScan()
                            if (DailymotionUrlUtils.isDailymotionUrl(url)) {
                                scheduleDailymotionRescans(url)
                            } else {
                                clearPendingDmRescans()
                            }
                            detectionViewModel.verifyPageWithYtDlp(url)
                        }
                    }
                }
            },
            onRequestIntercepted = { requestUrl, pageUrl ->
                // No download detection on FB / IG / YouTube (browse-only).
                val browseOnly = FacebookUrlUtils.isFacebookUrl(pageUrl) ||
                    InstagramUrlUtils.isInstagramUrl(pageUrl) ||
                    YoutubeUrlUtils.isYouTubeUrl(pageUrl)
                if (!browseOnly) {
                    detectionViewModel.onMediaCandidate(requestUrl, pageUrl)
                }
            },
            onNavigate = nav@{ url ->
                if (shouldHandleExternally(url)) {
                    return@nav handleExternalNavigation(url)
                }
                if (AdultSiteBlocker.isBlocked(url)) {
                    BrowserKitLog.w("Adult", "nav blocked ${BrowserKitLog.shortUrl(url)}")
                    showAdultBlockedDialog()
                    return@nav true
                }
                // Facebook: set desktop UA BEFORE the navigation (no later stopLoading reload).
                if (FacebookUrlUtils.isFacebookUrl(url)) {
                    applyFacebookDesktopUaOnly()
                    val rewritten = maybeRewriteFacebookUrl(url)
                    if (rewritten != null && rewritten != url) {
                        BrowserKitLog.i(
                            "FB",
                            "nav rewrite ${BrowserKitLog.shortUrl(url)} → ${BrowserKitLog.shortUrl(rewritten)}",
                        )
                        binding.webView.loadUrl(rewritten)
                        return@nav true
                    }
                    // Let WebView continue — UA already desktop.
                    return@nav false
                }
                if (YoutubeUrlUtils.isYouTubeUrl(url)) {
                    // Allow YouTube — warn only (do not block navigation).
                    maybeShowYouTubeWarning()
                    applyWebViewUserAgent()
                    return@nav false
                }
                applyWebViewUserAgent()
                false
            },
            currentPageUrl = { currentPageUrl },
            onHistoryChanged = { historyUrl ->
                updateNavButtons()
                handleSpaNavigation(historyUrl)
            },
        )
    }

    private fun shouldHandleExternally(url: String): Boolean {
        if (url.isBlank() || url == "about:blank") return false
        val lower = url.lowercase()
        return !UrlUtils.isHttpUrl(url) &&
            !lower.startsWith("javascript:") &&
            !lower.startsWith("data:") &&
            !lower.startsWith("blob:")
    }

    private fun handleExternalNavigation(url: String): Boolean {
        return runCatching {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = null
                selector = null
            }

            val packageManager = requireContext().packageManager
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")

            when {
                intent.resolveActivity(packageManager) != null -> {
                    startActivity(intent)
                }
                !fallbackUrl.isNullOrBlank() -> {
                    loadUrl(fallbackUrl)
                }
                else -> {
                    BrowserKitLog.w("Nav", "ignored external url ${BrowserKitLog.shortUrl(url)}")
                }
            }
            true
        }.getOrElse {
            BrowserKitLog.w("Nav", "external nav failed ${BrowserKitLog.shortUrl(url)} error=${it.message}")
            false
        }
    }

    /**
     * IG/FB/YT SPA URL change — update page flags + keep download UI hidden (no script inject).
     */
    private fun handleSpaNavigation(historyUrl: String) {
        if (_binding == null || historyUrl.isBlank() || historyUrl == "about:blank") return
        if (AdultSiteBlocker.isBlocked(historyUrl)) {
            BrowserKitLog.w("Adult", "SPA blocked ${BrowserKitLog.shortUrl(historyUrl)}")
            leaveBlockedAdultPage(historyUrl)
            return
        }
        val prev = currentPageUrl
        val sameDoc = urlsSameDocument(prev, historyUrl)
        if (historyUrl != prev) {
            currentPageUrl = historyUrl
            if (!addressEditing) showAddress(historyUrl)
            browserViewModel.updateTab(tabIndex, binding.webView.title.orEmpty(), historyUrl)
            if (UrlUtils.isHttpUrl(historyUrl)) {
                logBrowserSiteViewIfNeeded(historyUrl)
            }
        }
        when {
            InstagramUrlUtils.isInstagramUrl(historyUrl) -> {
                val reelChanged = !sameDoc || !InstagramUrlUtils.isInstagramUrl(prev)
                applyInstagramPageState(historyUrl)
                if (reelChanged) {
                    BrowserKitLog.i("IG", "SPA reel change ${BrowserKitLog.shortUrl(historyUrl)}")
                    detectionViewModel.onPageStarted(historyUrl)
                    synchronized(recentInstagramMediaUrls) { recentInstagramMediaUrls.clear() }
                }
                hideDownloadFab()
            }
            FacebookUrlUtils.isFacebookUrl(historyUrl) -> {
                val reelChanged = !sameDoc || !FacebookUrlUtils.isFacebookUrl(prev)
                applyFacebookPageState(historyUrl)
                if (reelChanged) {
                    BrowserKitLog.i("FB", "SPA reel change ${BrowserKitLog.shortUrl(historyUrl)}")
                    detectionViewModel.onPageStarted(historyUrl)
                    synchronized(recentFacebookMediaUrls) { recentFacebookMediaUrls.clear() }
                }
                hideDownloadFab()
            }
            YoutubeUrlUtils.isYouTubeUrl(historyUrl) -> {
                applyYouTubePageState(historyUrl)
                hideDownloadFab()
            }
            DailymotionUrlUtils.isDailymotionUrl(historyUrl) -> {
                // SPA: home → /video/… often skips full reload — re-run detect.
                val videoChanged = !sameDoc || !DailymotionUrlUtils.isDailymotionUrl(prev)
                if (videoChanged || historyUrl != prev) {
                    BrowserKitLog.i("DM", "SPA nav ${BrowserKitLog.shortUrl(historyUrl)}")
                    detectionViewModel.onPageStarted(historyUrl)
                    scanPageForVideos()
                    scheduleDelayedPageScan()
                    scheduleDailymotionRescans(historyUrl)
                    detectionViewModel.verifyPageWithYtDlp(historyUrl)
                }
            }
        }
    }

    private fun urlsSameDocument(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        fun norm(u: String): String =
            u.substringBefore('#').substringBefore('?').trimEnd('/').lowercase()
        return norm(a) == norm(b)
    }

    private fun applyFacebookPageState(url: String) {
        val fb = FacebookUrlUtils.isFacebookUrl(url)
        val wasFb = isFacebookPage
        isFacebookPage = fb
        if (_binding == null) return
        if (fb) {
            // Browse only — no FAB / overlay download buttons.
            hideDownloadFab()
            stopFacebookRescan()
            clearPendingFbInjects()
            if (!wasFb) {
                synchronized(recentFacebookMediaUrls) { recentFacebookMediaUrls.clear() }
            }
            ensureFacebookDesktopUserAgent(url)
        } else {
            stopFacebookRescan()
            clearPendingFbInjects()
            synchronized(recentFacebookMediaUrls) { recentFacebookMediaUrls.clear() }
            if (wasFb) {
                // Leaving FB — restore menu desktop/mobile preference.
                applyWebViewUserAgent()
            }
        }
    }

    /** Set desktop UA only — never stopLoading (that bounced FB → Google history). */
    private fun applyFacebookDesktopUaOnly() {
        if (_binding == null) return
        val desktopUa = BrowserKit.getConfig().desktopUserAgent
        if (binding.webView.settings.userAgentString != desktopUa) {
            binding.webView.settings.userAgentString = desktopUa
            BrowserKitLog.i("FB", "desktop UA applied (no reload)")
        }
        detectionViewModel.setUserAgent(desktopUa)
        facebookDesktopUaApplied = true
    }

    /**
     * Xilli: desktop UA for data-video-id markup.
     * Applied on navigate before load — no stopLoading / no forced /watch/ reload.
     */
    private fun ensureFacebookDesktopUserAgent(url: String) {
        if (_binding == null || !isFacebookPage) return
        applyFacebookDesktopUaOnly()
    }

    private fun applyInstagramPageState(url: String) {
        val ig = InstagramUrlUtils.isInstagramUrl(url)
        val wasIg = isInstagramPage
        isInstagramPage = ig
        if (_binding == null) return
        if (ig) {
            // Browse only — no FAB / overlay download buttons.
            hideDownloadFab()
            stopInstagramRescan()
            clearPendingIgInjects()
            applyWebViewUserAgent()
            if (!wasIg) {
                synchronized(recentInstagramMediaUrls) { recentInstagramMediaUrls.clear() }
            }
        } else {
            stopInstagramRescan()
            clearPendingIgInjects()
            synchronized(recentInstagramMediaUrls) { recentInstagramMediaUrls.clear() }
        }
    }

    private fun applyYouTubePageState(url: String) {
        val yt = YoutubeUrlUtils.isYouTubeUrl(url)
        isYouTubePage = yt
        if (_binding == null) return
        if (yt) {
            hideDownloadFab()
            stopFacebookRescan()
            stopInstagramRescan()
            clearPendingFbInjects()
            clearPendingIgInjects()
            clearPendingPageScan()
        }
    }

    private fun hideDownloadFab() {
        if (_binding == null) return
        binding.fabDownload.isVisible = false
        binding.fabDownloadLoading.isVisible = false
        setFabLoading(false)
    }

    private fun applyWebViewUserAgent() {
        if (_binding == null) return
        val ua = userAgent()
        if (binding.webView.settings.userAgentString != ua) {
            binding.webView.settings.userAgentString = ua
        }
        detectionViewModel.setUserAgent(ua)
    }

    /** m.facebook → www (Xilli). Desktop UA provides data-video-id DOM. */
    private fun maybeRewriteFacebookUrl(url: String): String? {
        if (!FacebookUrlUtils.isFacebookUrl(url)) return null
        val lower = url.lowercase()
        return when {
            lower.contains("m.facebook.com") ->
                url.replace(Regex("(?i)m\\.facebook\\.com"), "www.facebook.com")
            lower.contains("mbasic.facebook.com") ->
                url.replace(Regex("(?i)mbasic\\.facebook\\.com"), "www.facebook.com")
            else -> null
        }
    }

    /** Download overlay scripts disabled — browse-only on FB/IG. */
    private fun injectFacebookScript() = Unit

    /** Download overlay scripts disabled — browse-only on FB/IG. */
    private fun injectInstagramScript() = Unit

    private fun scheduleFacebookRescan() {
        // No-op: FB download buttons removed.
        stopFacebookRescan()
    }

    private fun scheduleInstagramRescan() {
        // No-op: IG download buttons removed.
        stopInstagramRescan()
    }

    private fun scheduleInstagramInjectRetries() {
        // No-op: IG download buttons removed.
        clearPendingIgInjects()
    }

    private fun clearPendingIgInjects() {
        val v = view
        pendingIgInjectRunnables.forEach { r -> v?.removeCallbacks(r) }
        pendingIgInjectRunnables.clear()
    }

    private fun stopFacebookRescan() {
        val runnable = facebookRescanRunnable ?: return
        _binding?.webView?.removeCallbacks(runnable)
        facebookRescanRunnable = null
    }

    private fun stopInstagramRescan() {
        val runnable = instagramRescanRunnable ?: return
        _binding?.webView?.removeCallbacks(runnable)
        instagramRescanRunnable = null
    }

    private fun looksLikeFacebookMediaUrl(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false
        val lower = url.lowercase()
        // Skip static assets / manifests — not playable video CDN.
        if (lower.contains("btmanifest") || lower.contains("rsrc.php") ||
            lower.contains("static.xx.fbcdn") || lower.contains("/images/") ||
            lower.contains(".js") || lower.contains(".css") ||
            lower.contains(".jpg") || lower.contains(".png") || lower.contains(".webp")
        ) {
            return false
        }
        return lower.contains("scontent") && lower.contains("fbcdn") ||
            lower.contains("/o1/v/") ||
            lower.contains("fbsbx.com") ||
            (lower.contains("fbcdn") && (lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains("/v/t"))) ||
            (lower.contains("facebook.com") && (lower.contains(".mp4") || lower.contains(".m3u8")))
    }

    private fun rememberFacebookMediaUrl(url: String) {
        if (!url.startsWith("http", ignoreCase = true)) return
        if (!looksLikeFacebookMediaUrl(url)) return
        synchronized(recentFacebookMediaUrls) {
            recentFacebookMediaUrls.remove(url)
            // Prefer video-ish CDN (m412/m69/.mp4) at front over audio (m366) / junk.
            if (isLikelyFacebookVideoCdn(url)) {
                recentFacebookMediaUrls.addFirst(url)
            } else {
                recentFacebookMediaUrls.addLast(url)
            }
            while (recentFacebookMediaUrls.size > 24) {
                recentFacebookMediaUrls.removeLast()
            }
        }
        // shouldInterceptRequest is off-main — never touch WebView here.
        // Only bind progressive .mp4 — extensionless /m412/ is often DASH moof.
        if (isProgressiveFacebookCdn(url)) {
            pushFacebookCdnToJs(url)
        }
    }

    private fun latestFacebookMediaUrl(): String? {
        synchronized(recentFacebookMediaUrls) {
            return recentFacebookMediaUrls.firstOrNull { isLikelyFacebookVideoCdn(it) }
                ?: recentFacebookMediaUrls.firstOrNull()
        }
    }

    /**
     * Only real progressive file URLs. Extensionless `/m78/` `/m412/` are CMAF moof stubs —
     * never treat as downloadable (causes empty/black files).
     */
    private fun isLikelyFacebookVideoCdn(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("/m366/")) return false
        return lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".m4v")
    }

    private fun isProgressiveFacebookCdn(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".m4v")
    }


    private fun pushFacebookCdnToJs(url: String) {
        if (!isFacebookPage) return
        val webView = _binding?.webView ?: return
        val escaped = url
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "")
            .replace("\r", "")
        val js = """
            (function(){
              window.__avdFbLatestCdn='$escaped';
              var btns=document.querySelectorAll('button[data-avd-fb-dl="1"]');
              for(var i=0;i<btns.length;i++){
                var cur=btns[i].getAttribute('data-avd-url')||'';
                if(!cur || cur.indexOf('http')!==0){
                  btns[i].setAttribute('data-avd-url', window.__avdFbLatestCdn);
                }
              }
            })();
        """.trimIndent()
        // Intercept thread → must hop to WebView/main thread.
        webView.post {
            if (_binding == null || !isFacebookPage) return@post
            _binding?.webView?.evaluateJavascript(js, null)
        }
    }


    private fun onBrowserVideoClicked(videoUrl: String) {
        BrowserKitLog.i(
            "Click",
            "url=${BrowserKitLog.shortUrl(videoUrl)} fb=$isFacebookPage ig=$isInstagramPage " +
                "yt=$isYouTubePage page=${BrowserKitLog.shortUrl(currentPageUrl)}",
        )
        // Browse-only sites — ignore overlay / bridge download clicks.
        if (isFacebookPage || isInstagramPage || isYouTubePage ||
            YoutubeUrlUtils.isYouTubeUrl(videoUrl) ||
            InstagramUrlUtils.isInstagramUrl(videoUrl) ||
            InstagramUrlUtils.isInstagramMediaUrl(videoUrl) ||
            looksLikeFacebookMediaUrl(videoUrl)
        ) {
            BrowserKitLog.w("Click", "download disabled for this site")
            if (isYouTubePage || YoutubeUrlUtils.isYouTubeUrl(videoUrl)) {
                maybeShowYouTubeWarning()
            }
            return
        }
        detectionViewModel.onMediaCandidate(videoUrl, currentPageUrl)
    }

    /**
     * FB download (Super-aligned):
     * 1) RegularMp4 verified (video + >5MB progressive) → CustomRegular
     * 2) else progressive `.mp4` CDN only → yt-dlp facebookMode
     * 3) else yt-dlp on **page/reel URL** (never moof /m78/ stubs)
     */
    private fun onFacebookVideoClicked(videoUrl: String) {
        val pageUrl = currentPageUrl
        val clicked = videoUrl.trim()
            .replace("&amp;", "&")
            .let { if (it.startsWith("//")) "https:$it" else it }
        BrowserKitLog.i(
            "FB",
            "click rawLen=${clicked.length} page=${BrowserKitLog.shortUrl(pageUrl)}",
        )
        lifecycleScope.launch {
            // Wait briefly — RegularMp4 checks run on intercept threads.
            var verified = detectionViewModel.bestVerifiedRegular()
            if (verified == null) {
                delay(1200)
                verified = detectionViewModel.bestVerifiedRegular()
            }
            if (verified != null) {
                BrowserKitLog.i(
                    "FB",
                    "RegularMp4 → CustomRegular len=${verified.contentLength} " +
                        "url=${BrowserKitLog.shortUrl(verified.finalUrl)}",
                )
                enqueueFacebookDownload(
                    url = verified.finalUrl,
                    pageUrl = pageUrl,
                    regular = true,
                    headersExtra = verified.headers,
                )
                return@launch
            }

            // Progressive .mp4 CDN only (never extensionless moof paths).
            val webView = _binding?.webView
            val scraped = webView?.let { scrapeFacebookPlayableUrl(it) }
            val progressiveCdn = sequenceOf(
                scraped,
                clicked.takeIf { it.startsWith("http") && !isFacebookWatchablePage(it) },
                latestFacebookMediaUrl(),
            ).firstOrNull { !it.isNullOrBlank() && isProgressiveFacebookCdn(it!!) }

            if (!progressiveCdn.isNullOrBlank()) {
                BrowserKitLog.i(
                    "FB",
                    "progressive CDN → YoutubeDl ${BrowserKitLog.shortUrl(progressiveCdn)}",
                )
                enqueueFacebookDownload(url = progressiveCdn, pageUrl = pageUrl, regular = false)
                return@launch
            }

            // Super-style: page URL yt-dlp (hd/sd via facebookMode), not fbcdn fragment.
            val pageTarget = when {
                isFacebookWatchablePage(pageUrl) -> pageUrl
                isFacebookWatchablePage(clicked) -> clicked
                FacebookUrlUtils.isFacebookUrl(pageUrl) -> pageUrl
                else -> null
            }
            if (pageTarget.isNullOrBlank()) {
                BrowserKitLog.e("FB", "no RegularMp4 and no page URL — play reel longer")
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No video yet — play the reel, then tap download",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                return@launch
            }
            BrowserKitLog.i(
                "FB",
                "page yt-dlp (Super) ${BrowserKitLog.shortUrl(pageTarget)}",
            )
            enqueueFacebookDownload(url = pageTarget, pageUrl = pageUrl, regular = false)
        }
    }

    private fun enqueueFacebookDownload(
        url: String,
        pageUrl: String,
        regular: Boolean,
        headersExtra: Map<String, String> = emptyMap(),
    ) {
        if (!isAdded || _binding == null) return
        val now = System.currentTimeMillis()
        if (url == lastFacebookEnqueueUrl && now - lastFacebookEnqueueAtMs < 2_500L) {
            BrowserKitLog.w("FB", "skip duplicate click within 2.5s")
            return
        }
        lastFacebookEnqueueUrl = url
        lastFacebookEnqueueAtMs = now
        // Never cache page/reel URLs as "CDN" — poisons later clicks.
        if (!isFacebookWatchablePage(url) && isProgressiveFacebookCdn(url)) {
            rememberFacebookMediaUrl(url)
        }
        if (regular) {
            startFacebookRegularDownload(url, pageUrl, headersExtra)
        } else {
            startFacebookYtdlpDownload(url, pageUrl)
        }
    }

    private suspend fun scrapeFacebookPlayableUrl(webView: WebView): String? {
        val raw = suspendCancellableCoroutine { cont ->
            webView.evaluateJavascript(FB_PLAYABLE_SCRAPE_JS) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }
        return parseJsStringResult(raw)
            ?.trim()
            ?.takeIf { it.startsWith("http", ignoreCase = true) }
    }

    private fun parseJsStringResult(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return runCatching {
            com.google.gson.JsonParser.parseString(raw).asString
        }.getOrElse {
            raw.trim().removePrefix("\"").removeSuffix("\"").replace("\\\"", "\"")
        }.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun isFacebookWatchablePage(url: String): Boolean {
        val lower = url.lowercase()
        if (!lower.contains("facebook.com") && !lower.contains("fb.watch")) return false
        return lower.contains("/reel/") || lower.contains("/watch") ||
            lower.contains("/videos/") || lower.contains("fb.watch")
    }

    /**
     * IG: RegularMp4 verified → CustomRegular CDN; else recent CDN; else page yt-dlp.
     */
    private fun onInstagramVideoClicked(videoUrl: String) {
        val pageUrl = currentPageUrl.ifBlank { InstagramUrlUtils.REFERER }
        val clicked = videoUrl.trim()
        lifecycleScope.launch {
            var verified = detectionViewModel.bestVerifiedRegular()
            if (verified == null) {
                delay(800)
                verified = detectionViewModel.bestVerifiedRegular()
            }
            if (verified != null &&
                (InstagramUrlUtils.isInstagramMediaUrl(verified.finalUrl) || isInstagramPage)
            ) {
                BrowserKitLog.i(
                    "IG",
                    "RegularMp4 → CDN len=${verified.contentLength} " +
                        BrowserKitLog.shortUrl(verified.finalUrl),
                )
                startInstagramCdnDownload(verified.finalUrl, pageUrl)
                return@launch
            }
            if (InstagramUrlUtils.isInstagramMediaUrl(clicked)) {
                BrowserKitLog.i("IG", "click CDN path ${BrowserKitLog.shortUrl(clicked)}")
                startInstagramCdnDownload(clicked, pageUrl)
                return@launch
            }
            val recent = latestInstagramMediaUrl()
            if (!recent.isNullOrBlank()) {
                BrowserKitLog.i("IG", "click recent CDN ${BrowserKitLog.shortUrl(recent)}")
                startInstagramCdnDownload(recent, pageUrl)
                return@launch
            }
            val postUrl = when {
                InstagramUrlUtils.isInstagramUrl(clicked) -> clicked
                InstagramUrlUtils.isInstagramUrl(pageUrl) -> pageUrl
                else -> null
            }
            if (postUrl.isNullOrBlank()) {
                BrowserKitLog.e("IG", "click: no CDN and no post url")
                if (isAdded) {
                    Toast.makeText(requireContext(), R.string.bk_no_video_detected, Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            BrowserKitLog.i("IG", "click page yt-dlp ${BrowserKitLog.shortUrl(postUrl)}")
            startInstagramPageDownload(postUrl)
        }
    }

    private fun rememberInstagramMediaUrl(url: String) {
        if (!InstagramUrlUtils.isInstagramMediaUrl(url)) return
        synchronized(recentInstagramMediaUrls) {
            recentInstagramMediaUrls.remove(url)
            recentInstagramMediaUrls.addFirst(url)
            while (recentInstagramMediaUrls.size > 24) {
                recentInstagramMediaUrls.removeLast()
            }
        }
        pushInstagramCdnToJs(url)
    }

    private fun pushInstagramCdnToJs(url: String) {
        if (!isInstagramPage) return
        val webView = _binding?.webView ?: return
        val escaped = url
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "")
            .replace("\r", "")
        val js = "window.__avdIgLatestCdn='$escaped';"
        webView.post {
            if (_binding == null || !isInstagramPage) return@post
            _binding?.webView?.evaluateJavascript(js, null)
        }
    }

    private fun latestInstagramMediaUrl(): String? {
        synchronized(recentInstagramMediaUrls) {
            return recentInstagramMediaUrls.firstOrNull()
        }
    }

    private fun startInstagramCdnDownload(cdnUrl: String, pageUrl: String) {
        BrowserDownloadManager.init(requireContext())
        rememberInstagramMediaUrl(cdnUrl)
        val headers = linkedMapOf<String, String>().apply {
            putAll(
                CookieUtils.headersFromWebView(
                    pageUrl.ifBlank { InstagramUrlUtils.REFERER },
                    userAgent(),
                    InstagramUrlUtils.REFERER,
                ),
            )
            put("Referer", InstagramUrlUtils.REFERER)
            putIfAbsent("User-Agent", userAgent())
        }
        BrowserKitLog.i(
            "IG",
            "CDN enqueue hasCookie=${headers.containsKey("Cookie")} " +
                "url=${BrowserKitLog.shortUrl(cdnUrl)}",
        )
        val info = DetectedVideoInfo(
            pageUrl = pageUrl.ifBlank { InstagramUrlUtils.REFERER },
            title = "instagram",
            formats = listOf(
                StreamFormat(
                    url = cdnUrl,
                    label = "Instagram",
                    ext = "mp4",
                    streamType = StreamType.PROGRESSIVE_MP4,
                    headers = headers,
                ),
            ),
            headers = headers,
        )
        BrowserDownloadManager.enqueue(info, info.formats.first())
    }

    private fun startInstagramPageDownload(postUrl: String) {
        BrowserDownloadManager.init(requireContext())
        lifecycleScope.launch {
            runCatching {
                com.avd.browserkit.BrowserKitInitializer.initializeAwait(requireContext())
            }.onFailure {
                BrowserKitLog.e("IG", "initAwait failed", it)
            }
            if (!isAdded || _binding == null) return@launch
            val cookies = CookieManager.getInstance().getCookie(postUrl)
            BrowserKitLog.i(
                "IG",
                "page extract cookies=${if (cookies.isNullOrBlank()) "none" else "${cookies.length}c"}",
            )
            val extracted = com.avd.browserkit.ytdlp.YtDlpExtractor.extract(
                postUrl,
                cookies,
                userAgent(),
            )
            if (extracted == null || extracted.formats.isEmpty()) {
                BrowserKitLog.e("IG", "page extract empty — no download")
                Toast.makeText(requireContext(), R.string.bk_no_video_detected, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val igHeaders = CookieUtils.headersFromWebView(
                postUrl,
                userAgent(),
                InstagramUrlUtils.REFERER,
            )
            val format = extracted.formats.first()
            val info = extracted.copy(
                pageUrl = postUrl,
                title = extracted.title.ifBlank { "instagram" },
                headers = extracted.headers + igHeaders,
                formats = extracted.formats.map { f ->
                    f.copy(headers = f.headers + igHeaders)
                },
            )
            BrowserDownloadManager.enqueue(info, format.copy(headers = format.headers + igHeaders))
        }
    }

    /** Xilli checkRegularMp4 hit → CustomRegularDownloader (isRegularDownload=true). */
    private fun startFacebookRegularDownload(
        downloadUrl: String,
        pageUrl: String,
        headersExtra: Map<String, String>,
    ) {
        BrowserDownloadManager.init(requireContext())
        val headers = linkedMapOf<String, String>().apply {
            putAll(headersExtra)
            putAll(CookieUtils.headersFromWebView(pageUrl, userAgent(), "https://www.facebook.com/"))
            put("Referer", "https://www.facebook.com/")
            putIfAbsent("User-Agent", userAgent())
        }
        BrowserKitLog.i(
            "FB",
            "enqueue CustomRegular headers=${headers.keys} " +
                "url=${BrowserKitLog.shortUrl(downloadUrl)}",
        )
        val info = DetectedVideoInfo(
            pageUrl = pageUrl,
            title = "face_book",
            formats = listOf(
                StreamFormat(
                    url = downloadUrl,
                    label = "Facebook",
                    ext = "mp4",
                    streamType = StreamType.PROGRESSIVE_MP4,
                    headers = headers,
                ),
            ),
            headers = headers,
            isRegularDownload = true,
        )
        BrowserDownloadManager.enqueue(info, info.formats.first())
    }

    /**
     * Xilli WebAppInterface.onVideoClicked:
     * title=face_book, isRegularDownload=false → YoutubeDlDownloader + isFaceBook.
     */
    private fun startFacebookYtdlpDownload(downloadUrl: String, pageUrl: String) {
        BrowserDownloadManager.init(requireContext())
        if (looksLikeFacebookMediaUrl(downloadUrl)) {
            rememberFacebookMediaUrl(downloadUrl)
        }
        // Cookies from facebook.com page (Xilli Chrome cookie store); Referer facebook.com.
        val headers = linkedMapOf<String, String>().apply {
            putAll(CookieUtils.headersFromWebView(pageUrl, userAgent(), "https://www.facebook.com/"))
            put("Referer", "https://www.facebook.com/")
            putIfAbsent("User-Agent", userAgent())
        }
        BrowserKitLog.i(
            "FB",
            "enqueue YoutubeDl isFaceBook=true headers=${headers.keys} " +
                "cdn=${BrowserKitLog.shortUrl(downloadUrl)}",
        )
        lifecycleScope.launch {
            runCatching {
                com.avd.browserkit.BrowserKitInitializer.initializeAwait(requireContext())
            }.onFailure {
                BrowserKitLog.e("FB", "yt-dlp init failed", it)
            }
            if (!isAdded || _binding == null) {
                BrowserKitLog.w("FB", "enqueue aborted: fragment gone")
                return@launch
            }
            val info = DetectedVideoInfo(
                pageUrl = pageUrl,
                title = "face_book",
                formats = listOf(
                    StreamFormat(
                        url = downloadUrl,
                        label = "Facebook",
                        ext = "mp4",
                        streamType = StreamType.FACEBOOK_YTDLP,
                        headers = headers,
                    ),
                ),
                headers = headers,
                isRegularDownload = false,
            )
            BrowserDownloadManager.enqueue(info, info.formats.first())
        }
    }

    private fun scanPageForVideos() {
        // Delayed postDelayed(2s) can fire after onDestroyView — never use binding!!
        val webView = _binding?.webView ?: return
        if (!isAdded) return
        val script = """
            (function() {
              function report(url) {
                if (!url || url.indexOf('http') !== 0) return;
                try { BrowserKitDetector.onMediaUrl(url); } catch (e) {}
              }
              function looksMedia(url) {
                if (!url) return false;
                var u = url.toLowerCase();
                return u.indexOf('.m3u8') >= 0 || u.indexOf('.mpd') >= 0 ||
                  u.indexOf('.mp4') >= 0 || u.indexOf('dmcdn') >= 0 ||
                  u.indexOf('manifest') >= 0 || u.indexOf('/hls/') >= 0 ||
                  u.indexOf('mpegurl') >= 0;
              }
              var videos = document.querySelectorAll('video');
              for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                report(v.currentSrc || v.src);
                var sources = v.querySelectorAll('source');
                for (var j = 0; j < sources.length; j++) {
                  report(sources[j].src);
                }
              }
              var audios = document.querySelectorAll('audio');
              for (var k = 0; k < audios.length; k++) {
                report(audios[k].currentSrc || audios[k].src);
              }
              // Dailymotion / MSE: stream URLs often only appear in resource timing after play.
              try {
                var entries = performance.getEntriesByType('resource');
                for (var n = 0; n < entries.length; n++) {
                  var name = entries[n].name || '';
                  if (looksMedia(name)) report(name);
                }
              } catch (e) {}
            })();
        """.trimIndent()
        runCatching { webView.evaluateJavascript(script, null) }
    }

    /**
     * DM player often fetches HLS only after play / a few seconds.
     * Rescan + yt-dlp retry without requiring another navigation.
     */
    private fun scheduleDailymotionRescans(pageUrl: String) {
        clearPendingDmRescans()
        if (!DailymotionUrlUtils.isDailymotionUrl(pageUrl)) return
        val v = view ?: return
        val delaysMs = longArrayOf(3_000L, 6_000L, 12_000L)
        for (delayMs in delaysMs) {
            val runnable = Runnable {
                if (_binding == null || !isAdded) return@Runnable
                val active = binding.webView.url.orEmpty().ifBlank { currentPageUrl }
                if (!DailymotionUrlUtils.isDailymotionUrl(active)) return@Runnable
                BrowserKitLog.d("DM", "rescan t=${delayMs}ms ${BrowserKitLog.shortUrl(active)}")
                scanPageForVideos()
                val state = detectionViewModel.buttonState.value
                if (state !is DownloadButtonState.CanDownload &&
                    DailymotionUrlUtils.isDailymotionVideoPage(active)
                ) {
                    detectionViewModel.verifyPageWithYtDlp(active)
                }
            }
            pendingDmRescanRunnables.add(runnable)
            v.postDelayed(runnable, delayMs)
        }
    }

    private fun clearPendingDmRescans() {
        val v = view
        pendingDmRescanRunnables.forEach { r -> v?.removeCallbacks(r) }
        pendingDmRescanRunnables.clear()
    }

    private fun scheduleCapturePreview() {
        clearPendingPreview()
        val v = view ?: return
        val runnable = Runnable {
            if (_binding == null || !isAdded) return@Runnable
            captureAndStorePreview()
        }
        pendingPreviewRunnable = runnable
        v.post(runnable)
    }

    private fun scheduleDelayedPageScan() {
        clearPendingPageScan()
        val v = view ?: return
        val runnable = Runnable { scanPageForVideos() }
        pendingScanRunnable = runnable
        v.postDelayed(runnable, 2000L)
    }

    private fun scheduleFacebookInjectRetries() {
        // No-op: FB download buttons removed.
        clearPendingFbInjects()
    }

    private fun clearPendingPageScan() {
        pendingScanRunnable?.let { view?.removeCallbacks(it) }
        pendingScanRunnable = null
    }

    private fun clearPendingPreview() {
        pendingPreviewRunnable?.let { view?.removeCallbacks(it) }
        pendingPreviewRunnable = null
    }

    private fun clearPendingFbInjects() {
        val v = view
        pendingFbInjectRunnables.forEach { r -> v?.removeCallbacks(r) }
        pendingFbInjectRunnables.clear()
    }

    private fun clearAllPendingPageCallbacks() {
        clearPendingPageScan()
        clearPendingPreview()
        clearPendingDmRescans()
        clearPendingFbInjects()
        clearPendingIgInjects()
    }

    private fun setupControls() {
        binding.btnBackNav.setOnClickListener { requireActivity().finish() }
        binding.btnWebBack.setOnClickListener { navigateBack() }
        binding.btnWebForward.setOnClickListener { navigateForward() }
        binding.btnHome.setOnClickListener {
            browserViewModel.hideTabsSwitcher()
            refreshChrome()
        }
        binding.btnTabs.setOnClickListener {
            captureAndStorePreview()
            (parentFragment as? BrowserHostFragment)?.openTabsDrawer()
        }
        binding.btnMenu.setOnClickListener { openBrowserMenu() }
        binding.btnClearUrl.setOnClickListener {
            binding.etAddress.setText("")
            binding.etAddress.requestFocus()
            showKeyboard()
        }
        binding.etAddress.setOnFocusChangeListener { _, hasFocus ->
            addressEditing = hasFocus
            if (hasFocus) {
                binding.etAddress.setText(currentPageUrl.takeIf { it != "about:blank" }.orEmpty())
                binding.etAddress.selectAll()
                binding.btnClearUrl.isVisible = binding.etAddress.text?.isNotEmpty() == true
            } else {
                showAddress(currentPageUrl)
                hideKeyboard()
            }
        }
        binding.etAddress.doAfterTextChanged { text ->
            if (addressEditing) {
                binding.btnClearUrl.isVisible = !text.isNullOrEmpty()
            }
        }
        binding.etAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = binding.etAddress.text?.toString().orEmpty()
                addressEditing = false
                binding.etAddress.clearFocus()
                loadUrl(
                    UrlUtils.normalizeInput(input, BrowserKit.getConfig().searchUrlTemplate),
                )
                true
            } else {
                false
            }
        }
        setupDraggableDownloadFab()
        updateNavButtons()
    }

    private var fabTranslationX = 0f
    private var fabTranslationY = 0f

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableDownloadFab() {
        val fab = binding.fabDownload
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var dragStartTranslationX = 0f
        var dragStartTranslationY = 0f
        var dragged = false

        fab.bringToFront()
        fab.translationX = fabTranslationX
        fab.translationY = fabTranslationY

        fab.setOnTouchListener { view, event ->
            val parent = view.parent as? View ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dragStartTranslationX = fabTranslationX
                    dragStartTranslationY = fabTranslationY
                    dragged = false
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragged && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragged = true
                    }
                    if (dragged) {
                        val nextX = dragStartTranslationX + dx
                        val nextY = dragStartTranslationY + dy
                        val minX = -view.left.toFloat()
                        val minY = -view.top.toFloat()
                        val maxX = (parent.width - view.right).toFloat()
                        val maxY = (parent.height - view.bottom).toFloat()
                        view.translationX = nextX.coerceIn(minX, maxX)
                        view.translationY = nextY.coerceIn(minY, maxY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragged) {
                        fabTranslationX = view.translationX
                        fabTranslationY = view.translationY
                    } else {
                        view.performClick()
                    }
                    dragged = false
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dragged = false
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }
        fab.setOnClickListener { onDownloadClicked() }
    }

    fun openBrowserMenu() {
        BrowserMenuBottomSheet.show(
            parentFragmentManager,
            desktopMode,
            object : BrowserMenuListener {
                override fun onMenuHome() {
                    browserViewModel.hideTabsSwitcher()
                    refreshChrome()
                }

                override fun onMenuDownloads() {
                    BrowserDownloadsDialog.show(parentFragmentManager, repository)
                }

                override fun onMenuHistory() {
                    historyLauncher.launch(BrowserHistoryActivity.intent(requireContext()))
                }

                override fun onMenuSettings() = showSettingsDialog()

                override fun onMenuFindInPage() = showFindInPageBottomSheet()

                override fun onMenuDesktopSiteChanged(enabled: Boolean) {
                    if (desktopMode == enabled) return
                    desktopMode = enabled
                    // Persist so new tabs inherit desktop/mobile preference.
                    BrowserKit.setConfig(BrowserKit.getConfig().copy(useDesktopMode = enabled))
                    BrowserKitLog.i(
                        "Desktop",
                        "toggle=$enabled fb=$isFacebookPage ig=$isInstagramPage",
                    )
                    val url = binding.webView.url.orEmpty().ifBlank { currentPageUrl }
                    if (isFacebookPage) {
                        // FB always needs desktop UA for data-video-id; rewrite m.→www + reload.
                        applyFacebookDesktopUaOnly()
                        val target = maybeRewriteFacebookUrl(url) ?: url
                        if (UrlUtils.isHttpUrl(target)) {
                            binding.webView.loadUrl(target)
                        } else {
                            binding.webView.reload()
                        }
                        return
                    }
                    applyWebViewUserAgent()
                    if (UrlUtils.isHttpUrl(url)) {
                        binding.webView.stopLoading()
                        binding.webView.loadUrl(url)
                    } else {
                        binding.webView.reload()
                    }
                }

                override fun onMenuClearBrowsingData() = confirmClearBrowsingData()

                override fun onMenuShare() = shareCurrentPage()
            },
        )
    }

    private fun showSettingsDialog() {
        val ctx = requireContext()
        val forceStream = com.avd.browserkit.util.BrowserDetectionPrefs.isForceStreamDetection(ctx)
        val legacyM3u8 = com.avd.browserkit.util.BrowserDetectionPrefs.isUseLegacyM3u8Detection(ctx)
        val checked = booleanArrayOf(forceStream, legacyM3u8)
        BrowserDialogBuilders.create(ctx)
            .setTitle(R.string.bk_settings)
            .setMultiChoiceItems(
                arrayOf(
                    getString(R.string.bk_force_stream_detection),
                    getString(R.string.bk_legacy_m3u8_detection),
                ),
                checked,
            ) { _, which, isChecked ->
                when (which) {
                    0 -> com.avd.browserkit.util.BrowserDetectionPrefs.setForceStreamDetection(ctx, isChecked)
                    1 -> com.avd.browserkit.util.BrowserDetectionPrefs.setUseLegacyM3u8Detection(ctx, isChecked)
                }
            }
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.bk_reload_page) { _, _ -> binding.webView.reload() }
            .setNegativeButton(R.string.bk_new_tab) { _, _ -> browserViewModel.addNewTab() }
            .show()
    }

    private fun showFindInPageBottomSheet() {
        FindInPageBottomSheet.show(
            parentFragmentManager,
            object : FindInPageListener {
                override fun onQueryChanged(query: String) {
                    if (query.isEmpty()) {
                        binding.webView.clearMatches()
                        return
                    }
                    binding.webView.findAllAsync(query)
                    binding.webView.setFindListener { _, numberOfMatches, done ->
                        if (done && numberOfMatches == 0) {
                            Toast.makeText(requireContext(), R.string.bk_no_matches, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFindNext() {
                    binding.webView.findNext(true)
                }

                override fun onFindPrevious() {
                    binding.webView.findNext(false)
                }

                override fun onClosed() {
                    binding.webView.clearMatches()
                }
            },
        )
    }

    private fun confirmClearBrowsingData() {
        BrowserDialogBuilders.create(requireContext())
            .setMessage(R.string.bk_clear_data_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    repository.clearHistory()
                    CookieManager.getInstance().removeAllCookies { _ ->
                        CookieManager.getInstance().flush()
                    }
                    WebStorage.getInstance().deleteAllData()
                    binding.webView.clearCache(true)
                    binding.webView.clearHistory()
                    Toast.makeText(requireContext(), R.string.bk_data_cleared, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun shareCurrentPage() {
        if (!UrlUtils.isHttpUrl(currentPageUrl)) return
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentPageUrl)
            putExtra(Intent.EXTRA_SUBJECT, binding.webView.title.orEmpty())
        }
        startActivity(Intent.createChooser(share, getString(R.string.bk_share)))
    }

    private fun observeDetection() {
        detectionViewModel.buttonState.observe(viewLifecycleOwner) { state ->
            // FB / IG / YouTube: browse-only — never show download FAB.
            if (isFacebookPage || isInstagramPage || isYouTubePage) {
                hideDownloadFab()
                return@observe
            }
            val effectiveState = when {
                state is DownloadButtonState.CanDownload -> state
                detectionViewModel.currentDownloadInfo() != null ->
                    DownloadButtonState.CanDownload(detectionViewModel.currentDownloadInfo()!!)
                else -> state
            }
            BrowserKitLog.d(
                "FAB.State",
                "raw=${state?.javaClass?.simpleName ?: "null"} effective=${effectiveState?.javaClass?.simpleName ?: "null"} " +
                    "page=${BrowserKitLog.shortUrl(currentPageUrl)}",
            )
            binding.fabDownload.isVisible = true
            when (effectiveState) {
                DownloadButtonState.CannotDownload -> {
                    setFabLoading(false)
                    binding.fabDownload.alpha = 0.5f
                }
                DownloadButtonState.Loading -> {
                    setFabLoading(true)
                    binding.fabDownload.alpha = 1f
                }
                is DownloadButtonState.CanDownload -> {
                    setFabLoading(false)
                    binding.fabDownload.alpha = 1f
                }
            }
        }
    }

    private fun setFabLoading(loading: Boolean) {
        if (_binding == null) return
        binding.fabDownloadLoading.isVisible = loading
        binding.fabDownloadIcon.isVisible = !loading
    }

    private fun onDownloadClicked() {
        if (isFacebookPage || isInstagramPage || isYouTubePage) {
            BrowserKitLog.w("FAB", "download disabled on FB/IG/YT")
            hideDownloadFab()
            if (isYouTubePage) maybeShowYouTubeWarning()
            return
        }
        val effectiveState = when (val rawState = detectionViewModel.buttonState.value) {
            is DownloadButtonState.CanDownload -> rawState
            else -> detectionViewModel.currentDownloadInfo()?.let { DownloadButtonState.CanDownload(it) } ?: rawState
        }
        when (val state = effectiveState) {
            is DownloadButtonState.CanDownload -> {
                BrowserKitLog.i(
                    "FAB",
                    "download formats=${state.info.formats.size} title=${state.info.title}",
                )
                FormatPickerDialog.show(parentFragmentManager, state.info) { format ->
                    BrowserKitLog.i(
                        "FAB",
                        "picked ${format.label} ${format.streamType} ${BrowserKitLog.shortUrl(format.url)}",
                    )
                    BrowserDownloadManager.enqueue(state.info, format)
                    Toast.makeText(requireContext(), R.string.bk_download_started, Toast.LENGTH_SHORT).show()
                }
            }
            DownloadButtonState.Loading -> {
                BrowserKitLog.d("FAB", "still detecting page=${BrowserKitLog.shortUrl(currentPageUrl)}")
                Toast.makeText(requireContext(), R.string.bk_detecting, Toast.LENGTH_SHORT).show()
            }
            DownloadButtonState.CannotDownload, null -> {
                BrowserKitLog.w("FAB", "no video state=$state page=${BrowserKitLog.shortUrl(currentPageUrl)}")
                Toast.makeText(requireContext(), R.string.bk_no_video_detected, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadUrl(url: String) {
        if (AdultSiteBlocker.isBlocked(url)) {
            BrowserKitLog.w("Adult", "loadUrl blocked ${BrowserKitLog.shortUrl(url)}")
            showAdultBlockedDialog()
            return
        }
        val target = maybeRewriteFacebookUrl(url) ?: url
        // Facebook needs desktop UA before load — never stopLoading after.
        if (FacebookUrlUtils.isFacebookUrl(target)) {
            isFacebookPage = true
            applyFacebookDesktopUaOnly()
        } else {
            applyWebViewUserAgent()
        }
        if (YoutubeUrlUtils.isYouTubeUrl(target)) {
            isYouTubePage = true
            maybeShowYouTubeWarning()
        }
        currentPageUrl = target
        addressEditing = false
        showAddress(target)
        binding.webView.loadUrl(target)
        updateNavButtons()
    }

    /** Show YouTube watch-only warning; debounced so SPA does not spam. */
    private fun maybeShowYouTubeWarning() {
        if (!isAdded) return
        val now = System.currentTimeMillis()
        if (now - lastYouTubeWarningAtMs < YOUTUBE_WARNING_DEBOUNCE_MS) return
        lastYouTubeWarningAtMs = now
        YoutubeBlockedDialog.show(parentFragmentManager)
    }

    private fun showAdultBlockedDialog() {
        if (!isAdded) return
        AdultBlockedDialog.show(parentFragmentManager)
    }

    /** Stop a blocked adult page that started via redirect / SPA and leave safely. */
    private fun leaveBlockedAdultPage(blockedUrl: String) {
        if (_binding == null) return
        showAdultBlockedDialog()
        binding.webView.stopLoading()
        when {
            binding.webView.canGoBack() -> binding.webView.goBack()
            else -> {
                currentPageUrl = "about:blank"
                addressEditing = false
                showAddress("")
                binding.webView.loadUrl("about:blank")
            }
        }
        BrowserKitLog.w("Adult", "left blocked page ${BrowserKitLog.shortUrl(blockedUrl)}")
    }

    fun captureAndStorePreview() {
        val b = _binding ?: return
        val webView = b.webView
        if (webView.width <= 0 || webView.height <= 0) return
        val tabId = browserViewModel.tabs.value?.getOrNull(tabIndex)?.id ?: return
        runCatching {
            val full = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.RGB_565)
            val canvas = Canvas(full)
            webView.draw(canvas)
            val targetW = 360
            val targetH = (targetW * 1.2f).toInt()
            val scaled = Bitmap.createScaledBitmap(full, targetW, targetH, true)
            if (scaled !== full) full.recycle()
            browserViewModel.setPreview(tabId, scaled)
        }
    }

    fun refreshChrome() {
        updateChromeButtons()
    }

    fun canNavigateBack(): Boolean = _binding?.webView?.canGoBack() == true

    fun canNavigateForward(): Boolean = _binding?.webView?.canGoForward() == true

    fun navigateBack(): Boolean {
        val webView = _binding?.webView ?: return false
        if (!webView.canGoBack()) return false
        webView.goBack()
        webView.post { updateNavButtons() }
        return true
    }

    fun navigateForward(): Boolean {
        val webView = _binding?.webView ?: return false
        if (!webView.canGoForward()) return false
        webView.goForward()
        webView.post { updateNavButtons() }
        return true
    }

    private fun showAddress(url: String) {
        val display = when {
            url.isBlank() || url == "about:blank" -> ""
            else -> displayHost(url)
        }
        binding.etAddress.setText(display)
        binding.btnClearUrl.isVisible = display.isNotEmpty()
    }

    private fun displayHost(url: String): String {
        return runCatching {
            Uri.parse(url).host.orEmpty().ifBlank { url }
        }.getOrDefault(url)
    }

    private fun updateNavButtons() {
        val b = _binding ?: return
        val canBack = b.webView.canGoBack()
        val canForward = b.webView.canGoForward()
        b.btnWebBack.isEnabled = true
        b.btnWebForward.isEnabled = true
        b.btnWebBack.alpha = if (canBack) 1f else 0.45f
        b.btnWebForward.alpha = if (canForward) 1f else 0.45f
        updateChromeButtons()
    }

    private fun updateChromeButtons() {
        val b = _binding ?: return
        val context = requireContext()
        val active = ContextCompat.getColor(context, R.color.bk_chrome_active)
        val icon = ContextCompat.getColor(context, R.color.bk_chrome_icon)
        val disabled = ContextCompat.getColor(context, R.color.bk_chrome_disabled)
        val onBrowserHomeScreen = browserViewModel.tabsSwitcherVisible.value != true

        if (onBrowserHomeScreen) {
            b.btnHome.setImageResource(R.drawable.bk_ic_home_filled)
            b.btnHome.imageTintList = ColorStateList.valueOf(active)
            b.btnTabs.imageTintList = ColorStateList.valueOf(icon)
        } else {
            b.btnHome.setImageResource(R.drawable.bk_ic_home_outline)
            b.btnHome.imageTintList = ColorStateList.valueOf(icon)
            b.btnTabs.imageTintList = ColorStateList.valueOf(active)
        }
        b.btnWebBack.imageTintList = ColorStateList.valueOf(
            if (b.webView.canGoBack()) icon else disabled,
        )
        b.btnWebForward.imageTintList = ColorStateList.valueOf(
            if (b.webView.canGoForward()) icon else disabled,
        )
        b.btnMenu.imageTintList = ColorStateList.valueOf(icon)
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService<InputMethodManager>() ?: return
        imm.showSoftInput(binding.etAddress, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService<InputMethodManager>() ?: return
        imm.hideSoftInputFromWindow(binding.etAddress.windowToken, 0)
    }

    private fun userAgent(): String {
        val config = BrowserKit.getConfig()
        // Facebook needs desktop UA for data-video-id overlay buttons (Xilli).
        if (isFacebookPage) return config.desktopUserAgent
        return if (desktopMode) config.desktopUserAgent else config.mobileUserAgent
    }

    override fun onPause() {
        flushCookies()
        super.onPause()
    }

    override fun onStop() {
        flushCookies()
        super.onStop()
    }

    private fun flushCookies() {
        runCatching { CookieManager.getInstance().flush() }
    }

    override fun onDestroyView() {
        clearAllPendingPageCallbacks()
        stopFacebookRescan()
        stopInstagramRescan()
        flushCookies()
        _binding?.webView?.destroy()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TAB_INDEX = "tab_index"
        private const val YOUTUBE_WARNING_DEBOUNCE_MS = 5_000L

        private val FB_PLAYABLE_SCRAPE_JS = """
            (function() {
              function unesc(u) {
                if (!u) return '';
                return String(u)
                  .replace(/\\\//g, '/')
                  .replace(/\\u0026/g, '&')
                  .replace(/\\u003A/g, ':')
                  .replace(/\\u0025/g, '%')
                  .replace(/&amp;/g, '&');
              }
              function ok(u) {
                if (!u || u.indexOf('http') !== 0) return false;
                if (u.indexOf('blob:') === 0) return false;
                var l = u.toLowerCase();
                if (l.indexOf('/m78/') >= 0 || l.indexOf('/m412/') >= 0 || l.indexOf('/m366/') >= 0) {
                  return false;
                }
                var progressive = l.indexOf('.mp4') >= 0 || l.indexOf('.webm') >= 0 ||
                  l.indexOf('.m4v') >= 0;
                if (!progressive) return false;
                return l.indexOf('fbcdn') >= 0 || l.indexOf('fbsbx') >= 0;
              }
              var html = (document.documentElement && document.documentElement.innerHTML) || '';
              var keys = [
                'browser_native_hd_url',
                'browser_native_sd_url',
                'playable_url_quality_hd',
                'playable_url',
                'hd_src_no_ratelimit',
                'sd_src_no_ratelimit',
                'hd_src',
                'sd_src'
              ];
              var found = [];
              for (var k = 0; k < keys.length; k++) {
                var re = new RegExp('"' + keys[k] + '"\\s*:\\s*"([^"]+)"', 'g');
                var m;
                while ((m = re.exec(html)) !== null) {
                  var u = unesc(m[1]);
                  if (ok(u) && found.indexOf(u) < 0) found.push(u);
                }
              }
              found.sort(function(a, b) { return b.length - a.length; });
              return found.length ? found[0] : '';
            })();
        """.trimIndent()

        fun newInstance(tabIndex: Int): WebTabFragment {
            return WebTabFragment().apply {
                arguments = Bundle().apply { putInt(ARG_TAB_INDEX, tabIndex) }
            }
        }
    }
}
