package com.avd.browserkit.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.avd.browserkit.util.BrowserKitLog

/**
 * Handles Cancel action from the browser download progress notification.
 */
class BrowserDownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_CANCEL) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
        BrowserKitLog.i("Notif", "CancelReceiver taskId=$taskId")
        if (taskId.isBlank()) return
        BrowserDownloadManager.init(context.applicationContext)
        BrowserDownloadManager.cancel(taskId)
    }

    companion object {
        const val ACTION_CANCEL = "com.avd.browserkit.download.ACTION_CANCEL"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
