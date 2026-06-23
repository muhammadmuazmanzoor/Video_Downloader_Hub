package com.avd.ui.dialog

import android.os.Bundle

data class DownloadCompletionInfo(
    val videoTitle: String = "Downloaded Video",
    val thumbnail: String = "",
    val duration: Long = 0L,
    val fileSize: Long = 0L,
    val videoName: String = "",
    val filePath: String = "",
    val playUri: String = ""
) {
    fun toBundle(): Bundle = Bundle().apply {
        putString(KEY_VIDEO_TITLE, videoTitle)
        putString(KEY_THUMBNAIL, thumbnail)
        putLong(KEY_DURATION, duration)
        putLong(KEY_FILE_SIZE, fileSize)
        putString(KEY_VIDEO_NAME, videoName)
        putString(KEY_FILE_PATH, filePath)
        putString(KEY_PLAY_URI, playUri)
    }

    companion object {
        private const val KEY_VIDEO_TITLE = "videoTitle"
        private const val KEY_THUMBNAIL = "thumbnail"
        private const val KEY_DURATION = "duration"
        private const val KEY_FILE_SIZE = "fileSize"
        private const val KEY_VIDEO_NAME = "videoName"
        private const val KEY_FILE_PATH = "filePath"
        private const val KEY_PLAY_URI = "playUri"

        fun fromBundle(bundle: Bundle?): DownloadCompletionInfo {
            if (bundle == null) return DownloadCompletionInfo()
            val filePath = bundle.getString(KEY_FILE_PATH).orEmpty()
            val playUri = bundle.getString(KEY_PLAY_URI).orEmpty().ifBlank { filePath }
            return DownloadCompletionInfo(
                videoTitle = bundle.getString(KEY_VIDEO_TITLE).orEmpty()
                    .ifBlank { bundle.getString(KEY_VIDEO_NAME).orEmpty() }
                    .ifBlank { "Downloaded Video" },
                thumbnail = bundle.getString(KEY_THUMBNAIL).orEmpty(),
                duration = bundle.getLong(KEY_DURATION, 0L),
                fileSize = bundle.getLong(KEY_FILE_SIZE, 0L),
                videoName = bundle.getString(KEY_VIDEO_NAME).orEmpty(),
                filePath = filePath,
                playUri = playUri
            )
        }

        fun fromIntentExtras(extras: Bundle?): DownloadCompletionInfo {
            if (extras == null) return DownloadCompletionInfo()
            val filePath = extras.getString(DownloadCompletionBroadcast.EXTRA_FILE_PATH).orEmpty()
            val playUri = extras.getString(DownloadCompletionBroadcast.EXTRA_PLAY_URI)
                .orEmpty()
                .ifBlank { filePath }
            return DownloadCompletionInfo(
                videoTitle = extras.getString(DownloadCompletionBroadcast.EXTRA_VIDEO_TITLE)
                    .orEmpty()
                    .ifBlank { extras.getString(DownloadCompletionBroadcast.EXTRA_VIDEO_NAME).orEmpty() }
                    .ifBlank { "Downloaded Video" },
                thumbnail = extras.getString(DownloadCompletionBroadcast.EXTRA_THUMBNAIL).orEmpty(),
                duration = extras.getLong(DownloadCompletionBroadcast.EXTRA_DURATION, 0L),
                fileSize = extras.getLong(DownloadCompletionBroadcast.EXTRA_FILE_SIZE, 0L),
                videoName = extras.getString(DownloadCompletionBroadcast.EXTRA_VIDEO_NAME).orEmpty(),
                filePath = filePath,
                playUri = playUri
            )
        }
    }
}
