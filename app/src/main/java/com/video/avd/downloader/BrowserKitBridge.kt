package com.video.avd.downloader

import android.content.Context
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.avd.browserkit.api.BrowserDownloadBridge
import com.avd.browserkit.api.BrowserHostDownloadRequest
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.data.repository.ProgressRepository
import com.avd.util.FileUtil
import com.avd.util.downloaders.custom_downloader_service.CustomRegularDownloader
import com.avd.util.downloaders.youtubedl_downloader.YoutubeDlDownloader
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BrowserKitHostEntryPoint {
    fun progressRepository(): ProgressRepository
    fun fileUtil(): FileUtil
}

object BrowserKitBridge : BrowserDownloadBridge {
    private const val TAG = "BrowserKitBridge"
    private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "init context=${context.packageName}")
    }

    override fun enqueueHostDownload(request: BrowserHostDownloadRequest): Boolean {
        val context = appContext ?: return false
        Log.d(
            TAG,
            "enqueueHostDownload title=${request.title} stream=${request.streamType} useYtdlp=${request.useYtdlp} " +
                "useAvd=${request.useAvd} facebook=${request.facebookMode} live=${request.isLive} " +
                "url=${request.downloadUrl} page=${request.pageUrl} headers=${request.headers.keys}",
        )
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            BrowserKitHostEntryPoint::class.java,
        )

        val fileUtil = entryPoint.fileUtil()
        if (!fileUtil.folderDir.exists() && !fileUtil.folderDir.mkdirs()) {
            Log.e(TAG, "Download folder creation failed for ${request.title}")
            return false
        }

        val useRegularDownloader = shouldUseRegularDownloader(request)
        val ext = inferExtension(request)
        val videoInfo = buildVideoInfo(request, useRegularDownloader, ext)
        Log.d(
            TAG,
            "mapped host download videoId=${videoInfo.id} regular=$useRegularDownloader ext=$ext " +
                "isM3u8=${videoInfo.isM3u8} formatCount=${videoInfo.formats.formats.size}",
        )
        val progressInfo = ProgressInfo(
            id = videoInfo.id,
            downloadId = videoInfo.id.hashCode().toLong(),
            videoInfo = videoInfo,
            isLive = request.isLive,
            isM3u8 = videoInfo.isM3u8,
        )

        bridgeScope.launch {
            Log.d(TAG, "saving progressInfo id=${progressInfo.id} downloadId=${progressInfo.downloadId}")
            entryPoint.progressRepository().saveProgressInfo(progressInfo)
            if (useRegularDownloader) {
                Log.d(TAG, "dispatch -> CustomRegularDownloader videoId=${videoInfo.id}")
                CustomRegularDownloader.addDownload(context, videoInfo)
            } else {
                Log.d(TAG, "dispatch -> YoutubeDlDownloader videoId=${videoInfo.id}")
                YoutubeDlDownloader.startDownload(context, videoInfo)
            }
        }

        Log.d(
            TAG,
            "Host download accepted title=${request.title}, regular=$useRegularDownloader, stream=${request.streamType}",
        )
        return true
    }

    override fun onTaskUpdated(snapshot: com.avd.browserkit.api.BrowserDownloadSnapshot) {
        Log.d(
            TAG,
            "bridge update taskId=${snapshot.taskId} status=${snapshot.status} percent=${snapshot.percent} " +
                "quality=${snapshot.qualityLabel} page=${snapshot.pageUrl}",
        )
    }

    override fun onTaskCompleted(result: com.avd.browserkit.api.BrowserDownloadResult) {
        Log.d(
            TAG,
            "bridge complete taskId=${result.taskId} success=${result.success} path=${result.filePath}",
        )
    }

    override fun onTaskFailed(taskId: String, message: String) {
        Log.e(TAG, "bridge failed taskId=$taskId message=$message")
    }

    private fun shouldUseRegularDownloader(request: BrowserHostDownloadRequest): Boolean {
        if (request.useYtdlp || request.useAvd || request.facebookMode || request.isLive) {
            return false
        }
        val streamType = request.streamType.lowercase()
        val url = request.downloadUrl.lowercase()
        return !streamType.contains("hls") &&
            !streamType.contains("mpd") &&
            !url.contains(".m3u8") &&
            !url.contains(".mpd")
    }

    private fun inferExtension(request: BrowserHostDownloadRequest): String {
        val url = request.downloadUrl.lowercase()
        val streamType = request.streamType.lowercase()
        return when {
            url.contains(".m3u8") || streamType.contains("hls") -> "m3u8"
            url.contains(".mpd") || streamType.contains("mpd") -> "mpd"
            else -> "mp4"
        }
    }

    private fun buildVideoInfo(
        request: BrowserHostDownloadRequest,
        isRegularDownload: Boolean,
        ext: String,
    ): VideoInfo {
        val headers = request.headers
        val format = VideoFormatEntity(
            formatId = request.formatId.ifBlank { "0" },
            url = request.downloadUrl,
            ext = ext,
            format = request.qualityLabel.ifBlank { "Default" },
            httpHeaders = headers,
        )
        return VideoInfo(
            id = UUID.randomUUID().toString(),
            title = request.title,
            originalUrl = request.pageUrl.ifBlank { request.downloadUrl },
            ext = ext,
            isRegularDownload = isRegularDownload,
            formats = VideFormatEntityList(listOf(format)),
            downloadUrls = listOf(
                Request.Builder()
                    .url(request.downloadUrl)
                    .headers(headers.toHeaders())
                    .build(),
            ),
        )
    }
}
