package com.video.avd.ui.folder.adapter

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R
import com.video.avd.databinding.ItemFolderGridVideoBinding
import com.video.avd.databinding.ItemFolderVideoBinding
import com.video.avd.ui.folder.model.VideoFolder

class FolderAdapterViewHolders() {

    inner class FolderViewHolder(val binding: ItemFolderVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
        fun bind(item: VideoFolder, privateVideosCount  : Int = 0) {
            binding.let {
                it.name.text = item.name
                it.count.text = item.videoCount.toString() +" "+ binding.root.context.getString(R.string.videos)
                if (item.hasNewVideos){
                    it.newtag.visibility=View.VISIBLE
                }else {
                    it.newtag.visibility = View.GONE
                }
                if (item.id == 7777777L){
                    val videos = binding.root.context.getString(R.string.videos)
                    val video = binding.root.context.getString(R.string.video)
                    it.count.visibility = View.VISIBLE
                    it.count.text = if (privateVideosCount <2) "$privateVideosCount $video" else "$privateVideosCount $videos"
                }
                else{
                    if (item.videoCount == 0){
                        it.count.visibility=View.GONE
                    }else{
                        it.count.visibility=View.VISIBLE
                    }
                }
                when(item.id){
                    786000000L ->{
                        it.imageView.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_storage))
                    }
                    -1L ->{
                        it.count.setTextColor(ContextCompat.getColor(binding.root.context,R.color.gSelector_light))
                        it.imageView.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_recent))
                    }
                    else -> { // Default case for other items
                        it.imageView.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_folders_new)) // Replace with your default icon
                        it.count.setTextColor(ContextCompat.getColor(binding.root.context, R.color.gray_text))
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    inner class FolderViewHolderGrid(val binding: ItemFolderGridVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: VideoFolder, privateVideosCount  : Int = 0) {
            binding.let {
                it.name.text = item.name
                it.count.text = item.videoCount.toString() +" " + binding.root.context.getString(R.string.videos)
                if (item.id == 7777777L){
                    val videos = binding.root.context.getString(R.string.videos)
                    val video = binding.root.context.getString(R.string.video)
                    it.count.visibility = View.VISIBLE
                    it.count.text = if (privateVideosCount <2) "$privateVideosCount $video" else "$privateVideosCount $videos"
                }else{
                    if (item.videoCount == 0){
                        it.count.visibility=View.GONE
                    }else{
                        it.count.visibility=View.VISIBLE
                    }
                }
                when(item.id){
                    786000000L ->{
                        it.imageView.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_storage))
                    }
                    -1L ->{
                        it.count.setTextColor(ContextCompat.getColor(it.count.context,R.color.gSelector))
                        it.imageView.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_recent))
                    }
                    else -> { // Default case for other items
                        it.imageView.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_folders_new)) // Replace with your default icon
                        it.count.setTextColor(ContextCompat.getColor(it.count.context,R.color.nonSelectedColor))
                    }
                }
            }
        }
    }


}