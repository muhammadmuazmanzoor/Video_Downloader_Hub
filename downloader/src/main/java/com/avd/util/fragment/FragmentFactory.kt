package com.avd.util.fragment

import androidx.fragment.app.Fragment
import com.avd.ui.main.downloder_queue.ui.main.FragmentDownloadQueue
import com.avd.ui.main.history.HistoryFragment
import com.avd.ui.main.link.LinkFragment
import com.avd.ui.main.settings.SettingsFragmentDownloader
import com.avd.ui.main.video.VideoFragment
import com.avd.browserkit.ui.browser.BrowserHostFragment
import com.avd.browserkit.api.BrowserLaunchMode
import javax.inject.Inject

interface FragmentFactory {
    fun createBrowserFragment(): Fragment
    fun createNewBrowserFragment(): Fragment
    fun createProgressFragment(): Fragment
    fun createVideoFragment(): Fragment
    fun createSettingsFragment(): Fragment
    fun createLinkFragment(): Fragment
    fun createHistoryFragment(): Fragment

    fun createBrowserHomeFragment(): Fragment

    fun createWebTabFragment(): Fragment

    fun createDetectedVideosTabFragment(): Fragment
}

class FragmentFactoryImpl @Inject constructor() : FragmentFactory {
    private fun createBrowserHost() =
        BrowserHostFragment.newInstance(BrowserLaunchMode.BLANK, null, null)

    override fun createBrowserFragment() = createBrowserHost()
    override fun createNewBrowserFragment() = createBrowserHost()

    override fun createProgressFragment() = FragmentDownloadQueue.newInstance()

    override fun createVideoFragment() = VideoFragment.newInstance()

    override fun createSettingsFragment() = SettingsFragmentDownloader.newInstance()

    override fun createLinkFragment() = LinkFragment.newInstance()

    override fun createHistoryFragment() = HistoryFragment.newInstance()

    override fun createBrowserHomeFragment() = createBrowserHost()

    override fun createWebTabFragment() = createBrowserHost()

    override fun createDetectedVideosTabFragment() = createBrowserHost()
}
