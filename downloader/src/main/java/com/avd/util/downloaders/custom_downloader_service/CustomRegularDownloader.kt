package com.avd.util.downloaders.custom_downloader_service

import android.content.Context
import androidx.work.*
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.data.local.room.entity.VideoInfo
import com.avd.util.AppLogger
import com.avd.util.downloaders.generic_downloader.GenericDownloader
import org.json.JSONObject
import android.util.Base64
import java.util.concurrent.TimeUnit

class CustomRegularDownloader : GenericDownloader() {

    companion object {
        fun addDownload(context: Context, videoInfo: VideoInfo, withoutHeaders: Boolean = false) {
            val downloadWork = getWorkRequest(videoInfo.id)
            val downloaderData = try {
                val downloaderData = getDownloadDataFromVideoInfo(context, videoInfo)
                downloaderData.putString(ACTION_KEY, DownloaderActions.DOWNLOAD)
                downloaderData.putString(TASK_ID_KEY, videoInfo.id)
                downloaderData.putBoolean(IS_TRY_WITHOUT_HEADERS, withoutHeaders)
            } catch (e: Exception) {
                val downloaderData = getDownloadDataFromVideoInfo(context, videoInfo, true)
                downloaderData.putString(ACTION_KEY, DownloaderActions.DOWNLOAD)
                downloaderData.putString(TASK_ID_KEY, videoInfo.id)
                downloaderData.putBoolean(IS_TRY_WITHOUT_HEADERS, withoutHeaders)
            }
            downloadWork.setInputData(downloaderData.build())
            runWorkerTask(context, videoInfo, downloadWork.build())
        }

        fun addDownload(context: Context, videoInfo: VideoInfo, withoutHeaders: Boolean = false,position: Int) {
            val downloadWork = getWorkRequest(videoInfo.id)
            val downloaderData = try {
                val downloaderData = getDownloadDataFromVideoInfo(context, videoInfo,true,position)
                downloaderData.putString(ACTION_KEY, DownloaderActions.DOWNLOAD)
                downloaderData.putString(TASK_ID_KEY, videoInfo.id)
                downloaderData.putBoolean(IS_TRY_WITHOUT_HEADERS, withoutHeaders)
                downloadWork.setInputData(downloaderData.build())
                runWorkerTask(context, videoInfo, downloadWork.build())
            }catch (e: IllegalStateException){
                e.printStackTrace()
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun cancelDownload(context: Context, progressInfo: ProgressInfo, removeFile: Boolean) {
            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.CANCEL)
            downloaderData.putString(IS_FILE_REMOVE_KEY, removeFile.toString())
            downloaderData.putString(TASK_ID_KEY, progressInfo.id)
            downloaderData.putInt(DOWNLOAD_ID_KEY, progressInfo.downloadId.toInt())
            downloadWork.setInputData(downloaderData.build())
            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }

        fun pauseDownload(context: Context, progressInfo: ProgressInfo) {
            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.PAUSE)
            downloaderData.putInt(DOWNLOAD_ID_KEY, progressInfo.downloadId.toInt())
            downloadWork.setInputData(downloaderData.build())
            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }

        fun resumeDownload(context: Context, progressInfo: ProgressInfo) {
            val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
            val downloaderData = getDownloadDataFromVideoInfo(context, progressInfo.videoInfo)
            downloaderData.putString(ACTION_KEY, DownloaderActions.RESUME)
            downloaderData.putString(TASK_ID_KEY, progressInfo.id)
            downloadWork.setInputData(downloaderData.build())
            downloadWork.addTag(progressInfo.videoInfo.id)
            runWorkerTask(context, progressInfo.videoInfo, downloadWork.build())
        }

        private fun runWorkerTask(context: Context, info: VideoInfo, taskData: OneTimeWorkRequest) {
            val op = WorkManager.getInstance(context)
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

        private fun getDownloadDataFromVideoInfo(
            context: Context,
            videoInfo: VideoInfo,
            skipCookie: Boolean = false
        ): Data.Builder {
            val videoUrl = videoInfo.firstUrlToString
            val headers = videoInfo.downloadUrls.firstOrNull()?.headers
            val headersMap = mutableMapOf<String, String>()

            for (name in headers?.names() ?: emptySet()) {
                headersMap[name] = headers?.get(name) ?: ""
            }

            var fileName = videoInfo.name
            if (!videoInfo.name.endsWith(".mp4")) {
                fileName = "${videoInfo.name}.mp4"
            }

            val cookie = headersMap["Cookie"]
            if (skipCookie && cookie != null) {
                headersMap.remove("Cookie")
            }
//            if (cookie != null) {
//                headersMap["Cookie"] =
//                    Base64.getEncoder().encodeToString(cookie.toString().toByteArray())
//            }

            if (cookie != null) {
                headersMap["Cookie"] = Base64.encodeToString(cookie.toString().toByteArray(), Base64.DEFAULT)
            }

            val headersForClean = (headersMap as Map<*, *>?)?.let { JSONObject(it).toString() }
//            val headersVal = try {
//                Base64.getEncoder().encodeToString(headersForClean?.toByteArray())
//            } catch (e: Exception) {
//                "{}"
//            }

            val headersVal = try {
                // Ensure headersForClean is not null before converting to ByteArray
                Base64.encodeToString(headersForClean?.toByteArray(), Base64.DEFAULT)
            } catch (e: Exception) {
                e.printStackTrace() // Log the exception for debugging
                "{}" // Return an empty JSON object in case of an error
            }

            val data = Data.Builder()
            data.putString(URL_KEY, videoUrl)
            data.putString(TASK_ID_KEY, videoInfo.id)

            val zipHeaders = compressString(headersVal)
            AppLogger.d("superZip ${zipHeaders.toByteArray().size}  ---- ${headersVal.toByteArray().size}")

            saveHeadersStringToSharedPreferences(
                context,
                videoInfo.id,
                zipHeaders
            )

            data.putString(TITLE_KEY, videoInfo.title)
            data.putString(FILENAME_KEY, fileName)

            return data
        }

        private fun getDownloadDataFromVideoInfo(
            context: Context,
            videoInfo: VideoInfo,
            skipCookie: Boolean = false,
            position: Int
        ): Data.Builder {
            val selectedRequest = videoInfo.downloadUrls.getOrNull(position)
                ?: videoInfo.downloadUrls.firstOrNull()
            val videoUrl = selectedRequest?.url?.toString() ?: videoInfo.firstUrlToString
            val headers = selectedRequest?.headers
            val headersMap = mutableMapOf<String, String>()
            for (name in headers?.names() ?: emptySet()) {
                headersMap[name] = headers?.get(name) ?: ""
            }
            var fileName = videoInfo.name
            if (!videoInfo.name.endsWith(".mp4")) {
                fileName = "${videoInfo.name}.mp4"
            }
            val cookie = headersMap["Cookie"]
            if (skipCookie && cookie != null) {
                headersMap.remove("Cookie")
            }
//            if (cookie != null) {
//                headersMap["Cookie"] =
//                    Base64.getEncoder().encodeToString(cookie.toString().toByteArray())
//            }
            if (cookie != null) {
                headersMap["Cookie"] = Base64.encodeToString(cookie.toString().toByteArray(), Base64.DEFAULT)
            }
            val headersForClean = (headersMap as Map<*, *>?)?.let { JSONObject(it).toString() }
//            val headersVal = try {
//                Base64.getEncoder().encodeToString(headersForClean?.toByteArray())
//            } catch (e: Exception) {
//                "{}"
//            }
            val headersVal = try {
                // Ensure headersForClean is not null before converting to ByteArray
                Base64.encodeToString(headersForClean?.toByteArray(), Base64.DEFAULT)
            } catch (e: Exception) {
                e.printStackTrace() // Log the exception for debugging
                "{}" // Return an empty JSON object in case of an error
            }
            val data = Data.Builder()
            data.putString(URL_KEY, videoUrl)
            data.putString(TASK_ID_KEY, videoInfo.id)
            val zipHeaders = compressString(headersVal)
            AppLogger.d("superZip ${zipHeaders.toByteArray().size}  ---- ${headersVal.toByteArray().size}")
            saveHeadersStringToSharedPreferences(
                context,
                videoInfo.id,
                zipHeaders
            )
            data.putString(TITLE_KEY, videoInfo.title)
            data.putString(FILENAME_KEY, fileName)
            return data
        }

        private fun getWorkRequest(id: String): OneTimeWorkRequest.Builder {
            return OneTimeWorkRequest.Builder(CustomRegularDownloaderWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS).addTag(id)
        }

    }

}

