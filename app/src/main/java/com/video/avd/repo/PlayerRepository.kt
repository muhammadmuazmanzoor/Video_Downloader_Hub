package com.video.avd.repo

import android.util.Log
import com.video.avd.data.local.AppDatabase
import com.video.avd.ui.player.bookmark.VideoBookmark
import com.video.avd.ui.player.subtitle.SubtitleState
import com.video.avd.ui.videos.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(local: AppDatabase) {
    val localRepo = local


    fun updateUserData(userEntities: Video) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localRepo.dao().insertEntities(userEntities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun insertSubtitleWithVideoId(subtitle : SubtitleState){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localRepo.dao().insertSubtitleWithVideoId(subtitle)
                Log.e("SearchDialog", "Insert Subtitle Video Detail:$subtitle")
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }


    suspend fun getVideoWithSubtitle(videoId: Long): SubtitleState? {
        return try {
            withContext(Dispatchers.IO) {
                localRepo.dao().getVideoSubtitle(videoId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updateSubtitleState(videoId : Long, toggle : Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localRepo.dao().updateSubtitleState(videoId, toggle)
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }

     fun addVideoBookmark( bookmark : VideoBookmark){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localRepo.videosDao().addVideoBookmark(bookmark)
            }catch (e : Exception){
                e.printStackTrace()
            }
        }
    }


    suspend fun getVideoBookmarksByUri(uri: String): Flow<List<VideoBookmark>>? {
        return try {
            withContext(Dispatchers.IO) {
                localRepo.videosDao().getBookmarksByVideoUri(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteVideoBookmark(uri : String, timeStamp : Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localRepo.videosDao().deleteVideoBookmark(timeStamp, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

     fun renameBookmark(uri : String, timeStamp : Long,name : String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                localRepo.videosDao().renameBookmark(uri,timeStamp,name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}