package com.video.avd.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R
import com.video.avd.ui.fragments.themeListener

class ThemesAdapter(private val imageList: List<Int>, private val listener: themeListener) :
    RecyclerView.Adapter<ThemesAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.theme_item, parent, false)
        return ImageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageResId = imageList[position]
        holder.bindImage(imageResId)

    }

    override fun getItemCount(): Int = imageList.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.themeImage)

        fun bindImage(imageResId: Int) {
            imageView.setImageResource(imageResId)
        }
    }

//    override fun onCurrentItemChanged(viewHolder: ImageViewHolder?, adapterPosition: Int) {
//        listener.onThemeClicked(adapterPosition)
//    }
}




