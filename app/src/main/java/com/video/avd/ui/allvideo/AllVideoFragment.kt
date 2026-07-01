package com.video.avd.ui.allvideo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.hideLoading
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.inter_videos
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.ads.AdsManager.recyclerNative
import com.video.avd.constent.GRID_ITEM_SPAN_COUNT
import com.video.avd.constent.SORT_TYPE
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.isClickedForCasting
import com.video.avd.constent.isExpendedRunning
import com.video.avd.constent.isvideo
import com.video.avd.constent.shouldUpdateRecyclerView
import com.video.avd.constent.showfloatandhide
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentAllVideoBinding
import com.video.avd.ui.apppurchase.PremiumDialogListener
import com.video.avd.ui.basefragment.BaseVideoFragment
import com.video.avd.ui.homeVideo.HomeFragment
import com.video.avd.ui.player.ChromeCastDelegate
import com.video.avd.ui.player.ChromeCastDelegate.Companion.mChromecastConnection
import com.video.avd.ui.player.ChromeCastDelegateImp
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.videos.adapter.VideoAdapter
import com.video.avd.ui.videos.adapter.VideoListner
import com.video.avd.ui.videos.model.Video
import com.video.avd.ads.AppOpenManager
import com.video.avd.ads.AppOpenManager.Companion.isShowingAd
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.getFilePathFromContentUri
import com.video.avd.utils.AppUtils.hideKeyboard
import com.video.avd.utils.CustomAlertDialog
import com.video.avd.utils.EventObserver
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.GridSpacingItemDecoration
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.SharedPreferencesManager
import com.video.avd.utils.ToastUtils
import com.video.avd.utils.WeakReferenceVideo
import com.video.avd.utils.chromecast.ChromecastConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AppSettingsDialog
import java.io.File
import javax.inject.Inject


private const val DELETE_PERMISSION_REQUEST = 0x1033
private const val RENAME_PERMISSION_REQUEST = 0x1876

@AndroidEntryPoint
class AllVideoFragment : BaseVideoFragment(), VideoListner, ChromeCastDelegate by ChromeCastDelegateImp(), VideoAdapter.MenuClickListener, PremiumDialogListener, OnUserEarnedRewardListener {

    var binding: FragmentAllVideoBinding? = null
    private val mViewmodel: AllVideosViewModel by activityViewModels()
    var mActivity: FragmentActivity? = null
    val adaptervideo by lazy {
        activity?.let {
            VideoAdapter(
                it,
                listener = this@AllVideoFragment
            )
        }
    }

    private var optionsItem: Video? = null
    var videosListReference = listOf<Video>()
    private var tempId = 0L
    private var selectedVideo: Video? = null
    var permissiongranted = false
    var isuserearned = false
    var sharedPreferencesManager: SharedPreferencesManager? = null
    val freeLimit = 2
    var isScrollingDown = false
    @Inject
    lateinit var myVideos: WeakReferenceVideo



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAllVideoBinding.inflate(inflater, container, false)
        AppUtils.getMain(mActivity).showbottombar()
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (permissiongranted) {
            getDataFromDevice()

        }
        binding?.swipeRefreshLayout?.setOnRefreshListener {
            if (permissiongranted) {
                getDataFromDevice()
            } else {
                binding?.swipeRefreshLayout?.isRefreshing = false
            }
        }


        AdBlockerHelper.isProVersion.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    adaptervideo?.isNativeAdLoaded = false
                    binding?.videoRv?.adapter?.notifyDataSetChanged()
                }
            }
        }
        GlobalValues.is24hourEnabled.observe(viewLifecycleOwner) {
            if (it) {
                adaptervideo?.isNativeAdLoaded = false
                binding?.videoRv?.adapter?.notifyDataSetChanged()
            }
        }
        sharedPreferencesManager = mActivity?.let { SharedPreferencesManager(it) }
        binding?.videoRv?.setHasFixedSize(true)
        binding?.videoRv?.layoutManager = if (VIEW_TYPE.value == 0) LinearLayoutManager(
            activity,
            LinearLayoutManager.VERTICAL,
            false
        )
        else GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
        adaptervideo?.setOnClickListner(this@AllVideoFragment)
        binding?.videoRv?.adapter = adaptervideo
        getDataFromDb()
        mActivity?.let { activity ->
         //   AppUtils.firebaseUserAction("onViewCreated_AllVideoFragment", "AllVideoFragment")
            isvideo = true
            observers(activity)
            mViewmodel.isLoading.observe(viewLifecycleOwner) {
                binding?.videoProgress?.visibility = if (it == true) View.VISIBLE else View.GONE
            }
            castWaitObserver()
            //  AppUtils.getMain(activity).hidebottombar()
            binding?.allow?.setOnClickListener {
                AppSettingsDialog.Builder(this).build().show()
            }
        }
