package com.avd.util

import com.avd.ui.main.home.MainViewModel
import com.avd.ui.main.proxies.ProxiesViewModel
import com.avd.ui.main.settings.SettingsViewModel

object DownloaderModuleNavigator {

    var settingsViewModel : SettingsViewModel?= null
    var mainViewModel : MainViewModel? = null

    var proxiesViewModel : ProxiesViewModel ? = null

    fun setSettingViewModel(settingsViewModel: SettingsViewModel){
        this.settingsViewModel=settingsViewModel
    }

    fun setMMainViewModel(mainViewModel: MainViewModel){
        this.mainViewModel=mainViewModel
    }

    fun setProxyViewModel ( proxiesViewModel: ProxiesViewModel){
        this.proxiesViewModel=proxiesViewModel
    }


}