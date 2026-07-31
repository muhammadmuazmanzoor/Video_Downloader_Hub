package com.avd.browserkit.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.avd.browserkit.BrowserKitActivity
import com.avd.browserkit.R
import com.avd.browserkit.ui.player.BrowserPlayerActivity
import com.avd.browserkit.util.BrowserKitLog
import java.io.File

object BrowserFileStorage {
    fun outputFile(context: Context, title: String, taskId: String, ext: String): File {
        val dir = File(context.getExternalFilesDir("BrowserDownloads"), "")
        if (!dir.exists()) dir.mkdirs()
        val safeName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64)
        return File(dir, "${safeName}_$taskId.$ext")
    }
}

object BrowserDownloadNotifier {
    private const val CHANNEL_PROGRESS = "browserkit_downloads_progress_v2"
    private const val CHANNEL_COMPLETE = "browserkit_downloads_complete_v2"

    fun progressId(taskId: String): Int {
        var id = taskId.hashCode()
        if (id == Int.MIN_VALUE) id = 0
        return kotlin.math.abs(id)
    }

    fun completeId(taskId: String): Int = progressId(taskId) xor 0x424B434D

    /** Immediate notification when download is queued / starts. */
    fun showStarted(context: Context, taskId: String, title: String) {
        BrowserKitLog.i("Notif", "STARTED id=${progressId(taskId)} taskId=$taskId title=$title")
        notifyProgress(context, taskId, title, percent = 0, indeterminate = true, started = true)
    }

    fun showProgress(context: Context, taskId: String, title: String, percent: Int) {
        val indeterminate = percent <= 0
        if (percent == 0 || percent % 25 == 0 || percent >= 100) {
            BrowserKitLog.d("Notif", "PROGRESS taskId=$taskId percent=$percent")
        }
        notifyProgress(
            context,
            taskId,
            title,
            percent = percent.coerceIn(0, 100),
            indeterminate = indeterminate,
            started = indeterminate,
        )
    }

    fun showComplete(context: Context, taskId: String, title: String, filePath: String? = null) {
        val manager = manager(context)
        ensureChannels(manager, context)
        manager.cancel(progressId(taskId))

        val playIntent = playPendingIntent(context, taskId, title, filePath)
        val openIntent = openBrowserPendingIntent(context, completeId(taskId))
        BrowserKitLog.i(
            "Notif",
            "COMPLETE taskId=$taskId playPi=${playIntent != null} path=$filePath",
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.bk_download_complete))
            .setContentText(title.ifBlank { context.getString(R.string.bk_download) })
            .setSubText(context.getString(R.string.bk_notification_tap_play))
            .setContentIntent(playIntent ?: openIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (playIntent != null) {
                    addAction(
                        android.R.drawable.ic_media_play,
                        context.getString(R.string.bk_play),
                        playIntent,
                    )
                }
            }
            .build()
        manager.notify(completeId(taskId), notification)
    }

    fun showFailed(context: Context, taskId: String, title: String) {
        BrowserKitLog.e("Notif", "FAILED taskId=$taskId title=$title")
        val manager = manager(context)
        ensureChannels(manager, context)
        manager.cancel(progressId(taskId))

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.bk_download_failed))
            .setContentText(title.ifBlank { context.getString(R.string.bk_download) })
            .setContentIntent(openBrowserPendingIntent(context, completeId(taskId)))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(completeId(taskId), notification)
    }

    fun showCancelled(context: Context, taskId: String) {
        BrowserKitLog.i("Notif", "CANCELLED taskId=$taskId")
        val manager = manager(context)
        ensureChannels(manager, context)
        manager.cancel(progressId(taskId))
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setContentTitle(context.getString(R.string.bk_download_cancelled))
            .setContentText(context.getString(R.string.bk_download))
            .setAutoCancel(true)
            .setTimeoutAfter(4_000L)
            .build()
        manager.notify(completeId(taskId), notification)
    }

    fun dismiss(context: Context, taskId: String) {
        val manager = manager(context)
        manager.cancel(progressId(taskId))
        manager.cancel(completeId(taskId))
    }

    fun foregroundInfo(context: Context, taskId: String, title: String, percent: Int): ForegroundInfo {
        val notification = buildProgressNotification(
            context,
            taskId,
            title,
            percent.coerceIn(0, 100),
            indeterminate = percent <= 0,
            started = percent <= 0,
        )
        return ForegroundInfo(
            progressId(taskId),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun notifyProgress(
        context: Context,
        taskId: String,
        title: String,
        percent: Int,
        indeterminate: Boolean,
        started: Boolean,
    ) {
        val manager = manager(context)
        ensureChannels(manager, context)
        manager.notify(
            progressId(taskId),
            buildProgressNotification(context, taskId, title, percent, indeterminate, started),
        )
    }

    private fun buildProgressNotification(
        context: Context,
        taskId: String,
        title: String,
        percent: Int,
        indeterminate: Boolean,
        started: Boolean,
    ): Notification {
        ensureChannels(manager(context), context)
        val contentText = when {
            started || indeterminate -> context.getString(R.string.bk_notification_download_started)
            else -> context.getString(R.string.bk_notification_percent, percent)
        }
        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title.ifBlank { context.getString(R.string.bk_download) })
            .setContentText(contentText)
            .setSubText(context.getString(R.string.bk_notification_tap_progress))
            .setContentIntent(openBrowserPendingIntent(context, progressId(taskId)))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setProgress(100, percent, indeterminate || started)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.bk_cancel),
                cancelPendingIntent(context, taskId),
            )
            .build()
    }

    private fun cancelPendingIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, BrowserDownloadCancelReceiver::class.java).apply {
            action = BrowserDownloadCancelReceiver.ACTION_CANCEL
            putExtra(BrowserDownloadCancelReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            progressId(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openBrowserPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, BrowserKitActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun playPendingIntent(
        context: Context,
        taskId: String,
        title: String,
        filePath: String?,
    ): PendingIntent? {
        val path = filePath?.takeIf { it.isNotBlank() } ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val intent = Intent(context, BrowserPlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BrowserPlayerActivity.EXTRA_PATH, file.absolutePath)
            putExtra(Intent.EXTRA_TITLE, title)
        }
        return PendingIntent.getActivity(
            context,
            completeId(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun manager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun ensureChannels(manager: NotificationManager, context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_PROGRESS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    context.getString(R.string.bk_notification_channel_progress),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.bk_section_browser_downloads)
                    setShowBadge(false)
                },
            )
        }
        if (manager.getNotificationChannel(CHANNEL_COMPLETE) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_COMPLETE,
                    context.getString(R.string.bk_notification_channel_complete),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.bk_section_browser_downloads)
                    setShowBadge(false)
                },
            )
        }
        // Drop old low-importance channel if present (silent / easy to miss).
        runCatching { manager.deleteNotificationChannel("browserkit_downloads") }
    }
}
