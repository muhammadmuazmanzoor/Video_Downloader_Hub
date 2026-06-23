package com.video.avd.ui.player.subtitle

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.video.avd.R


class LanguageAdapter(private val list: List<String>, private val listener: LanguageClickListener) :
    RecyclerView.Adapter<LanguageAdapter.HistoryFragViewHolder>() {
    private var currentPosition = 0

    inner class HistoryFragViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var unit: TextView?=null
        var radio: ImageView?=null

        init {
            unit = itemView.findViewById(R.id.tv_unit)
            radio = itemView.findViewById(R.id.radio_unit)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryFragViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return HistoryFragViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryFragViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val item = list[position]
        holder.unit?.text = item
        holder.itemView.setOnClickListener {
            listener.onLanguageClick(item)
            currentPosition = position
            notifyDataSetChanged()
        }
        if (currentPosition == position) {
            holder.radio?.setImageResource(R.drawable.ic_radio_checked)
        } else {
            holder.radio?.setImageResource(R.drawable.ic_radio_unchecked)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface LanguageClickListener {
        fun onLanguageClick(which: String?)
    }
}