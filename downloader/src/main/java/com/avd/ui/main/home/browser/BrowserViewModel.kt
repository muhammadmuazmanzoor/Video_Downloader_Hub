package com.avd.ui.main.home.browser

//import com.allVideoDownloaderXmaster.OpenForTesting
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avd.data.local.room.entity.VideoInfo
import com.avd.ui.main.base.BaseViewModel
import com.avd.ui.main.home.browser.webTab.WebTab
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        const val SEARCH_URL = "https://www.google.com/search?q=%s"

        var instance: BrowserViewModel? = null
    }
    var settingsModel: SettingsViewModel? = null

    val openPageEvent = SingleLiveEvent<WebTab>()

    val closePageEvent = SingleLiveEvent<WebTab>()

    val selectWebTabEvent = SingleLiveEvent<WebTab>()

    val updateWebTabEvent = SingleLiveEvent<WebTab>()

    val workerM3u8MpdEvent = MutableLiveData<DownloadButtonState>()

    val progress = ObservableInt(0)

    val changeSearchFocusEvent = SingleLiveEvent<Boolean>()

    val tabs = ObservableField(listOf(WebTab.HOME_TAB))

    val _tabscount = MutableLiveData<Int>()

    val tabscount: LiveData<Int> get() = _tabscount

    val currentTab = ObservableInt(HOME_TAB_INDEX)

    override fun start() {
        instance = this
    }

    override fun stop() {
        instance = null
    }

     fun handleRemoveAllTabsExceptHome() {
        // Always set the tabs to only HOME_TAB
        val tab = mutableListOf(WebTab.HOME_TAB)
        // Reset the tabs in the ViewModel
        tabs.set(tab)
        // Reset the current tab to HOME_TAB_INDEX (if necessary)
        currentTab.set(HOME_TAB_INDEX)
        // Update the tabs count
        _tabscount.value = tab.size
    }

     fun openNewTabWithDefaultUrl(defaultUrl: String) {
        // Create a new WebTab with the default URL
        val newWebTab = WebTab( url = defaultUrl,null,null)
        // Update the tabs list with the new tab
        val newList = tabs.get()?.plus(newWebTab) ?: listOf(newWebTab)
        tabs.set(newList)
        // Update tab count
        _tabscount.value = newList.size -1
        // Set the new tab as the current tab
        val index = newList.indexOf(newWebTab)
        currentTab.set(index.coerceAtLeast(0))
    }

    private fun generateUniqueTabId(): String {
        return System.currentTimeMillis().toString() // Example: use a timestamp
    }

}

abstract class DownloadButtonState

class DownloadButtonStateLoading : DownloadButtonState()

class DownloadButtonStateCanDownload(val info: VideoInfo?) : DownloadButtonState()
class DownloadButtonStateCanNotDownload : DownloadButtonState()