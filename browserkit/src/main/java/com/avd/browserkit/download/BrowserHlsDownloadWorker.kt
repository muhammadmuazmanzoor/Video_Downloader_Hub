package com.avd.browserkit.download

import android.content.Context
import androidx.work.WorkerParameters
import com.avd.browserkit.BrowserKitInitializer
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.ytdlp.YoutubeDlBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserHlsDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : BrowserDownloadWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (taskId.isBlank() || downloadUrl.isBlank()) return@withContext Result.failure()
        BrowserDownloadManager.init(applicationContext)
        BrowserKitLog.i(
            "Worker.HLS",
            "start taskId=$taskId title=$title headers=${headersMap().keys} url=${BrowserKitLog.shortUrl(downloadUrl)}",
        )
        if (!BrowserKitInitializer.isInitialized()) {
            BrowserKitLog.w("Worker.HLS", "initializing yt-dlp engine blocking")
            BrowserKitInitializer.initializeBlocking(applicationContext)
        }
        val engine = YoutubeDlBridge.engineOrNull()
        if (engine == null) {
            BrowserKitLog.e("Worker.HLS", "engine null")
            BrowserDownloadManager.markFailed(taskId, "yt-dlp module not ready")
            BrowserDownloadNotifier.showFailed(applicationContext, taskId, title)
            return@withContext Result.failure()
        }

        BrowserDownloadManager.updateProgress(taskId, 0, BrowserDownloadStatus.DOWNLOADING)
        BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, 0)
        setForeground(BrowserDownloadNotifier.foregroundInfo(applicationContext, taskId, title, 0))

        val output = BrowserFileStorage.outputFile(applicationContext, title, taskId, "mp4")
        BrowserKitLog.d("Worker.HLS", "output path=${output.absolutePath}")
        runCatching {
            engine.execute(
                url = downloadUrl,
                outputPath = output.absolutePath,
                headers = headersMap(),
            ) { progress ->
                val percent = progress.toInt().coerceIn(0, 100)
                BrowserDownloadManager.reportProgress(taskId, percent)
                BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, percent)
                if (percent == 0 || percent <= 5 || percent % 25 == 0 || percent >= 100) {
                    BrowserKitLog.d("Worker.HLS", "progress taskId=$taskId percent=$percent")
                }
            }
            BrowserKitLog.i("Worker.HLS", "done path=${output.absolutePath}")
            BrowserDownloadManager.markCompleted(taskId, output.absolutePath)
            BrowserDownloadNotifier.showComplete(
                applicationContext,
                taskId,
                title,
                output.absolutePath,
            )
            Result.success()
        }.getOrElse {
            if (isStopped) {
                BrowserDownloadNotifier.dismiss(applicationContext, taskId)
                return@withContext Result.failure()
            }
            BrowserKitLog.e("Worker.HLS", "fail", it)
            BrowserDownloadManager.markFailed(taskId, it.message ?: "hls failed")
            BrowserDownloadNotifier.showFailed(applicationContext, taskId, title)
            Result.failure()
        }
    }
}
