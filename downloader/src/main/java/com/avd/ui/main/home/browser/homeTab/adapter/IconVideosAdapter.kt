package com.avd.ui.main.home.browser.homeTab.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.IconItem


class IconVideosAdapter(
    private val items: List<IconItem>,
    private val onItemClick: (IconItem) -> Unit
) : RecyclerView.Adapter<IconVideosAdapter.IconViewHolder>() {

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.movie)
        val title: TextView? = itemView.findViewById(R.id.movie_text)

        fun bind(item: IconItem) {
            icon.setImageResource(item.iconResId)
            title?.text = item.title
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.video_content_items, parent, false)
        return IconViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
