package com.avd.ui.main.home.bottomsheet

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.avd.R
import com.avd.databinding.FragmentTabBottomSheetDownloaderBinding
import com.avd.ui.component.adapter.WebTabsAdapter
import com.avd.ui.component.adapter.WebTabsListener
import com.avd.ui.main.home.browser.BrowserViewModel
import com.avd.ui.main.home.browser.webTab.WebTab
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TabBottomSheetFragment : BottomSheetDialogFragment() {

    private var binding: FragmentTabBottomSheetDownloaderBinding? = null
    private val browserViewModel: BrowserViewModel by activityViewModels()
    private var mActivity: FragmentActivity? = null

    private lateinit var drawerAdapter: WebTabsAdapter


//    private val tabAdapter: TabHistory by lazy {
//        TabHistory(emptyList(),{tab->
//            callbackfragment?.onTabSelected(tab)
//            dismiss()
//        }){ url,id ->
//            when(url){
//                "0" ->{
////                    if (id.url != ""){
////                        mActivity?.let { copyUrlToClipboard(it,id.url) }
////                    }
//                }
//                "1" ->{
////                     callbackfragment?.tabDelete(id)
//                }
//            }
//        }
//    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()

    }
    override fun onStart() {
        super.onStart()

        try {
            val dialog = dialog as? BottomSheetDialog ?: return
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return

            // 🔥 Force full height
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = BottomSheetBehavior.from(bottomSheet)

            behavior.apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                isDraggable = false // optional (disable drag)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    companion object {
        var callbackfragment: TabBottomSheetCallback? = null
        fun registercallback(callback: TabBottomSheetCallback) {
            callbackfragment = callback
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        drawerAdapter = WebTabsAdapter(emptyList(), tabsListener)
        binding = FragmentTabBottomSheetDownloaderBinding.inflate(inflater, container, false).apply {
                lifecycleOwner = this@TabBottomSheetFragment
                viewModel = browserViewModel
                tabRec.adapter = drawerAdapter
            }
        return binding?.root
    }


    fun hideNavigationBarFromDialog() {
        mActivity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.delete?.setOnClickListener {
            mActivity
                ?.takeIf { !browserViewModel.tabs.get().isNullOrEmpty() }
                ?.let { activity -> showWarningDialog(activity) }
        }

        binding?.newtab?.setOnClickListener {
            tabsListener.insertNew()
            dismiss()
        }
        binding?.back?.setOnClickListener {
            dismiss()
        }
        binding?.home?.setOnClickListener {
            tabsListener.onSelectTabClicked(WebTab("","Home Tab",null,id="home"))
            dismiss()
        }

    }

    private fun showWarningDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.close_tabs))
            .setMessage(getString(R.string.are_u_sure_delete_tab))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                tabsListener.deleteAll()
                for (tab in browserViewModel.tabs.get() ?: emptyList()) {
                    tabsListener.onCloseTabClicked(tab)
                }
                dismiss()
            }.setNegativeButton(getString(R.string.cancel2)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun copyUrlToClipboard(context: Context, url: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied URL", url)
            clipboard.setPrimaryClip(clip)

            // Show a toast message to indicate that the URL has been copied
            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet = dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }

    private val tabsListener = object : WebTabsListener {
        override fun onCloseTabClicked(webTab: WebTab) {
            browserViewModel.closePageEvent.value = webTab

        }

        override fun onSelectTabClicked(webTab: WebTab) {
            browserViewModel.selectWebTabEvent.value = webTab
            dismiss()
        }

        override fun deleteAll() {
            browserViewModel.handleRemoveAllTabsExceptHome()
        }

        override fun insertNew() {
            browserViewModel.openNewTabWithDefaultUrl("https://www.google.com")
        }
    }

}