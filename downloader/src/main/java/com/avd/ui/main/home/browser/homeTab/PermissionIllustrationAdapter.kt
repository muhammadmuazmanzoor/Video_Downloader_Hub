package com.avd.ui.main.home.browser.homeTab

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.avd.databinding.ItemPermissionIllustrationBinding

class PermissionIllustrationAdapter(
    private val images: List<Int>
) : RecyclerView.Adapter<PermissionIllustrationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPermissionIllustrationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: Int) {
            binding.ivIllustration.setImageResource(imageRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPermissionIllustrationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size
}
