package com.avd.ui.main.home.browser.homeTab.enginedialogue

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import androidx.fragment.app.DialogFragment
import com.avd.R
import com.avd.ui.main.home.browser.homeTab.enginedialogue.adapter.GridAdapter
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.GridItem

class SearchEngineDialogFragment (private val onSearchEngineSelected: (String,Int) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Set the custom background with rounded corners
        dialog.window?.setBackgroundDrawableResource(R.drawable.ic_dialogu_background)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_search_engine_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var selecteditem=""

        var icon=0

        val items = listOf(

            GridItem(R.drawable.bing_1, "bing","bing",false),

            GridItem(R.drawable.ic_duck, "duck","duckDuckGo",false),

            GridItem(R.drawable.ic_yahoo, "yahoo","yahoo",false),

            GridItem(R.drawable.ic_yandex, "yandex","yandex",false),

            GridItem(R.drawable.ic_baidu, "baidu","baidu",false),

            GridItem(R.drawable.ic_coc_coc, "coc","coc coc",false)

        )

        val gridView: GridView = view.findViewById(R.id.search_engine_grid_view)
//        val cont: Button = view.findViewById(R.id.confirm)
//        val cancel: Button = view.findViewById(R.id.cancel)
        gridView.adapter = GridAdapter(requireContext(),items)

        gridView.setOnItemClickListener { _, _, position, _ ->
            selecteditem=items[position].enginename
            icon=items[position].imageResId
            (gridView.adapter as GridAdapter).setSelectedPosition(position)
            onSearchEngineSelected(selecteditem,icon)
            dismiss()
        }

//        cont.setOnClickListener {
//            onSearchEngineSelected(selecteditem,icon)
//            dismiss() // Close the dialog after selection
//        }
//
//        cancel.setOnClickListener {
//            dismiss()
//        }
    }

}