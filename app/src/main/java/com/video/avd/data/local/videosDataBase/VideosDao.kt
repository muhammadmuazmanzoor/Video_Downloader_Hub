package com.video.avd.data.local.videosDataBase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.video.avd.ui.player.bookmark.VideoBookmark
import com.video.avd.ui.videos.model.Video
import kotlinx.coroutines.flow.Flow

@Dao
interface VideosDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertvideos(video : List<Video>)

    @Query("SELECT * FROM Allvideos")
    fun getAllData(): Flow<List<Video>>

    @Query("SELECT * FROM Allvideos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Int): Video?

    @Query("SELECT * FROM Allvideos WHERE title IN (SELECT title FROM Allvideos GROUP BY title HAVING COUNT(*) > 1)")
    fun getDuplicateVideosByTitle(): Flow<List<Video>>

    @Query("SELECT * FROM Allvideos WHERE size > 52428800")
    fun getLargeFiles(): Flow<List<Video>>

    @Query("DELETE  From Allvideos WHERE title=:title")
    fun deleteData(title:String)

    @Query("DELETE FROM Allvideos WHERE id = :videoId")
    fun deleteDataFromDb(videoId: Long) : Int

    @Query("DELETE FROM Allvideos WHERE title IN (:titles)")
    suspend fun deleteVideos(titles: List<String>)

    @Query("DELETE FROM Allvideos WHERE id IN (:ids)")
    suspend fun deleteVideosById(ids: List<Long>)

    @Query("UPDATE Allvideos SET title = :newTitle WHERE id = :id")
    suspend fun updateVideoTitleById(id: Long, newTitle: String)

    @Query("SELECT * FROM Allvideos WHERE folderId = :id")
    fun getFolderVideos(id:String) : Flow<List<Video>>

    @Query("SELECT id FROM Allvideos")
    fun getAllVideoIds(): List<Long>

    //Video Bookmark Queries
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoBookmark(bookmark: VideoBookmark)

    @Query("SELECT * FROM VideoBookmark WHERE videoUri = :uri")
    fun getBookmarksByVideoUri(uri:String) : Flow<List<VideoBookmark>>

    @Query("DELETE FROM VideoBookmark WHERE timeStamp =:timeStamp AND videoUri=:uri")
    suspend fun deleteVideoBookmark(timeStamp: Long, uri : String)

    @Query("UPDATE VideoBookmark SET bookmarkName = :name WHERE timeStamp = :timeStamp AND videoUri = :uri")
    suspend fun renameBookmark(uri: String,timeStamp: Long, name: String)

    @Query("SELECT * FROM allvideos WHERE date > :sevenDaysAgo ORDER BY date DESC")
    fun getRecentVideos(sevenDaysAgo: String): Flow<List<Video>>

}