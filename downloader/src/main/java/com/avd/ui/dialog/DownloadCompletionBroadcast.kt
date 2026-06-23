package com.avd.ui.dialog

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.repository.ProgressRepository
import com.avd.util.FileUtil
import com.avd.util.downloaders.generic_downloader.models.VideoTaskItem
import java.io.File

object DownloadCompletionBroadcast {
    const val ACTION = "DOWNLOAD_COMPLETE"
    const val EXTRA_VIDEO_NAME = "videoName"
    const val EXTRA_VIDEO_TITLE = "videoTitle"
    const val EXTRA_THUMBNAIL = "thumbnail"
    const val EXTRA_DURATION = "duration"
    const val EXTRA_FILE_SIZE = "fileSize"
    const val EXTRA_FILE_PATH = "filePath"
    const val EXTRA_PLAY_URI = "playUri"
    const val EXTRA_DOWNLOAD_ID = "downloadId"

    private const val TAG = "TESTdialogue"

    fun buildFromProgressInfo(progressInfo: ProgressInfo, fileUtil: FileUtil): DownloadCompletionInfo {
        val videoInfo = progressInfo.videoInfo
        fileUtil.invalidateListFilesCache()

        val fileName = videoInfo.title
        val playUri = fileUtil.listFiles.entries
            .firstOrNull { it.key.contains(fileName, ignoreCase = true) }
            ?.value?.second?.toString()
            .orEmpty()
            .ifBlank { videoInfo.originalUrl }

        return DownloadCompletionInfo(
            videoTitle = videoInfo.title.ifBlank { "Downloaded Video" },
            thumbnail = videoInfo.thumbnail,
            duration = videoInfo.duration,
            fileSize = progressInfo.progressTotal.takeIf { it > 0 } ?: progressInfo.progressDownloaded,
            videoName = videoInfo.title,
            filePath = playUri,
            playUri = playUri
        )
    }

    fun send(
        context: Context,
        progressRepository: ProgressRepository,
        taskId: String,
        item: VideoTaskItem,
        fileUtil: FileUtil
    ) {
        try {
            val dbTask = progressRepository.getProgressInfos().blockingFirst()
                .find { it.id == taskId }
            val videoInfo = dbTask?.videoInfo

            val fileSize = when {
                item.totalSize > 0 -> item.totalSize
                (dbTask?.progressTotal ?: 0L) > 0 -> dbTask?.progressTotal ?: 0L
                item.downloadSize > 0 -> item.downloadSize
                else -> dbTask?.progressDownloaded ?: 0L
            }

            val title = videoInfo?.title?.takeIf { it.isNotBlank() }
                ?: item.title?.takeIf { it.isNotBlank() }
                ?: item.fileName
                ?: "Downloaded Video"

            val thumbnail = videoInfo?.thumbnail?.takeIf { it.isNotBlank() }
                ?: item.coverUrl
                ?: ""

            fileUtil.invalidateListFilesCache()
            val playUri = resolvePlayUri(fileUtil, item, videoInfo)

            val intent = Intent(ACTION).apply {
                putExtra(EXTRA_VIDEO_NAME, item.fileName ?: "Downloaded Video")
                putExtra(EXTRA_VIDEO_TITLE, title)
                putExtra(EXTRA_THUMBNAIL, thumbnail)
                putExtra(EXTRA_DURATION, videoInfo?.duration ?: 0L)
                putExtra(EXTRA_FILE_SIZE, fileSize)
                putExtra(EXTRA_FILE_PATH, item.filePath ?: "")
                putExtra(EXTRA_PLAY_URI, playUri)
                putExtra(EXTRA_DOWNLOAD_ID, taskId)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Broadcast sent: title=$title, playUri=$playUri, size=$fileSize")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending download completion broadcast: ${e.message}")
        }
    }

    private fun resolvePlayUri(
        fileUtil: FileUtil,
        item: VideoTaskItem,
        videoInfo: VideoInfo?
    ): String {
        val fileName = item.fileName?.takeIf { it.isNotBlank() }
        if (fileName != null) {
            fileUtil.listFiles[fileName]?.second?.toString()?.let { return it }
            val fileInFolder = File(fileUtil.folderDir, fileName)
            if (fileInFolder.exists()) {
                return fileInFolder.toUri().toString()
            }
        }

        item.filePath?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("content://") || path.startsWith("file://")) {
                return path
            }
            val file = File(path)
            if (file.exists()) {
                return file.toUri().toString()
            }
        }

        return videoInfo?.originalUrl.orEmpty()
    }
}
