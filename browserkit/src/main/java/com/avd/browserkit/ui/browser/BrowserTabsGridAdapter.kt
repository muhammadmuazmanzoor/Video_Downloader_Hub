package com.avd.browserkit.ui.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.avd.browserkit.R
import com.avd.browserkit.databinding.ItemBrowserTabBinding
import com.google.android.material.card.MaterialCardView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class BrowserTabsGridAdapter(
    private val onSelect: (Int) -> Unit,
    private val onClose: (Int) -> Unit,
    private val previewProvider: (String) -> Bitmap?,
) : RecyclerView.Adapter<BrowserTabsGridAdapter.Holder>() {

    private var tabs: List<BrowserTab> = emptyList()
    private var selectedIndex: Int = 0
    private val executor = Executors.newFixedThreadPool(2)

    fun submit(tabs: List<BrowserTab>, selectedIndex: Int) {
        this.tabs = tabs
        this.selectedIndex = selectedIndex
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val themedContext = ContextThemeWrapper(parent.context, R.style.Theme_BrowserKit)
        val binding = ItemBrowserTabBinding.inflate(LayoutInflater.from(themedContext), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(tabs[position], position, position == selectedIndex)
    }

    override fun getItemCount(): Int = tabs.size

    inner class Holder(private val binding: ItemBrowserTabBinding) : RecyclerView.ViewHolder(binding.root) {
        private var boundTabId: String? = null

        fun bind(tab: BrowserTab, index: Int, selected: Boolean) {
            boundTabId = tab.id
            val host = displayHost(tab.url)
            val title = tab.title.takeIf { it.isNotBlank() && it != "New tab" } ?: host.ifBlank { "New tab" }

            binding.tvTabTitle.text = title
            binding.tvPreviewTitle.text = title
            binding.tvTabUrl.text = host.ifBlank { tab.url.takeIf { it != "about:blank" }.orEmpty() }

            val preview = previewProvider(tab.id)
            if (preview != null && !preview.isRecycled) {
                binding.ivPreview.setImageBitmap(preview)
                binding.ivPreview.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            } else {
                binding.ivPreview.setImageResource(0)
                binding.ivPreview.setBackgroundResource(R.drawable.bk_bg_tab_preview)
            }

            val card = binding.tabCard
            if (selected) {
                card.strokeColor = ContextCompat.getColor(binding.root.context, R.color.bk_accent)
                card.strokeWidth = dp(card, 2)
            } else {
                card.strokeColor = android.graphics.Color.TRANSPARENT
                card.strokeWidth = 0
            }

            binding.root.setOnClickListener { onSelect(index) }
            binding.btnCloseTab.setOnClickListener { onClose(index) }
            binding.btnCloseTab.isVisible = true

            loadFavicon(host, tab.id)
        }

        private fun loadFavicon(host: String, tabId: String) {
            binding.ivFavicon.setImageResource(0)
            binding.ivFaviconSmall.setImageResource(0)
            if (host.isBlank()) return
            val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=64"
            executor.execute {
                val bitmap = runCatching {
                    val conn = (URL(faviconUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        instanceFollowRedirects = true
                    }
                    conn.inputStream.use { BitmapFactory.decodeStream(it) }
                }.getOrNull() ?: return@execute
                binding.root.post {
                    if (boundTabId == tabId) {
                        binding.ivFavicon.setImageBitmap(bitmap)
                        binding.ivFaviconSmall.setImageBitmap(bitmap)
                    }
                }
            }
        }

        private fun displayHost(url: String): String {
            if (url.isBlank() || url == "about:blank") return ""
            return runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
        }

        private fun dp(view: MaterialCardView, value: Int): Int {
            return (value * view.resources.displayMetrics.density).toInt()
        }
    }
}
