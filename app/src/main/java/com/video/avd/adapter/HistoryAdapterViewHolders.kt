package com.video.avd.adapter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.BaseRequestOptions
import com.bumptech.glide.request.RequestOptions
import com.video.avd.databinding.WatchHistoryFragmentItemBinding
import com.video.avd.databinding.WatchHistoryFragmentItemGridBinding
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils

class HistoryAdapterViewHolders(private val listener : WatchHistoryRecyclerView.OnHistoryCardClickListener?=null) {
    var cropOptions: RequestOptions? = null
    init {
        cropOptions = RequestOptions().centerCrop()
    }

    inner  class HistoryViewHolder(val binding: WatchHistoryFragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: Video,itemlist:List<Video>, position: Int) {
            binding.apply {
                name.text = item.title
                videoProgressBar.progress= AppUtils.calculateProgress(item.lastPlayed, AppUtils.parseDurationString(item.duration))
                try {
                    Glide.with(imageView.context)
                        .load(item.contentUri)
                        .apply(cropOptions as BaseRequestOptions<*>)
                        // Resize the image if necessary
                        .override(imageView.width, imageView.height)
                        .transition(DrawableTransitionOptions.withCrossFade(500)) // Shortened the crossfade duration
                        .into(imageView)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
               time.text = item.duration
                videoMenu.setOnClickListener {
                    listener?.onDeleteClick(item)
                }

            }
        }
    }

    inner  class HistoryViewHolderGrid(val binding: WatchHistoryFragmentItemGridBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: Video,itemlist:List<Video>, position: Int) {
            binding.apply {
                name.text = item.title
                videoProgressBar.progress= AppUtils.calculateProgress(
                    item.lastPlayed,
                    AppUtils.parseDurationString(item.duration)
                )
                try {
                    Glide.with(imageView.context)
                        .load(item.contentUri)
                        .apply(cropOptions as BaseRequestOptions<*>)
                        // Resize the image if necessary
                        .override(imageView.width, imageView.height)
                        .transition(DrawableTransitionOptions.withCrossFade(500)) // Shortened the crossfade duration
                        .into(imageView)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                time.text = item.duration
                videoMenu.setOnClickListener {
                    listener?.onDeleteClick(item)
                }
                historyLayout.setOnClickListener {
                    listener?.onCardClick(position,itemlist)
                }
            }
        }
    }

}