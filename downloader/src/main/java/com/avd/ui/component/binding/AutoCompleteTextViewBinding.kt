package com.avd.ui.component.binding

import androidx.databinding.BindingAdapter
import android.widget.AutoCompleteTextView
import com.avd.data.local.model.Suggestion
import com.avd.data.local.room.entity.HistoryItem
import com.avd.ui.component.adapter.SuggestionAdapter
import com.avd.ui.component.adapter.TabSuggestionAdapter

object AutoCompleteTextViewBinding {

    @BindingAdapter("app:items")
    @JvmStatic
    fun AutoCompleteTextView.setSuggestions(items: List<Suggestion>?) {
        with(adapter as SuggestionAdapter?) {
            if (items != null) {
                this?.setData(items)
            } else {
                this?.setData(emptyList())
            }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun AutoCompleteTextView.setTabSuggestions(items: List<HistoryItem>?) {
        with(adapter as TabSuggestionAdapter?) {
            if (items != null) {
                this?.setData(items)
            } else {
                this?.setData(emptyList())
            }
        }
    }

//    @BindingAdapter("app:items")
//    @JvmStatic
//    fun AppCompatAutoCompleteTextView.setSuggestions(items: List<Suggestion>?) {
//        val text = items?.joinToString(", ") { it.toString() } ?: ""
//        this.setText(text)
//    }
//
//    @BindingAdapter("app:items")
//    @JvmStatic
//    fun EditText.setTabSuggestions(items: List<HistoryItem>?) {
//        val text = items?.joinToString(", ") { it.toString() } ?: ""
//        this.setText(text)
//    }
}