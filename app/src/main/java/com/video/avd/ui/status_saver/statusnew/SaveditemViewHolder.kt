package com.video.avd.ui.status_saver.statusnew

import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R

class SaveditemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    var delete: ImageView
    @JvmField
    var share: ImageView
    @JvmField
    var imageView: ImageView
    @JvmField
    var rootLayout: ConstraintLayout

    init {
        imageView = itemView.findViewById(R.id.savedImage)
        delete = itemView.findViewById(R.id.saved_delete)
        share = itemView.findViewById(R.id.saved_share)
        rootLayout = itemView.findViewById(R.id.saved)
    }
}