package com.video.avd.ui.videos.adapter

import androidx.recyclerview.widget.DiffUtil
import com.video.avd.ui.videos.model.Video

class VideoDiffCallback(
    private val oldList: List<Video>,
    private val newList: List<Video>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Replace this with your own logic if needed
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Replace this with your own logic if needed
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}