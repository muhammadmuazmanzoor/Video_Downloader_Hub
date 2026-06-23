package com.xilli.hd.android.wallpapersmaker.bindingadapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

@BindingAdapter("bindVisibility")
fun bindVisibility(view: View, isGone: Boolean) {
    view.visibility = if (isGone) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

@BindingAdapter("loadImageDrawable")
fun loadImageDrawable(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        Glide.with(view.context)
            .asDrawable()
            .load(imageUrl)
            .into(view)
    }
}

@BindingAdapter("loadDrawableAsResource")
fun loadDrawableAsResource(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        Glide.with(view.context)
            .asDrawable()
            .load(imageUrl)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    view.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }
}

@BindingAdapter("loadImageUri")
fun loadImageUri(view: ImageView, imageUri: Uri?) {
    if (imageUri != null) {
        Glide.with(view.context)
            .asDrawable()
            .load(imageUri)
            .into(view)
    }
}


@BindingAdapter("loadReducedDrawable")
fun loadReducedDrawable(view: ImageView, imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        Glide.with(view.context)
            .asDrawable()
            .override(500)
            .load(imageUrl)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    view.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }
}

@BindingAdapter("loadIntAsDrawable")
fun loadIntAsDrawable(view: ImageView, drawable: Int) {
    if (drawable != null) {
        Glide.with(view.context)
            .asDrawable()
            .load(drawable)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    view.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }
}

@BindingAdapter("loadBitmapToImage")
fun loadBitmapToImage(view: ImageView, bitmap: Bitmap?) {
    if (bitmap != null) {
        Glide.with(view.context)
            .asBitmap()
            .override(500)
            .load(bitmap)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    view.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }
}