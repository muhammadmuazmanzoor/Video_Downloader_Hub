package com.video.avd.ui.videos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable


@Entity(tableName = "Allvideos")
data class Video(
    @PrimaryKey
    var id: Long = 0L,
    var contentUri: String? = null,
    var title: String? = null,
    var duration: String = "",
    var date: String = "",
    var size: String = "",
    var orignalpath: String = "",
    var isChecked: Boolean = false,
    var lastPlayed: Long = 0L,
    var folderid: String = "",
    var timeStump: Long? = null,
    var updatedTimeStump: Long? = null,
    var isRecent: Boolean = false,
    var playedCompletely: Boolean = false,
    var playedOver90Percent: Boolean = false,
    var playedPercentage : Int = 90,
    var isNew : Boolean=false
) : Serializable
