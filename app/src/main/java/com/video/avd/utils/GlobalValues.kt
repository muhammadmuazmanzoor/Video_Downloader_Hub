package com.video.avd.utils

import androidx.lifecycle.MutableLiveData
import com.inmobi.media.fa

object GlobalValues {
    //InApp Purchases
    //to check if user jas availed 24 hours ads
    var is24hourEnabled = MutableLiveData<Boolean>(false)
    var newProType = false
    var fromSplash=false
    var link:String=""
    var hidePopupPlayer = MutableLiveData<Boolean>()
    var disableAudioEqualizer = MutableLiveData<Boolean>(false)
    var disableVideoEqualizer = MutableLiveData<Boolean>(true)
}