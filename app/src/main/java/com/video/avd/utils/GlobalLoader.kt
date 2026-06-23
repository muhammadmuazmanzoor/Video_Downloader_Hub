package com.video.avd.utils


import android.app.Activity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.video.avd.R

object GlobalLoader {

    var isLoaderShowing = false

    fun show(activity: Activity) {
        isLoaderShowing = true
        val loader = activity.findViewById<View>(R.id.globalLoader)
        loader?.visibility = View.VISIBLE
        val img = activity.findViewById<ImageView>(R.id.progressImage)
        img?.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.rotate))

    }

    fun hide(activity: Activity) {
        isLoaderShowing = false
        val loader = activity.findViewById<View>(R.id.globalLoader)
        loader?.visibility = View.GONE
    }
}


