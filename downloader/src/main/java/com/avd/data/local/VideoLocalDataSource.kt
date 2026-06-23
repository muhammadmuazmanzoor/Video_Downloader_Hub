package com.avd.data.local

import com.avd.data.local.room.dao.VideoDao
import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.repository.VideoRepository
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoLocalDataSource @Inject constructor(
    private val videoDao: VideoDao
) : VideoRepository {

    override fun getVideoInfo(url: Request): VideoInfo? {
        return try {
            // Use blockingGet with timeout to prevent indefinite blocking
            // This prevents ANRs by ensuring the operation completes within a reasonable time
            videoDao.getVideoById(url.url.toString())
                .toSingle()
                .timeout(2, TimeUnit.SECONDS) // 2 second timeout to prevent ANR
                .blockingGet()
        } catch (e: Exception) {
            // Return null if query fails or times out instead of blocking indefinitely
            e.printStackTrace()
            null
        }
    }

    override fun saveVideoInfo(videoInfo: VideoInfo) {
        videoDao.insertVideo(videoInfo)
    }

}