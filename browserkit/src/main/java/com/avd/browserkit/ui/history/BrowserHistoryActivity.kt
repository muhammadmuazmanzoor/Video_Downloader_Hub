package com.avd.browserkit.ui.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avd.browserkit.R
import com.avd.browserkit.api.BrowserKit
import com.avd.browserkit.data.BrowserHistoryEntity
import com.avd.browserkit.data.BrowserRepository
import com.avd.browserkit.databinding.ActivityBrowserHistoryBinding
import com.avd.browserkit.databinding.ItemBrowserHistoryEntryBinding
import com.avd.browserkit.databinding.ItemBrowserHistorySectionBinding
import com.avd.browserkit.ui.dialog.BrowserDialogBuilders
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BrowserHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserHistoryBinding
    private lateinit var repository: BrowserRepository
    private val adapter = BrowserHistoryAdapter(
        onOpen = { url ->
            if (intent.getBooleanExtra(EXTRA_OPEN_IN_BROWSER, false)) {
                // Standalone entry (e.g. from the app home screen) — no caller to hand the URL to.
                BrowserKit.launchUrl(this, url)
            } else {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(EXTRA_RESULT_URL, url),
                )
            }
            finish()
        },
        onRemove = { id ->
            lifecycleScope.launch {
                repository.deleteHistoryItem(id)
                loadHistory()
            }
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBrowserHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.getInsetsController(window, binding.root)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        repository = BrowserRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        binding.btnHistoryBack.setOnClickListener { finish() }
        binding.btnClearHistory.setOnClickListener { confirmClearHistory() }

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val items = repository.getHistory()
            val grouped = HistoryUiMapper.group(items, this@BrowserHistoryActivity)
            adapter.submit(grouped)
            val hasItems = grouped.any { it is HistoryListItem.Entry }
            binding.rvHistory.isVisible = hasItems
            binding.tvHistoryEmpty.isVisible = !hasItems
            binding.btnClearHistory.isEnabled = hasItems
        }
    }

    private fun confirmClearHistory() {
        BrowserDialogBuilders.create(this)
            .setMessage(R.string.bk_clear_history_confirm)
            .setPositiveButton(R.string.bk_clear_history) { _, _ ->
                lifecycleScope.launch {
                    repository.clearHistory()
                    loadHistory()
                }
            }
            .setNegativeButton(R.string.bk_cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_RESULT_URL = "extra_result_url"
        const val EXTRA_OPEN_IN_BROWSER = "extra_open_in_browser"

        fun intent(context: Context, openInBrowser: Boolean = false): Intent =
            Intent(context, BrowserHistoryActivity::class.java)
                .putExtra(EXTRA_OPEN_IN_BROWSER, openInBrowser)
    }
}

private sealed class HistoryListItem {
    data class Section(val label: String) : HistoryListItem()
    data class Entry(val item: BrowserHistoryEntity) : HistoryListItem()
}

private enum class HistoryIconType {
    WEBSITE,
    VIDEO,
    SETTINGS,
}

private object HistoryUiMapper {
    fun group(items: List<BrowserHistoryEntity>, context: Context): List<HistoryListItem> {
        if (items.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        val startOfYesterday = startOfToday - DAY_MS
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        val result = mutableListOf<HistoryListItem>()
        var currentSection: String? = null

        items.sortedByDescending { it.visitedAt }.forEach { item ->
            val section = when {
                item.visitedAt >= startOfToday -> context.getString(R.string.bk_history_today)
                item.visitedAt >= startOfYesterday -> context.getString(R.string.bk_history_yesterday)
                else -> {
                    calendar.timeInMillis = item.visitedAt
                    dateFormat.format(calendar.time)
                }
            }
            if (section != currentSection) {
                currentSection = section
                result.add(HistoryListItem.Section(section))
            }
            result.add(HistoryListItem.Entry(item))
        }
        return result
    }

    fun displayUrl(url: String): String {
        return runCatching {
            val uri = Uri.parse(url)
            val host = uri.host.orEmpty()
            val path = uri.path.orEmpty().trimStart('/')
            when {
                host.isBlank() -> url
                path.isBlank() -> host
                else -> "$host/$path"
            }
        }.getOrDefault(url)
    }

    fun iconTypeForUrl(url: String): HistoryIconType {
        val lower = url.lowercase(Locale.US)
        return when {
            lower.startsWith("chrome://") ||
                lower.startsWith("about:") ||
                lower.contains("settings") -> HistoryIconType.SETTINGS

            lower.contains("youtube") ||
                lower.contains("youtu.be") ||
                lower.contains("tiktok") ||
                lower.contains("instagram.com/reel") ||
                lower.contains("vimeo") ||
                lower.contains(".mp4") ||
                lower.contains("/video") -> HistoryIconType.VIDEO

            else -> HistoryIconType.WEBSITE
        }
    }

    private const val DAY_MS = 24L * 60L * 60L * 1000L
}

private class BrowserHistoryAdapter(
    private val onOpen: (String) -> Unit,
    private val onRemove: (Long) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<HistoryListItem> = emptyList()

    fun submit(newItems: List<HistoryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HistoryListItem.Section -> VIEW_SECTION
        is HistoryListItem.Entry -> VIEW_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_SECTION -> SectionHolder(
                ItemBrowserHistorySectionBinding.inflate(inflater, parent, false),
            )
            else -> EntryHolder(
                ItemBrowserHistoryEntryBinding.inflate(inflater, parent, false),
                onOpen,
                onRemove,
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryListItem.Section -> (holder as SectionHolder).bind(item.label)
            is HistoryListItem.Entry -> (holder as EntryHolder).bind(item.item)
        }
    }

    override fun getItemCount(): Int = items.size

    private class SectionHolder(
        private val binding: ItemBrowserHistorySectionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(label: String) {
            binding.root.text = label
        }
    }

    private class EntryHolder(
        private val binding: ItemBrowserHistoryEntryBinding,
        private val onOpen: (String) -> Unit,
        private val onRemove: (Long) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BrowserHistoryEntity) {
            binding.tvHistoryTitle.text = item.title.ifBlank { item.url }
            binding.tvHistoryUrl.text = HistoryUiMapper.displayUrl(item.url)

            when (HistoryUiMapper.iconTypeForUrl(item.url)) {
                HistoryIconType.VIDEO -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.bk_bg_history_icon_video)
                    binding.ivHistoryIcon.setImageResource(R.drawable.bk_ic_history_play)
                }
                HistoryIconType.SETTINGS -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.bk_bg_history_icon_settings)
                    binding.ivHistoryIcon.setImageResource(R.drawable.bk_ic_history_settings)
                }
                HistoryIconType.WEBSITE -> {
                    binding.iconContainer.setBackgroundResource(R.drawable.bk_bg_history_icon_globe)
                    binding.ivHistoryIcon.setImageResource(R.drawable.bk_ic_history_globe)
                }
            }

            binding.root.setOnClickListener { onOpen(item.url) }
            binding.btnRemoveHistory.setOnClickListener { onRemove(item.id) }
        }
    }

    companion object {
        private const val VIEW_SECTION = 0
        private const val VIEW_ENTRY = 1
    }
}
