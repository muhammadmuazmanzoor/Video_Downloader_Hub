package com.video.avd.ui.dialoges.audiossorting.listners

import java.io.Serializable

interface OnAudiosSortChangedListner : Serializable {
    fun onSortChanged(isChanged : Boolean , sortType : Int)
}