package com.video.avd.ui.basefragment

import android.os.Build
import android.os.Environment
import androidx.fragment.app.Fragment
import com.video.avd.R
import com.video.avd.ui.folder.FolderFragment
import com.video.avd.ads.AppOpenManager
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions


abstract  class BaseVideoFragment : Fragment() , EasyPermissions.PermissionCallbacks , EasyPermissions.RationaleCallbacks {


    companion object{
        var isRationaleDialogShown = false
        const val PERMISSION_REQUEST_CODE = 124
    }

//    @AfterPermissionGranted(PERMISSION_REQUEST_CODE)
//    fun getPermission(){
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ){
//                if (EasyPermissions.hasPermissions(requireContext(), *FolderFragment.PERMISSIONS2_Folder)) {
//                        onPermissionsGranted()
//                } else {
//                    // Ask for both
//                    AppOpenManager.isShowingAd = true
//                    EasyPermissions.requestPermissions(
//                        this,
//                        getString(R.string.rationale_permissions),
//                        PERMISSION_REQUEST_CODE,
//                        *FolderFragment.PERMISSIONS2_Folder
//                    )
//                }
//            }else{
//                if (EasyPermissions.hasPermissions(requireContext(), *FolderFragment.PERMISSIONS_Folder)) {
//                    onPermissionsGranted()
//                } else {
//                    // Ask for both permissions
//                    AppOpenManager.isShowingAd = true
//                    EasyPermissions.requestPermissions(
//                        this,
//                        getString(R.string.rationale_permissions),
//                        PERMISSION_REQUEST_CODE,
//                        *FolderFragment.PERMISSIONS_Folder
//                    )
//                }
//            }
//    }

    @AfterPermissionGranted(PERMISSION_REQUEST_CODE)
    fun getPermission() {
        if (!isRationaleDialogShown) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()){
            onPermissionsGranted()
        }else{
            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FolderFragment.PERMISSIONS2_Folder
            } else {
                FolderFragment.PERMISSIONS_Folder
            }
            if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
                onPermissionsGranted()
            } else {
                AppOpenManager.isShowingAd = true
//                isSplash = true
                val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    FolderFragment.PERMISSIONS2_CHECK
                } else {
                    FolderFragment.PERMISSIONS_CHECK
                }
                if (EasyPermissions.hasPermissions(requireContext(),*requiredPermission)){
                    onPermissionsGranted()
                }else{
                    EasyPermissions.requestPermissions(
                        this,
                        getString(R.string.rationale_permissions),
                        PERMISSION_REQUEST_CODE,
                        *requiredPermissions
                    )
                }

            }
        }
        }else{
            val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FolderFragment.PERMISSIONS2_Folder
            } else {
                FolderFragment.PERMISSIONS_Folder
            }
            if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
                onPermissionsGranted()
            }else{
                onPermissionsDenied(emptyList())
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

            FolderFragment.PERMISSIONS2_CHECK
        } else {

            FolderFragment.PERMISSIONS_CHECK
        }
        if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {
            onPermissionsGranted()

        }else{
                EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)

        }
    }

    // EasyPermissions.PermissionCallbacks implementation
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            FolderFragment.PERMISSIONS2_CHECK
        } else {
            FolderFragment.PERMISSIONS_CHECK
        }
        if (perms.containsAll(requiredPermissions.toList())) {
            onPermissionsGranted()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            FolderFragment.PERMISSIONS2_CHECK
        } else {
            FolderFragment.PERMISSIONS_CHECK
        }
        if (EasyPermissions.hasPermissions(requireContext(), *requiredPermissions)) {

            onPermissionsGranted()
        }else{
                isRationaleDialogShown = true
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
