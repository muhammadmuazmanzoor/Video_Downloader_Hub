package com.avd.util

import android.util.Log

class AppLogger {

    companion object {

        private const val TAG = "Debug"

        fun d(message: String) {
           Log.d("Downloader-dd",message)
        }

        fun i(message: String) {

        }

        fun w(message: String) {

        }

        fun e(message: String) {
            Log.d("Downloader",message)
        }
    }
}