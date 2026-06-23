package com.video.avd.ui.player.subtitle

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "SubtitleState",indices = [Index(value = ["videoId"], unique = true)])
data class SubtitleState(
    val id : Int=0,
    @PrimaryKey
    val videoId: Long, // Unique video ID as the primary key
    val subtitlePath : String,
    val hasSubtitle : Boolean,
    val toggle : Boolean,
)
