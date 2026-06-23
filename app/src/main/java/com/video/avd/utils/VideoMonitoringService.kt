package com.video.avd.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.video.avd.R
import com.video.avd.ui.splash.SplashActivity

class VideoMonitoringService : Service() {

    private lateinit var contentObserver: ContentObserver
    private var lastVideoId: Long? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    private fun startForegroundService() {
        createNotificationChannel(this)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Monitoring")
            .setContentText("Monitoring new videos...")
            .setSmallIcon(R.drawable.ic_launcher_background) // Use your app's icon
            .build()
        startForeground(NOTIFICATION_ID, notification)
        contentObserver = object : ContentObserver(Handler(Looper.myLooper()!!)) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkForNewVideosAndNotify()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )

        // Initial check to set the baseline for what is considered "new"
        initializeLastVideoId()
    }

    private fun checkForNewVideosAndNotify() {
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED)
        val selection = "${MediaStore.Video.Media._ID} > ?"
        val selectionArgs = arrayOf(lastVideoId.toString())
        val sortOrder = "${MediaStore.Video.Media._ID} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        if (cursor != null && cursor.moveToFirst()) {
            // New video detected, update the last known video ID
            val newLastVideoId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
            if (newLastVideoId != lastVideoId) {
                lastVideoId = newLastVideoId
                // Show notification for the new video
                showNewVideoNotification()
            }
        }
        cursor?.close()
    }

    private fun initializeLastVideoId() {
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val sortOrder = "${MediaStore.Video.Media._ID} DESC"
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        if (cursor != null && cursor.moveToFirst()) {
            lastVideoId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        }
        cursor?.close()
    }

    private fun showNewVideoNotification() {
        val notificationIntent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("New Video Detected")
            .setContentText("Tap to view.")
            .setSmallIcon(R.mipmap.ic_launcher) // Use your app's icon
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification) // Use a different ID for each notification
    }

    companion object {
        const val CHANNEL_ID = "video_monitoring_service_channel"
        const val NOTIFICATION_ID = 1

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = CHANNEL_ID // Define this string in your strings.xml
                val descriptionText = "Notifications for new videos detected in storage" // Define this string in your strings.xml
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                // Correct way to get the NotificationManager:
                val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}