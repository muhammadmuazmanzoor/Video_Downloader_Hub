package com.video.avd.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.ViewGroup
import android.widget.EditText

abstract class RenameDialog(val context: Context, val hint: String) : DialogInterface.OnClickListener,
    DialogInterface.OnDismissListener {

    private val text: EditText = EditText(context)
    private val dialog: AlertDialog

    init {
        text.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog = AlertDialog.Builder(context)
            .setView(text)
            .setMessage("Type new name:")
            .setPositiveButton("OK", this)
            .setNegativeButton("CANCEL", this)
            .create()

        dialog.show()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        AppUtils.hideSoftKeyboard(context as Activity, text.windowToken)
        if (which == DialogInterface.BUTTON_POSITIVE) {
            onOK(text.text.toString())
        }
    }

    abstract fun onOK(newName: String)

    fun isActive(): Boolean {
        return dialog.isShowing
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
