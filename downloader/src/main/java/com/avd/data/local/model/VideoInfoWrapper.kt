package com.avd.data.local.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.avd.data.local.room.entity.VideoInfo

data class VideoInfoWrapper(
    @SerializedName("info")
    @Expose
    var videoInfo: VideoInfo?
)