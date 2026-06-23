package com.video.avd.ui.dialoges.videossorting.listners

import androidx.annotation.Keep
import java.io.Serializable


@Keep
interface OnSortChangedListner : Serializable {
    fun onSortChanged(isChanged : Boolean , sortType : Int)
}