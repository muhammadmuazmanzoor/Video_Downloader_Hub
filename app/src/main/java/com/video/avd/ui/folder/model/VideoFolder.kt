package com.video.avd.ui.folder.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.video.avd.utils.FrameDataType
import java.io.Serializable

@Keep
@Entity(tableName = "Folders")
data class VideoFolder(
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,
    val name: String = "",
    val videoIds: MutableList<Long> = mutableListOf(),
    var videoCount: Int = videoIds.size,
    var size: Long = 0,
    var dateAdded: Long = 0,
    val type: FrameDataType = FrameDataType.FRAME,
    var hasNewVideos: Boolean = false
) : Serializable