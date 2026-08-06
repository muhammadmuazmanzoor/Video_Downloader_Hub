package com.video.avd.ui.status_saver
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.video.avd.R

/**
 * Small centered dialog used in place of a Toast to tell the user that
 * WhatsApp Messenger or WhatsApp Business is not installed.
 *
 * Usage:
 *   WhatsAppNotInstalledDialog.newInstance(message)
 *       .show(childFragmentManager, "wa_not_installed")
 */
class WhatsAppNotInstalledDialog : DialogFragment() {

    private val message: String by lazy {
        arguments?.getString(ARG_MESSAGE).orEmpty()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_whatsapp_not_installed, null, false)

        view.findViewById<TextView>(R.id.tvMessage).text = message
        view.findViewById<Button>(R.id.btnOk).setOnClickListener { dismiss() }

        dialog.setContentView(view)
        dialog.setCancelable(true)

        dialog.window?.apply {
            // Transparent window so only the CardView (rounded, white) is
            // visible, centered, over the dimmed background.
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.5f)
        }

        return dialog
    }

    companion object {
        private const val ARG_MESSAGE = "arg_message"

        fun newInstance(message: String): WhatsAppNotInstalledDialog {
            return WhatsAppNotInstalledDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE, message)
                }
            }
        }
    }
}