package com.avd.ui.main.home.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.activity.addCallback
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avd.R
import com.avd.data.remote.service.YoutubedlHelper
import com.avd.databinding.FragmentBrowseBinding
import com.avd.databinding.FragmentBrowserBinding
import com.avd.ui.component.adapter.WebTabsAdapter
import com.avd.ui.component.adapter.WebTabsListener
import com.avd.ui.main.base.BaseFragment
import com.avd.ui.main.history.HistoryViewModel
import com.avd.ui.main.home.MainViewModel
import com.avd.ui.main.home.bottomsheet.TabBottomSheetFragment
import com.avd.ui.main.home.browser.detectedVideos.VideoDetectionAlgVModel
import com.avd.ui.main.home.browser.homeTab.BrowserHomeFragment
import com.avd.ui.main.home.browser.webTab.BrowserTabFragment
import com.avd.ui.main.home.browser.webTab.WebTab
import com.avd.ui.main.home.browser.webTab.WebTabFragment
import com.avd.ui.main.progress.WrapContentLinearLayoutManager
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.AppUtil
import com.avd.util.CookieUtils
import com.avd.util.DownloaderModuleNavigator
import com.avd.util.SharedPrefHelper
import com.avd.util.SingleLiveEvent
import com.avd.util.proxy_utils.CustomProxyController
import com.avd.util.proxy_utils.OkHttpProxyClient
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


interface BrowserServicesProvider : TabManagerProvider, PageTabProvider, HistoryProvider, WorkerEventProvider, CurrentTabIndexProvider

interface TabManagerProvider {
    fun getOpenTabEvent(): SingleLiveEvent<WebTab>

    fun getCloseTabEvent(): SingleLiveEvent<WebTab>

    fun getUpdateTabEvent(): SingleLiveEvent<WebTab>

    fun getTabsListChangeEvent(): ObservableField<List<WebTab>>
}

interface PageTabProvider {
    fun getPageTab(position: Int): WebTab
}

interface HistoryProvider {
    fun getHistoryVModel(): HistoryViewModel
}

interface WorkerEventProvider {
    fun getWorkerM3u8MpdEvent(): MutableLiveData<DownloadButtonState>
}

interface CurrentTabIndexProvider {
    fun getCurrentTabIndex(): ObservableInt
}

interface BrowserListener {
    fun onBrowserMenuClicked()

    fun onBrowserReloadClicked()

    fun onTabCloseClicked()

    fun onBrowserStopClicked()

    fun onBrowserBackClicked()

    fun onBrowserHomeClicked()

    fun onBrowserForwardClicked()
}

const val HOME_TAB_INDEX = 0

const val TAB_INDEX_KEY = "TAB_INDEX_KEY"

@AndroidEntryPoint

class BrowserFragment : BaseFragment(), BrowserServicesProvider {

    companion object {
        fun newInstance() = BrowserFragment()
        var DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"
        // TODO different agents for different androids
        var MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36"
    }

    private lateinit var tabsAdapter: TabsFragmentStateAdapter

    private lateinit var drawerAdapter: WebTabsAdapter


    @Inject
    lateinit var appUtil: AppUtil

    @Inject
    lateinit var proxyController: CustomProxyController

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    @Inject
    lateinit var youtubedlHelper: YoutubedlHelper

    @VisibleForTesting
    internal lateinit var dataBinding: FragmentBrowserBinding

    private  val browserViewModel: BrowserViewModel by activityViewModels()

    private  val mainViewModel: MainViewModel by activityViewModels()

    private val historyModel: HistoryViewModel by viewModels()

    private lateinit var settingsModel: SettingsViewModel

    private val videoDetectionModel: VideoDetectionAlgVModel by viewModels()

    private val compositeDisposable = CompositeDisposable()

