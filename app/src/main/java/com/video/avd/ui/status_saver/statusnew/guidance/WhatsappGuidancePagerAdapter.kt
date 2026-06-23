package com.video.avd.ui.status_saver.statusnew.guidance

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class WhatsappGuidancePagerAdapter(
    fragmentManager: FragmentManager, lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragmentList = arrayListOf<Fragment>()

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WhatsappGuidanceFragment(position)
            1 -> WhatsappGuidanceFragment(position)
            2 -> WhatsappGuidanceFragment(position)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }
}