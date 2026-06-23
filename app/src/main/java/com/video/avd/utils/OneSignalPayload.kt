package com.video.avd.utils

import java.io.Serializable

data class OneSignalPayload(
    val fragment: String? = "",
    val deepLink: String? = "",

    ) : Serializable