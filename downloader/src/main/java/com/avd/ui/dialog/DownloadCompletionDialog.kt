package com.avd.ui.dialog

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.avd.R
import com.avd.util.FileUtil
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Safe download completion dialog that prevents crashes and ANRs
 * Only shows in the downloader module
 */
class DownloadCompletionDialog : DialogFragment() {

    private var onDownloadMoreClick: (() -> Unit)? = null
    private var onGoToDownloadsClick: (() -> Unit)? = null
    private var onDismissListener: (() -> Unit)? = null
    private var completionInfo: DownloadCompletionInfo = DownloadCompletionInfo()

    companion object {
        private const val TAG = "TESTdialogue"
        private const val ARG_COMPLETION_INFO = "completion_info"

        fun newInstance(info: DownloadCompletionInfo = DownloadCompletionInfo()): DownloadCompletionDialog {
            return DownloadCompletionDialog().apply {
                arguments = Bundle().apply {
                    putBundle(ARG_COMPLETION_INFO, info.toBundle())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        completionInfo = DownloadCompletionInfo.fromBundle(arguments?.getBundle(ARG_COMPLETION_INFO))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_download_completion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setupViews(view)
            bindVideoInfo(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up dialog views: ${e.message}")
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    private fun setupViews(view: View) {
        view.findViewById<ImageView>(R.id.btn_close)?.setOnClickListener {
            Log.d(TAG, "Close button clicked — dismissing dialog")
            dismissSafely()
        }

        view.findViewById<Button>(R.id.btn_explore)?.setOnClickListener {
            Log.d(TAG, "'Explore' button clicked")
            handleGoToDownloadsClick()
        }

        view.findViewById<Button>(R.id.btn_watch)?.setOnClickListener {
            Log.d(TAG, "'Watch' button clicked")
            handleWatchClick()
        }
    }

    private fun bindVideoInfo(view: View) {
        view.findViewById<TextView>(R.id.tv_video_title)?.text = completionInfo.videoTitle

        val fileSizeView = view.findViewById<TextView>(R.id.tv_file_size)
        if (completionInfo.fileSize > 0) {
            fileSizeView?.text = FileUtil.getFileSizeReadable(completionInfo.fileSize.toDouble())
            fileSizeView?.isVisible = true
        } else {
            fileSizeView?.isVisible = false
        }

        val durationBadge = view.findViewById<View>(R.id.layout_duration_badge)
        val durationView = view.findViewById<TextView>(R.id.tv_duration)
        if (completionInfo.duration > 0) {
            durationView?.text = formatDuration(completionInfo.duration)
            durationBadge?.isVisible = true
        } else {
            durationBadge?.isVisible = false
        }

        val thumbnailView = view.findViewById<ImageView>(R.id.iv_thumbnail)
        val thumbnail = completionInfo.thumbnail
        if (!thumbnail.isNullOrBlank()) {
            Glide.with(requireContext())
                .load(thumbnail)
                .error(R.drawable.ic_vid_thumb)
                .placeholder(R.drawable.ic_vid_thumb)
                .centerCrop()
                .into(thumbnailView)
        } else {
            thumbnailView?.setImageResource(R.drawable.ic_vid_thumb)
        }
    }

    private fun formatDuration(durationInSeconds: Long): String {
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val seconds = durationInSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun handleDownloadMoreClick() {
        try {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    onDownloadMoreClick?.invoke()
                    dismissSafely()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling explore click: ${e.message}")
        }
    }

    private fun handleWatchClick() {
        val playUri = completionInfo.playUri.ifBlank { completionInfo.filePath }
        if (playUri.isBlank()) {
            Log.e(TAG, "No play URI available for watch action")
            return
        }
        try {
            val intent = Intent(
                requireContext(),
                Class.forName("com.video.avd.ui.player.PlayerVideoActivity")
            )
            val bundle = Bundle().apply {
                putBoolean("isliveuri", true)
                putBoolean("alreadyAdShown", false)
                putString("uri", playUri)
            }
            intent.putExtras(bundle)
            startActivity(intent)
            dismissSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening player: ${e.message}")
        }
    }

    private fun handleGoToDownloadsClick() {
        try {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    onGoToDownloadsClick?.invoke()
                    dismissSafely()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling go to downloads click: ${e.message}")
        }
    }

    private fun dismissSafely() {
        try {
            if (isAdded && !isRemoving) {
                dismiss()
                onDismissListener?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing dialog: ${e.message}")
        }
    }

    override fun onDestroyView() {
        try {
            super.onDestroyView()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroyView: ${e.message}")
        }
    }

    fun setOnDownloadMoreClickListener(listener: () -> Unit): DownloadCompletionDialog {
        this.onDownloadMoreClick = listener
        return this
    }

    fun setOnGoToDownloadsClickListener(listener: () -> Unit): DownloadCompletionDialog {
        this.onGoToDownloadsClick = listener
        return this
    }

    fun setOnDismissListener(listener: () -> Unit): DownloadCompletionDialog {
        this.onDismissListener = listener
        return this
    }
}
