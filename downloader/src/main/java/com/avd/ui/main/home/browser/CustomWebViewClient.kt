package com.avd.ui.main.home.browser

import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.viewModelScope
import com.avd.data.local.room.entity.HistoryItem
import com.avd.ui.main.history.HistoryViewModel
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.AdBlockerHelper
import com.avd.util.FaviconUtils
import com.avd.util.proxy_utils.CustomProxyController
import com.avd.util.proxy_utils.OkHttpProxyClient
import com.avd.ui.main.home.browser.detectedVideos.IVideoDetector
import com.avd.ui.main.home.browser.webTab.ButtonVisibilityYoutube
import com.avd.ui.main.home.browser.webTab.WebTab
import com.avd.ui.main.home.browser.webTab.WebTabViewModel
import com.avd.util.CookieUtils
import com.avd.util.FaceBookScript
import com.avd.util.SingleLiveEvent
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class CustomWebViewClient(
    private val tabViewModel: WebTabViewModel,
    private val settingsModel: SettingsViewModel,
    private val videoDetectionModel: IVideoDetector,
    private val historyModel: HistoryViewModel,
    private val proxyController: CustomProxyController,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val updateTabEvent: SingleLiveEvent<WebTab>,
    private val pageTabProvider: PageTabProvider,
    private val buttonVisibilityYoutube: ButtonVisibilityYoutube,
    val updatevisitedhistry : (()-> Unit),
    private val onPageFinishedCallback: (() -> Unit)? = null
) : WebViewClient() {
    var videoAlert: AlertDialog? = null
    private var searchEventCounter = 0
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()
    private var youtube=false
    private var isFacebook=false


    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        if (!youtube && !isFacebook){
        val viewTitle = view?.title
        val title = tabViewModel.currentTitle.get()
        val userAgent = view?.settings?.userAgentString ?: tabViewModel.userAgent.get()
        if (url != null && lastSavedHistoryUrl != url) {
            historyModel.viewModelScope.launch(historyModel.executorSingleHistory) {
                val icon = try {
                    FaviconUtils.getEncodedFaviconFromUrl(
                        okHttpProxyClient.getProxyOkHttpClient(), url
                    )
                } catch (e: Throwable) {
                    null
                }
                saveUrlToHistory(url, icon, viewTitle ?: title)
                videoDetectionModel.onStartPage(url, userAgent ?: BrowserFragment.MOBILE_USER_AGENT)
                tabViewModel.onUpdateVisitedHistory(url, title, userAgent)
            }
        }
        }
        updatevisitedhistry.invoke()
        super.doUpdateVisitedHistory(view, url, isReload)
    }

    // TODO handle for proxy and others
