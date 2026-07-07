package com.avd.ui.main.home.browser.detectedVideos

import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.avd.R
import com.avd.data.local.model.VideoInfoWrapper
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.repository.VideoRepository
import com.avd.ui.main.base.BaseViewModel
import com.avd.ui.main.home.browser.BrowserFragment
import com.avd.ui.main.home.browser.DownloadButtonState
import com.avd.ui.main.home.browser.DownloadButtonStateCanDownload
import com.avd.ui.main.home.browser.DownloadButtonStateCanNotDownload
import com.avd.ui.main.home.browser.DownloadButtonStateLoading
import com.avd.ui.main.home.browser.webTab.WebTabViewModel
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.AppLogger
import com.avd.util.ContextUtils
import com.avd.util.CookieUtils
import com.avd.util.NotificationsHelper
import com.avd.util.SingleLiveEvent
import com.avd.util.proxy_utils.OkHttpProxyClient
import com.avd.util.scheduler.BaseSchedulers
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import okhttp3.Response
import java.net.HttpCookie
import java.net.URL
import java.util.concurrent.Executors
import javax.inject.Inject


@HiltViewModel
class DetectedVideosTabViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val baseSchedulers: BaseSchedulers,
    private val okHttpProxyClient: OkHttpProxyClient,
) : BaseViewModel(), IVideoDetector {
    // key: videoInfo.id, value: format - string
    val selectedFormats = ObservableField<Map<String, String>>()

    // key: videoInfo.id, value: title - string
    val formatsTitles = ObservableField<Map<String, String>>()

    val selectedFormatUrl = ObservableField<String>()

    @Volatile
    var m3u8LoadingList = ObservableField<MutableSet<String>>(mutableSetOf())

    @Volatile
    var regularLoadingList = ObservableField<MutableSet<String>>(mutableSetOf())

    val showDetectedVideosEvent = SingleLiveEvent<Void?>()

    val videoPushedEvent = SingleLiveEvent<Void?>()

    @Volatile
    var downloadButtonState =
        ObservableField<DownloadButtonState>(DownloadButtonStateCanNotDownload())

    val executorReload = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    var webTabModel: WebTabViewModel? = null
    lateinit var settingsModel: SettingsViewModel
    val detectedVideosList = ObservableField(mutableSetOf<VideoInfo>())

    private val downloadButtonIcon = ObservableInt(R.drawable.invisible_24px)
    private val executorRegular = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Volatile
    private var verifyVideoLinkJobStorage = mutableMapOf<String, Disposable>()

    private val hasCheckLoadingsM3u8 = ObservableBoolean(false)
    private val hasCheckLoadingsRegular = ObservableBoolean(false)

    override fun start() {
        if (regularLoadingList.get() == null) {
            regularLoadingList.set(mutableSetOf())
        }
        if (m3u8LoadingList.get() == null) {
            m3u8LoadingList.set(mutableSetOf())
        }

        regularLoadingList.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val notEmpty = regularLoadingList.get()?.isNotEmpty() == true
                hasCheckLoadingsRegular.set(notEmpty)
                if (notEmpty) {
                    setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        })
        m3u8LoadingList.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val notEmpty = m3u8LoadingList.get()?.isNotEmpty() == true
                hasCheckLoadingsM3u8.set(notEmpty)
                if (notEmpty) {
                    setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        })
        downloadButtonState.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                when (downloadButtonState.get()) {
                    is DownloadButtonStateCanNotDownload -> downloadButtonIcon.set(R.drawable.refresh_24px)
                    is DownloadButtonStateCanDownload -> downloadButtonIcon.set(R.drawable.ic_download_24dp)
                    is DownloadButtonStateLoading -> {
                        downloadButtonIcon.set(R.drawable.invisible_24px)
                    }
                }
            }
        })
    }

    override fun stop() {
        cancelAllCheckJobs()
    }

    override fun onStartPage(url: String, userAgentString: String) {
        downloadButtonState.set(DownloadButtonStateCanNotDownload())
        detectedVideosList.set(mutableSetOf())
        cancelAllCheckJobs()
        val req = getRequestWithHeadersForUrl(
            url,
            url,
            userAgentString
        )?.build()
        if (req != null) {
            verifyLinkStatus(req)
        }
    }

    override fun hasCheckLoadingsRegular(): ObservableBoolean {
        return hasCheckLoadingsRegular
    }

    override fun hasCheckLoadingsM3u8(): ObservableBoolean {
        return hasCheckLoadingsM3u8
    }

    override fun showVideoInfo() {
        val state = downloadButtonState.get()
        if (state is DownloadButtonStateCanNotDownload) {
            webTabModel?.getTabTextInput()?.get()?.let {
                if (it.startsWith("http")) {
                    viewModelScope.launch(executorRegular) {
                        onStartPage(it.trim(), webTabModel?.userAgent?.get() ?: BrowserFragment.MOBILE_USER_AGENT)
                    }
                }
            }
        }
        if (detectedVideosList.get()?.isNotEmpty() == true) {
            showDetectedVideosEvent.call()
        }
    }

    override fun verifyLinkStatus(resourceRequest: Request, hlsTitle: String?) {
        // TODO list of sites, where youtube dl should be disabled
        if (resourceRequest.url.toString().contains("tiktok.")) {
            NotificationsHelper.istiktok=true
            return
        }else{
            NotificationsHelper.istiktok=false
        }

        val urlToVerify = resourceRequest.url.toString()

        val currentPageUrl = (webTabModel?.getTabTextInput()?.get() ?: "${resourceRequest.url}")

        if (urlToVerify.contains(".m3u8") || urlToVerify.contains(".mpd") || (urlToVerify.contains(
                ".txt"
            ) && currentPageUrl.contains("hentaihaven"))
        ) {
            startVerifyProcess(resourceRequest, true, hlsTitle)
        } else {
            if (urlToVerify.contains(
                    ".txt"
                )
            ) {
                return
            }
            if (settingsModel.getIsFindVideoByUrl().get()) {
                startVerifyProcess(resourceRequest, false)
            }
        }
    }

    private fun startVerifyProcess(resourceRequest: Request, isM3u8: Boolean, hlsTitle: String? = null) {

        val originalUrl = resourceRequest.url.toString()
        val taskUrlCleaned = normalizedUrl(originalUrl)

        val job = verifyVideoLinkJobStorage[taskUrlCleaned]
        if (job != null && !job.isDisposed || taskUrlCleaned.isEmpty()) {
            return
        }

        m3u8LoadingList.get()
            ?.toMutableSet()
            ?.apply { add(resourceRequest.url.toString()) }
            ?.let { m3u8LoadingList.set(it) }

        setButtonState(DownloadButtonStateLoading())

        verifyVideoLinkJobStorage[taskUrlCleaned] =
            io.reactivex.rxjava3.core.Observable.create { emitter ->
                val info = try {
                    videoRepository.getVideoInfo(resourceRequest)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                // always emit something so we can drive UI
                emitter.onNext(info ?: VideoInfo(id = ""))
                emitter.onComplete()
            }.subscribeOn(baseSchedulers.videoService)
                .observeOn(baseSchedulers.computation)
                .doFinally  {
                    // cleanup on complete *or* error
                    val url = resourceRequest.url.toString()

                    m3u8LoadingList.get()
                        ?.toMutableSet()
                        ?.apply {
                            remove(url)
                            remove(normalizedUrl(url))
                        }
                        ?.let { m3u8LoadingList.set(it) }

                    verifyVideoLinkJobStorage.remove(normalizedUrl(url))
                    resetButtonIfNoActiveDetection()
            }.subscribe(
                    { info ->
                        if (info.id.isNotEmpty()) {
                            if (info.isM3u8 && !hlsTitle.isNullOrEmpty()) {
                                info.title = hlsTitle
                            }
                            pushNewVideoInfoToAll(info)
                        } else {
                            setButtonState(DownloadButtonStateCanNotDownload())
                        }
                    },
                    { error ->
                        Log.e("VerifyProcess", "verification failed", error)
                        setButtonState(DownloadButtonStateCanNotDownload())
                    }
                )
    }


    fun pushNewVideoInfoToAll(newInfo: VideoInfo) {
        Log.e("checkName","pushVideoInfo thumb: ${newInfo?.thumbnail}")
        if (newInfo.id.isEmpty()) {
            return
        }
        val currentTabUrl = webTabModel?.getTabTextInput()?.get()
        val isTwitch = currentTabUrl?.contains(".twitch.") == true
        if ((isTwitch) && !newInfo.isMaster) {
            return
        }
        val detected = detectedVideosList.get()?.toList() ?: emptyList()
        var contains = false
        if (newInfo.isRegularDownload) {
            for (vid in detected) {
                val one = vid.firstUrlToString
                val searching = newInfo.firstUrlToString
                contains = one == searching
                if (contains) {
                    break
                }
            }
        } else {
            for (vid in detected) {
                for (vF in vid.formats.formats) {
                    for (k in newInfo.formats.formats) {
                        if (vF.url == k.url) {
                            contains = true
                            break
                        }
                    }
                    if (contains) {
                        break
                    }
                }
                if (vid.originalUrl == newInfo.originalUrl) {
                    contains = true
                    break
                }
            }
        }
        if (contains) {
            return
        }
        val list = detectedVideosList.get()?.toMutableSet() ?: mutableSetOf()
        list.add(newInfo)
        detectedVideosList.set(list)
        viewModelScope.launch(Dispatchers.Main) {
            videoPushedEvent.call()
        }
        setButtonState(DownloadButtonStateCanDownload(newInfo))
    }

    override fun getDownloadBtnIcon(): ObservableInt {
        return downloadButtonIcon
    }

//    override fun checkRegularMp4(request: Request?): Disposable? {
//        if (request == null) {
//            return null
//        }
//
//        val uriString = request.url.toString()
//
//        val isAd = webTabModel?.isAd(uriString) ?: false
//        if (!uriString.startsWith("http") || isAd) {
//            return null
//        }
//
//        val clearedUrl = uriString.split("?").first().trim()
//
//        if (clearedUrl.contains(Regex("^(.*\\.(apk|html|xml|ico|css|js|png|gif|json|jpg|jpeg|svg|woff|woff2|m3u8|mpd|ts|php|ttf|otf|eot|cur|webp|bmp|tif|tiff|psd|ai|eps|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|md|rtf|vtt|srt|swf|jar|log|txt))?$"))) {
//            return null
//        }
//
//        val headers = try {
//            request.headers.toMap().toMutableMap()
//        } catch (e: Throwable) {
//            mutableMapOf()
//        }
//
//        val disposable = io.reactivex.rxjava3.core.Observable.create<Unit> {
//            if (request.url.toString().contains(".mp4")) {
//                setButtonState(DownloadButtonStateLoading())
//            }
//            val loadings = regularLoadingList.get()
//            loadings?.add(request.url.toString())
//            regularLoadingList.set(loadings?.toMutableSet())
//            propagateCheckJob(uriString, headers)
//            it.onComplete()
//        }.subscribeOn(baseSchedulers.io).doOnComplete {
//            val loadings = regularLoadingList.get()
//            loadings?.remove(request.url.toString())
//            regularLoadingList.set(loadings?.toMutableSet())
//        }.onErrorComplete().doOnError {
//            AppLogger.d("Checking ERROR... $clearedUrl")
//        }.subscribe()
//
//        return disposable
//    }

    override fun checkRegularMp4(request: Request?): Disposable? {
        if (request == null) {
            Log.d("InstaGramDetection", "Request is null, returning without processing.")
            return null
        }

        val uriString = request.url.toString()
        Log.d("InstaGramDetection", "Processing URL: $uriString")

        val isAd = webTabModel?.isAd(uriString) ?: false
        if (!uriString.startsWith("http") || isAd) {
            Log.d("InstaGramDetection", "URL is either not HTTP or is identified as an ad. Skipping detection.")
            return null
        }

        val clearedUrl = uriString.split("?").first().trim()
        Log.d("InstaGramDetection", "Cleared URL (without parameters): $clearedUrl")

        if (clearedUrl.contains(Regex("^(.*\\.(apk|html|xml|ico|css|js|png|gif|json|jpg|jpeg|svg|woff|woff2|m3u8|mpd|ts|php|ttf|otf|eot|cur|webp|bmp|tif|tiff|psd|ai|eps|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|md|rtf|vtt|srt|swf|jar|log|txt))?$"))) {
            Log.d("InstaGramDetection", "URL matches excluded file types. Skipping detection.")
            return null
        }

        val headers = try {
            request.headers.toMap().toMutableMap().also {
                Log.d("InstaGramDetection", "Headers extracted: $it")
            }
        } catch (e: Throwable) {
            Log.e("InstaGramDetection", "Error extracting headers, proceeding with empty headers", e)
            mutableMapOf()
        }

        val disposable = io.reactivex.rxjava3.core.Observable.create<Unit> {
            if (request.url.toString().contains(".mp4")) {
                Log.d("InstaGramDetection", "URL contains .mp4, setting download button state to loading.")
                setButtonState(DownloadButtonStateLoading())
            }

            val loadings = regularLoadingList.get()
            loadings?.add(request.url.toString())
            regularLoadingList.set(loadings?.toMutableSet())
            Log.d("InstaGramDetection", "Added URL to loading list: ${request.url}")

            Log.d("InstaGramDetection", "Starting propagateCheckJob with URI: $uriString and headers: $headers")
            propagateCheckJob(uriString, headers)
            it.onComplete()
        }
            .subscribeOn(baseSchedulers.io)
            .doOnComplete {
                val loadings = regularLoadingList.get()
                val url = request.url.toString()
                loadings?.remove(url)
                loadings?.remove(normalizedUrl(url))
                regularLoadingList.set(loadings?.toMutableSet())
                resetButtonIfNoActiveDetection()
                //Log.d("InstaGramDetection", "Removed URL from loading list after completion: ${request.url}")
            }
            .onErrorComplete()
            .doOnError { error ->
                Log.e("InstaGramDetection", "Error during video check for URL: $clearedUrl", error)
            }
            .subscribe()

        Log.d("InstaGramDetection", "Disposable created and returned for URL: $uriString")
        return disposable
    }

    fun clearStaleLoadingIfNoVideos() {
        if (detectedVideosList.get()?.isNotEmpty() == true) {
            return
        }

        regularLoadingList.set(mutableSetOf())
        m3u8LoadingList.set(mutableSetOf())
        setButtonState(DownloadButtonStateCanNotDownload())
    }

    private fun resetButtonIfNoActiveDetection() {
        val hasDetectedVideos = detectedVideosList.get()?.isNotEmpty() == true
        val hasRegularLoading = regularLoadingList.get()?.isNotEmpty() == true
        val hasM3u8Loading = m3u8LoadingList.get()?.isNotEmpty() == true
        if (!hasDetectedVideos && !hasRegularLoading && !hasM3u8Loading) {
            setButtonState(DownloadButtonStateCanNotDownload())
        }
    }

    private fun normalizedUrl(url: String): String {
        return url.substringBefore("?").trim()
    }

    private val jobStorageLock = Any()
    override fun cancelAllCheckJobs() {
        try {
            // Lock access to prevent concurrent modification
            synchronized(jobStorageLock) {
                // 1) Reset loading indicators safely
                try {
                    regularLoadingList.set(mutableSetOf())
                    m3u8LoadingList.set(mutableSetOf())
                } catch (e: Throwable) {
                    AppLogger.e("Failed to reset loading lists: ${e.message}")
                }

                // 2) Cancel any scheduled executors safely
                try {
                    executorReload.cancel()
                } catch (e: Throwable) {
                    AppLogger.e("Failed to cancel executorReload: ${e.message}")
                }

                try {
                    executorRegular.cancel()
                } catch (e: Throwable) {
                    AppLogger.e("Failed to cancel executorRegular: ${e.message}")
                }

                // 3) Safely take a snapshot of jobs before iterating
                val jobsToDispose = try {
                    // Copy under the lock to avoid concurrent modification
                    ArrayList(verifyVideoLinkJobStorage.values)
                } catch (e: Throwable) {
                    AppLogger.e("Failed to snapshot job storage: ${e.message}")
                    emptyList()
                }

                // 4) Dispose or cancel all jobs
                for (job in jobsToDispose) {
                    try {
                        job.dispose() // use job.cancel() if you’re using kotlinx.coroutines.Job
                    } catch (e: Throwable) {
                        AppLogger.d("Failed to dispose job: ${e.message}")
                    }
                }

                // 5) Clear the storage
                try {
                    verifyVideoLinkJobStorage.clear()
                } catch (e: Throwable) {
                    AppLogger.e("Failed to clear job storage: ${e.message}")
                }
            }

        } catch (e: Throwable) {
            AppLogger.e("cancelAllCheckJobs failed: ${e.message}")
        }
    }


  /*  override fun cancelAllCheckJobs() {
        // 1) Reset loading indicators
        synchronized(jobStorageLock) {
            regularLoadingList.set(mutableSetOf())
            m3u8LoadingList.set(mutableSetOf())

            // 2) Cancel any scheduled executors
            executorReload.cancel()
            executorRegular.cancel()

            // 3) Safely copy active jobs and dispose
            val jobsToDispose = verifyVideoLinkJobStorage.values.toList() // safe snapshot
            for (job in jobsToDispose) {
                try {
                    job.dispose()
                } catch (e: Throwable) {
                    AppLogger.d("Failed to dispose job: ${e.message}")
                }
            }

            // 4) Clear the map
            verifyVideoLinkJobStorage.clear()
        }
    }
*/


//    fun setButtonState(state: DownloadButtonState) {
//        when (state) {
//            is DownloadButtonStateCanDownload -> {
//                downloadButtonState.set(state)
//            }
//
//            is DownloadButtonStateCanNotDownload -> {
//                val detectedSize = detectedVideosList.get()?.size
//                if (detectedSize == null || detectedSize == 0) {
//                    val impEl = regularLoadingList.get()?.find { it.contains(".mp4") }
//                    if (m3u8LoadingList.get()?.isEmpty() != true || (m3u8LoadingList.get()
//                            ?.isEmpty() == true && impEl != null)
//                    ) {
//                        downloadButtonState.set(DownloadButtonStateLoading())
//                    } else {
//                        downloadButtonState.set(DownloadButtonStateCanNotDownload())
//                    }
//                } else {
//                    downloadButtonState.set(
//                        DownloadButtonStateCanDownload(
//                            detectedVideosList.get()?.first()
//                        )
//                    )
//                }
//            }
//
//            is DownloadButtonStateLoading -> {
//                val list = detectedVideosList.get() ?: emptySet()
//                if (list.isEmpty()) {
//                    downloadButtonState.set(DownloadButtonStateLoading())
//                } else {
//                    downloadButtonState.set(DownloadButtonStateCanDownload(list.first()))
//                }
//            }
//        }
//    }


    fun setButtonState(state: DownloadButtonState) {
        when (state) {
            is DownloadButtonStateCanDownload -> {
                // Set the state to CanDownload directly
                downloadButtonState.set(state)
            }

            is DownloadButtonStateCanNotDownload -> {
                val detectedSize = detectedVideosList.get()?.size ?: 0 // Safely get size or 0 if null

                if (detectedSize == 0) { // No videos detected
                    Log.d("detectedSize","if==$detectedSize")
                   /* val impEl = regularLoadingList.get()
                        ?.toList()   // make a snapshot copy to avoid concurrent modification
                        ?.find { it.contains(".mp4") }*/

                    val impEl = regularLoadingList.get()?.let { HashSet(it) }?.find { it.contains(".mp4") }

                    // Check if m3u8LoadingList is not empty or if we have found a .mp4 file
                    if (m3u8LoadingList.get()?.isNotEmpty() == true || (m3u8LoadingList.get()?.isEmpty() == true && impEl != null)) {
                        // Set to Loading if m3u8List is not empty or a .mp4 file is found
                        downloadButtonState.set(DownloadButtonStateLoading())
                    } else {
                        // Otherwise, set to CanNotDownload
                        downloadButtonState.set(DownloadButtonStateCanNotDownload())
                    }
                } else {
                    Log.d("detectedSize","else==$detectedSize")
                    // Set state to CanDownload if we have detected videos
                    downloadButtonState.set(
                        DownloadButtonStateCanDownload(
                            detectedVideosList.get()?.first() // Get the first detected video
                        )
                    )
                }
            }

            is DownloadButtonStateLoading -> {
                val list = detectedVideosList.get() ?: emptySet() // Get detected video list or empty set if null

                // If the list is empty, keep the state as Loading
                if (list.isEmpty()) {
                    downloadButtonState.set(DownloadButtonStateLoading())
                } else {
                    // Otherwise, set the state to CanDownload with the first video in the list
                    downloadButtonState.set(DownloadButtonStateCanDownload(list.first()))
                }
            }
        }
    }



     fun getRequestWithHeadersForUrl(
        url: String,
        originalUrl: String,
        userAgent: String,
        alternativeHeaders: Map<String, String> = emptyMap()
    ): Request.Builder? {
        try {
            val cookies = try {
                CookieManager.getInstance().getCookie(url) ?: CookieManager.getInstance()
                    .getCookie(originalUrl) ?: ""
            } catch (e: Throwable) {
                ""
            }
            val stringBuilder = StringBuilder()
            if (cookies.isNotEmpty()) {
                for (cookie in cookies.split(";")) {
                    val parsedCookies = HttpCookie.parse(cookie)

                    for (httpCookie in parsedCookies) {
                        stringBuilder.append("${httpCookie.name}=${httpCookie.value};")
                    }
                }
            }

            if (alternativeHeaders.isEmpty()) {
                val builder = try {
                    Request.Builder().url(url.trim())
                } catch (e: Exception) {
                    null
                }
                builder?.addHeader("Referer", "https://${Uri.parse(originalUrl).host}/")

                builder?.addHeader("User-Agent", userAgent)

                try {
                    if (cookies.isNotEmpty()) {
                        builder?.addHeader("Cookie", stringBuilder.toString())
                    }
                } catch (e: Exception) {
                    AppLogger.d("Url parse error ${e.message}")
                }
                return builder

            } else {
                val builder = try {
                    Request.Builder().url(url.trim())
                } catch (e: Exception) {
                    null
                }
                builder?.headers(alternativeHeaders.toHeaders())
                if (cookies.isNotEmpty() && alternativeHeaders["Cookie"] == null) {
                    builder?.addHeader("Cookie", stringBuilder.toString())
                }

                return builder
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    private fun propagateCheckJob(url: String, headersMap: Map<String, String>) {
        val treshold = settingsModel.videoDetectionTreshold.get()
        var headers = headersMap.toMutableMap()
        val finlUrlPair = try {
            CookieUtils.getFinalRedirectURL(URL(Uri.parse(url).toString()), headers)
        } catch (e: Throwable) {
            null
        } ?: return

        try {
            val cookies = CookieManager.getInstance().getCookie(finlUrlPair.first.toString())
                ?: CookieManager.getInstance().getCookie(url) ?: ""
            if (cookies.isNotEmpty()) {
                headers["Cookie"] = cookies
            }
        } catch (_: Throwable) {

        }

        var respons: Response? = null
        try {
            headers = finlUrlPair.second.toMap().toMutableMap()
            val requestOk: Request =
                Request.Builder().url(finlUrlPair.first).headers(headers.toHeaders()).build()
            respons = okHttpProxyClient.getProxyOkHttpClient().newCall(requestOk).execute()

            val length = respons.body?.contentLength()
            val type = respons.body?.contentType()

            if (respons.code == 403 || respons.code == 401) {
                val finlUrlPairEmpty = try {
                    CookieUtils.getFinalRedirectURL(URL(Uri.parse(url).toString()), emptyMap())
                } catch (e: Throwable) {
                    null
                }

                if (finlUrlPairEmpty != null) {
                    val emptyHeadersReq = Request.Builder().url(finlUrlPairEmpty.first).build()
                    val emptyRes =
                        okHttpProxyClient.getProxyOkHttpClient().newCall(emptyHeadersReq).execute()
                    if (length != null) {
                        if (emptyRes.body?.contentType().toString()
                                .contains("video") && length > treshold
                        ) {
                            setVideoInfoWrapperFromUrl(
                                finlUrlPairEmpty.first,
                                webTabModel?.getTabTextInput()?.get(),
                                finlUrlPairEmpty.second.toMap(),
                                length
                            )
                            return
                        }
                    }
                }
            }

            val isTikTok = url.contains(".tiktok.com/")
            if (length != null) {
                if (type.toString()
                        .contains("video") && (length > treshold || (isTikTok && length > 1024 * 1024 / 3))
                ) {
                    setVideoInfoWrapperFromUrl(
                        finlUrlPair.first,
                        webTabModel?.getTabTextInput()?.get(),
                        finlUrlPair.second.toMap(),
                        length
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            respons?.close()
        }
    }

    private fun setVideoInfoWrapperFromUrl(
        url: URL,
        originalUrl: String?,
        alternativeHeaders: Map<String, String> = emptyMap(),
        contentLength: Long
    ) {
        try {
            if (!url.toString().startsWith("http")) {
                return
            }

            val builder = if (originalUrl != null) {
                Request.Builder().url(url.toString()).headers(alternativeHeaders.toHeaders())
            } else {
                null
            }

            val downloadUrls = listOfNotNull(
                builder?.build()
            )

            val video = VideoInfoWrapper(
                VideoInfo(
                    downloadUrls = downloadUrls,
                    title = webTabModel?.currentTitle?.get() ?: "no_title",
                    ext = "mp4",
                    originalUrl = webTabModel?.getTabTextInput()?.get() ?: "",
                    // TODO format regular file link
                    formats = VideFormatEntityList(
                        mutableListOf(
                            VideoFormatEntity(
                                formatId = "0",
                                format = ContextUtils.getApplicationContext()
                                    .getString(R.string.player_resolution),
                                ext = "mp4",
                                url = downloadUrls.first().url.toString(),
                                httpHeaders = downloadUrls.first().headers.toMap(),
                                fileSize = contentLength
                            )
                        )
                    ),
                    isRegularDownload = true
                )
            )
            Log.e("checkName","name: ${video.videoInfo?.name}")
            Log.e("checkName","title: ${video.videoInfo?.title}")
            Log.e("checkName","thumb: ${video.videoInfo?.thumbnail}")
            video.videoInfo?.let { pushNewVideoInfoToAll(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
