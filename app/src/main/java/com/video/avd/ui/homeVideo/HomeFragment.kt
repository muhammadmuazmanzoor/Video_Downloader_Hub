package com.video.avd.ui.homeVideo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.singular.sdk.Singular
import com.video.avd.R

import com.video.avd.constent.SORT_TYPE
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.isClickedForCasting
import com.video.avd.constent.isvideo
import com.video.avd.constent.showfloatandhide
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentHomeBinding
import com.video.avd.extension.nextNavigateTo
import com.video.avd.ui.MainActivityViewModel
import com.video.avd.ui.allvideo.AllVideoFragment
import com.video.avd.ui.dialoges.videossorting.listners.OnSortChangedListner
import com.video.avd.ui.folder.FolderFragment
import com.video.avd.ui.fragments.HistoryFragment
import com.video.avd.ui.player.ChromeCastDelegate
import com.video.avd.ui.player.ChromeCastDelegate.Companion.mChromecastConnection
import com.video.avd.ui.player.ChromeCastDelegateImp
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.ExitDialogListener
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.ToastUtils
import com.video.avd.utils.chromecast.ChromecastConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showExitScreen
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.BuildConfig

@AndroidEntryPoint
class HomeFragment : Fragment(), ExitDialogListener, OnSortChangedListner{

    @Transient
    private val chromeCastDelegate: ChromeCastDelegate = ChromeCastDelegateImp()

    // Delegate all ChromeCastDelegate methods manually or use interface delegation differently
   /* override fun setupChromecastConnection(entities: List<Video>, position: Int) {
        chromeCastDelegate.setupChromecastConnection(entities, position)
    }*/

    // Override other ChromeCastDelegate methods as needed
    var mSelectedMedia: ArrayList<Video>?
        get() = chromeCastDelegate.mSelectedMedia
        set(value) { chromeCastDelegate.mSelectedMedia = value }

     fun updateSelectedPosition(position: Int) {
        chromeCastDelegate.updateSelectedPosition(position)
    }

     fun loadRemoteMediaFromPlaylist(activity: Activity) {
        chromeCastDelegate.loadRemoteMediaFromPlaylist(activity)
    }


    private val viewModel: MainActivityViewModel by activityViewModels()
    var _binding: FragmentHomeBinding? = null
    val binding get() = _binding // Helper Property
    private var mActivity: FragmentActivity? = null

    var guidanceShown: Boolean? = false
    var historylist = arrayListOf<Video>()
    var isHistory: Boolean = true
    var isFolder: Boolean = true
    private val REQUEST_TAKE_VIDEO = 777


    companion object {
        var isHomeConnectin = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e("AdsManager", "mActivity is not null")
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        Log.e("AdsManager", "mActivity is null")
        mActivity = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isvideo = true
        mActivity?.let { AppPreference.isPermissionGrantedForStatus(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mActivity?.let { AppUtils.setLocate(it) }
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        lifecycleScope.launch {
            guidanceShown = mActivity?.let { AppPreference.getWhatsappGuidanceState(it) }
        }
        binding?.viewPager?.isUserInputEnabled = false
//        if (AdBlockerHelper.isProVersion.value == true) {
//            binding?.imgPronew?.visibility = View.GONE
//        } else {
//            binding?.imgPronew?.visibility = View.VISIBLE
//        }
        AppUtils.fbEvents("home_view", "HomeScreen",mActivity)
        Singular.event("HomeVideo_ViewCreated")
        AppUtils.getMain(mActivity).showbottombar()
        AppUtils.getMain(mActivity).showBannerAd()
        return binding?.root
    }



    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated_HomeFragment", "HomeFragment")
        isHistory = mActivity?.let { AppPreference.isHistoryOn(it) } == true
        mActivity?.let {
            AppUtils.firebaseUserAction("onCreateView_HomeFragment", "HomeFragment")
            observers()
            val adapter = MyPagerAdapter(it, childFragmentManager, lifecycle)
            adapter.addFragment(FolderFragment())
            adapter.addFragment(AllVideoFragment())
            adapter.addFragment(HistoryFragment())
            binding?.viewPager?.adapter = adapter
            binding?.viewPager?.isUserInputEnabled = true
            binding?.tabLayout?.selectTab(binding?.tabLayout?.getTabAt(0))
            binding?.tabLayout?.tabRippleColor = null
            binding?.tabLayout?.let {
                binding?.viewPager?.let { it1 ->
                    TabLayoutMediator(it, it1) { tab, position ->
                        when (position) {
                            0 -> {
                                tab.text = getString(R.string.folders_home)
                                //    AppUtils.firebaseUserAction("home_videos_click", "HomeFragment")
                            }

                            1 -> {
                                tab.text = getString(R.string.videos)
                                // AppUtils.firebaseUserAction("home_fold_view", "HomeFragment")

                            }

                            2 -> {
                                tab.text = getString(R.string.history)
                                //  AppUtils.firebaseUserAction("home_online_click", "HomeFragment")
                            }
                        }
                    }.attach()
                }
            }

            binding?.tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            AppUtils.firebaseUserAction("home_FoldersTabSelected", "HomeFragment")
                            // Show banner ad for Folders tab
                            AppUtils.getMain(mActivity).showBannerAd()
                        }
                        1 -> {
                            AppUtils.firebaseUserAction("home_VideoTabSelected", "HomeFragment")
                            // Show banner ad for Videos tab
                            AppUtils.getMain(mActivity).showBannerAd()
                        }
                        else -> {
                            AppUtils.firebaseUserAction(
                                "home_OnlineTabSelected",
                                "HomeFragment"
                            )
                            // Hide banner ad for History tab (position 2)
                            AppUtils.getMain(mActivity).hideBannerAd()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    // Handle tab unselect
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    // Handle tab reselect

                }
            })

