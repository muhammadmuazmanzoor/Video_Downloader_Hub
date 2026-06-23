package com.video.avd.ui.file_manager

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.databinding.ItemDirectoryBinding
import com.video.avd.databinding.ItemFilesBinding


class FileManagerAdapter(
    private var list: List<MediaResources>? = null,
    private val listener : FileMangerClickListener?=null,
    private val menuListener : FileManagerMenuClickListener?=null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_DIRECTORY = 0
    private val VIEW_TYPE_VIDEO = 1
    private val VIEW_TYPE_AUDIO = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
        val holder = FileManagerViewHolders()
        return when (viewType) {
            0 -> {
                val binding = ItemDirectoryBinding.inflate(view, parent, false)
                holder.DirectoryViewHolder(binding)
            }
            1->{
                val binding = ItemFilesBinding.inflate(view, parent, false)
                holder.VideoViewHolder(binding)
            }
            else -> {
                throw IllegalArgumentException("")
            }
        }
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return when (list?.get(position)) {
            is MediaResources.DirectoryItems -> VIEW_TYPE_DIRECTORY
            is MediaResources.VideoItems -> VIEW_TYPE_VIDEO
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list?.get(position)) {
            is MediaResources.DirectoryItems ->{
                (holder as FileManagerViewHolders.DirectoryViewHolder).bind(item, position)
                holder.itemView.setOnClickListener {
                  listener?.onDirectoryClick(position, item)
                }
            }
            is MediaResources.VideoItems ->{
                (holder as FileManagerViewHolders.VideoViewHolder).bind(item, position)
                holder.itemView.setOnClickListener {
                    listener?.onVideoClick(position,item)
                }
                holder.binding.menu.setOnClickListener {
                    menuListener?.onVideoMenuClick(position, item)
                }
            }
            else -> {Log.d("ddd","invalid type")}
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: List<MediaResources>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun removeItemAt(position: Int) {
        list?.let {
            if (position in it.indices) {
                it.toMutableList().removeAt(position)
                notifyItemRemoved(position)
            //    notifyItemRangeChanged(position, it.size)
            }
        }
    }

    interface FileMangerClickListener{
        fun onDirectoryClick(position: Int, item: MediaResources.DirectoryItems)
        fun onVideoClick(position: Int, item: MediaResources.VideoItems)
    }

    interface FileManagerMenuClickListener{
        fun onVideoMenuClick(position: Int, item: MediaResources.VideoItems)
    }
}