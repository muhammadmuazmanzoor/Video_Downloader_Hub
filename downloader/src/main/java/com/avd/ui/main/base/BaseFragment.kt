package com.avd.ui.main.base

import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.google.android.material.color.MaterialColors
import com.avd.R

abstract class BaseFragment : Fragment() {
    fun fixPopup(container: FrameLayout, popupSource: View): View {
        val myView = View(container.context)

        val params = RelativeLayout.LayoutParams(30, 40)
        val location = IntArray(2)
        popupSource.getLocationOnScreen(location)
        myView.visibility = View.INVISIBLE
        val x = location[0]
        val y = location[1]
        params.topMargin = y
        params.leftMargin = x
        myView.layoutParams = params

        container.removeAllViews()
        container.addView(myView)

        return myView
    }

    fun getThemeBackgroundColor(): Int {
        val color = MaterialColors.getColor(requireContext(), R.attr.colorSurface, Color.WHITE)
        return color
    }
}