package com.avd.ui.main.home.browser.homeTab.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.IconItem


class MovieItemAdapter(
    private val items: List<IconItem>,
    private val onItemClick: (IconItem) -> Unit
) : RecyclerView.Adapter<MovieItemAdapter.IconViewHolder>() {

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.movie)
        val icon: ImageView = itemView.findViewById(R.id.movie_icon)
        val title: TextView = itemView.findViewById(R.id.movie_text)

        fun bind(item: IconItem) {
            icon.setImageResource(item.iconResId)
            try {
                thumb.setImageResource(item.thumbId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            title.text = item.title

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.browser_movie_items, parent, false)
        return IconViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
