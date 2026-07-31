package com.example.ammar.youtubedldynamic

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import com.avd.youtubedl.YoutubeDLProvider
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.avd.youtubedl.YoutubeDLRequest as YoutubeDLRequest1


@Keep
class YoutubeDLProviderImpl(private val applicationContext: Application) : YoutubeDLProvider {

    @Volatile
    private var isInitialized = false

    init {
        ensureInitialized()
    }

    override fun getYoutubeDLInstance(): Any {
        ensureInitialized()
        return YoutubeDL.getInstance() // Return the YoutubeDL instance as Any
    }

    override fun getYoutubeDLRequest(url: Any): Any {
        ensureInitialized()
        return YoutubeDLRequest1(url.toString()) // Return the YoutubeDL instance as Any
    }

    override fun getorignalpathtoYoutubeDLRequest(url: Any): Any {
        ensureInitialized()
        return  com.yausername.youtubedl_android.YoutubeDLRequest(url.toString())
    }


    @Synchronized
    private fun ensureInitialized() {
        if (isInitialized) return
        initializeYoutubeDl(applicationContext)
        updateYoutubeDL(applicationContext)
        isInitialized = true
    }

    private fun initializeYoutubeDl(applicationContext: Application) {
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
