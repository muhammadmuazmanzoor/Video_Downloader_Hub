package com.video.avd.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun formatVideoSize(sizeInBytes: Int): String {
    val kilobytes = sizeInBytes / 1024
    val megabytes = kilobytes / 1024

    return when {
        megabytes > 0 -> "$megabytes MB"
        kilobytes > 0 -> "$kilobytes KB"
        else -> "$sizeInBytes B"
    }
}

fun getVideoDuration(context: Context, contentUri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, contentUri)
        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        durationString?.toLong()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}


fun formatDuration(duration: Long): String {
    val seconds = (duration / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

fun getVideoResolution(context: Context, videoPath: String): String? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(videoPath)
        val widthString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightString =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val width = widthString?.toIntOrNull()
        val height = heightString?.toIntOrNull()
        if (width != null && height != null) {
            "$width*$height"
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        retriever.release()
    }
}

fun getContentUriForId(context: Context, mediaId: Long): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Use MediaStore API to get the content URI on Android Q and above
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(mediaId.toString())
        context.contentResolver.query(collection, null, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val uri =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    Uri.withAppendedPath(collection, uri)
                } else {
                    null
                }
            }
    } else {
        // Not supported on Android versions below Q
        null
    }
}

