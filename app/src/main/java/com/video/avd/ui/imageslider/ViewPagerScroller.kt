package com.video.avd.ui.imageslider

import android.content.Context
import android.view.animation.Interpolator
import android.widget.Scroller



class ViewPagerScroller : Scroller {

    var fixedDuration = 1000 //time to scroll in milliseconds

    constructor(context: Context) : super(context)

    constructor(context: Context, interpolator: Interpolator) : super(context, interpolator)

    constructor(context: Context, interpolator: Interpolator, flywheel: Boolean) : super(context, interpolator, flywheel)


    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
        super.startScroll(startX, startY, dx, dy, fixedDuration)
    }

    override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        super.startScroll(startX, startY, dx, dy, fixedDuration)
    }
}