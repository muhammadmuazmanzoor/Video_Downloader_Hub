package com.video.avd.ui.player.subtitle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R

class PositionAdapter(
    private val positions: Array<String>,
    private val listener: PositionSelectionListener
) : RecyclerView.Adapter<PositionAdapter.PositionViewHolder>() {
    companion object {
        var selectedPosition = 1
    }
    inner class PositionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tvPosition)
        val container: ConstraintLayout = itemView.findViewById(R.id.container)

        init {
            container.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    updateSelection(position)
                    listener.onPositionSelected(positions[position])
                }
            }
        }
    }
    fun updatePosition(position: Int){
        selectedPosition=position
        notifyItemChanged(selectedPosition)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position_selection, parent, false)
        return PositionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        val positionText = positions[position]
        holder.textView.text = positionText

        // Update selection state
        if (position == selectedPosition) {
            holder.container.setBackgroundResource(R.drawable.bg_selected_color15dp)
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.greenSelector))
        } else {
            holder.container.setBackgroundResource(R.drawable.bg_subtitle_colors)
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.brand_text_primary))
        }
    }

    override fun getItemCount(): Int = positions.size

    private fun updateSelection(newPosition: Int) {
        if (selectedPosition != newPosition) {
            val previousPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    interface PositionSelectionListener {
        fun onPositionSelected(position: String)
    }
}