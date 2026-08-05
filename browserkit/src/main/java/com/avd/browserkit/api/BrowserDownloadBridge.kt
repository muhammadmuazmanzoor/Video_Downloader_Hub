package com.avd.browserkit.api

import com.avd.browserkit.download.BrowserDownloadStatus
import com.avd.browserkit.detection.DetectedVideoInfo

data class BrowserDownloadSnapshot(
    val taskId: String,
    val title: String,
    val pageUrl: String,
    val percent: Int,
    val status: BrowserDownloadStatus,
    val filePath: String? = null,
    val qualityLabel: String = "Default",
)

data class BrowserDownloadResult(
    val taskId: String,
    val title: String,
    val filePath: String?,
    val success: Boolean,
)

/**
 * Host request for Xilli + Super Avd triple downloader:
 * - [useAvd]=true → Avd HLS/MPD segment worker
 * - [useYtdlp]=true → yt-dlp (Facebook CDN / legacy manifests)
 * - else → regular OkHttp with headers (Instagram / direct CDN)
 */
data class BrowserHostDownloadRequest(
    val title: String,
    val pageUrl: String,
    val downloadUrl: String,
    val qualityLabel: String,
    val streamType: String,
    val headers: Map<String, String>,
    val useYtdlp: Boolean,
    val facebookMode: Boolean = false,
    val useAvd: Boolean = false,
    val formatId: String = "",
    val audioUrl: String? = null,
    val isLive: Boolean = false,
)

interface BrowserDownloadBridge {
    fun onTaskUpdated(snapshot: BrowserDownloadSnapshot) {}
    fun onTaskCompleted(result: BrowserDownloadResult) {}
    fun onTaskFailed(taskId: String, message: String) {}
    fun restartHostDownload(task: BrowserSharedDownloadTask): Boolean = false
    fun openDownloadQueue(): Boolean = false

    /**
     * Host app handles download (CustomRegular / yt-dlp).
     * @return true if host accepted the job (browserkit must not start its own worker).
     */
    fun enqueueHostDownload(request: BrowserHostDownloadRequest): Boolean = false

    /**
     * Called when the home button or home menu option is clicked in the browser.
     */
    fun onHomeClicked(activity: androidx.fragment.app.FragmentActivity, info: DetectedVideoInfo?) {}
}
