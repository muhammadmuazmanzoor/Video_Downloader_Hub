package com.video.avd.ui.file_manager

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.databinding.ItemDirectoryBinding


class FileManagerDirectoriesAdapter(
    private var list: List<DirectoryModel>? = null,
    private val listener: DirectoryClickListener? = null,
    private val isFavourite: Boolean = false
) : RecyclerView.Adapter<FileManagerDirectoriesAdapter.FileManagerViewHolder>() {

    inner class FileManagerViewHolder(var binding: ItemDirectoryBinding) :
        ViewHolder(binding.root) {
        fun bind(item: DirectoryModel) {
            binding.tvName.text = item.name

            if (item.name.lowercase().contains("music") || item.name.lowercase().contains("mp3")) {
                binding.tvNoOfFiles.text = "${item.audioCount} Songs"
                binding.icDir.visibility = View.GONE
                binding.divider.visibility = View.GONE
                binding.tvNoOfVideos.visibility = View.GONE
            } else {
                binding.tvNoOfFiles.text =
                if (item.subFolderCount == "Directory is empty") "0" else item.subFolderCount
                binding.icDir.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                binding.tvNoOfVideos.visibility = View.VISIBLE
                binding.tvNoOfVideos.text = "${item.videoCount} Videos"
            }

            if (isFavourite) {
                binding.favIcon.visibility = View.VISIBLE
                if (item.name.lowercase().contains("download")) {
                    Glide.with(binding.favIcon.context).load(R.drawable.ic_download_file)
                        .into(binding.favIcon)
                } else if (item.name.lowercase().contains("movie")) {
                    Glide.with(binding.favIcon.context).load(R.drawable.ic_video_selected)
                        .into(binding.favIcon)
                } else if (item.name.lowercase().contains("music")) {
                    Glide.with(binding.favIcon.context).load(R.drawable.ic_music_new)
                        .into(binding.favIcon)
                } else if (item.name.lowercase().contains("whatsapp")) {
                    binding.favIcon.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileManagerViewHolder {
        return FileManagerViewHolder(
            ItemDirectoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
            if (item != null) {
                listener?.onDirectoryClick(item, position)
            }
        }
    }

    interface DirectoryClickListener {
        fun onDirectoryClick(item: DirectoryModel, position: Int)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: List<DirectoryModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun getList() = list
}