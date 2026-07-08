package com.avd.util.downloaders.generic_downloader.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.avd.util.FileUtil
import com.avd.util.downloaders.generic_downloader.GenericDownloader
import com.avd.util.downloaders.generic_downloader.IDownloadListener
import com.avd.util.downloaders.generic_downloader.models.Video
import com.avd.util.downloaders.generic_downloader.models.VideoTaskItem
import com.google.gson.Gson
import com.avd.util.AppLogger
import java.io.File
import java.io.Serializable
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class GenericDownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams), IDownloadListener {

    @Volatile
    private lateinit var continuation: Continuation<Result>

    private val fileDir: String = File(applicationContext.filesDir.absolutePath, FileUtil.FOLDER_NAME).absolutePath

    @Volatile
    private var isDone: Boolean = false

    abstract fun finishWork(item: VideoTaskItem?)

    // Creates an instance of ForegroundInfo which can be used to update the
    // ongoing notification.
    abstract fun createForegroundInfo(task: VideoTaskItem): ForegroundInfo

    abstract fun handleAction(action: String, task: VideoTaskItem, headers: Map<String, String>, isFileRemove: Boolean)

    open fun fixFileName(fileName: String): String {
        val currentFile = File(fileName)
        return if (!currentFile.exists()) {
            var fixedFileName = File("${currentFile.parent}/${currentFile.nameWithoutExtension}.mp4")
            while (fixedFileName.exists()) {
                fixedFileName = File("${fixedFileName.parent}/${fixedFileName.nameWithoutExtension}_cp.mp4")
            }
            fixedFileName.absolutePath
        } else {
            var fixedFileName = File("${currentFile.parent}/${currentFile.nameWithoutExtension}_cp.mp4")
            while (fixedFileName.exists()) {
                fixedFileName =
                    File("${fixedFileName.parent}/${fixedFileName.nameWithoutExtension}_cp.mp4")
            }
            fixedFileName.absolutePath
        }
    }

    open fun getTaskFromInput(): VideoTaskItem {
        val url = inputData.getString(GenericDownloader.URL_KEY)
        val fileName = inputData.getString(GenericDownloader.FILENAME_KEY) ?: url.hashCode().toString()
        val title = inputData.getString(GenericDownloader.TITLE_KEY)
        val ext = inputData.getString(GenericDownloader.EXT_KEY)
        val task = VideoTaskItem(url)
        task.mId = try {
            inputData.getString(GenericDownloader.TASK_ID_KEY)
        } catch (e: Throwable) {
            null
        }
        task.title = title
        task.fileName = fileName
        task.saveDir = fileDir
        task.filePath = "${task.saveDir}/${task.fileName}"
        task.videoType = getVideoTypeByExt(ext)
        return task
    }

    // Add this method in your CustomRegularDownloaderWorker class
    private fun checkForegroundServicePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun doWork(): Result {
        val cor = suspendCoroutine { continuation ->
            setWorkContinuation(continuation)
            try {
                val task = getTaskFromInput()
                val action = inputData.getString(GenericDownloader.ACTION_KEY)
                val isFileRemove = inputData.getString(GenericDownloader.IS_FILE_REMOVE_KEY).toString() == "true"
                val inpHeaders = GenericDownloader.loadHeadersStringFromSharedPreferences(applicationContext, task.mId)
                val rawHeaders = if (inpHeaders != null) {
                    try {
                        val decompressedString = GenericDownloader.decompressString(inpHeaders) // Assuming this method is correctly defined
                        String(Base64.decode(decompressedString, Base64.DEFAULT)) // Decode the Base64 string
                    } catch (e: Exception) {
                        e.printStackTrace() // Log the exception for debugging
                        "{}" // Return an empty JSON object in case of an error
                    }
                } else {
                    "{}" // Return an empty JSON object if inpHeaders is null
                }

                val headers = Gson().fromJson<Map<String, String>>(
                    rawHeaders, Map::class.java
                )
                /*if (action.isNullOrEmpty() || action.isBlank() || task.url == null) {
                    continuation.resumeWithException(Throwable("ACTION or TASK is null"))
                }*/

                if (action.isNullOrEmpty() || action.isBlank() || task.url == null) {
                    // If validation fails, immediately resume with failure and exit
                    continuation.resume(Result.failure())
                    return@suspendCoroutine // Exit the block
                }
                Log.d("internetConnect","$action")
                handleAction(action!!, task, headers, isFileRemove)
            } catch (e: Throwable) {
                AppLogger.d(e.printStackTrace().toString())
               // continuation.resumeWithException(e)
                continuation.resume(Result.failure())
            }
        }
        afterDone()
        return cor
    }


    open fun afterDone() {

    }

    @Synchronized
    fun setDone() {
        isDone = true
    }

    @Synchronized
    fun getDone(): Boolean {
        return isDone
    }

    override fun onDownloadDefault(item: VideoTaskItem?) {
    }

    override fun onDownloadPending(item: VideoTaskItem?) {
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    override fun onDownloadPrepare(item: VideoTaskItem?) {
        if (getDone()) {
            return
        }
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    override fun onDownloadStart(item: VideoTaskItem?) {
        if (getDone()) {
            return
        }
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    override fun onDownloadProgress(item: VideoTaskItem?) {
        if (getDone()) {
            return
        }
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    override fun onDownloadSpeed(item: VideoTaskItem?) {
        if (getDone()) {
            return
        }
    }

    override fun onDownloadPause(item: VideoTaskItem?) {
        if (getDone()) {
            return
        }
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    override fun onDownloadError(item: VideoTaskItem?) {
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    override fun onDownloadSuccess(item: VideoTaskItem?) {
        item?.let { createForegroundInfo(it) }?.let { setForegroundAsync(it) }
    }

    @Synchronized
    private fun setWorkContinuation(continuation: Continuation<Result>) {
        this.continuation = continuation
    }

    @Synchronized
    fun getContinuation(): Continuation<Result> {
        return this.continuation
    }

    private fun getVideoTypeByExt(ext: String?): Int {
        return if (ext?.contains("m3u8") == true) {
            Video.Type.HLS_TYPE
        } else if (ext?.contains("mp4") == true) {
            Video.Type.MP4_TYPE
        } else if (ext?.contains("mkv") == true) {
            Video.Type.MKV_TYPE
        } else if (ext?.contains("3gp") == true) {
            Video.Type.GP3_TYPE
        } else if (ext?.contains("webm") == true) {
            Video.Type.WEBM_TYPE
        } else if (ext?.contains("qt") == true || ext?.contains(".mov") == true) {
            Video.Type.QUICKTIME_TYPE
        } else {
            Video.Type.DEFAULT
        }
    }

}





class Progress(var currentBytes: Long, var totalBytes: Long) : Serializable {
    override fun toString(): String {
        return "Progress{" +
                "currentBytes=" + currentBytes +
                ", totalBytes=" + totalBytes +
                '}'
    }
}
