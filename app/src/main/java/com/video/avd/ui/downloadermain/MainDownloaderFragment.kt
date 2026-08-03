package com.video.avd.ui.downloadermain

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavArgument
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.applovin.mediation.ads.MaxInterstitialAd
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.avd.R
import com.avd.data.repository.AdBlockHostsRepository
import com.avd.ui.component.adapter.MainAdapter
import com.avd.ui.main.home.MainViewModel
import com.avd.ui.main.proxies.ProxiesViewModel
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.adChoice
import com.avd.util.AdBlockerHelper.fromBrowser
import com.avd.util.AdBlockerHelper.is24hour
import com.avd.util.AdBlockerHelper.isPro
import com.avd.util.AdBlockerHelper.localeLangauge
import com.avd.util.AppUtil
import com.avd.util.DownloaderModuleNavigator
import com.avd.util.FileUtil
import com.avd.util.Memory.changeStatusBarColor
import com.avd.util.SharedPrefHelper
import com.avd.util.fragment.FragmentFactory
import com.video.avd.ads.AdsManager.maxAdImpressions
import com.video.avd.databinding.FragmentMainDownloaderBinding
import com.video.avd.utils.AppUtils
import com.video.avd.utils.GlobalValues
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainDownloaderFragment : Fragment() {

    @Inject
    lateinit var fragmentFactory: FragmentFactory

    @Inject
    lateinit var adBlockHostsRepository: AdBlockHostsRepository

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    @Inject
    lateinit var fileUtil: FileUtil

    @Inject
    lateinit var appUtil: AppUtil

    var mainViewModel: MainViewModel? = null

    val proxiesViewModel: ProxiesViewModel by viewModels()

    val settingsViewModel: SettingsViewModel by viewModels()

    private lateinit var dataBinding: FragmentMainDownloaderBinding

    private lateinit var mainAdapter: MainAdapter

    var downloadInterstitial: MaxInterstitialAd? = null

    private val currentItemCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            syncViewPagerWithViewModel()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataBinding = DataBindingUtil.inflate(inflater, com.video.avd.R.layout.fragment_main_downloader, container, false)
        initAppBase(requireContext())
        // Initialize Firebase and other services
        mainViewModel= AppUtils.getMain(requireActivity()).mainViewModel
        try {
            // Firebase initializes automatically via FirebaseInitProvider
            // Only initialize if not already initialized to prevent ANR from class loading
            if (FirebaseApp.getApps(requireContext()).isEmpty()) {
                FirebaseApp.initializeApp(requireContext())
            }
            FirebaseCrashlytics.getInstance().log("Testing Crashlytics")
        } catch (e: Exception) {
           e.printStackTrace()
        }
        changeStatusBarColor(R.color.black, requireActivity(), true)
        // Retrieve arguments or set defaults
        val args = arguments
        isPro = AdBlockerHelper.isProVersion.value?:false
        is24hour = GlobalValues.is24hourEnabled.value
        localeLangauge = args?.getString("language") ?: "en"
        adChoice = "admob"
        maxAdImpressions = args?.getInt("adcounterLocal", 0) ?: 0
        // Set up the view pager and adapter
//        fromBrowser= args?.getBoolean("fromBrowser",false) == true
        Log.d("checkBoolean","fromBrowser: ${args?.getBoolean("fromBrowser")}")
        mainAdapter = MainAdapter(childFragmentManager, lifecycle, fragmentFactory,fromBrowser)
        dataBinding.viewPager.isUserInputEnabled = false
        dataBinding.viewPager.adapter = mainAdapter
        dataBinding.viewPager.registerOnPageChangeCallback(onPageChangeListener)
        dataBinding.viewModel = mainViewModel
        mainViewModel?.currentItem?.addOnPropertyChangedCallback(currentItemCallback)
        syncViewPagerWithViewModel()
        // Handle intents (same logic as before)
        // handleIntent(arguments)
        // Start other view models
        proxiesViewModel.start()
        settingsViewModel.start()
        mainViewModel?.start()
        mainViewModel?.let {
            DownloaderModuleNavigator.setMMainViewModel(it)
        }
        DownloaderModuleNavigator.setSettingViewModel(settingsViewModel)
        DownloaderModuleNavigator.setProxyViewModel(proxiesViewModel)
        return dataBinding.root
    }

    private val onPageChangeListener = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            Log.d("checkBoolean","onPageSelected fromBrowser: $fromBrowser position=$position")

            if (position == 0) {
                val delayMillis = if (fromBrowser) 300L else 500L
                Handler(Looper.getMainLooper()).postDelayed({
                    mainViewModel?.isBrowserCurrent?.set(true)
                }, delayMillis)
            } else {
                mainViewModel?.isBrowserCurrent?.set(false)
            }

            val childrenCount = dataBinding.fragmentContainerView.childCount
            if (childrenCount > 0) {
                childFragmentManager.popBackStack()
            }

            mainViewModel?.currentItem?.set(position)

        }
    }

    override fun onDestroyView() {
        mainViewModel?.currentItem?.removeOnPropertyChangedCallback(currentItemCallback)
        super.onDestroyView()
    }

    private fun syncViewPagerWithViewModel() {
        val target = mainViewModel?.currentItem?.get() ?: return
        if (!::dataBinding.isInitialized) return
        if (dataBinding.viewPager.currentItem != target) {
            Log.d("checkBoolean", "syncViewPagerWithViewModel target=$target fromBrowser=$fromBrowser")
            dataBinding.viewPager.setCurrentItem(target, false)
        }
    }

    private fun initAppBase(context: Context) {
        initializeFileUtils(context.applicationContext)
        val file: File = fileUtil.folderDir
        CoroutineScope(Dispatchers.Default).launch {
            if (!file.exists()) {
                file.mkdirs()
            }
        }
    }

    private fun initializeFileUtils(applicationContext: Context?) {
        if (applicationContext != null) {
            val isExternal = SharedPrefHelper(applicationContext).getIsExternalUse()
            val isAppDir = SharedPrefHelper(applicationContext).getIsAppDirUse()
            FileUtil.IS_EXTERNAL_STORAGE_USE = isExternal
            FileUtil.IS_APP_DATA_DIR_USE = isAppDir
            FileUtil.INITIIALIZED = true
        }
    }

}
