package com.avd.util.downloaders.youtubedl_downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.data.local.room.entity.VideoInfo
import com.avd.util.AppLogger
import com.avd.util.ContextUtils
import com.avd.util.downloaders.generic_downloader.GenericDownloader
import com.avd.util.downloaders.youtubedl_downloader.YoutubeDlDownloaderWorker.Companion.STOP_SAVE_ACTION
import io.reactivex.rxjava3.disposables.Disposable
import org.reactivestreams.Subscription
import java.util.concurrent.TimeUnit


private const val TAG = "YT-DL-Downloader"

class YoutubeDlDownloader : GenericDownloader() {

    companion object {

        var isFaceBook=false

        fun startDownload(context: Context, videoInfo: VideoInfo) {
            Log.d(TAG, "▶️ startDownload() called for id=${videoInfo.id}")
            Log.d(TAG, "startDownload() called for videoInfo=${videoInfo}")
            YoutubeDlDownloaderDisposableContainer.disposableContainer[videoInfo.id].also {
                Log.d(TAG, "Disposing previous disposable for id=${videoInfo.id}")
                it?.dispose()
            }?: Log.d(TAG, "No existing disposable found for id=${videoInfo.id}")
            val downloadWork = getWorkRequest(videoInfo.id)
            Log.d(TAG, "WorkRequest builder created for id=${videoInfo.id}")
            val downloaderData = getDownloadDataFromVideoInfo(context, videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.DOWNLOAD)
            Log.d(TAG, "InputData prepared: ${downloaderData.toString()}")
            downloadWork.setInputData(downloaderData.build())
            Log.d(TAG, "WorkRequest built – workId=${downloadWork}")
            runWorkerTask(
                context, videoInfo, downloadWork.build()
            )
            Log.d(TAG, "WorkRequest enqueued; runWorkerTask() invoked")
        }

       /* fun cancelDownload(context: Context, progressInfo: ProgressInfo, removeFile: Boolean) {
            YoutubeDlDownloaderDisposableContainer.disposableContainer[progressInfo.videoInfo.id]?.dispose()

            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.CANCEL)
            downloaderData.putBoolean(IS_FILE_REMOVE_KEY, removeFile)
            downloadWork.setInputData(downloaderData.build())

            runWorkerTask(
                context, progressInfo.videoInfo, downloadWork.build()
            )
        }*/

        fun cancelDownload(context: Context, progressInfo: ProgressInfo, removeFile: Boolean) {
            // Dispose any running downloader
            YoutubeDlDownloaderDisposableContainer.disposableContainer[progressInfo.videoInfo.id]?.dispose()

            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)

            // Use safe Data: only small info
            val downloaderData = Data.Builder()
                .putString(TASK_ID_KEY, progressInfo.videoInfo.id)  // only ID
                .putString(ACTION_KEY, DownloaderActions.CANCEL)
                .putBoolean(IS_FILE_REMOVE_KEY, removeFile)
                .build()

            downloadWork.setInputData(downloaderData)

            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }


        fun stopAndSaveDownload(context: Context, progressInfo: ProgressInfo) {
            YoutubeDlDownloaderDisposableContainer.disposableContainer[progressInfo.videoInfo.id]?.dispose()

            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, STOP_SAVE_ACTION)
            downloadWork.setInputData(downloaderData.build())

