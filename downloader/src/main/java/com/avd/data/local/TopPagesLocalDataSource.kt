package com.avd.data.local

import android.net.Uri
import com.avd.R
import com.avd.data.local.room.dao.PageDao
import com.avd.data.local.room.entity.PageInfo
import com.avd.data.repository.TopPagesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopPagesLocalDataSource @Inject constructor(
    private val pageDao: PageDao
) : TopPagesRepository {

    override suspend fun getTopPages(): List<PageInfo> {
        val list1 = arrayListOf<PageInfo>()

        list1.add(PageInfo(link = "https://www.facebook.com/").apply {
            drawableResId= R.drawable.icon_facebook
        })
        list1.add(PageInfo(link = "https://www.instagram.com").apply {
            drawableResId= R.drawable.ic_instagram
        })

        list1.add(PageInfo(link = "https://www.tiktok.com").apply {
            drawableResId= R.drawable.ic_tiktok
        })
        list1.add(PageInfo(link = "https://www.Status.com").apply {
            drawableResId= R.drawable.ic_whatsapp
        })
        list1.add(PageInfo(link = "https://www.x.com").apply {
            drawableResId= R.drawable.twitter_x
        })
        list1.add(PageInfo(link = "https://www.dailymotion.com").apply {
            drawableResId= R.drawable.ic_dailymotion
        })

        list1.add(PageInfo(link = "https://www.imdb.com").apply {
            drawableResId= R.drawable.ic_imdb
        })

        list1.add(PageInfo(link = "https://vimeo.com/").apply {
            drawableResId= R.drawable.ic_vimeo
        })

        for (page in list1) {
            page.name = Uri.parse(page.link).host.toString()
        }

        return list1
    }

    override fun saveTopPage(pageInfo: PageInfo) {
        pageDao.insertProgressInfo(pageInfo)
    }

    override fun deletePageInfo(pageInfo: PageInfo) {
        pageDao.deleteProgressInfo(pageInfo)
    }

    override suspend fun updateLocalStorage() {

    }

}