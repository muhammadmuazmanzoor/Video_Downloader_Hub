package com.video.avd.ui.player.bookmark

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R

class VideoBookmarkAdapter(
    private val list: List<VideoBookmark>,
    private val listener: VideoBookmarkItemClickListener
    ) :
    RecyclerView.Adapter<VideoBookmarkAdapter.VideoBookmarkViewHolder>() {

    inner class VideoBookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBookmark: TextView? = itemView.findViewById(R.id.tv_bookmark)
        val tvPosition: TextView? = itemView.findViewById(R.id.tv_position)
        val delete : ImageView? = itemView.findViewById(R.id.bookmark_delete)
        val rename : ImageView? = itemView.findViewById(R.id.bookmark_rename)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoBookmarkViewHolder = VideoBookmarkViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_video_bookmark,
                parent,
                false
            )
    )

    override fun getItemCount(): Int = list.size


    override fun onBindViewHolder(
        holder: VideoBookmarkViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val item = list[position]
        list[position].position.let {
            if (item.bookmarkName.isEmpty()) holder.tvBookmark?.text = "Bookmark at ${formatDuration(it)}"
            else holder.tvBookmark?.text = item.bookmarkName
             holder.tvPosition?.text = formatDuration(it)
        }
        holder.itemView.setOnClickListener {
            listener.onVideoBookmarkClick(item, position, which = "", anchorView = it)
        }
        holder.delete?.setOnClickListener {
            listener.onVideoBookmarkClick(item, position, which = "delete", anchorView = it)
        }
        holder.rename?.setOnClickListener {
            listener.onVideoBookmarkClick(item, position, which = "rename", anchorView = it)
        }
    }

    private fun formatDuration(durationInMillis: Long): String {
        return try {
            val totalSeconds = durationInMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        } catch (e: Exception) {
            "00:00"
        }
    }
}

interface VideoBookmarkItemClickListener {
    fun onVideoBookmarkClick(item : VideoBookmark, position: Int, which : String ,anchorView : View)
}