@file:Suppress("PackageDirectoryMismatch")

package com.avd.util.downloaders.youtubedl_downloader

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.avd.data.local.model.Proxy
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.util.CookieUtils
import com.avd.ui.dialog.DownloadCompletionBroadcast
import com.avd.util.downloaders.generic_downloader.GenericDownloader
import com.avd.util.downloaders.generic_downloader.models.VideoTaskItem
import com.avd.util.downloaders.generic_downloader.models.VideoTaskState
import com.avd.util.downloaders.generic_downloader.workers.GenericDownloadWorkerWrapper
import com.google.gson.Gson
import com.avd.util.AppLogger
import com.avd.util.CompletionCallback
import com.avd.util.FileUtil
import com.avd.util.ProgressCallback
import com.avd.util.SharedPrefHelper
import com.avd.util.YoutubeDlUtils
import com.avd.util.YoutubeDlUtils.youtubeDl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class YoutubeDlDownloaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    override var fileUtil: FileUtil
) : GenericDownloadWorkerWrapper(appContext, workerParams, fileUtil) {
    companion object {
        var isCanceled = false
        const val IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY = "IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY"
        const val STOP_SAVE_ACTION = "STOP_AND_SAVE"
        const val DOWNLOAD_FILENAME_KEY = "download_filename"
        const val IS_FINISHED_DOWNLOAD_ACTION_KEY = "action"
        private const val TRESHOLD = 10 * 1024 * 1024
    }

    private lateinit var tmpFile: File
    private var isLiveCounter: Int = 0
    private var isDownloadOk: Boolean = false
    private var isDownloadJustStarted: Boolean = false
    private var monitorProcess: Disposable? = null
    private var progressCached = 0
    private var disposable: Disposable? = null
    private var progressDisposable: Disposable? = null
    private var cookieFile: File? = null
    private var lastTmpDirSize = 0L

    @Volatile
    var time = 0L
    private var isDownloadStarted = false

    override fun afterDone() {
        monitorProcess?.dispose()
        disposable?.dispose()
        progressDisposable?.dispose()
    }

    override fun handleAction(
        action: String, task: VideoTaskItem, headers: Map<String, String>, isFileRemove: Boolean
    ) {
        when (action) {
            GenericDownloader.DownloaderActions.DOWNLOAD -> {
                isCanceled = false
                try {
                    // Run startDownload in a coroutine scope to handle cancellation
                    CoroutineScope(Dispatchers.IO).launch {
                        startDownload(task, headers)
                    }
                } catch (e: CancellationException) {
                    Log.d("YoutubeDlDownloaderWorker", "Download action was cancelled")
                    // Clean up resources
                    monitorProcess?.dispose()
                    disposable?.dispose()
                    progressDisposable?.dispose()
                }
            }

            GenericDownloader.DownloaderActions.CANCEL -> {
                isCanceled = true
                cancelDownload(task, headers)
            }

            GenericDownloader.DownloaderActions.PAUSE -> {
                isCanceled = false
                pauseDownload(task, headers)
            }

            GenericDownloader.DownloaderActions.RESUME -> {
                isCanceled = false
                resumeDownload(task, headers)
            }

            STOP_SAVE_ACTION -> {
                stopAndSave(task)
            }
        }
    }

    private fun stopAndSave(task: VideoTaskItem) {
        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)

        if (taskId != null) {
            try {
                // Using the youtubeDl instance to call destroyProcessById
                val destroyMethod =
                    youtubeDl::class.java.getMethod("destroyProcessById", String::class.java)
                destroyMethod.invoke(youtubeDl, taskId)
            } catch (e: Exception) {
                Log.d("stopAndSave", "Error invoking destroyProcessById: " + e.message)
            }

            val partsFolder = File(
                "${fileUtil.tmpDir}/$taskId"
            )
            val firstPart = partsFolder.listFiles()?.firstOrNull()

            val dist = File(fileUtil.folderDir.absolutePath, "${task.title}.mp4")

            if (firstPart != null && firstPart.exists()) {
                try {
                    val moved =
                        fileUtil.moveMedia(applicationContext, firstPart.toUri(), dist.toUri())
                    if (moved) {
                        finishWork(task.also { it.taskState = VideoTaskState.SUCCESS })
                    } else {
                        finishWork(task.also { it.taskState = VideoTaskState.ERROR })
                    }
                } catch (e: Throwable) {
                    finishWork(task.also { it.taskState = VideoTaskState.ERROR })
                }
            } else {
                finishWork(task.also { it.taskState = VideoTaskState.ERROR })
            }
        }
    }



    @SuppressLint("CheckResult")
    private suspend fun startDownload(
        task: VideoTaskItem, headers: Map<String, String>, isContinue: Boolean = false
    ) {
        try {
        if (isStopped) {
            Log.d("YoutubeDlDownloaderWorker", "Work stopped before download started")
            return
        }

        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY) ?: return
        val originUrl = inputData.getString(GenericDownloader.ORIGIN_KEY) ?: throw Throwable("URL is NULL")
        val requestUrl = inputData.getString(GenericDownloader.URL_KEY) ?: originUrl

        // Load and decode headers
        val rawHeaders = GenericDownloader.loadHeadersStringFromSharedPreferences(applicationContext, taskId)
        if (rawHeaders.isNullOrBlank()) {
            Log.e("YoutubeDlDownloaderWorker", "Missing saved format headers taskId=$taskId requestUrl=$requestUrl")
            finishWork(task.also {
                it.taskState = VideoTaskState.ERROR
                it.errorMessage = "Missing saved format headers"
            })
            return
        }
        val decompressedRaw = try {
            GenericDownloader.decompressString(rawHeaders)
        } catch (e: Exception) {
            Log.e("YoutubeDlDownloaderWorker", "Header decompress failed taskId=$taskId", e)
            finishWork(task.also {
                it.taskState = VideoTaskState.ERROR
                it.errorMessage = "Header decompress failed"
            })
            return
        }
        val decodedBytes = try {
            Base64.decode(decompressedRaw, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("YoutubeDlDownloaderWorker", "Header base64 decode failed taskId=$taskId", e)
            finishWork(task.also {
                it.taskState = VideoTaskState.ERROR
                it.errorMessage = "Header decode failed"
            })
            return
        }
        val decodedHeadersString = String(decodedBytes, Charsets.UTF_8)
        val vFormat = Gson().fromJson(decodedHeadersString, VideoFormatEntity::class.java)
        if (vFormat == null) {
            Log.e("YoutubeDlDownloaderWorker", "Parsed format is null taskId=$taskId")
            finishWork(task.also {
                it.taskState = VideoTaskState.ERROR
                it.errorMessage = "Format parse failed"
            })
            return
        }

        AppLogger.d("Start download dl: formatId=${vFormat.formatId} requestUrl=$requestUrl originUrl=$originUrl task=$task")
        Log.d(
            "YoutubeDlDownloaderWorker",
            "Resolved worker URLs taskId=$taskId requestUrl=$requestUrl originUrl=$originUrl formatUrl=${vFormat.url}",
        )

        // Setup download directory and notifications
        val name = task.title
        val downloadDir = fileUtil.folderDir
        Log.d("TmpFile", "Download directory: ${downloadDir.absolutePath}")
        notificationsHelper.hideNotification(taskId.hashCode())
        notificationsHelper.hideNotification(taskId.hashCode() + 1)

        // Get the mapped YoutubeDLRequest
        val request = YoutubeDlUtils.getMappedYoutubeDLRequestDownload(requestUrl) ?: run {
            Log.e(
                "YoutubeDlDownloaderWorker",
                "Failed to map YoutubeDL request taskId=$taskId requestUrl=$requestUrl originUrl=$originUrl",
            )
            finishWork(task.also {
                it.taskState = VideoTaskState.ERROR
                it.errorMessage = "YoutubeDL request mapping failed"
            })
            return
        }

        cookieFile = CookieUtils.addCookiesToRequestdownload(
            requestUrl, request, originUrl
        )

        tmpFile = File("${fileUtil.tmpDir}/$taskId").apply {
            if (!exists()) mkdir()
        }
        Log.d("TmpFile", "Temporary file directory: ${tmpFile.absolutePath}")
        // Monitor download process
        monitorProcess = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .map { calculateFolderSize(tmpFile) }
            .onErrorReturn { -1 }
            .subscribe { folderSize -> handleMonitoring(folderSize, taskId, task, tmpFile) }

        // Set up request options
        setupRequestOptions(request, vFormat, isContinue)

        // Show fetching info progress
        showProgress(taskId, taskId, name, 0, "Fetching info, Please wait...", tmpFile)
        saveProgress(taskId,
            LineInfo(taskId, 0.0, 0.0, sourceLine = "Fetching info, Please wait..."),
            task.also { it.taskState = VideoTaskState.PREPARE }).blockingFirst(Unit)
        val addOptionMethod = request::class.java.getDeclaredMethod(
            "addOption", String::class.java, String::class.java
        )
        addOptionMethod.invoke(request,"-o", "${tmpFile.absolutePath}/${name}.%(ext)s")

        Log.d("TmpFile", "Download command output set to: ${tmpFile.absolutePath}/${name}.%(ext)s")
        val list = tmpFile.listFiles()
        // Define progress and completion callbacks with correct types
        val progressCallback = ProgressCallback { pr, _, line ->
            Log.d("TmpFile", "Size: $pr, Line: $line")
            Log.d("TmpFile", "Progress: $pr, Line: $line")

            if (line.contains("[download] Destination:")) {
                isDownloadJustStarted = true
                isDownloadStarted = true
                Log.d("TmpFile", "Download started for: $name")
            }

//            if (line.contains("[download]   0")) {
//                isDownloadOk = true
//            }
            // Check network only after download has started
            if (isDownloadStarted && !isNetworkAvailable()) {
                monitorProcess?.dispose()
                handleNetworkError(task)
                return@ProgressCallback
            }
            if (line.contains(Regex("""\[download] {3}\d+"""))) {
                isDownloadOk = true
            }
            val lineInfo: LineInfo? = try {
                parseInfoFromLine(line)
            } catch (e: Throwable) {
                null
            }
            progressCached = pr.toInt()
            if (Date().time - time > 1000 && !getDone()) {
                time = Date().time
                val totalBytes = (lineInfo?.total ?: 0).toLong()
                val downloadBytes = (totalBytes * (pr / 100)).toLong()
                val downloadBytesFixed = if (downloadBytes > 0) {
                    downloadBytes
                } else {
                    0
                }
                task.percent = pr
                task.totalSize = totalBytes
                task.downloadSize = downloadBytesFixed
                task.taskState = VideoTaskState.DOWNLOADING
                Log.d("TmpFile", "Progress: $pr%, Total: $totalBytes bytes, Downloaded: $downloadBytesFixed bytes")
                if (progressDisposable != null) {
                    progressDisposable?.dispose()
                    progressDisposable = null
                }
                saveProgress(taskId, lineInfo, task).blockingFirst(Unit)
                showProgress(taskId, taskId, name, pr.toInt(), line ?: "", tmpFile)
                val freeSpace = FileUtil.getFreeDiskSpace(this@YoutubeDlDownloaderWorker.applicationContext,fileUtil.folderDir)
                Log.d("TmpFile", "Free disk space: $freeSpace bytes")
                if (freeSpace < TRESHOLD) {
                    Log.e("TmpFile", "Not enough space to continue download.")
                    finishWork(task.also {
                        it.mId = taskId
                        it.taskState = VideoTaskState.ERROR
                        it.errorMessage = "Not enough space"
                    })
                }
            }

        }

        val completionCallback: CompletionCallback = CompletionCallback { exitCode, line ->
            if (exitCode == 0) {
                Log.d("TmpFile", "Download completed successfully.")
                // Move the file from tmp location to final location
                tmpFile.listFiles()?.firstOrNull {
                    val moved = fileUtil.moveMedia(
                        this@YoutubeDlDownloaderWorker.applicationContext,
                        Uri.fromFile(it),
                        Uri.fromFile(File(fixFileName("${downloadDir.absolutePath}/${it.name}")))
                    )

                    if (this@YoutubeDlDownloaderWorker.cookieFile != null) {
                        this@YoutubeDlDownloaderWorker.cookieFile!!.delete()
                    }

                    if (moved) {
                        tmpFile.delete()
                    }
                    finishWork(VideoTaskItem(requestUrl).also { f ->
                        f.fileName = it.name
                        f.errorCode = if (moved) 0 else 1
                        f.percent = 100F
                        f.taskState =
                            if (moved) VideoTaskState.SUCCESS else VideoTaskState.ERROR
                    })
                    true
                }
                finishWork(task.also {
                    it.taskState = VideoTaskState.SUCCESS
                    it.percent = 100F

                })
            } else {
                Log.e("TmpFile", "Download failed with exit code $exitCode.")
                // Detect network-related errors
                val isNetworkError = line?.contains("No internet") == true || line?.contains("network unreachable") == true
                finishWork(task.also {
                    it.taskState =  if (isNetworkError) VideoTaskState.NO_INTERNET else VideoTaskState.ERROR
                    it.errorMessage = if (isNetworkError) "No internet connection"
                    else "Download failed with exit code $exitCode"
                })

            }
        }

        // Execute the download using executeYoutubeDLCommand in a cancellable way
        val result = withContext(Dispatchers.IO) {
            YoutubeDlUtils.executeYoutubeDLCommand(request, taskId, progressCallback, completionCallback)
        }
        Log.d("YoutubeDlDownloaderWorker", "YoutubeDL command finished taskId=$taskId responseNull=${result == null}")

        // Check for cancellation after execution
        if (isStopped) {
            Log.d("YoutubeDlDownloaderWorker", "Work stopped after download execution")
            return
        }
        if (result == null && !getDone()) {
            Log.e("YoutubeDlDownloaderWorker", "YoutubeDL returned null response taskId=$taskId requestUrl=$requestUrl")
            finishWork(task.also {
                it.taskState = VideoTaskState.ERROR
                it.errorMessage = "YoutubeDL returned null response"
            })
        }
    } catch (e: CancellationException) {
        Log.d("YoutubeDlDownloaderWorker", "Download was cancelled")
        // Clean up resources
        monitorProcess?.dispose()
        disposable?.dispose()
        progressDisposable?.dispose()
        throw e
    } catch (e: Exception) {
        Log.e("YoutubeDlDownloaderWorker", "Error in startDownload: ${e.message}")
        e.printStackTrace()
        finishWork(task.also {
            it.taskState = VideoTaskState.ERROR
            it.errorMessage = e.message ?: "Unknown worker error"
        })
    } }


    private fun handleMonitoring(
        folderSize: Long,
        taskId: String,
        task: VideoTaskItem,
        tmpFile: File
    ) {

        // Add network check here
        if (isDownloadStarted && !isNetworkAvailable()) {
            handleNetworkError(task)
            monitorProcess?.dispose()
            return
        }

        if (folderSize > 0 && folderSize != lastTmpDirSize) {
            val downloadedTmpFolderSize = FileUtil.getFileSizeReadable(folderSize.toDouble())
            lastTmpDirSize = folderSize

            if (progressCached > 0) {
                isDownloadOk = true
                monitorProcess?.dispose()
                return
            }

            // Update download progress
            updateDownloadProgress(taskId, task, downloadedTmpFolderSize, folderSize)
        }
    }


    private fun handleNetworkError(task: VideoTaskItem) {
        finishWork(task.also {
            it.taskState = VideoTaskState.NO_INTERNET
            it.errorMessage = "No internet connection"
        })
        Result.retry()
    }


    private fun setupRequestOptions(
        request: Any,
        vFormat: VideoFormatEntity,
        isContinue: Boolean
    ) {
        try {

            // Log thread count being set
            val threadsCount = SharedPrefHelper(applicationContext).getM3u8DownloaderThreadCount() + 1
            Log.d("setupRequestOptions", "Setting thread count: $threadsCount")

            // Use reflection to access the 'addOption' method
            val addOptionMethod = request::class.java.getDeclaredMethod(
                "addOption", String::class.java, String::class.java
            )

            // Add various options using reflection
            addOptionMethod.invoke(request, "--progress", "")
            addOptionMethod.invoke(request, "-N", threadsCount.toString())
            addOptionMethod.invoke(request, "--merge-output-format", "mp4")
            addOptionMethod.invoke(request, "--hls-prefer-native", "")
            addOptionMethod.invoke(request, "--hls-use-mpegts", "")
            addOptionMethod.invoke(request, "--verbose", "")
            // Check if we need to add the continue option
            if (isContinue) {
                Log.d("setupRequestOptions", "Adding continue option to request")
                addOptionMethod.invoke(request, "--continue", "")
            }

            // Proxy setup
            val currentProxy = proxyController.getCurrentRunningProxy()
            Log.d("setupRequestOptions", "Current Proxy: $currentProxy")

            if (currentProxy != Proxy.noProxy()) {
                val user = proxyController.getProxyCredentials().first
                val password = proxyController.getProxyCredentials().second
                Log.d("setupRequestOptions", "Proxy User: $user, Proxy Password: $password")
                if (user.isNotEmpty() && password.isNotEmpty()) {
                    addOptionMethod.invoke(
                        request,
                        "--proxy",
                        "https://${user}:${password}@${currentProxy.host}:${currentProxy.port}"
                    )
                    Log.d("setupRequestOptions", "Adding proxy with credentials: ${currentProxy.host}:${currentProxy.port}")
                } else {
                    addOptionMethod.invoke(
                        request,
                        "--proxy",
                        "${currentProxy.host}:${currentProxy.port}"
                    )
                    Log.d("setupRequestOptions", "Adding proxy without credentials: ${currentProxy.host}:${currentProxy.port}")
                }
            }

            // Video format options
            val videoOnly = vFormat.vcodec != "none" && vFormat.acodec == "none"
            val formatOption = if (videoOnly) "${vFormat.formatId}+bestaudio" else vFormat.formatId
            if (!YoutubeDlDownloader.isFaceBook){
                addOptionMethod.invoke(request, "-f", formatOption)
                Log.d("CommandAdded", "-f")
            }
            Log.d("setupRequestOptions", "Video format: ${vFormat.formatId}, Video only: $videoOnly")
            // Add HTTP headers
            vFormat.httpHeaders?.forEach {
                if (it.key != "Cookie") {
                    addOptionMethod.invoke(request, "--add-header", "${it.key}:${it.value}")
                    Log.d("setupRequestOptions", "Adding HTTP header: ${it.key}:${it.value}")
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("setupRequestOptions", "Error during request options setup: ${e.message}", e)
        }
    }


    private fun updateDownloadProgress(
        taskId: String,
        task: VideoTaskItem,
        downloadedTmpFolderSize: String,
        folderSize: Long
    ) {
        if (isDownloadJustStarted && !isDownloadOk) {
            ++isLiveCounter
            if (isLiveCounter > 2) {
                isLiveCounter = 3
                val downloaded = lastTmpDirSize

                saveProgress(taskId, LineInfo("LIVE", downloaded.toDouble(), downloaded.toDouble(), sourceLine = "Downloading live stream...downloaded: $downloadedTmpFolderSize, press stop and save, to stop downloading and save downloaded at any time...!"),
                    task.also { item ->
                        item.taskState = VideoTaskState.DOWNLOADING
                        item.lineInfo = downloadedTmpFolderSize
                        item.downloadSize = downloaded
                        item.totalSize = downloaded
                    }).blockingFirst(Unit)

                showProgress(
                    taskId,
                    taskId,
                    task.title,
                    99,
                    "Downloading Live Stream... $downloadedTmpFolderSize",
                    tmpFile
                )
            }
        }
    }


    private fun calculateFolderSize(directory: File): Long {
        var length = 0L
        if (directory.isDirectory) {
            for (file in directory.listFiles() ?: emptyArray()) {
                length += calculateFolderSize(file)
            }
        } else {
            length += directory.length()
        }
        return length
    }

    private fun handleError(
        taskId: String,
        url: String,
        progressCached: Int,
        throwable: Throwable,
        tmpFileName: String,
        name: String
    ) {
        AppLogger.d("Download Error: $throwable \ntaskId: $taskId")

        finishWork(VideoTaskItem(url).also { f ->
            try {
                // Dynamically load YoutubeDL.CanceledException class
                val canceledExceptionClass =
                    Class.forName("com.example.youtubedl.YoutubeDL\$CanceledException")

                // Check if the throwable is an instance of CanceledException using reflection
                if (canceledExceptionClass.isInstance(throwable)) {
                    if (isCanceled) {
                        f.taskState = VideoTaskState.CANCELED
                        f.errorCode = 0
                    } else {
                        f.taskState = VideoTaskState.PAUSE
                        f.errorCode = 0
                    }
                } else {
                    // If not CanceledException, handle as a generic error
                    f.taskState = VideoTaskState.ERROR
                    f.errorCode = 1
                    f.errorMessage = throwable.message?.replace(Regex("WARNING:.+\n"), "") ?: ""
                }
            } catch (e: ClassNotFoundException) {
                // Handle the case where CanceledException is not found
                f.taskState = VideoTaskState.ERROR
                f.errorCode = 1
                f.errorMessage = throwable.message?.replace(Regex("WARNING:.+\n"), "") ?: ""
            }

            // Common fields
            f.fileName = name
            f.percent = progressCached.toFloat()
        })
    }

    //[download]   0.3% of ~  49.94MiB at  438.62KiB/s ETA 04:41 (frag 2/201)
    private fun parseInfoFromLine(line: String?): LineInfo? {
        if (line == null) {
            return null
        }
        return if (line.startsWith("[download]")) {
            val tmp = line.split(Regex(" +"))
            val percent = tmp[1].replace("%", "").trim().toDoubleOrNull()

            var indx = 3
            if (line.contains("~")) indx = 4
            val totalStr = tmp[indx]

            val p: Pattern = Pattern.compile("\\p{L}")
            val tM: Matcher = p.matcher(totalStr)
            if (tM.find()) {
                val indxT = totalStr.substring(0, tM.start())
                val valT = totalStr.substring(tM.start())
                val totalParsed = LineInfo.parse("$indxT $valT")

                return if (tmp.last().contains(")")) {
                    val downloadedFrag =
                        tmp.last().split("/")[0].replace("(frag ", "").toIntOrNull()
                    val totalFrag = tmp.last().split("/")[0].replace(") ", "").toIntOrNull()


                    LineInfo(
                        "download",
                        totalParsed * percent!! / 100,
                        totalParsed,
                        downloadedFrag,
                        totalFrag,
                        sourceLine = line
                    )
                } else {
                    LineInfo(
                        "download", totalParsed * percent!! / 100, totalParsed, sourceLine = line
                    )
                }
            }

            return null
        } else {
            LineInfo("download", 0.0, 0.0, sourceLine = line)
        }
    }

    private class LineInfo(
        val id: String,
        val progress: Double,
        val total: Double,
        val fragDownloaded: Int? = null,
        val fragTotal: Int? = null,
        val sourceLine: String
    ) {
        companion object {
            private const val KB_FACTOR: Long = 1000
            private const val KIB_FACTOR: Long = 1024
            private const val MB_FACTOR = 1000 * KB_FACTOR
            private const val MIB_FACTOR = 1024 * KIB_FACTOR
            private const val GB_FACTOR = 1000 * MB_FACTOR
            private const val GIB_FACTOR = 1024 * MIB_FACTOR

            fun parse(arg0: String): Double {
                val spaceNdx = arg0.indexOf(" ")
                val ret = arg0.substring(0, spaceNdx).toDouble()
                when (arg0.substring(spaceNdx + 1)) {
                    "GB" -> return ret * GB_FACTOR
                    "GiB" -> return ret * GIB_FACTOR
                    "MB" -> return ret * MB_FACTOR
                    "MiB" -> return ret * MIB_FACTOR
                    "KB" -> return ret * KB_FACTOR
                    "KiB" -> return ret * KIB_FACTOR
                    "B" -> return ret
                }
                return (-1).toDouble()
            }
        }

        override fun toString(): String {
            return "${FileUtils.byteCountToDisplaySize(progress.toLong())} / ${
                FileUtils.byteCountToDisplaySize(
                    total.toLong()
                )
            }  frag: $fragDownloaded / $fragTotal"
        }
    }

    private fun showProgress(
        id: String, taskId: String, name: String, progress: Int, line: String, tmpFile: File
    ) {
        val text = line.replace(tmpFile.toString(), "")

        val taskItem = VideoTaskItem("").also {
            it.mId = taskId
            it.fileName = name
            it.taskState = VideoTaskState.DOWNLOADING
            it.percent = progress.toFloat()
            it.lineInfo = text
        }
        val data = notificationsHelper.createNotificationBuilder(taskItem)

        // Only use setForegroundAsync for foreground workers to avoid redundant Binder calls
        // Removing blocking showNotification() call to prevent ANR from slow Binder calls
        showNotificationAsync(data.first, data.second)
    }


    @SuppressLint("CheckResult")
    override fun finishWork(item: VideoTaskItem?) {
        if (getDone()) {
            try {
                getContinuation().resume(Result.success())
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return
        }

        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)

        if (taskId != null) {
            GenericDownloader.deleteHeadersStringFromSharedPreferences(applicationContext, taskId)
        }

        notificationsHelper.hideNotification(taskId.hashCode())
        if (item != null) {
            showNotification(
                taskId.hashCode() + 1, notificationsHelper.createNotificationBuilder(item.also {
                    it.mId = taskId
                }).second
            )
        }
        disposable?.dispose()
        progressDisposable?.dispose()
        disposable = null
        progressDisposable = null
        cookieFile?.delete()

        if (taskId != null) {
            if (item != null) {
                saveProgress(
                    taskId,
                    line = LineInfo(taskId, 0.0, 0.0, sourceLine = item.errorMessage ?: ""),
                    item
                ).blockingFirst(Unit)
                setDone()

                try {
                    // Send broadcast for download completion dialog
                    if (item.taskState == VideoTaskState.SUCCESS) {
                        Log.d("TESTdialogue", "Sending download complete broadcast...")
                        DownloadCompletionBroadcast.send(
                            applicationContext,
                            progressRepository,
                            taskId,
                            item,
                            fileUtil
                        )
                    }


                    if (item.taskState == VideoTaskState.ERROR) {
                        getContinuation().resume(Result.failure())
                    } else {
                        getContinuation().resume(Result.success())
                    }
                } catch (_: Exception) {
                    try {
                        getContinuation().resume(Result.failure())
                    } catch (_: Throwable) {

                    }
                }
            } else {
                try {
                    getContinuation().resume(Result.failure())
                } catch (_: Throwable) {

                }
            }
        } else {
            try {
                getContinuation().resume(Result.failure())
            } catch (_: Throwable) {

            }
        }
    }

    private fun saveProgress(
        taskId: String, line: LineInfo? = null, task: VideoTaskItem
    ): Observable<Unit> {
        if (getDone() && task.taskState == VideoTaskState.DOWNLOADING) {
            AppLogger.d(
                "saveProgress task returned cause DONE!"
            )
            return Observable.empty()
        }
        val isBytesNoTouch = line?.total == null || line.total == 0.0
        val iProgressUpdate = task.downloadSize.toInt() > 0

        return progressRepository.getProgressInfos().doOnSubscribe {
            YoutubeDlDownloaderDisposableContainer.links[taskId] = it
        }.take(1).toObservable().flatMap { progressList ->
            val dbTask = progressList.find { it.id == taskId }

            if (!isBytesNoTouch) {
                dbTask?.progressTotal = (line?.total ?: task.totalSize).toLong()
            }

            if (task.taskState != VideoTaskState.SUCCESS) {
                if (!isBytesNoTouch && iProgressUpdate) {
                    dbTask?.progressDownloaded = task.downloadSize
                }
            } else {
                dbTask?.progressDownloaded = dbTask?.progressTotal ?: -1
            }

            dbTask?.fragmentsTotal = line?.fragTotal ?: 1
            dbTask?.fragmentsDownloaded = line?.fragDownloaded ?: 0
            dbTask?.downloadStatus = task.taskState

            dbTask?.infoLine = line?.sourceLine ?: ""

            if (line?.id == "LIVE" && dbTask?.isLive != true) {
                dbTask?.isLive = true
            }

            if (dbTask != null) {
                if (getDone() && task.taskState == VideoTaskState.DOWNLOADING) {
                    AppLogger.d(
                        "saveProgress task returned cause DONE!"
                    )
                } else {
                    progressRepository.saveProgressInfo(dbTask)
                }
            }
            Observable.empty()
        }
    }

    private fun resumeDownload(task: VideoTaskItem, headers: Map<String, String>) {
        try {
            // Run startDownload in a coroutine scope to handle cancellation
            CoroutineScope(Dispatchers.IO).launch {
                startDownload(task, headers,true)
            }
        } catch (e: CancellationException) {
            Log.d("YoutubeDlDownloaderWorker", "Download action was cancelled")
            // Clean up resources
            monitorProcess?.dispose()
            disposable?.dispose()
            progressDisposable?.dispose()
        }
    }

    private fun pauseDownload(task: VideoTaskItem, headers: Map<String, String>) {
        if (getDone()) return

        val id = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)
        if (id != null) {
            try {
                // Using the youtubeDl instance to call destroyProcessById
                val destroyMethod =
                    youtubeDl::class.java.getMethod("destroyProcessById", String::class.java)
                destroyMethod.invoke(youtubeDl, id)
            } catch (e: Exception) {
                Log.d("stopAndSave", "Error invoking destroyProcessById: " + e.message)
            }
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(id)
            if (task.taskState != VideoTaskState.DOWNLOADING) {
                finishWork(task.also {
                    it.mId = id.toString()
                    it.taskState = VideoTaskState.PAUSE
                })
            }
        }
    }

    private fun cancelDownload(task: VideoTaskItem, headers: Map<String, String>) {
        val taskId = inputData.getString(GenericDownloader.DOWNLOAD_ID_KEY)
        val isFileRemove = inputData.getBoolean(GenericDownloader.IS_FILE_REMOVE_KEY, false)

        if (taskId != null) {
            val fileToRemove = File("${fileUtil.tmpDir}/$taskId")

            if (isFileRemove) {
                fileToRemove.deleteRecursively()
            }

            try {
                // Using the youtubeDl instance to call destroyProcessById
                val destroyMethod =
                    youtubeDl::class.java.getMethod("destroyProcessById", String::class.java)
                destroyMethod.invoke(youtubeDl, taskId)
            } catch (e: Exception) {
                Log.d("stopAndSave", "Error invoking destroyProcessById: " + e.message)
            }

            if (task.taskState != VideoTaskState.DOWNLOADING) {
                finishWork(task.also {
                    it.mId = taskId.toString()
                    it.taskState = VideoTaskState.CANCELED
                })
            }
        }
    }

    private fun isM3u8OrMpd(url: String): Boolean {
        return url.contains(".m3u8") || url.contains(".mpd") || url.contains(".txt")
    }


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

}
