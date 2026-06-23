package com.video.avd.ui.searchvideo

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.data.local.Entities
import com.video.avd.repo.Repository
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.ClickEvent
import com.video.avd.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SearchVideoViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {


    var nameNew : String? = null

    private val _ForRename = MutableLiveData<Boolean?>()
    var isForRename : LiveData<Boolean?> = _ForRename
    private val _permissionNeededForRename = MutableLiveData<IntentSender?>()
    val permissionNeededForRename: LiveData<IntentSender?> = _permissionNeededForRename

    private val _insertedToFavMsg= SingleLiveEvent<String>()
    val insertedToFavMsg : LiveData<String> = _insertedToFavMsg


    private val _remove = MutableLiveData<ClickEvent<Int>>()
    val remove : LiveData<ClickEvent<Int>> = _remove




    var urinew: Uri? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete
    private val _ForDelete = MutableLiveData<Boolean?>()
    var isForDelete: LiveData<Boolean?> = _ForDelete

    suspend fun updateUserData(userEntities: Video) {
        withContext(Dispatchers.IO) {
            repository.localRepo.dao().insertEntities(userEntities)
        }
    }

    suspend fun getDataFromUrl(url: String): Video {
        return withContext(Dispatchers.IO) {
            repository.localRepo.dao().getEntitiesWithUrl(url)
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
                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val folderId = cursor.getString(folderIdColumn)
                videoList.add(
                    Video(
                        id=id,
                        contentUri=contentUri.toString(),
                        title= title,
                        duration= AppUtils.getDurationString(duration),
                        date = AppUtils.convertLongToDate(cursor.getLong(dateModifiedColumn)),
                        size =AppUtils.formatFileSize(cursor.getLong(sizeColumn)),
                        folderid = folderId
                    )
                )
            }
            emit(videoList)
        }
    }.flowOn(Dispatchers.IO)


    suspend fun performDeleteImage(uri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    // Use MediaStore API to delete files on Android Q and above
                    AppUtils.deleteVideoFile(context, uri)

                } else {
                    try {
                        // Use File API to delete files below Android Q
                        val file = AppUtils.getPathFromUri(context, uri)?.let { File(it) }
                        if (file?.exists() == true) {
                            if (file.delete()) {
                                MediaScannerConnection.scanFile(
                                    context, arrayOf(file.parent),
                                    null
                                ) { path, uri ->
                                    // Do something after the scan is complete, if needed
                                    _ForDelete.postValue(true)
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

                    urinew = uri
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                } else {
                }
            }
        }
    }


    fun deleteSongFromDb(track: Entities) = viewModelScope.launch(Dispatchers.IO) {
        if (repository.isObjectExists(track.name.toString())) {
            repository.localRepo.dao().deleteSongByName(track.name.toString())
        } else {
            Log.e("databases", "song dont exist in db")
        }
    }
    suspend fun renameVideo(context: Context, item: Video, newName: String) {
        withContext(Dispatchers.IO){

            try {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    val currentFile =
                        item.contentUri?.let { AppUtils.getPathFromUri(context, Uri.parse(it))?.let { File(it) } }
                    currentFile?.let { currentFile ->
                        val newName = newName
                        if (!isValidFileName(newName)){
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                            }
                            return@withContext
                        }
                        if (newName != null && currentFile.exists() && newName.isNotEmpty()) {
                            val newFile =
                                File(currentFile.parentFile, newName + "." + currentFile.extension)
                            val fromUri = item.contentUri
                            fromUri?.let {
                                ContentValues().also {
                                    it.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                                    context.contentResolver.update(Uri.parse(fromUri), it, null, null)
                                    it.clear()
                                    //updating file details
                                    it.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName.toString())
                                    it.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                                    context.contentResolver.update(Uri.parse(fromUri), it, null, null)
                                    _ForRename.postValue(true)
                                }
                            }


                        }
                    }

                }else{
                    val currentFile =
                        item.contentUri?.let { AppUtils.getPathFromUri(context, Uri.parse(it))?.let { File(it) } }
                    currentFile?.let {
                        if(newName != null && currentFile.exists() && newName.toString().isNotEmpty()){
                            val newFile = File(currentFile.parentFile, newName.toString()+"."+currentFile.extension)
                            if(currentFile.renameTo(newFile)){
                                MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()), arrayOf("video/*"), null)
                                _ForRename.postValue(true)

                            }
                        }
                    }
                }
            }catch (securityException: SecurityException){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException

                    nameNew = newName
                    _permissionNeededForRename.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                } else {
                }
            }catch (e: Exception){
                e.printStackTrace()
            }

        }

    }

    fun isValidFileName(fileName: String): Boolean {
        val forbiddenChars = arrayOf("/", "\\", "?", "%", "*", ":", "|", "\"", "<", ">")
        return fileName.none { it.toString() in forbiddenChars } && fileName.isNotBlank()
    }
}