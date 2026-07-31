package com.avd.browserkit.download

import android.content.Context
import androidx.work.WorkerParameters
import com.avd.browserkit.BrowserKitInitializer
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.ytdlp.YoutubeDlBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Xilli-style Facebook download: prefer direct HTTP on data-video-url CDN,
 * then yt-dlp facebookMode. Reject empty / HTML stubs.
 */
class BrowserFacebookDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : BrowserDownloadWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (taskId.isBlank() || downloadUrl.isBlank()) {
            BrowserKitLog.e("Worker.FB", "abort blank taskId/url")
            return@withContext Result.failure()
        }
        BrowserDownloadManager.init(applicationContext)
        if (!BrowserKitInitializer.isInitialized()) {
            BrowserKitLog.w("Worker.FB", "init blocking…")
            BrowserKitInitializer.initializeBlocking(applicationContext)
        }

        val mediaUrl = normalizeUrl(downloadUrl)
        BrowserKitLog.i(
            "Worker.FB",
            "start taskId=$taskId url=${BrowserKitLog.shortUrl(mediaUrl)}",
        )
        setForeground(BrowserDownloadNotifier.foregroundInfo(applicationContext, taskId, title, 0))
        BrowserDownloadManager.updateProgress(taskId, 0, BrowserDownloadStatus.DOWNLOADING)
        BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, 0)

        val outDir = BrowserFileStorage.outputFile(applicationContext, title, taskId, "mp4").parentFile
            ?: applicationContext.getExternalFilesDir("BrowserDownloads")
            ?: applicationContext.filesDir
        if (!outDir.exists()) outDir.mkdirs()
        BrowserKitLog.d("Worker.FB", "output dir=${outDir.absolutePath}")
        val baseName = "face_book_$taskId"
        val headers = headersMap().toMutableMap().apply {
            putIfAbsent("Referer", "https://www.facebook.com/")
            putIfAbsent("Origin", "https://www.facebook.com")
            putIfAbsent(
                "Accept",
                "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5",
            )
        }

        // 1) HTTP first — FB data-video-url is usually a signed CDN progressive file.
        val progressiveOut = File(outDir, "$baseName.mp4")
        progressiveOut.delete()
        val httpOk = runCatching {
            BrowserKitLog.d("Worker.FB", "HTTP primary start headers=${headers.keys}")
            downloadViaHttp(mediaUrl, progressiveOut, headers) { percent ->
                BrowserDownloadManager.reportProgress(taskId, percent)
                BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, percent)
            }
            isValidVideoFile(progressiveOut)
        }.onFailure {
            BrowserKitLog.e("Worker.FB", "HTTP failed", it)
            progressiveOut.delete()
        }.getOrDefault(false)

        if (httpOk) {
            BrowserKitLog.i("Worker.FB", "HTTP OK size=${progressiveOut.length()}")
            return@withContext finishOk(progressiveOut)
        }
        progressiveOut.delete()

        if (isStopped) {
            BrowserDownloadNotifier.dismiss(applicationContext, taskId)
            return@withContext Result.failure()
        }

        // 2) yt-dlp facebookMode fallback
        val engine = YoutubeDlBridge.engineOrNull()
        if (engine == null) {
            BrowserKitLog.e("Worker.FB", "engine null + HTTP failed")
            return@withContext finishFail("facebook download failed")
        }

        val ytdlpTemplate = File(outDir, "$baseName.%(ext)s").absolutePath
        BrowserKitLog.d("Worker.FB", "yt-dlp facebookMode fallback template=$ytdlpTemplate")
        val ytdlpOk = runCatching {
            engine.execute(
                url = mediaUrl,
                outputPath = ytdlpTemplate,
                headers = headers,
                facebookMode = true,
            ) { progress ->
                val percent = progress.toInt().coerceIn(0, 100)
                BrowserDownloadManager.reportProgress(taskId, percent)
                BrowserDownloadNotifier.showProgress(applicationContext, taskId, title, percent)
            }
            true
        }.onFailure {
            BrowserKitLog.e("Worker.FB", "yt-dlp failed", it)
        }.getOrDefault(false)

        if (ytdlpOk) {
            val saved = findDownloadedFile(outDir, baseName)
            if (saved != null && isValidVideoFile(saved)) {
                BrowserKitLog.i("Worker.FB", "yt-dlp OK path=${saved.absolutePath} size=${saved.length()}")
                return@withContext finishOk(saved)
            }
            BrowserKitLog.w(
                "Worker.FB",
                "yt-dlp finished but file invalid size=${saved?.length() ?: -1}",
            )
            saved?.delete()
        }

        finishFail("facebook download empty or failed")
    }

    private suspend fun finishOk(file: File): Result {
        BrowserDownloadManager.markCompleted(taskId, file.absolutePath)
        BrowserDownloadNotifier.showComplete(applicationContext, taskId, title, file.absolutePath)
        return Result.success()
    }

    private suspend fun finishFail(message: String): Result {
        BrowserKitLog.e("Worker.FB", message)
        BrowserDownloadManager.markFailed(taskId, message)
        BrowserDownloadNotifier.showFailed(applicationContext, taskId, title)
        return Result.failure()
    }

    private fun findDownloadedFile(dir: File, baseName: String): File? {
        val exact = listOf("mp4", "webm", "mkv", "m4v").map { File(dir, "$baseName.$it") }
            .firstOrNull { it.exists() && it.length() >= MIN_VALID_BYTES }
        if (exact != null) return exact
        return dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(baseName) && it.length() >= MIN_VALID_BYTES }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun downloadViaHttp(
        url: String,
        output: File,
        headers: Map<String, String>,
        onProgress: (Int) -> Unit,
    ) {
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) ->
            if (v.isNotBlank()) builder.header(k, v)
        }
        client.newCall(builder.build()).execute().use { response ->
            val contentType = response.header("Content-Type").orEmpty().lowercase()
            val contentLen = response.body?.contentLength() ?: -1L
            BrowserKitLog.i(
                "Worker.FB",
                "HTTP ${response.code} type=$contentType len=$contentLen",
            )
            if (!response.isSuccessful) error("HTTP ${response.code}")
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                error("Not a video content-type: $contentType")
            }
            val body = response.body ?: error("Empty body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                output.outputStream().use { out ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    var last = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (total > 0) {
                            ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                        } else {
                            ((downloaded / (1024 * 1024)) % 99).toInt()
                        }
                        if (percent != last) {
                            last = percent
                            onProgress(percent)
                            if (percent == 0 || percent <= 5 || percent % 25 == 0 || percent >= 100) {
                                BrowserKitLog.d(
                                    "Worker.FB",
                                    "HTTP progress $percent% bytes=$downloaded total=$total",
                                )
                            }
                        }
                    }
                    out.flush()
                    BrowserKitLog.i("Worker.FB", "HTTP wrote bytes=$downloaded")
                    if (downloaded < MIN_VALID_BYTES) {
                        error("Downloaded too small: $downloaded bytes")
                    }
                }
            }
        }
    }

    companion object {
        /** Reject empty / login-page / CMAF init stubs. Real FB clips are larger. */
        private const val MIN_VALID_BYTES = 1_000_000L

        private fun normalizeUrl(raw: String): String {
            var u = raw.trim().replace("&amp;", "&")
            if (u.startsWith("//")) u = "https:$u"
            return u
        }

        private fun isValidVideoFile(file: File?): Boolean {
            if (file == null || !file.exists()) return false
            val size = file.length()
            if (size < MIN_VALID_BYTES) {
                BrowserKitLog.w("Worker.FB", "invalid size=$size path=${file.name}")
                return false
            }
            // Reject HTML/error payloads saved as .mp4
            return runCatching {
                file.inputStream().use { input ->
                    val head = ByteArray(32)
                    val read = input.read(head)
                    if (read < 12) return@runCatching false
                    val asText = head.decodeToString(0, read).lowercase()
                    if (asText.contains("<html") || asText.contains("<!doctype")) {
                        BrowserKitLog.w("Worker.FB", "file looks like HTML")
                        return@runCatching false
                    }
                    // MP4/ISO BMFF has "ftyp" near start; webm starts with 0x1A45DFA3
                    val hasFtyp = head.indexOfSequence("ftyp".toByteArray()) >= 0
                    val hasMoof = head.indexOfSequence("moof".toByteArray()) >= 0
                    val isWebm = head.size >= 4 &&
                        head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() &&
                        head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()
                    if (hasMoof && !hasFtyp) {
                        BrowserKitLog.w("Worker.FB", "DASH moof fragment size=$size")
                        return@runCatching false
                    }
                    if (!hasFtyp && !isWebm) {
                        BrowserKitLog.w("Worker.FB", "no ftyp/webm magic — reject size=$size")
                        return@runCatching false
                    }
                    true
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
