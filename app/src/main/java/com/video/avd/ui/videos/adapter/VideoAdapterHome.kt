package com.video.avd.ui.videos.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.avd.util.AdBlockerHelper
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdOptionsView
import com.facebook.ads.MediaView
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdLayout
import com.facebook.ads.NativeAdListener
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAdView
import com.singular.sdk.Singular
import com.singular.sdk.SingularAdData
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.ads.MetaAds.nativeAdFacebook
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.databinding.CustomTemplateListBinding
import com.video.avd.databinding.ItemVideoBinding
import com.video.avd.databinding.ItemVideoGridBinding
import com.video.avd.databinding.MetaAdLayoutBinding
import com.video.avd.ui.videos.adapter.VideoAdapterViewHolders.VideoViewHolder
import com.video.avd.ui.videos.adapter.VideoAdapterViewHolders.VideoViewHolderGrid
import com.video.avd.ui.videos.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

//find a bug in this adapter

class VideoAdapterHome(var context: Context, var list: List<Video> = emptyList(), var isselect: Boolean = false, var listener: VideoAdapter.MenuClickListener? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var onClickFolder: VideoListner
    private var filteredVideos: MutableList<Video> = list.toMutableList()
    private val VIEW_TYPE_VIDEO = 0
    val VIEW_TYPE_AD = 1
    val VIEW_TYPE_AD_Meta = 2

    var isNativeAdLoaded = false
    var isMetaAdLoaded = false

    private val adPositions = setOf(6,18,30)
    val metaAdPositions = setOf(12,24)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
        val holder = VideoAdapterViewHolders(isselect, listener)
        return try {
            when (viewType) {
                VIEW_TYPE_VIDEO -> when (VIEW_TYPE.value) {
                    0 -> {
                        val binding = ItemVideoBinding.inflate(view, parent, false)
                        holder.VideoViewHolder(binding)
                    }
                    1 -> {
                        val binding = ItemVideoGridBinding.inflate(view, parent, false)
                        holder.VideoViewHolderGrid(binding)
                    }
                    else -> throw IllegalArgumentException("")
                }
                VIEW_TYPE_AD -> {
                        val adBinding = CustomTemplateListBinding.inflate(view, parent, false)
                        NativeAdViewHolderList(adBinding)
                }
                VIEW_TYPE_AD_Meta -> {
                    val adBinding = MetaAdLayoutBinding.inflate(view, parent, false)
                    NativeAdViewHolderLargeList(adBinding)
                }
                else -> throw IllegalArgumentException("")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val binding = ItemVideoBinding.inflate(view, parent, false)
            holder.VideoViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        return try {
            var totalItemCount = filteredVideos.size

            if (isNativeAdLoaded) {
                totalItemCount += adPositions.count { it < filteredVideos.size }
            }
            if (isMetaAdLoaded){
                totalItemCount += metaAdPositions.count { it < filteredVideos.size }
            }

            totalItemCount
        } catch (e: Exception) {
            e.printStackTrace()
            filteredVideos.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when  {
            isNativeAdLoaded && adPositions.contains(position) -> VIEW_TYPE_AD
            isMetaAdLoaded && metaAdPositions.contains(position) -> VIEW_TYPE_AD_Meta
            else -> VIEW_TYPE_VIDEO
        }

    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        try {
//            var adOffset = 0
//            var largeAdOffset = 0
//
//            if (isNativeAdLoaded) {
//                adPositions.forEach {
//                    if (position > it + adOffset) {
//                        adOffset++
//                    }
//                }
//            }
////            if (recyclerNative && isLargeAdLoaded) {
////                largeAdPositions.forEach {
////                    if (position > it + largeAdOffset + adOffset) {
////                        largeAdOffset++
////                    }
////                }
////            }
//
//            val videoIndex = position - adOffset - largeAdOffset
//            if (videoIndex < 0 || videoIndex >= filteredVideos.size) return  // Prevent crashes due to index errors
//
//            when (getItemViewType(position)) {
//                VIEW_TYPE_VIDEO -> {
//                    val item = filteredVideos[videoIndex]
//                    when (VIEW_TYPE.value) {
//                        0 -> {
//                            (holder as VideoViewHolder).bind(item, position)
//                            holder.binding.root.setOnClickListener {
//
//                                holder.binding.root.isEnabled = false
//                                holder.binding.root.postDelayed({
//                                    holder.binding.root.isEnabled = true
//                                }, 2000)
//
//                                if (::onClickFolder.isInitialized) {
//                                    onClickFolder.onVideoClick(
//                                        videoIndex.toString(),
//                                        filteredVideos as ArrayList<Video>
//                                    )
//                                }
//                            }
//                        }
//
//                        1 -> {
//                            (holder as VideoViewHolderGrid).bind(item, position)
//                            holder.binding.root.setOnClickListener {
//                                if (::onClickFolder.isInitialized) {
//                                    onClickFolder.onVideoClick(
//                                        videoIndex.toString(),
//                                        filteredVideos as ArrayList<Video>
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//
//                VIEW_TYPE_AD ->  (holder as NativeAdViewHolderList).bind()
////                    if (recyclerNative) {
////                    (holder as NativeAdViewHolderList).bind()
////                }
//
////                VIEW_TYPE_AD_LARGE -> if (recyclerNative) {
////                    (holder as NativeAdViewHolderLargeList).bind()
////                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            val videoIndex = getVideoIndex(position)
            if (videoIndex < 0 || videoIndex >= filteredVideos.size) return  // Prevent crashes due to index errors
            when (getItemViewType(position)) {
                VIEW_TYPE_VIDEO -> {

                    val item = filteredVideos[videoIndex]
                    if (videoIndex in filteredVideos.indices) {
                        when (VIEW_TYPE.value) {
                            0 -> {
                                (holder as VideoViewHolder).bind(item, position)
                                holder.binding.root.setOnClickListener {
                                    holder.binding.root.isEnabled = false
                                    holder.binding.root.postDelayed({
                                        holder.binding.root.isEnabled = true
                                    }, 2000)

                                    if (::onClickFolder.isInitialized) {
                                        onClickFolder.onVideoClick(
                                            videoIndex.toString(),
                                            filteredVideos as ArrayList<Video>
                                        )
                                    }
                                }
                            }
                            1 -> {
                                (holder as VideoViewHolderGrid).bind(item, position)
                                holder.binding.root.setOnClickListener {
                                    if (::onClickFolder.isInitialized) {
                                        onClickFolder.onVideoClick(
                                            videoIndex.toString(),
                                            filteredVideos as ArrayList<Video>
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                VIEW_TYPE_AD -> (holder as NativeAdViewHolderList).bind()

                VIEW_TYPE_AD_Meta ->{
                    Log.d("AdapterDebug", "Binding Meta Ad at position=$position")
                    (holder as NativeAdViewHolderLargeList).bind()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVideoIndex(position: Int): Int {
        var adOffset = 0
        if (isNativeAdLoaded) {
            adOffset += adPositions.count { it <= position }
        }
        if (isMetaAdLoaded){
            adOffset += metaAdPositions.count { it <= position }
        }
        return position - adOffset
    }

    fun setOnClickListner(onClickListner: VideoListner) {
        this.onClickFolder = onClickListner
    }

    fun filter(query: String) {
        filteredVideos.clear()
        if (query.isEmpty()) {
            filteredVideos.addAll(list)
        } else {
            for (item in list) {
                if (item.title?.lowercase(Locale.getDefault())
                        ?.contains(query.lowercase(Locale.getDefault())) == true
                ) {
                    filteredVideos.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Video>) {
        try {
            list = newList
            filteredVideos.clear()
            filteredVideos.addAll(newList)
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class NativeAdViewHolderList(var binding: CustomTemplateListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            if (AdsManager.nativeAdhigh != null){
                loadNativeListTemplate(binding.nativeAdView)
            }else{
                loadNativeListTemplateNor(binding.nativeAdView)
            }
        }
    }

    inner class NativeAdViewHolderLargeList(var binding: MetaAdLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            Log.d("AdapterDebug", "Meta bind called, ad null? ${nativeAdFacebook == null}")
                nativeAdFacebook?.let {
                    loadNativeListLargeTemplate(binding.root, it)
                } ?: run {
                    binding.root.visibility = View.GONE
                }
        }
    }

    fun loadNativeListLargeTemplate(view: View, nativeAd: NativeAd) {
        try {
            nativeAd.unregisterView()
            val nativeAdLayout = view as NativeAdLayout // 👈 cast safely here
            val nativeAdIcon: MediaView = view.findViewById(R.id.native_ad_icon)
            val nativeAdTitle: TextView = view.findViewById(R.id.native_ad_title)
            val nativeAdMedia: MediaView = view.findViewById(R.id.native_ad_media)
            val nativeAdBody: TextView = view.findViewById(R.id.native_ad_body)
            val nativeAdCallToAction: Button = view.findViewById(R.id.native_ad_call_to_action)
            val adChoicesContainer: LinearLayout = view.findViewById(R.id.ad_choices_container)
            // Set ad data
            nativeAdTitle.text = nativeAd.advertiserName
            nativeAdBody.text = nativeAd.adBodyText
            nativeAdCallToAction.text = nativeAd.adCallToAction
            nativeAdCallToAction.visibility = if (nativeAd.hasCallToAction()) View.VISIBLE else View.GONE
            // Clear and add AdOptionsView
            adChoicesContainer.removeAllViews()
            val adOptionsView = AdOptionsView(view.context, nativeAd,nativeAdLayout)
            adChoicesContainer.addView(adOptionsView)
            // Register views for interaction
            val clickableViews = listOf(nativeAdTitle, nativeAdCallToAction)
            nativeAd.registerViewForInteraction(
                view,
                nativeAdMedia,
                nativeAdIcon,
                clickableViews
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNativeListTemplate(adView: NativeAdView) {
                    try {
                        if (AdBlockerHelper.recycler_native) {
                            try {

                                // Ensure AdsManager.nativeAdhome is not null before accessing its properties
                                val nativeAd = AdBlockerHelper.nativeAdNow
                                adView.mediaView = adView.findViewById(R.id.ad_media)
                                adView.headlineView = adView.findViewById(R.id.ad_headline)
                                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

                                (adView.headlineView as TextView).text = nativeAd?.headline
                                (adView.callToActionView as Button).text = nativeAd?.callToAction
                                adView.mediaView?.mediaContent = nativeAd?.mediaContent

                                // Finally: must call this
                                nativeAd?.let {
                                    adView.setNativeAd(nativeAd)
                                }
                                // Optional: handle video
                                nativeAd?.mediaContent?.videoController?.let { vc ->
                                    if (vc.hasVideoContent()) {
                                        vc.videoLifecycleCallbacks =
                                            object : VideoController.VideoLifecycleCallbacks() {
                                                override fun onVideoEnd() {
                                                    super.onVideoEnd()
                                                }
                                            }
                                    }
                                }
                                Log.d("loadNativeListTemplate", "Home Populate nativeAd: ${nativeAd}")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("loadNativeListTemplate", "Error binding native ad: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

    }

    fun loadNativeListTemplateNor(adView: NativeAdView) {
        try {
            if (AdBlockerHelper.recycler_native) {
                try {
                    // Ensure AdsManager.nativeAdhome is not null before accessing its properties
                    val nativeAd = AdBlockerHelper.nativeAdNow
                    adView.mediaView = adView.findViewById(R.id.ad_media)
                    adView.headlineView = adView.findViewById(R.id.ad_headline)
                    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

                    (adView.headlineView as TextView).text = nativeAd?.headline
                    (adView.callToActionView as Button).text = nativeAd?.callToAction
                    adView.mediaView?.mediaContent = nativeAd?.mediaContent

                    // Finally: must call this
                    nativeAd?.let {
                        adView.setNativeAd(nativeAd)
                    }
                    // Optional: handle video
                    nativeAd?.mediaContent?.videoController?.let { vc ->
                        if (vc.hasVideoContent()) {
                            vc.videoLifecycleCallbacks =
                                object : VideoController.VideoLifecycleCallbacks() {
                                    override fun onVideoEnd() {
                                        super.onVideoEnd()
                                    }
                                }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("loadNativeListTemplate", "Error binding native ad: ${e.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}


