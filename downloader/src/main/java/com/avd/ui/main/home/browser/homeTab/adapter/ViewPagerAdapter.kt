
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.avd.R
import com.avd.data.local.room.entity.PageInfo


class ViewPagerAdapter(private val context: Context , val items : List<PageInfo>,var listner : onClickListener) : PagerAdapter() {

    private var currentPagePosition = 0
    private val PAGE_SIZE = 4

    // 1️⃣ PAGE COUNT ----------------------------------------------------------
    override fun getCount(): Int =
        if (items.size % PAGE_SIZE == 0) {
            items.size / PAGE_SIZE
        } else {
            (items.size / PAGE_SIZE) + 1
        }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_home_style_viewpager, container, false)
        val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerView)
        val layoutManager = GridLayoutManager(context, 4) // Customize the layout manager as needed
        recyclerView.layoutManager = layoutManager
        val start = position * PAGE_SIZE
        val end   = minOf(start + PAGE_SIZE, items.size)
        val itemsPerPage = items.subList(start, end)
        val adapter = GridAdapter(context, itemsPerPage)
        recyclerView.adapter = adapter
        container.addView(itemView)
        // Update the current page position
        setCurrentPagePosition(position)
        return itemView
    }

    private fun setCurrentPagePosition(position: Int) {
        currentPagePosition = position
    }

    fun getCurrentPagePosition(): Int {
        return currentPagePosition
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

   inner class GridAdapter(private val context: Context, private val items: List<PageInfo>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val inflater = LayoutInflater.from(context)
                    val itemView = inflater.inflate(R.layout.video_site, parent, false)
                   return  CameraViewHolder(itemView)
        }
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val data = items[position]
            (holder as CameraViewHolder).bind(data)
        }
       override fun getItemId(position: Int) = try {
           items[position].hashCode().toLong()
       } catch (e: Exception) {
           0
       }

        inner class CameraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.videoSiteIcon)
            private val title: AppCompatTextView = itemView.findViewById(R.id.videoSiteTitle)
            fun bind(pageInfo: PageInfo) {
          /*      if(pageInfo.link=="https://www.instagram.com"){
                    title.text = "Instagram"
                }
                else if(pageInfo.link=="https://www.Status.com"){
                    title.text = "Status"
                }
                else{*/
                    title.text = pageInfo.getTitleFiltered()
//                }

                icon.setImageResource(pageInfo.drawableResId)
                itemView.rootView.setOnClickListener {
                    listner.onClicklistner(pageInfo)
                }
            }
        }
        override fun getItemCount(): Int {
            return items.size
        }
    }

    interface onClickListener {
        fun onClicklistner(pageInfo: PageInfo)
    }


}

