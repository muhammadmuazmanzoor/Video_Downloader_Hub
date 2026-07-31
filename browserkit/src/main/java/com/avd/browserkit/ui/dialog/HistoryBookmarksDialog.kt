package com.avd.browserkit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avd.browserkit.R
import com.avd.browserkit.data.BrowserRepository
import com.avd.browserkit.databinding.ItemHistoryBookmarkBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

object HistoryBookmarksDialog {
    private const val TAG = "HistoryBookmarksDialog"

    fun show(manager: FragmentManager, repository: BrowserRepository, onOpenUrl: (String) -> Unit) {
        if (manager.findFragmentByTag(TAG) != null) return
        HistoryBookmarksDialogFragment().apply {
            this.repository = repository
            this.onOpenUrl = onOpenUrl
        }.show(manager, TAG)
    }
}

class HistoryBookmarksDialogFragment : DialogFragment() {
    lateinit var repository: BrowserRepository
    var onOpenUrl: ((String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_history_bookmarks, null, false)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabHistoryBookmarks)
        val recycler = view.findViewById<RecyclerView>(R.id.rvHistoryBookmarks)
        val adapter = HistoryBookmarkAdapter { url ->
            onOpenUrl?.invoke(url)
            dismiss()
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        fun loadTab(index: Int) {
            lifecycleScope.launch {
                val items = if (index == 0) {
                    repository.getHistory().map { it.title to it.url }
                } else {
                    repository.getBookmarks().map { it.title to it.url }
                }
                adapter.submit(items)
            }
        }

        tabLayout.addTab(tabLayout.newTab().setText(R.string.bk_history))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.bk_bookmarks))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = loadTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        loadTab(0)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.bk_history)
            .setView(view)
            .setNegativeButton(R.string.bk_cancel, null)
            .create()
    }
}

private class HistoryBookmarkAdapter(
    private val onClick: (String) -> Unit,
) : RecyclerView.Adapter<HistoryBookmarkAdapter.Holder>() {
    private var items: List<Pair<String, String>> = emptyList()

    fun submit(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHistoryBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class Holder(private val binding: ItemHistoryBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Pair<String, String>) {
            binding.tvTitle.text = item.first
            binding.tvUrl.text = item.second
            binding.root.setOnClickListener { onClick(item.second) }
        }
    }
}
