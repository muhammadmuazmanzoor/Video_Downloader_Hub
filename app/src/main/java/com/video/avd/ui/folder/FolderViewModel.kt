package com.video.avd.ui.folder

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.video.avd.repo.FolderRepository
import com.video.avd.ui.folder.model.VideoFolder
import com.video.avd.ui.videos.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class FolderViewModel @Inject constructor(
    private val repository: FolderRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val videosListFlow = repository.getAllVideos(context)





    suspend fun sortVideoFoldersListOld(sortType: Int, videos: List<VideoFolder>): List<VideoFolder> = withContext(Dispatchers.IO) {

        if (videos.isEmpty()) {
            return@withContext videos
        }
        // Partition the list into "Recent Added", "Storage", and others
        val recentAddedFolder = videos.find { it.name == "Recent Added" }
        val storageFolder = videos.find { it.name == "Directories" && it.id == 786000000L }
        val otherFolders = videos.filter { it != recentAddedFolder && it != storageFolder }

        // Sort the other folders based on sortType
        val sortedVideos = when (sortType) {
            0 -> otherFolders.sortedBy { it.name } // Name A to Z
            1 -> otherFolders.sortedByDescending { it.name } // Name Z to A
            2 -> otherFolders.sortedByDescending { it.dateAdded ?: 0L } // Date New to Old
            3 -> otherFolders.sortedBy { it.dateAdded ?: 0L } // Date Old to New
            4 -> otherFolders.sortedByDescending { it.size ?: 0L } // Size Big to Small
            5 -> otherFolders.sortedBy { it.size ?: 0L } // Size Small to Big
            else -> otherFolders
        }
        // Define the custom "Storage" folder
        val customFolder = VideoFolder(
            id = 786000000L,
            name = "Directories",
            dateAdded = 0,
            size = 0,
            videoCount = 0
        )
        // Combine lists appropriately
        val finalList = mutableListOf<VideoFolder>()
        // Add "Recent Added" at the start
        recentAddedFolder?.let { finalList.add(it) }
        // Add sorted folders in the middle
        finalList.addAll(sortedVideos)
        // Add "Storage" at the end only if not already present
        if (storageFolder != null) {
            finalList.add(storageFolder) // Use existing "Storage" folder
        } else {
            finalList.add(customFolder) // Add custom folder if not present
        }
        finalList
    }


    suspend fun sortVideoFoldersList(sortType: Int, videos: List<VideoFolder>): List<VideoFolder> = withContext(Dispatchers.IO) {

        if (videos.isEmpty()) {
            return@withContext videos
        }
        // Partition the list into "Recent Added", "Storage", and others
        val recentAddedFolder = videos.find { it.name == "Recent Added" }
        val storageFolder = videos.find { it.name == "Directories" && it.id == 786000000L }
        val otherFolders = videos.filter { it != recentAddedFolder && it != storageFolder }

        // Sort the other folders based on sortType
        val sortedVideos = when (sortType) {
            0 -> otherFolders.sortedBy { it.name } // Name A to Z
            1 -> otherFolders.sortedByDescending { it.name } // Name Z to A
            2 -> otherFolders.sortedByDescending { it.dateAdded ?: 0L } // Date New to Old
            3 -> otherFolders.sortedBy { it.dateAdded ?: 0L } // Date Old to New
            4 -> otherFolders.sortedByDescending { it.size ?: 0L } // Size Big to Small
            5 -> otherFolders.sortedBy { it.size ?: 0L } // Size Small to Big
            else -> otherFolders
        }

        // Define the custom "Storage" folder
        val customFolder = VideoFolder(
            id = 786000000L,
            name = "Directories",
            dateAdded = 0,
            size = 0,
            videoCount = 0
        )


        // Combine lists appropriately
        val finalList = mutableListOf<VideoFolder>()
        // Add "Recent Added" at the start
        recentAddedFolder?.let { finalList.add(it) }
        // Add sorted folders in the middle
        finalList.addAll(sortedVideos)
        // Add "Storage" at the end only if not already present
        if (storageFolder != null) {
            finalList.add(storageFolder) // Use existing "Storage" folder
        } else {
            finalList.add(customFolder) // Add custom folder if not present
        }
        finalList
    }



    fun addVideoToDb(listofvideos: List<Video>,fragmentActivity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            val foldersListFlow=repository.getVideoFoldersWithCount(fragmentActivity)
            collectAndInsertFolders(foldersListFlow)
            val existingvideoId=repository.database.videosDao().getAllVideoIds()
            // Iterate over the incoming list of videos and set isNew flag
            if (existingvideoId.isNotEmpty()){
                listofvideos.forEach { video ->
                    video.isNew = video.id !in existingvideoId
                }
            }
            repository.database.videosDao().insertvideos(listofvideos)
            repository.updateFoldersWithNewVideos(listofvideos)
        }
    }

    suspend fun collectAndInsertFolders(foldersListFlow: Flow<List<VideoFolder>>?) { foldersListFlow
            ?.debounce(500)
            ?.distinctUntilChanged()
            ?.collectLatest { folders ->
                withContext(Dispatchers.IO) {
                    repository.insetfolderintodb(folders) // Batch insert in the background
                }
            }
    }

    fun getFoldersFromDb(): Flow<List<VideoFolder>> {
        return repository.database.folderdao().getallFolders()
    }


}