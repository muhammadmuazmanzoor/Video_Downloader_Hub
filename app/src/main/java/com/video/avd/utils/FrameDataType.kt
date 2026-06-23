package com.video.avd.utils

enum class FrameDataType {

    FRAME,
    AD;

    companion object {
        fun fromString(value: String): FrameDataType {
            return values().firstOrNull { it.name == value } ?: FRAME // Default value if not found
        }
    }


}