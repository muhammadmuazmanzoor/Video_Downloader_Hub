package com.video.avd.ui.player.subtitle

interface SubtitleCustomizationsListener {
    fun onSetAlignment(alignment : String)
    fun onSetTextSize(textSize : String)
    fun onSetColor(colorCode : String)
    fun onSetTextShadow(position: String)
}