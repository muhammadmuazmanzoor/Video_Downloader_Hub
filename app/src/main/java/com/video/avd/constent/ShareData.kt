package com.video.avd.constent

import androidx.lifecycle.MutableLiveData
import com.video.avd.ui.videos.model.Video


var isSplash: Boolean = true
var isFirstTime: Boolean = true
var isvideo = false
var videolistglobal = listOf<Video>()
var videoListLocal: MutableLiveData<MutableList<Video>> = MutableLiveData()
var currentSongPlaying: MutableLiveData<Int> = MutableLiveData()
var fromVault = false

// for progress bar of casting
var isClickedForCasting = MutableLiveData<Boolean>(false)

var homeCastingClicked = false

var backFromPlayer = false

var isExpendedRunning = false

var isCastingForStreaming = true
var isDataInitialized = MutableLiveData<Boolean>()

var isMusicPlayerNativeSeenAlready = false

var is2Adwatched =0

@JvmField
var VIEW_TYPE = MutableLiveData<Int>(0) //0 for listview, 1 for grid view
const val GRID_ITEM_SPAN_COUNT = 2
var isUserAdSeen : Int =0

@JvmField
        /**
        0  for name a to z
        1  for name z to a
        2  for date new to old
        3  for date old to new
        4  for size big to small
        5  for size small to big
        6 for duration larger to smaller
        7 for duration smaller to larger
         */

var SORT_TYPE = MutableLiveData<Int>(2)
var SHORT_SORT_TYPE = MutableLiveData<Int>(4)
var AUDIO_SORT_TYPE = MutableLiveData<Int>(2)

/**
0 ->  order
1 -> loop all
2  -> shuffle all
3  -> repeat current
 */

var POP_UP_LIST_ORDER_TYPE = MutableLiveData<Int>(0)
var VIDEO_PLAYER_ORDER_TYPE = MutableLiveData<Int>(0)

var VIDEO_PAUSE = MutableLiveData<Boolean>()

val isFileSave = MutableLiveData<Boolean>(false)


//// all video recycler update control
var shouldUpdateRecyclerView = MutableLiveData<Boolean>(true)
var fromdownload = false
var showfloatandhide=MutableLiveData<Boolean>(false)
var singularinitialze=false
var splashAdClick=false
var isbackfromplayer=false
var isnotification =false
var isinternal = false



