package com.avd.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.avd.util.downloaders.generic_downloader.models.VideoTaskItem
import com.avd.util.downloaders.generic_downloader.models.VideoTaskState
import java.io.File
import javax.inject.Singleton

@Singleton
class NotificationsHelper(private val context: Context) {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "NOTIFICATION_CHANNEL_ID_ALL_DOWNLOADER"
        var  pendingurl=""
        var istiktok=false
        // Throttle notification updates to prevent excessive Binder calls (max once per 500ms)
        private const val MIN_NOTIFICATION_UPDATE_INTERVAL_MS = 500L
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // Use NotificationManagerCompat for better Binder call handling and to prevent ANRs
    private val notificationManagerCompat = NotificationManagerCompat.from(context)
    
    // Track last notification update time per notification ID to throttle updates
    private val lastNotificationUpdateTime = mutableMapOf<Int, Long>()
    
    // Lazy initialization flag to prevent blocking Binder calls during Hilt dependency injection
    @Volatile
    private var channelCreated = false

    // Removed init block - channel creation is now lazy to prevent ANR during Hilt injection
    // Channel will be created on first notification use instead of during construction

    fun createNotificationBuilder(task: VideoTaskItem): Pair<Int, NotificationCompat.Builder> {
        // Lazy channel creation - only create when actually needed to prevent blocking during Hilt injection
        ensureChannelCreated()
        
        val taskPercent = if (task.percentFromBytes == 0F) task.percent else task.percentFromBytes
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).setOnlyAlertOnce(true)
        builder.setContentTitle(File(task.fileName).name)
            .setContentText(task.lineInfo)
            .setSmallIcon(android.R.drawable.stat_sys_download).setOngoing(false)
            .setProgress(100, taskPercent.toInt(), false)
//          .addAction(notificationActionOpen(false))
        when (task.taskState) {
            VideoTaskState.PREPARE -> {
                builder.setSubText("prepare").setProgress(0, 0, true)
                builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download_done)
            }
            VideoTaskState.PENDING -> {
                builder.setSubText("pending").setProgress(0, 0, true)
                builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download_done)
            }
            VideoTaskState.DOWNLOADING -> {
                builder.setSubText("downloading...").setProgress(100, taskPercent.toInt(), false)
                builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download)
            }
            VideoTaskState.PAUSE -> {
                builder.setSubText("pause")
                builder.setProgress(100, taskPercent.toInt(), false)
                builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download)
            }

            VideoTaskState.SUCCESS -> {
                builder.clearActions()
//                val actionOpenInApp = notificationActionOpen(true)
//                val actionWatch = notificationActionWatch(task.fileName)
//                val actionWatchIntent = notificationIntentWatch(task.fileName)
//                builder.setContentIntent(actionWatchIntent)
                  Log.e("TaskDownloadSucess", pendingurl)
                  builder.setSubText("success!!!").setProgress(0, 0, false)
                  builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download_done)
//                if (!istiktok){
                    try {
                        // Create file and check existence
//                    val file = File(task.url)
//                    if (file.exists()) {
                        // Create intent to open video player

                        val bundle=Bundle()
                        bundle.putBoolean("isliveuri", true)
                        bundle.putBoolean("isBgNotAllowed", true)
                        bundle.putString("uri", Uri.parse(pendingurl).toString())
                        Log.d("NotificationsHelper", "Pending_Url ${pendingurl}")
                        val intent = Intent().apply {
                            component = ComponentName(context.packageName, "com.video.avd.ui.player.PlayerVideoActivity")
                            data = Uri.parse(pendingurl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtras(bundle)
                        }

                        // Create PendingIntent
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            task.mId.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        // Attach PendingIntent to notification
                        builder.setContentIntent(pendingIntent)
//                    } else {
//                        // File not found
//                        builder.setContentText("File not found: ${task.fileName}")
//                    }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        builder.setContentText("Error: ${e.message}")
                    }
//                }

            }

            VideoTaskState.ERROR, VideoTaskState.ENOSPC -> {
                builder.clearActions()
//              val action = notificationActionOpen(true)
                builder.setSubText("Error")
                builder.setContentText("Failed " + task.errorMessage).setProgress(100, taskPercent.toInt(), false)
                builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download_done)
//              builder.addAction(action)
            }
            VideoTaskState.CANCELED -> {
                builder.setSubText("Canceled")
                builder.setProgress(0, 0, false)
                builder.setOngoing(false).setSmallIcon(android.R.drawable.stat_sys_download)
            }
            else -> {}
        }
        hideNotification(task.mId.hashCode() + 1)
        if (task.taskState == VideoTaskState.SUCCESS || task.taskState == VideoTaskState.ERROR || task.taskState == VideoTaskState.CANCELED) {
            hideNotification(task.mId.hashCode())
            return Pair(task.mId.hashCode() + 1, builder)
        }
        return Pair(task.mId.hashCode(), builder)
    }


    fun showNotification(builderPair: Pair<Int, NotificationCompat.Builder>) {
        // Lazy channel creation - only create when actually needed
        ensureChannelCreated()
        
        val notificationId = builderPair.first
        val currentTime = System.currentTimeMillis()
        
        // Throttle notification updates to prevent excessive Binder calls that can cause ANRs
        val lastUpdateTime = lastNotificationUpdateTime[notificationId] ?: 0L
        val timeSinceLastUpdate = currentTime - lastUpdateTime
        
        // Check if this is likely a final state notification (SUCCESS, ERROR, CANCELED)
        // by checking the notification content before building
        val builder = builderPair.second
        val subText = builder.build().extras?.getCharSequence("android.subText")?.toString() ?: ""
        val isFinalState = subText.contains("success", ignoreCase = true) || 
                          subText.contains("error", ignoreCase = true) || 
                          subText.contains("canceled", ignoreCase = true)
        
        // Skip update if it's too soon (always allow final states through)
        if (!isFinalState && timeSinceLastUpdate < MIN_NOTIFICATION_UPDATE_INTERVAL_MS) {
            // Skip this update to prevent excessive Binder calls
            return
        }
        
        // Update last notification time
        lastNotificationUpdateTime[notificationId] = currentTime
        
        // Build notification only when we're actually going to show it
        val notification = builder.build()
        
        // Use NotificationManagerCompat which handles Binder calls more efficiently
        // This helps prevent ANRs from slow Binder calls to the system server
        try {
            notificationManagerCompat.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Fallback to regular NotificationManager if permission issue
            Log.e("NotificationsHelper", "Failed to show notification with NotificationManagerCompat", e)
            notificationManager.notify(notificationId, notification)
        }
    }
    
    /**
     * Ensures notification channel is created. Uses double-checked locking pattern
     * to prevent multiple Binder calls while ensuring thread safety.
     */
    private fun ensureChannelCreated() {
        if (!channelCreated) {
            synchronized(this) {
                if (!channelCreated) {
                    createChannel(context)
                    channelCreated = true
                }
            }
        }
    }


    fun hideNotification(id: Int) {
        notificationManager.cancel(id)
    }



    private fun createChannel(appContext: Context) {
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = appContext.applicationInfo.loadLabel(appContext.packageManager)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.setSound(null, null)

            val channelName = context.getString(com.avd.R.string.app_download_channel_id)
            channel.description = channelName
            // Add the channel
            notificationManager.createNotificationChannel(channel)
        }
    }


}