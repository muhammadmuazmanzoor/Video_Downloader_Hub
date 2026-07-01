package com.video.avd.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.singular.sdk.Singular
import com.video.avd.MyApplication.Companion.context
import com.video.avd.R
import com.video.avd.ui.MainActivity
import com.video.avd.ui.splash.SplashActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Formatter
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object AppUtils {
    val IS_ONBOARD = booleanPreferencesKey("onboarding-check")

    val IS_LANGUAGE = stringPreferencesKey("language")
    var isFromTheme: Boolean = false
    var isSmoothScrolled: Boolean = false
    var statusPermission: Boolean = false
    var downloaderShown:Boolean=false
    var isFirstlaunch =false
    var duration: Int = 0
    var localeLanguage:String?=null
    var localeCountry:String?=null
    const val TAG = "HD_VIDEO_PLAYER_TAG"
    const val SHARED_PROVIDER_AUTHORITY = "com.bpva.video.player.free" + ".provider"
    const val URLShare = "https://play.google.com/store/apps/details?id="
    var firebaseAnalytics: FirebaseAnalytics? = null
    var startTimeMillis: Long = 0
    var totalForegroundTimeMillis: Long = 0
    var wasRunning: Boolean = false


    var hasNotifiedThisSession = false           // 1‑per‑session guard
    val channelId = "bg_alert"

    fun maybeShowNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // You cannot pop a system request from here (app is already backgrounded).
            // Instead remember to ask next time the user opens the app (see TIP #2)
            Log.d("AppLife", "blocked ‑ permission not granted")
            return
        }
        createChannelIfNeeded(context)
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.noti)           // 24×24 px
            .setContentTitle("Don’t Miss the Moment.")
            .setContentText("One tap to download reels, shorts, stories & more.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)      // request banner
            .setDefaults(NotificationCompat.DEFAULT_ALL)        // sound + vibration
            .setCategory(NotificationCompat.CATEGORY_REMINDER)  // optional, helps ranking
            .setContentIntent(makePendingIntent(context))
            .setAutoCancel(true)
            .build()
        Log.d("AppLife", "calling notify()")
        NotificationManagerCompat.from(context).notify(1001, notif)
        hasNotifiedThisSession = true
        AppUtils.firebaseUserAction("NotificationBuiltin", "Notification")
    }

    private fun makePendingIntent(ctx: Context): PendingIntent {
        val splashIntent = Intent(ctx, SplashActivity::class.java).apply {
            // kill any running task then start Splash as the root activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Optional: mark that we came from the reminder notification
            action = "videoplayer.tfl.downloader.videodownloader.app.ACTION_REBOOT_FROM_NOTIF"
            putExtra("internalnotification","internalnotification")
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE    or
                PendingIntent.FLAG_ONE_SHOT      // ignore double‑taps

        return PendingIntent.getActivity(ctx, 0, splashIntent, pendingFlags)
    }

    private fun createChannelIfNeeded(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return          // pre‑O devices ignore channels

        val nm = ctx.getSystemService(NotificationManager::class.java)

        if (nm.getNotificationChannel(channelId) == null) {
            val chan = NotificationChannel(
                channelId,
                "Background reminders",
                NotificationManager.IMPORTANCE_HIGH       // <-- heads‑up
            ).apply {
                description = "Notifies you when the app is in background"
                enableVibration(true)
                enableLights(true)
            }
            nm.createNotificationChannel(chan)
        }
    }





    fun registerTokenToSingular(fcmDeviceToken: String) {
        try {
            Singular.setFCMDeviceToken(fcmDeviceToken)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun convertToEnglishDate(isoDate: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use java.time for Android API 26 and above
            val date = ZonedDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME)
            val outputFormat = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            date.format(outputFormat)
        } else {
            // Use SimpleDateFormat for lower API levels
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(isoDate)
            outputFormat.format(date!!)
        }
    }

    fun convertMillisecondsToMinutes(milliseconds: Long): Long {
        return milliseconds / 60_000
    }

    fun fetchDataFromFirebase(queryKey:String):String? {
        var json:String?=""
        val database = FirebaseDatabase.getInstance().reference
        val youTubeDataRef = database.child("youtubeDataApi").child(queryKey)
        youTubeDataRef.get().addOnSuccessListener { dataSnapshot ->
            json = dataSnapshot.getValue(String::class.java)
            Log.e("checkYoutubeData","Fetched from Firebase: $json")
            // Parse and use the JSON response
        }.addOnFailureListener {
            // Handle error
        }
        return json
    }

    fun changeStatusBarContrastStyle(window: Window, lightIcons: Boolean) {
//        val decorView = window.decorView
//        if (lightIcons) {
//            // Draw light icons on a dark background color
//            decorView.systemUiVisibility =
//                decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
//        } else {
//            // Draw dark icons on a light background color
//            decorView.systemUiVisibility =
//                decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//        }
    }

    fun calculateProgress(lastPlayedTime: Long, totalDuration: Long): Int {
        if (totalDuration == 0L) {
            return 0 // Avoid division by zero
        }
        val progress = (lastPlayedTime.toDouble() / totalDuration.toDouble()) * 100
        return progress.toInt()
    }

    fun changestatusandnavbarSplash(context:Activity){
//        context.window.apply {
//            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//            navigationBarColor=Color.WHITE
//            statusBarColor = Color.TRANSPARENT
//        }
    }

    fun Activity.showStatusBar(color:Int) {
//            window.apply {
//                clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//                addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) // Optionally, if you want a translucent effect
//                decorView.systemUiVisibility = 0 // Clear all system UI flags
//                statusBarColor = ContextCompat.getColor(this@showStatusBar, android.R.color.black) // Or any default color you prefer
//            }
    }

    fun parseDurationString(durationStr: String): Long {
        val parts = durationStr.split(":")

        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid duration string format")
        }

        try {
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val seconds = parts[2].toLong()

            return (hours * 3600000) + (minutes * 60000) + (seconds * 1000)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid number format in duration string")
        }
    }

    fun rateus(context: Context) {
        try {
            val pkg = context.packageName
            val rateIntent = Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=$pkg"
                )
            )
            context.startActivity(rateIntent)
        } catch (e: java.lang.Exception) {
            ToastUtils.showErrorToast(context)
        }

    }

    fun isMp4(filePath: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use MediaExtractor for API level 26 and above
            val extractor = MediaExtractor()
            extractor.setDataSource(filePath)
            val metrics = extractor.metrics
            val format = metrics.getString(MediaExtractor.MetricsConstants.FORMAT)
            extractor.release()
            return format != null && format.contains("mp4")
        } else {
            // Use MediaMetadataRetriever for API level 21 and above
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            retriever.release()
            return mimeType != null && mimeType.contains("video/mp4")
        }
    }


    fun isYouTubeVideoLink(url: String): Boolean {
        return url.contains("youtu.be", ignoreCase = true)
    }


    fun isSupportedVideoFile(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType in listOf("video/mp4", "video/webm", "video/mp2t")
    }

    fun setViewMargins(
        view: View,
        marginLeft: Int,
        marginTop: Int,
        marginRight: Int,
        marginBottom: Int
    ) {
        val layoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.setMargins(marginLeft, marginTop, marginRight, marginBottom)
        view.layoutParams = layoutParams
    }

    fun transparentStausBar(activity: FragmentActivity) {
//        activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
//        activity.window.statusBarColor = Color.TRANSPARENT
//        Log.d("statusbar_transparent","")
    }


    fun changeStatusBarColor(color: Int, context: FragmentActivity?, dark: Boolean = false) {
        context?.let { activity ->
            val window = activity.window
            window.statusBarColor = ContextCompat.getColor(activity, color)

            // Preserve the current system UI visibility flags
            val currentFlags = window.decorView.systemUiVisibility

            window.decorView.systemUiVisibility = if (dark) {
                // Add LIGHT_STATUS_BAR flag to make status bar text/icons dark
                currentFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                // Remove LIGHT_STATUS_BAR flag for light text/icons
                currentFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    fun changeStatusBarColor(hexColor: String, context: FragmentActivity?, dark: Boolean = false) {
        context?.let { activity ->
            val window = activity.window

            // Parse the hex color string and set it as the status bar color
            val color = Color.parseColor(hexColor)
            window.statusBarColor = color

            // Preserve the current system UI visibility flags
            val currentFlags = window.decorView.systemUiVisibility

            window.decorView.systemUiVisibility = if (dark) {
                // Add LIGHT_STATUS_BAR flag to make status bar text/icons dark
                currentFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                // Remove LIGHT_STATUS_BAR flag for light text/icons
                currentFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }


    fun setLocate(activity: Activity) {
        var lang = Locale.getDefault().language //System Default Language
        localeLanguage=lang
        if (AppPreference.getLanguage(activity)==null){
            //no language selected from app. so set the system's default language if app supported
            // List of supported languages in your app
            val supportedLangs = listOf("ar","ko", "ja", "es","in","pt","fr", "vi","ru","tr", "ms","th","pl")

            // Check if the system language is in the list of supported languages, else default to English
            var lange = if (lang in supportedLangs) lang else "en"
            lang = lange
        }else{
            // language is selected from app
            lang = AppPreference.getLanguage(activity).toString()
        }

        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        activity.baseContext.resources.updateConfiguration(
            config,
            activity.baseContext.resources.displayMetrics
        )
    }

    fun convertLongToDate(timestamp: Long): String {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .format(
                Instant.ofEpochMilli(timestamp * 1000)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            )
    }

    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return try {
            when {
                size < kb -> "$size B"
                size < mb -> "%.2f KB".format(size / kb.toDouble())
                size < gb -> "%.2f MB".format(size / mb.toDouble())
                else -> "%.2f GB".format(size / gb.toDouble())
            }
        }catch (e:Exception){
            e.printStackTrace()
            return ""
        }

    }

    fun getDeviceLanguages(): String {
        val locales: MutableList<String> = ArrayList()
        if (Build.VERSION.SDK_INT >= 24) {
            val localeList = Resources.getSystem().configuration.locales
            for (i in 0 until localeList.size()) {
                locales.add(localeList[i].isO3Language)
            }
        } else {
            val locale = Resources.getSystem().configuration.locale
            locales.add(locale.isO3Language)
        }
        return locales.toTypedArray()[0]
    }

    fun loadVideoThumbIntoFragmentImageView(fragment: Fragment?, view: ImageView, path: String) {
        val context = view.context
        val thumbWidth = 111
        val thumbHeight = roundFloat(thumbWidth * 9f / 16f)
        val lp = view.layoutParams
        if (lp.width != thumbWidth || lp.height != thumbHeight) {
            lp.width = thumbWidth
            lp.height = thumbHeight
            view.layoutParams = lp
        }
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        val requestManager: RequestManager = fragment?.let { Glide.with(it) } ?: Glide.with(context)
        requestManager
            .load(path)
            .override(thumbWidth, thumbHeight)
            .centerCrop()
            .placeholder(R.drawable.ic_video)
            .into(view)
    }

    fun roundFloat(value: Float): Int {
        return (if (value > 0) value + 0.5f else value - 0.5f).toInt()
    }

    fun convertDurationToTime(duration: Long): Double {
        val seconds = duration / 1000L
        val minutes = seconds / 60L
        val remainingSeconds = seconds % 60L
        val timeString = String.format("%d.%02d", minutes, remainingSeconds)
        return timeString.toDouble()
    }


    fun addFragment(id: Int, activity: AppCompatActivity, fragment: Fragment, tag: String) {
        try {
            val fragmentManager = activity.supportFragmentManager
            val existingFragment = fragmentManager.findFragmentByTag(tag)
            val fragmentTransaction = fragmentManager.beginTransaction()
            if (existingFragment == null) {
                fragmentTransaction.add(id, fragment, tag)
            } else {
                fragmentTransaction.replace(id, existingFragment, tag)
            }
            fragmentTransaction.addToBackStack(tag)
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            fragmentTransaction.commit()
        } catch (e: java.lang.Exception) {
        }
    }

    fun hideFragment(activity: AppCompatActivity, tag: String) {
        val fragment = activity.supportFragmentManager.findFragmentByTag(tag)
        if (fragment != null && fragment.isAdded && fragment.isVisible) {
            activity.supportFragmentManager.beginTransaction()
                .hide(fragment)
                .commit()
        }
    }


    fun setActionBarColor(drawableResId: Int, activity: Activity) {
        activity.actionBar?.setBackgroundDrawable(activity.resources.getDrawable(drawableResId))
    }

    suspend fun getbitmap(id: Long): Bitmap? {
        var bitmap: Bitmap? = null
        val job = CoroutineScope(Dispatchers.IO).async {
            var options = BitmapFactory.Options()
            options.inSampleSize = 1
            var curThumb = MediaStore.Video.Thumbnails.getThumbnail(
                context?.contentResolver,
                id,
                MediaStore.Video.Thumbnails.MICRO_KIND,
                options
            )
            bitmap = curThumb
        }
        job.await()
        return bitmap
    }

    fun createThumbnailAtTime(id: Long, context: Context): Bitmap? {
        val crThumb = context.contentResolver
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
        }
        val curThumb = MediaStore.Video.Thumbnails.getThumbnail(
            crThumb,
            id,
            MediaStore.Video.Thumbnails.MICRO_KIND,
            options
        )
        return curThumb
    }


    fun getDurationString(duration: Long): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val stringBuilder = StringBuilder()
        stringBuilder.append(String.format("%02d:", hours))
        stringBuilder.append(String.format("%02d:", minutes))
        stringBuilder.append(String.format("%02d", seconds))
        return stringBuilder.toString()
    }

    fun convertToMilliseconds(timeStr: String): Long {
        val parts = timeStr.split(":")
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val seconds = parts[2].toLong()
        return (hours * 3600 + minutes * 60 + seconds) * 1000
    }

    suspend fun getDurationStringInBackground(duration: Long): String =
        withContext(Dispatchers.Default) {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            val stringBuilder = StringBuilder()
            stringBuilder.append(String.format("%02d:", hours))
            stringBuilder.append(String.format("%02d:", minutes))
            stringBuilder.append(String.format("%02d", seconds))
            return@withContext stringBuilder.toString()
        }


    fun formatevideodate(videodate: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = Date(videodate) // Assuming videoDateInMillis is the timestamp in milliseconds
        val formattedDate = dateFormat.format(date) // Returns the formatted date string
        return formattedDate
    }

    fun getVideoSizeInMBs(sizeInBytes: Long): Double {
        val sizeInMBs = sizeInBytes.toDouble() / (1024 * 1024)
        return String.format("%.2f", sizeInMBs).toDouble()
    }

    fun formatMilisSign(time: Long): String {
        return if (time > -1000 && time < 1000) formatMilis(time) else (if (time < 0) "−" else "+") + formatMilis(
            time
        )
    }


    fun convertMBtoBytes(sizeString: String): Int {
        return try {
            // Remove all non-numeric characters except for '.' and 'E'/'e' for scientific notation
            val numberPart = sizeString.replace(Regex("[^\\d.Ee]"), "")
            val size = numberPart.toDouble()
            when {
                sizeString.contains("GB", ignoreCase = true) -> (size * 1024 * 1024 * 1024).toInt()
                sizeString.contains("MB", ignoreCase = true) -> (size * 1024 * 1024).toInt()
                sizeString.contains("KB", ignoreCase = true) -> (size * 1024).toInt()
                // Add additional cases for GB, TB, etc. if necessary
                else -> 0 // If unit is not recognized, return 0
            }
        } catch (e: NumberFormatException) {
            // Handle the exception if the string is not a valid number
            0
        }
    }


    fun formatMilis(time: Long): String {
        val totalSeconds = Math.abs(time.toInt() / 1000)
        val seconds = totalSeconds % 60
        val minutes = totalSeconds % 3600 / 60
        val hours = totalSeconds / 3600
        return if (hours > 0) String.format(
            "%d:%02d:%02d", hours, minutes, seconds
        ) else String.format("%02d:%02d", minutes, seconds)
    }

    fun getMain(activity: FragmentActivity?): MainActivity {
        return activity as MainActivity
    }


    fun getFilePathFromContentUri(contentUri: Uri, context: Context): String? {
         try {
            var filePath: String? = null
            // Query the media store for the file path associated with the content URI
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? =
                context.contentResolver.query(contentUri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = cursor.getString(columnIndex)
                cursor.close()
            }
            return filePath
        }catch (e:Exception){
            e.printStackTrace()
            return ""
        }
    }

    fun deleteVideoFile(context: Context, videoUri: Uri): Boolean {
        val rowsDeleted = context.contentResolver.delete(videoUri, null, null)
        return rowsDeleted > 0
    }

    fun deleteVideoFiles(context: Context, videoUris: List<Uri?>): Boolean {
        var allDeleted = true
        Log.e("checkCleanClick","deleteVideoFiles: True")
        for (uri in videoUris) {
            val rowsDeleted = uri?.let { context.contentResolver.delete(it, null, null) }
            if (rowsDeleted != null) {
                if (rowsDeleted <= 0) {
                    allDeleted = false
                    // Optionally handle the specific case of a file not being deleted,
                    // such as logging an error or notifying the user.
                }
            }
        }

        return allDeleted
    }


    fun deleteAudioFile(context: Context, audioUri: Uri): Boolean {
        val rowsDeleted = context.contentResolver.delete(audioUri, null, null)
        return rowsDeleted > 0
    }

    suspend fun getPathsFromUris(context: Context, uris: List<Uri?>): List<String> =
        withContext(Dispatchers.IO) {

            val filePaths = mutableListOf<String>()
            uris?.forEach { uri ->
                try {
                    val projection = arrayOf(MediaStore.Images.Media.DATA)
                    if (uri != null) {
                        context.contentResolver.query(uri, projection, null, null, null)
                            ?.use { cursor ->
                                val columnIndex =
                                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                                if (cursor.moveToFirst()) {
                                    filePaths.add(cursor.getString(columnIndex))
                                }

                            }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            filePaths
        }


    fun getPathFromUri(context: Context, uri: Uri): String? {
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                val path = cursor.getString(columnIndex)
                cursor.close()
                return path
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getAudioPathFRomUri(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close() // Ensure the cursor is closed
        }
        return null
    }


    infix fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }
        return result
    }


    fun firebaseUserAction(action: String, activityName: String) {
        /*CoroutineScope(Dispatchers.IO).launch {
            Singular.event(action)
            context?.let {
                if(NetworkUtils.isOnline(it)){
                    if (FirebaseApp.getApps(it).isEmpty()) {
                        FirebaseApp.initializeApp(it)
                    } else {
                        if (firebaseAnalytics == null) {
                            firebaseAnalytics = Firebase.analytics
                        }
                        firebaseAnalytics?.let { analytics ->
                            analytics.logEvent(action) {
                                param("Screen_Name", activityName)
                            }
                        }
                    }
                }

            }
        }*/
    }

    fun fbEvents(action: String, activityName: String,context: Context?=null) {
        CoroutineScope(Dispatchers.IO).launch {
            context?.let { ctx ->
                if (NetworkUtils.isOnline(ctx)) {
                    // Initialize if not already done
                    if (FirebaseApp.getApps(ctx).isEmpty()) {
                        FirebaseApp.initializeApp(ctx)
                    }

                    // Always log after ensuring initialized
                    if (firebaseAnalytics == null) {
                        firebaseAnalytics = Firebase.analytics
                    }

                    firebaseAnalytics?.let { analytics ->
                        analytics.logEvent(action) {
                            param("Screen_Name", activityName)
                        }
                    }
                }
            }
        }
    }


    fun getOriginalImagePath(contentResolver: ContentResolver, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = contentResolver.query(contentUri, projection, null, null, null)
            cursor?.let {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                it.moveToFirst()
                it.getString(columnIndex)
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }


    fun shareAudio(context: Context, audioFile: File) {
        try {
            if (audioFile.exists()) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "audio/mp3"
                val audioUri: Uri = FileProvider.getUriForFile(
                    context, "video.player.videodownloader.storysaver.provider", audioFile
                )
                shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri)

                // Grant temporary permissions to the receiving app
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                val chooserIntent = Intent.createChooser(shareIntent, "Share audio")
                // Verify that the intent will resolve to at least one activity
                if (chooserIntent.resolveActivity(context.packageManager) != null) {
                    ContextCompat.startActivity(context, chooserIntent, null)
                } else {
                    Toast.makeText(
                        context,
                        "No app available to handle the share action",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Handle case when the audio file doesn't exist
                Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

    }


    ///// Exo screen short
//    fun takeScreenshotWithPixelCopy(videoView: TextureView, callback: (Bitmap?) -> Unit) {
//        val bitmap: Bitmap = Bitmap.createBitmap(
//            videoView.width, videoView.height, Bitmap.Config.ARGB_8888
//        )
//        try {
//            val handlerThread = HandlerThread("PixelCopier")
//            handlerThread.start()
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                PixelCopy.request(
//                    videoView, bitmap, { copyResult ->
//                        if (copyResult == PixelCopy.SUCCESS) {
//                            callback(bitmap)
//                        }
//                        handlerThread.quitSafely()
//                    }, Handler(handlerThread.looper)
//                )
//            }
//        } catch (e: IllegalArgumentException) {
//            callback(null)
//            e.printStackTrace()
//        } catch (e: Exception) {
//            callback(null)
//            e.printStackTrace()
//        }
//    }

    fun takeScreenshotWithPixelCopy(textureView: TextureView, callback: (Bitmap?) -> Unit) {
        try {
            if (textureView.isAvailable) {
                // Create a bitmap with the same dimensions as the texture view
                val bitmap = Bitmap.createBitmap(
                    textureView.width, textureView.height, Bitmap.Config.ARGB_8888
                )

                // Create a Surface from the SurfaceTexture
                val surface = Surface(textureView.surfaceTexture)

                // Set up a new HandlerThread for PixelCopy to use
                val handlerThread = HandlerThread("PixelCopier").apply { start() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    PixelCopy.request(surface, bitmap, { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            // If PixelCopy is successful, pass the bitmap to the callback
                            callback(bitmap)
                        } else {
                            // If PixelCopy is not successful, pass null to the callback
                            callback(null)
                        }
                        // Terminate the handler thread
                        handlerThread.quitSafely()
                        // Release the Surface
                        surface.release()
                    }, Handler(handlerThread.looper))
                }
            } else {
                // TextureView is not ready, can't get the SurfaceTexture
                callback(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Function to save the screenshot to external storage in a background thread
    fun saveScreenshotToExternalStorage(bitmap: Bitmap, onSaved: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val folderPath =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
            val fileName = getDateTimeStamp() + "screenshot.jpg"
            val file = File(folderPath, fileName)

            var outputStream: OutputStream? = null
            var isSavedSuccessfully = false

            try {
                outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                isSavedSuccessfully = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    outputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                launch(Dispatchers.Main) {
                    onSaved(isSavedSuccessfully)
                }
            }
        }
    }

    fun getDateTimeStamp(): String {
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        return dateFormat.format(currentDate)
    }

    fun convertSecondsToTimeString(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        val timeString = when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
            else -> String.format("%02d:%02d", minutes, remainingSeconds)
        }

        return timeString
    }

    suspend fun isNavigationBarAvailable(): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
                val hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)
                !(hasBackKey && hasHomeKey)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun isNavigationBarAvailableWithOtherApproach(context: Activity): Boolean {
        return try {
            val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
            val hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)

            val hasNavigationBar =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    val display =
                        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                    val realSize = Point()
                    val screenSize = Point()
                    display.getRealSize(realSize)
                    display.getSize(screenSize)
                    realSize.y != screenSize.y
                } else {
                    !hasBackKey
                }

            !(hasBackKey && hasHomeKey && hasNavigationBar)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }

    }


    suspend fun isNavigationBarAvailableWithOtherApproachAgain(context: Context): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
                val hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)

                val hasNavigationBar =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        val navBarEnabled =
                            Settings.Global.getInt(
                                context.contentResolver,
                                "force_fsg_nav_bar",
                                0
                            ) != 0
                        val hasPermanentNavBar =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                val display =
                                    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                                val realMetrics = DisplayMetrics()
                                display.getRealMetrics(realMetrics)
                                val displayMetrics = DisplayMetrics()
                                display.getMetrics(displayMetrics)
                                realMetrics.widthPixels - displayMetrics.widthPixels > 0
                            } else {
                                false
                            }
                        navBarEnabled || hasPermanentNavBar
                    } else {
                        !hasBackKey
                    }

                !(hasBackKey && hasHomeKey && hasNavigationBar)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun pxToDp(px: Int): Float {
        val density = Resources.getSystem().displayMetrics.density
        return px / density
    }

    /*fun dpToPx(context: Context, dp: Float): Int {
        val displayMetrics = context.resources.displayMetrics
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics
        ).toInt()
    }*/

    fun stringForTime(timeMs: Long): String? {
        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
            return "00:00"
        }
        val totalSeconds = timeMs / 1000
        val seconds = (totalSeconds % 60).toInt()
        val minutes = (totalSeconds / 60 % 60).toInt()
        val hours = (totalSeconds / 3600).toInt()
        val stringBuilder = StringBuilder()
        val mFormatter = Formatter(stringBuilder, Locale.getDefault())
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    fun getWindow(context: Context?): Window? {
        return if (getAppCompActivity(context) != null) {
            getAppCompActivity(context)?.window
        } else {
            scanForActivity(context)?.window
        }
    }


    fun getAppCompActivity(context: Context?): AppCompatActivity? {
        if (context == null) return null
        if (context is AppCompatActivity) {
            return context
        } else if (context is ContextThemeWrapper) {
            return getAppCompActivity(context.baseContext)
        }
        return null
    }

    fun scanForActivity(context: Context?): Activity? {
        if (context == null) return null
        if (context is Activity) {
            return context
        } else if (context is ContextWrapper) {
            return scanForActivity(context.baseContext)
        }
        return null
    }

    fun hideSoftKeyboard(activity: Activity, token: IBinder?) {
        val inputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(token, 0)
    }

    fun SearchView.showSoftKeyboard() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        // Schedule the keyboard to be shown after the view has fully gained focus.
        post {
            imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun EditText.showSoftKeyboard() {
        requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        // Schedule the keyboard to be shown after the view has fully gained focus.
        post {
            imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
    fun disableSSLCertificateChecking() {
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

            override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String) {
                // Not implemented
            }

            override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String) {
                // Not implemented
            }
        })

        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, java.security.SecureRandom())

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

            // Create all-trusting host name verifier
            val allHostsValid = HostnameVerifier { hostname, session -> true }

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        manager?.let {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }

    fun getHrsMinsSecs(milliseconds: Long): String? {
        val totalHrs = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val totalHrsInMins = TimeUnit.HOURS.toMinutes(totalHrs)
        val totalMins = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val extraMins = totalMins - totalHrsInMins
        val totalMinsInSecs = TimeUnit.MINUTES.toSeconds(totalMins)
        val totalSecs = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        val extraSecs = totalSecs - totalMinsInSecs
        return if (totalHrs > 0) {
            totalHrs.toString() + "h " + extraMins + "m " + extraSecs + "s"
        } else if (totalMins > 0) {
            totalMins.toString() + "m " + extraSecs + "s"
        } else {
            totalSecs.toString() + "s"
        }
    }

    fun createDivider(context: Context): DividerItemDecoration {
        val divider = object : DividerItemDecoration(context, DividerItemDecoration.VERTICAL) {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val verticalSpacing = Math.ceil(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, 4f,
                        context.resources.displayMetrics
                    ).toDouble()
                ).toInt()
                outRect.top = verticalSpacing
                outRect.bottom = verticalSpacing
            }
        }
        divider.setDrawable(ContextCompat.getDrawable(context, R.drawable.greydivider)!!)
        return divider
    }

    val restrictedKeywords = listOf(
        "sex",
        "porn",
        "Pornhub",
        "8Tube",
        "Redtube",
        "Kink",
        "YouJizz",
        "Xvideos",
        "massage",
        "YouPorn",
        "Brazzers",
        "Omegle",
        "adult",
        "nude",
        "erotic",
        "PalTalk",
        "TalkWithStranger",
        "ChatRoulette",
        "Chat-Avenue",
        "Chatango",
        "Teenchat",
        "Wireclub",
        "ChatHour",
        "red tube",
        "Chatzy",
        "Chatib",
        "E-Chat",
        "4chan",
        "Reddit",
        "SomethingAwful",
        "Topix",
        "Stormfront",
        "Bodybuilding",
        "KiwiFarms",
        "Voat",
        "8kun",
        "Incels",
        "Tinder",
        "Match",
        "Bumble",
        "MeetMe",
        "OKCupid",
        "Plenty of Fish",
        "eHarmony",
        "Zoosk",
        "Hinge",
        "Grindr",
        "AshleyMadison",
        "AdultFriendFinder",
        "xxx",
        "BetOnline",
        "FreeSpin",
        "Bovada",
        "SlotoCash",
        "RoyalAceCasino",
        "PokerStars",
        "888casino",
        "SportsBetting",
        "Betway",
        "Blacks",
        "LiveLeak",
        "BestGore",
        "TheYNC",
        "DocumentingReality",
        "Ogrish",
        "HackThisSite",
        "ThePirateBay",
        "WikiLeaks",
        "DarkWebLinks",
        "IllegalHack",
        "Stormfront",
        "4chan",
        "Gab",
        "NationalVanguard",
        "DailyStormer",
        "xhamster"
    )

    val blockedWebsites = listOf(
        "https://www.sex.com",
        "https://www.porn.com",
        "https://www.pornhub.com",
        "https://www.8tube.com",
        "https://www.redtube.com",
        "https://www.kink.com",
        "https://www.youjizz.com",
        "https://www.xvideos.com",
        "https://www.youporn.com",
        "https://www.redtube.com",
        "https://www.adult.com",
        "https://www.nude.com",
        "https://www.massage.com",
        "https://www.erotic.com",
        "https://www.brazzers.com",
        "https://www.omegle.com",
        "https://www.paltalk.com",
        "https://www.talkwithstranger.com",
        "https://www.chatroulette.com",
        "https://www.chat-avenue.com",
        "https://www.chatango.com",
        "https://www.teenchat.com",
        "https://www.wireclub.com",
        "https://www.chathour.com",
        "https://www.chatzy.com",
        "https://www.chatib.us",
        "https://www.e-chat.co",
        "https://www.4chan.org",
        "https://www.reddit.com",
        "https://www.somethingawful.com",
        "https://www.topix.com",
        "https://www.stormfront.org",
        "https://www.bodybuilding.com",
        "https://www.kiwifarms.net",
        "https://www.voat.co",
        "https://www.8kun.top",
        "https://www.incels.me",
        "https://www.tinder.com",
        "https://www.match.com",
        "https://www.bumble.com",
        "https://www.meetme.com",
        "https://www.okcupid.com",
        "https://www.pof.com",
        "https://www.eharmony.com",
        "https://www.zoosk.com",
        "https://www.hinge.co",
        "https://www.grindr.com",
        "https://www.ashleymadison.com",
        "https://www.adultfriendfinder.com",
        "https://www.xxx.com",
        "https://www.betonline.ag",
        "https://www.freespin.com",
        "https://www.bovada.lv",
        "https://www.slotocash.im",
        "https://www.royalacecasino.com",
        "https://www.pokerstars.com",
        "https://www.888casino.com",
        "https://www.sportsbetting.ag",
        "https://www.betway.com",
        "https://www.blacks.com",
        "https://www.liveleak.com",
        "https://www.bestgore.com",
        "https://www.theync.com",
        "https://www.documentingreality.com",
        "https://www.ogrish.tv",
        "https://www.hackthissite.org",
        "https://www.thepiratebay.org",
        "https://www.wikileaks.org",
        "https://www.darkweblinks.net",
        "https://www.illegalhack.com",
        "https://www.stormfront.org",
        "https://www.4chan.org",
        "https://www.gab.com",
        "https://www.nationalvanguard.org",
        "https://www.dailystormer.su",
        "https://www.xhamster.com"
    )

    fun isContentAllowed(input: String): Boolean {
        val lowerInput = input.lowercase()
        val words = lowerInput.split("\\s+".toRegex()).map { it.trim() }
        if (words.any { it in restrictedKeywords }) {
            return false
        }
        if (blockedWebsites.any { it in lowerInput }) {
            return false
        }
        return true
    }

    fun openWarningDialog(context: FragmentActivity?) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.blocked_sites_warning_dialogue, null)
        val btnOk = view.findViewById<TextView>(R.id.btn_oks)

        val builder = android.app.AlertDialog.Builder(context)
        builder.setView(view)

        val alertDialog = builder.create()
        btnOk.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.setOnShowListener {
            // Set the width to 80% of the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width =
                (context?.resources?.displayMetrics?.widthPixels?.times(0.85))?.toInt()!!
            alertDialog.window?.attributes = layoutParams
        }
        alertDialog.show()
    }

    fun getSubtitleTextColor(code: String): Int {
        return when (code) {
            "#FFFFF" -> Color.WHITE
            "#0075FF" -> Color.parseColor("#006292")
            "#FF0000" -> Color.RED
            "#008000" -> Color.GREEN
            "#FFFF00" -> Color.YELLOW
            "#00FFFF" -> Color.CYAN
            "#FF00FF" -> Color.MAGENTA
            else -> Color.WHITE
        }
    }

    fun getBaseDomain(url: String?): String? {
        val host: String = getHost(url)
        var startIndex = 0
        var nextIndex = host.indexOf('.')
        val lastIndex = host.lastIndexOf('.')
        while (nextIndex < lastIndex) {
            startIndex = nextIndex + 1
            nextIndex = host.indexOf('.', startIndex)
        }
        return if (startIndex > 0) {
            host.substring(startIndex)
        } else {
            host
        }
    }

    fun getHost(url: String?): String {
        if (url == null || url.length == 0) return ""
        var doubleslash = url.indexOf("//")
        if (doubleslash == -1) doubleslash = 0 else doubleslash += 2
        var end = url.indexOf('/', doubleslash)
        end = if (end >= 0) end else url.length
        val port = url.indexOf(':', doubleslash)
        end = if (port > 0 && port < end) port else end
        return url.substring(doubleslash, end)
    }

    fun isSuspiciousUrl(url: String): Boolean {
        val uri = Uri.parse(url)

        // Block URLs with suspicious domains or subdomains
        if (uri.host?.contains("1xlite") == true || uri.host?.contains("top") == true) {
            return true
        }

        // Block URLs with multiple subdomains (e.g., "sub.sub.example.com")
        if (uri.host?.split(".")?.size ?: 0 > 3) {
            return true
        }

        // Block URLs with long query strings
        if (uri.query?.length ?: 0 > 100) {
            return true
        }

        // Block URLs with suspicious keywords in the path or query
        val suspiciousKeywords = listOf("clickunder", "redirect", "ads", "tracking", "null")
        if (suspiciousKeywords.any { keyword -> url.contains(keyword) }) {
            return true
        }

        // Block URLs with unusual characters
        if (url.contains("[]") || url.contains("}{")) {
            return true
        }

        return false
    }


    fun webRequestToHttpWithCookies(request: WebResourceRequest): Request? {
        val url = request.url.toString()

        val tmpHeaders = request.requestHeaders
        tmpHeaders["Cookie"] = try {
            CookieManager.getInstance().getCookie(url) ?: CookieManager.getInstance()
                .getCookie(url) ?: ""
        } catch (e: Throwable) {
            ""
        }
        val verReq = try {
            Request.Builder().headers(tmpHeaders.toHeaders()).url(url).build()
        } catch (e: Throwable) {
            null
        }

        return verReq
    }

    fun getFinalRedirectURL(url: URL, headers: Map<String, String>): Pair<URL, Headers>? {
        val currentHeaders = headers.toMutableMap()

        try {
            val con = url.openConnection() as HttpURLConnection
            con.instanceFollowRedirects = false
            for (header in currentHeaders) con.setRequestProperty(header.key, header.value)
            try {
                con.connect()
            } catch (_: Throwable) {

            }
            val resCode = con.responseCode
            if (resCode == HttpURLConnection.HTTP_SEE_OTHER || resCode == HttpURLConnection.HTTP_MOVED_PERM || resCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                var location = con.getHeaderField("Location")

                val origin = con.getHeaderField("Access-Control-Allow-Origin")

                if (location.startsWith("/")) {
                    location = if (location.startsWith("//")) {
                        url.protocol + "://" + location.replace("//", "")
                    } else {
                        url.protocol + "://" + url.host + location
                    }
                }
                if (origin != null) {
                    currentHeaders["Referer"] = origin
                }
                return getFinalRedirectURL(URL(location), currentHeaders)
            }
        } catch (_: Exception) {

        }

        return Pair(url, currentHeaders.toHeaders())
    }




    fun FragmentActivity.hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.navigationBarColor = ContextCompat.getColor(this, R.color.transparentBlackColor)
                // Set an OnApplyWindowInsetsListener to auto-hide system bars after they are shown
                window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                    if (insets.isVisible(WindowInsets.Type.systemBars())) {
                        // Hide the system bars after a delay
                        view.postDelayed({
                            controller.hide(WindowInsets.Type.systemBars())
                        }, 3000) // Delay in milliseconds (3 seconds)
                    }
                    insets
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.navigationBarColor = ContextCompat.getColor(this, R.color.transparentBlackColor)

            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                    // Hide the system UI after a delay
                    window.decorView.postDelayed({
                        window.decorView.systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                )
                    }, 3000) // Delay in milliseconds (3 seconds)
                }
            }
        }
    }

    fun FragmentActivity.hideNavigationBar(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }else{
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        }
    }

     fun DialogFragment.hideNavigationBarFromDialog() {
        dialog?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

    }



    fun shareVideo(mActivity: FragmentActivity, videoFile: File) {
        if (videoFile.exists()) {
            mActivity?.let {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "video/mp4"
                val videoUri: Uri = FileProvider.getUriForFile(
                    it,
                    "${it.packageName}.provider",
                    videoFile
                )
                shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ContextCompat.startActivity(it, Intent.createChooser(shareIntent, "Share video"), null)
            }
        } else {
            // Handle case when the video file doesn't exist
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
        }
    }


   fun isValidFileName(fileName: String): Boolean {
        val forbiddenChars = arrayOf("/", "\\", "?", "%", "*", ":", "|", "\"", "<", ">")
        return fileName.none { it.toString() in forbiddenChars } && fileName.isNotBlank()
    }


}
fun View.show() {
    if (!isVisible) visibility = View.VISIBLE
}

fun View.gone() {
    if (isVisible) visibility = View.GONE
}

fun View.invisible() {
    if (isVisible) visibility = View.INVISIBLE
}



