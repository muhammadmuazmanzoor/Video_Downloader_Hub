package com.avd.ui.main.downloder_queue.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.isAdShowing

class PermissionManagerNew(
    private val context: Context,
    private val activity: FragmentActivity,
    private val callback: Callback
) {


    companion object {
        const val PERMISSION_REQUEST_CODE = 3001
    }

    private var isSettingsDialogShown = false

    interface Callback {
        fun onStorageResult(isGranted: Boolean)
        fun onNotificationResult(isGranted: Boolean)
        fun onForegroundServiceResult(isGranted: Boolean)
    }

    /** ------------------- Check Permissions ------------------- **/
    fun isStorageGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun isForegroundServiceGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun areAllPermissionsGranted(): Boolean {
        return isStorageGranted() && isNotificationGranted() && isForegroundServiceGranted()
    }

    /** ------------------- Request Permissions ------------------- **/
    fun requestAllPermissions(fragment: Fragment) {
        if (isSettingsDialogShown) return

        val permissionsToRequest = mutableListOf<String>()

        // Storage
        if (!isStorageGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Notifications
        if (!isNotificationGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Foreground Service
        if (!isForegroundServiceGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
            } else {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            fragment.requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            AdBlockerHelper.setinterstitialshown(true)
        } else {
            // All granted or permanently denied
            checkPermanentlyDenied(fragment)
        }
    }

    /** ------------------- Handle Permissions Result ------------------- **/
    fun handlePermissionsResult(fragment: Fragment, permissions: Array<out String>, grantResults: IntArray) {
        if(!isAdShowing) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED

                when (permission) {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_VIDEO -> {
                        callback.onStorageResult(granted)
                        if (!granted && !fragment.shouldShowRequestPermissionRationale(permission)) {
                            showSettingsDialogOnce("Storage permission permanently denied.")
                        }
                    }

                    Manifest.permission.POST_NOTIFICATIONS -> {
                        callback.onNotificationResult(granted)
                        if (!granted && !fragment.shouldShowRequestPermissionRationale(permission)) {
                            showSettingsDialogOnce("Notification permission permanently denied.")
                        }
                    }

                    Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC -> {
                        callback.onForegroundServiceResult(granted)
                        if (!granted && !fragment.shouldShowRequestPermissionRationale(permission)) {
                            showSettingsDialogOnce("Foreground Service permission permanently denied.")
                        }
                    }
                }
            }
        }
    }

    /** ------------------- Show Settings Dialog Only Once ------------------- **/
    private fun showSettingsDialogOnce(message: String) {
            if (isSettingsDialogShown || isAdShowing) return
            isSettingsDialogShown = true
            AlertDialog.Builder(context)
                .setTitle("Permission Required")
                .setMessage("$message\nPlease allow it from App Settings.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    /** ------------------- Check Permanently Denied ------------------- **/
    private fun checkPermanentlyDenied(fragment: Fragment) {
        val permissions = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
        )

        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED &&
                !fragment.shouldShowRequestPermissionRationale(perm)
            ) {
                showSettingsDialogOnce("Permission permanently denied.")
                break
            }
        }
    }
}
