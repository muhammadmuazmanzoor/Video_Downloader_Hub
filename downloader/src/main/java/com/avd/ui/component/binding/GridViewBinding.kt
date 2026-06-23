package com.avd.ui.component.binding

import android.widget.GridView
import androidx.databinding.BindingAdapter
import com.avd.data.local.room.entity.PageInfo
import com.avd.ui.component.adapter.*

object GridViewBinding {
    @BindingAdapter("app:items")
    @JvmStatic
    fun GridView.setTopPages(items: List<PageInfo>) {
        with(adapter as TopPageAdapter?) {
            this?.let { setData(items) }
        }
    }
}