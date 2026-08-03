package com.video.avd.ui.folder.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
import com.video.avd.ads.AdsManager.recyclerNative
import com.video.avd.ads.MetaAds.nativeAdFacebook
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.databinding.CustomTemplateListBinding
import com.video.avd.databinding.ItemFolderGridVideoBinding
import com.video.avd.databinding.ItemFolderVideoBinding
import com.video.avd.databinding.MetaAdLayoutBinding
import com.video.avd.ui.folder.model.VideoFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class FolderAdapter(var list: List<VideoFolder>, var context: Context) : RecyclerView.Adapter<ViewHolder>() {

    private lateinit var onClickFolder: OnClickListner
    private var filteredVideos: MutableList<VideoFolder> = list.toMutableList()

    private val VIEW_TYPE_VIDEO = 0
    val VIEW_TYPE_AD = 1
    val VIEW_TYPE_AD_Meta = 2
    var isNativeAdLoaded = false
    var isMetaAdLoaded = false

    private val baseAdPositions = listOf(0, 16, 32)
    private val baseMetaAdPositions = listOf(8, 24)

    private val activeAdPositions: List<Int> get() = if (isNativeAdLoaded) { baseAdPositions.filter { it < filteredVideos.size } } else emptyList()

    private val activeMetaAdPositions: List<Int> get() = if (isMetaAdLoaded) {
            baseMetaAdPositions.filter { it < filteredVideos.size }
        } else emptyList()

    private var privateVideosCount = 0

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun setPrivateVideosCount(count : Int){
        privateVideosCount = count
        notifyItemChanged(1)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
        val holder = FolderAdapterViewHolders()
        return try {
            when (viewType) {
                VIEW_TYPE_VIDEO -> when (VIEW_TYPE.value) {
                    0 -> {
                        val binding = ItemFolderVideoBinding.inflate(view, parent, false)
                        holder.FolderViewHolder(binding)
                    }
                    1 -> {
                        val binding = ItemFolderGridVideoBinding.inflate(view, parent, false)
                        holder.FolderViewHolderGrid(binding)
                    }
                    else -> throw IllegalArgumentException("Unsupported VIEW_TYPE.value")
                }
                VIEW_TYPE_AD -> {

                        val adBinding = CustomTemplateListBinding.inflate(view, parent, false)
                        NativeAdViewHolderList(adBinding)
                }
                VIEW_TYPE_AD_Meta -> {
                      val adBinding = MetaAdLayoutBinding.inflate(view, parent, false)
                        NativeAdViewHolderLargeList(adBinding)
                }
                else -> {
                    when (VIEW_TYPE.value) {
                        0 -> {
                            Log.d("FolderAdapter", "Inflating video folder layout (List view)")
                            val binding = ItemFolderVideoBinding.inflate(view, parent, false)
                            holder.FolderViewHolder(binding)
                        }
                        1 -> {
                            Log.d("FolderAdapter", "Inflating video folder layout (Grid view)")
                            val binding = ItemFolderGridVideoBinding.inflate(view, parent, false)
                            holder.FolderViewHolderGrid(binding)
                        }
                        else -> throw IllegalArgumentException("Unsupported VIEW_TYPE.value")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val binding = ItemFolderVideoBinding.inflate(view, parent, false)
            holder.FolderViewHolder(binding)
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return try {
            var totalItemCount = filteredVideos.size
            var regularAdOffset = 0
            var metaAdOffset = 0

            if (isNativeAdLoaded) {
                regularAdOffset = activeAdPositions.count { it < filteredVideos.size }
                totalItemCount += regularAdOffset
            }

            if (isMetaAdLoaded) {
                metaAdOffset = activeMetaAdPositions.count {
                    it < (filteredVideos.size + regularAdOffset)
                }
                totalItemCount += metaAdOffset
            }

            // Optional: debug consecutive ads
            if (isNativeAdLoaded || isMetaAdLoaded) {
                val allAdPositions = (if (isNativeAdLoaded) activeAdPositions else emptyList()) +
                        (if (isMetaAdLoaded) activeMetaAdPositions else emptyList())
                val sorted = allAdPositions.sorted()
                for (i in 1 until sorted.size) {
                    if (sorted[i] == sorted[i - 1] + 1) {
                        Log.w("FolderAdapter", "Consecutive ads at ${sorted[i - 1]}, ${sorted[i]}")
                    }
                }
            }

            Log.d("FolderAdapter", "Final getItemCount: $totalItemCount")
            totalItemCount
        } catch (e: Exception) {
            e.printStackTrace()
            filteredVideos.size
        }
    }


    override fun getItemViewType(position: Int): Int {
        Log.d("FolderAdapter", "checking view type at $position — adPositions=$activeAdPositions, isNativeAdLoaded=$isNativeAdLoaded")

        if (position >= itemCount || position < 0) {
            Log.e("FolderAdapter", "Invalid position=$position, itemCount=$itemCount")
            return VIEW_TYPE_VIDEO
        }

        val adOffset = activeAdPositions.count { isNativeAdLoaded && it < position }
        val metaOffset = activeMetaAdPositions.count { isMetaAdLoaded && it < position - adOffset }
        val videoIndex = position - adOffset - metaOffset

        return when {
            recyclerNative && isMetaAdLoaded && nativeAdFacebook != null && nativeAdFacebook?.isAdLoaded == true && activeMetaAdPositions.contains(videoIndex) -> {
                if (position > 0 && isPrevAd(position)) VIEW_TYPE_VIDEO
                else {
                    Log.d("FolderAdapter", "→ Returning VIEW_TYPE_AD_Meta at position=$position (videoIndex=$videoIndex)")
                    VIEW_TYPE_AD_Meta
                }
            }
            recyclerNative && isNativeAdLoaded && activeAdPositions.contains(videoIndex) -> {
                if (position > 0 && isPrevAd(position)) VIEW_TYPE_VIDEO
                else {
                    Log.d("FolderAdapter", "→ Returning VIEW_TYPE_AD at position=$position (videoIndex=$videoIndex)")
                    VIEW_TYPE_AD
                }
            }
            else -> {
                Log.d("FolderAdapter", "→ Returning VIEW_TYPE_VIDEO at position=$position (videoIndex=$videoIndex)")
                VIEW_TYPE_VIDEO
            }
        }
    }

    private fun isPrevAd(position: Int): Boolean {
        val prevIndex = getVideoIndex(position - 1)
        return activeAdPositions.contains(prevIndex) || activeMetaAdPositions.contains(prevIndex)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= itemCount) {
            Log.e("FolderAdapter", "❌ Skipping bind: Invalid position=$position, itemCount=$itemCount")
            return
        }

        val viewType = getItemViewType(position)
        Log.d("FolderAdapter", "onBindViewHolder at position=$position, viewType=$viewType")
        when (viewType) {
            VIEW_TYPE_VIDEO -> {
                val videoIndex = getVideoIndex(position)
                if (videoIndex == -1) return
                if (videoIndex !in 0 until filteredVideos.size) return  // Prevent crash
                Log.d("FolderAdapter", "Binding video at position=$position, videoIndex=$videoIndex")
                if (videoIndex >= filteredVideos.size) return
                val item = filteredVideos[videoIndex]
                when (holder) {
                    is FolderAdapterViewHolders.FolderViewHolder -> {
                        Log.d("FolderAdapter", "Binding list holder for ${item.name}")
                        holder.bind(item, privateVideosCount)
                        holder.binding.root.setOnClickListener {
                            onClickFolder.onClickListner(item.id.toString(), item.name)
                        }
                    }
                    is FolderAdapterViewHolders.FolderViewHolderGrid -> {
                        Log.d("FolderAdapter", "Binding grid holder for ${item.name}")
                        holder.bind(item, privateVideosCount)
                        holder.binding.root.setOnClickListener {
                            onClickFolder.onClickListner(item.id.toString(), item.name)
                        }
                    }
                    else -> {
                        Log.e(
                            "FolderAdapter",
                            "Unexpected holder=${holder::class.java.simpleName} at position=$position"
                        )
                    }
                }
            }
            VIEW_TYPE_AD -> {
                if (recyclerNative) {
                    Log.d("FolderAdapter", "Binding ad view at position=$position")
                    (holder as NativeAdViewHolderList).bind()
                }
            }
            VIEW_TYPE_AD_Meta -> {
                if (recyclerNative && nativeAdFacebook != null && nativeAdFacebook?.isAdLoaded == true) {
                    Log.d("NativeAdsMeta", "Binding meta ad at position=$position")
                    (holder as NativeAdViewHolderLargeList).bind()
                } else {
                    Log.w("NativeAdsMeta", "Ad not ready at position=$position, skipping binding")
                }
            }
        }
    }


    private fun getVideoIndex(position: Int): Int {
        var adOffset = 0
        var metaOffset = 0

        activeAdPositions.forEach {
            if (it + adOffset < position) adOffset++
        }

        activeMetaAdPositions.forEach {
            if (it + adOffset + metaOffset < position) metaOffset++
        }

        val index = position - adOffset - metaOffset
        return if (index in filteredVideos.indices) index else -1
    }


    fun setOnClickListner(onClickListner: OnClickListner) {
        this.onClickFolder = onClickListner
        Log.d("FolderAdapter", "Click listener set")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        Log.d("FolderAdapter", "Filter called with query: $query")
        filteredVideos.clear()
        if (query.isEmpty()) {
            filteredVideos.addAll(list)
        } else {
            for (item in list) {
                if (item.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                ) {
                    filteredVideos.add(item)
                }
            }
        }
        Log.d("FolderAdapter", "Filtered list size: ${filteredVideos.size}")
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<VideoFolder>) {
        this.list = list
        filteredVideos = list.toMutableList()
        notifyDataSetChanged()
    }

    inner class NativeAdViewHolderList(var binding: CustomTemplateListBinding) : RecyclerView.ViewHolder(binding.root ) {
        fun bind() {
            if (AdsManager.nativeAdhigh != null){
                loadNativeListTemplate(binding.nativeAdView)
            }else{
                loadNativeListTemplateNor(binding.nativeAdView)
            }
        }
    }

    inner class NativeAdViewHolderLargeList(var binding: MetaAdLayoutBinding) : RecyclerView.ViewHolder(  binding.root ) {
        fun bind() {
            nativeAdFacebook?.let { loadNativeListLargeTemplate(binding.root, it) }
        }
    }

    fun loadNativeListLargeTemplate(view: View, nativeAd: NativeAd) {
        try {
            if (!nativeAd.isAdLoaded) {
                Log.e("NativeAdsMeta", "Ad is not loaded yet. Skipping view registration.")
                return
            }
            nativeAd.unregisterView()
            val nativeAdLayout = view as NativeAdLayout // 👈 cast safely here
            val nativeAdIcon: ImageView = view.findViewById(R.id.native_ad_icon)
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
                    Log.d("loadNativeListTemplate", "Folder Populate nativeAd: ${nativeAd}")
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

