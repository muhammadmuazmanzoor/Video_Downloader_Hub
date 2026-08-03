package com.video.avd.downloader

import android.content.Context
import android.util.Log
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
    private val tag = "BrowserHostRegular"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL).orEmpty()
        val pageUrl = inputData.getString(KEY_PAGE_URL).orEmpty()
        if (taskId.isBlank() || downloadUrl.isBlank()) {
            Log.e(tag, "reject invalid input taskIdBlank=${taskId.isBlank()} urlBlank=${downloadUrl.isBlank()}")
            return@withContext Result.failure()
        }

        val headers = BrowserDownloadHeadersStore.load(applicationContext, taskId).toMutableMap()
        if (pageUrl.contains("instagram", true) || downloadUrl.contains("cdninstagram", true)) {
            headers.putIfAbsent("Referer", "https://www.instagram.com/")
        }
        val tmpDir = File(applicationContext.cacheDir, "browser_host_regular/$taskId").apply { mkdirs() }
        val tmpFile = File(tmpDir, "video.mp4")
        Log.i(
            tag,
            "start taskId=$taskId title=$title headers=${headers.keys} " +
                "page=${pageUrl.take(160)} url=${downloadUrl.take(160)}",
        )

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

                    override fun onFailure(e: Throwable) {
                        Log.e(tag, "download listener failed taskId=$taskId msg=${e.message}", e)
                    }

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
            Log.i(tag, "download finished taskId=$taskId exists=${tmpFile.exists()} bytes=${tmpFile.length()}")

            val validationError = validateProgressiveVideo(tmpFile, pageUrl.ifBlank { downloadUrl })
            if (validationError != null) {
                Log.e(tag, "validation failed taskId=$taskId reason=$validationError bytes=${tmpFile.length()}")
                error("Invalid video stub: $validationError size=${tmpFile.length()}")
            }

            val displayName = PublicDownloadHelper.displayFileNameForTask(title, taskId)
            val uri = PublicDownloadHelper.insertPendingVideo(applicationContext, displayName)
            Log.i(tag, "writing MediaStore taskId=$taskId uri=$uri displayName=$displayName")
            applicationContext.contentResolver.openOutputStream(uri, "w")?.use { out ->
                tmpFile.inputStream().use { it.copyTo(out) }
            } ?: run {
                PublicDownloadHelper.deleteEntry(applicationContext, uri)
                error("Cannot open MediaStore stream")
            }
            PublicDownloadHelper.markVideoComplete(applicationContext, uri)
            tmpDir.deleteRecursively()
            BrowserDownloadHeadersStore.clear(applicationContext, taskId)
            Log.i(tag, "success taskId=$taskId uri=$uri")
            BrowserKitBridge.onTaskCompleted(
                BrowserDownloadResult(taskId = taskId, title = title, filePath = uri.toString(), success = true),
            )
            Result.success()
        } catch (t: Throwable) {
            tmpDir.deleteRecursively()
            BrowserDownloadHeadersStore.clear(applicationContext, taskId)
            Log.e(tag, "failed taskId=$taskId title=$title msg=${t.message}", t)
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
            return validateProgressiveVideo(file, sourceUrl) == null
        }

        fun validateProgressiveVideo(file: File, sourceUrl: String): String? {
            if (!file.exists()) return "missing_file"
            if (file.length() < MIN_BYTES) return "too_small_${file.length()}"
            return runCatching {
                file.inputStream().use { input ->
                    val head = ByteArray(64)
                    val read = input.read(head)
                    if (read < 12) return@runCatching "header_too_short_$read"
                    val asText = head.decodeToString(0, read).lowercase()
                    if (
                        asText.contains("<html") ||
                        asText.contains("<!doctype") ||
                        asText.contains("{\"error") ||
                        asText.contains("\"error\"")
                    ) {
                        return@runCatching "html_or_json_error"
                    }
                    val hasFtyp = head.indexOfSequence("ftyp".toByteArray()) >= 0
                    val hasMoof = head.indexOfSequence("moof".toByteArray()) >= 0
                    val isWebm = read >= 4 &&
                        head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() &&
                        head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()
                    if (hasMoof && !hasFtyp) return@runCatching "fragmented_without_ftyp"
                    if (hasFtyp || isWebm || sourceUrl.contains(".mp4", true) || sourceUrl.contains(".webm", true)) {
                        return@runCatching null
                    }
                    "unknown_container"
                }
            }.getOrElse { "validate_exception_${it.javaClass.simpleName}" }
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
