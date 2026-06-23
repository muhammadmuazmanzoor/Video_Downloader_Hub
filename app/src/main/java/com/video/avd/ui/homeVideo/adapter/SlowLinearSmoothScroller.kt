package com.video.avd.ui.homeVideo.adapter

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller

class SlowLinearSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
        // Control the speed by adjusting the multiplier
        return 200f / displayMetrics.densityDpi // Adjust the value as needed
    }
}