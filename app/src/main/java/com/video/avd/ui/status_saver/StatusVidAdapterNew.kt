package com.video.avd.ui.status_saver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.ui.status_saver.model.Status
import com.video.avd.ui.status_saver.statusnew.ItemViewHoldernew

class StatusVidAdapterNew(
    private var context: Context,
    private val recyclerView: RecyclerView,
    private var videoList: List<Status>,
    private val listener: StatusVideoClickListener
) : RecyclerView.Adapter<ItemViewHoldernew>() {

    private val selectedItems = mutableSetOf<Int>()
    private var isSelectionMode = false
    private var isBusiness = false
    private var onItemLongClickListener: (() -> Unit)? = null

    fun updateData(videoList: List<Status>, isBusiness: Boolean) {
        this.videoList = videoList
        this.isBusiness = isBusiness
        safeNotifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHoldernew {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_status_new, parent, false)
        return ItemViewHoldernew(view)
    }

    override fun onBindViewHolder(holder: ItemViewHoldernew, position: Int) {
        val status = videoList[position]

        // Load image
        if (status.isApi30) {
            Glide.with(context).load(status.documentFile?.uri).into(holder.imageView)
        } else {
            Glide.with(context).load(status.file).into(holder.imageView)
        }

        // Configure checkbox visibility and state
        holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = selectedItems.contains(position)

        // Click listeners
        holder.rootLayout.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                listener.onStatusVideoClick(videoList, holder.absoluteAdapterPosition, status)
            }
        }

        holder.rootLayout.setOnLongClickListener {
            if (!isSelectionMode) {
                enableSelectionMode()
                selectedItems.add(position) // Select only the long-pressed item
                safeNotifyDataSetChanged() // Update all items to show checkboxes
                onItemLongClickListener?.invoke()
            }
            true
        }

        // Handle checkbox changes
        holder.checkBox.setOnClickListener {
            toggleSelection(position)
        }

        // Download button
        holder.download.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                listener.onsaveClick(status, context)
            }
        }

        // Share button
        holder.share.setOnClickListener {
            if (!isSelectionMode) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/mp4"
                    putExtra(
                        Intent.EXTRA_STREAM,
                        if (status.isApi30) status.documentFile?.uri
                        else Uri.parse("file://${status.file?.absolutePath}")
                    )
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share image"))
            }
        }
    }

    override fun getItemCount(): Int = videoList.size

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
            if (selectedItems.isEmpty()) {
                disableSelectionMode()
                onItemLongClickListener?.invoke()
            }
        } else {
            selectedItems.add(position)
        }
        safeNotifyItemChanged(position)
    }

    fun enableSelectionMode() {
        isSelectionMode = true
        safeNotifyDataSetChanged()
    }

    fun disableSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        safeNotifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        disableSelectionMode()
    }

    fun setOnItemLongClickListener(callback: () -> Unit) {
        this.onItemLongClickListener = callback
    }

    fun getSelectedItems(): List<Status> {
        return selectedItems.map { videoList[it] }
    }

    private fun safeNotifyDataSetChanged() {
        if (recyclerView.isComputingLayout) {
            recyclerView.post { notifyDataSetChanged() }
        } else {
            notifyDataSetChanged()
        }
    }

    private fun safeNotifyItemChanged(position: Int) {
        if (recyclerView.isComputingLayout) {
            recyclerView.post { notifyItemChanged(position) }
        } else {
            notifyItemChanged(position)
        }
    }

    interface StatusVideoClickListener {
        fun onStatusVideoClick(list: List<Status>, position: Int, status: Status)
        fun onsaveClick(status: Status, context: Context)
    }
}