            runWorkerTask(
                context, progressInfo.videoInfo, downloadWork.build()
            )
        }

        /*fun pauseDownload(context: Context, progressInfo: ProgressInfo) {
            YoutubeDlDownloaderDisposableContainer.disposableContainer[progressInfo.videoInfo.id]?.dispose()
            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.PAUSE)
            downloadWork.setInputData(downloaderData.build())
            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }
*/


        fun pauseDownload(context: Context, progressInfo: ProgressInfo) {
            YoutubeDlDownloaderDisposableContainer.disposableContainer[progressInfo.videoInfo.id]?.dispose()

            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)

            // ✅ Only store minimal info
            val downloaderData = Data.Builder()
                .putString("video_id", progressInfo.videoInfo.id)
                .putString(ACTION_KEY, DownloaderActions.PAUSE)

            downloadWork.setInputData(downloaderData.build())

            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }

        fun resumeDownload(context: Context, progressInfo: ProgressInfo) {
            YoutubeDlDownloaderDisposableContainer.disposableContainer[progressInfo.videoInfo.id]?.dispose()
            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.RESUME)
            downloadWork.setInputData(downloaderData.build())
            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }

        private fun runWorkerTask(context: Context, info: VideoInfo, taskData: OneTimeWorkRequest) {
            val op = WorkManager.getInstance(ContextUtils.getApplicationContext())
                .cancelAllWorkByTag(info.id)
            try {
                op.result.get()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    info.id, ExistingWorkPolicy.REPLACE, taskData
                )
            }
        }

        private fun getDownloadDataFromVideoInfo(context: Context, videoInfo: VideoInfo): Data.Builder {
            val videoUrl: String = if (videoInfo.downloadUrls.isNotEmpty()) {
                videoInfo.originalUrl
            } else {
                videoInfo.formats.formats.firstOrNull()?.url.toString()
            }
            val headersMap = if (videoInfo.formats.formats.isNotEmpty()) {
                    videoInfo.formats.formats.firstOrNull()?.httpHeaders?.toMutableMap()
                        ?: mutableMapOf()
                } else {
                    mutableMapOf()
                }
            headersMap.remove("Cookie")
            val data = Data.Builder()
//            val headersVal = try {
//                Base64.getEncoder().encodeToString(Gson().toJson(headersMap).toString().toByteArray())
//            } catch (e: Exception) {
//                "{}"
//            }

            // Assuming headersMap is your Map<String, Any>
            val headersVal = try {
                val jsonString = Gson().toJson(headersMap) // Convert the map to JSON
                val encodedString = Base64.encodeToString(jsonString.toByteArray(), Base64.DEFAULT) // Encode to Base64
                encodedString
            } catch (e: Exception) {
                "{}" // Return an empty JSON object in case of an error
            }

            data.putString(URL_KEY, videoUrl)
            data.putString(
                HEADERS_KEY,
                headersVal
            )
            data.putString(TITLE_KEY, videoInfo.title)
            data.putString(FILENAME_KEY, videoInfo.name)
            data.putString(ORIGIN_KEY, videoInfo.originalUrl)
            data.putString(DOWNLOAD_ID_KEY, videoInfo.id)
            if (videoInfo.formats.formats.firstOrNull() != null && videoInfo.formats.formats.isNotEmpty()) {

                val stringHeaders = Gson().toJson(videoInfo.formats.formats.firstOrNull()).toString()

//                val zipHeaders = Base64.getEncoder().encodeToString(stringHeaders.toByteArray())

                // Assuming stringHeaders is your String that you want to encode
                val zipHeaders = try {
                    Base64.encodeToString(stringHeaders.toByteArray(), Base64.DEFAULT)
                } catch (e: Exception) {
                    "" // Return an empty string in case of an error
                }

                val superZip = compressString(zipHeaders)

                AppLogger.d("superZip ${superZip.toByteArray().size}  ---- ${zipHeaders.toByteArray().size}")

                saveHeadersStringToSharedPreferences(context, videoInfo.id, superZip)
            }
            return data
        }

        private fun getWorkRequest(id: String): OneTimeWorkRequest.Builder {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            return OneTimeWorkRequest.Builder(YoutubeDlDownloaderWorker::class.java).addTag(id)
                .setConstraints(constraints).setBackoffCriteria(
                    BackoffPolicy.LINEAR, // Retry with increasing delay
                    30, // Initial retry delay of 30 seconds
                    TimeUnit.SECONDS
                )
        }
    }
}

class CancelReceiver : BroadcastReceiver() {
    companion object {
        const val TASK_ID = "taskId"
        const val NOTIFICATION_ID = "notificationId"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val taskId = intent.getStringExtra(TASK_ID)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)

        if (taskId.isNullOrEmpty()) return

        YoutubeDlDownloader.cancelDownload(
            context!!,
            ProgressInfo(
                id = taskId,
                downloadId = taskId.hashCode().toLong(),
                videoInfo = VideoInfo(id = taskId)
            ),
            false
        )
    }
}

class YoutubeDlDownloaderDisposableContainer {
    companion object {
        val disposableContainer = mutableMapOf<String, Disposable>()
        val links = mutableMapOf<String, Subscription>()
    }
}