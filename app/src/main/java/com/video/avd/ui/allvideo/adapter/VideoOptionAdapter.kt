package com.video.avd.ui.allvideo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.video.avd.databinding.ItemVideoOptionBottomsheetBinding

class VideoOptionAdapter(
   private val context:Context,
    private val list : List<VideoOptions>,
    private val listener : VideoOptionsClickListener?=null
) :RecyclerView.Adapter<VideoOptionAdapter.VideoOptionViewHolder>() {


    inner class VideoOptionViewHolder(val binding: ItemVideoOptionBottomsheetBinding) : ViewHolder(binding.root){
        fun bind(item: VideoOptions, position: Int) {
            binding.tvVideoOption.text = item.name
            Glide.with(context).load(item.icon).into(binding.ivVideoOption)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoOptionViewHolder {
       return VideoOptionViewHolder(
           ItemVideoOptionBottomsheetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
       )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VideoOptionViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item, position)
        holder.itemView.setOnClickListener {
            listener?.onVideoOptionClick(item, position)
        }
    }

    interface VideoOptionsClickListener{
        fun onVideoOptionClick(item:VideoOptions, position:Int)
    }
}