            binding?.imgSearch?.setOnClickListener {
                AppUtils.firebaseUserAction("search_clicked_HomeScreen", "HomeFragment")
                mActivity?.nextNavigateTo(
                    HomeFragmentDirections.actionHomeFragmentToSearchVideoFragment(false)
                )
                AppUtils.firebaseUserAction("searchBtnClicked_HomeFragment", "HomeFragment")
            }

            binding?.imgPronew?.setOnClickListener {
                AppUtils.firebaseUserAction("pro_button_clicked_HomeScreen", "HomeFragment")
                mActivity?.let {
                    if (AdBlockerHelper.isProVersion.value != true) {
                        if (!NetworkUtils.isOnline(it)) {
                            ToastUtils.showToast(requireContext(), "Internet connection error")
                        } else {
                            viewModel.prepareNavigation()
                            AppUtils.getMain(mActivity).hidebottombar()
                            AppUtils.getMain(mActivity).hideBannerAd()
                            try {
                                AppUtils.getMain(mActivity).navController?.navigate(R.id.propanel)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        ToastUtils.showToast(requireContext(), "Already Purchased")
                    }
                }
            }

            binding?.gridview?.setOnClickListener { view ->
                if (VIEW_TYPE.value == 0) {
                    VIEW_TYPE.value = 1
                    binding?.gridview?.setImageDrawable(
                        ContextCompat.getDrawable(
                            it,
                            R.drawable.ic_grid_view
                        )
                    )
                } else {
                    VIEW_TYPE.value = 0
                    binding?.gridview?.setImageDrawable(
                        ContextCompat.getDrawable(
                            it,
                            R.drawable.ic_list_view
                        )
                    )
                }
                AppPreference.saveViewType(requireContext(), VIEW_TYPE.value ?: 0)
                AppUtils.firebaseUserAction("gridviewClickedHomeScreen", "HomeFragment")
            }

            binding?.viewPager?.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 2 || position == 3 || position == 4) {
                        binding?.imgSearch?.visibility = View.GONE
                        binding?.sort?.visibility = View.GONE
                        // Hide banner ad when History tab is selected (position 2)
                        if (position == 2) {
                            AppUtils.getMain(mActivity).hideBannerAd()
                        }
                    } else {
                        binding?.imgSearch?.visibility = View.VISIBLE
                        binding?.sort?.visibility = View.VISIBLE
                        // Show banner ad for other tabs
                        AppUtils.getMain(mActivity).showBannerAd()
                    }
                }
            })

        }





        binding?.sort?.setOnClickListener {
            AppUtils.firebaseUserAction("home_sort_click", "HomeFragment")
            mActivity?.nextNavigateTo(
                HomeFragmentDirections.actionHomeFragment1ToVideosSortingDialog(
                    this,
                    SORT_TYPE.value ?: 2
                )
            )
        }

        binding?.nextPlay?.setOnClickListener {
            AppUtils.firebaseUserAction("home_videply_view ", "homescreen")
            if (PlayerVideoActivity.getInstance() != null && PlayerVideoActivity.isPipMode) {
                PlayerVideoActivity.getInstance()?.finishAndRemoveTask()
            }
           /* if (splashAdShown){
                videolistglobal = emptyList()
                videolistglobal = ArrayList(historylist)
                Log.d("listSize", "${videolistglobal.size}+" + "btnclick_home")
                if (mChromecastConnection?.isChromeCastConnect == true) {
                    setupChromecastConnection(historylist, 0)
                } else {
                    val result = Bundle()
                    result.putString("id", "0")
                    result.putBoolean("isliveuri", false)
                    result.putString("uri", "")
                    result.putBoolean("isPlaybackCount", true)
                    result.putString("fragmentName", "History")
                    mActivity?.let { activity ->
                        val intent = Intent(activity, PlayerVideoActivity::class.java)
                        intent.putExtras(result)
                        try {
                            activity.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }else{*/
            mActivity?.let { activity ->
                showInterstitialHome(activity = activity) {
                        videolistglobal = emptyList()
                        videolistglobal = ArrayList(historylist)
                        Log.d("listSize", "${videolistglobal.size}+" + "btnclick_home")
                        if (mChromecastConnection?.isChromeCastConnect == true) {
                            setupChromecastConnection(historylist, 0)
                        } else {
                            val result = Bundle()
                            result.putString("id", "0")
                            result.putBoolean("isliveuri", false)
                            result.putString("uri", "")
                            result.putBoolean("isPlaybackCount", true)
                            result.putString("fragmentName", "History")
                            mActivity?.let { activity ->
                                val intent = Intent(activity, PlayerVideoActivity::class.java)
                                intent.putExtras(result)
                                try {
                                    activity.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
           // }
        }

    }

    fun navigateToNext(activity: FragmentActivity, navDirections: NavDirections) {
        activity.nextNavigateTo(navDirections)
    }


    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Handle the back button event
            Log.d("exitTag", "onCreate: 1")
            try {
                showExitScreen?.invoke()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        try {
            activity?.onBackPressedDispatcher?.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback
            )
        } catch (e: Exception) {
           e.printStackTrace()
        }
        showfloatandhide.observe(viewLifecycleOwner) {
            if (historylist.isNotEmpty()) {
                if (it) {
                    binding?.nextPlay?.show()
                } else {
                    binding?.nextPlay?.hide()
                }
            }
        }
        if (binding?.viewPager?.currentItem == 2) {
            binding?.imgSearch?.visibility = View.GONE
            // Hide banner ad when History tab is active
            AppUtils.getMain(mActivity)?.hideBannerAd()
        } else {
            // Show banner ad for other tabs
            AppUtils.getMain(mActivity)?.showBannerAd()
        }
    }

    private fun observers() {
      /*  viewModel.callTheRateUsPlease.observe(viewLifecycleOwner) {
            if (it) {
                mActivity?.nextNavigateTo(HomeFragmentDirections.actionHomeFragmentToRateUs())
                viewModel.callTheRateUsPlease.postValue(false)
            }
        }*/
        VIEW_TYPE.observe(viewLifecycleOwner) {
            mActivity?.let { activity ->
                if (it == 0) {
                    binding?.gridview?.setImageDrawable(
                        ContextCompat.getDrawable(
                            activity,
                            R.drawable.ic_list_view
                        )
                    )
                } else {
                    binding?.gridview?.setImageDrawable(
                        ContextCompat.getDrawable(
                            activity,
                            R.drawable.ic_grid_view
                        )
                    )
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showExitDialog() {
        mActivity?.let { activity ->
            showInterstitialHome(activity = activity, forFragment = true) {
                    mActivity?.nextNavigateTo(HomeFragmentDirections.actionHomeFragmentToExitFragment())
                }
        }
    }

    override fun onSortChanged(isChanged: Boolean, sortType: Int) {
        SORT_TYPE.value = sortType
        mActivity?.let {
            AppPreference.saveSortType(it, sortType)
        }
    }


    private fun setupChromecastConnection(entities: List<Video>, position: Int) {
        try {
            val item = entities[position]
            val isMp4 = item.contentUri?.let { uri ->
                mActivity?.let {
                    AppUtils.isSupportedVideoFile(
                        it,
                        uri.toUri()
                    )
                }
            }
            if (isMp4 == true) {
                isClickedForCasting.value = true
                mSelectedMedia = entities as java.util.ArrayList<Video>
                ChromecastConnection.position = position
                updateSelectedPosition(position)
                loadRemoteMediaFromPlaylist(mActivity as Activity)
            } else {
                Toast.makeText(
                    mActivity,
                    "sorry this file format is not supported by chromse cast",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
