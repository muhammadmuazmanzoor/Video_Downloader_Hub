package com.video.avd.utils

import android.webkit.JavascriptInterface




class MyJavaScriptInterface {

    var html: String? = null

    @JavascriptInterface
    fun showHTML(_html: String?) {
        html = _html
    }
}