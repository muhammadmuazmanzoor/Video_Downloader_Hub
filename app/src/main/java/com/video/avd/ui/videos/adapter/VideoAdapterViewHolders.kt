package com.video.avd.ui.videos.adapter

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.BaseRequestOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.video.avd.databinding.ItemVideoBinding
import com.video.avd.databinding.ItemVideoGridBinding
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils.calculateProgress
import com.video.avd.utils.AppUtils.parseDurationString

class VideoAdapterViewHolders(private val isselect : Boolean, private val listener : VideoAdapter.MenuClickListener?=null) {
    var cropOptions: RequestOptions? = null
    init {
        cropOptions = RequestOptions().centerCrop()
    }

   inner  class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: Video, position: Int) {
                binding.apply  {
                    name.text = item.title
                    // Update your UI with the formatted durationString
                    //Will use latter, time/size (don't remove)
                    time.text = item.duration
                    date.text= item.date
                    size.text= item.size
                    videoProgressBar.progress=calculateProgress(item.lastPlayed,parseDurationString(item.duration))
                    newtag.visibility = if (item.isNew) View.VISIBLE else View.GONE
                    try {
                        Glide.with(imageView.context)
                            .load(item.contentUri)
                            .apply(cropOptions as BaseRequestOptions<*>)
                            // Resize the image if necessary
                            .override(imageView.width, imageView.height)
                            .transition(DrawableTransitionOptions.withCrossFade(500)) // Shortened the crossfade duration
                            .into(imageView)
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                    if (isselect) {
                        videoMenu.visibility = View.GONE
                    }

                    videoMenu.setOnClickListener {
                        videoMenu.isClickable = false
                        listener?.onMenuClick(item, position)
                        // Re-enable click after 500ms
                        videoMenu.postDelayed({ videoMenu.isClickable = true }, 500)
                    }

                }
        }
    }

     inner  class VideoViewHolderGrid(val binding: ItemVideoGridBinding) : RecyclerView.ViewHolder(binding.root) {
         @RequiresApi(Build.VERSION_CODES.O)
         fun bind(item: Video, position: Int) {
             binding.let {
                 it.name.text = item.title
                 // Update your UI with the formatted durationString
                 it.time.text = item.duration
                 it.date.text= item.date
                 it.size.text= item.size
                 it.videoProgressBar.progress=calculateProgress(item.lastPlayed,parseDurationString(item.duration))
                 try {
                     Glide.with(it.imageView.context)
                         .load(item.contentUri)
                         .listener(object : RequestListener<Drawable?> {
                             override fun onLoadFailed(
                                 e: GlideException?,
                                 model: Any?,
                                 target: Target<Drawable?>,
                                 isFirstResource: Boolean
                             ): Boolean {
                                 return false
                             }

                             override fun onResourceReady(
                                 resource: Drawable,
                                 model: Any,
                                 target: Target<Drawable?>?,
                                 dataSource: DataSource,
                                 isFirstResource: Boolean
                             ): Boolean {
                                 return false
                             }


                         })
                         .transition(DrawableTransitionOptions.withCrossFade(1000))
                         .apply(cropOptions as BaseRequestOptions<*>)
                         .into(it.imageView)
                 } catch (e: IllegalStateException) {
                     e.printStackTrace()
                 }
                 if (isselect) {
                     it.videoMenu.visibility = View.GONE
                 }
                 it.videoMenu.setOnClickListener {
                     listener?.onMenuClick(item, position)
                 }
             }
         }
     }



}