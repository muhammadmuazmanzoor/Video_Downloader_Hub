package com.video.avd.data.local

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.video.avd.constent.DatabaseConstant.VAULT_VIDEO_TABLE_NAME
import java.io.Serializable


@Keep
@Entity(tableName = VAULT_VIDEO_TABLE_NAME)
data class VideoEntity(
    @PrimaryKey val id: Long? = 0,
    val displayName: String? = "",
    val originalPath: String? = "",
    val contentUri: String? = "",
    val duration: Long? = 0,
    val size: Int? = 0,
    val date: Long? = 0,
    var newPath: String? = ""
) : Serializable