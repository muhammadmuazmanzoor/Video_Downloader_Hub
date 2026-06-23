package com.video.avd.ui.status_saver

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.R
import com.video.avd.constent.GRID_ITEM_SPAN_COUNT
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentStatusVideosBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.status_saver.CommonStatusUtils.getVideoLengthAsString
import com.video.avd.ui.status_saver.model.Status
import com.video.avd.ui.status_saver.statusnew.StatusHomeFragment
import com.video.avd.ui.videos.model.Video
import com.video.avd.ads.AppOpenManager
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecentFragment : Fragment(), StatusVidAdapterNew.StatusVideoClickListener {
    private val viewModel: StatusViewModel by activityViewModels()
    var binding: FragmentStatusVideosBinding? = null
    private var mActivity: FragmentActivity? = null
    var adapter: StatusVidAdapterNew? = null

    private var isBusiness: Boolean = false

    private var hasData = false

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
        binding = FragmentStatusVideosBinding.inflate(inflater, container, false)
        setAdapter()
        clickListeners()
        initViews()
        prepareNavigation()
        checkBelowAndroidPie()
        return binding?.root
    }


    private fun setAdapter() {
        adapter = binding?.recyclerViewVideo?.let {
            StatusVidAdapterNew(requireContext(),it, emptyList(), this)
        }
        binding?.recyclerViewVideo?.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
            adapter = this@RecentFragment.adapter
        }
        binding?.apply {
            btnClearSelection.setOnClickListener {
                adapter?.clearSelection()
                batch.visibility = View.GONE
            }

            // Show the Clear Selection and Save Batch buttons when selection mode is enabled
            adapter?.setOnItemLongClickListener {
                if (adapter?.getSelectedItems()?.isNotEmpty() == true) {
                    batch.visibility = View.VISIBLE
                } else {
                    batch.visibility = View.GONE
                }
            }

            // Set up Save Batch button to save selected items
            btnSaveBatch.setOnClickListener {
                val selectedItems = adapter?.getSelectedItems()
                lifecycleScope.launch {
                    if (selectedItems?.isNotEmpty() == true) {
                        CommonStatusUtils.copyFiles(selectedItems, requireActivity())
                    }
                    adapter?.clearSelection()
                    batch.visibility = View.GONE
                }
            }

        }

    }

    private fun clickListeners() {
        mActivity?.let { activity ->
            binding?.btnOpenWhatsapp?.setOnClickListener {
                openWhatsApp(requireContext())
            }
//            binding?.clHowDownload?.setOnClickListener {
//                StatusHomeFragment.howToDownloadClicked.value = true
//            }
            binding?.back?.setOnClickListener { findNavController().popBackStack() }

            binding?.permission?.setOnClickListener {
                AppOpenManager.isShowingAd = true
//                isSplash = true
                Log.d("isssss", isBusiness.toString())
                val mIntent = Intent(requireActivity(), StatusActivity::class.java)
                mIntent.putExtra("isBusiness", isBusiness)

                requireActivity().startActivity(
                    mIntent
                )
            }
        }

    }

    private fun initViews() {
//        binding?.appbar?.visibility =
//            if (ToolsFragment.showToolbarInStatusVidFragment) View.VISIBLE else View.GONE
    }

    private fun prepareNavigation() {
        mActivity?.let { activity ->
            if (isBusiness) {
                if (AppPreference.isPermissionGrantedForStatusBusiness(activity)) {
                    binding?.permission?.visibility = View.GONE
                    binding?.detail?.visibility = View.GONE
                    binding?.folderImage?.visibility = View.GONE
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        binding?.permission?.visibility = View.VISIBLE

                    }
                }
            } else {
                if (AppPreference.isPermissionGrantedForStatus(activity)) {
                    binding?.permission?.visibility = View.GONE
                    binding?.detail?.visibility = View.GONE
                    binding?.folderImage?.visibility = View.GONE
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        binding?.permission?.visibility = View.VISIBLE
                    }
                }
            }
            checkIfData(activity)
        }
    }

    private fun checkBelowAndroidPie() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            binding?.permission?.visibility = View.GONE
            binding?.detail?.visibility = View.GONE
            binding?.folderImage?.visibility = View.GONE

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated whatsapp recent fragment", "RecentFragment")
        mActivity?.let { activity ->
            if (activity is MainActivity) {
                AppUtils.getMain(activity).hidebottombar()
            }
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            observers(activity)
        }
    }

    private fun observers(activity: FragmentActivity) {

        StatusHomeFragment.isBusinessWhatsapp.observe(viewLifecycleOwner) {
            if (it) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viewModel.clearData()
                    isBusiness = true
                    if (AppPreference.isPermissionGrantedForStatusBusiness(activity)) {
                        viewModel.getStatus(activity, true)
                    }
                    prepareNavigation()
                } else {
                    viewModel.clearData()
                    isBusiness = true
                    viewModel.getStatus(activity, true)
                    prepareNavigation()
                }

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    viewModel.clearData()
                    isBusiness = false
                    if (AppPreference.isPermissionGrantedForStatus(activity)) {
                        viewModel.getStatus(activity, false)
                    }
                    prepareNavigation()
                } else {
                    viewModel.clearData()
                    isBusiness = false
                    viewModel.getStatus(activity, false)
                    prepareNavigation()
                }

            }
        }

        viewModel.videoList.observe(viewLifecycleOwner) { statusList ->
            if (!statusList.isNullOrEmpty() && statusList.isNotEmpty()) {
                viewModel.updateHasData(true)


                adapter?.notifyItemRangeChanged(0, statusList.size)
                binding?.prgressBarVideo?.visibility = View.GONE
                binding?.messageTextVideo?.visibility = View.GONE

                val uniqueList = ArrayList<Status>()
                if (isBusiness) {
                    for (item in statusList) {
                        if (item.isBusiness) {
                            uniqueList.add(item)
                        }
                    }
                } else {
                    for (item in statusList) {
                        if (!item.isBusiness) {
                            uniqueList.add(item)
                        }
                    }
                }

                adapter?.updateData(uniqueList, isBusiness)


            } else {
                viewModel.updateHasData(false)
                if (isBusiness) {
                    if (AppPreference.isPermissionGrantedForStatusBusiness(activity)) {
                        binding?.messageTextVideo?.visibility = View.GONE
                        binding?.permission?.visibility = View.GONE
                        binding?.detail?.visibility = View.GONE
                        binding?.folderImage?.visibility = View.GONE
                    }
                } else {
                    if (AppPreference.isPermissionGrantedForStatus(activity)) {
                        binding?.messageTextVideo?.visibility = View.GONE
                        binding?.permission?.visibility = View.GONE
                        binding?.detail?.visibility = View.GONE
                        binding?.folderImage?.visibility = View.GONE
                    }
                }
                binding?.messageTextVideo?.setText(R.string.cant_find_whatsapp_dir)
                adapter?.updateData(emptyList(), isBusiness)
            }
        }

        viewModel.hasData.observe(viewLifecycleOwner) { hasData ->
            if (!hasData) {
                this.hasData = false
                checkIfData(activity)
            } else {
                this.hasData = true
                checkIfData(activity)
            }
        }

        try {
            VIEW_TYPE.observe(viewLifecycleOwner) {
                binding?.recyclerViewVideo?.layoutManager =
                    GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    private fun checkIfData(activity: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasData) {
                if (isBusiness) {
                    if (AppPreference.isPermissionGrantedForStatusBusiness(activity)) {
                        binding?.btnOpenWhatsapp?.visibility = View.VISIBLE
                        //binding?.clHowDownload?.visibility = View.VISIBLE
                        binding?.tvWatchVideo?.visibility = View.VISIBLE
                        binding?.folderImage1?.visibility = View.VISIBLE
                    } else {
                        binding?.btnOpenWhatsapp?.visibility = View.GONE
                        //binding?.clHowDownload?.visibility = View.GONE
                        binding?.tvWatchVideo?.visibility = View.GONE
                        binding?.folderImage1?.visibility = View.GONE
                    }
                } else {
                    if (AppPreference.isPermissionGrantedForStatus(activity)) {
                        binding?.btnOpenWhatsapp?.visibility = View.VISIBLE
                        //binding?.clHowDownload?.visibility = View.VISIBLE
                        binding?.tvWatchVideo?.visibility = View.VISIBLE
                        binding?.folderImage1?.visibility = View.VISIBLE
                    } else {
                        binding?.btnOpenWhatsapp?.visibility = View.GONE
                        //binding?.clHowDownload?.visibility = View.GONE
                        binding?.tvWatchVideo?.visibility = View.GONE
                        binding?.folderImage1?.visibility = View.GONE
                    }
                }
            } else {
                binding?.btnOpenWhatsapp?.visibility = View.GONE
                //binding?.clHowDownload?.visibility = View.GONE
                binding?.tvWatchVideo?.visibility = View.GONE
                binding?.folderImage1?.visibility = View.GONE
            }
        } else {
            if (!hasData) {
                binding?.btnOpenWhatsapp?.visibility = View.VISIBLE
                //binding?.clHowDownload?.visibility = View.VISIBLE
                binding?.tvWatchVideo?.visibility = View.VISIBLE
                binding?.folderImage1?.visibility = View.VISIBLE

            } else {
                binding?.btnOpenWhatsapp?.visibility = View.GONE
                //binding?.clHowDownload?.visibility = View.GONE
                binding?.tvWatchVideo?.visibility = View.GONE
                binding?.folderImage1?.visibility = View.GONE
            }
        }

    }

    override fun onResume() {
        try {
            mActivity?.let { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    if (isBusiness) {
                        if (AppPreference.isPermissionGrantedForStatusBusiness(activity)) {
                            viewModel.getStatus(activity, true)
                            binding?.permission?.visibility = View.GONE
                            binding?.detail?.visibility = View.GONE
                            binding?.folderImage?.visibility = View.GONE
                        }
                    } else {
                        if (AppPreference.isPermissionGrantedForStatus(activity)) {
                            viewModel.getStatus(activity, false)
                            binding?.permission?.visibility = View.GONE
                            binding?.detail?.visibility = View.GONE
                            binding?.folderImage?.visibility = View.GONE
                        }
                    }
                } else {
                    viewModel.getStatus(activity, isBusiness)
                }


            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onResume()
    }


    override fun onDestroyView() {
        binding = null
        if (mActivity is MainActivity){
            AppUtils.getMain(mActivity).hidebottombar()
        }
        super.onDestroyView()
    }


    private fun openWhatsApp(context: Context) {
        var whats = "Whatsapp"
        var pk = "com.whatsapp"
        try {
            if (isBusiness) {
                pk = "com.whatsapp.w4b"
                whats = "Whatsapp Business"
            }
            // Check if WhatsApp is installed
            context.packageManager.getPackageInfo(pk, PackageManager.GET_ACTIVITIES)
            // Intent to launch WhatsApp
            val intent = context.packageManager.getLaunchIntentForPackage(pk)
            if (intent != null) {
                context.startActivity(intent)
            } else {
                // If WhatsApp is not installed, display a toast
                Toast.makeText(
                    context,
                    "$whats is not installed on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Catch exception if WhatsApp is not installed
            Toast.makeText(context, "$whats is not installed on this device.", Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            // Catch any other exceptions
            Toast.makeText(context, "Failed to open $whats", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStatusVideoClick(list: List<Status>, position: Int, status: Status) {
        mActivity?.let {
            showInterstitialHome(activity = it) {
                videoclick(list,position,status)
            }
        }
        /*if (AdsManager.mInterstitialAdHigh != null){
            mActivity?.let {
               // AdsManager.showAppInterstitialAdHigh(it,"STATUS_VID_SHOW"){ videoclick(list,position,status) }
            }
        }else{
            mActivity?.let {
               // AdsManager.showAppInterstitialAd(it,"STATUS_VID_SHOW"){ videoclick(list,position,status) }
            }
        }*/
    }

    fun videoclick(list: List<Status>, position: Int, status: Status){
        lifecycleScope.launchWhenStarted {
            try {
                AppUtils.firebaseUserAction("onStatusVideoClick_StatusFragment", "StatusFragment")
                val newList = ArrayList<Video>()
                for (item in list) {
                    val video = Video()
                    var durationsdkHandled: String? = "00:00"
                    video.id = position.toLong()
                    if (item.isApi30) {
                        video.contentUri = item.documentFile.uri.toString()
                    } else {
                        video.contentUri = Uri.fromFile(item.file).toString()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        video.title = item.documentFile.name
                        val uri = item.documentFile.uri
                        durationsdkHandled = mActivity?.let { getVideoLengthAsString(uri, it) }
                    } else {
                        video.title = item.title
                        durationsdkHandled = getVideoLengthAsString(item.path)
                    }
                    if (durationsdkHandled != null) {
                        video.duration = durationsdkHandled
                    }
                    video.date = "0"
                    video.size = "0"
                    newList.add(video)
                }
                viewModel.status = status
                videolistglobal = newList
                Log.d("app", "size is " + newList.size)
                val result = Bundle()
                result.putString("id", position.toString())
                result.putBoolean("isliveuri", false)
                result.putString("fragmentName", "Status")
                result.putString("uri", "")
                mActivity?.let {
                    if (it is MainActivity){
                        AppUtils.getMain(it).hidebottombar()
                    }
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
            } catch (e: Exception) {
                mActivity?.let { ToastUtils.showToast(it, "Video is corrupted") }
            }
        }
    }

    override fun onsaveClick(status: Status, context: Context) {
        AppUtils.firebaseUserAction("oSavedClicked", "RecentFragment")
        lifecycleScope.launch {
            status.let { context.let { it1 -> CommonStatusUtils.copyFile(it, it1) } }
        }
    }

}