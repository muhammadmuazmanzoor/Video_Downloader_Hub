package com.video.avd.data.local

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.video.avd.constent.DatabaseConstant
import com.video.avd.utils.FrameDataType

/**THIS IS HISTORY ENTITY**/
@Keep
@Entity(tableName = DatabaseConstant.TABLE_DATA)
data class Entities(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val name: String? = null,
    var duration: Long? = null,
    val url: String? = null,
    val timeStump: Long? = null,
    val updatedTimeStump: Long? = null,
    val type: FrameDataType = FrameDataType.FRAME
)

