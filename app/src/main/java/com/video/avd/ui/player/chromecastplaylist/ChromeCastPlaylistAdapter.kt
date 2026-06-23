package com.video.avd.ui.player.chromecastplaylist

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.ui.videos.model.Video

class ChromeCastPlaylistAdapter(
    private val context: Context,
    private val list: List<Video>,
    private val currentPosition: Int,
    private val listener: ChromeCastPlayListItemClickListener
) :
    RecyclerView.Adapter<ChromeCastPlaylistAdapter.PlayListViewHolder>() {
    private var mCurrentPosition = currentPosition

    inner class PlayListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView? = itemView.findViewById(R.id.iv_playlist_thumbnail)
        val duration: TextView? = itemView.findViewById(R.id.tv_video_duration)
        val title: TextView? = itemView.findViewById(R.id.tv_song_title)
        val date: TextView? = itemView.findViewById(R.id.tv_date)
        val size: TextView? = itemView.findViewById(R.id.tv_song_size)
        val rootLayout: ConstraintLayout? = itemView.findViewById(R.id.rootLayout)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlayListViewHolder = PlayListViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_playlist_chrome_cast,
                parent,
                false
            )
    )

    override fun getItemCount(): Int = list.size


    override fun onBindViewHolder(
        holder: PlayListViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        list[position].title?.let {
            holder.title?.text = it
        }

        list[position].duration.let {
                holder.duration?.text = it
        }

        list[position].date.let {
            holder.date?.text = it.toString()
        }

        list[position].size.let {
            holder.size?.text = it.toString()
        }
        list[position].contentUri?.let {
            holder.image?.let { it1 -> Glide.with(context).load(it).into(it1) }
        }

        holder.itemView.setOnClickListener {
            listener.onItemClick(position, list)
            mCurrentPosition = position
        }
        if (currentPosition == position) {
            //  holder.rootLayout.background=context.resources.getDrawable(R.drawable.bg_playing_video)
            holder.title?.setTextColor(ContextCompat.getColor(context, R.color.dark_mode_green))
            holder.duration?.setTextColor(ContextCompat.getColor(context, R.color.dark_mode_green))
        } else {
            //holder.rootLayout.background=null
            holder.title?.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.duration?.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

    }

    interface ChromeCastPlayListItemClickListener {
        fun onItemClick(position: Int, list: List<Video>)

    }
}