    private val serviceWorkerClient = object : ServiceWorkerClient() {
        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()
            if (url.contains(".m3u8") || url.contains(".mpd") || url.contains(".txt")) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val okRequest = CookieUtils.webRequestToHttpWithCookies(request)
                    if (okRequest != null) {
                        videoDetectionModel.verifyLinkStatus(okRequest)
                    }
                }
            }
            return super.shouldInterceptRequest(request)
        }
    }

    inner class TabsFragmentStateAdapter(private var webTabsRoutes: List<WebTab>) : FragmentStateAdapter(this) {
        fun setRoutes(newRoutes: List<WebTab>) {
            Log.d("TabsFragmentState", "Updating routes. Initial size: ${webTabsRoutes.size}, New size: ${newRoutes.size}")
            webTabsRoutes = newRoutes
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = webTabsRoutes.size

        override fun getItemId(position: Int): Long {
            return webTabsRoutes[position].id.hashCode().toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            val webTab = webTabsRoutes.find { it.id.hashCode().toLong() == itemId }
            return webTab != null
        }

        override fun createFragment(position: Int): Fragment {
                return createHomeTabFragment()
        }
    }

    private fun createHomeTabFragment(): Fragment {
        return BrowserHomeFragment.newInstance()
    }
    override fun getOpenTabEvent(): SingleLiveEvent<WebTab> {
        return browserViewModel.openPageEvent
    }

    override fun getCloseTabEvent(): SingleLiveEvent<WebTab> {
        return browserViewModel.closePageEvent
    }

    override fun getUpdateTabEvent(): SingleLiveEvent<WebTab> {
        return browserViewModel.updateWebTabEvent
    }

    override fun getTabsListChangeEvent(): ObservableField<List<WebTab>> {
        return browserViewModel.tabs
    }

    override fun getPageTab(position: Int): WebTab {
        val list = browserViewModel.tabs.get() ?: listOf(WebTab.HOME_TAB)
        if (position in list.indices) {
            return list[position]
        }
        return WebTab("error", "error")
    }

    private fun createTabFragment(index: Int): Fragment {
        val fragment = WebTabFragment.newInstance().apply {
            val args = Bundle().apply {
                putInt(TAB_INDEX_KEY, index)
            }
            arguments = args
        }
        return fragment
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val swController = ServiceWorkerController.getInstance()
        swController.setServiceWorkerClient(serviceWorkerClient)
        swController.serviceWorkerWebSettings.allowContentAccess = true
        DownloaderModuleNavigator.settingsViewModel?.let {
            videoDetectionModel.settingsModel =it
            browserViewModel.settingsModel = it
            settingsModel = it
        }
        DownloaderModuleNavigator.mainViewModel?.browserServicesProvider = this
        tabsAdapter = TabsFragmentStateAdapter(emptyList())
        drawerAdapter = WebTabsAdapter(emptyList(), tabsListener)
        val webTabsManagerLayout = WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        dataBinding = FragmentBrowserBinding.inflate(inflater, container, false).apply {
            this.viewPager.adapter = tabsAdapter
            this.viewPager.setOnGoThroughListener(onGoThroughListener)
            this.tabsList.layoutManager = webTabsManagerLayout
            this.tabsList.adapter = drawerAdapter
//            this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            this.viewModel = browserViewModel
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onBackPressed()
        }
        videoDetectionModel.downloadButtonState.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
                    browserViewModel.workerM3u8MpdEvent.value = videoDetectionModel.downloadButtonState.get()
                }
            }
        })
        return dataBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        browserViewModel.start()
      /*  handlePressWebTabEvent()
        handleOpenTabEvent()
        handleCloseWebTabEventEvent()
        handleOpenNavDrawerEvent()
        handleUpdateWebTabEventEvent()
        checkIsPowerSaveMode()*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        browserViewModel.stop()
        videoDetectionModel.stop()
        compositeDisposable.clear()
    }

    override fun getHistoryVModel(): HistoryViewModel {
        return this.historyModel
    }

    override fun getWorkerM3u8MpdEvent(): MutableLiveData<DownloadButtonState> {
        return browserViewModel.workerM3u8MpdEvent
    }

    override fun getCurrentTabIndex(): ObservableInt {
        return browserViewModel.currentTab
    }

    private val tabsListener = object : WebTabsListener {
        override fun onCloseTabClicked(webTab: WebTab) {
            browserViewModel.closePageEvent.value = webTab
        }
        override fun onSelectTabClicked(webTab: WebTab) {
            browserViewModel.selectWebTabEvent.value = webTab
        }

        override fun deleteAll() {
        }

        override fun insertNew() {

        }
    }

    private fun onBackPressed() {
        val rootPagerIndex = DownloaderModuleNavigator.mainViewModel?.currentItem?.get() ?: 0
        if (rootPagerIndex > 0) {
            DownloaderModuleNavigator.mainViewModel?.currentItem?.set(HOME_TAB_INDEX)
        }
        if (rootPagerIndex == HOME_TAB_INDEX) {
            requireActivity().finish()
            return
        }
    }

    private val onGoThroughListener = object : OnGoThroughListener {
        override fun onRightGoThrough() {
            val currentTabIndex = browserViewModel.currentTab.get()
            if (currentTabIndex == 0) {
                mainViewModel.currentItem.set((mainViewModel.currentItem.get() ?: 0) + 1)
            }
        }
    }

}
