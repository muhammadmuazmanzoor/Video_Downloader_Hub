package com.avd.ui.main.home.downloadapi.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.ui.main.home.downloadapi.VideoItem
import com.google.android.material.radiobutton.MaterialRadioButton

class SocialFormatAdapter(
    private val formats: List<VideoItem>,
    private val onItemClick: (VideoItem, Int) -> Unit
) : RecyclerView.Adapter<SocialFormatAdapter.ViewHolder>() {

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
        holder.quality.text = format.format
        holder.size.visibility = View.GONE

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
}
