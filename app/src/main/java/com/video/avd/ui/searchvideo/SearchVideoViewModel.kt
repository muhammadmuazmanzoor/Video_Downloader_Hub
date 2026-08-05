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
            val mediaUri = resolveVolumeSpecificUri(context, uri)
            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    _ForDelete.postValue(
                        context.contentResolver.delete(mediaUri, null, null) > 0
                    )

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
                    urinew = mediaUri
                    val intentSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        runCatching {
                            MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(mediaUri)
                            ).intentSender
                        }.getOrNull()
                    } else {
                        (securityException as? RecoverableSecurityException)
                            ?.userAction?.actionIntent?.intentSender
                    }
                    _permissionNeededForDelete.postValue(intentSender)
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
            nameNew = newName
            try {
                if (!isValidFileName(newName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    item.contentUri?.let { value ->
                        val uri = resolveVolumeSpecificUri(context, Uri.parse(value))
                        val oldDisplayName = context.contentResolver.query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                        val extension = oldDisplayName
                            ?.substringAfterLast('.', "")
                            ?.takeIf { it.isNotBlank() }
                        val displayName = if (extension == null || newName.endsWith(".$extension")) {
                            newName
                        } else {
                            "$newName.$extension"
                        }
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        }
                        _ForRename.postValue(
                            context.contentResolver.update(uri, values, null, null) > 0
                        )
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
                    val uri = item.contentUri
                        ?.let(Uri::parse)
                        ?.let { resolveVolumeSpecificUri(context, it) }
                    val intentSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri != null) {
                        runCatching {
                            MediaStore.createWriteRequest(
                                context.contentResolver,
                                listOf(uri)
                            ).intentSender
                        }.getOrNull()
                    } else {
                        (securityException as? RecoverableSecurityException)
                            ?.userAction?.actionIntent?.intentSender
                    }
                    _permissionNeededForRename.postValue(intentSender)
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

    private fun resolveVolumeSpecificUri(context: Context, uri: Uri): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return uri

        val id = runCatching { ContentUris.parseId(uri) }.getOrNull() ?: return uri
        val volumeName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.VOLUME_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: MediaStore.VOLUME_EXTERNAL_PRIMARY

        return MediaStore.Video.Media.getContentUri(volumeName, id)
    }
}
