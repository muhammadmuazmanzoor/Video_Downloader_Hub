package com.video.avd.utils.firebasepushnotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.video.avd.R
import com.video.avd.ui.MainActivity
import com.video.avd.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFirebaseMessagingService :  FirebaseMessagingService(){

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e("TokenFireBase","onNewToken "+token)
        AppUtils.registerTokenToSingular(token)
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.data.isNotEmpty().let {
            createNotificationChannel()
            val type = message.data["type"]
            val title = message.notification?.title
            val desc = message.notification?.body
            val image=message.notification?.imageUrl

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            when (type) {
                "1" -> intent.putExtra("fragment", "home")
                "2" -> intent.putExtra("fragment", "music")
                "3" -> intent.putExtra("fragment", "download")
                "4" -> intent.putExtra("youtubelink", message.data["youtubelink"])
                "5" -> intent.putExtra("fragment", "status_saver")
                "6" -> intent.putExtra("url",message.data["url"])
                else -> {} // Optional: Handle unknown type or default case
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            try {
                // Build and show notification
                if (title != null) {
                    if (desc != null) {
                        if (image != null){
                            CoroutineScope(Dispatchers.Main).launch {
                                val imag= loadImageFromUrl(this@MyFirebaseMessagingService,image.toString())
                                imag?.let { it1 -> createimagenotification(it1,pendingIntent,title,desc) }
                            }
                        }else{
                            showNotification(pendingIntent, title, desc)
                        }
                    }
                }
            }catch (e:Exception){
                e.printStackTrace()
            }

        }

    }

    private fun showNotification(pendingIntent: PendingIntent,title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, "MY_CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher) // replace with your own icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.default_notification_channel_id) // for user-visible name of the channel
            val descriptionText = getString(R.string.notification_title) // description
            val importance = NotificationManager.IMPORTANCE_HIGH // choose importance level
            val channel = NotificationChannel("MY_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createimagenotification(thumbnail : Bitmap, pendingIntent: PendingIntent, title: String, body: String){
        // Create the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, "MY_CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher) // replace with your own icon
            .setContentTitle(title)
            .setContentText(body)
            .setLargeIcon(thumbnail)
            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(thumbnail))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Use high priority for immediate notification
            .setAutoCancel(true) // Dismiss notification after it's been touched
            .setContentIntent(pendingIntent)


        // Notification ID is a unique integer for each notification that you must define
        notificationManager.notify(1234, notificationBuilder.build())
    }

//    suspend fun loadVideoThumbnail(context: Context, videoPath: String): Bitmap? = withContext(Dispatchers.IO) {
//        suspendCancellableCoroutine { continuation ->
//            try {
//                Glide.with(context)
//                    .asBitmap()
//                    .load(videoPath) // Load from the video file path
//                    .apply(RequestOptions().frame(5000)) // Fetch frame at 1 second
//                    .into(object : CustomTarget<Bitmap>() {
//                        override fun onLoadCleared(placeholder: Drawable?) {
//                            // This can be ignored for this use case
//                        }
//
//                        override fun onLoadFailed(errorDrawable: Drawable?) {
//                            val exception = RuntimeException("Failed to load image from $videoPath")
//                            Log.e("ThumbnailLoader", "Error loading thumbnail", exception)
//                            continuation.resume(null)
//                        }
//
//                        override fun onResourceReady(
//                            resource: Bitmap,
//                            transition: Transition<in Bitmap>?
//                        ) {
//                            continuation.resume(resource)
//                        }
//                    })
//            } catch (e: Exception) {
//                Log.e("ThumbnailLoader", "Exception while loading thumbnail", e)
//                continuation.resume(null)
//            }
//        }
//    }

    suspend fun loadImageFromUrl(context: Context, imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val futureTarget: FutureTarget<Bitmap> = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()

                return@withContext futureTarget.get()
            } catch (e: Exception) {
                Log.e("LoadImageError", "Failed to load image", e)
                null
            }
        }
    }






}