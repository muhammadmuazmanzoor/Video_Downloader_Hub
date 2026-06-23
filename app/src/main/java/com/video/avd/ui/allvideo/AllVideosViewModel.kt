package com.video.avd.ui.allvideo

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.repo.AllVideoRepository
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.isValidFileName
import com.video.avd.utils.ClickEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AllVideosViewModel @Inject constructor(private val repository: AllVideoRepository, @ApplicationContext private val context: Context) : ViewModel() {

    var urinew: Uri? = null
    var nameNew: String? = null
    var idrename: Long? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    private val _permissionNeededForRename = MutableLiveData<IntentSender?>()
    val permissionNeededForRename: LiveData<IntentSender?> = _permissionNeededForRename
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete
    val totalSizeLiveData = MutableLiveData<String>()
    private val _ForDelete = MutableLiveData<Boolean?>()
    var isForDelete: LiveData<Boolean?> = _ForDelete
    private val _ForRename = MutableLiveData<Boolean?>()
    var isForRename: LiveData<Boolean?> = _ForRename

    val isLoading :MutableLiveData<Boolean> = repository.isLoading



    private val _videoDeletedFromDB = MutableLiveData<ClickEvent<Boolean>>()
    val videoDeletedFromDB: LiveData<ClickEvent<Boolean>> = _videoDeletedFromDB

    val insertedToFavMsg: LiveData<String> = repository.insertedToFavMsg



    val remove: LiveData<ClickEvent<Int>> = repository.remove


    val videosListFlow = repository.getAllVideos(context)

    var videosData = getDataFromDb()

    suspend fun updateUserData(userEntities: Video) {
        withContext(Dispatchers.IO) {
            repository.localRepo.dao().insertEntities(userEntities)
        }
    }

    fun moveFile(
        context: Context,
        path: Uri,
        sourceUri: Uri?,
        destinationFile: File,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (sourceUri != null) {
                    copyFile(context, path, sourceUri, destinationFile, callback)
                } else {
                    // Handle the case when sourceUri is null
                    callback(false)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                callback(false)
            }

        }
    }

    private fun copyFile(
        context: Context,
        path: Uri,
        sourceUri: Uri,
        destinationFile: File,
        callback: (Boolean) -> Unit
    ) {
        try {
            val contentResolver = context.contentResolver
            val sourceStream = contentResolver.openInputStream(sourceUri)
            val destinationStream = FileOutputStream(destinationFile)
            sourceStream?.use { input ->
                destinationStream.use { output ->
                    input.copyTo(output)
                }
            }
            callback(true)
        } catch (e: IOException) {
            e.printStackTrace()
            callback(false)
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)

        }
    }





    // Define a suspending function to calculate the total size asynchronously
    suspend fun calculateTotalSizeAsync(videos: List<Video>): Long = withContext(Dispatchers.Default) {
        videos.sumOf { video ->
            convertSizeToBytes(video.size) // Convert video.size to Long if necessary
        }
    }

    fun convertSizeToBytes(size: String): Long {
        val number = size.substringBefore(" ").toDoubleOrNull() ?: 0.0
        val unit = size.substringAfter(" ")
        val bytes = when (unit.uppercase()) {
            "KB" -> number * 1024
            "MB" -> number * 1024 * 1024
            "GB" -> number * 1024 * 1024 * 1024
            else -> number // Assuming the default unit is bytes
        }
        return bytes.toLong()
    }

    fun calculateTotalSizeAndUpdateUI(videos: List<Video>) {
        viewModelScope.launch {
            try {
                val totalSize = calculateTotalSizeAsync(videos)
                val formattedSize = AppUtils.formatFileSize(totalSize)
                totalSizeLiveData.value = formattedSize
            } catch (e: Exception) {
                // Handle exceptions here
                e.printStackTrace()
            }
        }
    }

    suspend fun sortVideosList(sortType: Int, videos: List<Video>): List<Video> =
        withContext(Dispatchers.IO) {
            when (sortType) {
                0 -> {
                    videos.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
                        it.title ?: ""
                    })  // Nam
                    // e A to Z
                }
                1 -> {
                    videos.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) {
                        it.title ?: ""
                    })  // Nam
                    // e A to Z
                }  // Name Z to A
                2 -> videos.sortedByDescending { it.date } // Date New to Old
                3 -> videos.sortedBy { it.date } // Date Old to New
                4 -> videos.sortedByDescending {
                    AppUtils.convertMBtoBytes(it.size)

                } // Size Big to Small
                5 -> videos.sortedBy { AppUtils.convertMBtoBytes(it.size) }
                // Size Small to Big
                6 -> videos.sortedByDescending { it.duration }//duration from big to smalle
                7 -> videos.sortedBy { it.duration }
                else -> videos // Default order (0: Name A to Z)
            }
        }


    suspend fun renameVideo(context: Context, item: Video, newName: String) {
        idrename=item.id
        nameNew=newName
        withContext(Dispatchers.IO) {
            try {
                val currentUri = item.contentUri?.let { Uri.parse(it) }
                val currentPath = currentUri?.let { AppUtils.getPathFromUri(context, it) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val currentFile = item.contentUri?.let {
                        AppUtils.getPathFromUri(context, Uri.parse(it))?.let { File(it) }
                    }
                    currentFile?.let { currentFile ->
                        val newName = nameNew
                        if (!newName?.let { isValidFileName(it) }!!) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Please enter a valid name",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@withContext
                        }
                        if (currentFile.exists() && newName.isNotEmpty()) {
                            val newFile =
                                File(currentFile.parentFile, newName + "." + currentFile.extension)
                            val fromUri = item.contentUri
                            fromUri?.let {
                                ContentValues().also {
                                    it.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                                    context.contentResolver.update(
                                        Uri.parse(fromUri),
                                        it,
                                        null,
                                        null
                                    )
                                    it.clear()
                                    //updating file details
                                    it.put(
                                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                                        newName.toString()
                                    )
                                    it.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                                    context.contentResolver.update(
                                        Uri.parse(fromUri),
                                        it,
                                        null,
                                        null
                                    )
                                    _ForRename.postValue(true)
                                }
                            }
                        }
                    }
                }else{
                    if (currentUri != null && currentPath != null && newName.isNotEmpty()) {
                        if (!isValidFileName(newName)) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                            }
                            return@withContext
                        }

                        val currentFile = File(currentPath)
                        val newFileName = "$newName.${currentFile.extension}"
                        val newFile = File(currentFile.parent, newFileName)

                        // Step 1: Copy the file to the new name
                        try {
                            currentFile.inputStream().use { input ->
                                newFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            // Step 2: Delete the old file using ContentResolver
                            val deletedRows = context.contentResolver.delete(currentUri, null, null)

                            if (deletedRows > 0) {
                                // Step 3: Scan the new file with MediaScanner
                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(newFile.absolutePath),
                                    arrayOf("video/*")
                                ) { _, _ ->
                                    _ForRename.postValue(true)

                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to delete the original file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "File operation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = securityException as? RecoverableSecurityException
                    _permissionNeededForRename.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    suspend fun performDeleteImage(uri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore API to delete files on Android Q and above
                    val deleted = AppUtils.deleteVideoFile(context, uri)
                    if (deleted) {
                        _ForDelete.postValue(true)
                    } else {
                        // Handle deletion failure, if needed
                    }
                } else {
                    // Use File API to delete files below Android Q
                    val file = AppUtils.getPathFromUri(context, uri)?.let { File(it) }
                    if (file?.exists() == true) {
                        if (file.delete()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                // Use MediaScannerConnection API to notify the system about the deletion
                                MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(file.path),
                                    null
                                ) { path, uri ->
                                    // Do something after the scan is complete, if needed
                                    _ForDelete.postValue(true)
                                }
                            } else {
                                // For devices below Android KitKat, use Intent.ACTION_MEDIA_MOUNTED broadcast
                                context.sendBroadcast(
                                    Intent(
                                        Intent.ACTION_MEDIA_MOUNTED,
                                        Uri.parse("file://${file.absolutePath}")
                                    )
                                )
                                _ForDelete.postValue(true)
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

                    urinew = uri
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                } else {
                }
            }
        }
    }





    fun addVideoToDb(listofvideos: List<Video>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.localRepo.videosDao().insertvideos(listofvideos)
        }
    }

    fun getDataFromDb(): Flow<List<Video>> {
        return repository.localRepo.videosDao().getAllData()
    }

    fun deletedatafromdb(title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.localRepo.videosDao().deleteData(title)
        }
    }

    fun deletedatafromdb(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.localRepo.videosDao().deleteDataFromDb(id)
        }
    }

    suspend fun UpdateDataItemTitle() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                nameNew?.let {
                    idrename?.let { it1 ->
                        repository.localRepo.videosDao().updateVideoTitleById(
                            it1,
                            it
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSong(track: Video){
        repository.deleteSongFromDb(track)
    }



}