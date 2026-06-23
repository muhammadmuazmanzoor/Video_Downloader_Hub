package com.avd.ui.main.home.browser.homeTab

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.avd.R
import com.avd.databinding.DialogOverlayPermissionBinding
import androidx.core.net.toUri

class OverlayPermissionDialog(
    context: Context,
    private val onPermissionGranted: (() -> Unit)? = null,
    private val onOpenOverlaySettings: (() -> Unit)? = null
) : Dialog(context, R.style.OverlayPermissionDialogTheme) {

    private var binding: DialogOverlayPermissionBinding? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogOverlayPermissionBinding.inflate(LayoutInflater.from(context))
        setContentView(binding?.root ?: View(context))
        
        // Set dialog properties to match DialogFragment behavior
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        
        setupDialog()
        setupViews()
    }

    private fun setupDialog() {
        window?.apply {
            // Set background drawable (same as DialogFragment)
            setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
            
            // Set layout dimensions (same as DialogFragment)
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun setupViews() {
        // Image will be set later by the caller
        // binding?.ivPermissionIllustration?.setImageResource(R.drawable.your_image_here)

        binding?.btnClosePermission?.setOnClickListener {
            dismiss()
        }

        binding?.btnGrantPermission?.setOnClickListener {
            requestOverlayPermission()
            dismiss()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                onOpenOverlaySettings?.invoke()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
        }
    }

    override fun dismiss() {
        binding = null
        super.dismiss()
    }
}
