package com.video.avd.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
        val params = view.layoutParams as GridLayoutManager.LayoutParams
        val spanIndex = params.spanIndex
        val spanCount = layoutManager.spanCount

        // Adjust the left and right margins based on the item position
        if (spanIndex % spanCount == 0) {
            // This is the first item in a row
            outRect.right = space
        } else {
            // This is the second item in a row
            outRect.left = space
        }
    }
}
