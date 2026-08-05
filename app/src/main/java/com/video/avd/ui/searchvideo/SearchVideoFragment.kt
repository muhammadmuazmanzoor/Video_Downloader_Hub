package com.video.avd.ui.searchvideo


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ClipData
import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.ads.AppOpenManager
import com.video.avd.constent.GRID_ITEM_SPAN_COUNT
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.isSplash
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentSearchVideoBinding
import com.video.avd.ui.allvideo.VideoInfoBottomSheetFragment
import com.video.avd.ui.allvideo.VideoOptionBottomSheetFragment
import com.video.avd.ui.apppurchase.PremiumDialogListener
import com.video.avd.ui.folder.adapter.FolderAdapter
import com.video.avd.ui.folder.adapter.OnClickListner
import com.video.avd.ui.player.ChromeCastDelegate
import com.video.avd.ui.player.ChromeCastDelegateImp
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.videos.adapter.VideoAdapter
import com.video.avd.ui.videos.adapter.VideoListner
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject


private const val DELETE_PERMISSION_REQUEST = 0x1033
private const val RENAME_PERMISSION_REQUEST = 0x1876
private const val VIDEO_ACTIONS_TAG = "SearchVideoActions"

@AndroidEntryPoint
class SearchVideoFragment : Fragment(),ChromeCastDelegate by ChromeCastDelegateImp() , OnClickListner , VideoListner, VideoAdapter.MenuClickListener,
    PremiumDialogListener, OnUserEarnedRewardListener,AdDismissedListener {

    private var binding: FragmentSearchVideoBinding? = null
    private val args : SearchVideoFragmentArgs by navArgs()
    var adaptervideo : VideoAdapter? = null
    var adapterfolder : FolderAdapter? = null
    var videolist = listOf<Video>()
    var posiion : Int? = null
    var nameNew : String? = null
    private val mViewModel : SearchVideoViewModel by viewModels()
    @Inject
    lateinit var myWeakRefrence : WeakReferenceVideo
    var mActivity: FragmentActivity? = null
    private var tempTitle = ""
    private var optionsItem: Video? = null
    private var selectedVideo : Video?=null
    var isuserearned=false
    val sharedPreferencesManager = mActivity?.let { SharedPreferencesManager(it) }
    val freeLimit = 2

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        if (binding==null){
        binding = FragmentSearchVideoBinding.inflate(inflater, container, false)
        binding?.appHeader?.back?.setOnClickListener {
            try {
                findNavController().popBackStack()
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
        binding?.appHeader?.back?.visibility = View.VISIBLE
        binding?.searchRv?.hasFixedSize()
        val spanCount = if (args.isFolder) 3 else GRID_ITEM_SPAN_COUNT
        binding?.searchRv?.layoutManager= if (VIEW_TYPE.value==0) LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        else GridLayoutManager(requireContext(), spanCount)
        if (args.isFolder){
            adapterfolder= myWeakRefrence.getObjectfolder().let { mActivity?.let { it1 ->
                FolderAdapter(it,
                    it1
                )
            } }
            binding?.searchRv?.adapter = adapterfolder
            adapterfolder?.setOnClickListner(this)
        }else{
            adaptervideo= myWeakRefrence.getObjectvideo().let { mActivity?.let { it1 ->
                videolist=it
                VideoAdapter(
                    context = it1,list =it,listener = this@SearchVideoFragment, showAd = false)
            } }
            binding?.searchRv?.adapter = adaptervideo
            adaptervideo?.setOnClickListner(this)
            binding?.searchRv?.addItemDecoration(GridSpacingItemDecoration(20))
        }
        binding?.appHeader?.back?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.appHeader?.back?.visibility=View.VISIBLE
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.getMain(mActivity).showBannerAd()
        AppUtils.firebaseUserAction("onViewCreated_SearchVideoFragment", "SearchVideoFragment")
        binding?.appHeader?.searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (args.isFolder){
                    adapterfolder?.filter(newText)
                    if (newText.isNullOrEmpty()) {
                        myWeakRefrence.getObjectfolder().let { adapterfolder?.setData(it) }
                    }
                }else{
                    adaptervideo?.filter(newText)
                    if (newText.isNullOrEmpty()) {
                        myWeakRefrence.getObjectvideo().let { adaptervideo?.updateList(it) }
                    }
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                if (args.isFolder){
                    adapterfolder?.filter(query)
                }else{
                    adaptervideo?.filter(query)
                }
                return true
            }

        })

        @SuppressLint("Range")
        fun handleSuggestionClick(position: Int) {
            val cursor = binding?.appHeader?.searchView?.suggestionsAdapter?.cursor
            if (cursor != null && cursor.moveToPosition(position)) {
                val folderName = cursor.getString(cursor.getColumnIndex("folderName") ?: 0 )
                val folderId = cursor.getString(cursor.getColumnIndex("folderId") ?: 0)

                if (!folderId.isNullOrEmpty()) {
                    // Open the selected folder
                    if (args.isFolder){
                        adapterfolder?.filter(folderName)
                    }else{
                        adaptervideo?.filter(folderName)
                    }

                } else {
                    Log.e("SearchView", "Folder ID is null or empty")
                }
            }
        }

        // Add this listener to handle suggestion clicks
        binding?.appHeader?.searchView?.setOnSuggestionListener(object : SearchView.OnSuggestionListener,
            androidx.appcompat.widget.SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                handleSuggestionClick(position)
                return true
            }

            override fun onSuggestionClick(position: Int): Boolean {
                handleSuggestionClick(position)
                return true
            }
        })

        binding?.appHeader?.back?.setOnClickListener {
            findNavController().popBackStack()
        }

        mViewModel.permissionNeededForDelete.observe(viewLifecycleOwner, Observer { intentSender ->
            Log.d(VIDEO_ACTIONS_TAG, "delete authorization sender received: ${intentSender != null}")
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
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

//        VIEW_TYPE.observe(viewLifecycleOwner){
//            try {
//                if (it==1){
//                    val spanCount = if (args.isFolder) 3 else GRID_ITEM_SPAN_COUNT
//                    binding?.searchRv?.layoutManager = GridLayoutManager(requireContext(), spanCount)
//                    adapterfolder?.notifyDataSetChanged()
//                }else{
//                    binding?.searchRv?.layoutManager =
//                        LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
//                    adapterfolder?.notifyDataSetChanged()
//                }
//            }catch (e:Exception){
//                e.printStackTrace()
//            }
//        }
//        mActivity?.let { binding?.adView?.let { it1 -> AdUtils.loadBannerAd(it1, it) } }

        binding?.appHeader?.searchView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                binding?.appHeader?.searchView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                val transition: Transition? =
                    mActivity?.let { TransitionInflater.from(it).inflateTransition(R.transition.slide_effect) }

                (binding?.appHeader?.searchView?.parent as ViewGroup?)?.let {
                    TransitionManager.beginDelayedTransition(
                        it, transition)
                }
                binding?.appHeader?.searchView?.visibility=View.VISIBLE
            }
        })
        mViewModel.remove.observe(viewLifecycleOwner, EventObserver{
            if (it > 0){
                Toast.makeText(requireContext(), "removed from favourites", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(requireContext(), "an error occurred", Toast.LENGTH_SHORT).show()
            }
        })
        mViewModel.isForDelete.observe(viewLifecycleOwner) { success ->
            Log.d(VIDEO_ACTIONS_TAG, "delete result observed: success=$success")
            if (success == null) return@observe
            if (success == true) {
                optionsItem?.let { deletedItem ->
                    videolist = videolist.filter { it.id != deletedItem.id }
                    adaptervideo?.updateList(videolist)
                    val query = binding?.appHeader?.searchView?.query?.toString()
                    if (!query.isNullOrEmpty()) {
                        adaptervideo?.filter(query)
                    }
                    Toast.makeText(requireContext(), "Video deleted", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Unable to delete video", Toast.LENGTH_SHORT).show()
            }
        }
        mViewModel.isForRename.observe(viewLifecycleOwner) { success ->
            Log.d(VIDEO_ACTIONS_TAG, "rename result observed: success=$success")
            if (success == null) return@observe
            if (success == true) {
                refreshVideoResults()
            } else {
                Toast.makeText(requireContext(), "Unable to rename video", Toast.LENGTH_SHORT).show()
            }
        }
        mViewModel.permissionNeededForRename.observe(viewLifecycleOwner){intentSender ->
            Log.d(VIDEO_ACTIONS_TAG, "rename authorization sender received: ${intentSender != null}")
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
    }


    override fun onResume() {
        super.onResume()
        mActivity?.let {
            binding?.appHeader?.searchView?.requestFocus()
            binding?.appHeader?.searchView?.postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // For Android 11 (API Level 30) and above
                    val controller = binding?.appHeader?.searchView?.windowInsetsController
                    controller?.show(WindowInsets.Type.ime())
                } else {
                    // For older versions
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                }
            }, 200) // 200 milliseconds delay
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            Log.d(VIDEO_ACTIONS_TAG, "delete authorization result: RESULT_OK")
            // On Android 11+, MediaStore.createDeleteRequest performs the deletion when the user
            // confirms the system dialog. The file is already deleted at this point.
            optionsItem?.let { deletedItem ->
                // Remove from Room DB
                mViewModel.deletedatafromdb(deletedItem.id)
                // Remove from adapter list
                videolist = videolist.filter { it.id != deletedItem.id }
                adaptervideo?.updateList(videolist)
                val query = binding?.appHeader?.searchView?.query?.toString()
                if (!query.isNullOrEmpty()) {
                    adaptervideo?.filter(query)
                }
                Toast.makeText(requireContext(), "Video deleted", Toast.LENGTH_SHORT).show()
            }
        }
        else if (resultCode == Activity.RESULT_OK && requestCode == RENAME_PERMISSION_REQUEST) {
            Log.d(
                VIDEO_ACTIONS_TAG,
                "rename authorization result: RESULT_OK itemId=${optionsItem?.id} newName=${mViewModel.nameNew}"
            )
            optionsItem?.let { item ->
                mActivity?.let { activity ->
                    mViewModel.nameNew?.let { newName ->
                        lifecycleScope.launch {
                            mViewModel.renameVideo(activity, item, newName)
                        }

                    }

                }

            }

        }
        else if  (resultCode == Activity.RESULT_CANCELED && requestCode == DELETE_PERMISSION_REQUEST){
            Log.d(VIDEO_ACTIONS_TAG, "delete authorization result: RESULT_CANCELED")
            lifecycleScope.launch {
                delay(1000)
                AppOpenManager.isShowingAd =false
                isSplash = false
            }
        }
    }

    private fun refreshVideoResults() {
        val activity = mActivity ?: return
        Log.d(VIDEO_ACTIONS_TAG, "refresh requested")
        lifecycleScope.launch {
            delay(300)
            mViewModel.getAllVideos(activity).collect { videos ->
                Log.d(VIDEO_ACTIONS_TAG, "refresh received ${videos.size} videos")
                videolist = videos
                adaptervideo?.updateList(videos)
            }
        }
    }



    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onClickListner(id: String, name: String) {
        mActivity?.let {
            AppUtils.firebaseUserAction("onClickListener_SearchVideoFragment", "SearchVideoFragment")
            AppUtils.getMain(it).hidebottombar()
            val bundle = Bundle()
            bundle.putString("id",id)
            bundle.putString("name",name)
            try {
                findNavController().navigate(R.id.action_global_videosFragment,bundle)
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onVideoClick(id: String, list: ArrayList<Video>) {
        if (PlayerVideoActivity.getInstance() != null && PlayerVideoActivity.isPipMode){
            PlayerVideoActivity.getInstance()?.finishAndRemoveTask()
        }
        lifecycleScope.launch {
            try {
                AppUtils.firebaseUserAction("onVideoClick_SearchVideoFragment", "SearchVideoFragment")
                if (list.isNotEmpty() && id.toInt() >= 0 && id.toInt() < list.size) {
                    val video = list[id.toInt()]
                    video.updatedTimeStump = System.currentTimeMillis()
                    video.timeStump = System.currentTimeMillis()
                    video.isRecent=true
                    mViewModel.updateUserData(video)
                }
            } catch (e: NullPointerException) {
                if (list.isNotEmpty() && id.toInt() >= 0 && id.toInt() < list.size) {
                    val video = list[id.toInt()]
                    video.updatedTimeStump = System.currentTimeMillis()
                    video.timeStump = System.currentTimeMillis()
                    video.isRecent=true
                    mViewModel.updateUserData(video)
                }
            }
        }
        videolistglobal=list
        if (ChromeCastDelegate.mChromecastConnection?.isChromeCastConnect == true){
            setupChromecastConnection(list, position = id.toInt())
        }else{
            val result = Bundle()
            result.putString("id", id)
            result.putBoolean("isliveuri", false)
            result.putString("uri", "")
            mActivity?.let {
                AppUtils.getMain(it).hidebottombar()
                val intent=Intent(it, PlayerVideoActivity::class.java)
                intent.putExtras(result)
                startActivity(intent)
            }
        }
    }

    override fun onVideoDelete(item: Video) {
        lifecycleScope.launch {
            item.contentUri?.let {
                mActivity?.let { it1 -> mViewModel.performDeleteImage(Uri.parse(it), it1) }
            }
        }
    }

    override fun reNameVideo(position: Int) {
        showRenameDialog(position)
    }


    fun showRenameDialog(position: Int) {
        val builder: AlertDialog.Builder? = mActivity?.let { AlertDialog.Builder(it) }
        builder?.setTitle("Rename Video")
        val view: View = LayoutInflater.from(context).inflate(R.layout.rename_dialogue, null)
        builder?.setView(view)
        val newNameEditText: EditText = view.findViewById(R.id.new_name_edittext)
        val newName = newNameEditText.text.toString()
        val oldVideo: Video = videolist[position]
        val file = oldVideo.contentUri?.let {
            mActivity?.let { it1 ->
                AppUtils.getPathFromUri(
                    it1,
                    Uri.parse(it)
                )?.let { File(it) }
            }
        }
        newNameEditText.setText(oldVideo.title)
        builder?.setPositiveButton("OK") { dialog, which ->
            posiion=position
            val path= file?.parentFile?.absolutePath
            val newpath =path+"/"+newNameEditText.text.toString()+".mp4"
            val newfile= File(newpath)
            nameNew=newNameEditText.text.toString()
            val rename =file?.renameTo(newfile)
            if (rename == true){
                MediaScannerConnection.scanFile(
                    context, arrayOf(path),
                    null
                ) { path, uri ->
                    // Do something after the scan is complete, if needed
                    val flow = mActivity?.let { mViewModel.getAllVideos(it) }
                    lifecycleScope.launch {
                        flow?.collect { videos ->
                            // Do something with the list of videos
                            videolist=videos
                            adaptervideo= mActivity?.let { VideoAdapter(context = it, list = videos, listener = this@SearchVideoFragment, showAd = false) }!!
                            binding?.searchRv?.adapter=adaptervideo
                            adaptervideo?.setOnClickListner(this@SearchVideoFragment)
                        }
                    }
                }
            }else{
                Toast.makeText(mActivity,"Not Renamed", Toast.LENGTH_SHORT).show()
            }
        }
        builder?.setNegativeButton("Cancel", null)
        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setupChromecastConnection(list: ArrayList<Video>, position: Int) {
        mSelectedMedia = list
        updatePosition(position)
        mActivity?.let { AppPreference.saveChromeListPosition(it,position) }
        loadRemoteMedia(mActivity as Activity)
    }

    override fun onMenuClick(item: Video, position: Int) {
        Log.d(
            VIDEO_ACTIONS_TAG,
            "menu opened: adapterPosition=$position id=${item.id} uri=${item.contentUri} title=${item.title}"
        )
        val dg = VideoOptionBottomSheetFragment(true, false)
        dg.setOptionSelected(object : VideoOptionBottomSheetFragment.OptionSelectedListener {
            override fun onOptionSelected(selectedPosition: Int) {
                Log.d(
                    VIDEO_ACTIONS_TAG,
                    "menu selected: option=$selectedPosition id=${item.id} uri=${item.contentUri}"
                )
                dg.dismiss()
                when (selectedPosition) {
//                    0 -> {
//                        if (mActivity?.let { NetworkUtils.isOnline(it) } == true){
//                            var conversionCount = sharedPreferencesManager?.getConversionCount()
//                            if (conversionCount != null) {
//                                if (conversionCount < freeLimit) {
//                                    // Allow free conversion
//                                    conversionCount++
//                                    sharedPreferencesManager?.saveConversionCount(conversionCount)
//                                    val outputDir: File =
//                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZM_mp3")
//                                        } else {
//                                            File(Environment.getExternalStorageDirectory(), "ZM_mp3")
//                                        }
//                                    if (!outputDir.exists()) {
//                                        outputDir.mkdirs()
//                                    }
//                                    val title = item?.title
//                                    val outputAudioFilePath = File(outputDir, "$title.mp3").path
//                                    val videoPath =
//                                        mActivity?.let {
//                                            AppUtils.getPathFromUri(
//                                                it,
//                                                Uri.parse(item?.contentUri)
//                                            )
//                                        }
//
//                                    val bottomDialog = ConverterBottomSheet()
//                                    bottomDialog.setCancelable(false)
//                                    val bundle = Bundle()
//                                    bundle.putString("videoPath", videoPath)
//                                    bundle.putString("audioPath", outputAudioFilePath)
//                                    bundle.putString("title", title)
//                                    bottomDialog.arguments = bundle
//                                    // bundle.putSerializable("video", item)
//                                    if (isAdded && !requireActivity().isFinishing) {
//                                        bottomDialog.show(
//                                            requireActivity().supportFragmentManager,
//                                            bottomDialog.tag
//                                        )
//                                    }
//                                    Log.d("videoPath", "$videoPath")
//                                } else {
//                                    // Logic to show rewarded ad
//                                    // After watching the ad, call performConversion() and consider if you want to reset or increment the count
//                                    if (GlobalValues.AdBlockerHelper.isProVersion.value != true) {
//                                        selectedVideo = item
//                                        val dg = PremiumDialog()
//                                        dg.setListner(this@SearchVideoFragment)
//                                        PremiumDialog.isMp3=true
//                                        dg.show(parentFragmentManager, "")
//                                    } else {
//                                        val outputDir: File =
//                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                                File(
//                                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//                                                    "ZM_mp3"
//                                                )
//                                            } else {
//                                                File(Environment.getExternalStorageDirectory(), "ZM_mp3")
//                                            }
//                                        if (!outputDir.exists()) {
//                                            outputDir.mkdirs()
//                                        }
//                                        val title = item?.title
//                                        val outputAudioFilePath = File(outputDir, "$title.mp3").path
//                                        val videoPath =
//                                            mActivity?.let {
//                                                AppUtils.getPathFromUri(
//                                                    it,
//                                                    Uri.parse(item?.contentUri)
//                                                )
//                                            }
//
//                                        val bottomDialog = ConverterBottomSheet()
//                                        bottomDialog.setCancelable(false)
//                                        val bundle = Bundle()
//                                        bundle.putString("videoPath", videoPath)
//                                        bundle.putString("audioPath", outputAudioFilePath)
//                                        bundle.putString("title", title)
//                                        bottomDialog.arguments = bundle
//                                        // bundle.putSerializable("video", item)
//                                        if (isAdded && !requireActivity().isFinishing) {
//                                            bottomDialog.show(
//                                                requireActivity().supportFragmentManager,
//                                                bottomDialog.tag
//                                            )
//                                        }
//                                        Log.d("videoPath", "$videoPath")
//                                    }
//                                }
//                            }
//                        }else{
//                            val outputDir: File =
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                    File(
//                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//                                        "ZM_mp3"
//                                    )
//                                } else {
//                                    File(Environment.getExternalStorageDirectory(), "ZM_mp3")
//                                }
//                            if (!outputDir.exists()) {
//                                outputDir.mkdirs()
//                            }
//                            val title = item?.title
//                            val outputAudioFilePath = File(outputDir, "$title.mp3").path
//                            val videoPath =
//                                mActivity?.let {
//                                    AppUtils.getPathFromUri(
//                                        it,
//                                        Uri.parse(item?.contentUri)
//                                    )
//                                }
//
//                            val bottomDialog = ConverterBottomSheet()
//                            bottomDialog.setCancelable(false)
//                            val bundle = Bundle()
//                            bundle.putString("videoPath", videoPath)
//                            bundle.putString("audioPath", outputAudioFilePath)
//                            bundle.putString("title", title)
//                            bottomDialog.arguments = bundle
//                            // bundle.putSerializable("video", item)
//                            if (isAdded && !requireActivity().isFinishing) {
//                                bottomDialog.show(
//                                    requireActivity().supportFragmentManager,
//                                    bottomDialog.tag
//                                )
//                            }
//                            Log.d("videoPath", "$videoPath")
//                        }
//                    }
//                    1->{
//                        if (!inFavourite){//insert
//                            try {
//                                item.let {
//                                    if (it !=null){
//                                        val videoPlaylist = VideoEntityPlayList(
//                                            id=it.id,contentUri = it.contentUri.toString(), title = it.title, duration = it.duration, date = it.date, size = it.size, orignalpath = it.orignalpath
//                                          , playlist_id = 1
//                                        )
//                                        mViewModel.insertVideoToFavourites(videoPlaylist)
//                                    }
//                                }
//                            }catch (e: SQLiteConstraintException){
//                                e.printStackTrace()
//                            }
//                            catch (e:Exception){
//                                e.printStackTrace()
//                            }
//                        }else{//remove
//                            mViewModel.removeVideoFromFavourite(item.id)
//                        }
//                    }
//                    2 -> {
//                        val bs = CreateNewPlaylistBottomsheet()
//                        val bundle = Bundle()
//                        bundle.putSerializable("video", item)
//                        bs.arguments = bundle
//                        bs.show(parentFragmentManager, "")
//                    }
//                    3 -> {
//                        mActivity?.let {
//                            item.contentUri?.let {uri ->
//                                VideoCutterUtils.openTrimActivity(uri.toString(), it)
//                            }
//                        }
//                    }
                    0 -> {
                        optionsItem = item
                        val activity = mActivity ?: return
                        lifecycleScope.launch {
                            tempTitle = item.title.orEmpty()
                            deleteVideoPermanently(resolveVideoUri(item), activity)
                        }
                    }
                    1 -> {
                        shareVideo(item)
                    }
                    2 -> {
                        optionsItem = item.copy(contentUri = resolveVideoUri(item).toString())
                        showRenameDialogue(optionsItem!!)
                    }
                    3 -> {
                        val video=Video(item.id,null,item.title,item.duration,item.date,item.size,item.orignalpath,item.isChecked)
                        val bottomDialog = VideoInfoBottomSheetFragment()
                        val bundle = Bundle()
                        bundle.putSerializable("video",video)
                        bundle.putString("uri",item.contentUri.toString())
                        bottomDialog.arguments=bundle
                        bottomDialog.show(parentFragmentManager, "video_info")
                    }
                }
            }
        })
        dg.show(parentFragmentManager, "video_options")
    }

    private fun showRenameDialogue(item: Video) {
        mActivity?.let {activity->
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.rename_dailog, null)
            val btnCancel = view.findViewById<TextView>(R.id.tvCancell)
            val btnOk = view.findViewById<TextView>(R.id.tvOk)
            val editText = view.findViewById<EditText>(R.id.etName)
            editText.setText(item.title)
            val builder = android.app.AlertDialog.Builder(mActivity)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set the width to 80% of the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams
            btnOk.setOnClickListener {
                val newName = editText.text.toString()
                Log.d(
                    VIDEO_ACTIONS_TAG,
                    "rename confirmed: id=${item.id} uri=${item.contentUri} old=${item.title} new=$newName"
                )
                lifecycleScope.launch {
                    mViewModel.renameVideo(activity,item,newName)
                }
                alertDialog.dismiss()
            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.show()

        }
    }

    private fun shareVideo(item: Video) {
        mActivity?.let {
            val videoUri = resolveVideoUri(item)
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = it.contentResolver.getType(videoUri) ?: "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)
                shareIntent.clipData = ClipData.newRawUri("video", videoUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ContextCompat.startActivity(it, Intent.createChooser(shareIntent, "Share video"), null)
            } catch (e: Exception) {
                Log.e("SearchVideoMenu", "Unable to share $videoUri", e)
                Toast.makeText(context, "Unable to share this video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveVideoUri(item: Video): Uri {
        val resolved = item.contentUri
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?: ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                item.id
            )
        Log.d(VIDEO_ACTIONS_TAG, "resolved uri: id=${item.id} uri=$resolved")
        return resolved
    }

    private fun deleteVideoPermanently(uri: Uri, context: Context) {

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q){
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.alert_dailog, null)

            view.findViewById<TextView>(R.id.alertMessage).text =
                getString(R.string.do_you_want_to_delete_this)
            val icon = view.findViewById<ImageView>(R.id.alertImage)
            val btnCancel = view.findViewById<Button>(R.id.btnCancel)
            val btnOk = view.findViewById<Button>(R.id.btnOk)
            btnOk.text = getString(R.string.delete)
            icon.setImageResource(R.drawable.ci_delete_p)
            val builder = android.app.AlertDialog.Builder(mActivity)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            btnOk.setOnClickListener {
                lifecycleScope.launch {
                    mViewModel.performDeleteImage(uri, context)
                }
                alertDialog.dismiss()
            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.show()
        }else{
            lifecycleScope.launch {
                mViewModel.performDeleteImage(uri, context)
            }
        }

    }

    override fun onWatchVideoClick() {
        mActivity?.let {
            if (mActivity?.let { NetworkUtils.isOnline(it) } == true) {
                val scope = CoroutineScope(Dispatchers.Main + Job())
                scope.launch {
                    val maxAttempts = 4
                    var attempts = 0

                    var adShown = false

                    while (attempts < maxAttempts && !adShown) {
                        attempts++

                        mActivity?.let { activity ->

                            when(AdsManager.adSdkChoice){
                                "admob"->{
                                    // Check if the ad is already loaded
                                    if (AdsManager.rewardedAd != null) {
                                        AdsManager.showRewardedVideo(
                                            requireContext(),
                                            activity,
                                            this@SearchVideoFragment,
                                        )
                                        adShown = true
                                    } else {
                                        mActivity?.let { AdsManager.loadRewardedAd(it) }
                                    }
                                }
                                "applovin"->{
                                    AdsManager.showRewardedVideoAppLovin(
                                        context = requireContext(),
                                        activity = requireActivity(),
                                        onUserEarnedRewardListener = {
                                            // Reward the user
                                            Log.d("AppLovin", "User earned reward!")
                                        },
                                        rewardAdDismissListener = {
                                            // Handle ad dismissal
                                            Log.d("AppLovin", "Ad dismissed!")
                                        }
                                    )
                                }
                            }
                            if (!adShown) {
                                delay(4000)
                            }
                        }
                    }
                    if (!adShown) {
                        //   binding.progressRewarded.visibility = View.GONE
                        Toast.makeText(
                            mActivity,
                            getString(R.string.failed_to_show_ad_please_try_again),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } else {
                Toast.makeText(
                    mActivity,
                    getString(R.string.please_turn_your_internet_on),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }


        mActivity?.let {
            when(AdsManager.adSdkChoice){
                "admob"-> {
                    AdsManager.showRewardedVideoforMp3(
                        requireContext(),
                        it,
                        this, this
                    )
                }
                "applovin"-> {
                    AdsManager.showRewardedVideoforMp3AppLovin(
                        context = requireContext(),
                        activity = requireActivity(),
                        onUserEarnedRewardListener = {
                            // Reward the user
                        },
                        adDismissedListener = {
                            // Handle ad dismissal
                        }
                    )
                }
            }
            /*   AdsManager.showRewardedVideoforMp3(
                   requireContext(),
                   it,
                   this,this
               )*/
        }
    }

    override fun onUnlockThemeClick() {
        mActivity?.let {
            if (!NetworkUtils.isOnline(it)) {
                ToastUtils.showToast(requireContext(), "Internet connection error")
            }else{
                AppUtils.getMain(it).hideBannerAd()
                AppUtils.getMain(it).navController?.navigate(R.id.propanel)
            }
        }
    }

    override fun onUserEarnedReward(p0: RewardItem) {
        isuserearned=true
    }

    override fun onAdDismissed() {
        if (isuserearned){
//            isuserearned=false
//            val outputDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZM_mp3")
//            } else {
//                File(Environment.getExternalStorageDirectory(), "ZM_mp3")
//            }
//            if (!outputDir.exists()) {
//                outputDir.mkdirs()
//            }
//            val title = selectedVideo?.title
//            val outputAudioFilePath = File(outputDir, "$title.mp3").path
//            val videoPath = AppUtils.getPathFromUri(mActivity!!, Uri.parse(selectedVideo?.contentUri))
//            val bottomDialog = ConverterBottomSheet()
//            bottomDialog.setCancelable(false)
//            val bundle = Bundle()
//            bundle.putString("videoPath",videoPath)
//            bundle.putString("audioPath",outputAudioFilePath)
//            bundle.putString("title",title)
//            bottomDialog.arguments = bundle
//            if (isAdded && !requireActivity().isFinishing){
//                bottomDialog.show(requireActivity().supportFragmentManager, bottomDialog.tag)
//            }
        }
        isSplash =false
    }

}
