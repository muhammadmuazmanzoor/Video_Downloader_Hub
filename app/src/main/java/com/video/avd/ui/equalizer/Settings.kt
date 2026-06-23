package com.video.avd.ui.equalizer

object Settings {
    var isEqualizerEnabled = true
    var isEqualizerReloaded = true
    var seekbarpos = IntArray(5)
    var presetPos = 0
    var reverbPreset: Short = -1
    var bassStrength: Short = -1
    var equalizerModel: EqualizerModel? = null
    var ratio = 1.0
    var isEditing = false
}