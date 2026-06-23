package com.video.avd.ui.equalizer

import java.io.Serializable

/**
 * Created by Harjot on 09-Dec-16.
 */
class EqualizerModel : Serializable {
    var isEqualizerEnabled = true
    var seekbarpos = IntArray(5)
    var presetPos = 0
    var reverbPreset: Short = -1
    var bassStrength: Short = -1

}