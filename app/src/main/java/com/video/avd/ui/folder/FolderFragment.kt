package com.video.avd.ui.folder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.inter_videos
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.ads.AdsManager.recyclerNative
import com.video.avd.constent.SORT_TYPE
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.isSplash
import com.video.avd.constent.isvideo
import com.video.avd.constent.showfloatandhide
import com.video.avd.databinding.FragmentFolderBinding
import com.video.avd.extension.nextNavigateWithId
import com.video.avd.ui.basefragment.BaseVideoFragment
import com.video.avd.ui.folder.adapter.FolderAdapter
import com.video.avd.ui.folder.adapter.OnClickListner
import com.video.avd.ui.folder.model.VideoFolder
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.WeakReferenceVideo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AppSettingsDialog
import javax.inject.Inject

@AndroidEntryPoint
class FolderFragment : BaseVideoFragment(), OnClickListner {
    var mActivity: FragmentActivity? = null
    var foldersListReference = listOf<VideoFolder>()
    var permissiongranted = false
    var flow: Flow<List<Video>>? = null
    private val mutex = Mutex()
    var isScrollingDown = false
    @Inject
    lateinit var myFolders: WeakReferenceVideo
    private val videolistfornewtag = ArrayList<Video>()
    private var isDataFetched = false

    companion object {
        val PERMISSIONS_Folder = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        val PERMISSIONS2_Folder = arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        val PERMISSIONS_CHECK = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        val PERMISSIONS2_CHECK = arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    }

    var binding: FragmentFolderBinding? = null
    private val mViewmodel: FolderViewModel by viewModels()

