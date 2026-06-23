package com.video.avd.ui.playbackspeed

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R
import com.video.avd.databinding.ItemPlaybackspeedBinding

interface PlayBackSpeedButtonListener {
    fun onPlayBackButtonClick(position: Int, modelPlayBackSpeed: ModelPlayBackSpeed)
}

open class AdapterPlaybackSpeedSelection constructor(
    private val context: Context,
    private val listener: PlayBackSpeedButtonListener
) : ListAdapter<ModelPlayBackSpeed, RecyclerView.ViewHolder>(PlaybackSpeedDiffCallback()) {

    companion object {
        var selectedItemPosition: Int = 1
    }

    fun defaultMode() {
        selectedItemPosition = 1
        notifyDataSetChanged()
    }

    fun selectMode(position: Int) {
        selectedItemPosition = position
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            context,
            listener,
            ItemPlaybackspeedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val apiAds = getItem(position)
        (holder as ViewHolder).bind(apiAds, position, selectedItemPosition)
    }

    class ViewHolder(
        private val context: Context,
        listener: PlayBackSpeedButtonListener,
        private val binding: ItemPlaybackspeedBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.setClickListener {
                binding.modelPlayBackSpeed?.let { item ->
                    listener.onPlayBackButtonClick(adapterPosition, item)
                }
            }
        }

        fun bind(item: ModelPlayBackSpeed, position: Int, selectedItemPosition: Int) {
            highlightItemAt(
                binding = binding,
                position = position,
                selectedItemPosition = selectedItemPosition
            )
            binding.apply {
                modelPlayBackSpeed = item
                executePendingBindings()
            }
        }

        private fun highlightItemAt(
            binding: ItemPlaybackspeedBinding,
            position: Int,
            selectedItemPosition: Int
        ) {
            /*binding.icon.setColorFilter(
                if (selectedItemPosition == position) ContextCompat.getColor(
                    context, R.color.white
                ) else ContextCompat.getColor(
                    context, R.color.colorPrimaryDark
                )
            )*/
            binding.buttonSpeed.setBackgroundResource(if (selectedItemPosition == position) R.drawable.bg_playback_button_select else R.drawable.bg_playback_button)

        }
    }
}

class PlaybackSpeedDiffCallback : DiffUtil.ItemCallback<ModelPlayBackSpeed>() {

    override fun areItemsTheSame(
        oldItem: ModelPlayBackSpeed,
        newItem: ModelPlayBackSpeed
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ModelPlayBackSpeed,
        newItem: ModelPlayBackSpeed
    ): Boolean {
        return oldItem == newItem
    }
}