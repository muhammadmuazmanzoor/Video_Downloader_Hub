package com.video.avd.ui.file_manager

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.databinding.ItemDirectoryBinding

class DirAdapter(var list: List<DirectoryModel> = emptyList(), var listener : DirectoryClickListener) : RecyclerView.Adapter<DirAdapter.FileManagerViewHolder>() {

    inner class FileManagerViewHolder(val binding: ItemDirectoryBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: DirectoryModel) {
            binding.tvName.text = item.name
            binding.tvNoOfFiles.text = item.subFolderCount

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileManagerViewHolder {
        return FileManagerViewHolder(
            ItemDirectoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FileManagerViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.binding.root.setOnClickListener {
            listener.onDirectoryClick(item, position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }



    fun updateList(list: List<DirectoryModel>) {
        this.list = list
        notifyDataSetChanged()
    }
    interface DirectoryClickListener {
        fun onDirectoryClick(item: DirectoryModel, position: Int)
    }

}