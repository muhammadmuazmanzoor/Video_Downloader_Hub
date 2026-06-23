package com.video.avd.ui.file_manager

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.repo.Repository
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import javax.inject.Inject


@HiltViewModel
class FileManagerDirectoriesViewModel @Inject constructor(private val repository: Repository) :
    ViewModel() {

    private val _isVideoForDelete = MutableLiveData<Boolean?>()
    var isVideoForDelete: LiveData<Boolean?> = _isVideoForDelete

    private val _isAudioForDelete = MutableLiveData<Boolean?>()
    var isAudioForDelete: LiveData<Boolean?> = _isAudioForDelete


    private val _permissionNeededForDeleteVideo = MutableLiveData<IntentSender?>()
    val permissionNeededForDeleteVideo: LiveData<IntentSender?> = _permissionNeededForDeleteVideo

    private val _permissionNeededForDeleteAudio = MutableLiveData<IntentSender?>()
    val permissionNeededForDeleteAudio: LiveData<IntentSender?> = _permissionNeededForDeleteAudio

    var newUri: Uri? = null

    private val _allItems = MutableLiveData<List<MediaResources>>()
    val allItems: LiveData<List<MediaResources>> = _allItems


//    fun loadData(directoryPath: String, contentResolver: ContentResolver) {
//        viewModelScope.launch {
//            val directories = listDirectories(File(directoryPath)).first().map { MediaResources.DirectoryItems(it) }
//            val videos = getVideosFromDirectory(contentResolver,directoryPath).first().map { MediaResources.VideoItems(it) }
//            val audios = getAudiosFromDirectory(contentResolver,directoryPath).first().map { MediaResources.AudioItems(it) }
//
//            val combinedList = mutableListOf<MediaResources>().apply {
//                addAll(directories)
//                addAll(videos)
//                addAll(audios)
//            }
//
//            _allItems.postValue(combinedList)
//        }
//    }


    fun loadData(directoryPath: String, contentResolver: ContentResolver) {
        viewModelScope.launch {
            val directoriesFlow = listDirectories(File(directoryPath))
            val videosFlow = getVideosFromDirectory(contentResolver, directoryPath)

            val directories = directoriesFlow.firstOrNull()?.map { MediaResources.DirectoryItems(it) } ?: emptyList()
            val videos = videosFlow.firstOrNull()?.map { MediaResources.VideoItems(it) } ?: emptyList()

            val combinedList = mutableListOf<MediaResources>().apply {
                addAll(directories)
                addAll(videos)
            }
            _allItems.postValue(combinedList)
        }
    }

    private fun listDirectories(root: File): Flow<List<DirectoryModel>> = flow {
        val files = root.listFiles() ?: return@flow
        val directoryFlowList = files
            .filter { it.isDirectory }
            .map { file -> viewModelScope.async { processFile(file) } }
            .toList()

        val directories = directoryFlowList.awaitAll().filterNotNull()
        emit(directories)
    }.flowOn(Dispatchers.IO)

    private suspend fun processFile(file: File): DirectoryModel? {
        return try {
            withContext(Dispatchers.IO) {

                var attrs : BasicFileAttributes?=null
                var creationTime : Instant? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                    creationTime = attrs.creationTime().toInstant()
                }else{
                    creationTime = null
                }

                val subFolderCount = countFolders(file)
                val directoryCount = if (subFolderCount == 0) "Directory is empty" else "$subFolderCount"
                val subFolderCountWithoutHidden = countNonHiddenFolders(file)
                val directoryCountWithoutHidden = if (subFolderCountWithoutHidden == 0) "Directory is empty" else "$subFolderCountWithoutHidden"
                val (videos, audios) = countVideosAndAudios(file)

                DirectoryModel(
                    file.name,
                    directoryCount,
                    file.absolutePath,
                    createdDate = creationTime.toString(),
                    folderSize = "",
                    videoCount = videos.toString(),
                    audioCount = audios.toString(),
                    subFolderCountWithoutHidden = directoryCountWithoutHidden
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun countFolders(file: File): Int {
        var subFolderCount = 0
        try {
            file.listFiles()?.forEach { subFile ->
                if (subFile.isDirectory) subFolderCount++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return subFolderCount
    }
    private fun countNonHiddenFolders(file: File): Int {
        var subFolderCount = 0
        try {
            file.listFiles()?.forEach { subFile ->
                // Check if it's a directory and the name does not start with "."
                if (subFile.isDirectory && !subFile.name.startsWith(".")) {
                    subFolderCount++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return subFolderCount
    }


    private fun getVideosFromDirectory(
        contentResolver: ContentResolver,
        subDirectoryPath: String
    ): Flow<List<Video>> = flow {
        try {
            val videoList = mutableListOf<Video>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )
            val directoryPath =
                if (subDirectoryPath.endsWith(File.separator)) subDirectoryPath else "$subDirectoryPath${File.separator}"
            // Refine the selection to get only direct children of the specified directory
            val selection =
                "${MediaStore.Video.Media.DATA} LIKE ? AND NOT ${MediaStore.Video.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("$directoryPath%", "$directoryPath%${File.separator}%")
            val cursor: Cursor? = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn)
                    val duration = it.getLong(durationColumn)
                    val size = it.getLong(sizeColumn)
                    val dateAdded = it.getLong(dateAddedColumn)
                    val contentUri: Uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    videoList.add(
                        Video(
                            contentUri = contentUri.toString(),
                            title = title,
                            duration = duration.toString(),
                            size = size.toString(),
                            date = dateAdded.toString()
                        )
                    )
                }
            }

            emit(videoList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.flowOn(Dispatchers.IO)



    private fun countVideosAndAudios(file: File): Pair<Int, Int> {
        var videos = 0
        var audios = 0
        try {
            file.listFiles()?.forEach { subFile ->
                if (subFile.isFile && isVideoFile(subFile)) videos++
                else if (subFile.isFile && isAudioFile(subFile)) audios++
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(videos, audios)
        }
        return Pair(videos, audios)
    }


    private fun isVideoFile(file: File): Boolean {
        try {
            // val audioExtensions = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg")
            val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "flv", "wmv", "ts")
            val extension = file.extension.lowercase()
            return extension in videoExtensions
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun isAudioFile(file: File): Boolean {
        try {
            val audioExtensions = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg")
            // val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "flv", "wmv", "ts")
            val extension = file.extension.lowercase()
            return extension in audioExtensions
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    suspend fun deleteVideo(uri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore API to delete files on Android Q and above
                    val deleted = AppUtils.deleteVideoFile(context, uri)
                    if (deleted) {
                        _isVideoForDelete.postValue(true)
                    } else {
                        // Handle deletion failure, if needed
                    }
                } else {
                    // Use File API to delete files below Android Q
                    val file = AppUtils.getPathFromUri(context, uri)?.let { File(it) }
                    if (file?.exists() == true) {
                        if (file.delete()) {
                                // Use MediaScannerConnection API to notify the system about the deletion
                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(file.path),
                                    null
                                ) { _, _ ->
                                    // Do something after the scan is complete, if needed
                                    _isVideoForDelete.postValue(true)
                                }
                            Log.d("delete", "deleted")
                        } else {
                            // Handle deletion failure, if needed
                        }
                    } else {
                        Log.d("delete", "not deleted")
                    }
                }
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException

                    newUri = uri
                    _permissionNeededForDeleteVideo.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                } else {
                }
            }
        }
    }





    fun deleteVideoFromDB(title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.localRepo.videosDao().deleteData(title)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    suspend fun deleteSong(uri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val deleted = context.contentResolver.delete(uri, null, null)
                        if (deleted > 0) {
                            _isAudioForDelete.postValue(true)
                        } else {

                        }
                    } catch (securityException: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val recoverableSecurityException =
                                securityException as? RecoverableSecurityException
                            newUri = uri
                            _permissionNeededForDeleteAudio.postValue(
                                recoverableSecurityException?.userAction?.actionIntent?.intentSender
                            )
                        } else {
                        }
                    }

                } else {
                    try {
                        val file = AppUtils.getAudioPathFRomUri(context, uri)?.let { File(it) }
                        if (file?.exists() == true) {
                            if (file.delete()) {
                                MediaScannerConnection.scanFile(
                                    context, arrayOf(file.absolutePath),
                                    null
                                ) { _, _ ->
                                    // Do something after the scan is complete, if needed
                                    _isAudioForDelete.postValue(true)
                                }
                            }
                            Log.d("delete", "deleted")
                        } else {
                            Log.d("delete", "not deleted")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                    newUri = uri
                    _permissionNeededForDeleteAudio.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                } else {
                }
            }
        }

    }

    fun shareVideo(mActivity: Context, videoFile: File) {
        try {
            if (videoFile.exists()) {
                mActivity.let {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "video/mp4"
                    val videoUri: Uri = FileProvider.getUriForFile(
                        it,
                        "video.player.videodownloader.storysaver.provider",
                        videoFile
                    )
                    shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ContextCompat.startActivity(
                        it,
                        Intent.createChooser(shareIntent, "Share video"),
                        null
                    )
                }
            } else {
                Toast.makeText(mActivity, "File does not exist", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun createNewFolder(folderName: String, path : String): String {
        // Check if external storage is available
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return "please try again"
        }
        val folder = File(path, folderName)
        // Create the folder if it doesn't exist
        if (!folder.exists()) {
            folder.mkdirs()
            return "folder created successfully"
        } else {
            return "folder already exist"
        }
    }

    suspend fun sortDirectoryList(sortType: Int, list: List<MediaResources>): List<MediaResources> = withContext(Dispatchers.IO) {
        try {
            // Sort each type individually
            val sortedDirectories = list.filterIsInstance<MediaResources.DirectoryItems>()
                .let { sortDirectories(it, sortType) }

            val sortedVideos = list.filterIsInstance<MediaResources.VideoItems>()
                .let { sortVideos(it, sortType) }



            // Merge the sorted lists while maintaining original order
            list.map { item ->
                when (item) {
                    is MediaResources.DirectoryItems -> sortedDirectories.find { it == item } ?: item
                    is MediaResources.VideoItems -> sortedVideos.find { it == item } ?: item
                }
            }
        } catch (e: Exception) {
            // Log the error for debugging
            e.printStackTrace()
            list
        }
    }
    fun sortDirectories(dirList: List<MediaResources.DirectoryItems>, sortType: Int): List<MediaResources.DirectoryItems> {
                   return when (sortType) {
                0 -> dirList.sortedBy { it.item.name } // Name A to Z
                1 -> dirList.sortedByDescending { it.item.name } // Name Z to A
                2 -> dirList.sortedByDescending { it.item.createdDate } // Date New to Old
                3 -> dirList.sortedBy { it.item.createdDate } // Date Old to New
                4 -> dirList.sortedByDescending { it.item.folderSize } // Size Big to Small
                5 -> dirList.sortedBy { it.item.folderSize } // Size Small to Big
                else -> dirList // Default order (0: Name A to Z)
            }
    }

    fun sortVideos(dirList: List<MediaResources.VideoItems>, sortType: Int): List<MediaResources.VideoItems> {
        return when (sortType) {
            0 -> dirList.sortedBy { it.item.title } // Name A to Z
            1 -> dirList.sortedByDescending { it.item.title } // Name Z to A
            2 -> dirList.sortedByDescending { it.item.date } // Date New to Old
            3 -> dirList.sortedBy { it.item.date } // Date Old to New
//            4 -> dirList.sortedByDescending { it.item.folderSize } // Size Big to Small
//            5 -> dirList.sortedBy { it.item.folderSize } // Size Small to Big
            else -> dirList // Default order (0: Name A to Z)
        }
    }



    fun convertVideoItemsToVideo(videoItem: MediaResources.VideoItems): Video {
        return Video(
            id = videoItem.item.id,
            contentUri = videoItem.item.contentUri,
            title = videoItem.item.title,
            duration = videoItem.item.duration,
            date = videoItem.item.date,
            size = videoItem.item.size,
            orignalpath = videoItem.item.orignalpath,
            isChecked = videoItem.item.isChecked,
            lastPlayed = videoItem.item.lastPlayed,
            folderid = videoItem.item.folderid,
            timeStump = videoItem.item.timeStump,
            updatedTimeStump = videoItem.item.updatedTimeStump,
            isRecent = videoItem.item.isRecent,
            playedCompletely = videoItem.item.playedCompletely,
            playedOver90Percent = videoItem.item.playedOver90Percent,
            playedPercentage = videoItem.item.playedPercentage
        )
    }



}