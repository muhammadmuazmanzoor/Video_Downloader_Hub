package com.avd.util.fragment

import androidx.fragment.app.Fragment
import com.avd.ui.main.downloder_queue.ui.main.FragmentDownloadQueue
import com.avd.ui.main.history.HistoryFragment
import com.avd.ui.main.home.browser.BrowserFragment
import com.avd.ui.main.home.browser.homeTab.BrowserHomeFragment
import com.avd.ui.main.home.browser.webTab.WebTabFragment
import com.avd.ui.main.home.browser.detectedVideos.DetectedVideosTabFragment
import com.avd.ui.main.home.browser.webTab.NewBrowserFragment
import com.avd.ui.main.link.LinkFragment
import com.avd.ui.main.settings.SettingsFragmentDownloader
import com.avd.ui.main.video.VideoFragment
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
    override fun createBrowserFragment() = BrowserFragment.newInstance()
    override fun createNewBrowserFragment() = NewBrowserFragment.newInstance()

    override fun createProgressFragment() = FragmentDownloadQueue.newInstance()

    override fun createVideoFragment() = VideoFragment.newInstance()

    override fun createSettingsFragment() = SettingsFragmentDownloader.newInstance()

    override fun createLinkFragment() = LinkFragment.newInstance()

    override fun createHistoryFragment() = HistoryFragment.newInstance()

    override fun createBrowserHomeFragment() = BrowserHomeFragment.newInstance()

    override fun createWebTabFragment() = WebTabFragment.newInstance()

    override fun createDetectedVideosTabFragment() = DetectedVideosTabFragment.newInstance()
}