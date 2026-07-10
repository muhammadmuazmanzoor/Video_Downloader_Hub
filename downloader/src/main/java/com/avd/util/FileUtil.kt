package com.avd.util


import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.avd.data.local.model.LocalVideo
import org.apache.commons.io.FileExistsException
import java.io.File
import java.io.FileNotFoundException
import java.text.DecimalFormat
import java.util.Arrays
import javax.inject.Inject

class FileUtil @Inject constructor() {

    // Cache for listFiles to prevent repeated binder IPC calls and deadlocks
    private var cachedListFiles: Map<String, Triple<Long, Uri, Long>>? = null
    private var listFilesCacheTime: Long = 0
    private val LIST_FILES_CACHE_DURATION_MS = 3000L // Cache for 3 seconds

    companion object {
        var INITIIALIZED = false

        // For downloads and tmp data
        var IS_EXTERNAL_STORAGE_USE = true

        // For downloads
        var IS_APP_DATA_DIR_USE = false

        const val FOLDER_NAME = "SuperX"
        const val TMP_DATA_FOLDER_NAME = "superx_tmp_data"

        private const val KB = 1024
        private const val MB = 1024 * 1024
        fun getFileSizeReadable(length: Double): String {

            val decimalFormat = DecimalFormat("#.##")
            return when {
                length > MB -> decimalFormat.format(length / MB) + " MB"
                length > KB -> decimalFormat.format(length / KB) + " KB"
                else -> decimalFormat.format(length) + " B"
            }
        }

        fun getFreeDiskSpace(context: Context, path: File?): Long {
            return try {
                val target = if (path != null && path.exists()) {
                    path
                } else {
                    // Fallback: use app’s internal storage dir
                    context.filesDir
                }

                val stat = StatFs(target.absolutePath)
                stat.availableBlocksLong * stat.blockSizeLong
            } catch (e: Exception) {
                // Final fallback if even StatFs fails
                try {
                    val stat = StatFs(context.filesDir.absolutePath)
                    stat.availableBlocksLong * stat.blockSizeLong
                } catch (ignored: Exception) {
                    0L
                }
            }
        }

        fun isExternalStorageWritable(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }
    }

