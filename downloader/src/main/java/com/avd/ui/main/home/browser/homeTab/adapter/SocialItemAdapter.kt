package com.avd.ui.main.home.browser.homeTab.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.data.local.room.entity.PageInfo

class SocialItemAdapter(
    private val items: List<PageInfo>,
    private val onItemClick: (PageInfo) -> Unit
) : RecyclerView.Adapter<SocialItemAdapter.IconViewHolder>() {

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.movie)
        val title: TextView? = itemView.findViewById(R.id.movie_text)

        fun bind(item: PageInfo) {
            itemView.visibility = View.VISIBLE
            icon.setImageResource(item.drawableResId)

            title?.text = when {
                item.link.contains("Status", ignoreCase = true) -> "Status"
                item.link.contains("x.com", ignoreCase = true) -> "X"
                item.link.contains("facebook", ignoreCase = true) -> "Facebook"
                item.link.contains("tiktok", ignoreCase = true) -> "TikTok"
                item.link.contains("instagram", ignoreCase = true) -> "Instagram"
                else -> "Social"
            }
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
