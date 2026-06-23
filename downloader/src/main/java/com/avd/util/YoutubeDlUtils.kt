package com.avd.util

import android.app.Application
import android.util.Log
import com.avd.youtubedl.VideoFormat
import com.avd.youtubedl.VideoInfo
import com.avd.youtubedl.VideoThumbnail
import com.avd.youtubedl.YoutubeDLProvider
import com.avd.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy
import kotlin.coroutines.cancellation.CancellationException

fun interface ProgressCallback {
    fun invoke(progress: Float, param: Long?, line: String)
}

fun interface CompletionCallback {
    fun invoke(param1: Int?, param2: String?)
}

object YoutubeDlUtils {
    var application: Application? = null

    lateinit var youtubeDl: Any

    fun initYtdl(
        youtubeDLInstance: (Any) -> Unit
    ) {
        try {
            // Load the YoutubeDLProviderImpl class from the dynamic feature module
            val youtubeDLProviderClass = Class.forName("com.example.ammar.youtubedldynamic.YoutubeDLProviderImpl")
            val constructor = youtubeDLProviderClass.getConstructor(Application::class.java)
            val youtubeDLProvider = constructor.newInstance(application) as YoutubeDLProvider
            youtubeDLInstance(youtubeDLProvider.getYoutubeDLInstance())

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "ClassNotFoundException: " + e.message)

        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "NoSuchMethodException: " + e.message)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "Exception: " + e.message)
        }
    }

    fun getorignalMappedYoutubeDLRequest(url: Any): YoutubeDLRequest? {
        try {
            // Load the YoutubeDLProviderImpl class from the dynamic feature module
            val youtubeDLProviderClass =
                Class.forName("com.example.ammar.youtubedldynamic.YoutubeDLProviderImpl")
            val constructor = youtubeDLProviderClass.getConstructor(Application::class.java)
            val youtubeDLProvider = constructor.newInstance(application) as YoutubeDLProvider

            // Get YoutubeDLRequest object from dynamic module
            val dynamicYoutubeDLRequest = youtubeDLProvider.getorignalpathtoYoutubeDLRequest(url)

            // Map the dynamic YoutubeDLRequest to local YoutubeDLRequest
            val dynamicClass = dynamicYoutubeDLRequest::class.java

            // Extract 'urls' field
            val urlsField = dynamicClass.getDeclaredField("urls")
            urlsField.isAccessible = true
            val urls = urlsField.get(dynamicYoutubeDLRequest) as List<String>

            // Create an instance of local YoutubeDLRequest with urls
            val localRequest = YoutubeDLRequest(urls)

            // Extract 'customCommandList' field
            val commandListField = dynamicClass.getDeclaredField("customCommandList")
            commandListField.isAccessible = true
            val commandList = commandListField.get(dynamicYoutubeDLRequest) as List<String>
            localRequest.addCommands(commandList)

            // Extract 'options' field
            val optionsField = dynamicClass.getDeclaredField("options")
            optionsField.isAccessible = true
            val optionsInstance = optionsField.get(dynamicYoutubeDLRequest)

            // Access 'options' map from the optionsInstance (which is a YoutubeDLOptions object)
            val optionsClass = optionsInstance::class.java
            val optionsMapField = optionsClass.getDeclaredField("options")
            optionsMapField.isAccessible = true
            val optionsMap = optionsMapField.get(optionsInstance) as Map<String, MutableList<String>>

            // Add all options to the local YoutubeDLRequest
            for ((key, valueList) in optionsMap) {
                for (value in valueList) {
                    localRequest.addOption(key, value)
                }
            }

            Log.d("YoutubeDlUtils", "localRequest: $localRequest")
            return localRequest

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "ClassNotFoundException: ${e.message}")

        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "NoSuchMethodException: ${e.message}")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "Exception: ${e.message}")
        }
        return null
    }

    fun getMappedYoutubeDLRequest(url: Any): YoutubeDLRequest? {
        try {
            // Load the YoutubeDLProviderImpl class from the dynamic feature module
            val youtubeDLProviderClass =
                Class.forName("com.example.ammar.youtubedldynamic.YoutubeDLProviderImpl")
            val constructor = youtubeDLProviderClass.getConstructor(Application::class.java)
            val youtubeDLProvider = constructor.newInstance(application) as YoutubeDLProvider

            // Get YoutubeDLRequest object from dynamic module
            val dynamicYoutubeDLRequest = youtubeDLProvider.getYoutubeDLRequest(url)

            // Map the dynamic YoutubeDLRequest to local YoutubeDLRequest
            val dynamicClass = dynamicYoutubeDLRequest::class.java

            // Extract 'urls' field
            val urlsField = dynamicClass.getDeclaredField("urls")
            urlsField.isAccessible = true
            val urls = urlsField.get(dynamicYoutubeDLRequest) as List<String>

            // Create an instance of local YoutubeDLRequest with urls
            val localRequest = YoutubeDLRequest(urls)

            // Extract 'customCommandList' field
            val commandListField = dynamicClass.getDeclaredField("customCommandList")
            commandListField.isAccessible = true
            val commandList = commandListField.get(dynamicYoutubeDLRequest) as List<String>
            localRequest.addCommands(commandList)

            // Extract 'options' field
            val optionsField = dynamicClass.getDeclaredField("options")
            optionsField.isAccessible = true
            val optionsInstance = optionsField.get(dynamicYoutubeDLRequest)

            // Access 'options' map from the optionsInstance (which is a YoutubeDLOptions object)
            val optionsClass = optionsInstance::class.java
            val optionsMapField = optionsClass.getDeclaredField("options")
            optionsMapField.isAccessible = true
            val optionsMap = optionsMapField.get(optionsInstance) as Map<String, MutableList<String>>

            // Add all options to the local YoutubeDLRequest
            for ((key, valueList) in optionsMap) {
                for (value in valueList) {
                    localRequest.addOption(key, value)
                }
            }

            Log.d("YoutubeDlUtils", "localRequest: $localRequest")
            return localRequest

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "ClassNotFoundException: ${e.message}")

        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "NoSuchMethodException: ${e.message}")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "Exception: ${e.message}")
        }
        return null
    }


    fun getMappedYoutubeDLRequestDownload(url: Any): Any? {
        try {
            // Load the YoutubeDLProviderImpl class from the dynamic feature module
            val youtubeDLProviderClass = Class.forName("com.example.ammar.youtubedldynamic.YoutubeDLProviderImpl")
            val constructor = youtubeDLProviderClass.getConstructor(Application::class.java)
            val youtubeDLProvider = constructor.newInstance(application) as YoutubeDLProvider

            // Get YoutubeDLRequest object from the dynamic module
            val dynamicYoutubeDLRequest = youtubeDLProvider.getorignalpathtoYoutubeDLRequest(url)

            // Get the class of the dynamic YoutubeDLRequest
            val dynamicClass = dynamicYoutubeDLRequest::class.java

            // Extract 'urls' field using reflection
            val urlsField = dynamicClass.getDeclaredField("urls")
            urlsField.isAccessible = true
            val urls = urlsField.get(dynamicYoutubeDLRequest) as List<String>
            urlsField.isAccessible = false  // Reset accessibility

            // Dynamically load the local YoutubeDLRequest class
            val localRequestClass = Class.forName("com.yausername.youtubedl_android.YoutubeDLRequest")

            // Get the constructor of local YoutubeDLRequest class and instantiate it with urls
            val localRequestConstructor = localRequestClass.getConstructor(List::class.java)
            val localRequest = localRequestConstructor.newInstance(urls)

            // Use reflection to add custom commands to localRequest
            val addCommandsMethod = localRequestClass.getDeclaredMethod("addCommands", List::class.java)

            // Extract 'customCommandList' field from the dynamic YoutubeDLRequest
            val commandListField = dynamicClass.getDeclaredField("customCommandList")
            commandListField.isAccessible = true
            val commandList = commandListField.get(dynamicYoutubeDLRequest) as List<String>
            commandListField.isAccessible = false  // Reset accessibility

            // Invoke addCommands on the localRequest
            addCommandsMethod.invoke(localRequest, commandList)

            // Extract 'options' field from the dynamic YoutubeDLRequest
            val optionsField = dynamicClass.getDeclaredField("options")
            optionsField.isAccessible = true
            val optionsInstance = optionsField.get(dynamicYoutubeDLRequest)
            optionsField.isAccessible = false  // Reset accessibility

            // Access 'options' map from the optionsInstance (assuming it's a YoutubeDLOptions object)
            val optionsClass = optionsInstance::class.java
            val optionsMapField = optionsClass.getDeclaredField("options")
            optionsMapField.isAccessible = true
            val optionsMap = optionsMapField.get(optionsInstance) as Map<String, MutableList<String>>
            optionsMapField.isAccessible = false  // Reset accessibility

            // Use reflection to add options to the localRequest
            val addOptionMethod = localRequestClass.getDeclaredMethod("addOption", String::class.java, String::class.java)

            // Add all options to the local YoutubeDLRequest via reflection
            for ((key, valueList) in optionsMap) {
                for (value in valueList) {
                    addOptionMethod.invoke(localRequest, key, value)
                }
            }

            Log.d("YoutubeDlUtils", "localRequest (via reflection): $localRequest")
            return localRequest

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "ClassNotFoundException: ${e.message}")

        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "NoSuchMethodException: ${e.message}")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "Exception: ${e.message}")
        }
        return null
    }








    fun getYtdlInfo(youtubeDLInstance: Any, mUrl: YoutubeDLRequest?): VideoInfo? {
        try {
            // Convert the local YoutubeDLRequest to the dynamic module's YoutubeDLRequest
            val dynamicRequestClass =
                Class.forName("com.yausername.youtubedl_android.YoutubeDLRequest")
            val constructor = dynamicRequestClass.getConstructor(List::class.java)
            val urlsField = mUrl?.javaClass?.getDeclaredField("urls")
            urlsField?.isAccessible = true
            val urls = urlsField?.get(mUrl) as List<String>

            for(i in urls){
                Log.d("YoutubeDlUtils", "$i")
            }

            val dynamicRequestInstance = constructor.newInstance(urls)

            // Get the 'getInfo' method from the YoutubeDL instance using reflection
            val getInfoMethod =
                youtubeDLInstance::class.java.getMethod("getInfo", dynamicRequestClass)

            // Call the 'getInfo' method with the converted dynamic YoutubeDLRequest
            val videoInfoInstance = getInfoMethod.invoke(youtubeDLInstance, dynamicRequestInstance)
            if (videoInfoInstance != null) {
                Log.d("YoutubeDlUtils", "videoInfo found inside")

                // Access the VideoInfo class and get its methods via reflection
                val videoInfoClass = videoInfoInstance::class.java
                val videoInfo = VideoInfo()

                videoInfo.apply {
                    title = videoInfoClass.getDeclaredMethod("getTitle")
                        .invoke(videoInfoInstance) as String?
                    duration = videoInfoClass.getDeclaredMethod("getDuration")
                        .invoke(videoInfoInstance) as Int
                    description = videoInfoClass.getDeclaredMethod("getDescription")
                        .invoke(videoInfoInstance) as String?
                    thumbnail = videoInfoClass.getDeclaredMethod("getThumbnail")
                        .invoke(videoInfoInstance) as String?
                    license = videoInfoClass.getDeclaredMethod("getLicense")
                        .invoke(videoInfoInstance) as String?
                    extractor = videoInfoClass.getDeclaredMethod("getExtractor")
                        .invoke(videoInfoInstance) as String?
                    extractorKey = videoInfoClass.getDeclaredMethod("getExtractorKey")
                        .invoke(videoInfoInstance) as String?
                    viewCount = videoInfoClass.getDeclaredMethod("getViewCount")
                        .invoke(videoInfoInstance) as String?
                    likeCount = videoInfoClass.getDeclaredMethod("getLikeCount")
                        .invoke(videoInfoInstance) as String?
                    dislikeCount = videoInfoClass.getDeclaredMethod("getDislikeCount")
                        .invoke(videoInfoInstance) as String?
                    repostCount = videoInfoClass.getDeclaredMethod("getRepostCount")
                        .invoke(videoInfoInstance) as String?
                    averageRating = videoInfoClass.getDeclaredMethod("getAverageRating")
                        .invoke(videoInfoInstance) as String?
                    uploaderId = videoInfoClass.getDeclaredMethod("getUploaderId")
                        .invoke(videoInfoInstance) as String?
                    uploader = videoInfoClass.getDeclaredMethod("getUploader")
                        .invoke(videoInfoInstance) as String?
                    playerUrl = videoInfoClass.getDeclaredMethod("getPlayerUrl")
                        .invoke(videoInfoInstance) as String?
                    webpageUrl = videoInfoClass.getDeclaredMethod("getWebpageUrl")
                        .invoke(videoInfoInstance) as String?
                    webpageUrlBasename = videoInfoClass.getDeclaredMethod("getWebpageUrlBasename")
                        .invoke(videoInfoInstance) as String?
                    resolution = videoInfoClass.getDeclaredMethod("getResolution")
                        .invoke(videoInfoInstance) as String?
                    width = videoInfoClass.getDeclaredMethod("getWidth")
                        .invoke(videoInfoInstance) as Int
                    height = videoInfoClass.getDeclaredMethod("getHeight")
                        .invoke(videoInfoInstance) as Int
                    format = videoInfoClass.getDeclaredMethod("getFormat")
                        .invoke(videoInfoInstance) as String?
                    formatId = videoInfoClass.getDeclaredMethod("getFormatId")
                        .invoke(videoInfoInstance) as String?
                    ext = videoInfoClass.getDeclaredMethod("getExt")
                        .invoke(videoInfoInstance) as String?
                    fileSize = videoInfoClass.getDeclaredMethod("getFileSize")
                        .invoke(videoInfoInstance) as Long
                    fileSizeApproximate = videoInfoClass.getDeclaredMethod("getFileSizeApproximate")
                        .invoke(videoInfoInstance) as Long
                    httpHeaders = videoInfoClass.getDeclaredMethod("getHttpHeaders")
                        .invoke(videoInfoInstance) as Map<String, String>?
                    categories = videoInfoClass.getDeclaredMethod("getCategories")
                        .invoke(videoInfoInstance) as ArrayList<String>?
                    tags = videoInfoClass.getDeclaredMethod("getTags")
                        .invoke(videoInfoInstance) as ArrayList<String>?
                    requestedFormats = videoInfoClass.getDeclaredMethod("getRequestedFormats")
                        .invoke(videoInfoInstance) as ArrayList<VideoFormat>?
                    // Get formats using reflection
                    val formatsList = videoInfoClass.getDeclaredMethod("getFormats").invoke(videoInfoInstance) as List<*>
                    // Map each format from the dynamic class to the local VideoFormat class
                    formats = formatsList.map { mapDynamicVideoFormatToLocal(it!!) } as ArrayList<VideoFormat>
                    thumbnails = videoInfoClass.getDeclaredMethod("getThumbnails")
                        .invoke(videoInfoInstance) as ArrayList<VideoThumbnail>?
                    manifestUrl = videoInfoClass.getDeclaredMethod("getManifestUrl")
                        .invoke(videoInfoInstance) as String?
                    url = videoInfoClass.getDeclaredMethod("getUrl")
                        .invoke(videoInfoInstance) as String?
                }

                Log.d("YoutubeDlUtils", "title: ${videoInfo.title}")
                Log.d("YoutubeDlUtils", "description: ${videoInfo.description}")
                Log.d("YoutubeDlUtils", "duration: ${videoInfo.duration}")
                Log.d("YoutubeDlUtils", "formats: ${videoInfo.formats}")
                Log.d("YoutubeDlUtils", "thumbs: ${videoInfo.thumbnails}")
                Log.d("YoutubeDlUtils", "thumb: ${videoInfo.thumbnail}")
                return videoInfo
            } else {
                Log.d("YoutubeDlUtils", "VideoInfo is null.")
            }

        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "NoSuchMethodException: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("YoutubeDlUtils", "Exception: ${e.message}")
        }
        return null
    }

    private fun mapDynamicVideoFormatToLocal(videoFormatInstance: Any): VideoFormat {
        // Access the dynamically loaded VideoFormat class
        val videoFormatClass = videoFormatInstance::class.java

        val videoFormat = VideoFormat()
        videoFormat.apply {
            asr = videoFormatClass.getDeclaredMethod("getAsr").invoke(videoFormatInstance) as Int
            tbr =
                videoFormatClass.getDeclaredMethod("getTbr").invoke(videoFormatInstance) as Int
            abr = videoFormatClass.getDeclaredMethod("getAbr").invoke(videoFormatInstance) as Int
            format = videoFormatClass.getDeclaredMethod("getFormat")
                .invoke(videoFormatInstance) as String?
            formatId = videoFormatClass.getDeclaredMethod("getFormatId")
                .invoke(videoFormatInstance) as String?
            formatNote = videoFormatClass.getDeclaredMethod("getFormatNote")
                .invoke(videoFormatInstance) as String?
            ext =
                videoFormatClass.getDeclaredMethod("getExt").invoke(videoFormatInstance) as String
            preference = videoFormatClass.getDeclaredMethod("getPreference")
                .invoke(videoFormatInstance) as Int
            vcodec = videoFormatClass.getDeclaredMethod("getVcodec")
                .invoke(videoFormatInstance) as String?
            acodec = videoFormatClass.getDeclaredMethod("getAcodec")
                .invoke(videoFormatInstance) as String?
            width =
                videoFormatClass.getDeclaredMethod("getWidth").invoke(videoFormatInstance) as Int
            height =
                videoFormatClass.getDeclaredMethod("getHeight").invoke(videoFormatInstance) as Int
            fileSize = videoFormatClass.getDeclaredMethod("getFileSize")
                .invoke(videoFormatInstance) as Long
            fileSizeApproximate = videoFormatClass.getDeclaredMethod("getFileSizeApproximate")
                .invoke(videoFormatInstance) as Long
            fps =
                videoFormatClass.getDeclaredMethod("getFps").invoke(videoFormatInstance) as Int
            url =
                videoFormatClass.getDeclaredMethod("getUrl").invoke(videoFormatInstance) as String?
            manifestUrl = videoFormatClass.getDeclaredMethod("getManifestUrl")
                .invoke(videoFormatInstance) as String?
            httpHeaders = videoFormatClass.getDeclaredMethod("getHttpHeaders")
                .invoke(videoFormatInstance) as Map<String, String>?
        }
        return videoFormat
    }

    fun executeYoutubeDLCommand(
        request: Any,  // Accept the dynamic YoutubeDLRequest
        processId: String? = null,
        progressCallback: ProgressCallback? = null,
        completionCallback: CompletionCallback? = null
    ): com.avd.youtubedl.YoutubeDLResponse? {  // Return your local YoutubeDLResponse
        return try {
            // Use withContext to make it cancellable
            runBlocking {
                withContext(Dispatchers.IO) {
                    executeYoutubeDLCommandInternal(request, processId, progressCallback, completionCallback)
                }
            }
        } catch (e: CancellationException) {
            Log.d("YoutubeDlUtils", "YouTube-DL execution cancelled")
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("YoutubeDlUtils", "Error executing YoutubeDL command: ${e.message}")
            null
        }
    }

    fun executeYoutubeDLCommandInternal(
        request: Any,  // Accept the dynamic YoutubeDLRequest
        processId: String? = null,
        progressCallback: ProgressCallback? = null,
        completionCallback: CompletionCallback? = null
    ): com.avd.youtubedl.YoutubeDLResponse? {  // Return your local YoutubeDLResponse
        try {
            // Dynamically load YoutubeDLProviderImpl from the dynamic module
            val youtubeDLProviderClass = Class.forName("com.example.ammar.youtubedldynamic.YoutubeDLProviderImpl")
            val youtubeDLProviderConstructor = youtubeDLProviderClass.getConstructor(Application::class.java)
            val youtubeDLProvider = youtubeDLProviderConstructor.newInstance(application) as YoutubeDLProvider

            val youtubeDLInstance = youtubeDLProvider.getYoutubeDLInstance()

            // Load the YoutubeDLRequest class dynamically and get the execute method
            val youtubeDLRequestClass = request::class.java
            val function3Class = Class.forName("kotlin.jvm.functions.Function3")

            val executeMethod = youtubeDLInstance::class.java.getDeclaredMethod(
                "execute",
                youtubeDLRequestClass,
                String::class.java,
                function3Class
            )

            // Create a progress callback proxy
            val progressFunction = Proxy.newProxyInstance(
                function3Class.classLoader,
                arrayOf(function3Class)
            ) { _, _, args ->
                val progress = args[0] as Float
                val time = args[1] as Long?
                val line = args[2] as String
                Log.d("YoutubeDlUtils", "Progress update: Size: $progress, Line: $line")
                progressCallback?.invoke(progress, time, line)
                Unit
            }

            // Execute the method and get the dynamic YoutubeDLResponse
            val dynamicResponse = executeMethod.invoke(
                youtubeDLInstance,
                request,
                processId ?: "",
                progressFunction
            )

            // Reflect on the dynamic YoutubeDLResponse to extract data
            val dynamicResponseClass = dynamicResponse::class.java

            val commandField = dynamicResponseClass.getDeclaredField("command")
            val exitCodeField = dynamicResponseClass.getDeclaredField("exitCode")
            val elapsedTimeField = dynamicResponseClass.getDeclaredField("elapsedTime")
            val outField = dynamicResponseClass.getDeclaredField("out")
            val errField = dynamicResponseClass.getDeclaredField("err")

            // Set fields as accessible
            commandField.isAccessible = true
            exitCodeField.isAccessible = true
            elapsedTimeField.isAccessible = true
            outField.isAccessible = true
            errField.isAccessible = true

            // Extract values from the dynamic response
            val command = commandField.get(dynamicResponse)
            val commandList = if (command is String) {
                listOf(command)  // If command is a String, wrap it in a List
            } else {
                command as? List<String?> ?: emptyList()  // If it's a List, cast it
            }

            val exitCode = exitCodeField.get(dynamicResponse) as Int
            val elapsedTime = elapsedTimeField.get(dynamicResponse) as Long
            val out = outField.get(dynamicResponse) as String
            val err = errField.get(dynamicResponse) as String

            // Create your local YoutubeDLResponse using the extracted values
            val localResponse = com.avd.youtubedl.YoutubeDLResponse(
                command = commandList,  // Ensure command is now a List<String?>
                exitCode = exitCode,
                elapsedTime = elapsedTime,
                out = out,
                err = err
            )

            // Invoke the completion callback with the local response data
            completionCallback?.invoke(exitCode, out)

            // Return the local YoutubeDLResponse
            return localResponse

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("YoutubeDlUtils", "Error executing YoutubeDL command: ${e.message}")
            return null
        }
    }

}