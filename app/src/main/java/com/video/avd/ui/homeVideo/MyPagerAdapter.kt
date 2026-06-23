package com.video.avd.ui.homeVideo

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.video.avd.ui.allvideo.AllVideoFragment
import com.video.avd.ui.folder.FolderFragment
import com.video.avd.ui.fragments.HistoryFragment

class MyPagerAdapter(private val context: Context, fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragmentList = arrayListOf<Fragment>()

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FolderFragment()
            1 -> AllVideoFragment()
            2 -> HistoryFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }

    fun getFragment(position:Int):Fragment{
        return  fragmentList[position]
    }
    
}