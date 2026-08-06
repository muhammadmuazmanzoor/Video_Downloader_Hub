package com.avd.ui.main.downloder_queue.ui.main

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.avd.R
import com.avd.databinding.FragmentDownloadQueueBinding
import com.avd.ui.dialog.DownloadDialogManager
import com.avd.ui.dialog.DownloadCompletionBroadcast
import com.avd.ui.main.downloder_queue.utils.PermissionManagerNew
import com.avd.ui.main.progress.ProgressViewModel
import com.avd.ui.main.video.VideoViewModel
import com.avd.util.AdBlockerHelper.showExitScreen
import com.avd.util.DownloaderModuleNavigator


class FragmentDownloadQueue : Fragment() {

    private var mActivity: FragmentActivity? = null
    private lateinit var binding: FragmentDownloadQueueBinding
    private val progressViewModel: ProgressViewModel by activityViewModels()
    private val videoViewModel: VideoViewModel by activityViewModels()

    private var pagerAdapter: DownloadListPagerAdapter? = null
    private var permissionManager: PermissionManagerNew? = null
    private var hasRequestedPermissionsForView = false
    private var downloadCompletedReceiver: BroadcastReceiver? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionManager = PermissionManagerNew(requireContext(), requireActivity(),
            object : PermissionManagerNew.Callback {
                override fun onStorageResult(isGranted: Boolean) {
                    Log.d("Permissions", "Storage granted=$isGranted")
                }

                override fun onNotificationResult(isGranted: Boolean) {
                    Log.d("Permissions", "Notification granted=$isGranted")
                }

                override fun onForegroundServiceResult(isGranted: Boolean) {
                    Log.d("Permissions", "Foreground Service granted=$isGranted")
                }
            }
        )

        initLayout()

        binding.waBack.setOnClickListener {
            getActivity()?.onBackPressed()
        }

