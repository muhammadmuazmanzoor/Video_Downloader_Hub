package com.avd.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.avd.R

object AppConstant {
    const val ADJUST_TOKEN=  "9chwtbkp5xc0"
    const val AD_IMPRESSION_TOKEN=  "aau031"
    const val ADJUST_HOME_TOKEN=  "8oek2i"
    const val ADJUST_SUBSCRIPTION_TOKEN=  "wradzj"


}
enum class DownloadDialogType {
    INTERNET_ERROR,
    EMPTY_URL,
    INVALID_URL
}
private var downloadDialog: AlertDialog? = null
fun Context.showDownloadDialog(
    type: DownloadDialogType,
    onRetryConnection: (() -> Unit)? = null
) {
    try {

        // 🚫 prevent multiple dialogs
        if (downloadDialog?.isShowing == true) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_status, null)

        val tvIcon = dialogView.findViewById<ImageView>(R.id.tvIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)
        val tvSupportedLinks = dialogView.findViewById<LinearLayout>(R.id.supported_links)
        val btnPrimary = dialogView.findViewById<TextView>(R.id.btnPrimary)
        val btnSecondary = dialogView.findViewById<TextView>(R.id.btnSecondary)

        downloadDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        downloadDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        downloadDialog?.setOnDismissListener {
            downloadDialog = null // clear reference
        }

        when (type) {

            DownloadDialogType.INTERNET_ERROR -> {
                tvIcon.setBackgroundResource(R.drawable.network)
                tvTitle.text = "Connection Interrupted"
                tvMessage.text = "You'll need an active internet connection\nto download videos."

                tvSupportedLinks.visibility = View.GONE

                btnPrimary.text = "⟳  Retry Connection"
                btnSecondary.text = "Check Network Settings"

                btnPrimary.setOnClickListener {
                    downloadDialog?.dismiss()
                    onRetryConnection?.invoke()
                }

                btnSecondary.setOnClickListener {
                    downloadDialog?.dismiss()
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            }

            DownloadDialogType.EMPTY_URL -> {
                tvIcon.setBackgroundResource(R.drawable.supported_link)
                tvTitle.text = "Supported Links"
                tvMessage.text = "Paste a link from supported platforms..."

                tvSupportedLinks.visibility = View.VISIBLE

                btnPrimary.text = "OK, SURE"
                btnSecondary.visibility = View.GONE

                btnPrimary.setOnClickListener {
                    downloadDialog?.dismiss()
                }
            }

            DownloadDialogType.INVALID_URL -> {
                tvIcon.setBackgroundResource(R.drawable.invalid_link)
                tvTitle.text = "Oops! Invalid URL"
                tvMessage.text = "Invalid video link. Try again."

                tvSupportedLinks.visibility = View.GONE

                btnPrimary.text = "Got It"
                btnSecondary.visibility = View.GONE

                btnPrimary.setOnClickListener {
                    downloadDialog?.dismiss()
                }
            }
        }

        downloadDialog?.show()

    } catch (e: Exception) {
        e.printStackTrace()
    }
}