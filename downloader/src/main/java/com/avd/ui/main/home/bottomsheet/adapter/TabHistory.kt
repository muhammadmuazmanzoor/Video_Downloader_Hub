package com.avd.ui.main.home.bottomsheet.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.avd.R
import com.avd.ui.main.home.browser.webTab.WebTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TabHistory (private var historyList: List<WebTab>, private val onTabClick: (WebTab) -> Unit, private val onMenuClick: (String, WebTab) -> Unit
) : RecyclerView.Adapter<TabHistory.TabViewHolder>() {

    inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookmarkTextView: TextView = itemView.findViewById(R.id.bookmark_text_view)
        val rootitem: ConstraintLayout = itemView.findViewById(R.id.constriantlayout)
        val urlTextView: TextView = itemView.findViewById(R.id.url_text_view)
        val icon: ImageView = itemView.findViewById(R.id.icon_image)
        val imgsetting: ImageView = itemView.findViewById(R.id.imgSetting)

        fun bind(tab: WebTab){
            imgsetting.setOnClickListener {
                imgsetting.let {
                    showPopupMenu(it, false, R.style.PopupMenuStyle,it.context,tab)
                }
            }
            rootitem.setOnClickListener {
                onTabClick.invoke(tab)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item_download, parent, false)
        return TabViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = historyList[position]
        holder.bind(tab)
        CoroutineScope(Dispatchers.Main).launch {
            holder.urlTextView.text = tab.getUrl()
            holder.bookmarkTextView.text = tab.getTitle()
            if (position == 0){
                holder.imgsetting.visibility= View.GONE
            }
            Glide.with(holder.icon)
                .asBitmap()
                .load("https://www.google.com/s2/favicons?sz=64&domain_url=" + tab.getUrl())
                .into(object : CustomTarget<Bitmap?>() {
                    public override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        holder.icon.setImageBitmap(resource)
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTab(newHistoryList: List<WebTab>) {
        historyList = newHistoryList
        notifyDataSetChanged()
    }


    fun showPopupMenu(anchor: View, isWithIcons: Boolean, style: Int, context: Context, url: WebTab) {
        try {
            //init the wrapper with style
            val wrapper: Context = ContextThemeWrapper(context, style)
            //init the popup
            val popup = PopupMenu(wrapper, anchor)
            /*  The below code in try catch is responsible to display icons*/
            try {
                val fields = popup.javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field[popup]
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon",
                            Boolean::class.javaPrimitiveType
                        )
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            //inflate menu
            popup.menuInflater.inflate(R.menu.bookmark_menu, popup.menu)

            //implement click events
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.copy_url -> {
                        onMenuClick("0",url)
                    }
                    R.id.delete -> {
                        onMenuClick("1",url)
                    }
                }
                true
            }
            popup.show()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

}