//        mViewmodel.totalSizeLiveData.observe(viewLifecycleOwner) {
//            it?.let {
//                binding?.size?.text = it
//            }
//        }

        binding?.videoRv?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && !isScrollingDown) {
                    showfloatandhide.value=false
                    isScrollingDown = true
                } else if (dy < 0 && isScrollingDown) {
                    showfloatandhide.value=true
                    isScrollingDown = false
                }
            }
        })

    }



    private fun castWaitObserver() {
        isClickedForCasting.observe(viewLifecycleOwner) {
            binding?.videoProgress?.visibility = if (it) View.VISIBLE else View.GONE
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setAdapter(videosList: List<Video>) {
        adaptervideo?.updateList(videosList)
        myVideos.setObjectvideo(videosList)
    }


    override fun onResume() {
        super.onResume()
        if (videosListReference.isEmpty()) {
            getPermission()
        }
    }


    private fun observers(activity: FragmentActivity) {
        try {
            VIEW_TYPE.observe(viewLifecycleOwner) {
                val currentState = binding?.videoRv?.layoutManager?.onSaveInstanceState()
                if (it == 1) {
                    val itemgrid = GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
                    itemgrid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            val viewType: Int? = adaptervideo?.getItemViewType(position)
//                                    && viewType == adaptervideo?.VIEW_TYPE_AD_Meta
                            return if (viewType == VideoAdapter.VIEW_TYPE_ADMOB || viewType == VideoAdapter.VIEW_TYPE_META) {
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
                    binding?.videoRv?.layoutManager =
                        LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
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
        try {
            SORT_TYPE.observe(viewLifecycleOwner) {
                it?.let {
                    lifecycleScope.launch {
                        val list = mViewmodel.sortVideosList(it, videosListReference)
                        ChromecastConnection.listofvideos = list
                        if (list.isNotEmpty()) {
                            adaptervideo?.updateList(list)
                            binding?.videoRv?.layoutManager?.scrollToPosition(0)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mViewmodel.isForDelete.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                mViewmodel.deletedatafromdb(tempId)
            }
        })
        mViewmodel.isForRename.observe(viewLifecycleOwner) {
            it?.let { renamed ->
                if (renamed) {
                    lifecycleScope.launch {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            mViewmodel.UpdateDataItemTitle()
                        }else{
                            mViewmodel.videosListFlow.collectLatest { videosList ->
                                if (videosList.isNotEmpty()) {
                                    mViewmodel.addVideoToDb(videosList)
                                }else {
                                }
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
                isShowingAd = true
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
            isShowingAd = true
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
        mViewmodel.videoDeletedFromDB.observe(viewLifecycleOwner, EventObserver {
            if (it) {
                Toast.makeText(requireContext(), "deleted from db", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "not deleted from db", Toast.LENGTH_SHORT).show()
            }
        })
    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onVideoClick(id: String, list: ArrayList<Video>) {
        if (PlayerVideoActivity.getInstance() != null && PlayerVideoActivity.isPipMode) {
            PlayerVideoActivity.getInstance()?.finishAndRemoveTask()
        }
        /*if (AdsManager.mInterstitialAdHigh != null){
           showAppInterstitialAdHighVideo(requireActivity(),"PLAYER_SHOWN",id,list)


        }else{
           showAppInterstitialAdVideo(requireActivity(),"PLAYER_SHOWN",id,list)
        }*/
        mActivity?.let {activity ->
            videoClick(id, list)
           /* if(AdBlockerHelper.interHome!=null){
                AdBlockerHelper.interHome?.let {
                    AdBlockerHelper.showInterstitial(false, it,activity,{
                        videoClick(id, list)
                        hideLoading()
                    },
                        AdBlockerHelper.inter_home)
                }

            }
            else{
                videoClick(id, list)
            }*/

        }
        /*activity?.let {
            showInterstitialHome(activity = it) {
                videoClick(id, list)
            }
        }*/

    }




    fun videoClick(id:String,list: ArrayList<Video>){
        lifecycleScope.launch(Dispatchers.Main){
            HomeFragment.isHomeConnectin = false
            videolistglobal = emptyList()
            videolistglobal = list
            if (mChromecastConnection?.isChromeCastConnect == true) {
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
                        //   binding?.videoProgress?.visibility = View.VISIBLE
                        if (binding?.videoProgress?.visibility == View.VISIBLE) {
                            Log.d("loadingStatus", "happening")
                        } else {
                            Log.d("loadingStatus", "not happening")
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
                id.let {
                    result.putString("id", it)
                    result.putBoolean("isliveuri", false)
                    result.putString("uri", "")
                    result.putBoolean("isPlaybackCount", true)
                    result.putString("fragmentName", getString(R.string.all_videos))
                    if(AdBlockerHelper.interHome!=null){
                        AdBlockerHelper.interHome?.let {
                            showInterstitial(false,it,requireActivity(),{
                                mActivity?.let { activity ->
                                    val intent = Intent(activity, PlayerVideoActivity::class.java)
                                    intent.putExtras(result)
                                    shouldUpdateRecyclerView.value = false
                                    activity.startActivity(intent)
                                    hideLoading()
                                    AppUtils.firebaseUserAction("onVideoClick_AllVideoFragment", "MovetoNext")
                                }
                            },inter_videos)
                        }
                    }
                    else{
                        Log.w("checkAd","interHome is Null")
                        mActivity?.let { activity ->
                            val intent = Intent(activity, PlayerVideoActivity::class.java)
                            intent.putExtras(result)
                            shouldUpdateRecyclerView.value = false
                            activity.startActivity(intent)
                            AppUtils.firebaseUserAction("onVideoClick_AllVideoFragment", "MovetoNext")
                        }
                        loadFallbackInterstitialAd(mActivity?:requireActivity(), BuildConfig.inter_home_high, BuildConfig.inter_home,inter_home_high,inter_home_normal,{
                            interHome=it
                        },{
                            interHome=it
                        })
                    }


                }
            }

        }
    }



    private fun divideAndSubtract(position: Int): Int {
        val result: Int = if (position > 3) {
            val position1 = position - 4
            val quotient = position1 / 11
            position - quotient - 1
        } else {
            position
        }
        return result
    }



    override fun onVideoDelete(item: Video) {
        lifecycleScope.launch {
            item.contentUri?.let {
                mActivity?.let { it1 ->
                    deleteVideoPermanently(Uri.parse(it), it1)
                }
            }
        }
    }



    override fun reNameVideo(position: Int) {
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            mViewmodel.urinew?.let {
                mActivity?.let { it1 ->
                    try {
                        AppUtils.deleteVideoFile(it1, it)
                        mViewmodel.deletedatafromdb(tempId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                isShowingAd = false
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
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                isShowingAd = false
            }
        } else if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            isvideo = true
            getPermission()
        }
        else{
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                isShowingAd = false
            }
        }
    }


    override fun onPermissionsGranted() {
        Log.e("AddedtoDatabase", "DB added")
        AppUtils.firebaseUserAction("vid_permission_granted", "AllVideoFragment")
        mViewmodel.isLoading.postValue(true)
        getDataFromDevice()
        permissiongranted = true
        binding?.permission?.visibility = View.GONE
        binding?.swipeRefreshLayout?.isEnabled = true
    }


    fun getDataFromDb() {
        lifecycleScope.launch {
            mViewmodel.videosData.collectLatest { it ->
                if (it.isNotEmpty()) {
                    val sortedList = mViewmodel.sortVideosList(SORT_TYPE.value ?: 2, it)
                    mActivity?.let { activity ->
                        setAdapter(sortedList)
                        if (NetworkUtils.isOnline(activity) && AdBlockerHelper.isProVersion.value != true) {
                            if (GlobalValues.is24hourEnabled.value == false) {
                                if (recyclerNative){
//                                    adaptervideo?.loadNativeAd(activity)
//                                    adaptervideo?.loadMetaNativeAd(activity)
                                }
                            }
                        } else {
                            adaptervideo?.isNativeAdLoaded = false
                            adaptervideo?.isLargeAdLoaded=false
                        }
                    }
                    videosListReference = sortedList
                    ChromecastConnection.listofvideos = sortedList
                    binding?.permission?.visibility = View.GONE
                }
            }
        }
    }



    fun getDataFromDevice() {
        mActivity?.let { activity ->
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mViewmodel.videosListFlow.collectLatest { videosList ->
                        if (videosList.isNotEmpty()) {
                            binding?.noVideos?.visibility = View.GONE
                            if (shouldUpdateRecyclerView.value == true) {
                                mViewmodel.isLoading.postValue(false)
                                Log.e("DBINSERT", "DATABASE INSERTION")
                                mViewmodel.addVideoToDb(videosList)
                            } else {
                                mViewmodel.isLoading.postValue(false)
                                shouldUpdateRecyclerView.value = true
                            }
                        } else {
                            mViewmodel.isLoading.value = false
                            binding?.noVideos?.visibility = View.VISIBLE
                        }
                        withContext(Dispatchers.Main) {
                            binding?.swipeRefreshLayout?.isRefreshing = false
                        }
                    }
                }
            }
        }
    }


    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        AppUtils.firebaseUserAction("vid_permission_not_granted", "AllVideoFragment")
        binding?.noVideos?.visibility = View.GONE
        binding?.permission?.visibility = View.VISIBLE
        binding?.swipeRefreshLayout?.isEnabled = false
    }


    @Inject
    lateinit var videoWeakrefrence: WeakReferenceVideo


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }


    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }




    private fun deleteVideoPermanently(uri: Uri, context: Context) {
        mActivity?.let {activity->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val inflater = LayoutInflater.from(mActivity)
                val view = inflater.inflate(R.layout.alert_dailog, null)
                view.findViewById<TextView>(R.id.alertMessage).text =
                    getString(R.string.do_you_want_to_delete_this)
                val icon = view.findViewById<ImageView>(R.id.alertImage)
                val btnCancel = view.findViewById<Button>(R.id.btnCancel)
                val btnOk = view.findViewById<Button>(R.id.btnOk)
                btnOk.text = getString(R.string.delete)
                icon.setImageResource(R.drawable.ci_delete_p)
                val alertDialog = CustomAlertDialog(activity)
                alertDialog.setView(view)

                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(alertDialog.window?.attributes)
                layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
                alertDialog.window?.attributes = layoutParams
                btnOk.setOnClickListener {
                    lifecycleScope.launch {
                        mViewmodel.performDeleteImage(uri, context)
                    }
                    alertDialog.dismiss()
                }
                btnCancel.setOnClickListener {
                    alertDialog.dismiss()
                }
                alertDialog.show()
            } else {
                lifecycleScope.launch {
                    mViewmodel.performDeleteImage(uri, context)
                }
            }
        }
    }

    private fun showRenameDialogue(item: Video) {
        mActivity?.let { activity ->
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.rename_dailog, null)
            val btnCancel = view.findViewById<TextView>(R.id.tvCancell)
            val btnOk = view.findViewById<TextView>(R.id.tvOk)
            val editText = view.findViewById<EditText>(R.id.etName)
            editText.setText(item.title)
            val alertDialog=CustomAlertDialog(activity)
            alertDialog.setView(view)
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set the width to 80% of the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams
            btnOk.setOnClickListener {
                lifecycleScope.launch {
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


    override fun onMenuClick(item: Video, position: Int) {
        AppUtils.firebaseUserAction("3dot_clicked_home", "AllVideoFragment")
        val dg = VideoOptionBottomSheetFragment(true)
        dg.show(parentFragmentManager, "")
        dg.setOptionSelected(object : VideoOptionBottomSheetFragment.OptionSelectedListener {
            override fun onOptionSelected(selectedPosition: Int) {
                dg?.dismiss()
                when (selectedPosition) {
                    0 -> {
                        lifecycleScope.launch {
                            tempId = item.id
                            item.contentUri?.let {
                                mActivity?.let { it1 ->
                                    deleteVideoPermanently(Uri.parse(it), it1)
                                }
                            }
                        }
                    }

                    1 -> {
                        mActivity?.let {
                            item.contentUri?.let { it1 ->
                                getFilePathFromContentUri(Uri.parse(it1), it)?.let { it1 ->
                                    File(it1)
                                }?.let { it2 -> shareVideo(it2) }
                            }
                        }
                    }

                    2 -> {
                        item.contentUri?.let { uri ->
                            item.title?.let { title ->
                                mActivity?.let {
                                    optionsItem = item
                                    showRenameDialogue(item)

                                }
                            }
                        }
                    }

                    3 -> {
                        val video = Video(
                            id = item.id,
                            contentUri = null,
                            title = item.title,
                            duration = item.duration,
                            date = item.date,
                            size = item.size,
                            orignalpath = item.orignalpath,
                            isChecked = item.isChecked
                        )
                        val bottomDialog = VideoInfoBottomSheetFragment()
                        val bundle = Bundle()
                        bundle.putSerializable("video", video)
                        bundle.putString("uri", item.contentUri.toString())
                        bottomDialog.arguments = bundle
                        bottomDialog.show(requireActivity().supportFragmentManager,"video_info_dialog")
                    }
                }
            }
        })
    }

    override fun onPause() {
        val existingDialog = parentFragmentManager.findFragmentByTag("video_info_dialog")
        if (existingDialog != null && existingDialog is DialogFragment) {
            existingDialog.dismissAllowingStateLoss()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun shareVideo(videoFile: File) {
        if (videoFile.exists()) {
            mActivity?.let {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "video/mp4"
                val videoUri: Uri = FileProvider.getUriForFile(
                    it,
                    "${it.packageName}.provider",
                    videoFile
                )
                shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ContextCompat.startActivity(
                    it,
                    Intent.createChooser(shareIntent, "Share video"),
                    null
                )
            }
        } else {
            // Handle case when the video file doesn't exist
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupChromecastConnection(list: ArrayList<Video>, position: Int) {
        mActivity?.let {
            mSelectedMedia = list
            ChromecastConnection.listofvideos = list
            ChromecastConnection.position = position
            updateSelectedPosition(position)
            isExpendedRunning = false
            loadRemoteMediaFromPlaylist(mActivity as Activity)
        }
    }


    override fun onWatchVideoClick() {

    }


    override fun onUnlockThemeClick() {
        mActivity?.let {
            if (!NetworkUtils.isOnline(it)) {
                ToastUtils.showToast(requireContext(), "Internet connection error")
            } else {
                AppUtils.getMain(it).hidebottombar()
                AppUtils.getMain(it).hideBannerAd()
                AppUtils.getMain(it).navController?.navigate(R.id.propanel)
            }
        }
    }


    override fun onUserEarnedReward(p0: RewardItem) {
        isuserearned = true
    }





/*
    fun showAppInterstitialAdVideo(currentActivity: Activity, screenName: String, id: String, list: ArrayList<Video>) {
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
                                    isSplash = true
                                    onInterstitialImpressionSuccess()
                                    maxAdImpressions++
                                    setAdShown(ScreenName.valueOf(screenName), true)
                                    ifAdDisplayed?.value=true
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialAd = null
                                    Log.e("AdsManager", "Splash Ad failed: ${adError.message}")
                                    loadAppInterstitialAd(currentActivity)
                                    ifAdDisplayed?.value=false
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    isSplash = false
                                    mInterstitialAd = null
                                    ifAdDisplayed?.value=false
                                    loadAppInterstitialAd(currentActivity)
                                }
                            }
                        AppUtils.firebaseUserAction("inter_home", "inter_home")
                        videoClick(id,list)
                        delay(100)
                        mInterstitialAd?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        videoClick(id,list)
                    }
                } else {
                    if (isAdShown(ScreenName.valueOf(screenName))){
                        removeAdShown(ScreenName.valueOf(screenName), false)
                    }
                    Log.e("AdsManager", "invoked without ad")
                    videoClick(id,list)
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                videoClick(id,list)
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

                        videoClick(id,list)
                        delay(100)
                        mInterstitialAdHigh?.show(currentActivity)
                    } else {
                        Log.e("AdsManager", "Splash Ad not ready")
                        videoClick(id,list)
                    }
                } else {
                    if (isAdShown(ScreenName.valueOf(screenName))){
                        removeAdShown(ScreenName.valueOf(screenName), false)
                    }
                    Log.e("AdsManager", "invoked without ad")
                    videoClick(id,list)
                }
            } catch (e: Exception) {
                Log.e("AdsManager", "Error showing interstitial ad", e)
                videoClick(id,list)
            } finally {
                hideLoading()
            }
        }
    }
*/

}



