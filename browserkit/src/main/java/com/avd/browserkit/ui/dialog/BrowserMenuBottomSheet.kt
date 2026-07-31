package com.avd.browserkit.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.avd.browserkit.R
import com.avd.browserkit.databinding.BottomSheetBrowserMenuBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

object BrowserMenuBottomSheet {
    private const val TAG = "BrowserMenuBottomSheet"
    const val ARG_DESKTOP = "desktop_mode"

    fun show(
        manager: FragmentManager,
        desktopMode: Boolean,
        listener: BrowserMenuListener,
    ) {
        if (manager.findFragmentByTag(TAG) != null) return
        BrowserMenuBottomSheetFragment().apply {
            this.listener = listener
            arguments = Bundle().apply { putBoolean(ARG_DESKTOP, desktopMode) }
        }.show(manager, TAG)
    }
}

interface BrowserMenuListener {
    fun onMenuHome()
    fun onMenuDownloads()
    fun onMenuHistory()
    fun onMenuSettings()
    fun onMenuFindInPage()
    fun onMenuDesktopSiteChanged(enabled: Boolean)
    fun onMenuClearBrowsingData()
    fun onMenuShare()
}

class BrowserMenuBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: BrowserMenuListener? = null
    private var _binding: BottomSheetBrowserMenuBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.Theme_BrowserKit_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetBrowserMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val desktopMode = arguments?.getBoolean(BrowserMenuBottomSheet.ARG_DESKTOP, false) ?: false
        binding.switchDesktopSite.isChecked = desktopMode

        binding.actionHome.setOnClickListener { dismissThen { listener?.onMenuHome() } }
        binding.actionDownloads.setOnClickListener { dismissThen { listener?.onMenuDownloads() } }
        binding.actionHistory.setOnClickListener { dismissThen { listener?.onMenuHistory() } }
        binding.actionSettings.setOnClickListener { dismissThen { listener?.onMenuSettings() } }
        binding.menuFindInPage.setOnClickListener { dismissThen { listener?.onMenuFindInPage() } }
        binding.menuClearData.setOnClickListener { dismissThen { listener?.onMenuClearBrowsingData() } }
        binding.menuShare.setOnClickListener { dismissThen { listener?.onMenuShare() } }

        binding.switchDesktopSite.setOnCheckedChangeListener { _, checked ->
            listener?.onMenuDesktopSiteChanged(checked)
        }

        (dialog as? BottomSheetDialog)?.let { sheet ->
            sheet.setOnShowListener {
                val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { panel ->
                    panel.background = null
                    BottomSheetBehavior.from(panel).apply {
                        skipCollapsed = true
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            }
        }
    }

    private fun dismissThen(action: () -> Unit) {
        dismiss()
        action()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
