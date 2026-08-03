package com.video.avd.downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avd.browserkit.api.BrowserDownloadResult
import com.avd.browserkit.api.BrowserDownloadSnapshot
import com.avd.browserkit.download.BrowserDownloadStatus
import com.avd.util.downloaders.custom_downloader_service.CustomFileDownloader
import com.avd.util.downloaders.custom_downloader_service.DownloadListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.net.URL

class BrowserHostRegularWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL).orEmpty()
        val pageUrl = inputData.getString(KEY_PAGE_URL).orEmpty()
        if (taskId.isBlank() || downloadUrl.isBlank()) return@withContext Result.failure()

        val headers = BrowserDownloadHeadersStore.load(applicationContext, taskId).toMutableMap()
        if (pageUrl.contains("instagram", true) || downloadUrl.contains("cdninstagram", true)) {
            headers.putIfAbsent("Referer", "https://www.instagram.com/")
        }
        val tmpDir = File(applicationContext.cacheDir, "browser_host_regular/$taskId").apply { mkdirs() }
        val tmpFile = File(tmpDir, "video.mp4")

        BrowserKitBridge.onTaskUpdated(
            BrowserDownloadSnapshot(taskId, title, pageUrl, 0, BrowserDownloadStatus.QUEUED),
        )
        return@withContext try {
            var lastPercent = -1
            val client = OkHttpClient()
            CustomFileDownloader(
                url = URL(downloadUrl),
                file = tmpFile,
                threadCount = 1,
                headers = headers,
                client = client,
                listener = object : DownloadListener {
                    override fun onSuccess() = Unit

                    override fun onFailure(e: Throwable) = Unit

                    override fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long) {
                        val percent =
                            if (totalBytes > 0L) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                        if (percent != lastPercent || totalBytes <= 0L) {
                            lastPercent = percent
                            BrowserKitBridge.onTaskUpdated(
                                BrowserDownloadSnapshot(
                                    taskId,
                                    title,
                                    pageUrl,
                                    percent,
                                    BrowserDownloadStatus.DOWNLOADING,
                                ),
                            )
                        }
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

            if (!isValidProgressiveMp4(tmpFile, pageUrl.ifBlank { downloadUrl })) {
                error("Invalid video stub size=${tmpFile.length()}")
            }

            val displayName = PublicDownloadHelper.displayFileNameForTask(title, taskId)
            val uri = PublicDownloadHelper.insertPendingVideo(applicationContext, displayName)
            applicationContext.contentResolver.openOutputStream(uri, "w")?.use { out ->
                tmpFile.inputStream().use { it.copyTo(out) }
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
            BrowserKitBridge.onTaskFailed(taskId, t.message ?: "regular download failed")
            Result.failure()
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PAGE_URL = "page_url"
        private const val MIN_BYTES = 1_024L

        fun isValidProgressiveMp4(file: File, sourceUrl: String): Boolean {
            if (!file.exists() || file.length() < MIN_BYTES) return false
            return runCatching {
                file.inputStream().use { input ->
                    val head = ByteArray(64)
                    val read = input.read(head)
                    if (read < 12) return@runCatching false
                    val asText = head.decodeToString(0, read).lowercase()
                    if (
                        asText.contains("<html") ||
                        asText.contains("<!doctype") ||
                        asText.contains("{\"error") ||
                        asText.contains("\"error\"")
                    ) {
                        return@runCatching false
                    }
                    val hasFtyp = head.indexOfSequence("ftyp".toByteArray()) >= 0
                    val hasMoof = head.indexOfSequence("moof".toByteArray()) >= 0
                    val isWebm = read >= 4 &&
                        head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() &&
                        head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()
                    if (hasMoof && !hasFtyp) return@runCatching false
                    hasFtyp || isWebm || sourceUrl.contains(".mp4", true) || sourceUrl.contains(".webm", true)
                }
            }.getOrDefault(false)
        }

        private fun ByteArray.indexOfSequence(seq: ByteArray): Int {
            outer@ for (i in 0..size - seq.size) {
                for (j in seq.indices) {
                    if (this[i + j] != seq[j]) continue@outer
                }
                return i
            }
            return -1
        }
    }
}
