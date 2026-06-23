package com.video.avd.utils.chromecast

import android.view.View
import com.google.android.gms.cast.framework.media.uicontroller.UIController

class MyCustomUIController(private val mView: View) : UIController() {
    override fun onMediaStatusUpdated() {
        super.onMediaStatusUpdated()
        mView.visibility = View.INVISIBLE
    }
}