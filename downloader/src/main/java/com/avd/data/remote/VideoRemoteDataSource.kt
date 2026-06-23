package com.avd.data.remote

import com.avd.data.local.room.entity.VideoInfo
import com.avd.data.remote.service.VideoService
import com.avd.data.repository.VideoRepository
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRemoteDataSource @Inject constructor(
    private val videoService: VideoService
) : VideoRepository {

    override fun getVideoInfo(url: Request): VideoInfo? {
        return videoService.getVideoInfo(url)?.videoInfo
    }

    override fun saveVideoInfo(videoInfo: VideoInfo) {
    }
}