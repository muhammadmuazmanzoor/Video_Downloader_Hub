package com.video.avd.repo

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.video.avd.data.local.AppDatabase
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.ClickEvent
import com.video.avd.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Singleton


@Singleton
class VideoRepository(val local: AppDatabase,val repository: Repository) {

    val localRepo = local
    private val _insertedToFavMsg = SingleLiveEvent<String>()
    val insertedToFavMsg: LiveData<String> = _insertedToFavMsg



    private val _remove = MutableLiveData<ClickEvent<Int>>()
    val remove: LiveData<ClickEvent<Int>> = _remove

    suspend fun isObjectExists(userId: String): Boolean {
        return repository.isObjectExists(userId)
    }


    fun getAllVideos(context: Context): Flow<List<Video>> = flow {
        // Trigger media scan to ensure newly added videos are included in the query
        MediaScannerConnection.scanFile(
            context,
            arrayOf(Environment.getExternalStorageDirectory().toString()),
            null,
            null
        )
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_ID
        )
        val selection = null
        val selectionArgs = null
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val query = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        val videoList = mutableListOf<Video>()
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val folderIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID) // Added
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val duration = cursor.getLong(durationColumn)
                val contentUri =
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val folderId = cursor.getString(folderIdColumn)
                videoList.add(
                    Video(
                        id = id,
                        contentUri = contentUri.toString(),
                        title = title,
                        duration = AppUtils.getDurationString(duration),
                        date = AppUtils.convertLongToDate(cursor.getLong(dateModifiedColumn)),
                        size = AppUtils.formatFileSize(cursor.getLong(sizeColumn)),
                        folderid = folderId
                    )
                )
            }
            emit(videoList)
        }
    }.flowOn(Dispatchers.IO)


    suspend fun updateFolder(folderId: Long) {
        val folder = local.folderdao().getFolderById(folderId)
        folder?.let {
            // Check if the folder still has new videos
            // Step 2: Count remaining new videos in the folder
            val newVideoCount = local.folderdao().countNewVideosInFolder(folderId.toString())

            // Step 3: If no new videos remain, remove `hasNewVideos` flag from the folder
            if (newVideoCount == 0) {
                local.folderdao().setFolderHasNewToFalse(folderId)
            }
        }
    }


}