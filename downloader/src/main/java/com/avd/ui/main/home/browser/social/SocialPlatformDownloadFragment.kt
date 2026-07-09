package com.avd.ui.main.home.browser.social

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.avd.R
import com.avd.data.local.model.VideoInfoWrapper
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.remote.sealed.ApiState
import com.avd.data.remote.service.VideoServiceLocal.Companion.COOKIE_HEADER
import com.avd.data.remote.service.VideoServiceLocal.Companion.MP4_EXT
import com.avd.databinding.DialogFetchingVideoBinding
import com.avd.databinding.FragmentSocialPlatformDownloadBinding
import com.avd.ui.dialog.DownloadCompletionListener
import com.avd.util.FileUtil
import com.avd.ui.main.home.browser.BaseWebTabFragment
import com.avd.ui.main.home.browser.homeTab.BrowserHomeFragment
import com.avd.ui.main.home.downloadapi.ApiViewModel
import com.avd.ui.main.home.downloadapi.SocialDownloaderResponse
import com.avd.ui.main.home.downloadapi.VideoItem
import com.avd.ui.main.home.downloadapi.adapter.SocialFormatAdapter
import com.avd.ui.main.progress.ProgressViewModel
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.home_native
import com.avd.util.AdBlockerHelper.hideLoading
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_browser
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.isDownloading
import com.avd.util.AdBlockerHelper.isProVersion
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.refreshAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.AppLogger
import com.avd.util.CookieUtils
import com.avd.util.DownloadDialogType
import com.avd.util.FirebaseEvents.fbEvents
import com.avd.util.NetworkUtils
import com.avd.util.RemoteConfigHelper
import com.avd.util.YoutubeDlUtils
import com.avd.util.showDownloadDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SocialPlatformDownloadFragment : BaseWebTabFragment() {

    private var _binding: FragmentSocialPlatformDownloadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ApiViewModel by activityViewModels()
    private val progressViewModel: ProgressViewModel by viewModels()

    @Inject
    lateinit var fileUtil: FileUtil

    private lateinit var downloadCompletionListener: DownloadCompletionListener

    private var platform: SocialPlatform = SocialPlatform.FACEBOOK
    private var fetchingDialog: Dialog? = null
    private var activeBottomSheet: BottomSheetDialog? = null
    private var lastHandledClipboardUrl: String? = null
    private var expectingDownloadResult = false
    private var copiedLinkDialog: AlertDialog? = null

    override fun shareWebLink() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialPlatformDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }
    fun navigateToHome() {
        if(interHome!=null) {
            interHome?.let {
                showInterstitial(true, it, requireActivity(), {
                    try {
                        val currentFragment = this
                        val activityFragmentContainer = currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                        activityFragmentContainer?.let {
                            val transaction = currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                            transaction.replace(it.id, BrowserHomeFragment.newInstance())
                            transaction.addToBackStack("home")
                            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            transaction.commit()
                        }
                    } catch (e: ClassCastException) {
                        AppLogger.d("Can't get the fragment manager with this")
                    }
                },inter_browser)
            }

        }
        else{
            try {
                val currentFragment = this
                val activityFragmentContainer = currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                activityFragmentContainer?.let {
                    val transaction = currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(it.id, BrowserHomeFragment.newInstance())
                    transaction.addToBackStack("home")
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    transaction.commit()
                }
            } catch (e: ClassCastException) {
                AppLogger.d("Can't get the fragment manager with this")
            }
        }

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fromIcon = arguments?.getBoolean(ARG_FROM_ICON) == true

        if (fromIcon) {
            platform = arguments?.getString(ARG_PLATFORM)?.let { platformName ->
                runCatching { SocialPlatform.valueOf(platformName) }.getOrNull()
            } ?: SocialPlatform.FACEBOOK
            binding.etVideoLink.text?.clear()
            viewModel.texturl.value?.takeIf { it.isNotBlank() }?.trim()?.let { lastHandledClipboardUrl = it }
        } else {
            val initialUrl = arguments?.getString(ARG_INITIAL_URL).orEmpty()
            platform = SocialPlatform.fromInput(initialUrl) ?: SocialPlatform.FACEBOOK
            if (initialUrl.isNotBlank()) {
                applyUrlToUi(initialUrl)
                lastHandledClipboardUrl = initialUrl.trim()
            }
        }

        progressViewModel.start()
        downloadCompletionListener = DownloadCompletionListener(this, fileUtil)
        viewLifecycleOwner.lifecycle.addObserver(downloadCompletionListener)
        progressViewModel.progressInfos.addOnPropertyChangedCallback(object :
            androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                downloadCompletionListener.onProgressInfosChanged(progressViewModel.progressInfos.get())
            }
        })
        fetchingDialog = showFetchingVideoDialog()
        applyPlatformUi(platform)
        bindHowToSteps(platform)

        binding.btnBack.setOnClickListener {
            navigateToHome()
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateToHome()
                }
            })
        binding.btnPaste.setOnClickListener { pasteFromClipboard() }
        binding.btnDownload.setOnClickListener { startDownloadFlow() }
        binding.btnOpenApp.setOnClickListener { openPlatformApp() }

        binding.progressloading.setOnClickListener {}

        setupNativeAd()
        observeClipboardLink()
        observeDownloadState()
    }

    override fun onDestroyView() {
        activeBottomSheet?.dismiss()
        copiedLinkDialog?.dismiss()
        copiedLinkDialog = null
        fetchingDialog?.dismiss()
        progressViewModel.stop()
        _binding = null
        super.onDestroyView()
    }

    private fun applyPlatformUi(platform: SocialPlatform) {
        binding.tvToolbarTitle.text = platform.displayName
        binding.ivToolbarPlatform.setImageResource(platform.iconRes)
        binding.ivAppIcon.setImageResource(platform.iconRes)
        binding.tvAppName.text = platform.displayName
        binding.tvAppSubtitle.setText(platform.appSubtitleRes)
    }

    private fun bindHowToSteps(platform: SocialPlatform) {
        bindStep(binding.step1.root, "1", platform.step1TitleRes, platform.step1SubtitleRes, platform.step1ImageRes)
        bindStep(binding.step2.root, "2", platform.step2TitleRes, platform.step2SubtitleRes, platform.step2ImageRes)
        bindStep(binding.step3.root, "3", platform.step3TitleRes, platform.step3SubtitleRes, platform.step3ImageRes)
       // bindStep(binding.step4.root, "4", platform.step4TitleRes, platform.step4SubtitleRes, platform.step4ImageRes)
    }

    private fun bindStep(
        stepRoot: View,
        number: String,
        titleRes: Int,
        subtitleRes: Int,
        imageRes: Int
    ) {
        stepRoot.findViewById<TextView>(R.id.tvStepNumber).text = number
        stepRoot.findViewById<TextView>(R.id.tvStepTitle).setText(titleRes)
        stepRoot.findViewById<TextView>(R.id.tvStepSubtitle).setText(subtitleRes)
        stepRoot.findViewById<ImageView>(R.id.ivStepImage).setImageResource(imageRes)
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pasted = clip.getItemAt(0).coerceToText(requireContext()).toString().trim()
            if (pasted.isNotEmpty()) {
                lastHandledClipboardUrl = pasted
                applyUrlToUi(pasted)
            }
        } else {
            Toast.makeText(requireContext(), R.string.social_paste_here, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyUrlToUi(url: String) {
        binding.etVideoLink.setText(url)
        SocialPlatform.fromInput(url)?.let { detected ->
            platform = detected
            applyPlatformUi(platform)
            bindHowToSteps(platform)
        }
    }

    private fun startDownloadFlow() {
        hideKeyboard()
        val url = binding.etVideoLink.text.toString().trim()
        if (url.isBlank()) {
            Log.d("muaz_debug","Empty URL")
            requireContext().showDownloadDialog(type = DownloadDialogType.EMPTY_URL)
            return
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            requireContext().showDownloadDialog(
                type = DownloadDialogType.INTERNET_ERROR,
                onRetryConnection = {
                    Toast.makeText(requireContext(), "Connect internet first", Toast.LENGTH_SHORT).show()
                }
            )
            return
        }
        if (!SocialPlatform.isSupportedSocialMediaUrl(url)) {
            requireContext().showDownloadDialog(type = DownloadDialogType.INVALID_URL)
            return
        }
        lastHandledClipboardUrl = url
        expectingDownloadResult = true
        viewModel.socialDownloader(url)
    }

    private fun openPlatformApp() {
        val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(platform.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(platform.webUrl)))
        }
    }

    private fun observeClipboardLink() {
        viewModel.texturl.observe(viewLifecycleOwner) { url ->
            if (url.isNullOrBlank()) return@observe
            val trimmed = url.trim()
            if (trimmed == lastHandledClipboardUrl) return@observe

            lastHandledClipboardUrl = trimmed
            applyUrlToUi(trimmed)

            if (SocialPlatform.isSupportedSocialMediaUrl(trimmed)) {
                showCopiedLinkDetectedDialog(trimmed)
            }
        }
    }

    private fun showCopiedLinkDetectedDialog(url: String) {
        if (copiedLinkDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_copied_link_detected, null)
        val copiedLink = dialogView.findViewById<TextView>(R.id.tvCopiedLink)
        val cancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val download = dialogView.findViewById<TextView>(R.id.btnDownload)

        copiedLink.text = url

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

        download.setOnClickListener {
            copiedLinkDialog?.dismiss()
            applyUrlToUi(url)
            startDownloadFlow()
        }

        copiedLinkDialog?.show()
        copiedLinkDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun observeDownloadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.socialDownloadState.collectLatest { state ->
                if (!isVisible || !isAdded) return@collectLatest
                when (state) {
                    is ApiState.Idle -> {
                        binding.progressloading.visibility = View.GONE
                        binding.videoProgressBar.visibility = View.GONE
                        isDownloading = false
                    }

                    else -> {
                        if (!expectingDownloadResult) return@collectLatest
                        when (state) {
                            is ApiState.Loading -> {
                                isDownloading = true
                                binding.progressloading.visibility = View.VISIBLE
                                binding.videoProgressBar.visibility = View.VISIBLE
                            }

                            is ApiState.Success -> {
                                expectingDownloadResult = false
                                binding.progressloading.visibility = View.GONE
                                binding.videoProgressBar.visibility = View.GONE
                                if (isAdded && view != null) {
                                    showSocialDownloadOptions(state.data)
                                }
                            }

                            is ApiState.Error -> {
                                expectingDownloadResult = false
                                requireContext().showDownloadDialog(type = DownloadDialogType.INVALID_URL)
                                binding.progressloading.visibility = View.GONE
                                binding.videoProgressBar.visibility = View.GONE
                                isDownloading = false
                            }

                            else -> {
                                expectingDownloadResult = false
                                requireContext().showDownloadDialog(type = DownloadDialogType.INVALID_URL)
                                binding.progressloading.visibility = View.GONE
                                binding.videoProgressBar.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun preloadDownloadInterstitial() {
        if (isProVersion.value == true || interHome != null) return
        loadFallbackInterstitialAd(
            requireActivity(),
            requireActivity().resources.getString(R.string.Interstitial_Home_ID_High),
            requireActivity().resources.getString(R.string.Interstitial_Home_ID),
            inter_home_high,
            inter_home_normal,
            { interHome = it },
            { interHome = it }
        )
    }

    private fun runDownloadWithInterstitial(onProceed: () -> Unit) {
        if (isProVersion.value == true) {
            onProceed()
            return
        }

        fun showLoadedInterstitial() {
            val ad = interHome ?: run {
                onProceed()
                return
            }
            showInterstitial(false, ad, requireActivity(), onProceed, inter_home)
        }

        if (interHome != null) {
            showLoadedInterstitial()
            return
        }

        AdBlockerHelper.showLoading(requireActivity(), "Loading Ad...")
        loadFallbackInterstitialAd(
            requireActivity(),
            requireActivity().resources.getString(R.string.Interstitial_Home_ID_High),
            requireActivity().resources.getString(R.string.Interstitial_Home_ID),
            inter_home_high,
            inter_home_normal,
            onAdLoadedHigh = { ad ->
                interHome = ad
                hideLoading()
                showInterstitial(false, ad, requireActivity(), onProceed, inter_home)
            },
            onAdLoadedNormal = { ad ->
                interHome = ad
                hideLoading()
                showInterstitial(false, ad, requireActivity(), onProceed, inter_home)
            },
            onAdFailed = {
                hideLoading()
                onProceed()
            }
        )
    }

    private fun showSocialDownloadOptions(videosResponse: SocialDownloaderResponse) {
        var selectedFormat: VideoItem? = null
        var position = 0
        AdBlockerHelper.loadRewardedAd(requireContext(), highRequest = true, lowRequest = false)
        preloadDownloadInterstitial()
        activeBottomSheet?.dismiss()
        activeBottomSheet = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_download, null)
        val recyclerView = sheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFormats)
        val thumbnail = sheetView.findViewById<com.avd.ui.main.home.CustomImageView>(R.id.thumbnail)
        val title = sheetView.findViewById<TextView>(R.id.title)
        val watchAd = sheetView.findViewById<TextView>(R.id.watchAd)
        if (isProVersion.value == true) {
            watchAd.visibility = View.GONE
        }
        val download = sheetView.findViewById<ConstraintLayout>(R.id.cl_download)
        val watch = sheetView.findViewById<ConstraintLayout>(R.id.cl_watch)
        sheetView.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            activeBottomSheet?.dismiss()
        }
        recyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)

        if (videosResponse.videos.isNotEmpty()) {
            selectedFormat = videosResponse.videos[0]
            recyclerView.adapter = SocialFormatAdapter(videosResponse.videos) { format, pos ->
                selectedFormat = format
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
                bundle.putString("uri", selectedFormat?.url)
                intent.putExtras(bundle)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        isDownloading = false
        viewModel.clearDownloadState()
        viewModel.texturl.value = ""

        download.setOnClickListener {
            fbEvents("download_click", "Downloader", requireContext())
            runDownloadWithInterstitial {
                val outputDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                selectedFormat?.url?.let { videoUrl ->
                    if (outputDir != null) {
                        downloadVideo(videoUrl, vTitle, videosResponse, position)
                    }
                }
                binding.etVideoLink.text?.clear()
                activeBottomSheet?.dismiss()
            }
        }

        activeBottomSheet?.setContentView(sheetView)
        activeBottomSheet?.show()

        val bottomSheet = activeBottomSheet?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) as? FrameLayout
        bottomSheet?.apply {
            background = null
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        sheetView.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
    }

    private fun downloadVideo(
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
                    delay(500)
                    handledInfo.videoInfo
                }
                fetchingDialog?.dismiss()
                videoInfo?.let {
                    downloadCompletionListener.prepareForDownload(it.id)
                    progressViewModel.downloadVideo(it, false, pos)
                }
            } else {
                val info = videosResponse.toVideoInfo(title, url)
                downloadCompletionListener.prepareForDownload(info.id)
                progressViewModel.downloadVideo(info, true, 0)
            }
        }
    }

    private fun handleYoutubeDlUrl(url: Request): VideoInfoWrapper {
        return try {
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
                val formats = info?.formats?.map { videoEntityFromFormat(it) }
                val filtered = arrayListOf<VideoFormatEntity>()
                val listFormats = VideFormatEntityList(
                    filtered.ifEmpty {
                        formats?.filter { !(it.acodec != "none" && it.vcodec == "none") }
                    } ?: emptyList()
                )
                if (listFormats.formats.isEmpty()) throw Exception("Audio Only Detected")
                VideoInfoWrapper(VideoInfo(title = info?.title ?: "no title").also { videoInfo ->
                    videoInfo.ext = info?.ext ?: MP4_EXT
                    videoInfo.thumbnail = info?.thumbnail ?: ""
                    videoInfo.duration = info?.duration?.toLong()!!
                    videoInfo.originalUrl = url.url.toString()
                    videoInfo.downloadUrls = emptyList()
                    videoInfo.formats = listFormats
                    videoInfo.isRegularDownload = false
                })
            } finally {
                tmpCookieFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            VideoInfoWrapper(VideoInfo(title = "no title"))
        }
    }

    private fun videoEntityFromFormat(videoFormat: com.avd.youtubedl.VideoFormat): VideoFormatEntity {
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

    private fun SocialDownloaderResponse.toVideoInfo(title: String, selectedUrl: String): VideoInfo {
        val refererPlatform = platform?.lowercase() ?: this@SocialPlatformDownloadFragment.platform.displayName.lowercase()
        val socialHeaders = RemoteConfigHelper.getSocialDownloaderHeaders()
        val requestBuilder = Request.Builder()
            .url(selectedUrl)
            .addHeader("User-Agent", "Mozilla/5.0")
            .addHeader("Referer", "https://www.$refererPlatform.com/")
        socialHeaders.forEach { (name, value) -> requestBuilder.addHeader(name, value) }
        val downloadHeaders = mutableMapOf(
            "User-Agent" to "Mozilla/5.0",
            "Referer" to "https://www.$refererPlatform.com/"
        )
        downloadHeaders.putAll(socialHeaders)
        Log.d(
            "SocialDownloaderDownload",
            "Prepared regular download url=$selectedUrl platform=$refererPlatform headers=${downloadHeaders.keys}"
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
                        url = selectedUrl,
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

    private fun showFetchingVideoDialog(): Dialog {
        val dialogBinding = DialogFetchingVideoBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private fun setupNativeAd() {
 isProVersion.observe(viewLifecycleOwner) { isPro ->
            if (isPro == true) {
                binding.flAdplace.visibility = View.GONE
            } else {
                binding.flAdplace.visibility = View.VISIBLE
                refreshAd(this, requireContext(), true, binding.flAdplace, home_native)
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etVideoLink.windowToken, 0)
    }

    companion object {
        const val TAG = "social_platform_download"
        private const val ARG_INITIAL_URL = "arg_initial_url"
        private const val ARG_PLATFORM = "arg_platform"
        private const val ARG_FROM_ICON = "arg_from_icon"

        fun isVisibleIn(activity: FragmentActivity): Boolean {
            val container = activity.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                ?: return false
            val current = activity.supportFragmentManager.findFragmentById(container.id)
            return current is SocialPlatformDownloadFragment && current.isAdded && current.isVisible
        }

        fun newInstance(initialUrl: String): SocialPlatformDownloadFragment {
            return SocialPlatformDownloadFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_URL, initialUrl)
                    putBoolean(ARG_FROM_ICON, false)
                }
            }
        }

        fun newInstanceFromIcon(platform: SocialPlatform): SocialPlatformDownloadFragment {
            return SocialPlatformDownloadFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLATFORM, platform.name)
                    putBoolean(ARG_FROM_ICON, true)
                }
            }
        }
    }
}
