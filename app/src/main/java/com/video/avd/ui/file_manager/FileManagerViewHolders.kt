package com.video.avd.ui.file_manager


import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.databinding.ItemDirectoryBinding
import com.video.avd.databinding.ItemFilesBinding

class FileManagerViewHolders() {

   inner  class DirectoryViewHolder(val binding: ItemDirectoryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaResources.DirectoryItems, position: Int) {
                binding.apply  {
                    tvName.text = item.item.name
                    if (item.item.name.lowercase().contains("music") || item.item.name.lowercase().contains("mp3")) {
                        tvNoOfFiles.text = "${item.item.audioCount} Songs"
                        icDir.visibility = View.GONE
                        divider.visibility = View.GONE
                        tvNoOfVideos.visibility = View.GONE
                    } else {
                        tvNoOfFiles.text =
                            if (item.item.subFolderCount == "Directory is empty") "0" else item.item.subFolderCount
                        icDir.visibility = View.VISIBLE
                        divider.visibility = View.VISIBLE
                        tvNoOfVideos.visibility = View.VISIBLE
                        tvNoOfVideos.text = "${item.item.videoCount} Videos"
                    }
                }
        }
    }
     inner  class VideoViewHolder(val binding: ItemFilesBinding) : RecyclerView.ViewHolder(binding.root) {

         fun bind(item: MediaResources.VideoItems, position: Int) {
             binding.apply  {
                 binding.tvName.text = item.item.title
                icon.let {
                     it.context?.let {
                         Glide.with(it).load(R.drawable.ic_videos).into(binding.icon)
                     }
                 }
//                 binding.menu.setOnClickListener {
//                     val originalPosition = list?.indexOf(item) ?: 0
//                     menuClickListener?.onVideoMenuClick(originalPosition, item)
//                 }
             }
         }
     }
}