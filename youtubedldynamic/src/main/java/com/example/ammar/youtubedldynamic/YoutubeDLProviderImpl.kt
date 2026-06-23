package com.example.ammar.youtubedldynamic

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import com.avd.youtubedl.YoutubeDLProvider
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.avd.youtubedl.YoutubeDLRequest as YoutubeDLRequest1


@Keep
class YoutubeDLProviderImpl(private val applicationContext: Application) : YoutubeDLProvider {

    private var isInitialized = false

    init {
        // Initialize YoutubeDL if not already initialized
        initializeYoutubeDl(applicationContext)
        updateYoutubeDL(applicationContext)
        isInitialized = true
    }

    override fun getYoutubeDLInstance(): Any {
        if (!isInitialized) {
            initializeYoutubeDl(applicationContext)
        }
        return YoutubeDL.getInstance() // Return the YoutubeDL instance as Any
    }

    override fun getYoutubeDLRequest(url: Any): Any {
        if (!isInitialized) {
            throw IllegalStateException("YoutubeDL is not initialized")
        }
        return YoutubeDLRequest1(url.toString()) // Return the YoutubeDL instance as Any
    }

    override fun getorignalpathtoYoutubeDLRequest(url: Any): Any {
        if (!isInitialized) {
            throw IllegalStateException("YoutubeDL is not initialized")
        }
        return  com.yausername.youtubedl_android.YoutubeDLRequest(url.toString())
    }


    private fun initializeYoutubeDl(applicationContext: Application) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                Log.d("YoutubeDlUtils", "YoutubeDL and FFmpeg initialized successfully")
            } catch (e: Exception) {
                Log.e("YoutubeDlUtils", "YoutubeDL initialization failed: ${e.message}")
                e.printStackTrace()
            } catch (e: Throwable) {
                // Catch NoSuchMethodError or any other runtime errors
                Log.e("YoutubeDlUtils", "Unexpected error initializing YoutubeDL: ${e.message}")
                e.printStackTrace()
            }
        }

    }

    private fun updateYoutubeDL(applicationContext: Application) {
//        try {
//            val status = YoutubeDL.getInstance()
//                .updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.MASTER)
//            Log.d("YoutubeDlUtils", "YoutubeDL updated successfully: $status")
//        } catch (e: Throwable) {
//            Log.e("YoutubeDlUtils", "YoutubeDL update failed", e)
//            e.printStackTrace()
//        }
    }

}
