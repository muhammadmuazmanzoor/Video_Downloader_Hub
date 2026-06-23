package com.video.avd.ui.player.audiotrack

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C

class AudioTrackImpl :AudioTrack {


    override fun getAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }


}