    val folderDir: File
        get() {
            if (!INITIIALIZED) {
//                throw Error("File Util Not Initialized")
            }

            val context = ContextUtils.getApplicationContext()

            when {
                IS_EXTERNAL_STORAGE_USE && !IS_APP_DATA_DIR_USE -> {
                    return File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .toURI()
                    )
                }

                IS_EXTERNAL_STORAGE_USE && IS_APP_DATA_DIR_USE -> {
                    return File(context.getExternalFilesDir(null), FOLDER_NAME)

                }

                else -> {
                    return File(context.filesDir.absolutePath, FOLDER_NAME)
                }
            }
        }

    val tmpDir: File
        get() {
            if (!INITIIALIZED) {
//                throw Error("File Util Not Initialized")
            }

            val context = ContextUtils.getApplicationContext()

            return getTmpDataDir(context, IS_EXTERNAL_STORAGE_USE)
        }

    val listFilesOld: Map<String, Pair<Long, Uri>>
        get() {
            val context = ContextUtils.getApplicationContext()
            val result = mutableMapOf<String, Pair<Long, Uri>>()

            val externalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, true)
            val internalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val externalPublicFilesObjs = getPublicDownloadsVideoFilesObj(context, true)
                val internalPublicFilesObjs = getPublicDownloadsVideoFilesObj(context, false)
                result.putAll(externalPublicFilesObjs)
                result.putAll(internalPublicFilesObjs)
            } else {
                val externalPublicFilesObjsNew = getPublicDownloadsDirFilesObjNew()
                result.putAll(externalPublicFilesObjsNew)
            }
            result.putAll(externalPrivateFilesObjs)
            result.putAll(internalPrivateFilesObjs)

            return result

        }

    val listFiles: Map<String, Triple<Long, Uri, Long>>
        get() {
            // Check cache validity to avoid repeated binder IPC calls that can cause deadlocks
            val currentTime = System.currentTimeMillis()
            if (cachedListFiles != null && (currentTime - listFilesCacheTime) < LIST_FILES_CACHE_DURATION_MS) {
                return cachedListFiles!!
            }

            // Cache expired or not set, refresh it
            val context = ContextUtils.getApplicationContext()
            val result = mutableMapOf<String, Triple<Long, Uri, Long>>()

            val externalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, true)
            val internalPrivateFilesObjs = getPrivateDownloadsDirFilesObj(context, false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val externalPublicFilesObjs = getPublicDownloadsVideoFilesObj(context, true)
                val internalPublicFilesObjs = getPublicDownloadsVideoFilesObj(context, false)
                result.putAll(addTimestamps(externalPublicFilesObjs))
                result.putAll(addTimestamps(internalPublicFilesObjs))
            } else {
                val externalPublicFilesObjsNew = getPublicDownloadsDirFilesObjNew()
                result.putAll(addTimestamps(externalPublicFilesObjsNew))
            }

            result.putAll(addTimestamps(externalPrivateFilesObjs))
            result.putAll(addTimestamps(internalPrivateFilesObjs))

            // Update cache
            cachedListFiles = result
            listFilesCacheTime = currentTime

            return result
        }

    /**
     * Invalidates the listFiles cache. Call this when files are added, deleted, or modified
     * to ensure the cache stays fresh.
     */
    fun invalidateListFilesCache() {
        cachedListFiles = null
        listFilesCacheTime = 0
    }

    private fun addTimestamps(fileMap: Map<String, Pair<Long, Uri>>): Map<String, Triple<Long, Uri, Long>> {
        val context = ContextUtils.getApplicationContext()
        return fileMap.mapValues { entry ->
            val uri = entry.value.second
            val timestamp = getLastModifiedTimestamp(context, uri)
            Triple(entry.value.first, uri, timestamp)
        }
    }
    private fun getLastModifiedTimestamp(context: Context, uri: Uri): Long {
        if (isFileUri(uri)) {
            return File(uri.path ?: uri.toString()).lastModified()
        }

        val projection = arrayOf(
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED
        )
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val modifiedIndex = it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                if (modifiedIndex != -1 && !it.isNull(modifiedIndex)) {
                    val modified = it.getLong(modifiedIndex)
                    if (modified > 0) return modified * 1000
                }

                val addedIndex = it.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                if (addedIndex != -1 && !it.isNull(addedIndex)) {
                    val added = it.getLong(addedIndex)
                    if (added > 0) return added * 1000
                }
            }
        }
        return 0L
    }

    fun getVideoPreviewFromContent(context: Context, uri: Uri): Bitmap? {
        return try {
            if (isFileApiSupportedByUri(context, uri)) {
                return getVideoPreviewFromFile(uri.toFile())
            }

            return loadThumbnailFromMediaStore(context, uri)
        } catch (e: Throwable) {
            null
        }
    }

    private fun loadThumbnailFromMediaStore(context: Context, uri: Uri): Bitmap? {
        val videoId = getIdFromContentUri(context, uri) ?: return null
        return MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver, videoId, MediaStore.Video.Thumbnails.MINI_KIND, null
        )
    }

    private fun getVideoPreviewFromFile(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val bitmap = retriever.frameAtTime
        retriever.release()
        return bitmap
    }

    private fun getIdFromContentUri(context: Context, uri: Uri): Long? {
        // Check if the URI is a document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // Get the document ID
            val docId = DocumentsContract.getDocumentId(uri)
            // Split the document ID to get the last segment, which is the ID
            return docId.split(":").last().toLongOrNull()
        } else {
            // Get the ID from the last segment of the URI
            val pathSegments = uri.pathSegments

            return pathSegments.last().toLongOrNull()
        }
    }

    fun isFileWithNameNotExists(context: Context, uri: Uri, newName: String): Boolean {
        return if (isFileApiSupportedByUri(context, uri)) {
            !File(uri.toFile().parentFile, newName).exists()
        } else {
            !isDownloadedVideoContentExistsByName(context.contentResolver, uri, newName)
        }
    }


    fun moveMedia(context: Context, from: Uri, to: Uri): Boolean {
        return try {
            // Check if the destination URI is supported for file operations
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // API < 29: Move using file methods
                Log.d("IS_FILE_API",": TRUE -- from $from to $to")
                val fromFile = File(from.path ?: throw FileNotFoundException("Invalid from URI: $from"))
                val toFile = File(to.path ?: throw FileNotFoundException("Invalid to URI: $to"))

                if (fromFile.exists()) {
                    fromFile.copyTo(toFile, overwrite = true)
                    fromFile.delete() // Delete the original file after copying
                    true
                } else {
                    throw FileNotFoundException("File not found: $fromFile")
                }
            } else {
                // API 29 and above: Use ContentResolver to move the file
                Log.d("IS_FILE_API",": TRUE -- from $from to $to")
                moveFileUsingContentResolver(context, from, to)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun moveFileUsingContentResolver(context: Context, from: Uri, to: Uri): Boolean {
        val contentResolver = context.contentResolver

        // Open input stream from the source URI
        contentResolver.openInputStream(from)?.use { inputStream ->
            // Open output stream to the destination URI
            contentResolver.openOutputStream(to)?.use { outputStream ->
                inputStream.copyTo(outputStream) // Copy data from input to output
            }
        }

        NotificationsHelper.pendingurl=to.toString()

        // After copying, delete the original file if the copy was successful
        return if (from.scheme == "content") {
            // For content URIs, delete using ContentResolver
            val deletedRows = contentResolver.delete(from, null, null)
            deletedRows > 0 // Return true if the original file was deleted
        } else {
            // For file URIs, delete directly from the file system
            val fileToDelete = File(from.path ?: throw FileNotFoundException("Invalid URI: $from"))
            fileToDelete.delete() // Delete the file directly
        }

    }

    fun renameMedia(context: Context, from: Uri, newName: String): Pair<String, Uri>? {
        try {
            val cleanedFileName = FileNameCleaner.cleanFileName(newName) + ".mp4"
            val isNewFileNotExists = isFileWithNameNotExists(context, from, newName)

            if (cleanedFileName.isEmpty()) {
                throw Error("Empty file name")
            }

            if (!isUriExists(context, from)) {
                throw FileNotFoundException("File not found: $from")
            }

            if (!isNewFileNotExists) {
                throw FileExistsException("File already exists")
            }
            if (isFileApiSupportedByUri(context, from)) {
                val fromFile = from.toFile()
                val toFile = File(fromFile.parentFile, cleanedFileName)
                if (toFile.exists()) {
                    throw FileExistsException("File already exists: $toFile")
                }
                fromFile.renameTo(toFile)

                return Pair(toFile.name, Uri.fromFile(toFile))
            } else {
                val newUri = renameVideoContentFromDownloads(context, from, cleanedFileName)

                return Pair(cleanedFileName, newUri ?: from)
            }
        } catch (e: Throwable) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
        }

        return null
    }

    @Deprecated("This old")
    fun scanMedia(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = uri
            context.sendBroadcast(intent)
        } catch (e: Throwable) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteMedia(context: Context, uri: Uri): Boolean {
        return try {
            deleteMediaOrThrow(context, uri)
        } catch (e: Throwable) {
            e.printStackTrace()
            showToast(context, "Error: ${e.message}")
            false
        }
    }

    fun deleteMediaOrThrow(context: Context, uri: Uri): Boolean {
        if (!isUriExists(context, uri)) {
            throw FileNotFoundException("File not found: $uri")
        }

        val deleted = if (isFileUri(uri)) {
            deleteFileUri(uri)
        } else {
            deleteDownloadedVideoContent(context, uri)
        }

        if (!deleted) {
            throw FileNotFoundException("Unable to delete: $uri")
        }

        // Invalidate cache after deletion completes to ensure fresh data
        invalidateListFilesCache()
        return true
    }

    private fun isFileUri(uri: Uri): Boolean {
        return uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme.isNullOrBlank()
    }

    private fun deleteFileUri(uri: Uri): Boolean {
        val filePath = uri.path ?: uri.toString()
        val file = File(filePath)
        if (!file.exists()) {
            throw FileNotFoundException("File not found: $file")
        }
        return file.delete() || !file.exists()
    }

    private fun showToast(context: Context, message: String) {
        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isUriExists(context: Context, uri: Uri): Boolean {
        if (isFileUri(uri)) {
            return File(uri.path ?: uri.toString()).exists()
        }

        if (isFileApiSupportedByUri(context, uri)) {
            return runCatching { uri.toFile().exists() }.getOrDefault(false)
        }

        try {
            context.contentResolver.openInputStream(uri)?.close()
        } catch (e: FileNotFoundException) {
            return false
        } catch (e: Exception) {
            // Handle other exceptions as needed
        }

        // If there were no exceptions, the URI exists
        return true
    }

    fun getContentLength(context: Context, uri: Uri): Long {
        return if (isFileApiSupportedByUri(context, uri)) {
            uri.toFile().length()
        } else {
            getContentSize(context, uri)
        }
    }

    fun isFileApiSupportedByUri(context: Context, uri: Uri): Boolean {
        if (!isFileUri(uri)) {
            return false
        }

        val isExternalTo = isExternalUri(uri)

        val privateDir = getPrivateDownloadsDir(context, isExternalTo)
        val isAppDir = uri.toString().startsWith(Uri.fromFile(privateDir).toString())

        return !(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !isAppDir)
    }

    private fun getContentSize(context: Context, uri: Uri): Long {
        val resolver = context.contentResolver
        resolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE, MediaStore.MediaColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val openableSize = cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { cursor.getLong(it) }
                if (openableSize != null && openableSize >= 0L) return openableSize

                val mediaSize = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { cursor.getLong(it) }
                if (mediaSize != null && mediaSize >= 0L) return mediaSize
            }
        }

        return try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0L } ?: descriptor.parcelFileDescriptor.statSize
            }?.takeIf { it >= 0L } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun renameVideoContentFromDownloads(context: Context, uri: Uri, newName: String): Uri? {
        // Check if the URI is a document URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // Rename the document using the DocumentsContract API
            return DocumentsContract.renameDocument(context.contentResolver, uri, newName)
        } else {
            // Rename the file using the ContentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, newName)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            context.contentResolver.update(uri, values, null, null)

            return Uri.parse(uri.toString())
        }
    }

    private fun isDownloadedVideoContentExistsByName(
        contentResolver: ContentResolver, contentOrig: Uri, fileName: String
    ): Boolean {
        // Determine if the provided URI is external
        val isExternal = isExternalUri(contentOrig)

        // Handle API levels below 29 (Android 10)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Use the legacy file system access for Downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, fileName)
            return targetFile.exists() // Check if the file exists
        } else {
            // Handle API 29 and above with MediaStore
            val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            return contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        }
    }

    private fun getTmpDataDir(context: Context, isExternal: Boolean): File {
        val path = if (isExternal) {
            "${context.getExternalFilesDir(null)}/$TMP_DATA_FOLDER_NAME"
        } else {
            "${context.filesDir.absolutePath}/$TMP_DATA_FOLDER_NAME"
        }

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        return file
    }

    private fun getPrivateDownloadsDirFilesObj(
        context: Context, isExternal: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()

        val path = getPrivateDownloadsDir(context, isExternal).absolutePath

        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        val files = file.listFiles()

        if (files != null) {
            for (f in files) {
                filesMap[f.name] = Pair(f.length(), Uri.fromFile(f))
            }
        }

        return filesMap
    }

    private fun getPrivateDownloadsDir(context: Context, isExternal: Boolean): File {
        val path = if (isExternal) {
            "${context.getExternalFilesDir(null)}/$FOLDER_NAME"
        } else {
            "${context.filesDir.absolutePath}/$FOLDER_NAME"
        }

        return File(path)
    }

    private fun getPublicDownloadsDirFilesObjNew(): Map<String, Pair<Long, Uri>> {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI()
        )
        val filesList =
            downloadsDir.listFiles()?.filter { it.isFile && it.extension == "mp4" }?.toTypedArray()
                ?: emptyArray<File>()
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()

        for (file in filesList) {
            filesMap[file.name] = Pair(file.name.hashCode().toLong(), Uri.fromFile(file))
        }

        return filesMap
    }

    private fun getPublicDownloadsVideoFilesObj(
        context: Context,
        isExternalStorage: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()
        val targetUri = if (isExternalStorage) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.INTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%${Environment.DIRECTORY_DOWNLOADS}%")

        context.contentResolver.query(targetUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: continue
                val contentUri = ContentUris.withAppendedId(targetUri, id)

                if (isUriExists(context, contentUri)) {
                    filesMap[name] = Pair(id, contentUri)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val downloadsUri = if (isExternalStorage) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Downloads.INTERNAL_CONTENT_URI
            }
            val downloadsProjection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME
            )
            val downloadsSelection =
                "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.MIME_TYPE} LIKE ?"
            val downloadsSelectionArgs = arrayOf("%${Environment.DIRECTORY_DOWNLOADS}%", "video/%")

            context.contentResolver.query(
                downloadsUri,
                downloadsProjection,
                downloadsSelection,
                downloadsSelectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: continue
                    val contentUri = ContentUris.withAppendedId(downloadsUri, id)

                    if (isUriExists(context, contentUri)) {
                        filesMap[name] = Pair(id, contentUri)
                    }
                }
            }
        }

        return filesMap
    }

    private fun getPublicDownloadsDirFilesObjOld(
        context: Context, isExternalStorage: Boolean
    ): Map<String, Pair<Long, Uri>> {
        val filesMap = mutableMapOf<String, Pair<Long, Uri>>()
        // Check if the URI is valid for the current API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val targetUri = if (isExternalStorage) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Downloads.INTERNAL_CONTENT_URI
            }
            context.contentResolver.query(
                targetUri,
                arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(targetUri, id)
                    val isUriExists = isUriExists(context, contentUri) // Assuming this function checks if the URI is accessible

                    if (isUriExists) {
                        filesMap[name] = Pair(id, contentUri)
                    }
                }
            }
        } else {
            // Fallback for API < 29: Access files directly from the downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val contentUri = Uri.fromFile(file)
                    filesMap[file.name] = Pair(file.length(), contentUri)
                }
            }
        }

        return filesMap
    }

    private fun deleteDownloadedVideoContent(context: Context, uri: Uri): Boolean {
        return try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null) > 0
            }
        } catch (e: IllegalArgumentException) {
            deleteVideoContentFallback(context, uri)
        }
    }

    private fun deleteVideoContentFallback(context: Context, uri: Uri): Boolean {
        val displayName = getDisplayName(context, uri) ?: return false
        val videoUri = findVideoContentUriByName(context, displayName) ?: return false
        return context.contentResolver.delete(videoUri, null, null) > 0
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) cursor.getString(nameColumn) else null
        }
    }

    private fun findVideoContentUriByName(context: Context, displayName: String): Uri? {
        return listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.INTERNAL_CONTENT_URI
        ).firstNotNullOfOrNull { targetUri ->
            context.contentResolver.query(
                targetUri,
                arrayOf(MediaStore.Video.Media._ID),
                "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                arrayOf(displayName),
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(targetUri, cursor.getLong(idColumn))
                } else {
                    null
                }
            }
        }
    }

    private fun isExternalUri(uri: Uri): Boolean {
        val context = ContextUtils.getApplicationContext()

        // External content URI from MediaStore for Downloads
        val externalContentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            // Return null or handle in another way for API < 29
            null
        }

        // External file directories for app-specific storage and public storage
        val appSpecificExternalUri = Uri.fromFile(context.getExternalFilesDir(null))
        val publicDownloadsUri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))

        return when {
            // Check if the URI is in the MediaStore external Downloads content URI
            uri.toString().contains(externalContentUri.toString()) -> true

            // Check if the URI is in the app-specific external storage directory
            uri.toString().contains(appSpecificExternalUri.toString()) -> true

            // Check if the URI is in the public Downloads directory
            uri.toString().contains(publicDownloadsUri.toString()) -> true

            else -> false
        }
    }

    private fun moveFileToDownloadsFolder(
        contentResolver: ContentResolver, sourceFile: File, fileName: String
    ): Boolean {
        AppLogger.d("moveFileToDownloadsFolder $sourceFile $fileName")

        // Check if there is enough free space in the Downloads folder
        val downloadsDirectory = folderDir
        val isFolderExternal = isExternalUri(folderDir.toUri())
        val availableSpace = downloadsDirectory.freeSpace

        if (availableSpace < sourceFile.length()) {
            // Handle the case where there is not enough free space
            throw Error("Not enough available space $availableSpace, file size: ${sourceFile.length()}")
        }

        // API 29 and above (Scoped Storage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var name = fileName
            var counter = 1
            while (isDownloadExists(contentResolver, name)) {
                name = "$fileName($counter)"
                counter++
            }

            val cleanedFileName = FileNameCleaner.cleanFileName(name)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanedFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            // Insert the file into the Downloads collection via MediaStore
            val collectionUri = if (isFolderExternal) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Downloads.INTERNAL_CONTENT_URI
            }

            var fileUri = contentResolver.insert(collectionUri, values)
            if (fileUri == null) {
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, cleanedFileName.replace("mp4", "") + "_e")
                fileUri = contentResolver.insert(collectionUri, values)
            }

            // Copy the file to the Downloads folder
            val isMoved = fileUri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    val copiedBytes = sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    if (copiedBytes > 0) {
                        AppLogger.d("Source file removed... $sourceFile")
                        sourceFile.delete() // Delete the source file after moving
                        true
                    } else {
                        AppLogger.d("Source move error $sourceFile")
                        false
                    }
                }
            }
            return isMoved ?: false
        }
        // Below API 29 (Legacy File Access)
        else {
            val destination = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

            // If the file already exists, rename it with a counter
            var name = fileName
            var counter = 1
            while (destination.exists()) {
                name = "$fileName($counter)"
                counter++
            }

            val newFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)

            // Move file by copying and deleting the original
            return try {
                sourceFile.inputStream().use { inputStream ->
                    newFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                sourceFile.delete() // Delete the source file after copying
                true
            } catch (e: Exception) {
                AppLogger.e("Error moving file: ${e.message}")
                false
            }
        }
    }

    private fun isDownloadExists(contentResolver: ContentResolver, displayName: String): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        // API 29 and above: Use MediaStore.Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(displayName)

            val uri = if (isExternalUri(folderDir.toUri())) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Downloads.INTERNAL_CONTENT_URI
            }

            contentResolver.query(uri, projection, selection, selectionArgs, null).use { cursor ->
                return cursor?.moveToFirst() ?: false
            }
        }
        // Below API 29: Fallback method
        else {
            // Prior to Android 10 (Q), direct access to files is available
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), displayName)
            return file.exists()
        }
    }
}

object FileNameCleaner {
    private const val MAX_FILE_NAME_LENGTH = 100
    private val illegalChars = intArrayOf(
        34,
        60,
        62,
        124,
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        15,
        16,
        17,
        18,
        19,
        20,
        21,
        22,
        23,
        24,
        25,
        26,
        27,
        28,
        29,
        30,
        31,
        58,
        42,
        63,
        92,
        47
    )

    init {
        Arrays.sort(illegalChars)
    }

    fun cleanFileName(badFileName: String): String {
        val cleanName = StringBuilder()
        for (element in badFileName) {
            val c = element.code
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append(c.toChar())
            }
        }
        var finalName = cleanName.toString()
            .replace(".mp4", "")
            .replace("/", "").replace("\\", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace(".", "_")
            .replace("|", "")
            .replace(Regex("\\s*-\\s*"), "-")
            .replace(" ", "_").trim()
        if (finalName.isEmpty()) {
            finalName = "Untitled"
        }

        if (finalName.length > MAX_FILE_NAME_LENGTH) {
            return finalName.substring(0, MAX_FILE_NAME_LENGTH)
        }

        return finalName
    }
}
