package com.avd.ui.main.downloder_queue.ui.main

import android.content.Context
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.avd.R
import com.avd.databinding.FragmentDownloadQueueBinding
import com.avd.ui.dialog.DownloadDialogManager
import com.avd.ui.main.downloder_queue.utils.PermissionManagerNew
import com.avd.util.AdBlockerHelper.showExitScreen
import com.avd.util.DownloaderModuleNavigator


class FragmentDownloadQueue : Fragment() {

    private var mActivity: FragmentActivity? = null
    private lateinit var binding: FragmentDownloadQueueBinding

    private var pagerAdapter: DownloadListPagerAdapter? = null
    private var permissionManager: PermissionManagerNew? = null


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
        mActivity?.let { activity ->
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


            initLayout(activity)

            binding.waBack.setOnClickListener {
                getActivity()?.onBackPressed()
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

    private fun initLayout(activity: FragmentActivity) {
        pagerAdapter = DownloadListPagerAdapter(activity)

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
    override fun onResume() {
        super.onResume()
        (activity as? com.avd.util.CommunicateWithActivity)?.showBottomBar()

        if (!permissionManager?.areAllPermissionsGranted()!!) {
            permissionManager?.requestAllPermissions(this)
        }
        try {
            activity?.onBackPressedDispatcher?.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback
            )
        } catch (e: Exception) {
            e.printStackTrace()
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
        fun newInstance() = FragmentDownloadQueue()
        var lastTab:Int=0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("FragmentDownloadQueue", "onDestroyView")
        lastTab=0
        //DownloadDialogManager.defaultTabPos = DownloadListPagerAdapter.QUEUED_FRAG_POS
    }

    override fun onPause() {
        super.onPause()
        Log.d("FragmentDownloadQueue", "onPause")

//        DownloadDialogManager.defaultTabPos = DownloadListPagerAdapter.QUEUED_FRAG_POS
    }
}
