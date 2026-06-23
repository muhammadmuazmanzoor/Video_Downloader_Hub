package com.avd.ui.component.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.databinding.ItemWebTabButtonBinding
import com.avd.ui.main.home.browser.webTab.WebTab
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.http.Url

interface WebTabsListener {
    fun onCloseTabClicked(webTab: WebTab)
    fun onSelectTabClicked(webTab: WebTab)
    fun deleteAll()
    fun insertNew()
}

class WebTabsAdapter(private var webTabs: List<WebTab>, private var webTabsListener: WebTabsListener ) : RecyclerView.Adapter<WebTabsAdapter.WebTabsViewHolder>() {

  inner  class WebTabsViewHolder(val binding: ItemWebTabButtonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(webTab: WebTab, webTabsListener: WebTabsListener) {
            with(binding) {
                this.webTab = webTab
                this.tabListener = webTabsListener
                this.imgSetting.visibility = if (webTab.isHome()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                if (webTab.getFavicon() == null && !webTab.isHome()) {
                    val bm = AppCompatResources.getDrawable(iconImage.context, R.drawable.public_24px)
                    this.iconImage.setImageDrawable(bm)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    val bitmap = getWebsiteThumbBitmap(
                        pageUrl = webTab.getUrl(),
                        imageView = binding.thumb
                    )

                    if (bitmap == null) {
                        binding.thumb.setImageBitmap(webTab.getFavicon())
                    }
                }
//                if (webTab.isHome()) {
//                    val bm = AppCompatResources.getDrawable(iconImage.context, R.drawable.home_svg)
//                    this.iconImage.setImageDrawable(bm)
//                    setCustomMargins(bookmarkTextView,0,60,0,0)
//                    setCustomMargins(view1,0,0,0,0)
//                }
//                if (!webTab.isHome()) {
//                    setCustomMargins(bookmarkTextView,0,20,0,0)
                    setCustomMargins(view1,0,10,0,0)
                    if (webTab.getTitle().isEmpty()) {
                        this.bookmarkTextView.text = webTab.getUrl()
                    } else {
                        this.bookmarkTextView.text = webTab.getTitle()
                    }
//                }
                imgSetting.setOnClickListener {
                    imgSetting.let {
                     //   showPopupMenu(it, false, R.style.PopupMenuStyle,it.context,webTab)
                        webTabsListener.onCloseTabClicked(webTab)
                    }
                }
                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebTabsViewHolder {
        val binding = DataBindingUtil.inflate<ItemWebTabButtonBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_web_tab_button, parent, false
        )

        return WebTabsViewHolder(binding)
    }

    override fun getItemCount() = webTabs.size

    override fun onBindViewHolder(holder: WebTabsViewHolder, position: Int) =
        holder.bind(webTabs[position], webTabsListener)

    fun setData(webTabs: List<WebTab>) {
        val filteredList = webTabs?.drop(1) ?: emptyList()
        this.webTabs = filteredList
        notifyDataSetChanged()
    }

    suspend fun getWebsiteThumbBitmap(
        pageUrl: String,
        imageView: ImageView
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get()

                val imageUrl = doc.select("meta[property=og:image]").attr("content")
                    .ifEmpty { doc.select("meta[name=twitter:image]").attr("content") }
                    .toAbsoluteUrl(pageUrl)

                if (imageUrl.isNullOrBlank()) return@withContext null

                withContext(Dispatchers.Main) {
                    loadBitmapWithGlide(imageView, imageUrl)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    suspend fun loadBitmapWithGlide(
        imageView: ImageView,
        imageUrl: String
    ): Bitmap? = suspendCancellableCoroutine { continuation ->

        Glide.with(imageView)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    imageView.setImageBitmap(resource)

                    if (continuation.isActive) {
                        continuation.resume(resource) {}
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    if (continuation.isActive) {
                        continuation.resume(null) {}
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)

                    if (continuation.isActive) {
                        continuation.resume(null) {}
                    }
                }
            })
    }
    private fun String.toAbsoluteUrl(baseUrl: String): String? {
        if (this.isBlank()) return null

        return try {
            java.net.URL(java.net.URL(baseUrl), this).toString()
        } catch (e: Exception) {
            null
        }
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
            popup.menuInflater.inflate(R.menu.tab_menu, popup.menu)

            //implement click events
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.delete -> {
                        webTabsListener.onCloseTabClicked(url)
                    }
                }
                true
            }
            popup.show()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun setCustomMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

}
