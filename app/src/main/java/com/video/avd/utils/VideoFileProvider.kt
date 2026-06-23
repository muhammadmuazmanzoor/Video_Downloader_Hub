package com.video.avd.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider


class VideoFileProvider() : FileProvider() {
    fun getFilePath(context: Context, uri: Uri?): String? {
        val cursor = context.contentResolver.query(uri!!, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val filePath = cursor.getString(index)
            cursor.close()
            return filePath
        }
        return null
    }
}
