package com.video.avd.data.local

import androidx.room.*
import androidx.room.Dao
import com.video.avd.ui.player.subtitle.SubtitleState
import com.video.avd.ui.videos.model.Video
import kotlinx.coroutines.flow.Flow


/**THIS IS HISTORY DAO**/
@Dao
interface Dao {

    @Update
    suspend fun insertEntities(entities: Video)

    @Query("UPDATE Allvideos SET isRecent = 0 WHERE id = :videoId")
    suspend fun deleteEntities(videoId: Long)

    @Query("DELETE FROM Allvideos")
    suspend fun deleteAllData()

    @Query("SELECT * FROM Allvideos WHERE isRecent = 1 ORDER By updatedTimeStump DESC")
    fun getEntitiesWithUpdatedTimeStump(): Flow<List<Video>>


    @Query("SELECT * FROM Allvideos where contentUri = :url")
    fun getEntitiesWithUrl(url: String?): Video

    //@Query("SELECT * FROM Allvideos WHERE playedCompletely = 1")
    @Query("SELECT * FROM Allvideos WHERE playedCompletely = 1 OR playedOver90Percent = 1")
    fun getVideosPlayedCompletely(): Flow<List<Video>>

    @Query("update  Allvideos set  updatedTimeStump =:updatedTimeStump" + " WHERE contentUri=:url")
    fun updateByTimeStump(
        url: String?,
        updatedTimeStump: Long?,
    )

    @Query("SELECT * FROM Allvideos WHERE title = :userId")
    fun getSongById(userId: String): Video?

    @Query("DELETE FROM Allvideos WHERE title = :songName")
    suspend fun deleteSongByName(songName: String)

    @Query("DELETE FROM Allvideos WHERE contentUri = :uri")
    suspend fun deleteSongByUri(uri : String)

    @Query("UPDATE Allvideos SET isRecent = 0")
    suspend fun markAllVideosAsNotRecent()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitleWithVideoId(subtitle: SubtitleState)

    @Query("UPDATE SubtitleState SET toggle = :toggle WHERE videoId = :videoId")
    suspend fun updateSubtitleState(videoId: Long, toggle: Boolean)

    @Query("SELECT * FROM SubtitleState where videoId = :videoId")
    fun getVideoSubtitle(videoId: Long): SubtitleState


}