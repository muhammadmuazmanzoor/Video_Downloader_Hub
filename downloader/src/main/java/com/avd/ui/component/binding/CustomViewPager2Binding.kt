package com.avd.ui.component.binding

import androidx.databinding.BindingAdapter
import com.avd.ui.main.home.browser.BrowserFragment
import com.avd.ui.main.home.browser.CustomViewPager2
import com.avd.ui.main.home.browser.webTab.NewBrowserFragment
import com.avd.ui.main.home.browser.webTab.WebTab

object CustomViewPager2Binding {


    @BindingAdapter("app:items")
    @JvmStatic
    fun CustomViewPager2.setWebItems(currentItems: List<WebTab>?) {
        with(adapter as BrowserFragment.TabsFragmentStateAdapter?) {
            this?.setRoutes(currentItems ?: emptyList())
        }
    }
    @BindingAdapter("app:borwseitems")
    @JvmStatic
    fun CustomViewPager2.setWebBorwseItems(currentItems: List<WebTab>?) {
        with(adapter as NewBrowserFragment.TabFragmentStateAdapter?) {
            this?.setRoutes(currentItems ?: emptyList())
        }
    }

    @BindingAdapter("app:offScreenPageLimit")
    @JvmStatic
    fun CustomViewPager2.setOffScreenPageLimit(pageLimit: Int) {
        offscreenPageLimit = pageLimit
    }

    @BindingAdapter("app:currentItem")
    @JvmStatic
    fun CustomViewPager2.setCurrentItem(currentItemPosition: Int) {
        currentItem = currentItemPosition
    }
}
