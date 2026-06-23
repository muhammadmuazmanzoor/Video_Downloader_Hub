package com.video.avd.ui.player.bookmark

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "VideoBookmark")
data class VideoBookmark(
    val videoUri : String = "",
    val position : Long = 0L,
    val timeStamp : Long = 0L,
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val bookmarkName : String = ""
)
