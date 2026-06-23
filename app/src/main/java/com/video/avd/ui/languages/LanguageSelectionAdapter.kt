package com.video.avd.ui.languages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.video.avd.R

class LanguageSelectionAdapter(private val list: List<LanguageSelectionModel>, private val listener: LanguageSelectionClickListener) : RecyclerView.Adapter<LanguageSelectionAdapter.LanguageSelectionViewHolder>() {

    private var startupHighlight = true

    inner class LanguageSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var rootLayout: ConstraintLayout? = null
        var name: TextView? = null
        var flag: ImageView? = null
        var radio: ImageView? = null
        var item_back: ImageView? = null
        var anim : LottieAnimationView ?= null
        init {
            rootLayout = itemView.findViewById(R.id.rootLayout)
            name = itemView.findViewById(R.id.tv_name)
            flag = itemView.findViewById(R.id.iv_flag)
            radio = itemView.findViewById(R.id.radio_button)
            item_back = itemView.findViewById(R.id.item_back)
            anim = itemView.findViewById(R.id.anim)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageSelectionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language_selection, parent, false)
        return LanguageSelectionViewHolder(view)
    }


    override fun onBindViewHolder(holder: LanguageSelectionViewHolder, position: Int) {
        val item = list[position]
        holder.name?.text = item.name
        holder.flag?.setImageDrawable(item.flag)

        // Set text direction based on language
        holder.name?.textDirection = when (item.lang) {
            "ar" -> View.TEXT_DIRECTION_RTL
            else -> View.TEXT_DIRECTION_LTR
        }

        holder.itemView.setOnClickListener {
            if (startupHighlight) {
                startupHighlight = false
                notifyItemChanged(0)
            }
            val old = list.indexOfFirst { it.isSelected }
            if (old == position) return@setOnClickListener   // already selected
            
            // update model
            if (old != -1) {
                list[old].isSelected = false
                notifyItemChanged(old)
            }
            item.isSelected = true
            notifyItemChanged(position)
            listener.onLanguageClick(item)
        }

        // Handle animation visibility
        if (startupHighlight && position == 2) {
            holder.item_back?.visibility = View.GONE
            holder.anim?.visibility = View.VISIBLE
            holder.anim?.playAnimation()
        } else {
            holder.item_back?.visibility = View.GONE
            holder.anim?.visibility = View.GONE
            holder.anim?.pauseAnimation()
        }

        // Update selection state
        holder.itemView.isSelected = item.isSelected
        if (item.isSelected) {
            holder.radio?.setImageResource(R.drawable.ic_check)
            holder?.rootLayout?.context?.let { holder?.rootLayout?.foreground = ContextCompat.getDrawable(it, R.drawable.bg_rectangled_cl_bordered) }
            holder.name?.setTextColor(holder.itemView.context.resources.getColor(R.color.brand_text_primary))
        } else {
            holder.radio?.setImageResource(R.drawable.ic_uncheck)
            holder?.rootLayout?.context?.let { holder?.rootLayout?.foreground = ContextCompat.getDrawable(it, R.drawable.ic_transparent_top) }
            holder.name?.setTextColor(holder.itemView.context.resources.getColor(R.color.gray_text))
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }


    interface LanguageSelectionClickListener {
        fun onLanguageClick(language: LanguageSelectionModel?)
    }

}