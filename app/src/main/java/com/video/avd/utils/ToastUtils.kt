package com.video.avd.utils

import android.content.Context
import android.widget.Toast
import com.video.avd.R

object ToastUtils {
    fun showToast(context: Context, message: String){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showErrorToast(context: Context){
        Toast.makeText(context,  context.getString(R.string.toast_error), Toast.LENGTH_SHORT).show()
    }

    fun showInternetWarningToast(context: Context){
        Toast.makeText(context, context.getString(R.string.toast_internet_warning), Toast.LENGTH_SHORT).show()
    }
}