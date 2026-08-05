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
import android.webkit.MimeTypeMap
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

    private val actionsTag = "SearchVideoActions"


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
            Log.d(actionsTag, "delete started: input=$uri normalized=$mediaUri sdk=${Build.VERSION.SDK_INT}")
            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    val deletedRows = context.contentResolver.delete(mediaUri, null, null)
                    Log.d(actionsTag, "delete resolver result: rows=$deletedRows uri=$mediaUri")
                    _ForDelete.postValue(deletedRows > 0)

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
                Log.w(actionsTag, "delete needs authorization: uri=$mediaUri", securityException)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    urinew = mediaUri
                    val intentSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        createMediaRequestSender(context, mediaUri, delete = true)
                    } else {
                        (securityException as? RecoverableSecurityException)
                            ?.userAction?.actionIntent?.intentSender
                    }
                    if (intentSender != null) {
                        Log.d(actionsTag, "delete authorization request created: uri=$mediaUri")
                        _permissionNeededForDelete.postValue(intentSender)
                    } else {
                        Log.e("SearchVideoDelete", "Unable to create delete request for uri=$mediaUri")
                        _ForDelete.postValue(false)
                    }
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
            Log.d(
                actionsTag,
                "rename started: id=${item.id} uri=${item.contentUri} old=${item.title} new=$newName sdk=${Build.VERSION.SDK_INT}"
            )
            try {
                if (!isValidFileName(newName)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    val contentUri = item.contentUri
                    if (contentUri.isNullOrBlank()) {
                        _ForRename.postValue(false)
                        return@withContext
                    }
                    contentUri.let { value ->
                        val uri = resolveVolumeSpecificUri(context, Uri.parse(value))
                        Log.d(actionsTag, "rename normalized uri: input=$value normalized=$uri")
                        val oldDisplayName = context.contentResolver.query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                        val extensionFromName = oldDisplayName
                            ?.substringAfterLast('.', "")
                            ?.takeIf { it.isNotBlank() }
                        val extension = extensionFromName
                            ?: MimeTypeMap.getSingleton()
                                .getExtensionFromMimeType(context.contentResolver.getType(uri))
                            ?: item.orignalpath.substringAfterLast('.', "")
                                .takeIf { it.isNotBlank() }
                            ?: "mp4"
                        val displayName = if (newName.endsWith(".$extension", ignoreCase = true)) {
                            newName
                        } else {
                            "$newName.$extension"
                        }
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        }
                        val updatedRows = context.contentResolver.update(uri, values, null, null)
                        Log.d(
                            "SearchVideoRename",
                            "uri=$uri old=$oldDisplayName new=$displayName rows=$updatedRows"
                        )
                        _ForRename.postValue(updatedRows > 0)
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
                Log.w(actionsTag, "rename needs authorization: uri=${item.contentUri}", securityException)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri = item.contentUri
                        ?.let(Uri::parse)
                        ?.let { resolveVolumeSpecificUri(context, it) }
                    val intentSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uri != null) {
                        createMediaRequestSender(context, uri, delete = false)
                    } else {
                        (securityException as? RecoverableSecurityException)
                            ?.userAction?.actionIntent?.intentSender
                    }
                    if (intentSender != null) {
                        Log.d(actionsTag, "rename authorization request created: uri=$uri")
                        _permissionNeededForRename.postValue(intentSender)
                    } else {
                        Log.e("SearchVideoRename", "Unable to create write request for uri=$uri")
                        _ForRename.postValue(false)
                    }
                } else {
                }
            }catch (e: Exception){
                Log.e(actionsTag, "rename failed unexpectedly", e)
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
        val resolved = MediaStore.Video.Media.getContentUri(volumeName, id)
        Log.d(actionsTag, "volume uri resolved: input=$uri volume=$volumeName id=$id output=$resolved")
        return resolved
    }

    /**
     * Some MediaProvider versions expose a row through the video collection but only
     * accept its equivalent Files-table URI when creating a write/delete request.
     * Try each representation of the same MediaStore row and use the first one the
     * provider accepts. No operation is performed until the user confirms the prompt.
     */
    private fun createMediaRequestSender(
        context: Context,
        uri: Uri,
        delete: Boolean
    ): IntentSender? {
        val id = runCatching { ContentUris.parseId(uri) }.getOrNull() ?: return null
        val volume = runCatching { MediaStore.getVolumeName(uri) }
            .getOrDefault(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val candidates = linkedSetOf(
            uri,
            MediaStore.Video.Media.getContentUri(volume, id),
            ContentUris.withAppendedId(MediaStore.Files.getContentUri(volume), id),
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
            ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
        )

        candidates.forEach { candidate ->
            val sender = runCatching {
                val request = if (delete) {
                    MediaStore.createDeleteRequest(context.contentResolver, listOf(candidate))
                } else {
                    MediaStore.createWriteRequest(context.contentResolver, listOf(candidate))
                }
                request.intentSender
            }.onFailure { error ->
                Log.w(
                    actionsTag,
                    "${if (delete) "delete" else "write"} request rejected: uri=$candidate",
                    error
                )
            }.getOrNull()

            if (sender != null) {
                urinew = candidate
                Log.d(
                    actionsTag,
                    "${if (delete) "delete" else "write"} request accepted: uri=$candidate"
                )
                return sender
            }
        }
        return null
    }
}
