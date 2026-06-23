package com.video.avd.ui.video_downloader.guidance

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.video.avd.ui.download_guidance.DownloadGuidanceFragment


class DownloadGuidancePagerAdapter(
    fragmentManager: FragmentManager, lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragmentList = arrayListOf<Fragment>()

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DownloadGuidanceFragment(position)
            1 -> DownloadGuidanceFragment(position)
            2 -> DownloadGuidanceFragment(position)
            3 -> DownloadGuidanceFragment(position)

            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }
}