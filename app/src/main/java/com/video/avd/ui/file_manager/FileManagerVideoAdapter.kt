package com.video.avd.ui.file_manager

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.databinding.ItemFilesBinding
import com.video.avd.ui.videos.model.Video

@Deprecated("Deprecated, Use FileManagerAdapter")
class FileManagerVideoAdapter(
    private var list: List<Video>? = null,


    ) : RecyclerView.Adapter<FileManagerVideoAdapter.FileManagerViewHolder>() {

    inner class FileManagerViewHolder(var binding: ItemFilesBinding) : ViewHolder(binding.root) {
        fun bind(item: Video) {
            binding.tvName.text = item.title
            binding.icon.let {
                binding.icon.context?.let {
                    Glide.with(it).load(R.drawable.ic_videos).into(binding.icon)
                }
            }
            binding.menu.setOnClickListener {
                val originalPosition = list?.indexOf(item) ?: 0

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileManagerViewHolder {
        return FileManagerViewHolder(
            ItemFilesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    override fun onBindViewHolder(holder: FileManagerViewHolder, position: Int) {
        val item = list?.get(position)
        if (item != null) {
            holder.bind(item)
        }

        holder.itemView.setOnClickListener {
            list?.let { fileList ->
                val originalPosition = list?.indexOf(item) ?: 0

            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: List<Video>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun removeItemAt(position: Int) {
        if (list?.indices?.contains(position) == true) {
            list = list?.filterIndexed { index, _ -> index != position }
            notifyItemRemoved(position)
        }
    }

}