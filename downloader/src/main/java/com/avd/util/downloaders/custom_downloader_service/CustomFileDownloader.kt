package com.avd.util.downloaders.custom_downloader_service

import com.avd.util.AppLogger
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.RandomAccessFile
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray

interface DownloadListener {
    fun onSuccess()

    fun onFailure(e: Throwable)

    fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long)

    fun onChunkProgressUpdate(downloadedBytes: Long, allBytesChunk: Long, chunkIndex: Int)

    fun onChunkFailure(e: Throwable, index: CustomFileDownloader.Chunk)
}

class CustomFileDownloader(
    private val url: URL,
    // File is always must be placed in folder with file name without extension
    private val file: File,
    private val threadCount: Int,
    private val headers: MutableMap<String, String>,
    private val client: OkHttpClient,
    private val listener: DownloadListener?,
) : DownloadListener {
    private val executorService: ExecutorService = Executors.newFixedThreadPool(threadCount)
    private val isPaused = AtomicBoolean(false)
    private val isCanceled = AtomicBoolean(false)
    private var lastProgressUpdate = AtomicLong(0L)
    private val totalBytesAll = AtomicLong(0L)
    private val totalBytesChunks = AtomicLongArray(threadCount)
    private val copiedBytesChunks = AtomicLongArray(threadCount)
    private val copiedBytesSingle = AtomicLong(0L)
    private val callBackIntervalMin = 1000

    companion object {
        const val STOPPED = "STOPPED"
        const val CANCELED = "CANCELED"
        private const val STOP_FILE_NAME = "stop"
        private const val DOWNLOAD_BUFFER_SIZE = 256 * 1024
        private const val CHECKPOINT_BYTES_INTERVAL = 512 * 1024
        private const val CHECKPOINT_TIME_INTERVAL_MS = 1000L

        fun stop(fileToStop: File) {
            File(fileToStop.parentFile, STOP_FILE_NAME).createNewFile()
        }

        fun cancel(fileToStop: File) {
            fileToStop.parentFile?.deleteRecursively()
        }

        fun unStop(fileToUnStop: File) {
            File(fileToUnStop.parentFile, STOP_FILE_NAME).delete()
        }

        fun isStopped(fileToCheck: File): Boolean {
            return File(
                fileToCheck.parentFile, STOP_FILE_NAME
            ).exists()
        }

        fun isCanceled(fileToCheck: File): Boolean {
            return !(fileToCheck.parentFile?.exists() ?: false)
        }
    }


    private val totalCopiedBytes: Long
        get() {
            var sum = 0L
            // Replace the "..<" operator with the "until" function
            for (i in 0 until copiedBytesChunks.length()) {
                val value = copiedBytesChunks.get(i)
                sum += value
            }

            return sum
        }

    fun download() {
        val randomAccessFile = try {
            RandomAccessFile(file, "rw")
        } catch (e: Throwable) {
            this.onFailure(e)
            return
        }
        val fileChannel = randomAccessFile.channel

        unStop(file)

        val isUrlSupportBytesRangeHeader = isUrlSupportingBytesRangeHeader()

        if (!isUrlSupportBytesRangeHeader) {
            AppLogger.d("Range download unsupported for $url. Falling back to single-thread download.")
            singleThreadDownload(fileChannel)
            return
        }

        val contentSize = try {
            getContentLength()
        } catch (e: Throwable) {
            this.onFailure(e)

            return
        }
        totalBytesAll.set(contentSize)

        if (contentSize <= 0L) {
            singleThreadDownload(fileChannel)
            return
        }

        val effectiveThreadCount = if (contentSize < threadCount) {
            contentSize.toInt().coerceAtLeast(1)
        } else {
            threadCount
        }
        val chunkSize = contentSize / effectiveThreadCount
        val ranges = (0 until effectiveThreadCount).map {
            val start = it * chunkSize
            val end = if (it == effectiveThreadCount - 1) contentSize - 1 else (it + 1) * chunkSize - 1
            start..end
        }

        val chunkFutureMap = mutableMapOf<Chunk, Future<*>>()
        AppLogger.d(
            "Start Downloading: file: $file threadCount: $effectiveThreadCount ranges: $ranges"
        )
        ranges.forEachIndexed { index, range ->
            chunkFutureMap[Chunk(index, range, chunkSize)] = executorService.submit {
                downloadChunk(range, fileChannel, index * chunkSize, index)
            }
        }

        var allPartsSucceed = true
        chunkFutureMap.forEach { entry ->
            try {
                entry.value.get()
            } catch (e: Throwable) {
                allPartsSucceed = false
                this.onChunkFailure(e, entry.key)
            }
        }

        val isStopped = isPaused.get()
        val isCanceled = isCanceled.get()

        if (allPartsSucceed && !isStopped) {
            this.onSuccess()
        } else if (isStopped) {
            AppLogger.d("CHUNKS STOPPED")
            this.onFailure(Error(STOPPED))
        } else if (isCanceled) {
            AppLogger.d("CHUNKS CANCELED")
            this.onFailure(Error(CANCELED))
        } else {
            AppLogger.d("CHUNKS ERROR")
            this.onFailure(Error("Not All Chunks downloaded, retry"))
        }
    }




    override fun onSuccess() {
        executorService.shutdown()

        AppLogger.d("DOWNLOAD SUCCESS: $file")

        listener?.onSuccess()
    }

    override fun onFailure(e: Throwable) {
        executorService.shutdown()

        AppLogger.e("Task Download Failed $e")

        listener?.onFailure(e)
    }

    override fun onProgressUpdate(downloadedBytes: Long, totalBytes: Long) {
        val time = Date().time
        if (time - lastProgressUpdate.get() >= callBackIntervalMin) {
            isPaused.set(isStopped(file))
            isCanceled.set(isCanceled(file))
            lastProgressUpdate.set(time)
            listener?.onProgressUpdate(downloadedBytes, totalBytes)
        }
    }

    override fun onChunkProgressUpdate(downloadedBytes: Long, allBytes: Long, chunkIndex: Int) {
        copiedBytesChunks[chunkIndex] = downloadedBytes

        onProgressUpdate(totalCopiedBytes, totalBytesAll.get())

        listener?.onChunkProgressUpdate(downloadedBytes, allBytes, chunkIndex)
    }

    override fun onChunkFailure(e: Throwable, index: Chunk) {
        AppLogger.e("Chunk $index Download Failed ${e.printStackTrace()}")
        listener?.onChunkFailure(e, index)
    }

    private fun downloadChunk(
        range: LongRange, fileChannel: FileChannel, offset: Long, chunkIndex: Int
    ) {
        val chunkFile = File(file.parentFile, "chunk_$chunkIndex")
        val isResume = !chunkFile.createNewFile()
        var bytesCopied = 0L
        if (isResume) {
            bytesCopied = chunkFile.inputStream().use { chunkStream ->
                chunkStream.bufferedReader().use {
                    val text = it.readText().trim()
                    text.toLongOrNull() ?: 0L
                }
            }
        }
        AppLogger.d(
            "CHUNK $chunkIndex DOWNLOAD START, bytes copied: $bytesCopied  isResume: $isResume"
        )

        copiedBytesChunks[chunkIndex] = bytesCopied

        if (range.first + bytesCopied >= range.last) {
            val total = range.last - range.first + 1
            totalBytesChunks[chunkIndex] = total
            copiedBytesChunks[chunkIndex] = total

            return
        }

        val req = if (threadCount == 1) {
            getOkRequestRange(range.first + bytesCopied, null)
        } else
            getOkRequestRange(range.first + bytesCopied, range.last)
        val res = client.newCall(req).execute()

        if (res.body?.contentLength() == -1L) {
            throw Error("Content Length Not Found")
        }

        val inputStream = res.body?.byteStream() ?: throw Error("Input stream is null")
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)

        copiedBytesChunks[chunkIndex] = bytesCopied
        totalBytesChunks[chunkIndex] = res.body?.contentLength() ?: throw Error("content length is  null")

        var bytesRead = 0
        var lastCheckpointBytes = bytesCopied
        var lastCheckpointTime = System.currentTimeMillis()

        RandomAccessFile(chunkFile, "rw").channel.use { chunkChannel ->
            fun saveChunkCheckpoint(force: Boolean = false) {
                val now = System.currentTimeMillis()
                if (
                    force ||
                    bytesCopied - lastCheckpointBytes >= CHECKPOINT_BYTES_INTERVAL ||
                    now - lastCheckpointTime >= CHECKPOINT_TIME_INTERVAL_MS
                ) {
                    val checkpoint = bytesCopied.toString().toByteArray()
                    chunkChannel.truncate(0)
                    chunkChannel.position(0)
                    chunkChannel.write(ByteBuffer.wrap(checkpoint))
                    lastCheckpointBytes = bytesCopied
                    lastCheckpointTime = now
                }
            }

            inputStream.use { urlStream ->
                while (!isPaused.get() && !isCanceled.get() && (urlStream.read( // && bytesCopied < range.last
                        buffer
                    ).also { bytesRead = it }) >= 0
                ) {
                    fileChannel.write(ByteBuffer.wrap(buffer, 0, bytesRead), offset + bytesCopied)
                    bytesCopied += bytesRead
                    saveChunkCheckpoint()
                    this.onChunkProgressUpdate(
                        bytesCopied, totalBytesChunks[chunkIndex], chunkIndex
                    )
                }
                saveChunkCheckpoint(force = true)
                if (isStopped(file)) {
                    throw Exception(STOPPED)
                }
                if (isCanceled(file)) {
                    throw Exception(CANCELED)
                }
            }
        }
    }

    private fun singleThreadDownload(fileChannel: FileChannel) {
        var response: Response? = null
        try {
            val req = getOkRequest()
            AppLogger.d("Single-thread download request url=$url headers=${headers.keys}")
            response = client.newCall(req).execute()
            AppLogger.d(
                "Single-thread download response code=${response.code} message=${response.message} contentLength=${response.body?.contentLength()}"
            )
            if (!response.isSuccessful) {
                throw Exception("Download request failed: ${response.code} ${response.message}")
            }
            val inputStream = response.body?.byteStream() ?: throw Error("Input stream is null")
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            val totalBytes = response.body?.contentLength()?.takeIf { it > 0 } ?: totalBytesAll.get()
            totalBytesAll.set(totalBytes.coerceAtLeast(0L))

            var bytesCopied = 0L
            inputStream.use { urlStream ->
                while (!isPaused.get() && !isCanceled.get()) {
                    val bytesRead = urlStream.read(buffer)
                    if (bytesRead < 0) break
                    fileChannel.write(ByteBuffer.wrap(buffer, 0, bytesRead), bytesCopied)
                    bytesCopied += bytesRead
                    copiedBytesSingle.set(bytesCopied)
                    this.onProgressUpdate(bytesCopied, totalBytesAll.get())
                }
                if (isStopped(file)) {
                    AppLogger.d("SINGLE THREAD DOWNLOAD STOPPED")
                    throw Exception(STOPPED)
                }
                if (isCanceled(file)) {
                    AppLogger.d("SINGLE THREAD DOWNLOAD CANCELED")
                    throw Exception(CANCELED)
                }
            }
            this.onSuccess()
        } catch (e: Throwable) {
            this.onFailure(e)
        } finally {
            response?.close()
        }
    }

    private fun isUrlSupportingBytesRangeHeader(): Boolean {
        val req = getOkRequestRange(0, 0)

        var res: Response? = null
        try {
            res = client.newCall(req).execute()
            AppLogger.d(
                "Range support check url=$url code=${res.code} message=${res.message} contentRange=${res.header("Content-Range")} acceptRanges=${res.header("Accept-Ranges")} contentLength=${res.body?.contentLength()} headers=${headers.keys}"
            )
            return res.code == 206
        } catch (e: Throwable) {
            AppLogger.e("Range support check failed for $url: ${e.message}")
            return false
        } finally {
            res?.close()
        }
    }

    private fun getOkRequest(): Request {
        return Request.Builder().url(url).headers(headers.toHeaders()).build()
    }

    private fun getOkRequestRange(startByte: Long?, endByte: Long?): Request {
        val end = endByte ?: ""
        val range = "bytes=$startByte-$end"

        return Request.Builder().url(url).headers(headers.toHeaders())
            .header("Range", range).build()
    }

    private fun getContentLength(): Long {
        val req = getOkRequest()
        client.newCall(req).execute().use { response ->
            AppLogger.d(
                "Content length check url=$url code=${response.code} message=${response.message} contentLength=${response.body?.contentLength()} headers=${headers.keys}"
            )
            if (!response.isSuccessful) {
                throw Exception("Content length request failed: ${response.code} ${response.message}")
            }
            return response.body?.contentLength()?.takeIf { it >= 0 } ?: 0L
        }
    }

    data class Chunk(val chunkIndex: Int, val range: LongRange, val chunkSize: Long) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Chunk

            if (chunkIndex != other.chunkIndex) return false
            if (range != other.range) return false
            if (chunkSize != other.chunkSize) return false

            return true
        }

        override fun hashCode(): Int {
            var result = chunkIndex
            result = 31 * result + range.hashCode()
            result = 31 * result + chunkSize.hashCode()
            return result
        }
    }
}
