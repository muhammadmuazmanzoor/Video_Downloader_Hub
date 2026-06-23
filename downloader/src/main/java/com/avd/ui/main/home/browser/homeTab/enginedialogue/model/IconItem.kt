package com.avd.ui.main.home.browser.homeTab.enginedialogue.model

import androidx.annotation.DrawableRes

data class IconItem(
    @DrawableRes val iconResId: Int,
    val title: String,
    @DrawableRes val thumbId: Int=0,
)
