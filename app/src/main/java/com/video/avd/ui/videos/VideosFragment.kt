package com.video.avd.ui.videos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.inter_videos
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.R
import com.video.avd.constent.GRID_ITEM_SPAN_COUNT
import com.video.avd.constent.SORT_TYPE
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.isClickedForCasting
import com.video.avd.constent.isSplash
import com.video.avd.constent.videolistglobal
import com.video.avd.data.local.Entities
import com.video.avd.databinding.FragmentVideosBinding
import com.video.avd.extension.nextNavigateTo
import com.video.avd.ui.MainActivityViewModel
import com.video.avd.ui.allvideo.VideoInfoBottomSheetFragment
import com.video.avd.ui.allvideo.VideoOptionBottomSheetFragment
import com.video.avd.ui.dialoges.videossorting.listners.OnSortChangedListner
import com.video.avd.ui.player.ChromeCastDelegate
import com.video.avd.ui.player.ChromeCastDelegateImp
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.videos.adapter.VideoAdapter
import com.video.avd.ui.videos.adapter.VideoListner
import com.video.avd.ui.videos.model.Video
import com.video.avd.ads.AppOpenManager
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.convertToMilliseconds
import com.video.avd.utils.AppUtils.hideKeyboard
import com.video.avd.utils.AppUtils.shareVideo
import com.video.avd.utils.CustomAlertDialog
import com.video.avd.utils.EventObserver
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.GridSpacingItemDecoration
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.SharedPreferencesManager
import com.video.avd.utils.WeakReferenceVideo
import com.video.avd.utils.chromecast.ChromecastConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val DELETE_PERMISSION_REQUEST = 0x1033
private const val RENAME_PERMISSION_REQUEST = 0x1876

@AndroidEntryPoint
class VideosFragment : Fragment(), VideoListner, ChromeCastDelegate by ChromeCastDelegateImp(), VideoAdapter.MenuClickListener, OnSortChangedListner {

    var binding: FragmentVideosBinding? = null
    private val mViewmodel: VideosViewModel by viewModels()
    private val viewmodel: MainActivityViewModel by viewModels()
    private val args: VideosFragmentArgs by navArgs()
    var mActivity: FragmentActivity? = null
    val adapterfolder: VideoAdapter  by lazy { VideoAdapter(requireContext(), emptyList(), listener = this@VideosFragment) }
    private var tempTitle = ""
    private var optionsItem: Video? = null
    private var videosListReference = listOf<Video>()
    private var selectedVideo: Video? = null
    private var isuserearned = false
    var sharedPreferencesManager: SharedPreferencesManager? = null
    val freeLimit = 2
    private var isClickable = true
    var alreadyshown=false

