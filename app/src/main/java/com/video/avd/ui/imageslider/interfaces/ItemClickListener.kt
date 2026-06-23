package com.video.avd.ui.imageslider.interfaces

interface ItemClickListener {
    /**
     * Click listener selected item function.
     *
     * @param  position  selected item position
     */
    fun onItemSelected(position: Int)

    /**
     * Click listener double click item function.
     *
     * @param  position  selected item position
     */
    fun doubleClick(position: Int)
}