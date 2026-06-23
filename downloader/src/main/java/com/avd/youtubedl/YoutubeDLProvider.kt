package com.avd.youtubedl

// In your base module
interface YoutubeDLProvider {
    fun getYoutubeDLInstance(): Any
    fun getYoutubeDLRequest(url: Any): Any
    fun getorignalpathtoYoutubeDLRequest(url: Any): Any
}
