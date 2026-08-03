package com.video.avd.downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avd.browserkit.BrowserKitInitializer
import com.avd.browserkit.api.BrowserDownloadResult
import com.avd.browserkit.api.BrowserDownloadSnapshot
import com.avd.browserkit.download.BrowserDownloadStatus
import com.avd.browserkit.ytdlp.YoutubeDlBridge
import com.avd.util.downloaders.custom_downloader_service.CustomFileDownloader
import com.avd.util.downloaders.custom_downloader_service.DownloadListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.net.URL

class BrowserHostYtDlpWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL).orEmpty()
        val pageUrl = inputData.getString(KEY_PAGE_URL).orEmpty()
        val facebookMode = inputData.getBoolean(KEY_FACEBOOK_MODE, false)
        if (taskId.isBlank() || downloadUrl.isBlank()) return@withContext Result.failure()

        BrowserKitBridge.onTaskUpdated(
            BrowserDownloadSnapshot(taskId, title, pageUrl, 0, BrowserDownloadStatus.QUEUED),
        )
        val headers = BrowserDownloadHeadersStore.load(applicationContext, taskId).toMutableMap()
        when {
            facebookMode -> {
                headers.putIfAbsent("Referer", "https://www.facebook.com/")
                headers.putIfAbsent("Origin", "https://www.facebook.com")
            }
            pageUrl.startsWith("http") -> headers.putIfAbsent("Referer", pageUrl)
        }
        headers.putIfAbsent(
            "Accept",
            "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5",
        )

        val tmpDir = File(applicationContext.cacheDir, "browser_host_ytdlp/$taskId").apply { mkdirs() }
        val baseName = if (facebookMode) "face_book_$taskId" else "browser_$taskId"
        val template = File(tmpDir, "$baseName.%(ext)s").absolutePath
        val httpFile = File(tmpDir, "$baseName.mp4")

        return@withContext try {
            if (!BrowserKitInitializer.isInitialized()) {
                BrowserKitInitializer.initializeBlocking(applicationContext)
            }
            val engine = YoutubeDlBridge.engineOrNull() ?: error("yt-dlp engine not ready")
            var lastPercent = -1
            engine.execute(
                url = downloadUrl,
                outputPath = template,
                headers = headers,
                facebookMode = facebookMode,
            ) { progress ->
                val percent = progress.toInt().coerceIn(0, 100)
                if (percent != lastPercent) {
                    lastPercent = percent
                    BrowserKitBridge.onTaskUpdated(
                        BrowserDownloadSnapshot(taskId, title, pageUrl, percent, BrowserDownloadStatus.DOWNLOADING),
                    )
                }
            }
            var saved = findOutput(tmpDir, baseName)
            var valid = saved != null && BrowserHostRegularWorker.isValidProgressiveMp4(saved, pageUrl.ifBlank { downloadUrl })
            if (!valid && !isHtmlWatchPage(downloadUrl)) {
                saved?.delete()
                httpFile.delete()
                val client = OkHttpClient()
                CustomFileDownloader(
                    url = URL(downloadUrl),
                    file = httpFile,
                    threadCount = 1,
                    headers = headers,
                    client = client,
                    listener = object : DownloadListener {
                        override fun onSuccess() = Unit

                        override fun onFailure(e: Throwable) = Unit

                        override fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long) {
                            val percent =
                                if (totalBytes > 0L) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                            BrowserKitBridge.onTaskUpdated(
                                BrowserDownloadSnapshot(taskId, title, pageUrl, percent, BrowserDownloadStatus.DOWNLOADING),
                            )
                        }

                        override fun onChunkProgressUpdate(
                            downloadedBytes: Long,
                            allBytesChunk: Long,
                            chunkIndex: Int,
                        ) = Unit

                        override fun onChunkFailure(
                            e: Throwable,
                            index: CustomFileDownloader.Chunk,
                        ) = Unit
                    },
                ).download()
                if (BrowserHostRegularWorker.isValidProgressiveMp4(httpFile, pageUrl.ifBlank { downloadUrl })) {
                    saved = httpFile
                    valid = true
                }
            }
            if (!valid || saved == null) error("unplayable output")

            val displayName = PublicDownloadHelper.displayFileNameForTask(title, taskId)
            val uri = PublicDownloadHelper.insertPendingVideo(applicationContext, displayName)
            applicationContext.contentResolver.openOutputStream(uri, "w")?.use { out ->
                saved.inputStream().use { it.copyTo(out) }
            } ?: run {
                PublicDownloadHelper.deleteEntry(applicationContext, uri)
                error("Cannot open MediaStore stream")
            }
            PublicDownloadHelper.markVideoComplete(applicationContext, uri)
            tmpDir.deleteRecursively()
            BrowserDownloadHeadersStore.clear(applicationContext, taskId)
            BrowserKitBridge.onTaskCompleted(
                BrowserDownloadResult(taskId = taskId, title = title, filePath = uri.toString(), success = true),
            )
            Result.success()
        } catch (t: Throwable) {
            tmpDir.deleteRecursively()
            BrowserDownloadHeadersStore.clear(applicationContext, taskId)
            BrowserKitBridge.onTaskFailed(taskId, t.message ?: "yt-dlp download failed")
            Result.failure()
        }
    }

    private fun findOutput(dir: File, baseName: String): File? {
        val exact = listOf("mp4", "webm", "mkv", "m4v")
            .map { File(dir, "$baseName.$it") }
            .firstOrNull { it.exists() && it.length() > 0L }
        if (exact != null) return exact
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(baseName) && it.length() > 0L }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun isHtmlWatchPage(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("facebook.com") ||
            lower.contains("instagram.com") ||
            lower.contains("watch?v=") ||
            lower.contains("/video/") ||
            lower.contains("/reel/")
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PAGE_URL = "page_url"
        const val KEY_FACEBOOK_MODE = "facebook_mode"
    }
}
