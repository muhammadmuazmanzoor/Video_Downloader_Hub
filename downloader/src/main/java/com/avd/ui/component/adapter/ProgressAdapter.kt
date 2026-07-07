package com.avd.ui.component.adapter

import android.content.Context
import android.util.DisplayMetrics
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.avd.R
import com.avd.data.local.room.entity.ProgressInfo
import com.avd.databinding.ItemProgressBinding
import androidx.recyclerview.widget.DiffUtil

class ProgressAdapter(
    private var progressInfos: List<ProgressInfo>,
    private var videoListener: ProgressListener
) : RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = DataBindingUtil.inflate<ItemProgressBinding>(
            LayoutInflater.from(parent.context), R.layout.item_progress, parent, false
        )

        return ProgressViewHolder(binding)
    }

    override fun getItemCount() = progressInfos.size

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) = holder.bind(progressInfos[position], videoListener)

    override fun onBindViewHolder(
        holder: ProgressViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_PROGRESS)) {
            holder.updateProgress(progressInfos[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemId(position: Int): Long {
        return progressInfos[position].downloadId
    }

    class ProgressViewHolder(val binding: ItemProgressBinding) : RecyclerView.ViewHolder(binding.root) {

        // Cache screen resolution to avoid repeated DisplayMetrics allocation during GC-sensitive layout
        private var cachedScreenSize: Pair<Int, Int>? = null

        fun bind(progressInfo: ProgressInfo, progressListener: ProgressListener) {
            val thumbnail = progressInfo.videoInfo.formats.formats.firstOrNull()?.url
            val placeholder = R.drawable.ic_video_24dp
            val size = getScreenResolution(itemView.context)
            with(binding) {
                this.progressInfo = progressInfo
                this.progressListener = progressListener
                this.downloadId = progressInfo.downloadId
                this.isRegular = progressInfo.videoInfo.isRegularDownload
                Glide.with(this@ProgressViewHolder.itemView.context).load(thumbnail).fitCenter()
                    .error(placeholder)
                    .placeholder(placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions().override(size.first / 8, size.second / 8))
                    .into(this.ivThumbnail)
                executePendingBindings()
            }
        }

        fun updateProgress(progressInfo: ProgressInfo) {
            with(binding) {
                this.progressInfo = progressInfo
                this.downloadId = progressInfo.downloadId
                this.isRegular = progressInfo.videoInfo.isRegularDownload
                progressBar.progress = progressInfo.progress
                tvProgress.text = progressInfo.progressSize
                executePendingBindings()
            }
        }

        private fun getScreenResolution(context: Context): Pair<Int, Int> {
            // Cache screen resolution to reduce object allocation during GC-sensitive operations
            if (cachedScreenSize == null) {
                val displayMetrics = DisplayMetrics()
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                cachedScreenSize = Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
            }
            return cachedScreenSize!!
        }

    }

    fun setData(progressInfos: List<ProgressInfo>) {
        val oldItems = this.progressInfos
        this.progressInfos = progressInfos
        DiffUtil.calculateDiff(ProgressDiffCallback(oldItems, progressInfos))
            .dispatchUpdatesTo(this)
    }

    private class ProgressDiffCallback(
        private val oldItems: List<ProgressInfo>,
        private val newItems: List<ProgressInfo>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].id == newItems[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val old = oldItems[oldItemPosition]
            val new = newItems[newItemPosition]
            return if (
                old.videoInfo == new.videoInfo &&
                (old.progressDownloaded != new.progressDownloaded ||
                    old.progressTotal != new.progressTotal ||
                    old.downloadStatus != new.downloadStatus ||
                    old.infoLine != new.infoLine)
            ) {
                PAYLOAD_PROGRESS
            } else {
                null
            }
        }
    }

    companion object {
        private const val PAYLOAD_PROGRESS = "payload_progress"
    }
}

interface ProgressListener {
    fun onMenuClicked(view: View, downloadId: Long, isRegular: Boolean)
}
