package com.video.avd.ui.status_saver.statusnew

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R

class ItemViewHoldernew(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    var download: ImageView
    @JvmField
    var checkBox: CheckBox
    @JvmField
    var share: ImageView
    @JvmField
    var imageView: ImageView
    @JvmField
    var rootLayout: ConstraintLayout


    init {
        imageView = itemView.findViewById(R.id.recentImage)
        download = itemView.findViewById(R.id.recent_downloadfile)
        share = itemView.findViewById(R.id.recent_share)
        checkBox = itemView.findViewById(R.id.status_check)
        rootLayout = itemView.findViewById(R.id.recent)
    }
}