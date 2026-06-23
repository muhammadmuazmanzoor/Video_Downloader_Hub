package com.video.avd.repo

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.video.avd.data.local.AppDatabase
import com.video.avd.ui.folder.model.VideoFolder
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Singleton


const val RECENT_ADDED_FOLDER_ID = -1L

@Singleton
class FolderRepository(local: AppDatabase) {


    val database = local
    private val insertMutex = Mutex()

    fun getVideoFoldersWithCount(context: Context): Flow<List<VideoFolder>> = flow {
        try {
            // Dynamically get the internal storage root path
            val internalStorageRootPath = Environment.getExternalStorageDirectory().absolutePath

            // Projection for MediaStore query
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATA // File path to determine folder structure
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

            val query = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            val folderMap = mutableMapOf<String, VideoFolder>()
            val internalStorageVideos = mutableListOf<Long>()
            var internalStorageBucketId: String? = null
            var internalStorageSize = 0L
            var latestDateAdded = 0L

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val bucketId = cursor.getString(bucketIdColumn)
                    val bucketName = cursor.getString(bucketNameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val filePath = cursor.getString(dataColumn)

                    // Check if the video is in the root of internal storage or has no folder
                    val isInInternalStorageRoot = filePath.startsWith(internalStorageRootPath) &&
                            !filePath.substringAfter("$internalStorageRootPath/").contains('/')

                    if (isInInternalStorageRoot || bucketId == null || bucketName == null) {
                        // Assign a bucketId dynamically from the first valid video
                        if (internalStorageBucketId == null) {
                            internalStorageBucketId = bucketId ?: "-1"
                        }
                        internalStorageVideos.add(id)
                        internalStorageSize += size
                        latestDateAdded = maxOf(latestDateAdded, dateAdded)
                    } else {
                        val folder = folderMap.getOrPut(bucketId) {
                            VideoFolder(bucketId.toLong(), bucketName, mutableListOf()).apply {
                                videoCount = 0
                                this.size = 0
                                this.dateAdded = 0L
                            }
                        }
                        folder.videoIds.add(id)
                        folder.videoCount = folder.videoIds.size
                        folder.size += size // Aggregate size
                        folder.dateAdded = maxOf(folder.dateAdded, dateAdded) // Latest date added
                    }
                }
            }

            // Convert the folder map to a list
            val folderList = folderMap.values.toMutableList()

            // Add the "Internal Storage" folder if applicable
            if (internalStorageVideos.isNotEmpty()) {
                val internalStorageFolder = VideoFolder(
                    id = internalStorageBucketId?.toLong() ?: -1, // Use the dynamic bucketId
                    name = "Internal Storage",
                    videoIds = internalStorageVideos
                ).apply {
                    videoCount = internalStorageVideos.size
                    size = internalStorageSize
                    dateAdded = latestDateAdded
                }
                Log.e("checkFolder", "InternalStorageVideos: $internalStorageVideos")
                // Add "Internal Storage" at the start of the list
                folderList.add(0, internalStorageFolder)
            }

            // Emit the final list
            emit(folderList)
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO).catch { e->
        e.printStackTrace()
    }

    suspend fun updateFoldersWithNewVideos(videos: List<Video>) {
        val groupedVideos = videos.groupBy { it.folderid }
        // Fetch folders that are affected
        val folderIds = groupedVideos.keys.map { it.toLong() }
        val existingFolders = database.folderdao().getFoldersByIds(folderIds)
        // Map existing folders for lookup
        val folderMap = existingFolders.associateBy { it.id }
        val updatedFolders = mutableListOf<VideoFolder>()
        groupedVideos.forEach { (folderId, videosInFolder) ->
            val hasNewVideos = videosInFolder.any { it.isNew }
            val videoCount = videosInFolder.size // Recalculate video count
            val existingFolder = folderMap[folderId.toLong()]
            if (existingFolder != null) {
                var needsUpdate = false
                // Update `hasNewVideos` if necessary
                if (!existingFolder.hasNewVideos && hasNewVideos) {
                    existingFolder.hasNewVideos = true
                    needsUpdate = true
                }
                // Update `videoCount` if it has changed
                if (existingFolder.videoCount != videoCount) {
                    existingFolder.videoCount = videoCount
                    needsUpdate = true
                }
                if (needsUpdate) {
                    updatedFolders.add(existingFolder)
                }
            } else if (hasNewVideos) {
                // Create a new folder if it doesn't exist
                val newFolder = VideoFolder(
                    id = folderId.toLong(),
                    name = videosInFolder.firstOrNull()?.title ?: "Unknown",
                    videoIds = videosInFolder.map { it.id }.toMutableList(),
                    hasNewVideos = true,
                    videoCount = videoCount
                )
                updatedFolders.add(newFolder)
                Log.d("FolderUpdate", "Creating new folder with ID: ${newFolder.id}, hasNewVideos: true, videoCount: $videoCount")
            }
        }

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val currentTime = System.currentTimeMillis()
        val sevenDaysAgo = currentTime - 7 * 24 * 60 * 60 * 1000

        val recentVideosCount = videos.count { video ->
            val videoTimestamp = try {
                dateFormat.parse(video.date)?.time ?: 0L
            } catch (e: Exception) {
                Log.e("DateParsing", "Error parsing date: ${video.date}", e)
                0L
            }
            Log.d("DateCheck", "Video Date: ${video.date}, Parsed Timestamp: $videoTimestamp, SevenDaysAgo: $sevenDaysAgo")
            videoTimestamp >= sevenDaysAgo
        }
        // Check if the "Recent Added" folder exists
        val recentFolder = database.folderdao().getFolderByName("Recent Added")
        if (recentFolder != null) {
            if (recentFolder.videoCount != recentVideosCount) {
                recentFolder.videoCount = recentVideosCount
                updatedFolders.add(recentFolder)
                Log.d("FolderUpdate", "Updating 'Recent Added' folder with videoCount: $recentVideosCount")
            }
        } else {
            // Create the "Recent Added" folder if it doesn't exist
            val newRecentFolder = VideoFolder(
                id = RECENT_ADDED_FOLDER_ID, // Let the database auto-generate the ID
                name = "Recent Added",
                videoCount = recentVideosCount,
                hasNewVideos = false // Mark as new if there are videos
            )
            database.folderdao().insertfolder(newRecentFolder)
            Log.d("FolderUpdate", "Creating 'Recent Added' folder with videoCount: $recentVideosCount")
        }
        if (updatedFolders.isNotEmpty()) {
            Log.d("FolderUpdate", "Updating Folders: ${updatedFolders.map { it.id }}")
            database.folderdao().updateFolders(updatedFolders)
        } else {
            Log.d("FolderUpdate", "No folders to update.")
        }
    }

    suspend fun insetfolderintodb(listoffolder: List<VideoFolder>) {
        insertMutex.withLock {
            val chunkSize = 50
            listoffolder.chunked(chunkSize).forEach { chunk ->
                database.folderdao().insertfolder(chunk)
            }
        }
    }


    @SuppressLint("InlinedApi")
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

}