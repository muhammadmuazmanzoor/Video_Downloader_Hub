package com.avd.ui.main.home.downloadapi.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.ui.main.home.downloadapi.VideoFormat
import com.google.android.material.radiobutton.MaterialRadioButton

class FormatAdapter(
    private val formats: List<VideoFormat>,
    private val extracor: String,
    private val onItemClick: (VideoFormat, Int) -> Unit
) : RecyclerView.Adapter<FormatAdapter.ViewHolder>() {

    private var selectedPosition = 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val quality: TextView = view.findViewById(R.id.tv_title)
        val size: TextView = view.findViewById(R.id.tv_size)
        val radioFormat: MaterialRadioButton = view.findViewById(R.id.radio_format)
        val itemselect: ConstraintLayout = view.findViewById(R.id.itemselect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_format, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val format = formats[position]

        if (extracor == "Facebook") {
            holder.quality.text = format.format_id.orEmpty()
        } else {
            holder.quality.text = format.height?.let {
                calculateQuality(it)
            } ?: ""
        }

        val sizeText = format.filesize_approx?.let {
            val sizeInMB = it.toDouble() / (1024 * 1024)
            String.format("%.2f MB", sizeInMB)
        }.orEmpty()

        if (sizeText.isNotEmpty()) {
            holder.size.text = sizeText
            holder.size.visibility = View.VISIBLE
        } else {
            holder.size.visibility = View.GONE
        }

        val isSelected = position == selectedPosition
        holder.radioFormat.isChecked = isSelected
        holder.itemselect.isSelected = isSelected

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(format, position)
        }
    }

    override fun getItemCount() = formats.size

    fun calculateQuality(height: Int?): String = when (height ?: 0) {
        144 -> "144p"
        240 -> "240p"
        360 -> "360p"
        480 -> "480p"
        720 -> "720p"
        1080 -> "1080p"
        1440 -> "1440p"
        2160 -> "4K"
        else -> "${height}p"
    }
}
