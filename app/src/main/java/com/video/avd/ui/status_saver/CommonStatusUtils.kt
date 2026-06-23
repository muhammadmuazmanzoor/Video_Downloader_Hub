package com.video.avd.ui.status_saver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.material.snackbar.Snackbar
import com.video.avd.R
import com.video.avd.constent.isFileSave
import com.video.avd.ui.status_saver.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random


object CommonStatusUtils {
    const val GRID_COUNT = 1
    const val CHANNEL_NAME = "TERAFORT"
     val savedFilesList: MutableList<Status> = mutableListOf()
    @JvmField
    val STATUS_DIRECTORY = File(Environment.getExternalStorageDirectory().toString() + File.separator + "WhatsApp/Media/.Statuses")

    @JvmField
    val STATUS_DIRECTORY_BUSINESS = File(Environment.getExternalStorageDirectory().toString() + File.separator + "WhatsApp Business/Media/.Statuses")

    @JvmField
    var APP_DIR: String? = Environment.getExternalStorageDirectory().toString() + File.separator + "WhatsApp"


//    fun copyFile(status: Status, context: Context) {
//        val file = File(APP_DIR)
//        val fileName: String
//        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
//        val currentDateTime = sdf.format(Date())
//        fileName = if (status.isVideo) {
//            "VID_$currentDateTime.mp4"
//        } else {
//            "IMG_$currentDateTime.jpg"
//        }
//        val destFile = File(file.toString() + File.separator + fileName)
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                val values = ContentValues()
//                val destinationUri: Uri?
//                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
//                values.put(
//                    MediaStore.MediaColumns.RELATIVE_PATH,
//                    Environment.DIRECTORY_DCIM + "/status_saver"
//                )
//                val collectionUri: Uri = if (status.isVideo) {
//                    values.put(MediaStore.MediaColumns.MIME_TYPE, "video/*")
//                    MediaStore.Video.Media.getContentUri(
//                        MediaStore.VOLUME_EXTERNAL_PRIMARY
//                    )
//                } else {
//                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
//                    MediaStore.Images.Media.getContentUri(
//                        MediaStore.VOLUME_EXTERNAL_PRIMARY
//                    )
//                }
//                destinationUri = context.contentResolver.insert(collectionUri, values)
//                val inputStream = context.contentResolver.openInputStream(status.documentFile.uri)
//                val outputStream = context.contentResolver.openOutputStream(
//                    destinationUri!!
//                )
//                IOUtils.copy(inputStream, outputStream)
//                Toast.makeText(context,"Saved", Toast.LENGTH_SHORT).show()
//                isFileSave.value = true
//            } else {
//                FileUtils.copyFile(status.file, destFile)
//                destFile.setLastModified(System.currentTimeMillis())
//                SingleMediaScanner(context, file)
//                val data = FileProvider.getUriForFile(
//                    context,
//                    "com.video.avd.provider",
//                    destFile
//                )
//                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
//                isFileSave.value = true
//            }
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }



    suspend fun copyFile(status: Status, context: Context) {
        val file = File(APP_DIR)
        val fileName: String
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val currentDateTime = sdf.format(Date())
        fileName = if (status.isVideo) {
            "VID_$currentDateTime.mp4"
        } else {
            "IMG_$currentDateTime.jpg"
        }
        val destFile = File(file.toString() + File.separator + fileName)

        try {
            withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues()
                    val destinationUri: Uri?
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    values.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DCIM + "/zmstatus_saver"
                    )
                    val collectionUri: Uri = if (status.isVideo) {
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/*")
                        MediaStore.Video.Media.getContentUri(
                            MediaStore.VOLUME_EXTERNAL_PRIMARY
                        )
                    } else {
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
                        MediaStore.Images.Media.getContentUri(
                            MediaStore.VOLUME_EXTERNAL_PRIMARY
                        )
                    }
                    destinationUri = context.contentResolver.insert(collectionUri, values)
                    val inputStream = context.contentResolver.openInputStream(status.documentFile.uri)
                    val outputStream = context.contentResolver.openOutputStream(destinationUri!!)
                    IOUtils.copy(inputStream, outputStream)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.resources.getString(R.string.saved), Toast.LENGTH_SHORT).show()
                        isFileSave.value = true
                    }
                } else {
                    copyFileUsingStream(status.file, destFile)
                    destFile.setLastModified(System.currentTimeMillis())
                    SingleMediaScanner(context, file)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.resources.getString(R.string.saved), Toast.LENGTH_SHORT).show()
                        isFileSave.value = true
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
    suspend fun copyFiles(statusList: List<Status>, context: Context) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

        try {
            withContext(Dispatchers.IO) {
                statusList.forEach { status ->
                    val fileName: String
                    val currentDateTime = sdf.format(Date())
                    fileName = if (status.isVideo) {
                        "VID_$currentDateTime.mp4"
                    } else {
                        "IMG_$currentDateTime.jpg"
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, if (status.isVideo) "video/*" else "image/*")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/status_saver")
                        }

                        val collectionUri = if (status.isVideo) {
                            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        } else {
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        }

                        val destinationUri = context.contentResolver.insert(collectionUri, values)
                        val inputStream = context.contentResolver.openInputStream(status.documentFile.uri)
                        val outputStream = context.contentResolver.openOutputStream(destinationUri!!)

                        outputStream?.use { outStream ->
                            inputStream?.copyTo(outStream)
                        }
                    } else {
                        // Handle Android versions below Q
                        val appDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "status_saver")
                        if (!appDir.exists()) appDir.mkdirs()
                        val destFile = File(appDir, fileName)
                        copyFileUsingStream(status.file, destFile)
                        destFile.setLastModified(System.currentTimeMillis())
                        MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
                    }
                }

                // Notify once all items have been saved
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${statusList.size} items saved to gallery.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getVideoLengthAsString(videoPath: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                var durationString: String? = null
                try {
                    retriever.setDataSource(videoPath)
                    durationString =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
                if (durationString != null) {
                    val duration = durationString.toLong()
                    return@withContext DateUtils.formatElapsedTime(duration / 1000)
                }
                return@withContext "00:00"
            } catch (e: Exception) {
                // Handle the exception if needed
                return@withContext "00:00"
            }
        }
    }
    suspend fun convertDurationToLong(duration: String): Long {
        return withContext(Dispatchers.Default) {
            try {
                val timeParts = duration.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val hours = timeParts[0].toInt()
                val minutes = timeParts[1].toInt()
                ((hours * 60L + minutes) * 1000)
            } catch (e: Exception) {
                // Handle the exception if needed
                0L
            }
        }
    }
    suspend fun getVideoLengthAsString(uri: Uri, context: Context): String {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val durationString =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationString?.toLong() ?: 0
                retriever.release()
                return@withContext DateUtils.formatElapsedTime(duration / 1000)
            } catch (e: Exception) {
                // Handle the exception if needed
                return@withContext "00:00"
            }
        }
    }
    fun showNotification(
        context: Context, container: RelativeLayout, status: Status,
        fileName: String, data: Uri?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            makeNotificationChannel(context)
        }
        val intent = Intent(Intent.ACTION_VIEW)
        if (status.isVideo) {
            intent.setDataAndType(data, "video/*")
        } else {
            intent.setDataAndType(data, "image/*")
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_NAME)
        notification.setSmallIcon(R.drawable.play_icon_round)
            .setContentTitle(fileName)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) notification.setContentText(
            "File Saved to " +
                    Environment.DIRECTORY_DCIM + "/status_saver"
        ) else notification.setContentText("File Saved to" + APP_DIR)
        val notificationManager =
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.notify(Random().nextInt(), notification.build())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) Snackbar.make(
            container,
            "Saved to " + APP_DIR,
            Snackbar.LENGTH_LONG
        ).show() else Snackbar.make(
            container, "Saved to " + Environment.DIRECTORY_DCIM + "/status_saver",
            Snackbar.LENGTH_LONG
        ).show()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun makeNotificationChannel(context: Context) {
        val channel =
            NotificationChannel(CHANNEL_NAME, "Saved", NotificationManager.IMPORTANCE_DEFAULT)
        channel.setShowBadge(true)
        val notificationManager =
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        notificationManager.createNotificationChannel(channel)
    }

    @Throws(IOException::class)
    fun copyFileUsingStream(sourceFile: File, destFile: File) {
        FileInputStream(sourceFile).use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
        }
    }

}