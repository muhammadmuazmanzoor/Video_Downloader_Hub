package com.video.avd.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.video.avd.R


class CustomButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        background = resources.getDrawable(R.drawable.bg_rounded_textview)
        setTextColor(Color.BLACK)
    }
}
