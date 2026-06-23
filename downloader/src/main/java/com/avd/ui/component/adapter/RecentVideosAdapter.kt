package com.avd.ui.component.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.avd.data.local.model.LocalVideo
import com.avd.data.local.room.entity.VideoInfo
import com.avd.databinding.ItemHomeVideoBinding
import com.bumptech.glide.load.engine.DiskCacheStrategy

sealed class VideoItem {
    data class DownloadedVideo(val video: LocalVideo) : VideoItem()
    data class DownloadingVideo(val videoInfo: VideoInfo, val progress: Int) : VideoItem()
}

class RecentVideosAdapter(
    private val onItemClick: (VideoInfo) -> Unit
) : ListAdapter<VideoItem, RecentVideosAdapter.VideoViewHolder>(VideoDiffCallback()) {

    private val downloadingVideos = mutableMapOf<String, Int>()

    fun updateDownloadProgress(videoId: String, progress: Int, videoInfo: VideoInfo? = null): Boolean {
        downloadingVideos[videoId] = progress
        val index = currentList.indexOfFirst {
            it is VideoItem.DownloadingVideo && it.videoInfo.id == videoId
        }
        return if (index != -1) {
            // Update existing item
            val updatedList = currentList.mapIndexed { i, item ->
                if (i == index && item is VideoItem.DownloadingVideo) {
                    item.copy(progress = progress)
                } else item
            }
            submitList(updatedList)
            false // not inserted
        } else if (videoInfo != null) {
            // Insert new item at top
            val newList = listOf(VideoItem.DownloadingVideo(videoInfo, progress)) + currentList
            submitList(newList)
            true // new item inserted
        } else {
            false
        }
    }


    fun removeDownloadProgress(videoId: String)  {
        downloadingVideos.remove(videoId)
        // Remove the downloading item
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { 
            it is VideoItem.DownloadingVideo && it.videoInfo.id == videoId 
        }
        if (index != -1) {
            currentList.removeAt(index)
            submitList(currentList)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDownloadingVideos(videos: List<VideoInfo>) : Boolean {
        val currentList = currentList.toMutableList()
        // Remove old downloading items
        currentList.removeAll { it is VideoItem.DownloadingVideo }
        // Add new downloading items
        videos.forEach { videoInfo ->
            val progress = downloadingVideos[videoInfo.id] ?: 0
            currentList.add(0, VideoItem.DownloadingVideo(videoInfo, progress))
        }
        submitList(currentList)
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDownloadedVideos(videos: List<LocalVideo>) :Boolean {
        val currentList = currentList.toMutableList()
        // Keep only downloading items
        val downloadingItems = currentList.filterIsInstance<VideoItem.DownloadingVideo>()
        // Add downloaded videos
        val newList = downloadingItems + videos.map { VideoItem.DownloadedVideo(it) }
        submitList(newList)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemHomeVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = getItem(position)
        when (item) {
            is VideoItem.DownloadedVideo -> holder.bindDownloaded(item.video)
            is VideoItem.DownloadingVideo -> holder.bindDownloading(item.videoInfo, item.progress)
        }
    }

    inner class VideoViewHolder(
        private val binding: ItemHomeVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindDownloaded(video: LocalVideo) {
            binding.apply {
                videoInfo = VideoInfo(
                    id = video.id.toString(),
                    title = video.name,
                    thumbnail = video.thumbnailPath.toString(),
                    duration = 0,
                    originalUrl = video.uri.toString(),
                    downloadUrls = emptyList(),
                    ext = "mp4"
                )
                details="${videoInfo?.duration?:"00:00"} | ${videoInfo?.ext?:".mp4"}"
                isDownloading = false
                progress = 0
                Glide.with(thumbnail.context)
                    .load(videoInfo?.thumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(thumbnail)
                executePendingBindings()
                root.setOnClickListener {
                    onItemClick(videoInfo!!)
                }
            }
        }

        fun bindDownloading(videoInfo: VideoInfo, progress: Int) {
            binding.apply {
                this.videoInfo = videoInfo
                isDownloading = true
                this.progress = progress
                Glide.with(thumbnail.context)
                    .load(videoInfo?.thumbnail)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(thumbnail)
//                Glide.with(thumbnail.context).load(videoInfo.thumbnail).into(thumbnail)
                executePendingBindings()
                root.setOnClickListener {
                    onItemClick(videoInfo)
                }
            }
        }
    }

    private class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            val areSame = when {
                oldItem is VideoItem.DownloadedVideo && newItem is VideoItem.DownloadedVideo ->
                    oldItem.video.id == newItem.video.id
                oldItem is VideoItem.DownloadingVideo && newItem is VideoItem.DownloadingVideo ->
                    oldItem.videoInfo.id == newItem.videoInfo.id
                else -> false
            }
            return areSame
        }

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            val areSame = when {
                oldItem is VideoItem.DownloadedVideo && newItem is VideoItem.DownloadedVideo ->
                    oldItem.video == newItem.video
                oldItem is VideoItem.DownloadingVideo && newItem is VideoItem.DownloadingVideo ->
                    oldItem.videoInfo.id == newItem.videoInfo.id &&
                    oldItem.videoInfo.title == newItem.videoInfo.title &&
                    oldItem.progress == newItem.progress
                else -> false
            }
            return areSame
        }
    }

} 