package com.avd.util

import android.app.Application
import android.util.Log
import com.avd.youtubedl.VideoInfo
import com.avd.youtubedl.YoutubeDLRequest
import com.avd.youtubedl.YoutubeDLResponse
import com.google.gson.Gson
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest as DirectYoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

fun interface ProgressCallback {
    fun invoke(progress: Float, param: Long?, line: String)
}

fun interface CompletionCallback {
    fun invoke(param1: Int?, param2: String?)
}

object YoutubeDlUtils {
    private const val TAG = "YoutubeDlUtils"
    private val gson = Gson()

    var application: Application? = null

    lateinit var youtubeDl: Any

    @Volatile
    private var initialized = false

    fun initYtdl(youtubeDLInstance: (Any) -> Unit) {
        try {
            val instance = ensureInitialized()
            youtubeDl = instance
            youtubeDLInstance(instance)
        } catch (e: Exception) {
            Log.e(TAG, "initYtdl failed: ${e.message}", e)
        }
    }

    fun getorignalMappedYoutubeDLRequest(url: Any): YoutubeDLRequest? {
        return getMappedYoutubeDLRequest(url)
    }

    fun getMappedYoutubeDLRequest(url: Any): YoutubeDLRequest? {
        return runCatching { YoutubeDLRequest(url.toString()) }
            .onFailure { Log.e(TAG, "getMappedYoutubeDLRequest failed for $url", it) }
            .getOrNull()
    }

    fun getMappedYoutubeDLRequestDownload(url: Any): Any? {
        return runCatching { DirectYoutubeDLRequest(url.toString()) }
            .onFailure { Log.e(TAG, "getMappedYoutubeDLRequestDownload failed for $url", it) }
            .getOrNull()
    }

    fun getYtdlInfo(youtubeDLInstance: Any, mUrl: YoutubeDLRequest?): VideoInfo? {
        val request = localToDirectRequest(mUrl) ?: return null
        return try {
            val info = ensureInitialized().getInfo(request)
            mapVideoInfo(info)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "getYtdlInfo failed instance=${youtubeDLInstance::class.java.name} url=${mUrl?.getUrls()?.firstOrNull()}",
                e
            )
            null
        }
    }

    fun executeYoutubeDLCommand(
        request: Any,
        processId: String? = null,
        progressCallback: ProgressCallback? = null,
        completionCallback: CompletionCallback? = null
    ): YoutubeDLResponse? {
        return try {
            runBlocking {
                withContext(Dispatchers.IO) {
                    executeYoutubeDLCommandInternal(request, processId, progressCallback, completionCallback)
                }
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "YouTube-DL execution cancelled processId=$processId")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error executing YoutubeDL command processId=$processId: ${e.message}", e)
            null
        }
    }

    fun executeYoutubeDLCommandInternal(
        request: Any,
        processId: String? = null,
        progressCallback: ProgressCallback? = null,
        completionCallback: CompletionCallback? = null
    ): YoutubeDLResponse? {
        return try {
            val directRequest = when (request) {
                is DirectYoutubeDLRequest -> request
                is YoutubeDLRequest -> localToDirectRequest(request)
                else -> null
            } ?: run {
                Log.e(TAG, "Unsupported YoutubeDL request type=${request::class.java.name}")
                completionCallback?.invoke(-1, "Unsupported YoutubeDL request type")
                return null
            }

            val youtubeDLInstance = ensureInitialized()
            Log.d(
                TAG,
                "executeYoutubeDLCommandInternal processId=$processId instance=${youtubeDLInstance::class.java.name} requestClass=${directRequest::class.java.name}"
            )

            val response = youtubeDLInstance.execute(directRequest, processId ?: "") { progress, eta, line ->
                Log.d(TAG, "Progress update processId=$processId progress=$progress line=$line")
                progressCallback?.invoke(progress, eta, line)
            }

            if (response == null) {
                Log.e(TAG, "YoutubeDL execute returned null processId=$processId")
                completionCallback?.invoke(-1, "YoutubeDL execute returned null")
                return null
            }

            completionCallback?.invoke(response.exitCode, response.out)
            YoutubeDLResponse(
                command = response.command,
                exitCode = response.exitCode,
                elapsedTime = response.elapsedTime,
                out = response.out,
                err = response.err
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error executing YoutubeDL command processId=$processId requestClass=${request::class.java.name}: ${e.message}",
                e
            )
            completionCallback?.invoke(-1, e.message)
            null
        }
    }

    private fun localToDirectRequest(localRequest: YoutubeDLRequest?): DirectYoutubeDLRequest? {
        if (localRequest == null) return null
        val directRequest = DirectYoutubeDLRequest(localRequest.getUrls())
        localRequest.getCustomCommands().takeIf { it.isNotEmpty() }?.let { commands ->
            directRequest.addCommands(commands)
        }
        localRequest.getOptionsSnapshot().forEach { (option, values) ->
            values.forEach { argument ->
                if (argument.isEmpty()) {
                    directRequest.addOption(option)
                } else {
                    directRequest.addOption(option, argument)
                }
            }
        }
        return directRequest
    }

    private fun mapVideoInfo(info: com.yausername.youtubedl_android.mapper.VideoInfo?): VideoInfo? {
        if (info == null) return null
        return runCatching {
            gson.fromJson(gson.toJson(info), VideoInfo::class.java)
        }.onFailure {
            Log.e(TAG, "mapVideoInfo failed: ${it.message}", it)
        }.getOrNull()
    }

    @Synchronized
    private fun ensureInitialized(): YoutubeDL {
        val app = application ?: throw IllegalStateException("YoutubeDlUtils.application is null")
        val instance = YoutubeDL.getInstance()
        if (!initialized) {
            instance.init(app)
            FFmpeg.getInstance().init(app)
            initialized = true
            Log.d(TAG, "YoutubeDL and FFmpeg initialized directly in downloader module")
        }
        youtubeDl = instance
        return instance
    }
}
