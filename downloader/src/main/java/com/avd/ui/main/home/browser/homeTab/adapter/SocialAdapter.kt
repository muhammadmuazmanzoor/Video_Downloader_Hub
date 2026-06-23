package com.avd.ui.main.home.browser.homeTab.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.data.local.room.entity.PageInfo
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.IconItem


class SocialAdapter(
    private val items: List<PageInfo>,
    private val onItemClick: (PageInfo) -> Unit
) : RecyclerView.Adapter<SocialAdapter.IconViewHolder>() {

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.movie)
        val title: TextView = itemView.findViewById(R.id.movie_text)

        fun bind(item: PageInfo) {
            icon.setImageResource(item.drawableResId)

            if(item.link.contains("dailymotion")){
                title.text = "DailyMotion"
            }
            else if(item.link.contains("imdb")){
                title.text = "IMDB"
            }
            else if(item.link.contains("vimeo")){
                title.text = "Vimeo"
            }
            else if(item.link.contains("Status")){
                title.text = "Status"
            }
            else if(item.link.contains("x")){
                title.text = "X"
            }
            else if(item.link.contains("facebook")){
                title.text = "Facebook"
            }
            else if(item.link.contains("bing")){
                title.text = "Bing"
            }
            else if(item.link.contains("duckduckgo")){
                title.text = "DuckDuckGo"
            }
            else if(item.link.contains("google")){
                title.text = "Google"
            }
            else if(item.link.contains("tiktok")){
                title.text = "TikTok"
            }
            else if(item.link.contains("instagram")){
                title.text = "Instagram"
            }
            else{
                title.text = "Social"
            }
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.social_item_browser, parent, false)
        return IconViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
