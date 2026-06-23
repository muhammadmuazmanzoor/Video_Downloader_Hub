package com.video.avd.ui.languages

import android.graphics.drawable.Drawable

data class LanguageSelectionModel(
    val lang : String,
    var name: String,
    var isSelected : Boolean = false,
    var flag :Drawable? = null
)
