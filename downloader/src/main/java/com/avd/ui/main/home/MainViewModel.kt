package com.avd.ui.main.home

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
//import com.allVideoDownloaderXmaster.OpenForTesting
import com.avd.data.local.room.entity.VideoInfo
import com.avd.ui.main.base.BaseViewModel
import com.avd.ui.main.home.browser.BrowserServicesProvider
import com.avd.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : BaseViewModel() {

    var isLink=false
    var url=""

    val showInterstitialAdEvent = SingleLiveEvent<Void?>()

    var browserServicesProvider: BrowserServicesProvider? = null

    val openedUrl = ObservableField<String?>()

    val openedText = ObservableField<String?>()

    val isBrowserCurrent = ObservableBoolean(false)

    val currentItem = ObservableField<Int>()

    val offScreenPageLimit = ObservableField(4)

    // pair - format:url
    val selectedFormatTitle = ObservableField<Pair<String, String>?>()

    val currentOriginal = ObservableField<String>()

    val downloadVideoEvent = SingleLiveEvent<VideoInfo>()

    val openDownloadedVideoEvent = SingleLiveEvent<String>()

    val openNavDrawerEvent = SingleLiveEvent<Unit?>()

    override fun start() {
    }

    override fun stop() {
    }

}
