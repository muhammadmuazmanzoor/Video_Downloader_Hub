package com.video.avd.ui.player.playlist

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.ui.videos.model.Video
import com.video.avd.constent.videoListLocal

class PlaylistAdapter(
    private val context: Context,
    private val listener: PlayListItemClickListener,
    var currentPlayingPos: Int = -1 // Position of currently playing item
) : RecyclerView.Adapter<PlaylistAdapter.PlayListViewHolder>() {

    private var videoList: MutableList<Video> = mutableListOf() // Mutable list to manage items

    inner class PlayListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_playlist_thumbnail)
        val removeItem: ImageView = itemView.findViewById(R.id.remove_item)
        val duration: TextView = itemView.findViewById(R.id.tv_video_duration)
        val title: TextView = itemView.findViewById(R.id.tv_song_title)
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val size: TextView = itemView.findViewById(R.id.tv_song_size)
        val rootLayout: ConstraintLayout = itemView.findViewById(R.id.rootLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlayListViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
        val video = videoList[position]
        holder.title.text = video.title
        holder.duration.text = video.duration
        holder.date.text = video.date
        holder.size.text = video.size

        video.contentUri?.let {
            Glide.with(context).load(it).into(holder.image)
        }

        // Highlight currently playing item
        if (position == currentPlayingPos) {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.dark_mode_green))
            holder.duration.setTextColor(ContextCompat.getColor(context, R.color.dark_mode_green))
            holder.size.setTextColor(ContextCompat.getColor(context, R.color.dark_mode_green))
        } else {
            holder.title.setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
            holder.duration.setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
            holder.size.setTextColor(ContextCompat.getColor(context, R.color.brand_text_primary))
        }

        // Item click listener
        holder.itemView.setOnClickListener {
            currentPlayingPos = position // Update currently playing item position
            listener.onItemClick(position, videoList)
            notifyDataSetChanged() // Refresh entire list
        }

        // Remove item when clicked on remove icon
        holder.removeItem.setOnClickListener {
            if(videoList.size>1) {
                removeItem(position)
            }
            else{
                Toast.makeText(context, "Currently playing , can't removed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setData(list: List<Video>) {
        videoList.clear()
        videoList.addAll(list)
        Handler(Looper.getMainLooper()).postDelayed({
            notifyDataSetChanged() // Notify the adapter of data change
        },500)
    }
    fun removeItem(position: Int) {
        try {
            val removedVideo = videoList[position]

            // Check if the removed item is currently playing
            if (position == currentPlayingPos) {
                listener.onPlayingItemRemoved(removedVideo)
                // Reset `currentPlayingPos` since the playing item was removed
                currentPlayingPos = -1
            } else if (position < currentPlayingPos) {
                // Decrement `currentPlayingPos` if an item above it is removed
                currentPlayingPos -= 1
            }

            // Remove item and update list
            videoList.removeAt(position)
            notifyItemRemoved(position)

            // Update the global LiveData
            videoListLocal.value = videoList

            // Delay UI refresh for smoothness
            Handler(Looper.getMainLooper()).postDelayed({
                notifyDataSetChanged() // Refresh entire list
            }, 500)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        try {
            // Move item from one position to another
            val movedItem = videoList.removeAt(fromPosition)
            videoList.add(toPosition, movedItem)
            notifyItemMoved(fromPosition, toPosition)

            // Update the global LiveData
            videoListLocal.value = videoList

            // Adjust the currentPlayingPos if necessary
            when {
                fromPosition == currentPlayingPos -> {
                    // If the currently playing item was moved, update to new position
                    currentPlayingPos = toPosition
                }
                fromPosition < currentPlayingPos && toPosition >= currentPlayingPos -> {
                    // If an item was moved past the current playing position from below, decrease `currentPlayingPos`
                    currentPlayingPos -= 1
                }
                fromPosition > currentPlayingPos && toPosition <= currentPlayingPos -> {
                    // If an item was moved past the current playing position from above, increase `currentPlayingPos`
                    currentPlayingPos += 1
                }
            }

            // Delay UI refresh for smoothness
            Handler(Looper.getMainLooper()).postDelayed({
                notifyDataSetChanged() // Refresh entire list
            }, 500)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /*    fun removeItem(position: Int) {
            try {
    //            Log.e("checkCurrentMedia","current before: ${PlayerVideoActivity.player?.currentMediaItemIndex}")
                val removedVideo = videoList[position]
                // Check if the removed item is currently playing
                if (position == currentPlayingPos) {
                    listener.onPlayingItemRemoved(removedVideo)
                }else {
                    videoList.removeAt(position)
                    notifyItemRemoved(position)
                    // Update the global LiveData
                    videoListLocal.value = videoList
                    Handler(Looper.getMainLooper()).postDelayed({
    //                    Log.e("checkCurrentMedia","current after: ${PlayerVideoActivity.player?.currentMediaItemIndex}")
                        notifyDataSetChanged() // Notify the adapter of data change
                    },500)
                }
                listvideos.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun moveItem(fromPosition: Int, toPosition: Int) {
            try {
    //            Log.e("checkCurrentMedia","current before: ${PlayerVideoActivity.player?.currentMediaItemIndex}")
                // Move item from one position to another
                val movedItem = videoList.removeAt(fromPosition)
                videoList.add(toPosition, movedItem)
                notifyItemMoved(fromPosition, toPosition)
                listvideos.clear()
                // Update the global LiveData
                videoListLocal.value = videoList
                Handler(Looper.getMainLooper()).postDelayed({
                    currentPlayingPos+1
    //                Log.e("checkCurrentMedia","current after: ${PlayerVideoActivity.player?.currentMediaItemIndex}")
                    notifyDataSetChanged() // Notify the adapter of data change
                },500)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/

    override fun getItemCount(): Int = videoList.size

    interface PlayListItemClickListener {
        fun onItemClick(position: Int, list: List<Video>)
        fun onPlayingItemRemoved(video: Video) // Hint when playing item is removed
        fun onbackpresscalled()
    }
}
