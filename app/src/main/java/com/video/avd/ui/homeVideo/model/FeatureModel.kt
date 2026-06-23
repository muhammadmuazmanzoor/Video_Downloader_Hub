package com.video.avd.ui.homeVideo.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class FeatureModel(
    var id: Int,
    var iconResName: String,
    var name: String,
    var clickCount: Int = 0,
    var lastClicked: Long = 0L
) : Serializable