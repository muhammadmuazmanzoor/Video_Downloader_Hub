package com.video.avd.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CustomAlertDialog(
    context: Context,
) : AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window attributes and hide system bars
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            // Hide navigation bar
            val insetsController = WindowCompat.getInsetsController(this, decorView)
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Calculate the horizontal margin in pixels
            val marginPx = (context.resources.displayMetrics.density * 16).toInt()
            val displayMetrics = context.resources.displayMetrics

            // Set the width to match parent minus the horizontal margin
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(attributes)
            layoutParams.width = displayMetrics.widthPixels - (2 * marginPx)
            attributes = layoutParams
        }
    }

    override fun show() {
        window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        super.show()
    }
}
