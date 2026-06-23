package com.video.avd.ui.file_manager

import com.video.avd.ui.videos.model.Video


sealed class MediaResources {
    data class VideoItems(val item: Video) : MediaResources()
    data class DirectoryItems(val item: DirectoryModel) : MediaResources()
}