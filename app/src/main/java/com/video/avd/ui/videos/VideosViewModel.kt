package com.video.avd.ui.videos

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.R
import com.video.avd.data.local.Entities
import com.video.avd.repo.VideoRepository
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.getPathFromUri
import com.video.avd.utils.ClickEvent
import com.video.avd.utils.CustomAlertDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VideosViewModel @Inject constructor(val repository: VideoRepository, @ApplicationContext private val context: Context) : ViewModel() {

    val videosListFlow = repository.getAllVideos(context)

    var urinew: Uri? = null

    //Expose insetto favourite from repository
    val insertedToFavMsg: LiveData<String> = repository.insertedToFavMsg

    //Expose remove  from repository
    val remove: LiveData<ClickEvent<Int>> = repository.remove


    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    private val _ForDelete = MutableLiveData<Boolean?>()

    var isForDelete: LiveData<Boolean?> = _ForDelete

    var nameNew: String? = null
    var idrename: Long? = null
    private val _ForRename = MutableLiveData<Boolean?>()
    var isForRename: LiveData<Boolean?> = _ForRename

    private val _permissionNeededForRename = MutableLiveData<IntentSender?>()
    val permissionNeededForRename: LiveData<IntentSender?> = _permissionNeededForRename





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
                    try {
                        val deleted = context.contentResolver.delete(uri, null, null)
                        if (deleted > 0) {
                            _ForDelete.postValue(true)
                        } else {
                            // Handle deletion failure, if needed
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
                } else {
                    // Use File API to delete files below Android Q
                    val file = getPathFromUri(context, uri)?.let { File(it) }
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
                            Log.d("delete", "not deleted")
                            _ForDelete.postValue(false)
                            // Handle deletion failure, if needed
                        }
                    } else {
                        Log.d("delete", "not deleted")
                        _ForDelete.postValue(false)
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


    /*-----------------------------------------------------FAVORITE FUNCTIONS STARTED------------------------------------------------------------*/


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
                } // Name Z to A
                2 -> videos.sortedByDescending { it.date } // Date New to Old
                3 -> videos.sortedBy { it.date } // Date Old to New
                4 -> videos.sortedByDescending { AppUtils.convertMBtoBytes(it.size) } // Size Big to Small
                5 -> videos.sortedBy { AppUtils.convertMBtoBytes(it.size) } // Size Small to Big
                6 -> videos.sortedByDescending { it.duration }//duration from big to smalle
                7 -> videos.sortedBy { it.duration }
                else -> videos // Default order (0: Name A to Z)
            }

        }



    fun isValidFileName(fileName: String): Boolean {
        val forbiddenChars = arrayOf("/", "\\", "?", "%", "*", ":", "|", "\"", "<", ">")
        return fileName.none { it.toString() in forbiddenChars } && fileName.isNotBlank()
    }

    fun getvideosfromdb(id: String): Flow<List<Video>> {
        return if (id=="-1"){
            repository.localRepo.videosDao().getRecentVideos(getFormattedDateForLast7Days())
        }else{
            repository.localRepo.videosDao().getFolderVideos(id)
        }
    }

    fun getFormattedDateForLast7Days(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Move back 7 days
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        return dateFormat.format(calendar.time) // Format the date as yyyy/MM/dd
    }

    suspend fun deletedatafromdb(title: String) {
        withContext(Dispatchers.IO) {
            repository.localRepo.videosDao().deleteData(title)
        }
    }


    fun addVideoToDb(listofvideos: List<Video>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingvideoId=repository.localRepo.videosDao().getAllVideoIds()
            // Iterate over the incoming list of videos and set isNew flag
            if (existingvideoId.isNotEmpty()){
                listofvideos.forEach { video ->
                    video.isNew = video.id !in existingvideoId
                }
            }
            repository.localRepo.videosDao().insertvideos(listofvideos)
        }
    }

     suspend fun UpdateDataItemTitle() {
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

    fun deleteVideoPermanently(uri: Uri, context: Context) {
        context?.let { activity->
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                val inflater = LayoutInflater.from(activity)
                val view = inflater.inflate(R.layout.alert_dailog, null)
                view.findViewById<TextView>(R.id.alertMessage).text =
                    activity.getString(R.string.do_you_want_to_delete_this)
                val icon = view.findViewById<ImageView>(R.id.alertImage)
                val btnCancel = view.findViewById<Button>(R.id.btnCancel)
                val btnOk = view.findViewById<Button>(R.id.btnOk)
                btnOk.text = activity.getString(R.string.delete)
                icon.setImageResource(R.drawable.ci_delete_p)
                val alertDialog = CustomAlertDialog(activity)
                alertDialog.setView(view)
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                btnOk.setOnClickListener {
                    viewModelScope.launch {
                        performDeleteImage(uri, context)
                    }
                    alertDialog.dismiss()
                }
                btnCancel.setOnClickListener {
                    alertDialog.dismiss()
                }
                alertDialog.show()
            } else {
                viewModelScope.launch {
                    performDeleteImage(uri, context)
                }
            }
        }
    }


    fun markFolderAsOpened(folderId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFolder(folderId)
        }
    }


}