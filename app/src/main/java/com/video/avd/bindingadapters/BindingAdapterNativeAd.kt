package com.video.avd.bindingadapters

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.google.android.gms.ads.MediaContent
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.databinding.ItemNativeAdNewBinding

@BindingAdapter("loadNativeIconView")
fun loadNativeIconView(view: ImageView, drawable: Drawable?) {
    drawable?.let {
        Glide.with(view.context)
            .asDrawable()
            .load(it)
            .into(view)
    }
}

@BindingAdapter("invisibleView")
fun invisibleView(view: View, isInvisible: Boolean) {
    view.visibility = if (isInvisible) {
        View.VISIBLE
    } else {
        View.INVISIBLE
    }
}

@BindingAdapter("nativeAd", "mediaContent")
fun setNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd?, mediaContent: MediaContent?) {
    val binding = ItemNativeAdNewBinding.bind(nativeAdView)
    nativeAdView.mediaView = binding.adMedia
    mediaContent?.let { nativeAdView.mediaView?.setMediaContent(it) }
    nativeAd?.let {
        nativeAdView.setNativeAd(it)
    }
}

