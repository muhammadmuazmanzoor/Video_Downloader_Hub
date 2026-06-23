package com.video.avd.ui.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Rational
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.video.avd.R
import com.video.avd.constent.isSplash
import com.video.avd.ui.status_saver.StatusViewModel
import com.video.avd.ads.AppOpenManager


class PlayerDelegateImpl : PlayerDelegate {

    companion object {
        var isSoundMuted = false
    }


    override fun shareStatus(activity: Activity, statusViewModel: StatusViewModel) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            val status = statusViewModel.status
            shareIntent.type = "image/mp4"
            if (status?.isApi30 == true) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, status.documentFile.uri)
            } else {
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + status?.file?.absolutePath))
            }
            activity.startActivity(Intent.createChooser(shareIntent, "Share image"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun startPictureInPictureWithRatio(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Set the custom actions for the PIP mode
                val actions = mutableListOf<RemoteAction>()
                var icon: Icon? = null
                icon = if (PlayerVideoActivity.player?.isPlaying == true) {
                    Icon.createWithResource(activity, R.drawable.ic_play)
                } else {
                    Icon.createWithResource(activity, R.drawable.ic_pause)
                }
                val title = "Play/Pause"
                val pendingIntent = PendingIntent.getBroadcast(
                    activity,
                    if (PlayerVideoActivity.player?.isPlaying == true) 0 else 1,
                    Intent("PIP_PLAY_PAUSE_PLAYER"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val playPauseAction: RemoteAction = RemoteAction(icon, title, title, pendingIntent)
                playPauseAction.let {
                    actions.add(it)
                }
                activity.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setActions(actions)
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            } else {
                Log.e("TAG", "Not Allowed")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createPIPMode(activity: Activity) {
        isPipModeEnable(activity)
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun isPipModeEnable(activity: Activity) {
        try {
            startPictureInPictureWithRatio(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isPipModeCheck(activity)) {
                    startPictureInPictureWithRatio(activity)
                    // PiP mode is currently enabled
                } else {
                    // PiP mode is currently disabled
                    val builder = AlertDialog.Builder(activity)


                    val title = SpannableString("Enable Picture-in-Picture mode")
                    title.setSpan(ForegroundColorSpan(ContextCompat.getColor(activity, R.color.brand_text_primary)), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    val message = SpannableString("Do you want to enable Picture-in-Picture mode?")
                    val color = ContextCompat.getColor(activity,R.color.msg_color)
                    message.setSpan(ForegroundColorSpan(color), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)


                    builder.setTitle(title)
                    builder.setMessage(message)
                    // builder.setTitle("Enable Picture-in-Picture mode")
                    // builder.setMessage("Do you want to enable Picture-in-Picture mode?")
                    builder.setCancelable(false)
                    builder.setPositiveButton("Yes") { _, _ ->
                        PlayerVideoActivity.isPipMode = false
                        // Launch the PiP activity to enable PiP mode
                        val packageName = activity.packageName // replace with your app package name
                        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.fromParts("package", packageName, null))

//                        val intent = Intent().apply {
//                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                            data = Uri.fromParts("package", packageName, null)
//                        }
                        activity.startActivity(intent)
                        AppOpenManager.isShowingAd=true
                        isSplash = true
                    }
                    builder.setNegativeButton("No") { _, _ ->
                        PlayerVideoActivity.isPipMode = false
                    }
                    val dialog = builder.create()
                    dialog.setOnShowListener {
                        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        positiveButton.setTextColor(ContextCompat.getColor(activity, R.color.brand_text_primary))

                        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        negativeButton.setTextColor(ContextCompat.getColor(activity, R.color.brand_text_primary))
                    }

                    dialog.show()
                }
            } else {
                Toast.makeText(
                    activity,
                    "PiP Not Supported in this device",
                    Toast.LENGTH_SHORT
                ).show()
                // PiP mode is not available on this device
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun isPipModeCheck(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isInPictureInPictureMode = activity.isInPictureInPictureMode
            isInPictureInPictureMode
        } else {
            false
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildPIPParams(context: Context?, nowPlaying: Boolean): PictureInPictureParams {
        val icon: Icon = if (nowPlaying) {
            Icon.createWithResource(
                context,
                R.drawable.ic_play
            )
        } else {
            Icon.createWithResource(
                context,
                R.drawable.ic_pause
            )
        }
        val broadcast = PendingIntent.getBroadcast(
            context,
            if (nowPlaying) 0 else 1,
            Intent("PIP_PLAY_PAUSE_PLAYER"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        var remoteAction: RemoteAction? = null
        remoteAction = RemoteAction(icon, "", "", broadcast)
        val actions: MutableList<RemoteAction> = java.util.ArrayList()
        actions.add(remoteAction)
        val aspectRatio = Rational(16, 9)
        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(actions)
            .build()
    }

//    override fun toggleSound(sound: Float, activity: Activity, adapterPlayerTopFeatures: AdapterPlayerTopFeatures) {
//            if (isSoundMuted) {
//                // Set sound to 50 percent
//                unmuteSound(activity)
//
//                // Update the adapter with the unmuted icon
//                adapterPlayerTopFeatures.updateSoundIconDrawable(R.drawable.top_ic_unmute)
//            } else {
//                muteSound(activity)
//                // Mute sound
//                // Update the adapter with the muted icon
//                adapterPlayerTopFeatures.updateSoundIconDrawable(R.drawable.top_ic_mute_selected)
//            }
//        // Toggle the sound state
//        isSoundMuted = !isSoundMuted
//    }



    override fun unmuteSound(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_PLAY_SOUND)
    }


    override fun muteSound(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_PLAY_SOUND)
    }

    override fun shareLiveLink(context: Context, link: String) {
        Log.d("customs","$link")
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            "ICS"
        )
        val shareMessage =
            "Visit the give link $link"
        sharingIntent.putExtra(
            Intent.EXTRA_TEXT,
            shareMessage
        )
        startActivity(context,Intent.createChooser(sharingIntent, "Share using"), null)
    }
}