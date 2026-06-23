package com.video.avd.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import pub.devrel.easypermissions.EasyPermissions

const val PERMISSIONS_REQUEST_CODE = 234

object PermissionsUtils {
    fun hasPermissions(context: Context) =
        EasyPermissions.hasPermissions(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )


    fun hasManageExternalPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun hasSettingPermission(context: Context) =
        EasyPermissions.hasPermissions(
            context,
            Manifest.permission.WRITE_SETTINGS
        )

    fun requestWriteSettingsPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(activity)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
            }
        }
    }

    fun requestManageExternalPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val permissions = arrayOf(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            )
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
            }
        }

    }
}