package com.avd.ui.main.progress


import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.local.room.entity.VideFormatEntityList
import com.avd.data.repository.ProgressRepository
import com.avd.browserkit.api.BrowserDownloadSharedStore
import com.avd.browserkit.api.BrowserSharedDownloadTask
import com.avd.ui.main.base.BaseViewModel
import com.avd.util.ContextUtils
import com.avd.util.FileUtil
import com.avd.util.NetworkUtils
import com.avd.util.downloaders.custom_downloader_service.CustomRegularDownloader
import com.avd.util.downloaders.generic_downloader.models.VideoTaskState
import com.avd.util.downloaders.youtubedl_downloader.YoutubeDlDownloader
import androidx.work.WorkManager
import com.video.avd.downloader.BrowserHostDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val fileUtil: FileUtil,
    private val progressRepository: ProgressRepository,
) : BaseViewModel() {
    @VisibleForTesting
    internal val compositeDisposable: CompositeDisposable = CompositeDisposable()

    var progressInfos: ObservableField<List<ProgressInfo>> = ObservableField(emptyList())
    val isLoadingProgress = ObservableField(true)
    private val executor = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    private val executor2 = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private var progressListenJob: Job? = null

    override fun start() {
        if (progressListenJob?.isActive == true) return
        isLoadingProgress.set(progressInfos.get().isNullOrEmpty())
        progressListenJob = downloadProgressStartListen()
    }

    override fun stop() {
        progressListenJob?.cancel()
        progressListenJob = null
        compositeDisposable.clear()
    }

    // TODO: strange, should fix
    fun stopAndSaveDownload(id: Long) {
        val inf = progressInfos.get()?.find { it.downloadId == id }

        if (inf?.videoInfo?.isRegularDownload == false) {
            inf.let {
                YoutubeDlDownloader.stopAndSaveDownload(
                    ContextUtils.getApplicationContext(), it
                )
            }
        } else {
            // TODO For Regular
        }
    }

    fun cancelDownload(id: Long, removeFile: Boolean) {
        val inf = progressInfos.get()?.find { it.downloadId == id }
        if (inf != null && isBrowserSharedTask(inf)) {
            val taskId = inf.id.removePrefix(BROWSER_ID_PREFIX)
            WorkManager.getInstance(ContextUtils.getApplicationContext()).cancelUniqueWork("browser_host_$taskId")
            BrowserDownloadSharedStore.remove(taskId)
            progressInfos.set(progressInfos.get()?.filterNot { it.id == inf.id }?.sortedBy { it.id })
            return
        }
        inf?.let { progressInfo ->
            deleteProgressInfo(progressInfo) { info ->
                if (info.videoInfo.isRegularDownload) {
                    CustomRegularDownloader.cancelDownload(
                        ContextUtils.getApplicationContext(),
                        inf,
                        removeFile
                    )
                } else {
                    info.let {
                        YoutubeDlDownloader.cancelDownload(
                            ContextUtils.getApplicationContext(), it, removeFile
                        )
                    }
                }
                val newList = progressInfos.get()?.filter { it.id != info.id }
                progressInfos.set(newList?.sortedBy { it.id })
            }
        }
    }

    fun pauseDownload(id: Long) {
        val inf = progressInfos.get()?.find { it.downloadId == id }
        if (inf != null && isBrowserSharedTask(inf)) {
            val taskId = inf.id.removePrefix(BROWSER_ID_PREFIX)
            WorkManager.getInstance(ContextUtils.getApplicationContext()).cancelUniqueWork("browser_host_$taskId")
            BrowserDownloadSharedStore.update(
                taskId = taskId,
                title = inf.videoInfo.title,
                pageUrl = inf.videoInfo.originalUrl,
                percent = inf.progressDownloaded.toInt().coerceIn(0, 100),
                status = com.avd.browserkit.download.BrowserDownloadStatus.PAUSED,
            )
            return
        }

        if (inf?.videoInfo?.isRegularDownload == true) {
            CustomRegularDownloader.pauseDownload(ContextUtils.getApplicationContext(), inf)
        } else {
            val updated = inf?.copy(downloadStatus = VideoTaskState.PAUSE)
            if (updated != null) {
                saveProgressInfo(updated) { info ->
                    YoutubeDlDownloader.pauseDownload(ContextUtils.getApplicationContext(), info)
                }
            }
        }
    }

    fun resumeDownload(id: Long) {
        try {
            val inf = progressInfos.get()?.find { it.downloadId == id }
            if (inf != null && isBrowserSharedTask(inf)) {
                val taskId = inf.id.removePrefix(BROWSER_ID_PREFIX)
                val task = BrowserDownloadSharedStore.tasks.value.firstOrNull { it.taskId == taskId }
                if (task == null) {
                    Toast.makeText(ContextUtils.getApplicationContext(), "Browser download can not be resumed", Toast.LENGTH_SHORT).show()
                    return
                }
                val restarted = BrowserHostDownloader.restart(ContextUtils.getApplicationContext(), task)
                if (!restarted) {
                    Toast.makeText(ContextUtils.getApplicationContext(), "Failed to restart browser download", Toast.LENGTH_SHORT).show()
                }
                return
            }
            if (inf?.videoInfo?.isRegularDownload == true) {
                CustomRegularDownloader.resumeDownload(ContextUtils.getApplicationContext(), inf)
            } else {
                inf?.let {
                    val updated = inf.copy(downloadStatus = VideoTaskState.PREPARE)
                    saveProgressInfo(updated) { info ->
                        YoutubeDlDownloader.resumeDownload(ContextUtils.getApplicationContext(), info)
                    }
                }
            }
        }catch (e:IllegalStateException){
            e.printStackTrace()
        }
    }

    fun downloadVideo(videoInfo: VideoInfo?) {
        val context = ContextUtils.getApplicationContext()
        if (NetworkUtils.isOnline(context)){
            videoInfo?.let {
                if (!fileUtil.folderDir.exists() && !fileUtil.folderDir.mkdirs()) {
                    return
                }
                val downloadId = videoInfo.id.hashCode().toLong()
                val progressInfo = ProgressInfo(id = videoInfo.id, downloadId = downloadId, videoInfo = videoInfo, isM3u8 = videoInfo.isM3u8)
                addProgressInfoToList(progressInfo)
                saveProgressInfo(progressInfo) { info ->
                    if (info.videoInfo.isRegularDownload) {
                        CustomRegularDownloader.addDownload(context, info.videoInfo)
                        Log.d("startDownload","CustomRegularDownloader")
                    } else {
                        YoutubeDlDownloader.startDownload(context, info.videoInfo)
                        Log.d("startDownload","YoutubeDlDownloader")
                    }
                }
            }
        }else{
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadVideo(videoInfo: VideoInfo?,isRegularDownload:Boolean?=false,pos:Int=0) {
        Log.d("startDownload","CustomRegularDownloader1 ${videoInfo}")

        val context = ContextUtils.getApplicationContext()
        videoInfo?.let {
            if (!fileUtil.folderDir.exists() && !fileUtil.folderDir.mkdirs()) {
                return
            }
            val downloadId = videoInfo.id.hashCode().toLong()
            val progressInfo = ProgressInfo(id = videoInfo.id, downloadId = downloadId, videoInfo = videoInfo, isM3u8 = videoInfo.isM3u8)
            addProgressInfoToList(progressInfo)
            saveProgressInfo(progressInfo) { info ->
                if (info.videoInfo.isRegularDownload || isRegularDownload==true) {
                    CustomRegularDownloader.addDownload(context, info.videoInfo,true,pos)
                    Log.d("startDownload","CustomRegularDownloader1 ${info.videoInfo.originalUrl}")
                } else {
                    val selectedVideoInfo = info.videoInfo.withSelectedFormatFirst(pos)
                    YoutubeDlDownloader.startDownload(context, selectedVideoInfo)
                    Log.d("startDownload","YoutubeDlDownloader1 ${selectedVideoInfo.originalUrl}")
                }
            }
        }
    }

    private fun VideoInfo.withSelectedFormatFirst(position: Int): VideoInfo {
        val formatsList = formats.formats
        val selectedFormat = formatsList.getOrNull(position) ?: return this
        return copy(
            formats = VideFormatEntityList(listOf(selectedFormat) + formatsList.filterIndexed { index, _ ->
                index != position
            })
        )
    }

    private fun addProgressInfoToList(progressInfo: ProgressInfo) {
        val updatedList = progressInfos.get()
            .orEmpty()
            .filterNot { it.id == progressInfo.id }
            .plus(progressInfo)
            .sortedBy { it.id }

        progressInfos.set(updatedList)
        isLoadingProgress.set(false)
    }

    private fun saveProgressInfo(progressInfo: ProgressInfo, onSuccess: (ProgressInfo) -> Unit = {}) {
        viewModelScope.launch(executor2) {
            progressRepository.saveProgressInfo(progressInfo)
            onSuccess(progressInfo)
        }
    }

    private fun deleteProgressInfo(progressInfo: ProgressInfo, onSuccess: (ProgressInfo) -> Unit = {}) {
        viewModelScope.launch(executor2) {
            progressRepository.deleteProgressInfo(progressInfo)
            onSuccess(progressInfo)
        }
    }

    @VisibleForTesting
    internal fun downloadProgressStartListen(): Job {
        return viewModelScope.launch(executor) {
            progressObservable().doOnError {
                it.printStackTrace()
            }.blockingForEach { progressInfoList ->
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    progressInfos.set(progressInfoList.sortedBy { it.id })
                    isLoadingProgress.set(false)
                }
            }
        }
    }

    private fun progressObservable(): Observable<List<ProgressInfo>> {
        val youtubeDlDownloads = Observable.interval(0, 300, TimeUnit.MILLISECONDS).flatMap {
            progressRepository.getProgressInfos().take(1).flatMap {
                val filtered = it.filter { info -> info.downloadStatus != VideoTaskState.SUCCESS }
                // Don't TOUCH(если убрать это возникнет конфликт ID-ков и не будет показываться прогресс для обычных загрузок)
                //////////////////////////////
                val successed = it.filter { info -> info.downloadStatus == VideoTaskState.SUCCESS }
                for (task in successed) {
                    progressRepository.deleteProgressInfo(task)
                }
                /////////////////////////////
                Observable.just(filtered).toFlowable(BackpressureStrategy.LATEST).take(1)
            }.toObservable().doOnError { error ->
                error.printStackTrace()
            }
        }
        return youtubeDlDownloads.map { dbItems ->
            mergeBrowserkitTasks(dbItems)
        }
    }

    private fun mergeBrowserkitTasks(dbItems: List<ProgressInfo>): List<ProgressInfo> {
        val browserItems = BrowserDownloadSharedStore.tasks.value
            .filter { it.status != com.avd.browserkit.download.BrowserDownloadStatus.COMPLETED }
            .map { it.toProgressInfo() }
        return (dbItems + browserItems)
            .distinctBy { it.id }
            .sortedBy { it.id }
    }

    private fun BrowserSharedDownloadTask.toProgressInfo(): ProgressInfo {
        val infoId = "$BROWSER_ID_PREFIX$taskId"
        val safeUrl = downloadUrl.ifBlank { pageUrl }
        val videoInfo = VideoInfo(
            id = infoId,
            title = title,
            ext = "mp4",
            originalUrl = safeUrl,
            formats = VideFormatEntityList(emptyList()),
            isRegularDownload = false,
        )
        return ProgressInfo(
            id = infoId,
            downloadId = browserDownloadId(taskId),
            videoInfo = videoInfo,
            progressDownloaded = percent.toLong(),
            progressTotal = 100L,
            downloadStatus = when (status) {
                com.avd.browserkit.download.BrowserDownloadStatus.QUEUED -> VideoTaskState.PENDING
                com.avd.browserkit.download.BrowserDownloadStatus.DOWNLOADING -> VideoTaskState.DOWNLOADING
                com.avd.browserkit.download.BrowserDownloadStatus.PAUSED -> VideoTaskState.PAUSE
                com.avd.browserkit.download.BrowserDownloadStatus.COMPLETED -> VideoTaskState.SUCCESS
                com.avd.browserkit.download.BrowserDownloadStatus.FAILED -> VideoTaskState.ERROR
            },
            isLive = false,
            isM3u8 = safeUrl.contains(".m3u8") || safeUrl.contains(".mpd"),
            infoLine = "browserkit",
        )
    }

    private fun isBrowserSharedTask(info: ProgressInfo): Boolean = info.id.startsWith(BROWSER_ID_PREFIX)

    private fun browserDownloadId(taskId: String): Long = -kotlin.math.abs(taskId.hashCode().toLong()).coerceAtLeast(1L)

    private companion object {
        const val BROWSER_ID_PREFIX = "browser_"
    }

}
