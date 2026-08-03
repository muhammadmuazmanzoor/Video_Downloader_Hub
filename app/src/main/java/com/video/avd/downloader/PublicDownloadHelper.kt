package com.video.avd.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.util.Locale

object PublicDownloadHelper {
    private const val DEFAULT_TITLE = "browser_video"
    private const val BROWSER_VIDEO_DIR = "All Video Downloader/Browser"

    fun displayFileNameForTask(title: String, taskId: String, extension: String = "mp4"): String {
        val cleanedTitle = title.trim()
            .ifBlank { "${DEFAULT_TITLE}_$taskId" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.')
            .ifBlank { "${DEFAULT_TITLE}_$taskId" }
        val suffix = ".$extension"
        return if (cleanedTitle.lowercase(Locale.US).endsWith(suffix)) cleanedTitle else "$cleanedTitle$suffix"
    }

    fun insertPendingVideo(context: Context, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/" + BROWSER_VIDEO_DIR,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")
    }
    fun markVideoComplete(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        context.contentResolver.update(uri, values, null, null)
    }

    fun deleteEntry(context: Context, uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }
}
