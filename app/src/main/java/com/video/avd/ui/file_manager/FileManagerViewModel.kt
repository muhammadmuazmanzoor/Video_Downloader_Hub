package com.video.avd.ui.file_manager

import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.DecimalFormat
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor() : ViewModel() {

    private val _directories= MutableLiveData<List<DirectoryModel>>()
    val directories : LiveData<List<DirectoryModel>> = _directories

    init {
        _directories.postValue(emptyList())
        collectDirectories()
    }

    private fun collectDirectories() {
        viewModelScope.launch {
            val root = Environment.getExternalStorageDirectory()
            val directoriesFlow = listDirectories(root)

            directoriesFlow.map { list ->
                list.asFlow()
                    .filter { shouldProcessDirectory(it.name.lowercase(Locale.getDefault())) }
                    .map { dir -> processDirectory(dir) }
                    .flowOn(Dispatchers.Default) // Perform mapping in parallel
                    .toList()
            }.collect { processedList ->
                _directories.postValue(processedList.sortedBy { it.name.lowercase() })
            }
        }
    }

    private suspend fun processDirectory(dir: DirectoryModel): DirectoryModel {
        return withContext(Dispatchers.IO) { // Offload to background thread
            val file = File(dir.path)

            var attrs : BasicFileAttributes?=null
            var creationTime : Instant? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                creationTime = attrs.creationTime().toInstant()
            }else{
                creationTime = null
            }

            val subFolderCount = countFolders(file)
            val subFolderCountWithoutHidden = countNonHiddenFolders(file)
            val (videos, audios) = countVideosAndAudios(file)

            DirectoryModel(
                dir.name,
                subFolderCount.toString(),
                dir.path,
                createdDate = creationTime.toString(),
                folderSize = "",
                videoCount = videos.toString(),
                audioCount = audios.toString(),
                subFolderCountWithoutHidden = subFolderCountWithoutHidden.toString()
            )
        }
    }

    private fun shouldProcessDirectory(name: String): Boolean {
        val favoriteFolders = setOf("download", "downloads", "music", "movie", "movies", "whatsapp")
        return name in favoriteFolders
    }

    private fun <A, B> Iterable<A>.parallelMap(transform: suspend (A) -> B): List<B> {
        return runBlocking {
            map { async(Dispatchers.Default) { transform(it) } }.awaitAll()
        }
    }



    private fun listDirectories(root: File): Flow<List<DirectoryModel>> = flow {
        val directories = mutableListOf<DirectoryModel>()
        val files = root.listFiles() ?: return@flow

        // Parallelize directory processing
        val deferredList = files.filter { it.isDirectory }.map { file ->
            viewModelScope.async(Dispatchers.IO) { processFile(file) }
        }

        deferredList.awaitAll().filterNotNull().let { directories.addAll(it) }
        emit(directories)
    }.flowOn(Dispatchers.IO)

    private suspend fun processFile(file: File): DirectoryModel? {
        return try {
            var attrs : BasicFileAttributes? = null
            var creationTime : Instant? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                creationTime = attrs.creationTime().toInstant()
            }else{
                creationTime = null
            }


            val subFolderCount = countFolders(file)
            val subFolderCountWithoutHidden = countNonHiddenFolders(file)
            val (videos, audios) = countVideosAndAudios(file)

            DirectoryModel(
                file.name,
                subFolderCount.toString(),
                file.absolutePath,
                createdDate = creationTime.toString(),
                folderSize = "",
                videoCount = videos.toString(),
                audioCount = audios.toString(),
                subFolderCountWithoutHidden = subFolderCountWithoutHidden.toString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun countFolders(file: File): Int {
        var subFolderCount = 0
        try {
            file.listFiles()?.forEach { subFile ->
                if (subFile.isDirectory) subFolderCount++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return subFolderCount
    }
    private fun countNonHiddenFolders(file: File): Int {
        var subFolderCount = 0
        try {
            file.listFiles()?.forEach { subFile ->
                // Check if it's a directory and the name does not start with "."
                if (subFile.isDirectory && !subFile.name.startsWith(".")) {
                    subFolderCount++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return subFolderCount
    }

    private fun countVideosAndAudios(file: File): Pair<Int, Int> {
        var videos = 0
        var audios = 0
        try {
            file.listFiles()?.forEach { subFile ->
                if (subFile.isFile && isVideoFile(subFile)) videos++
                else if (subFile.isFile && isAudioFile(subFile)) audios++
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(videos, audios)
        }
        return Pair(videos, audios)
    }


    private fun isVideoFile(file: File): Boolean {
        try {
            // val audioExtensions = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg")
            val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "flv", "wmv", "ts")
            val extension = file.extension.lowercase()
            return extension in videoExtensions
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun isAudioFile(file: File): Boolean {
        try {
            val audioExtensions = setOf("mp3", "wav", "aac", "m4a", "flac", "ogg")
            // val videoExtensions = setOf("mp4", "avi", "mov", "mkv", "flv", "wmv", "ts)
            val extension = file.extension.lowercase()
            return extension in audioExtensions
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    suspend fun sortDirectoryList(
        sortType: Int, dirList: List<DirectoryModel>
    ): List<DirectoryModel> = withContext(Dispatchers.IO) {
        try {
            when (sortType) {
                0 -> dirList.sortedBy { it.name } // Name A to Z
                1 -> dirList.sortedByDescending { it.name } // Name Z to A
                2 -> dirList.sortedByDescending { it.createdDate } // Date New to Old
                3 -> dirList.sortedBy { it.createdDate } // Date Old to New
                4 -> dirList.sortedByDescending { it.folderSize } // Size Big to Small
                5 -> dirList.sortedBy { it.folderSize } // Size Small to Big
                else -> dirList // Default order (0: Name A to Z)
            }
        } catch (e: Exception) {
            dirList
        }

    }

    fun getStorageInfo(): FileManagerFragment.StorageInfo? {
        try {
            val root = Environment.getExternalStorageDirectory()
            val stat = StatFs(root.path)

            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalSpace = totalBlocks * blockSize
            val freeSpace = availableBlocks * blockSize
            val usedSpace = totalSpace - freeSpace

            return FileManagerFragment.StorageInfo(freeSpace, usedSpace, totalSpace)
        } catch (e: Exception) {
            Log.e("StorageInfo", "Error getting storage info: ${e.message}")
            return null
        }
    }

    fun bytesToGB(bytes: Long): String {
        return try {
            val gigabyte = 1024.0 * 1024.0 * 1024.0
            val result = bytes / gigabyte
            // Format the result to have two decimal places
            val decimalFormat = DecimalFormat("#.##")
            decimalFormat.format(result)
        } catch (e: Exception) {
            e.printStackTrace()
            "0.00"
        }
    }

    private fun getDirectorySize(list: List<String>) {
        list.forEach { dir ->
            getFolderSize(File(dir))
        }
    }

    private fun getFolderSize(directory: File): Long {
        return try {
            var length = 0L
            directory.listFiles()?.forEach { file ->
                length += if (file.isFile) file.length() else getFolderSize(file)
            }
            length
        } catch (e: Exception) {
            0L
        }
    }


}