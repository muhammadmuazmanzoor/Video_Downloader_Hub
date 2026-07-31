package com.video.avd.downloader

import android.content.Context
import android.util.Log
import com.avd.browserkit.api.BrowserDownloadBridge
import com.avd.browserkit.api.BrowserDownloadResult
import com.avd.browserkit.api.BrowserDownloadSnapshot
import com.avd.browserkit.api.BrowserHostDownloadRequest
object BrowserKitBridge : BrowserDownloadBridge {
    private const val TAG = "BrowserKitBridge"

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

    override fun onTaskUpdated(snapshot: BrowserDownloadSnapshot) {
        Log.d(
            TAG,
            "bridge update taskId=${snapshot.taskId} status=${snapshot.status} percent=${snapshot.percent} " +
                "quality=${snapshot.qualityLabel} page=${snapshot.pageUrl}",
        )
    }

    override fun onTaskCompleted(result: BrowserDownloadResult) {
        Log.d(
            TAG,
            "bridge complete taskId=${result.taskId} success=${result.success} path=${result.filePath}",
        )
    }

    override fun onTaskFailed(taskId: String, message: String) {
        Log.e(TAG, "bridge failed taskId=$taskId message=$message")
    }
}
