package com.avd.ui.main.widgetactivity

import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.avd.R
import com.avd.data.remote.sealed.ApiState
import com.avd.databinding.ActivityFloatingBallBinding
import com.avd.ui.main.home.downloadapi.ApiViewModel
import com.avd.ui.main.home.downloadapi.SocialDownloaderResponse
import com.avd.ui.main.progress.ProgressViewModel
import com.avd.util.CookieUtils.LAST_DOWNLOADED
import com.avd.util.CookieUtils.toVideoInfo
import com.avd.util.Prefs
import com.avd.util.handleYoutubeDlUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

@AndroidEntryPoint
class FloatingBallActivity : AppCompatActivity() {
    var binding: ActivityFloatingBallBinding? = null

    private val viewModel: ApiViewModel by viewModels()
    val progressViewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFloatingBallBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        Log.d("clipboard_url", "Start Floating Activity")
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            setWindowAnimations(R.style.DialogAnimation)
        }

        binding?.btnClose?.setOnClickListener {
            finish()
        }
        observer()
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        lifecycleScope.launch {
            delay(300)
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().toString()
            if (text.isNotBlank()) {
                Log.d("Video_data", "Url: $text")
                val storeUrl = Prefs[LAST_DOWNLOADED, ""]
                Log.d("Video_data", "store Url: $storeUrl")
                val normalizedText = ApiViewModel.normalizeSocialDownloadUrl(text)
                if(storeUrl != normalizedText){
                    if (normalizedText != null && isSupportedSocialMediaUrl(normalizedText)) {
                        viewModel.socialDownloader(normalizedText)
                    } else {
                        Toast.makeText(
                            this@FloatingBallActivity,
                            "Invalid URL format: ${text.trim().take(120)}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(this@FloatingBallActivity, "This video already downloaded", Toast.LENGTH_SHORT).show()
                }

            }
        }


    }

    private fun observer() {

        lifecycleScope.launch {
            viewModel.socialDownloadState.collectLatest { state ->
                when (state) {
                    is ApiState.Loading -> {
                        // Show progress bar
                        binding?.pBar?.visibility=View.VISIBLE

                    }
                    is ApiState.Success -> {
                        // Hide progress bar & show data
                        binding?.pBar?.visibility =View.GONE
                        Log.d("Video_data", "store Url: ${state.data}")
                        val videoData = state.data
                        if (videoData.videos.isNotEmpty()) {
                            videoData.videos.first().url?.let { downloadVideo(it, videoData) }
                        }

                    }
                    is ApiState.Error -> {
                        // Hide progress bar & show error
                        binding?.pBar?.visibility =View.GONE
                        Toast.makeText(this@FloatingBallActivity, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                    is ApiState.Idle ->{
                        binding?.pBar?.visibility =View.GONE

                    }
                    else -> {
                        binding?.pBar?.visibility =View.GONE
                        Toast.makeText(this@FloatingBallActivity, "Some Thing Went Wrong! try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

    fun isSupportedSocialMediaUrl(text: String): Boolean {
        val regex = Regex(
            pattern = """^https?://(?:www\.)?(twitter\.com|t\.co|x\.com|facebook\.com|fb\.com|fb\.watch|m\.facebook\.com|instagram\.com|instagr\.am|tiktok\.com|vm\.tiktok\.com|vt\.tiktok\.com)(?:/.*)?$""",
            option = RegexOption.IGNORE_CASE
        )
        return regex.containsMatchIn(text.trim())
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun downloadVideo(
        url: String,
        response: SocialDownloaderResponse,
    ) {
        val isTwitter = response.platform.equals("twitter", ignoreCase = true)
        lifecycleScope.launch {
            if (isTwitter) {
                val videoInfo = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url.trim()).build()
                    val handledInfo = handleYoutubeDlUrl(request)
                    delay(500) // only if necessary — consider removing if not needed
                    handledInfo?.videoInfo
                }
                progressViewModel.downloadVideo(videoInfo, false)
            } else {
                progressViewModel.downloadVideo(response.toVideoInfo(), true)
            }
        }
    }
}
