package com.avd.ui.component.binding

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.avd.data.local.model.LocalVideo
import com.avd.data.local.model.Proxy
import com.avd.data.local.model.Suggestion
import com.avd.data.local.room.entity.HistoryItem
import com.avd.data.local.room.entity.PageInfo
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.data.local.room.entity.VideoInfo
import com.avd.ui.component.adapter.*
import com.avd.ui.main.home.bottomsheet.adapter.TabHistory
import com.avd.ui.main.home.browser.webTab.WebTab

object RecyclerViewBinding {
    @BindingAdapter("app:itemsbottom")
    @JvmStatic
    fun RecyclerView.setWebTabsbottom(tabs: List<WebTab>) {
        with(adapter as TabHistory?) {
            this?.let { updateTab(tabs) }
        }
    }


    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setWebTabs(tabs: List<WebTab>) {
        with(adapter as WebTabsAdapter?) {
            this?.let { setData(tabs) }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setTopPages(items: List<PageInfo>) {
        with(adapter as TopPageAdapter?) {
            this?.let { setData(items) }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setSuggestions(items: List<Suggestion>) {
        with(adapter as SuggestionAdapter?) {
            this?.let { setData(items) }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setProgressInfos(items: List<ProgressInfo>) {
        with(adapter as ProgressAdapter?) {
            this?.let { setData(items) }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setProxiesList(items: List<Proxy>) {
        with(adapter as ProxiesAdapter?) {
            this?.let { setData(items) }
        }
    }

    @BindingAdapter("items")
    @JvmStatic
    fun RecyclerView.setVideoInfos(items: List<LocalVideo>?) {
        val safeItems = items.orEmpty()

        (adapter as? VideoAdapter)?.setData(safeItems)
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.historyItems(items: List<HistoryItem>) {

        if (adapter is HistoryAdapter?) {
            with(adapter as HistoryAdapter?) {
                this?.let { setData(items) }
            }
        }
        if (adapter is HistorySearchAdapter?) {
            with(adapter as HistorySearchAdapter?) {
                this?.let { setData(items) }
            }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setDetectedVideoInfos(items: List<VideoInfo>) {
        with(adapter as VideoInfoAdapter?) {
            this?.let { setData(items) }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun RecyclerView.setDetectedVideoInfosSet(items: Set<VideoInfo>?) {
       /* val videoAdapter = adapter as? VideoInfoAdapter ?: return
        videoAdapter.setData(items?.toList() ?: emptyList())*/

        val videoAdapter = adapter as? VideoInfoAdapter ?: return
        val safeList = items?.toList() ?: emptyList()
        if (isAttachedToWindow) {
            videoAdapter.setData(safeList)
        } else {
            post { videoAdapter.setData(safeList) }
        }
    }
}