package com.avd.ui.component.adapter

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.avd.R
import com.avd.data.local.room.entity.PageInfo
import com.avd.databinding.ItemTopPageBinding
import com.avd.util.ContextUtils
import com.bumptech.glide.Glide

class TopPageAdapter(
    context: Context,
    private var pageInfos: List<PageInfo>,
    private val itemListener: TopPagesListener
) : ArrayAdapter<TopPageAdapter.TopPageViewHolder>(context, R.layout.item_top_page) {
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val binding = if (view == null) {
            val inflater = LayoutInflater.from(parent.context)
            ItemTopPageBinding.inflate(inflater, parent, false)
        } else {
            DataBindingUtil.getBinding(view)
        }

        with(binding) {
            this?.pageInfo = pageInfos[position]
            this?.listener = itemListener
            if (this?.pageInfo?.faviconBitmap() != null) {
                pageInfo?.faviconBitmap()?.let { bmp ->
                    Glide.with(imgIcon.context)
                        .load(bmp)
                        .override(128, 128) // ensure it never blows up
                        .into(imgIcon)
                }
            } else {
                val drawable = AppCompatResources.getDrawable(
                    ContextUtils.getApplicationContext(), R.drawable.ic_browser
                )
                drawable?.setColorFilter(
                    ContextUtils.getApplicationContext().resources.getColor(
                        R.color.color_gray_2
                    ), PorterDuff.Mode.MULTIPLY
                )
                this?.imgIcon?.setImageDrawable(drawable)
            }
            this?.executePendingBindings()
        }

        return binding!!.root
    }

    // TODO bullshit
    override fun getItemId(position: Int) = try {
        pageInfos[position].hashCode().toLong()
    } catch (e: Exception) {
        0
    }

    override fun getCount(): Int {
        return pageInfos.size
    }

    class TopPageViewHolder(val binding: ItemTopPageBinding) : RecyclerView.ViewHolder(binding.root)

    fun setData(pageInfos: List<PageInfo>) {
        this.pageInfos = pageInfos
        notifyDataSetChanged()
    }

    interface TopPagesListener {
        fun onItemClicked(pageInfo: PageInfo)
    }
}
