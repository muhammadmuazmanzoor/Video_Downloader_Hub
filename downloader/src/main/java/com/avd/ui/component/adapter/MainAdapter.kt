package com.avd.ui.component.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avd.util.fragment.FragmentFactory

class MainAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle,
    private val fragmentFactory: FragmentFactory,
    val fromBrowser:Boolean=false
) : FragmentStateAdapter(fm, lifecycle) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> if(fromBrowser) fragmentFactory.createNewBrowserFragment() else fragmentFactory.createBrowserFragment()
            else -> fragmentFactory.createProgressFragment()
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}