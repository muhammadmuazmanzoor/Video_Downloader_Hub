package com.avd.browserkit.ui.dialog

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.avd.browserkit.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BrowserDialogBuilders {
    fun create(context: Context): MaterialAlertDialogBuilder {
        val themedContext = ContextThemeWrapper(context, R.style.Theme_BrowserKit_Dialog)
        return MaterialAlertDialogBuilder(themedContext)
    }
}
