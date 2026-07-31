package com.avd.browserkit.download

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.avd.browserkit.util.BrowserKitLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class BrowserProgressiveDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : BrowserDownloadWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (taskId.isBlank() || downloadUrl.isBlank()) {
            BrowserKitLog.e("Worker.Prog", "abort blank taskId/url")
            return@withContext Result.failure()
        }
        BrowserDownloadManager.init(applicationContext)
        val headers = headersMap()
        BrowserKitLog.i(
            "Worker.Prog",
            "start taskId=$taskId title=$title headers=${headers.keys} " +
                "url=${BrowserKitLog.shortUrl(downloadUrl)}",
        )
        BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, 0)
        BrowserDownloadManager.updateProgress(taskId, 0, BrowserDownloadStatus.DOWNLOADING)
        setForeground(createForeground(0))

        val output = BrowserFileStorage.outputFile(applicationContext, title, taskId, "mp4")
        BrowserKitLog.d("Worker.Prog", "output path=${output.absolutePath}")
        val client = OkHttpClient()
        val requestBuilder = Request.Builder().url(downloadUrl).get()
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

        runCatching {
            val request = requestBuilder.build()
            BrowserKitLog.d("Worker.Prog", "executing request headers=${request.headers.names()}")
            client.newCall(request).execute().use { response ->
                BrowserKitLog.i(
                    "Worker.Prog",
                    "HTTP ${response.code} len=${response.body?.contentLength()} " +
                        "type=${response.header("Content-Type")}",
                )
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body ?: error("Empty body")
                val total = body.contentLength().coerceAtLeast(-1L)
                BrowserKitLog.d("Worker.Prog", "stream open total=$total taskId=$taskId")
                body.byteStream().use { input ->
                    output.outputStream().use { out ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var lastPercent = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            val percent = if (total > 0) {
                                ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            } else {
                                ((downloaded / (1024 * 1024)) % 100).toInt()
                            }
                            if (percent != lastPercent) {
                                lastPercent = percent
                                BrowserDownloadManager.updateProgress(taskId, percent, BrowserDownloadStatus.DOWNLOADING)
                                BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, percent)
                                setForeground(createForeground(percent))
                                if (percent == 0 || percent <= 5 || percent % 25 == 0 || percent >= 100) {
                                    BrowserKitLog.d(
                                        "Worker.Prog",
                                        "progress $percent% bytes=$downloaded total=$total taskId=$taskId",
                                    )
                                }
                            }
                        }
                    }
                }
            }
            BrowserKitLog.i(
                "Worker.Prog",
                "done size=${output.length()} path=${output.absolutePath}",
            )
            BrowserDownloadManager.markCompleted(taskId, output.absolutePath)
            BrowserDownloadNotifier.showComplete(
                applicationContext,
                taskId,
                title,
                output.absolutePath,
            )
            Result.success()
        }.getOrElse { error ->
            if (isStopped) {
                BrowserKitLog.w("Worker.Prog", "stopped/cancelled taskId=$taskId")
                BrowserDownloadNotifier.dismiss(applicationContext, taskId)
                return@withContext Result.failure()
            }
            BrowserKitLog.e("Worker.Prog", "fail taskId=$taskId", error)
            BrowserDownloadManager.markFailed(taskId, error.message ?: "failed")
            BrowserDownloadNotifier.showFailed(applicationContext, taskId, title)
            Result.failure()
        }
    }

    private fun createForeground(percent: Int): ForegroundInfo {
        return BrowserDownloadNotifier.foregroundInfo(applicationContext, taskId, title, percent)
    }
}
