package com.avd.ui.component.binding

import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2
import com.avd.ui.main.home.browser.BrowserFragment
import com.avd.ui.main.home.browser.webTab.WebTab

object ViewPager2Binding {


    @BindingAdapter("items")
    @JvmStatic
    fun ViewPager2.setWebItems(currentItems: List<WebTab>?) {
        with(adapter as BrowserFragment.TabsFragmentStateAdapter?) {
            this?.setRoutes(currentItems ?: emptyList())
        }
    }

    @BindingAdapter("offScreenPageLimit")
    @JvmStatic
    fun ViewPager2.setOffScreenPageLimit(pageLimit: Int) {
        offscreenPageLimit = pageLimit
    }

    @BindingAdapter("currentItem")
    @JvmStatic
    fun ViewPager2.setCurrentItemBinding(currentItemPosition: Int?) {
        val target = currentItemPosition ?: return
        if (currentItem != target) {
            setCurrentItem(target, false)
        }
    }
}