    @Inject
    lateinit var myVideos: WeakReferenceVideo

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isClickable) {
                    isClickable = false
                    lifecycleScope.launch {
                        delay(1000)
                        isClickable = true
                    }
                    mActivity?.let { activity ->
                        if(interHome!=null){
                            interHome?.let {
                                showInterstitial(true,it,requireActivity(),{
                                    try {
                                        activity.findNavController(R.id.nav_host).popBackStack()
                                    } catch (e: Exception) {
                                        Log.e("NavigationError", "NavController not found: ${e.message}")
                                    }
                                },inter_videos)
                            }
                        }
                        else{
                            loadFallbackInterstitialAd(requireActivity(), requireActivity().resources.getString(
                                com.avd.R.string.Interstitial_Home_ID_High), requireActivity().resources.getString(
                                com.avd.R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                                interHome=it
                            },{
                                interHome=it
                            })
                            try {
                                activity.findNavController(R.id.nav_host).popBackStack()
                            } catch (e: Exception) {
                                Log.e("NavigationError", "NavController not found: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
        mActivity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, callback)
        sharedPreferencesManager = mActivity?.let { SharedPreferencesManager(it) }
        mActivity?.let { activity ->
            setAdapter()
            AppUtils.getMain(activity).showBannerAd()
            AppUtils.firebaseUserAction("onViewCreated_FVideoFragment", "FolderVideoFragment")
            binding?.sortVideos?.visibility = View.VISIBLE
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    val videosListFlow = mViewmodel.getvideosfromdb(args.id)
                    if (!alreadyshown){
                        alreadyshown=true
                        binding?.videoProgress?.visibility=View.VISIBLE
                    }
                    videosListFlow.collectLatest { videos ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (videos.isEmpty()) {
                                binding?.videoProgress?.visibility=View.GONE
                                binding?.noVideos?.visibility = View.VISIBLE
                            } else {
                                binding?.videoProgress?.visibility=View.GONE
                                binding?.noVideos?.visibility = View.GONE
                                val sortedList = mViewmodel.sortVideosList(SORT_TYPE.value ?: 0, videos)
                                videolistglobal = sortedList
                                videosListReference = sortedList
                                myVideos.setObjectvideo(videosListReference)
                                if (sortedList.isNotEmpty()) {
                                    if (NetworkUtils.isOnline(activity) && AdBlockerHelper.isProVersion.value != true) {
                                        if (GlobalValues.is24hourEnabled.value == false) {
//                                            adapterfolder.loadNativeAd(activity)
//                                            adapterfolder.loadMetaNativeAd(activity)
                                        }
                                    }
                                }
                                adapterfolder.updateList(sortedList)
                                binding?.videoProgress?.visibility=View.GONE
                            }
                        }
                    }
                }
            }
            initActionbar()
            observers(activity)
            AppUtils.getMain(activity).hidebottombar()
            binding?.sortVideos?.setOnClickListener {
                mActivity?.nextNavigateTo(
                    VideosFragmentDirections.actionVideosFragmentToVideosSortingDialog(
                        this,
                        SORT_TYPE.value ?: 0
                    )
                )
            }
            binding?.gridImg?.setOnClickListener {
                mActivity?.let {
                    if (VIEW_TYPE.value == 0) {
                        VIEW_TYPE.value = 1
                        binding?.gridImg?.setImageDrawable(
                            ContextCompat.getDrawable(
                                it,
                                R.drawable.ic_grid_view
                            )
                        )
                    } else {
                        VIEW_TYPE.value = 0
                        binding?.gridImg?.setImageDrawable(
                            ContextCompat.getDrawable(
                                it,
                                R.drawable.ic_list_view
                            )
                        )
                    }
                    AppPreference.saveViewType(it, VIEW_TYPE.value ?: 0)
                }
            }

            binding?.imgSearch?.setOnClickListener {
                activity.nextNavigateTo(
                    VideosFragmentDirections.actionVideosFragmentToSearchVideoFragment(
                        isFolder = false
                    )
                )
            }

        }
        mViewmodel.insertedToFavMsg.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(mActivity, msg.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        try {
            VIEW_TYPE.observe(viewLifecycleOwner) {
                val currentState = binding?.videoRv?.layoutManager?.onSaveInstanceState()
                if (it == 1) {
                    val itemgrid = GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
                    itemgrid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            val viewType: Int? = adapterfolder?.getItemViewType(position)
                            return if (viewType ==VideoAdapter.VIEW_TYPE_ADMOB || viewType == VideoAdapter.VIEW_TYPE_META) {
                                // Make ad items span across all columns, or adjust as needed
                                GRID_ITEM_SPAN_COUNT
                            } else {
                                // Content items span 1
                                1
                            }
                        }
                    }
                    binding?.videoRv?.layoutManager = itemgrid
                } else {
                    binding?.videoRv?.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                }
                binding?.videoRv?.recycledViewPool?.clear()
                binding?.videoRv?.layoutManager?.onRestoreInstanceState(currentState)
                for (i in 0 until binding?.videoRv?.itemDecorationCount!!) {
                    binding?.videoRv?.removeItemDecorationAt(i)
                }
                binding?.videoRv?.addItemDecoration(GridSpacingItemDecoration(20))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        castWaitObserver()
        GlobalValues.is24hourEnabled.observe(viewLifecycleOwner) {
            if (it) {
                adapterfolder.isNativeAdLoaded = false
                adapterfolder.isLargeAdLoaded = false
                binding?.videoRv?.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun castWaitObserver() {
        isClickedForCasting.observe(viewLifecycleOwner) {
            binding?.videoProgress?.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun setAdapter() {
        binding?.videoRv?.setHasFixedSize(true)
        binding?.videoRv?.adapter = adapterfolder
        adapterfolder.setOnClickListner(this@VideosFragment)
        binding?.videoRv?.addItemDecoration(GridSpacingItemDecoration(20))
    }

    private fun initActionbar() {
        binding?.back?.visibility = View.VISIBLE
        binding?.startname?.text = args.name
        binding?.back?.setOnClickListener {
            mActivity?.let { activity ->
                showInterstitialHome(activity = activity, forFragment = true) {
                    try {
                        findNavController().popBackStack()
                    } catch (e: Exception) {
                        Log.e("NavigationError", "NavController not found: ${e.message}")
                    }
                }
            }
        }
    }

    private fun observers(activity: FragmentActivity) {
    /*    viewmodel.callTheRateUsPlease.observe(viewLifecycleOwner) {
            if (it) {
                activity.nextNavigateTo(VideosFragmentDirections.actionVideosFragmentToRateUs())
                viewmodel.callTheRateUsPlease.postValue(false)
            }
        }*/
        mViewmodel.isForDelete.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                lifecycleScope.launch {
                    mViewmodel.deletedatafromdb(tempTitle)
                }
            }
        })
        mViewmodel.isForRename.observe(viewLifecycleOwner) {
            it?.let { renamed ->
                if (renamed) {
                    hideKeyboard()
                    lifecycleScope.launch {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            mViewmodel.UpdateDataItemTitle()
                        }else{
                            mViewmodel.videosListFlow.collectLatest { videosList ->
                                if (videosList.isNotEmpty()) {
                                    mViewmodel.addVideoToDb(videosList)
                                    binding?.videoProgress?.visibility=View.GONE
                                }else {
                                    binding?.videoProgress?.visibility=View.GONE
                                }
                            }
                        }
                        val flow = mViewmodel.getvideosfromdb(args.id)
                        flow.collectLatest { videosList ->
                            if (videosList.isNotEmpty()) {
                                binding?.noVideos?.visibility = View.GONE
                                val sortedList = mViewmodel.sortVideosList(SORT_TYPE.value ?: 0, videosList)
                                adapterfolder.updateList(sortedList)
                            } else {
                                binding?.noVideos?.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
        mViewmodel.permissionNeededForDelete.observe(viewLifecycleOwner, Observer { intentSender ->
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
                AppOpenManager.isShowingAd = true
//                isSplash = true
                startIntentSenderForResult(
                    intentSender,
                    DELETE_PERMISSION_REQUEST,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }
        })
        mViewmodel.permissionNeededForRename.observe(viewLifecycleOwner) { intentSender ->
            AppOpenManager.isShowingAd = true
            intentSender?.let {
                startIntentSenderForResult(
                    it,
                    RENAME_PERMISSION_REQUEST,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }
        }
        VIEW_TYPE.observe(viewLifecycleOwner) {
            try {
                val currentState = binding?.videoRv?.layoutManager?.onSaveInstanceState()
                it?.let {
                    if (it == 1) {
                        binding?.videoRv?.layoutManager =
                            GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
                        binding?.gridImg?.setImageDrawable(
                            ContextCompat.getDrawable(
                                activity,
                                R.drawable.ic_grid_view
                            )
                        )
                    } else {
                        binding?.videoRv?.layoutManager =
                            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
                        binding?.gridImg?.setImageDrawable(
                            ContextCompat.getDrawable(
                                activity,
                                R.drawable.ic_list_view
                            )
                        )
                    }
                    binding?.videoRv?.layoutManager?.onRestoreInstanceState(currentState)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        SORT_TYPE.observe(viewLifecycleOwner) {
            it?.let {
                lifecycleScope.launch {
                    val list = mViewmodel.sortVideosList(it, videosListReference)
                    if (list.isNotEmpty()) {
                        videosListReference = list
                        adapterfolder.updateList(list)
                        binding?.videoRv?.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        mViewmodel.remove.observe(viewLifecycleOwner, EventObserver {
            if (it > 0) {
                Toast.makeText(requireContext(), "removed from favourites", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "an error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            mViewmodel.markFolderAsOpened(args.id.toLong())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            mViewmodel.urinew?.let {
                mActivity?.let { it1 ->
                    try {
                        AppUtils.deleteVideoFile(it1, it)
                        lifecycleScope.launch {
                            mViewmodel.deletedatafromdb(tempTitle)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            lifecycleScope.launch {
                delay(1000)
                AppOpenManager.isShowingAd = false
                isSplash = false
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == RENAME_PERMISSION_REQUEST) {
            optionsItem?.let { item ->
                mActivity?.let { activity ->
                    mViewmodel.nameNew?.let { newName ->
                        lifecycleScope.launch {
                            mViewmodel.renameVideo(activity, item, newName)
                        }

                    }

                }

            }
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == DELETE_PERMISSION_REQUEST) {
            lifecycleScope.launch {
                delay(1000)
                AppOpenManager.isShowingAd = false
                isSplash = false
            }
        }
    }

    override fun onVideoClick(id: String, list: ArrayList<Video>) {
        if (PlayerVideoActivity.getInstance() != null && PlayerVideoActivity.isPipMode) {
            PlayerVideoActivity.getInstance()?.finishAndRemoveTask()
        }
        /*if (AdsManager.mInterstitialAdHigh != null){
            showAppInterstitialAdHighVideo(requireActivity(),"PLAYER_SHOWN",id,list)
        }else{
            showAppInterstitialAdVideo(requireActivity(),"PLAYER_SHOWN",id,list)
        }*/
        activity?.let {
            showInterstitialHome(activity = it) {
                clickVideo(id,list)
            }
        }
    }

    fun clickVideo(id:String,list: ArrayList<Video>){
        lifecycleScope.launch {
            videolistglobal = emptyList()
            videolistglobal = list
            if (ChromeCastDelegate.mChromecastConnection?.isChromeCastConnect == true) {
                val item = if (id.toInt() >= 0 && id.toInt() < list.size) {
                    list[id.toInt()]
                    // Process the item
                } else {
                    // Handle the index out of bounds situation
                    null // or any appropriate value or action
                }
                if (item != null) {
                    val isMp4 = item.contentUri?.let { uri ->
                        mActivity?.let {
                            AppUtils.isSupportedVideoFile(
                                it,
                                Uri.parse(uri)
                            )
                        }
                    }
                    if (isMp4 == true) {
                        if (binding?.videoProgress?.visibility == View.VISIBLE) {
                            Log.d("loadingStatus", "happening")
                        } else {
                            setupChromecastConnection(list, position = id.toInt())
                        }
                    } else {
                        Toast.makeText(
                            mActivity,
                            "sorry this file format is not supported by chromse cast",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                val result = Bundle()
                id?.let {
                    AppUtils.firebaseUserAction("onVideoClick_FVideoFragment", "VideoFragment")
                    result.putString("id", it.toString())
                    result.putBoolean("isliveuri", false)
                    result.putString("uri", "")
                    result.putBoolean("isPlaybackCount", true)
                    result.putString("fragmentName", args.name)
                    if(interHome!=null){
                        interHome?.let {
                            showInterstitial(false,it,requireActivity(),{
                                mActivity?.let { activity ->
                                    val intent = Intent(activity, PlayerVideoActivity::class.java)
                                    intent.putExtras(result)
                                    activity.startActivity(intent)
                                }
                            },inter_videos)
                        }
                    }
                    else{
                        loadFallbackInterstitialAd(requireActivity(), requireActivity().resources.getString(
                            com.avd.R.string.Interstitial_Home_ID_High), requireActivity().resources.getString(
                            com.avd.R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                            interHome=it
                        },{
                            interHome=it
                        })
                        mActivity?.let { activity ->
                            val intent = Intent(activity, PlayerVideoActivity::class.java)
                            intent.putExtras(result)
                            activity.startActivity(intent)
                        }
                    }

                }
            }
        }
    }

    override fun onVideoDelete(item: Video) {
        lifecycleScope.launch {
            item.contentUri?.let {
                mActivity?.let { it1 ->
                    mViewmodel.deleteVideoPermanently(Uri.parse(it), it1)
                }
            }
            var item2 = Entities(
                url = item.contentUri.toString(),
                name = item.title,
                duration = convertToMilliseconds(item.duration)
            )
            mViewmodel.deleteSongFromDb(item2)
        }
    }

    override fun reNameVideo(position: Int) {
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        alreadyshown=false
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onMenuClick(item: Video, position: Int) {
        val dg = VideoOptionBottomSheetFragment(true, false)
        val bundle = Bundle()
        bundle.putBoolean("isFromVideoRelated", true)
        dg.show(parentFragmentManager, "")
        dg.setOptionSelected(object : VideoOptionBottomSheetFragment.OptionSelectedListener {
            override fun onOptionSelected(selectedPosition: Int) {
                dg?.dismiss()
                when (selectedPosition) {
                    0 -> {
                        lifecycleScope.launch {
                            tempTitle = item.title.toString()
                            item.contentUri?.let {
                                mActivity?.let { it1 ->
                                    mViewmodel.deleteVideoPermanently(Uri.parse(it), it1)
                                }
                            }
                        }
                    }
                    1 -> {
                        mActivity?.let {
                            item.contentUri?.let { it1 ->
                                AppUtils.getFilePathFromContentUri(Uri.parse(it1), it)?.let { it1 ->
                                    File(it1)
                                }?.let { it2 ->  mActivity?.let { shareVideo(it,it2) } }
                            }
                        }
                    }
                    2 -> {
                        item.contentUri?.let { uri ->
                            item.title?.let { title ->
                                mActivity?.let {
                                    val uriList: List<Uri> = listOf(
                                        Uri.withAppendedPath(
                                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                            item.id.toString()
                                        )
                                    )
                                    optionsItem = item
                                    showRenameDialogue(item)

                                }
                            }
                        }
                    }
                   3 -> {
                        val video = Video(
                            id = item.id,
                            title = item.title,
                            duration = item.duration,
                            date = item.date,
                            size = item.size,
                            orignalpath = item.orignalpath,
                            isChecked = item.isChecked
                        )
                        val bottomDialog = VideoInfoBottomSheetFragment()
                        bottomDialog.show(
                            requireActivity().supportFragmentManager,
                            bottomDialog.tag
                        )
                        val bundle = Bundle()
                        bundle.putSerializable("video", video)
                        bundle.putString("uri", item.contentUri.toString())
                        bottomDialog.arguments = bundle
                        bottomDialog.show(parentFragmentManager, "")
                    }
                }
            }
        })
    }

    private fun showRenameDialogue(item: Video) {
        mActivity?.let { activity ->
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.rename_dailog, null)
            val btnCancel = view.findViewById<TextView>(R.id.tvCancell)
            val btnOk = view.findViewById<TextView>(R.id.tvOk)
            val editText = view.findViewById<EditText>(R.id.etName)
            editText.setText(item.title)
            val alertDialog = CustomAlertDialog(activity)
            alertDialog.setView(view)
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set the width to 80% of the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams
            btnOk.setOnClickListener {

                lifecycleScope.launch {
                   // mViewmodel.renameVideo(activity, item, newName)

                    val newName = editText.text.toString()
                    if (newName.isEmpty()){
                        Toast.makeText(mActivity, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    else if (item.title.equals(newName)){
                        Toast.makeText(mActivity, "Old title can't change", Toast.LENGTH_SHORT).show()
                        return@launch
                    }else{
                        lifecycleScope.launch {
                            delay(100)
                            hideKeyboard()
                        }
                        mViewmodel.renameVideo(activity, item, newName)
                        alertDialog.dismiss()
                    }
                }

            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
                lifecycleScope.launch {
                    delay(100)
                    hideKeyboard()
                }
            }
            alertDialog.show()
        }
    }

    override fun onSortChanged(isChanged: Boolean, sortType: Int) {
        SORT_TYPE.value = sortType
        mActivity?.let {
            AppPreference.saveSortType(it, sortType)
        }
    }

    private fun setupChromecastConnection(list: ArrayList<Video>, position: Int) {
        isClickedForCasting.value = true
        mSelectedMedia = list
        ChromecastConnection.position = position
        updateSelectedPosition(position)
        loadRemoteMediaFromPlaylist(mActivity as Activity)
    }


   /* fun showAppInterstitialAdVideo(currentActivity: Activity, screenName: String, id: String, list: ArrayList<Video>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!isAdShown(ScreenName.valueOf(screenName)) && maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
                    Log.e("High/Low", "low Ads Shown")
                    if (mInterstitialAd != null) {
                        showLoading(currentActivity)
                        delay(1000)
                        mInterstitialAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    ifAdDisplayed?.value=true
                                    isSplash = true
                                    onInterstitialImpressionSuccess()
                                    maxAdImpressions++
                                    setAdShown(ScreenName.valueOf(screenName), true)
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    ifAdDisplayed?.value=false
                                    mInterstitialAd = null
                                    Log.e("AdsManager", "Splash Ad failed: ${adError.message}")
                                    loadAppInterstitialAd(currentActivity)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    ifAdDisplayed?.value=false
                                    isSplash = false
                                    mInterstitialAd = null
                                    loadAppInterstitialAd(currentActivity)
                                }
                            }
                        AppUtils.firebaseUserAction("inter_home", "inter_home")

                        clickVideo(id,list)
                        delay(100)
                        mInterstitialAd?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        clickVideo(id,list)
                    }
                } else {
                    if (isAdShown(ScreenName.valueOf(screenName))){
                        removeAdShown(ScreenName.valueOf(screenName), false)
                    }
                    Log.e("AdsManager", "invoked without ad")
                    clickVideo(id,list)
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                clickVideo(id,list)
            } finally {
                hideLoading()
            }
        }
    }

    fun showAppInterstitialAdHighVideo(currentActivity: Activity, screenName: String,id: String, list: ArrayList<Video>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (!isAdShown(ScreenName.valueOf(screenName)) && maxAdImpressions < remotemaxAdImpressions && GlobalValues.AdBlockerHelper.isProVersion.value != true) {
                    Log.e("High/Low", "High Ads Shown")
                    if (mInterstitialAdHigh != null) {
                        showLoading(currentActivity)
                        delay(1000)
                        mInterstitialAdHigh?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() {
                                ifAdDisplayed?.value=true
                                isSplash = true
                                onInterstitialImpressionSuccess()
                                maxAdImpressions++
                                setAdShown(ScreenName.valueOf(screenName), true)
                            }
                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                ifAdDisplayed?.value=false
                                mInterstitialAdHigh = null
                                Log.e("AdsManager", "Splash Ad failed: ${adError.message}")
                                loadAppInterstitialAdHigh(currentActivity)
                            }
                            override fun onAdDismissedFullScreenContent() {
                                ifAdDisplayed?.value=false
                                isSplash = false
                                mInterstitialAdHigh = null
                                loadAppInterstitialAdHigh(currentActivity)

                            }
                        }
                        AppUtils.firebaseUserAction("inter_home_high", "inter_home_high")

                        clickVideo(id,list)
                        delay(100)
                        mInterstitialAdHigh?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        clickVideo(id,list)
                    }
                } else {
                    if (isAdShown(ScreenName.valueOf(screenName))){
                        removeAdShown(ScreenName.valueOf(screenName), false)
                    }
                    Log.e("AdsManager", "invoked without ad")
                    clickVideo(id,list)
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                clickVideo(id,list)
            } finally {
                hideLoading()
            }
        }
    }*/
}