        progressViewModel.downloadCompletedEvent.observe(viewLifecycleOwner) { downloadId ->
            refreshCompletedTab("progress event: $downloadId")
        }

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

    }

    override fun onStart() {
        super.onStart()
        registerDownloadCompletedReceiver()
    }

    override fun onStop() {
        unregisterDownloadCompletedReceiver()
        super.onStop()
    }

    private fun registerDownloadCompletedReceiver() {
        if (downloadCompletedReceiver != null) return

        downloadCompletedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != DownloadCompletionBroadcast.ACTION) return
                val downloadId = intent.getStringExtra(
                    DownloadCompletionBroadcast.EXTRA_DOWNLOAD_ID
                ).orEmpty()
                refreshCompletedTab("completion broadcast: $downloadId")
            }
        }

        ContextCompat.registerReceiver(
            requireContext(),
            downloadCompletedReceiver,
            IntentFilter(DownloadCompletionBroadcast.ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterDownloadCompletedReceiver() {
        downloadCompletedReceiver?.let { receiver ->
            context?.let { runCatching { it.unregisterReceiver(receiver) } }
        }
        downloadCompletedReceiver = null
    }

    private fun refreshCompletedTab(source: String) {
        Log.d("FragmentDownloadQueue", "Download completed via $source; refreshing completed list")
        videoViewModel.refreshCompletedDownloads {
            if (isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                    androidx.lifecycle.Lifecycle.State.STARTED
                )
            ) {
                switchToCompletedTab()
            }
        }
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

    // In Fragment, override onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionManagerNew.PERMISSION_REQUEST_CODE) {
            permissionManager?.handlePermissionsResult(this, permissions, grantResults)
        }
    }

    private fun initLayout() {
        pagerAdapter = DownloadListPagerAdapter(this, SHOW_LATEST_COMPLETED_DOWNLOADS_FIRST)

        binding.downloadListViewpager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = DownloadListPagerAdapter.NUM_FRAGMENTS
        }

        setupTabs()
    }

    private fun setupTabs() {
        TabLayoutMediator(
            binding.downloadListTabs,
            binding.downloadListViewpager
        ) { tab, position ->

            tab.customView = createTabView(
                title = when (position) {
                    DownloadListPagerAdapter.QUEUED_FRAG_POS -> "In Progress"
                    DownloadListPagerAdapter.COMPLETED_FRAG_POS -> "Completed"
                    else -> ""
                },
                iconRes = when (position) {
                    DownloadListPagerAdapter.QUEUED_FRAG_POS -> R.drawable.progress_new
                    DownloadListPagerAdapter.COMPLETED_FRAG_POS -> R.drawable.completed
                    else -> 0
                },
                isSelected = position == binding.downloadListViewpager.currentItem
            )

        }.attach()

        updateTabs(binding.downloadListViewpager.currentItem)

        binding.downloadListTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabs(tab.position)
                lastTab=tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabs(binding.downloadListTabs.selectedTabPosition)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }      

    private fun createTabView(
        title: String,
        iconRes: Int,
        isSelected: Boolean
    ): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_download_tab, null, false)

        val icon = view.findViewById<ImageView>(R.id.tabIcon)
        val text = view.findViewById<TextView>(R.id.tabText)

        text.text = title
        text.isAllCaps = false
        if (iconRes != 0) {
            icon.setImageResource(iconRes)
        }
        icon.apply{
            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.brand_text_primary else R.color.gray_text
                )
            )
        }
        text.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.brand_text_primary else R.color.gray_text
            )
        )
        view.isSelected = isSelected

        return view
    }

    private fun updateTabs(selectedPosition: Int) {
        for (i in 0 until binding.downloadListTabs.tabCount) {
            val tab = binding.downloadListTabs.getTabAt(i)
            val customView = tab?.customView ?: continue

            val icon = customView.findViewById<ImageView>(R.id.tabIcon)
            val text = customView.findViewById<TextView>(R.id.tabText)

            val selected = i == selectedPosition
            icon.apply{
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (selected) R.color.gSelector_light else R.color.gray_text
                    )
                )
            }
            text.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.gSelector_light else R.color.gray_text
                )
            )

            customView.isSelected = selected
        }
    }

    private fun switchToCompletedTab() {
        if (!isAdded || view == null) return
        if (binding.downloadListViewpager.adapter == null) return

        lastTab = DownloadListPagerAdapter.COMPLETED_FRAG_POS
        if (binding.downloadListViewpager.currentItem == DownloadListPagerAdapter.COMPLETED_FRAG_POS) {
            updateTabs(DownloadListPagerAdapter.COMPLETED_FRAG_POS)
            return
        }

        binding.downloadListViewpager.post {
            if (!isAdded || view == null) return@post
            binding.downloadListViewpager.setCurrentItem(
                DownloadListPagerAdapter.COMPLETED_FRAG_POS,
                true
            )
            updateTabs(DownloadListPagerAdapter.COMPLETED_FRAG_POS)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.avd.util.CommunicateWithActivity)?.showBottomBar()

        // Delay permission request to allow ads to fully dismiss
        if (!hasRequestedPermissionsForView && permissionManager?.areAllPermissionsGranted() == false) {
            hasRequestedPermissionsForView = true
            // Add delay to ensure ad has fully dismissed before requesting permissions
            binding.root.postDelayed({
                if (isAdded && permissionManager?.areAllPermissionsGranted() == false) {
                    Log.d("PermissionManager", "Requesting permissions after ad dismissal")
                    permissionManager?.requestAllPermissions(this)
                }
            }, 500) // 500ms delay to allow ad to dismiss
        }
        
        try {
            Log.d("FragmentDownloadQueue", "Resumed → switched to tab: $lastTab")
            if (binding.downloadListViewpager.adapter != null &&
                lastTab in 0 until (binding.downloadListViewpager.adapter?.itemCount ?: 0)) {

                // Only change if it's not already on the correct tab
                if (binding.downloadListViewpager.currentItem != lastTab) {
                    binding.downloadListViewpager.post {
                        binding.downloadListViewpager.setCurrentItem(lastTab, false)
                        android.util.Log.d("FragmentDownloadQueue", "Resumed → switched to tab: $lastTab")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FragmentDownloadQueue", "Error updating tab onResume: ${e.message}")
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    companion object {
        @Suppress("unused")
        private val TAG: String = FragmentDownloadQueue::class.java.simpleName
        private const val SHOW_LATEST_COMPLETED_DOWNLOADS_FIRST = true
        fun newInstance() = FragmentDownloadQueue()
        var lastTab:Int=0
    }

    override fun onDestroyView() {
        unregisterDownloadCompletedReceiver()
        super.onDestroyView()
        Log.d("FragmentDownloadQueue", "onDestroyView")
        lastTab=0
        pagerAdapter = null
        permissionManager = null
        hasRequestedPermissionsForView = false
        //DownloadDialogManager.defaultTabPos = DownloadListPagerAdapter.QUEUED_FRAG_POS
    }

    override fun onPause() {
        super.onPause()
        Log.d("FragmentDownloadQueue", "onPause")

//        DownloadDialogManager.defaultTabPos = DownloadListPagerAdapter.QUEUED_FRAG_POS
    }
}
