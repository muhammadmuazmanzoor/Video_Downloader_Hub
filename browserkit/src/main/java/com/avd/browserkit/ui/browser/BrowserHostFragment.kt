package com.avd.browserkit.ui.browser

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ContextThemeWrapper
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avd.browserkit.R
import com.avd.browserkit.api.BrowserKit
import com.avd.browserkit.api.BrowserLaunchMode
import com.avd.browserkit.databinding.FragmentBrowserHostBinding
import com.avd.browserkit.ui.dialog.AdultBlockedDialog
import com.avd.browserkit.util.AdultSiteBlocker
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.UrlUtils

class BrowserHostFragment : Fragment() {

    private var _binding: FragmentBrowserHostBinding? = null
    private val binding get() = _binding!!

    private val browserViewModel: BrowserViewModel by activityViewModels()
    private lateinit var tabsAdapter: BrowserTabsPagerAdapter
    private lateinit var gridAdapter: BrowserTabsGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val themedInflater = inflater.cloneInContext(ContextThemeWrapper(requireContext(), R.style.Theme_BrowserKit))
        _binding = FragmentBrowserHostBinding.inflate(themedInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mode = arguments?.getString(ARG_MODE)?.let {
            runCatching { BrowserLaunchMode.valueOf(it) }.getOrNull()
        } ?: BrowserLaunchMode.BLANK
        val query = arguments?.getString(ARG_QUERY)
        val url = arguments?.getString(ARG_URL)

        val requestedUrl = when (mode) {
            BrowserLaunchMode.SEARCH -> UrlUtils.normalizeInput(
                query.orEmpty(),
                BrowserKit.getConfig().searchUrlTemplate,
            )
            BrowserLaunchMode.URL -> UrlUtils.normalizeInput(
                url.orEmpty(),
                BrowserKit.getConfig().searchUrlTemplate,
            )
            BrowserLaunchMode.BLANK -> "about:blank"
        }
        val adultBlocked = AdultSiteBlocker.isBlocked(requestedUrl) ||
            (mode == BrowserLaunchMode.URL && AdultSiteBlocker.isBlocked(url)) ||
            (mode == BrowserLaunchMode.SEARCH && AdultSiteBlocker.isBlocked(query))
        val initialUrl = if (adultBlocked) {
            BrowserKitLog.w("Adult", "launch blocked ${BrowserKitLog.shortUrl(requestedUrl)}")
            view.post { AdultBlockedDialog.show(childFragmentManager) }
            "about:blank"
        } else {
            requestedUrl
        }

        BrowserKit.analytics()?.onBrowserOpen(
            mode = mode.name.lowercase(),
            entryUrl = initialUrl,
        )

        if (initialUrl != "about:blank") {
            browserViewModel.tabs.value?.let { tabs ->
                if (tabs.isNotEmpty()) {
                    browserViewModel.updateTab(0, "", initialUrl)
                }
            }
        }

        tabsAdapter = BrowserTabsPagerAdapter(this)
        gridAdapter = BrowserTabsGridAdapter(
            onSelect = { index ->
                browserViewModel.setCurrentIndex(index)
                browserViewModel.hideTabsSwitcher()
            },
            onClose = { index ->
                browserViewModel.closeTab(index)
            },
            previewProvider = { tabId -> browserViewModel.getPreview(tabId) },
            faviconProvider = { tabId -> browserViewModel.getFavicon(tabId) },
        )

        binding.tabPager.adapter = tabsAdapter
        binding.tabPager.isUserInputEnabled = false
        binding.rvTabs.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvTabs.adapter = gridAdapter

        binding.btnTabsBack.setOnClickListener { browserViewModel.hideTabsSwitcher() }
        binding.btnTabsClose.setOnClickListener { browserViewModel.hideTabsSwitcher() }
        binding.btnTabsSwitcher.setOnClickListener { browserViewModel.hideTabsSwitcher() }
        binding.fabNewTab.setOnClickListener {
            browserViewModel.addNewTab()
            browserViewModel.hideTabsSwitcher()
        }
        binding.btnTabsHome.setOnClickListener {
            browserViewModel.hideTabsSwitcher()
        }
        binding.btnTabsWebBack.setOnClickListener {
            if (currentWebTab()?.navigateBack() == true) {
                updateSwitcherNavButtons()
            }
        }
        binding.btnTabsWebForward.setOnClickListener {
            if (currentWebTab()?.navigateForward() == true) {
                updateSwitcherNavButtons()
            }
        }
        binding.btnTabsMenu.setOnClickListener {
            browserViewModel.hideTabsSwitcher()
            currentWebTab()?.openBrowserMenu()
        }

        browserViewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            tabsAdapter.submitTabs(tabs.size)
            refreshGrid()
        }
        browserViewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            if (index in 0 until tabsAdapter.itemCount) {
                binding.tabPager.setCurrentItem(index, false)
            }
            refreshGrid()
            updateSwitcherAddress()
            updateSwitcherChrome()
        }
        browserViewModel.tabsSwitcherVisible.observe(viewLifecycleOwner) { visible ->
            binding.tabsSwitcher.isVisible = visible
            if (visible) {
                refreshGrid()
                updateSwitcherAddress()
                updateSwitcherChrome()
            } else {
                currentWebTab()?.refreshChrome()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (browserViewModel.tabsSwitcherVisible.value == true) {
                        browserViewModel.hideTabsSwitcher()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
    }

    fun openTabsDrawer() {
        currentWebTab()?.captureAndStorePreview()
        browserViewModel.showTabsSwitcher()
    }

    private fun refreshGrid() {
        val tabs = browserViewModel.tabs.value.orEmpty()
        val index = browserViewModel.currentIndex.value ?: 0
        gridAdapter.submit(tabs, index)
    }

    private fun updateSwitcherAddress() {
        val tabs = browserViewModel.tabs.value.orEmpty()
        val index = browserViewModel.currentIndex.value ?: 0
        val url = tabs.getOrNull(index)?.url.orEmpty()
        binding.tvTabsAddress.text = displayHost(url)
    }

    private fun updateSwitcherChrome() {
        val context = requireContext()
        val active = ContextCompat.getColor(context, R.color.bk_chrome_active)
        val icon = ContextCompat.getColor(context, R.color.black)
        val disabled = ContextCompat.getColor(context, R.color.black)

        binding.btnTabsHome.setImageResource(R.drawable.bk_ic_home_outline)
        binding.btnTabsHome.imageTintList = ColorStateList.valueOf(icon)
        binding.btnTabsSwitcher.imageTintList = ColorStateList.valueOf(active)
        binding.btnTabsMenu.imageTintList = ColorStateList.valueOf(icon)
        updateSwitcherNavButtons()
    }

    private fun updateSwitcherNavButtons() {
        val canBack = currentWebTab()?.canNavigateBack() == true
        val canForward = currentWebTab()?.canNavigateForward() == true
        val context = requireContext()
        val icon = ContextCompat.getColor(context, R.color.black)
        val disabled = ContextCompat.getColor(context, R.color.black)

        binding.btnTabsWebBack.isEnabled = true
        binding.btnTabsWebForward.isEnabled = true
        binding.btnTabsWebBack.alpha = if (canBack) 1f else 0.45f
        binding.btnTabsWebForward.alpha = if (canForward) 1f else 0.45f
        binding.btnTabsWebBack.imageTintList = ColorStateList.valueOf(if (canBack) icon else disabled)
        binding.btnTabsWebForward.imageTintList = ColorStateList.valueOf(if (canForward) icon else disabled)
    }

    private fun displayHost(url: String): String {
        if (url.isBlank() || url == "about:blank") return ""
        return runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault(url)
    }

    private fun currentWebTab(): WebTabFragment? {
        val index = browserViewModel.currentIndex.value ?: 0
        return childFragmentManager.fragments
            .filterIsInstance<WebTabFragment>()
            .firstOrNull { it.arguments?.getInt(WebTabFragment.ARG_TAB_INDEX) == index }
            ?: childFragmentManager.fragments.filterIsInstance<WebTabFragment>().getOrNull(index)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MODE = "arg_mode"
        private const val ARG_QUERY = "arg_query"
        private const val ARG_URL = "arg_url"
        const val TAB_DRAWER_REQUEST = "open_tabs_drawer"

        fun newInstance(mode: BrowserLaunchMode, query: String?, url: String?): BrowserHostFragment {
            return BrowserHostFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                    putString(ARG_QUERY, query)
                    putString(ARG_URL, url)
                }
            }
        }
    }
}

private class BrowserTabsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private var tabCount = 1

    fun submitTabs(count: Int) {
        if (count != tabCount) {
            tabCount = count.coerceAtLeast(1)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = tabCount

    override fun createFragment(position: Int): Fragment = WebTabFragment.newInstance(position)
}