//    override fun onReceivedHttpAuthRequest(
//        view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
//    ) {
//        val creds = proxyController.getProxyCredentials()
//        handler?.proceed(creds.first, creds.second)
//    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (!youtube || !isFacebook){
            val isAdBlockerOn = settingsModel.isAdBlocker.get()
            val url = request?.url.toString()
            val isUrlAd: Boolean = isAdBlockerOn && tabViewModel.isAd(url)
            when {
                isUrlAd -> {
                    return AdBlockerHelper.createEmptyResource()
                }
                url.contains(".m3u8") || url.contains(".mpd") || url.contains(".txt") -> {
                    if (request != null) {
                        val verReq = try {
                            CookieUtils.webRequestToHttpWithCookies(request)
                        } catch (e: Throwable) {
                            null
                        }
                        if (verReq != null) {
                            videoDetectionModel.verifyLinkStatus(
                                verReq, tabViewModel.currentTitle.get()
                            )
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
                else -> {
                    if (settingsModel.getIsCheckEveryRequestOnVideo().get()) {
                        val requestWithCookies = request?.let { resourceRequest ->
                            CookieUtils.webRequestToHttpWithCookies(
                                resourceRequest
                            )
                        }
                        val disposable = videoDetectionModel.checkRegularMp4(requestWithCookies)
                        val currentUrl = tabViewModel.getTabTextInput().get() ?: ""
                        if (currentUrl != lastRegularCheckUrl) {
                            regularJobsStorage[lastRegularCheckUrl]?.forEach {
                                it.dispose()
                            }
                            regularJobsStorage.remove(lastRegularCheckUrl)
                            lastRegularCheckUrl = currentUrl
                        }
                        if (disposable != null) {
                            val overall = mutableListOf<Disposable>()
                            overall.addAll(regularJobsStorage[currentUrl]?.toList() ?: emptyList())
                            overall.add(disposable)
                            regularJobsStorage[currentUrl] = overall
                        }
                    }
                    return super.shouldInterceptRequest(
                        view, request
                    )
                }
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        // Check if the URL is a YouTube link
        val isYoutube = url.contains("youtube", ignoreCase = true)
        val facebookPattern = ".*(facebook\\.com|fbcdn\\.net|fbsbx\\.com).*".toRegex()
        var isFacebooknew = facebookPattern.matches(url)
        // Hide or show the download button based on YouTube link detection

        if (isFacebooknew) {
            view.evaluateJavascript(FaceBookScript.jsCode, null)
        }

        when {
            isFacebooknew -> {
                // Show only Facebook button
                buttonVisibilityYoutube.isFaceBookOpen(true)
                buttonVisibilityYoutube.isYoutubeOpen(false)
                youtube = false
            }
            isYoutube -> {
                // Show only YouTube button
                buttonVisibilityYoutube.isYoutubeOpen(true)
                buttonVisibilityYoutube.isFaceBookOpen(false)
                isFacebook = false

            }
            else -> {
                // Hide both buttons if neither YouTube nor Facebook
                buttonVisibilityYoutube.isFaceBookOpen(false)
                buttonVisibilityYoutube.isYoutubeOpen(false)
                isFacebook = false
                youtube = false
            }
        }
        videoAlert = null
        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
        val headers = pageTab.getHeaders() ?: emptyMap()
        val favi = pageTab.getFavicon() ?: view.favicon ?: favicon

        updateTabEvent.value = WebTab(
            url,
            view.title,
            favi,
            headers,
            view,
            id = pageTab.id
        )
        tabViewModel.onStartPage(url, view.title)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
        val isAdBlockerOn = settingsModel.isAdBlocker.get()
        val isAd = if (isAdBlockerOn) tabViewModel.isAd(url.url.toString()) else false
        val uri=url.url.toString()
        return if (url.url.toString().startsWith("http") && url.isForMainFrame && !isAd) {
            if (!tabViewModel.isTabInputFocused.get()) {
                tabViewModel.setTabTextInput(url.url.toString())
            }
            false
        } else {
            true
        }
    }

//    override fun onPageFinished(view: WebView, url: String) {
//        super.onPageFinished(view, url)
//        // Check if the URL is a YouTube link
//        // Inject JavaScript to detect iframes with YouTube links
//        view.evaluateJavascript(
//            """
//            (function() {
//                var iframes = document.getElementsByTagName('iframe');
//                for (var i = 0; i < iframes.length; i++) {
//                    var src = iframes[i].src;
//                    if (src.includes("youtube.com/embed") || src.includes("youtube-nocookie.com")) {
//                        return true; // Embedded YouTube video detected
//                    }
//                }
//                return false;
//            })();
//            """
//        ) { result ->
//            if (result == "true") {
//                buttonVisibilityYoutube.isYoutubeOpen(true)
//                buttonVisibilityYoutube.isFaceBookOpen(false)
//                isFacebook = false
//            }
//        }
//        val isYoutube = url.contains("youtube.com") || url.contains("youtu.be")
//        val facebookPattern = ".*(facebook\\.com|fbcdn\\.net|fbsbx\\.com).*".toRegex()
//        var isFacebooknew = facebookPattern.matches(url)
//        // Hide or show the download button based on YouTube link detection
//        if (isFacebooknew) {
//            view.evaluateJavascript(FaceBookScript.jsCode, null)
//        }
//        when {
//            isFacebooknew -> {
//                // Show only Facebook button
//                buttonVisibilityYoutube.isFaceBookOpen(true)
//                buttonVisibilityYoutube.isYoutubeOpen(false)
//                youtube = false
//            }
//            isYoutube -> {
//                // Show only YouTube button
//                buttonVisibilityYoutube.isYoutubeOpen(true)
//                buttonVisibilityYoutube.isFaceBookOpen(false)
//                isFacebook = false
//
//            }
//            else -> {
//                // Hide both buttons if neither YouTube nor Facebook
//                buttonVisibilityYoutube.isFaceBookOpen(false)
//                buttonVisibilityYoutube.isYoutubeOpen(false)
//                isFacebook = false
//                youtube = false
//            }
//        }
//        tabViewModel.finishPage(url)
//    }


    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        // Pattern to detect Facebook URLs
        val facebookPattern = ".*(facebook\\.com|fbcdn\\.net|fbsbx\\.com).*".toRegex()
        val isYoutube = url.contains("youtube", ignoreCase = true)
        val isFacebooknew = facebookPattern.matches(url)
        // Check for embedded YouTube videos using JavaScript
        view.evaluateJavascript(
            """
        (function() {
            var iframes = document.getElementsByTagName('iframe');
            for (var i = 0; i < iframes.length; i++) {
                var src = iframes[i].src;
                if (src.includes("youtube.com/embed") || src.includes("youtube-nocookie.com")) {
                    return true;
                }
            }
            return false;
        })();
        """
        ) { result ->
            if (result == "true") {
                // Detected embedded YouTube video, show YouTube button and hide Facebook button
                buttonVisibilityYoutube.isYoutubeOpen(true)
                buttonVisibilityYoutube.isFaceBookOpen(false)
                youtube = true
                isFacebook = false
            } else {
                // Handle direct links for YouTube and Facebook
                when {
                    isFacebooknew -> {
                        // Show Facebook button and execute script if it's a Facebook URL
                        buttonVisibilityYoutube.isFaceBookOpen(true)
                        buttonVisibilityYoutube.isYoutubeOpen(false)
                        view.evaluateJavascript(FaceBookScript.jsCode, null)
                        youtube = false
                    }
                    isYoutube -> {
                        // Show YouTube button for direct YouTube link
                        buttonVisibilityYoutube.isYoutubeOpen(true)
                        buttonVisibilityYoutube.isFaceBookOpen(false)
                        isFacebook = false
                    }
                    else -> {
                        // Hide both buttons if not YouTube or Facebook
                        buttonVisibilityYoutube.isFaceBookOpen(false)
                        buttonVisibilityYoutube.isYoutubeOpen(false)
                        isFacebook = false
                        youtube = false
                    }
                }
            }
        }
        // Notify ViewModel that page loading has finished
        tabViewModel.finishPage(url)
        updatevisitedhistry.invoke()
        onPageFinishedCallback?.invoke()
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
        val webView = pageTab.getWebView()
        if (view == webView && detail?.didCrash() == true) {
            webView?.destroy()
            return true
        }
        return super.onRenderProcessGone(view, detail)
    }

    private suspend fun saveUrlToHistory(url: String, favicon: Bitmap?, title: String?) {
        val isTitleEmpty = title?.trim()?.isEmpty() == true
        if (!isTitleEmpty && lastSavedTitleHistory != title && lastSavedHistoryUrl != url && url.isNotEmpty() && !url.contains("about:blank")) {
            lastSavedHistoryUrl = url
            lastSavedTitleHistory = title ?: ""
            val outputFavicon = FaviconUtils.bitmapToBytes(favicon)
            yield()
            historyModel.saveHistory(
                HistoryItem(
                    url = url, favicon = outputFavicon, title = title
                )
            )
            ++searchEventCounter
            if (searchEventCounter % 3 == 0) {
//                withContext(Dispatchers.Main) {
//                    mainActivity.mainViewModel.showInterstitialAdEvent.call()
//                }
            }
        }
    }


}
