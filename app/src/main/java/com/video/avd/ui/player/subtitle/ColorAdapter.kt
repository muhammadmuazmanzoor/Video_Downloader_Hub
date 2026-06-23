package com.video.avd.ui.player.subtitle

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R

class ColorAdapter(
    private val colorList: List<ColorItem>,
    private val listener: SubtitleCustomizationsListener
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedPosition = -1 // Track selected position

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: View = itemView.findViewById(R.id.color_view)
        val bg: ConstraintLayout = itemView.findViewById(R.id.viewBG)

        init {
            colorView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    updateSelection(position)
                    listener.onSetColor(colorList[position].colorHex) // Notify listener with selected color
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val colorItem = colorList[position]

        // Set color background
//        holder.colorView.setBackgroundColor(Color.parseColor(colorItem.colorHex))
        holder.colorView.backgroundTintList= ColorStateList.valueOf(Color.parseColor(colorItem.colorHex))

        // Update selection state with a stroke for the selected item
        if (position == selectedPosition) {
            holder.bg.background = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.bg_selected_color // drawable for selected round corner stroke
            )
        } else {
            holder.bg.background = ContextCompat.getDrawable(
                holder.itemView.context,
                R.drawable.bg_subtitle_colors // default drawable background
            )
        }
    }

    override fun getItemCount(): Int = colorList.size

    private fun updateSelection(newPosition: Int) {
        if (selectedPosition != newPosition) {
            val previousPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(previousPosition) // Refresh previous item
            notifyItemChanged(selectedPosition) // Refresh newly selected item
        }
    }

    interface SubtitleCustomizationsListener {
        fun onSetColor(colorHex: String)
    }
}
