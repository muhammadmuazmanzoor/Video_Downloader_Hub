package com.video.avd.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.avd.util.AdBlockerHelper
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
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.databinding.CustomTemplateListBinding
import com.video.avd.databinding.WatchHistoryFragmentItemBinding
import com.video.avd.databinding.WatchHistoryFragmentItemGridBinding
import com.video.avd.ui.videos.model.Video


class WatchHistoryRecyclerView(
    var list: List<Video>,
    var context: Context,
    private var listener: OnHistoryCardClickListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_VIDEO = 0
    val VIEW_TYPE_AD = 1
    var isNativeAdLoaded = false


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
        val holder = HistoryAdapterViewHolders(listener)
        if (viewType == VIEW_TYPE_VIDEO) {
            return when (VIEW_TYPE.value) {
                0 -> {
                    val binding = WatchHistoryFragmentItemBinding.inflate(view, parent, false)
                    holder.HistoryViewHolder(binding)
                }

                1 -> {
                    val binding = WatchHistoryFragmentItemGridBinding.inflate(view, parent, false)
                    holder.HistoryViewHolderGrid(binding)
                }

                else -> {
                    val binding = WatchHistoryFragmentItemBinding.inflate(view, parent, false)
                    holder.HistoryViewHolder(binding)
                }
            }
        } else {
            val adBinding = CustomTemplateListBinding.inflate(view, parent, false)
            return NativeAdViewHolderList(adBinding)
        }
    }


    override fun getItemCount(): Int {
        return try {
            if (isNativeAdLoaded) list.size + 1 else list.size
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            if (getItemViewType(position) == VIEW_TYPE_VIDEO) {
                val adjustedPosition = getAdjustedPosition(position) // Adjust the position
                val item = list[adjustedPosition]
                when (VIEW_TYPE.value) {
                    0 -> {
                        (holder as HistoryAdapterViewHolders.HistoryViewHolder)
                        holder.bind(item, list, adjustedPosition)
                        holder.binding.historyLayout.setOnClickListener {
                            listener?.onCardClick(adjustedPosition, list)
                        }
                    }

                    1 -> {
                        (holder as HistoryAdapterViewHolders.HistoryViewHolderGrid)
                        holder.bind(item, list, adjustedPosition)
                    }
                }
            } else {
                (holder as NativeAdViewHolderList).bind()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAdjustedPosition(position: Int): Int {
        // Subtract 1 from the position if the ad is loaded and the position is greater than 0
        return if (isNativeAdLoaded && position > 0) position - 1 else position
    }

    fun updateList(newList: List<Video>) {
        try {
            val diffResult = DiffUtil.calculateDiff(VideoDiffUtil(list, newList))
            list = newList
            diffResult.dispatchUpdatesTo(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemViewType(position: Int): Int {
        // return VIEW_TYPE.value ?: 0
        return if (isNativeAdLoaded && position == 0) VIEW_TYPE_AD else VIEW_TYPE_VIDEO
    }


    inner class VideoDiffUtil(
        private val oldList: List<Video>,
        private val newList: List<Video>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    interface OnHistoryCardClickListener {
        fun onCardClick(position: Int, entities: List<Video>)
        fun onDeleteClick(entities: Video)
    }

    private fun setCustomMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    inner class NativeAdViewHolderList(var binding: CustomTemplateListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            loadNativeListTemplate(binding.nativeAdView)
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
                    adView.bodyView = adView.findViewById(R.id.ad_body)
                    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

                    (adView.headlineView as TextView).text = nativeAd?.headline
                    (adView.bodyView as TextView).apply {
                        if (nativeAd?.body.isNullOrBlank()) {
                            visibility = View.GONE
                        } else {
                            visibility = View.VISIBLE
                            text = nativeAd?.body
                        }
                    }
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


    @SuppressLint("NotifyDataSetChanged")
    fun loadNativeAd(context: Context) {
        try {
            if (AdsManager.nativeAd == null){
                val adLoader = AdLoader.Builder(context,context.resources.getString(R.string.Native_static))
                    .forNativeAd { nativeAd ->
                        Log.d("nativeAd", "loaded")
                        AdsManager.nativeAd = nativeAd
                        isNativeAdLoaded = true
                        notifyDataSetChanged()
                        AdsManager.nativeAd?.setOnPaidEventListener {
                            val impressionData: AdValue = it
                            val data = SingularAdData(
                                "AdMob",
                                impressionData.currencyCode,
                                impressionData.valueMicros / 1000000.0)
                            Singular.adRevenue(data)
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isNativeAdLoaded = false
                            Log.d("nativeAd", "not loaded")
                        }
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            }else{
                isNativeAdLoaded = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
