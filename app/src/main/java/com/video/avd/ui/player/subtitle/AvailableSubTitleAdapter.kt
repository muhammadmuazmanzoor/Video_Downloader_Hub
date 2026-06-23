package com.video.avd.ui.player.subtitle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R

class AvailableSubtitlesAdapter(
    private val listener: SubTitleClickListener
) : ListAdapter<SubModel, AvailableSubtitlesAdapter.HistoryFragViewHolder>(SubDiffCallback()) {

    private var currentPosition = -1

    inner class HistoryFragViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView? = null
        var size: TextView? = null
        var radio: ImageView? = null

        init {
            name = itemView.findViewById(R.id.tv_name)
            size = itemView.findViewById(R.id.tv_size)
            radio = itemView.findViewById(R.id.radio_unit)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryFragViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtitles, parent, false)
        return HistoryFragViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryFragViewHolder, position: Int) {
        val item = getItem(position)

        holder.name?.text = item.subname
        holder.size?.text = item.subpath

        // Set click listener
        holder.itemView.setOnClickListener {
            currentPosition = position
            listener.onSubtitleClick(item, position)
            notifyDataSetChanged()
        }

        // Update radio button based on the current position
        if (currentPosition == position) {
            holder.radio?.setImageResource(R.drawable.check_new)
        } else {
            holder.radio?.setImageResource(R.drawable.uncheck_new)
        }
    }

    interface SubTitleClickListener {
        fun onSubtitleClick(item: SubModel?, position: Int)
    }

    class SubDiffCallback : DiffUtil.ItemCallback<SubModel>() {
        override fun areItemsTheSame(oldItem: SubModel, newItem: SubModel): Boolean {
            // Compare unique identifier (if any)
            return oldItem.subpath == newItem.subpath
        }

        override fun areContentsTheSame(oldItem: SubModel, newItem: SubModel): Boolean {
            // Compare the contents
            return oldItem == newItem
        }
    }
}
