package com.video.avd.utils.newvideo_receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.video.avd.R
import com.video.avd.ui.splash.SplashActivity
import com.video.avd.utils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VideoCheckJobService(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val lastCheckTime = getLastCheckTime(applicationContext)
            val newVideos = fetchNewVideos(applicationContext, lastCheckTime)

            if (newVideos.isNotEmpty()) {
                notifyUserNewVideo(applicationContext, newVideos.size, newVideos)
                saveLastCheckTime(applicationContext, System.currentTimeMillis() / 1000)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }


    // ------------------ NOTIFICATION ------------------
    private suspend fun notifyUserNewVideo(
        context: Context,
        videoCount: Int,
        newList: List<Pair<String, String>>
    ) {
        val prefs = AppPreference.getLanguage(context).toString()
        val localizedContext = adjustLocale(context, prefs) ?: return

        val channelId = "new_video_channel"
        val name = "$videoCount New Video Detected"
        val descriptionText = "Let's watch it!"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("opened_from_notification", true)
            putExtra("notification_id", 1234)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (newList.size > 1) {
            showMultipleVideosNotification(localizedContext, channelId, videoCount, newList, pendingIntent)
        } else {
            showSingleVideoNotification(localizedContext, channelId, videoCount, newList, pendingIntent)
        }
    }

    private suspend fun showMultipleVideosNotification(
        context: Context,
        channelId: String,
        videoCount: Int,
        newList: List<Pair<String, String>>,
        pendingIntent: PendingIntent
    ) = withContext(Dispatchers.IO) {
        val remoteViews = RemoteViews(context.packageName, R.layout.custom_notification_layout)
        val ids = listOf(R.id.image1, R.id.image2, R.id.image3)

        ids.forEach { id -> remoteViews.setViewVisibility(id, android.view.View.GONE) }

        for (i in ids.indices) {
            if (i < newList.size) {
                val bitmap = loadVideoThumbnail(context, newList[i].second)
                bitmap?.let {
                    remoteViews.setImageViewBitmap(ids[i], it)
                    remoteViews.setViewVisibility(ids[i], android.view.View.VISIBLE)
                }
            }
        }

        remoteViews.setViewVisibility(R.id.image4_overlay, if (newList.size > 3) android.view.View.VISIBLE else android.view.View.GONE)
        remoteViews.setTextViewText(R.id.video_count, "$videoCount (Videos) Added")

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Video Downloader Browser Hub")
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1234, notificationBuilder.build())
    }

    private suspend fun showSingleVideoNotification(
        context: Context,
        channelId: String,
        videoCount: Int,
        newList: List<Pair<String, String>>,
        pendingIntent: PendingIntent
    ) = withContext(Dispatchers.IO) {
        val latestVideoThumbnail = if (newList.isNotEmpty())
            loadVideoThumbnail(context, newList[0].second) else null

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$videoCount New Video Available")
            .setContentText(context.resources.getString(R.string.desc_new_notifi))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        latestVideoThumbnail?.let {
            notificationBuilder
                .setLargeIcon(it)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(it))
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1234, notificationBuilder.build())
    }

    // ------------------ VIDEO FETCH ------------------
    private suspend fun fetchNewVideos(context: Context, lastCheckTime: Long): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val videoList = mutableListOf<Pair<String, String>>()
            try {
                val projection = arrayOf(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DATA
                )
                val selection = "${MediaStore.Video.Media.DATE_ADDED} > ?"
                val selectionArgs = arrayOf(lastCheckTime.toString())
                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val data = cursor.getString(dataColumn)
                        videoList.add(Pair(name, data))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            videoList
        }

    // ------------------ THUMBNAIL ------------------
    private suspend fun loadVideoThumbnail(context: Context, videoPath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                try {
                    Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(Uri.fromFile(File(videoPath)))
                        .apply(RequestOptions().frame(1000000))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                            override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                continuation.resume(null)
                            }
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                continuation.resume(resource)
                            }
                        })
                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(null)
                }
            }
        }

    // ------------------ PREFERENCES ------------------
    private fun saveLastCheckTime(context: Context, time: Long) {
        val sharedPreferences = context.getSharedPreferences("VideoCheckPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("LastCheckTime", time)
            apply()
        }
    }

    private fun getLastCheckTime(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences("VideoCheckPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("LastCheckTime", 0)
    }

    // ------------------ LOCALE ------------------
    private fun adjustLocale(context: Context, languageCode: String): Context? {
        return try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } catch (e: Exception) {
            Log.e("LocaleAdjust", "Failed to adjust locale", e)
            null
        }
    }
}
