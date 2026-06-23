package com.video.avd.data.local.videosDataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.video.avd.ui.folder.model.VideoFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface FoldersDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertfolder(video : List<VideoFolder>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertfolder(video : VideoFolder)

    @Query("SELECT * FROM Folders")
    fun getallFolders () : Flow<List<VideoFolder>>

    @Update
    suspend fun updateFolders(folders: List<VideoFolder>)

    @Query("SELECT * FROM folders WHERE id IN (:folderIds)")
    suspend fun getFoldersByIds(folderIds: List<Long>): List<VideoFolder>

    // Existing methods
    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): VideoFolder?

    @Update
    suspend fun updateFolder(folder: VideoFolder)

    // Count new videos in a specific folder
    @Query("SELECT COUNT(*) FROM Allvideos WHERE folderid = :folderId AND isNew = 1")
    suspend fun countNewVideosInFolder(folderId: String): Int

    @Query("SELECT * FROM folders WHERE name = :folderName LIMIT 1")
    suspend fun getFolderByName(folderName: String): VideoFolder?

    @Query("SELECT COUNT(*) FROM allvideos WHERE date >= :sevenDaysAgo")
    suspend fun getRecentVideosCount(sevenDaysAgo: Long): Int

    @Query("UPDATE Folders SET hasNewVideos = 0 WHERE id = :folderId")
    suspend fun setFolderHasNewToFalse(folderId: Long)

}