    private val adapterfolder: FolderAdapter by lazy {
        FolderAdapter(emptyList(), requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFolderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
           // AppUtils.firebaseUserAction("onViewCreated_FolderFragment", "FolderFragment")
            isvideo = true
            AppUtils.getMain(activity).showbottombar()
            binding?.allow?.setOnClickListener {
                AppSettingsDialog.Builder(this).build().show()
            }
            sortAndViewTypeObservers()
            binding?.swipeRefreshLayout?.setOnRefreshListener {
                if (permissiongranted) {
                    mActivity?.let { loadFoldersIntoAdapter(it) }
                } else {
                    binding?.swipeRefreshLayout?.isRefreshing = false
                }
            }
        }
        GlobalValues.is24hourEnabled.observe(viewLifecycleOwner) {
            if (it) {
                adapterfolder.isNativeAdLoaded = false
                adapterfolder.isMetaAdLoaded = false
                binding?.folderRv?.adapter?.notifyDataSetChanged()
            }
        }
        binding?.folderRv?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && !isScrollingDown) {
                    showfloatandhide.value = false
                    isScrollingDown = true
                } else if (dy < 0 && isScrollingDown) {
                    showfloatandhide.value = true
                    isScrollingDown = false
                }
            }
        })
    }


    override fun onResume() {
        super.onResume()
        if (foldersListReference.isEmpty()) {
            isDataFetched=false
            getPermission()
        } else {
            if (binding?.folderRv?.adapter == null) {
                activity?.let { setAdapter(it, foldersListReference) }
            }
        }
    }


    fun getDataFromDevice() {
        mActivity?.let { activity ->
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mViewmodel.videosListFlow
                        .debounce(300)
                        .collectLatest { videosList ->
                            if (videosList.isNotEmpty()) {
                                try {
                                    // Process data in IO thread
                                    withContext(Dispatchers.IO) {
                                        // Synchronize access to videolistfornewtag
                                        mutex.withLock {
                                            if (videolistfornewtag != videosList) {
                                                videolistfornewtag.clear()
                                                videolistfornewtag.addAll(videosList)
                                            }
                                        }
                                    }

                                    // Perform ViewModel and lifecycle interactions on Main thread
                                    withContext(Dispatchers.Main) {
                                        mViewmodel.addVideoToDb(videosList, activity)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                }
            }
        }
    }

    private fun sortAndViewTypeObservers() {
        VIEW_TYPE.observe(viewLifecycleOwner) {
            binding?.folderRv?.post {
                try {
                    val currentState = binding?.folderRv?.layoutManager?.onSaveInstanceState()

                    if (it == 1) {
                        val itemgrid = GridLayoutManager(mActivity, 3)
                        itemgrid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                val viewType = adapterfolder.getItemViewType(position)
                                return if (viewType == adapterfolder.VIEW_TYPE_AD || viewType == adapterfolder.VIEW_TYPE_AD_Meta) 3 else 1
                            }
                        }
                        binding?.folderRv?.layoutManager = itemgrid
                    } else {
                        binding?.folderRv?.layoutManager =
                            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                    }

                    binding?.folderRv?.recycledViewPool?.clear()
                    binding?.folderRv?.layoutManager?.onRestoreInstanceState(currentState)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        try {
            SORT_TYPE.observe(viewLifecycleOwner) {
                it?.let {
                    lifecycleScope.launch {
                        val list = mViewmodel.sortVideoFoldersList(it, foldersListReference)
                        if (list.isNotEmpty()) {
                            adapterfolder?.setData(ArrayList(list))
                        }

                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun loadFoldersIntoAdapter(activity: FragmentActivity) {
        getDataFromDevice()
        getFolderFromDb()
    }

    private fun setAdapter(activity: FragmentActivity, videoarraylist: List<VideoFolder>) {
        Log.d("FolderSize Changed", videoarraylist.size.toString())
        binding?.folderRv?.setHasFixedSize(true)
        if (VIEW_TYPE.value == 1) {
            val itemgrid = GridLayoutManager(requireContext(), 3)
            itemgrid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val viewType: Int = adapterfolder.getItemViewType(position)
                    return if (viewType == adapterfolder.VIEW_TYPE_AD || viewType == adapterfolder?.VIEW_TYPE_AD_Meta) {
                        // Make ad items span across all columns, or adjust as needed
                        3
                    } else {
                        // Content items span 1
                        1
                    }
                }
            }
            binding?.folderRv?.layoutManager = itemgrid
        } else {
            binding?.folderRv?.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        }
        binding?.folderRv?.adapter = adapterfolder
        adapterfolder.setOnClickListner(this@FolderFragment)
        adapterfolder.setData(videoarraylist)
        myFolders.setObjectfolder(videoarraylist.toList())
        if (NetworkUtils.isOnline(activity) && AdBlockerHelper.isProVersion.value != true) {
            if (GlobalValues.is24hourEnabled.value == false) {
                if (recyclerNative){
//                    adapterfolder.loadNativeAd(activity)
//                    adapterfolder.loadMetaNativeAd(activity)
                }
            }
        }
    }

    fun getFolderFromDb() {
        lifecycleScope.launch {
            // Collect the flow in the background
            withContext(Dispatchers.IO) {
                mViewmodel.getFoldersFromDb().collectLatest { dbfolder ->
                    // Update folder list reference
                    foldersListReference = dbfolder
                    if (dbfolder.isEmpty()) {
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            binding?.noVideos?.visibility = View.VISIBLE
                          //  binding?.progressBar?.visibility = View.GONE
                            binding?.swipeRefreshLayout?.isRefreshing = false
                        }
                    } else {
                        val sortedFoldersList = mViewmodel.sortVideoFoldersList(SORT_TYPE.value ?: 0, dbfolder.toMutableList())
                        // Update UI on main thread
                        withContext(Dispatchers.Main) {
                            binding?.noVideos?.visibility = View.GONE
                           // binding?.progressBar?.visibility = View.GONE
                            foldersListReference = sortedFoldersList
                            activity?.let { setAdapter(it, sortedFoldersList) }
                            binding?.swipeRefreshLayout?.isRefreshing = false
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("SuspiciousIndentation")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            isvideo = true
            BaseVideoFragment.isRationaleDialogShown = false
            getPermission()
        }
    }

    override fun onPermissionsGranted() {
        AppUtils.firebaseUserAction("vid_permission_granted", "FolderFragment")
        if (!isDataFetched){
            isDataFetched=true
           // binding?.progressBar?.visibility = View.GONE
            mActivity?.let { loadFoldersIntoAdapter(it) }
            binding?.permission?.visibility = View.GONE
            permissiongranted = true
            Handler(Looper.getMainLooper()).postDelayed({
                isSplash = false
            }, 1000)
        }
        binding?.permission?.visibility = View.GONE
    }

    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        AppUtils.firebaseUserAction("vid_permission_not_granted", "FolderFragment")
        binding?.noVideos?.visibility = View.GONE
        binding?.permission?.visibility = View.VISIBLE
    }

    override fun onClickListner(id: String, name: String) {
        if(interHome!=null) {
            interHome?.let {
                showInterstitial(true, it, mActivity?:requireActivity(), {
                    if (id.toLong() == 786000000L) {
                        mActivity?.let {
                            AppUtils.getMain(it).hidebottombar()
                            it.nextNavigateWithId(
                                R.id.action_global_file_manager_fragment_directories,
                                Bundle()
                            )
                        }
                    } else {
                        // When the folder is opened, remove the "new" tag
                        mActivity?.let {
                            Log.e("checkFolder", "id : $id name:$name")
                            val bundle = Bundle()
                            bundle.putString("id", id)
                            bundle.putString("name", name)
                            try {
                                findNavController().navigate(R.id.action_global_videosFragment, bundle)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },inter_videos)
            }

        }
        else{
            loadFallbackInterstitialAd(mActivity?:requireActivity(), BuildConfig.inter_home_high, BuildConfig.inter_home,inter_home_high,inter_home_normal,{
                interHome=it
            },{
                interHome=it
            })
            if (id.toLong() == 786000000L) {
                mActivity?.let {
                    AppUtils.getMain(it).hidebottombar()
                    it.nextNavigateWithId(
                        R.id.action_global_file_manager_fragment_directories,
                        Bundle()
                    )
                }
            } else {
                // When the folder is opened, remove the "new" tag
                mActivity?.let {
                    Log.e("checkFolder", "id : $id name:$name")
                    val bundle = Bundle()
                    bundle.putString("id", id)
                    bundle.putString("name", name)
                    try {
                        findNavController().navigate(R.id.action_global_videosFragment, bundle)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        AppUtils.firebaseUserAction("onClickListener_FolderFragment", "FolderFragment")

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        Log.d("DebugNew", "Fragment onStart")
    }

    override fun onStop() {
        super.onStop()
        Log.d("DebugNew", "Fragment onStop")
    }

}