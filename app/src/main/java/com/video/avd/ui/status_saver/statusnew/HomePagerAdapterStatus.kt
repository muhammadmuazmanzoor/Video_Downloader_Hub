package com.video.avd.ui.status_saver.statusnew

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.video.avd.ui.status_saver.RecentFragment

class HomePagerAdapterStatus(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager,lifecycle) {

    private val fragmentList = arrayListOf<Fragment>()

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RecentFragment()
            1 -> SavedFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }

}