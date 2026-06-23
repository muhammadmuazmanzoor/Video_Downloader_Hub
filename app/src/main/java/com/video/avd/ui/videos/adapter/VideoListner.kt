package com.video.avd.ui.videos.adapter

import com.video.avd.ui.videos.model.Video

interface  VideoListner {

    fun onVideoClick(id: String, list: ArrayList<Video>)

    fun onVideoDelete(item : Video)

    fun reNameVideo(position : Int)
}