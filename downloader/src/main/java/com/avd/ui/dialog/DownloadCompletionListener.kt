package com.avd.ui.dialog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.util.FileUtil
import com.avd.util.SharedPrefHelper
import com.avd.util.downloaders.generic_downloader.models.VideoTaskState

/**
 * Listens for download completion via broadcast and progress updates, then shows the dialog once per download.
 */
class DownloadCompletionListener(
    private val fragment: Fragment,
    private val fileUtil: FileUtil
) : DefaultLifecycleObserver {

    private var broadcastReceiver: BroadcastReceiver? = null
    private val handledDownloadIds = mutableSetOf<String>()

    fun prepareForDownload(downloadId: String) {
        handledDownloadIds.remove(downloadId)
        DownloadDialogManager.resetCompletionSession()
        fragment.context?.let { SharedPrefHelper(it).resetDownloadDialogSession() }
    }

    override fun onStart(owner: LifecycleOwner) {
        registerBroadcastReceiver()
    }

    override fun onStop(owner: LifecycleOwner) {
        unregisterBroadcastReceiver()
    }

    private fun registerBroadcastReceiver() {
        if (broadcastReceiver != null) return
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != DownloadCompletionBroadcast.ACTION) return
                if (!fragment.isAdded) return
                val activity = fragment.activity as? FragmentActivity ?: return
                val info = DownloadCompletionInfo.fromIntentExtras(intent.extras)
                val downloadId = intent.extras?.getString(DownloadCompletionBroadcast.EXTRA_DOWNLOAD_ID).orEmpty()
                    .ifBlank { info.videoName.ifBlank { info.videoTitle } }
                showCompletionDialog(activity, downloadId, info)
            }
        }
        ContextCompat.registerReceiver(
            fragment.requireContext(),
            broadcastReceiver,
            IntentFilter(DownloadCompletionBroadcast.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterBroadcastReceiver() {
        broadcastReceiver?.let {
            runCatching { fragment.requireContext().unregisterReceiver(it) }
        }
        broadcastReceiver = null
    }

    fun onProgressInfosChanged(progressInfos: List<ProgressInfo>?) {
        if (!fragment.isAdded || progressInfos.isNullOrEmpty()) return
        val activity = fragment.activity as? FragmentActivity ?: return
        progressInfos
            .filter { it.downloadStatus == VideoTaskState.SUCCESS && it.progress >= 100 }
            .forEach { progressInfo ->
                val info = DownloadCompletionBroadcast.buildFromProgressInfo(progressInfo, fileUtil)
                showCompletionDialog(activity, progressInfo.id, info)
            }
    }

    private fun showCompletionDialog(
        activity: FragmentActivity,
        downloadId: String,
        completionInfo: DownloadCompletionInfo
    ) {
        if (downloadId.isNotBlank() && !handledDownloadIds.add(downloadId)) {
            Log.d(TAG, "Completion already handled for: $downloadId")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return

        Log.d(TAG, "Showing completion dialog for: ${completionInfo.videoTitle}")
        DownloadDialogManager.showDownloadCompletionDialog(
            context = activity,
            completionInfo = completionInfo,
            onDismiss = {
                fragment.context?.let { SharedPrefHelper(it).resetDownloadDialogSession() }
            }
        )
    }

    companion object {
        private const val TAG = "TESTdialogue"
    }
}
