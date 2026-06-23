package com.avd.ui.main.home.browser.homeTab.enginedialogue.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.avd.R
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.GridItem

class GridAdapter(
    private val context: Context,
    private val items: List<GridItem> // Define a data model to hold item data (image, text)
) : BaseAdapter() {

    private var selectedPosition = -1 // Initially, no item is selected

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.grid_item_layout, parent, false)
        val item = items[position]
        val imageView: ImageView = view.findViewById(R.id.grid_item_image)
        val textView: TextView = view.findViewById(R.id.grid_item_text)
        imageView.setImageResource(item.imageResId)
        textView.text = item.text


        // Manually set background based on selected state
        if (position == selectedPosition) {
            view.setBackgroundResource(R.drawable.search_engine_selector) // Selected background
        } else {
            view.setBackgroundResource(R.drawable.default_background) // Default background
        }



        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged() // Notify the adapter to update the view
    }
}