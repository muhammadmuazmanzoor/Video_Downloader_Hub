package com.video.avd.downloader

import android.content.Context
import android.util.Log
import com.avd.browserkit.api.BrowserDownloadBridge
import com.avd.browserkit.api.BrowserDownloadResult
import com.avd.browserkit.api.BrowserDownloadSharedStore
import com.avd.browserkit.api.BrowserDownloadSnapshot
import com.avd.browserkit.api.BrowserHostDownloadRequest
import com.avd.browserkit.api.BrowserSharedDownloadTask
import com.avd.util.DownloaderModuleNavigator
object BrowserKitBridge : BrowserDownloadBridge {
    private const val TAG = "BrowserKitBridge"
    private const val DOWNLOAD_QUEUE_PAGE_INDEX = 1

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "init context=${context.packageName}")
    }

    override fun enqueueHostDownload(request: BrowserHostDownloadRequest): Boolean {
        Log.d(
            TAG,
            "enqueueHostDownload stream=${request.streamType} ytdlp=${request.useYtdlp} avd=${request.useAvd} " +
                "fb=${request.facebookMode} live=${request.isLive} url=${request.downloadUrl} page=${request.pageUrl}",
        )
        val ctx = appContext ?: return false
        return BrowserHostDownloader.enqueue(ctx, request)
    }

    override fun restartHostDownload(task: BrowserSharedDownloadTask): Boolean {
        val ctx = appContext ?: return false
        Log.d(TAG, "restartHostDownload taskId=${task.taskId} status=${task.status} url=${task.downloadUrl}")
        return BrowserHostDownloader.restart(ctx, task)
    }

    override fun openDownloadQueue(): Boolean {
        val mainViewModel = DownloaderModuleNavigator.mainViewModel
        if (mainViewModel == null) {
            Log.w(TAG, "openDownloadQueue skipped: mainViewModel is null")
            return false
        }
        Log.d(TAG, "openDownloadQueue route=$DOWNLOAD_QUEUE_PAGE_INDEX")
        mainViewModel.isBrowserCurrent.set(false)
        mainViewModel.currentItem.set(DOWNLOAD_QUEUE_PAGE_INDEX)
        return true
    }

    override fun onTaskUpdated(snapshot: BrowserDownloadSnapshot) {
        BrowserDownloadSharedStore.update(
            taskId = snapshot.taskId,
            title = snapshot.title,
            pageUrl = snapshot.pageUrl,
            percent = snapshot.percent,
            status = snapshot.status,
            filePath = snapshot.filePath,
            qualityLabel = snapshot.qualityLabel,
        )
        Log.d(
            TAG,
            "bridge update taskId=${snapshot.taskId} status=${snapshot.status} percent=${snapshot.percent} " +
                "quality=${snapshot.qualityLabel} page=${snapshot.pageUrl}",
        )
    }

    override fun onTaskCompleted(result: BrowserDownloadResult) {
        BrowserDownloadSharedStore.update(
            taskId = result.taskId,
            title = result.title,
            pageUrl = "",
            percent = 100,
            status = com.avd.browserkit.download.BrowserDownloadStatus.COMPLETED,
            filePath = result.filePath,
        )
        Log.d(
            TAG,
            "bridge complete taskId=${result.taskId} success=${result.success} path=${result.filePath}",
        )
    }

    override fun onTaskFailed(taskId: String, message: String) {
        BrowserDownloadSharedStore.update(
            taskId = taskId,
            title = "",
            pageUrl = "",
            percent = 0,
            status = com.avd.browserkit.download.BrowserDownloadStatus.FAILED,
        )
        Log.e(TAG, "bridge failed taskId=$taskId message=$message")
    }
}
