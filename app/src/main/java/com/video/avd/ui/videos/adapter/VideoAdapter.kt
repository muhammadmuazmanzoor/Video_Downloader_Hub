package com.video.avd.ui.videos.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
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
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.ads.AdsManager.recyclerNative
import com.video.avd.ads.MetaAds
import com.video.avd.ads.MetaAds.nativeAdFacebook
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.databinding.CustomTemplateListBinding
import com.video.avd.databinding.ItemVideoBinding
import com.video.avd.databinding.ItemVideoGridBinding
import com.video.avd.databinding.MetaAdLayoutBinding
import com.video.avd.databinding.NativeAdApplovinsmallBinding
import com.video.avd.databinding.NativeAdSmallBinding
import com.video.avd.ui.videos.adapter.VideoAdapterViewHolders.VideoViewHolder
import com.video.avd.ui.videos.adapter.VideoAdapterViewHolders.VideoViewHolderGrid
import com.video.avd.ui.videos.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class VideoAdapter(
    var context: Context,
    var list: List<Video> = emptyList(),
    var isselect: Boolean = false,
    var listener: MenuClickListener? = null,

    val showAd: Boolean = true,
    val isAllVideoFragment: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var onClickFolder: VideoListner
    private var filteredVideos: MutableList<Video> = list.toMutableList()

    companion object {
        private const val VIEW_TYPE_CONTENT = 0
        const val VIEW_TYPE_ADMOB = 1
        const val VIEW_TYPE_META = 2
        private const val AD_SPACING = 4      // 1 ad + 3 content rows
    }


    var isNativeAdLoaded = false
    var isLargeAdLoaded = false

    /* --------‑ Helper: is *loaded* ad slot? -------- */
    private fun isAdLoadedAt(position: Int): Boolean =
        when (position / AD_SPACING % 2) {       // even slots = AdMob, odd = Meta
            0 -> isAdmobReady()
            else -> isMetaReady()
        }

    /* ---------- ViewHolder creation ---------- */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {

            VIEW_TYPE_ADMOB -> {
                val view = LayoutInflater.from(parent.context)
                val holder = VideoAdapterViewHolders(isselect, listener)
                if (AdBlockerHelper.recycler_native && AdBlockerHelper.isProVersion.value!=true) {
                    val adBindingnativeSmall =
                        NativeAdSmallBinding.inflate(view, parent, false)
                    Log.d("loadNativeListTemplate", "adBindingnativeSmall: ${AdBlockerHelper.recycler_native}")
                    NativeAdViewHolderList(adBindingnativeSmall)

                } else {
                    when (VIEW_TYPE.value) {
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
                }
            }

            VIEW_TYPE_META -> {
                val view = LayoutInflater.from(parent.context)
                val holder = VideoAdapterViewHolders(isselect, listener)
                if (recyclerNative) {
                    val adBinding = MetaAdLayoutBinding.inflate(view, parent, false)
                    NativeAdViewHolderLargeList(adBinding)
                } else {
                    when (VIEW_TYPE.value) {
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
                }

            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                val holder = VideoAdapterViewHolders(isselect, listener)
                when (VIEW_TYPE.value) {
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
            }
        }

    /* --------‑ Item count = data + loaded ads only -------- */
    override fun getItemCount(): Int {
        var count = filteredVideos.size
        if (showAd) {
            if (isAdmobReady()) count++
            if (isMetaReady()) count++
        }
        // Meta  row at position 8
        return count
    }
    /* ---------- View‑type logic ---------- */

    /* --------‑ View‑type logic -------- */
    override fun getItemViewType(position: Int): Int {
        if (!showAd) return VIEW_TYPE_CONTENT

        val isAdSlot = position % AD_SPACING == 0          // 0,8,16,…
        if (!isAdSlot) return VIEW_TYPE_CONTENT
        val adIndex = position / AD_SPACING                // 0,1,2,…
        return if (adIndex % 2 == 0) {                      // even  = AdMob slot
            if (isAdmobReady()) VIEW_TYPE_ADMOB else VIEW_TYPE_CONTENT
        } else {                                            // odd   = Meta slot
            if (isMetaReady()) VIEW_TYPE_META else VIEW_TYPE_CONTENT   // <- ***
        }
    }


    private fun isMetaReady() =
        MetaAds.nativeAdFacebook?.isAdLoaded == true

    private fun isAdmobReady() =
        AdBlockerHelper.nativeAdNow != null

    /* --------‑ Map adapter → data index -------- */
    private fun dataIndex(adapterPos: Int): Int {
        // count loaded ads *before* this position
        var loadedAdsBefore = 0
        var p = 0
        while (p < adapterPos) {
            if (p % AD_SPACING == 0 && isAdLoadedAt(p)) loadedAdsBefore++
            p += 1
        }
        return adapterPos - loadedAdsBefore
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        // 🔹 If ads are disabled, skip ad view binding completely
        if (!showAd) {
            when (holder) {
                is VideoViewHolder -> {
                    val idx = position // no need for dataIndex() when no ads
                    if (idx !in filteredVideos.indices) return
                    val item = filteredVideos[idx]
                    holder.bind(item, position)
                    holder.binding.root.setOnClickListener {
                        val realPos = holder.bindingAdapterPosition
                        if (realPos == RecyclerView.NO_POSITION) return@setOnClickListener
                        holder.binding.root.isEnabled = false
                        holder.binding.root.postDelayed({
                            holder.binding.root.isEnabled = true
                        }, 2000)
                        if (::onClickFolder.isInitialized) {
                            onClickFolder.onVideoClick(
                                realPos.toString(),
                                filteredVideos as ArrayList<Video>
                            )
                        }
                    }
                }

                is VideoViewHolderGrid -> {
                    val idx = position
                    if (idx !in filteredVideos.indices) return
                    val item = filteredVideos[idx]
                    holder.bind(item, position)
                    holder.binding.root.setOnClickListener {
                        val realPos = holder.bindingAdapterPosition
                        if (realPos == RecyclerView.NO_POSITION) return@setOnClickListener
                        if (::onClickFolder.isInitialized) {
                            onClickFolder.onVideoClick(
                                realPos.toString(),
                                filteredVideos as ArrayList<Video>
                            )
                        }
                    }
                }
            }
            return // 🚫 stop here, no ad binding
        }

        // 🔹 When ads are enabled (for other fragments)
        when (holder) {
            is VideoViewHolder -> {
                val idx = dataIndex(position)
                if (idx !in filteredVideos.indices) return
                val item = filteredVideos[idx]
                holder.bind(item, position)
                holder.binding.root.setOnClickListener {
                    val realPos = holder.bindingAdapterPosition
                    if (realPos == RecyclerView.NO_POSITION) return@setOnClickListener
                    holder.binding.root.isEnabled = false
                    holder.binding.root.postDelayed({
                        holder.binding.root.isEnabled = true
                    }, 2000)
                    val videoIdx = dataIndex(realPos)
                    if (videoIdx !in filteredVideos.indices) return@setOnClickListener
                    if (::onClickFolder.isInitialized) {
                        onClickFolder.onVideoClick(
                            videoIdx.toString(),
                            filteredVideos as ArrayList<Video>
                        )
                    }
                }
            }

            is VideoViewHolderGrid -> {
                val idx = dataIndex(position)
                if (idx !in filteredVideos.indices) return
                val item = filteredVideos[idx]
                val realPos = holder.bindingAdapterPosition
                holder.binding.root.isEnabled = false
                holder.binding.root.postDelayed({
                    holder.binding.root.isEnabled = true
                }, 2000)
                val videoIdx = dataIndex(realPos)
                holder.bind(item, position)
                holder.binding.root.setOnClickListener {
                    if (realPos == RecyclerView.NO_POSITION) return@setOnClickListener
                    if (::onClickFolder.isInitialized) {
                        onClickFolder.onVideoClick(
                            videoIdx.toString(),
                            filteredVideos as ArrayList<Video>
                        )
                    }
                }
            }

            // 🟩 Ad ViewHolders only bind if ads enabled
            is NativeAdViewHolderList -> if (AdBlockerHelper.recycler_native && AdBlockerHelper.isProVersion.value!=true) {
                holder.bind()
            }

            is NativeAdViewHolderLargeList -> if (AdBlockerHelper.recycler_native && AdBlockerHelper.isProVersion.value!=true) {
                if (nativeAdFacebook != null && nativeAdFacebook?.isAdLoaded == true) {
                    Log.d("NativeAdsMeta", "Binding meta ad at position=$position")
                    holder.bind()
                } else {
                    Log.w("NativeAdsMeta", "Ad not ready at position=$position, skipping binding")
                }
            }
        }
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

    interface MenuClickListener {
        fun onMenuClick(item: Video, position: Int)
    }

    inner class NativeAdViewHolderList(
        var bindingNativeSmall: NativeAdSmallBinding
    ) :
        RecyclerView.ViewHolder(bindingNativeSmall.root) {
        fun bind() {
            Log.d("loadNativeListTemplate", "Bind NAtive")
            loadNativeListTemplate(bindingNativeSmall.nativeAdView)
        }
    }

    inner class NativeAdViewHolderLargeList(var binding: MetaAdLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            Log.d("loadNativeListTemplate", "Meta bind called, ad null? ${nativeAdFacebook == null}")
            nativeAdFacebook?.let {
                loadNativeListLargeTemplate(binding.root, it)
            } ?: run {
                binding.root.visibility = View.GONE
            }
        }
    }

    fun loadNativeListLargeTemplate(view: View, nativeAd: NativeAd) {
        if (!showAd) return
        try {
            if (!nativeAd.isAdLoaded) {
                Log.e("loadNativeListTemplate", "Ad is not loaded yet. Skipping view registration.")
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
            nativeAdCallToAction.visibility =
                if (nativeAd.hasCallToAction()) View.VISIBLE else View.GONE
            // Clear and add AdOptionsView
            adChoicesContainer.removeAllViews()
            val adOptionsView = AdOptionsView(view.context, nativeAd, nativeAdLayout)
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
        if (!AdBlockerHelper.recycler_native || AdBlockerHelper.isProVersion.value==true) {
            Log.d("loadNativeListTemplate", "RecyclerNative is false, skipping ad load")
            return
        }

        try {
            val nativeAd = AdBlockerHelper.nativeAdNow

            if (nativeAd == null) {
                Log.e("loadNativeListTemplate", "nativeAdNow is null")
                return
            }

            val mediaView = adView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)
            val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
            val ad_body = adView.findViewById<TextView>(R.id.ad_body)
            val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
            adView.mediaView = mediaView
            adView.headlineView = headlineView
            adView.bodyView = ad_body
            adView.callToActionView = ctaView
            headlineView.text = nativeAd.headline
            headlineView.visibility =
                if (nativeAd.headline.isNullOrBlank()) View.GONE else View.VISIBLE

            if (nativeAd.callToAction.isNullOrBlank()) {
                ctaView.visibility = View.GONE
            } else {
                ctaView.visibility = View.VISIBLE
                ctaView.text = nativeAd.callToAction
            }

            mediaView?.mediaContent = nativeAd.mediaContent
            mediaView?.visibility =
                if (nativeAd.mediaContent == null) View.GONE else View.VISIBLE
            if (nativeAd.body.isNullOrBlank()) {
                ad_body.visibility = View.GONE
            } else {
                ad_body.visibility = View.VISIBLE
                ad_body.text = nativeAd.body
            }

            adView.setNativeAd(nativeAd)

            nativeAd.mediaContent?.videoController?.let { videoController ->
                if (videoController.hasVideoContent()) {
                    videoController.videoLifecycleCallbacks =
                        object : VideoController.VideoLifecycleCallbacks() {
                            override fun onVideoEnd() {
                                super.onVideoEnd()
                            }
                        }
                }
            }

            Log.d("loadNativeListTemplate", "Native ad populated successfully")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("loadNativeListTemplate", "Error binding native ad: ${e.message}")
        }
    }
}


