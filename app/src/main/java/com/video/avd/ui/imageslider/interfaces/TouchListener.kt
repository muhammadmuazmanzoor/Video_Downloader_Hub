package com.video.avd.ui.imageslider.interfaces

import com.video.avd.ui.imageslider.constants.ActionTypes



interface TouchListener {
    /**
     * Click listener touched item function.
     *
     * @param  touched  slider boolean
     */
    fun onTouched(touched: ActionTypes, position: Int)
}