package com.video.avd.downloader

import android.content.Context
import android.util.Log
import com.avd.browserkit.api.BrowserDownloadSharedStore
import com.avd.browserkit.api.BrowserSharedDownloadTask
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.avd.browserkit.api.BrowserDownloadSnapshot
import com.avd.browserkit.api.BrowserHostDownloadRequest
import com.avd.browserkit.download.BrowserDownloadStatus
import com.avd.browserkit.detection.StreamType
import com.avd.browserkit.util.DailymotionUrlUtils

object BrowserHostDownloader {
    private const val TAG = "BrowserHostDownloader"

    fun enqueue(context: Context, request: BrowserHostDownloadRequest): Boolean {
        return enqueueInternal(context.applicationContext, request)
    }

    fun restart(context: Context, task: BrowserSharedDownloadTask): Boolean {
        val request = BrowserHostDownloadRequest(
            title = task.title,
            pageUrl = task.pageUrl,
            downloadUrl = task.downloadUrl,
            qualityLabel = task.qualityLabel,
            streamType = task.streamType,
            headers = task.headers,
            useYtdlp = task.useYtdlp,
            facebookMode = task.facebookMode,
            useAvd = task.useAvd,
            formatId = task.formatId,
            audioUrl = task.audioUrl,
            isLive = task.isLive,
        )
        return enqueueInternal(context.applicationContext, request, existingTaskId = task.taskId)
    }

    private fun enqueueInternal(
        app: Context,
        request: BrowserHostDownloadRequest,
        existingTaskId: String? = null,
    ): Boolean {
        var url = request.downloadUrl.trim()
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) {
            Log.e(TAG, "reject blank/non-http url")
            return false
        }

        val dailymotion = DailymotionUrlUtils.isDailymotionVideoPage(request.pageUrl) ||
            DailymotionUrlUtils.isDailymotionMediaUrl(url)
        if (dailymotion && DailymotionUrlUtils.isDailymotionVideoPage(request.pageUrl)) {
            url = request.pageUrl.trim()
        }

        val taskId = existingTaskId ?: System.currentTimeMillis().toString()
        val title = request.title.ifBlank {
            if (request.facebookMode) "face_book" else "browser_video"
        }
        val useYtdlp = when {
            dailymotion -> true
            request.useAvd -> true
            request.useYtdlp -> true
            request.streamType.contains("HLS", true) -> true
            request.streamType.contains("MPD", true) -> true
            else -> false
        }

        val headers = if (dailymotion) {
            request.headers + mapOf(
                "Referer" to DailymotionUrlUtils.REFERER,
                "Origin" to "https://www.dailymotion.com",
            )
        } else {
            request.headers
        }
        BrowserDownloadHeadersStore.save(app, taskId, headers)
        BrowserDownloadSharedStore.upsert(
            BrowserSharedDownloadTask(
                taskId = taskId,
                title = title,
                pageUrl = request.pageUrl,
                downloadUrl = url,
                qualityLabel = request.qualityLabel,
                streamType = if (dailymotion) StreamType.HLS_M3U8.name else request.streamType,
                headers = headers,
                useYtdlp = useYtdlp,
                facebookMode = request.facebookMode,
                useAvd = request.useAvd,
                formatId = request.formatId,
                audioUrl = request.audioUrl,
                isLive = request.isLive,
                percent = 0,
                status = BrowserDownloadStatus.QUEUED,
            ),
        )
        BrowserKitBridge.onTaskUpdated(
            BrowserDownloadSnapshot(taskId, title, request.pageUrl, 0, BrowserDownloadStatus.QUEUED, qualityLabel = request.qualityLabel),
        )

        val requestBuilder = if (useYtdlp) {
            OneTimeWorkRequestBuilder<BrowserHostYtDlpWorker>()
                .setInputData(
                    workDataOf(
                        BrowserHostYtDlpWorker.KEY_TASK_ID to taskId,
                        BrowserHostYtDlpWorker.KEY_TITLE to title,
                        BrowserHostYtDlpWorker.KEY_DOWNLOAD_URL to url,
                        BrowserHostYtDlpWorker.KEY_PAGE_URL to request.pageUrl,
                        BrowserHostYtDlpWorker.KEY_FACEBOOK_MODE to request.facebookMode,
                    ),
                )
        } else {
            OneTimeWorkRequestBuilder<BrowserHostRegularWorker>()
                .setInputData(
                    workDataOf(
                        BrowserHostRegularWorker.KEY_TASK_ID to taskId,
                        BrowserHostRegularWorker.KEY_TITLE to title,
                        BrowserHostRegularWorker.KEY_DOWNLOAD_URL to url,
                        BrowserHostRegularWorker.KEY_PAGE_URL to request.pageUrl,
                    ),
                )
        }

        val workRequest = requestBuilder
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(app).enqueueUniqueWork(
            "browser_host_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )
        Log.i(
            TAG,
            "enqueued taskId=$taskId ytdlp=$useYtdlp fb=${request.facebookMode} avd=${request.useAvd} " +
                "stream=${request.streamType} headers=${headers.keys} url=${url.take(160)}",
        )
        return true
    }
}
