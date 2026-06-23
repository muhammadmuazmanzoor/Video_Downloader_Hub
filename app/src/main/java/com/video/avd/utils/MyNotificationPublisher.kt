package com.video.avd.utils
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.video.avd.R
import com.video.avd.ui.splash.SplashActivity
import java.util.Locale


class MyNotificationPublisher : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            // Assuming you're storing the user's preferred language in SharedPreferences
            val prefs = AppPreference.getLanguage(context).toString()
            // Adjust the locale based on the user's preference
            val localizedContext = adjustLocale(context, prefs)
            localizedContext?.let {
                createNotificationChannel(localizedContext)
                // Intent to start the main activity of your app when the notification is tapped
                val intentToOpenApp = Intent(localizedContext, SplashActivity::class.java) // Use the appropriate class that represents the main activity of your app
                intentToOpenApp.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intentToOpenApp.putExtra("opened_from_notification", true)
                intentToOpenApp.putExtra("notification_id", NOTIFICATION_ID)
                val pendingIntentToOpenApp = PendingIntent.getActivity(localizedContext, 0, intentToOpenApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notificationManager = localizedContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notification = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
                    .setContentTitle(localizedContext.resources.getString(R.string.notification_title))
                    .setContentText(localizedContext.resources.getString(R.string.notification_desc))
                    .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app's notification icon
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntentToOpenApp) // Set the PendingIntent that will be sent when the notification is clicked
                    .setAutoCancel(true) // Automatically removes the notification when tapped
                    .build()
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Video Player Notifications"
            val descriptionText = "Notification channel for Video Player app"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "video_player_notification_channel"
        const val NOTIFICATION_ID = 100
    }

    fun adjustLocale(context: Context, languageCode: String): Context? {
        try {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        } catch (e: Exception) {
            // Log the exception or handle it as necessary
            Log.e("LocaleAdjust", "Failed to adjust locale", e)
        }
        return context
    }
}