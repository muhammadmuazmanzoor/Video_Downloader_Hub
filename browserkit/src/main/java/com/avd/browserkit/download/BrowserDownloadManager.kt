package com.avd.browserkit.download

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.avd.browserkit.api.BrowserHostDownloadRequest
import com.avd.browserkit.api.BrowserKit
import com.avd.browserkit.data.BrowserDownloadEntity
import com.avd.browserkit.data.BrowserRepository
import com.avd.browserkit.detection.DetectedVideoInfo
import com.avd.browserkit.detection.StreamFormat
import com.avd.browserkit.detection.StreamType
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.DailymotionUrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object BrowserDownloadManager {
    private const val WORK_TAG = "browserkit_download"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tasks = MutableStateFlow<List<BrowserDownloadTask>>(emptyList())
    val tasks: StateFlow<List<BrowserDownloadTask>> = _tasks.asStateFlow()

    private lateinit var repository: BrowserRepository
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        repository = BrowserRepository(appContext)
        refresh()
    }

    fun refresh() {
        if (!::repository.isInitialized) return
        scope.launch {
            val entities = repository.getAllDownloads()
            BrowserKitLog.d("Download.Refresh", "count=${entities.size}")
            _tasks.value = entities.map { it.toTask() }
        }
    }

    fun enqueue(info: DetectedVideoInfo, format: StreamFormat) {
        if (!::appContext.isInitialized) {
            BrowserKitLog.e("Enqueue", "abort: BrowserDownloadManager not init")
            return
        }
        val downloadUrl = format.url
        // Drop duplicate active jobs for same media URL (multi-fire JS bridge).
        val activeDuplicate = _tasks.value.firstOrNull { task ->
            task.downloadUrl == downloadUrl &&
                (
                    task.status == BrowserDownloadStatus.QUEUED ||
                        task.status == BrowserDownloadStatus.DOWNLOADING
                    )
        }
        if (activeDuplicate != null) {
            BrowserKitLog.w(
                "Enqueue",
                "skip duplicate active taskId=${activeDuplicate.id} " +
                    "url=${BrowserKitLog.shortUrl(downloadUrl)}",
            )
            return
        }

        val mergedHeaders = info.headers + format.headers
        val site = com.avd.browserkit.util.BrowserSiteUtils.siteNameFromUrl(
            info.pageUrl.ifBlank { downloadUrl },
        )
        BrowserKitLog.i(
            "Enqueue",
            "route start title=${info.title} type=${format.streamType} regular=${info.isRegularDownload} " +
                "avd=${info.isDetectedByAvd} formats=${info.formats.size} " +
                "url=${BrowserKitLog.shortUrl(downloadUrl)} page=${BrowserKitLog.shortUrl(info.pageUrl)}",
        )
        // Xilli dual path: hand FB/IG CDN to host CustomRegular / yt-dlp first.
        val hostHandled = tryEnqueueViaHost(info, format, mergedHeaders)
        if (hostHandled) {
            BrowserKitLog.i(
                "Enqueue",
                "host handled type=${format.streamType} url=${BrowserKitLog.shortUrl(downloadUrl)}",
            )
            BrowserKit.analytics()?.onBrowserDownloadStart(
                site = site,
                pageUrl = info.pageUrl,
                method = format.streamType.name.lowercase(),
            )
            return
        }
        BrowserKitLog.i("Enqueue", "host declined -> browserkit worker route")

        val taskId = System.currentTimeMillis().toString()
        BrowserKitLog.i(
            "Enqueue",
            "taskId=$taskId type=${format.streamType} label=${format.label} " +
                "title=${info.title} url=${BrowserKitLog.shortUrl(downloadUrl)} " +
                "page=${BrowserKitLog.shortUrl(info.pageUrl)} headers=${mergedHeaders.keys}",
        )
        val entity = BrowserDownloadEntity(
            id = taskId,
            title = info.title,
            pageUrl = info.pageUrl,
            downloadUrl = downloadUrl,
            qualityLabel = format.label,
            streamType = format.streamType.name,
            headersJson = BrowserRepository(appContext).headersToJson(mergedHeaders),
            percent = 0,
            status = BrowserDownloadStatus.QUEUED.name,
            filePath = null,
            workerId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        scope.launch {
            // Re-check after switching to IO — rapid double-tap race.
            val existing = repository.getAllDownloads().firstOrNull { row ->
                row.downloadUrl == downloadUrl &&
                    (
                        row.status == BrowserDownloadStatus.QUEUED.name ||
                            row.status == BrowserDownloadStatus.DOWNLOADING.name
                        )
            }
            if (existing != null) {
                BrowserKitLog.w("Enqueue", "skip duplicate db taskId=${existing.id}")
                return@launch
            }
            repository.upsertDownload(entity)
            BrowserKitLog.d(
                "Enqueue",
                "db saved taskId=$taskId status=${entity.status} stream=${entity.streamType} percent=${entity.percent}",
            )
            refresh()
            // Show notification immediately (before worker may start).
            BrowserDownloadNotifier.showStarted(appContext, taskId, info.title)
            val workerId = UUID.randomUUID()
            val actualRequest = when (format.streamType) {
                StreamType.FACEBOOK_YTDLP -> OneTimeWorkRequestBuilder<BrowserFacebookDownloadWorker>()
                StreamType.HLS_M3U8 -> OneTimeWorkRequestBuilder<BrowserHlsDownloadWorker>()
                StreamType.DASH_MPD -> OneTimeWorkRequestBuilder<BrowserMpdDownloadWorker>()
                else -> OneTimeWorkRequestBuilder<BrowserProgressiveDownloadWorker>()
            }.setInputData(buildWorkerInput(entity))
                .addTag(WORK_TAG)
                .addTag(taskId)
                .setId(workerId)
                .build()

            repository.upsertDownload(entity.copy(workerId = workerId.toString()))
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                taskId,
                ExistingWorkPolicy.REPLACE,
                actualRequest,
            )
            BrowserKitLog.i(
                "Enqueue",
                "WorkManager enqueued taskId=$taskId workerId=$workerId request=${actualRequest.workSpec.workerClassName}",
            )
            refresh()
            BrowserKit.notifyBridge(entity.toTask())
            BrowserKit.analytics()?.onBrowserDownloadStart(
                site = site,
                pageUrl = info.pageUrl,
                method = format.streamType.name.lowercase(),
            )
        }
    }

    fun cancel(taskId: String) {
        if (!::appContext.isInitialized) return
        BrowserKitLog.i("Cancel", "taskId=$taskId")
        WorkManager.getInstance(appContext).cancelUniqueWork(taskId)
        BrowserDownloadNotifier.showCancelled(appContext, taskId)
        scope.launch {
            repository.deleteDownload(taskId)
            refresh()
        }
    }

    /**
     * Triple route (never Android DownloadManager):
     * - isDetectedByAvd + HLS/DASH → Avd segment worker
     * - FACEBOOK / legacy HLS/DASH / page extract → yt-dlp
     * - isRegularDownload / progressive → CustomRegular
     */
    private fun tryEnqueueViaHost(
        info: DetectedVideoInfo,
        format: StreamFormat,
        headers: Map<String, String>,
    ): Boolean {
        val bridge = BrowserKit.getDownloadBridge() ?: return false
        val facebook = format.streamType == StreamType.FACEBOOK_YTDLP
        // Avd HLS concat → .ts often unplayable for DM (separate audio + wrong MIME).
        // Always remux via yt-dlp using the watch page URL.
        val dailymotion = DailymotionUrlUtils.isDailymotionVideoPage(info.pageUrl) ||
            DailymotionUrlUtils.isDailymotionMediaUrl(format.url)
        val useAvd = !dailymotion &&
            info.isDetectedByAvd &&
            (format.streamType == StreamType.HLS_M3U8 || format.streamType == StreamType.DASH_MPD) &&
            !facebook
        val useYtdlp = when {
            dailymotion -> true
            useAvd -> false
            facebook -> true
            !info.isRegularDownload -> true
            format.streamType == StreamType.HLS_M3U8 -> true
            format.streamType == StreamType.DASH_MPD -> true
            else -> false
        }
        val downloadUrl = when {
            dailymotion && DailymotionUrlUtils.isDailymotionVideoPage(info.pageUrl) -> info.pageUrl
            else -> format.url
        }
        val dmHeaders = if (dailymotion) {
            headers + mapOf(
                "Referer" to DailymotionUrlUtils.REFERER,
                "Origin" to "https://www.dailymotion.com",
            )
        } else {
            headers
        }
        // HLS Avd: [format.url] is media playlist; MPD Avd: master .mpd URL.
        val request = BrowserHostDownloadRequest(
                title = info.title,
                pageUrl = info.pageUrl,
                downloadUrl = downloadUrl,
                qualityLabel = format.label,
                streamType = if (dailymotion) StreamType.HLS_M3U8.name else format.streamType.name,
                headers = dmHeaders,
                useYtdlp = useYtdlp,
                facebookMode = facebook,
                useAvd = useAvd,
                formatId = format.formatId,
                audioUrl = format.audioUrl,
                isLive = info.isLive,
        )
        BrowserKitLog.i(
            "Enqueue.Route",
            "host request ytdlp=$useYtdlp avd=$useAvd facebook=$facebook dailymotion=$dailymotion " +
                "stream=${request.streamType} formatId=${request.formatId} audio=${request.audioUrl != null} " +
                "headers=${request.headers.keys} url=${BrowserKitLog.shortUrl(request.downloadUrl)}",
        )
        val accepted = bridge.enqueueHostDownload(request)
        BrowserKitLog.i("Enqueue.Route", "host accepted=$accepted")
        return accepted
    }

    fun reportProgress(taskId: String, percent: Int) {
        if (!::repository.isInitialized) return
        scope.launch {
            updateProgress(taskId, percent.coerceIn(0, 100), BrowserDownloadStatus.DOWNLOADING)
        }
    }

    internal suspend fun updateProgress(taskId: String, percent: Int, status: BrowserDownloadStatus) {
        val entity = repository.getDownload(taskId) ?: return
        val oldPercent = entity.percent
        val oldStatus = entity.status
        val updated = entity.copy(
            percent = percent,
            status = status.name,
            updatedAt = System.currentTimeMillis(),
        )
        if (oldPercent != percent || oldStatus != status.name) {
            BrowserKitLog.d(
                "Progress",
                "taskId=$taskId $oldStatus/$oldPercent -> ${status.name}/$percent " +
                    "url=${BrowserKitLog.shortUrl(entity.downloadUrl)}",
            )
        }
        repository.upsertDownload(updated)
        refresh()
        BrowserKit.notifyBridge(updated.toTask())
    }

    internal suspend fun markCompleted(taskId: String, filePath: String) {
        val entity = repository.getDownload(taskId) ?: return
        BrowserKitLog.i("Complete", "taskId=$taskId path=$filePath")
        val updated = entity.copy(
            percent = 100,
            status = BrowserDownloadStatus.COMPLETED.name,
            filePath = filePath,
            updatedAt = System.currentTimeMillis(),
        )
        repository.upsertDownload(updated)
        refresh()
        BrowserKit.notifyBridgeCompleted(updated.toTask())
        val site = com.avd.browserkit.util.BrowserSiteUtils.siteNameFromUrl(
            entity.pageUrl.ifBlank { entity.downloadUrl },
        )
        BrowserKit.analytics()?.onBrowserDownloadSuccess(site = site, pageUrl = entity.pageUrl)
    }

    internal suspend fun markFailed(taskId: String, message: String) {
        val entity = repository.getDownload(taskId) ?: return
        BrowserKitLog.e("Fail", "taskId=$taskId msg=$message")
        val updated = entity.copy(
            status = BrowserDownloadStatus.FAILED.name,
            updatedAt = System.currentTimeMillis(),
        )
        repository.upsertDownload(updated)
        refresh()
        BrowserKit.notifyBridgeFailed(taskId, message)
        val site = com.avd.browserkit.util.BrowserSiteUtils.siteNameFromUrl(
            entity.pageUrl.ifBlank { entity.downloadUrl },
        )
        BrowserKit.analytics()?.onBrowserDownloadFailed(
            site = site,
            pageUrl = entity.pageUrl,
            reason = message,
        )
    }

    private fun buildWorkerInput(entity: BrowserDownloadEntity): Data {
        return Data.Builder()
            .putString(BrowserDownloadWorker.KEY_TASK_ID, entity.id)
            .putString(BrowserDownloadWorker.KEY_TITLE, entity.title)
            .putString(BrowserDownloadWorker.KEY_PAGE_URL, entity.pageUrl)
            .putString(BrowserDownloadWorker.KEY_DOWNLOAD_URL, entity.downloadUrl)
            .putString(BrowserDownloadWorker.KEY_QUALITY, entity.qualityLabel)
            .putString(BrowserDownloadWorker.KEY_STREAM_TYPE, entity.streamType)
            .putString(BrowserDownloadWorker.KEY_HEADERS_JSON, entity.headersJson)
            .build()
    }

    private fun BrowserDownloadEntity.toTask(): BrowserDownloadTask {
        return BrowserDownloadTask(
            id = id,
            title = title,
            pageUrl = pageUrl,
            downloadUrl = downloadUrl,
            qualityLabel = qualityLabel,
            streamType = streamType,
            headers = BrowserRepository(appContext).headersFromJson(headersJson),
            percent = percent,
            status = runCatching { BrowserDownloadStatus.valueOf(status) }
                .getOrDefault(BrowserDownloadStatus.QUEUED),
            filePath = filePath,
            workerId = workerId,
        )
    }
}
