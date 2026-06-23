package com.video.avd.ui.basefragment

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.video.avd.R
import com.video.avd.ads.AppOpenManager
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


abstract  class BaseFragment : Fragment(), EasyPermissions.PermissionCallbacks ,
    EasyPermissions.RationaleCallbacks {

    var isRationaleDialogShown = false

    private val PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val PERMISSIONS2 = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_MEDIA_AUDIO)


    private val PERMISSIONS_CHECK = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val PERMISSIONS2_CHECK = arrayOf(Manifest.permission.READ_MEDIA_AUDIO)


    companion object{
        const val PERMISSION_REQUEST_CODE = 124
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @AfterPermissionGranted(PERMISSION_REQUEST_CODE)
    fun getPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()){
            onPermissionsGranted()
        }else{
            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PERMISSIONS2
            } else {
                PERMISSIONS
            }
            if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
                onPermissionsGranted()
            } else {
                val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PERMISSIONS2_CHECK
                } else {
                    PERMISSIONS_CHECK
                }
                if (EasyPermissions.hasPermissions(requireContext(),*requiredPermission)){
                    onPermissionsGranted()
                }else{
                    AppOpenManager.isShowingAd = true
//                    isSplash = true
                    EasyPermissions.requestPermissions(
                        this,
                        getString(R.string.rationale_permissions),
                        PERMISSION_REQUEST_CODE,
                        *requiredPermission
                    )
                }

            }
        }

    }

    // Handle permission request result using EasyPermissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS2_CHECK
        } else {
           PERMISSIONS_CHECK
        }
        if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
            onPermissionsGranted()
        }else{
            onPermissionsDenied(emptyList())
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
        }
    }

    // EasyPermissions.PermissionCallbacks implementation
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
           PERMISSIONS2_CHECK
        } else {
           PERMISSIONS_CHECK
        }
        if (perms.containsAll(requiredPermissions.toList())) {
            onPermissionsGranted()
        }else{
            onPermissionsDenied(perms)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          PERMISSIONS2_CHECK
        } else {
          PERMISSIONS_CHECK
        }
        if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
            onPermissionsGranted()
        }else{
            onPermissionsDenied(perms)
        }
    }

    // EasyPermissions.RationaleCallbacks implementation
    override fun onRationaleAccepted(requestCode: Int) {}

    override fun onRationaleDenied(requestCode: Int) {

    }

    // Override these methods in your fragments to handle permissions
    abstract fun onPermissionsGranted()

    abstract fun onPermissionsDenied(deniedPermissions: List<String>)

}