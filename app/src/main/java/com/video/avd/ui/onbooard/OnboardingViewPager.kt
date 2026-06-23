package com.video.avd.ui.onbooard

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.obNativeAdFullScr1
import com.video.avd.ads.AdsHelper.obNativeAdFullScr2
import com.video.avd.ads.AdsHelper.obNativeAdHighFullScr1
import com.video.avd.ads.AdsHelper.obNativeAdHighFullScr2
import kotlin.reflect.KClass

class OnboardingViewPager(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    // ---------- internal page enum -----------------------------------------------------------

    private enum class Page(
        val clazz: KClass<out Fragment>,
        val id: Long,
        val factory: () -> Fragment
    ) {
        CORE_1(FragmentOnboardFirst::class,  1L, { FragmentOnboardFirst() }),
        CORE_2(FragmentOnboardSecondFull::class, 3L, { FragmentOnboardSecondFull() }),
        CORE_FULL1(OnBoardingFullScr1::class,   2L, { OnBoardingFullScr1() }),
        CORE_3(FragmentOnboardThirdFull::class, 7L, { FragmentOnboardThirdFull() }),
        CORE_FULL2(OnBoardingFullScr2::class,   4L, { OnBoardingFullScr2() }),

        CORE_4(FragmentOnboardFourth::class,  5L, { FragmentOnboardFourth() }),

//        CORE_5(FragmentOnboardFifth::class, 7L, { FragmentOnboardFifth() }),
    }

    // ---------- mutable list of *current* pages ----------------------------------------------

    private val pages = mutableListOf<Page>()

    init {
        rebuildPages()
    }
    /** Call from ad‑load callbacks to insert/remove ad pages and refresh the ViewPager */
    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        val before = pages.toList()
        rebuildPages()
        // Quick & safe: redraw everything (fine for <10 pages)
        if (before != pages) notifyDataSetChanged()
        // For perfectionists: diff `before` vs `pages` and call notifyItemInserted/Removed.
    }

    private fun rebuildPages() {
        pages.clear()
        if(AdsHelper.obFirstEnable) {
            pages += Page.CORE_1
        }
        if(AdsHelper.obSecondEnable) {
            pages += Page.CORE_2
        }
        if(obNativeAdHighFullScr1 != null || obNativeAdFullScr1 != null) {
            pages += Page.CORE_FULL1
        }
        if(AdsHelper.obThirdEnable) {
            pages += Page.CORE_3
        }
        if (obNativeAdHighFullScr2 != null || obNativeAdFullScr2 != null) {
            pages += Page.CORE_FULL2
        }

        if (AdsHelper.obFourthEnable ) {
            pages += Page.CORE_4
        }

        /*if (AdsHelper.obFifthEnable ) {
            pages += Page.CORE_5
        }*/
    }

    // ---------- FragmentStateAdapter overrides -----------------------------------------------

    override fun getItemCount(): Int = pages.size
    override fun createFragment(position: Int): Fragment = pages[position].factory()
    override fun getItemId(position: Int): Long      = pages[position].id
    override fun containsItem(itemId: Long): Boolean = pages.any { it.id == itemId }
}
