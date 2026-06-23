package com.avd.ui.main.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.avd.R
import com.avd.databinding.FragmentHistoryDownloadBinding
import com.avd.ui.component.adapter.HistoryAdapter
import com.avd.ui.component.adapter.HistoryListener
import com.avd.ui.main.base.BaseFragment
import com.avd.ui.main.home.browser.BrowserViewModel
import com.avd.ui.main.home.browser.webTab.NewBrowserFragment
import com.avd.ui.main.home.browser.webTab.WebTab
import com.avd.ui.main.progress.WrapContentLinearLayoutManager
import com.avd.util.AppLogger
import com.avd.util.CommunicateWithActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryFragment : BaseFragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private  val historyModel: HistoryViewModel by viewModels()

    private lateinit var dataBinding: FragmentHistoryDownloadBinding

    private lateinit var historyAdapter: HistoryAdapter

    private var host: CommunicateWithActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            host = context as? CommunicateWithActivity ?: error("Activity must implement HostActions")
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        historyAdapter = HistoryAdapter(emptyList(), historyListener)
//        val color = getThemeBackgroundColor()
        dataBinding = FragmentHistoryDownloadBinding.inflate(inflater, container, false).apply {
//            this.historyContainer.setBackgroundColor(color)
            val historyManagerLayout = WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            historyManagerLayout.stackFromEnd = true
            this.viewModel = historyModel
            this.historyList.layoutManager = historyManagerLayout
            this.historyList.adapter = historyAdapter
            this.clearButton.setOnClickListener {
               if (viewModel?.historyItems?.get()?.isNotEmpty() == true){
                   showWarningDialog(requireContext())
               } else{
                   Toast.makeText(requireContext(), "History Already Cleared", Toast.LENGTH_SHORT).show()
               }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }
        return dataBinding.root
    }

    private val historyListener = object : HistoryListener {
        override fun onHistoryOpenClicked(view: View, id: String) {
            AppLogger.d("onHistoryOpenClicked: $id")
            historyModel.historyItems.get()?.find {
                it.id == id
            }.let {
                it?.let { item ->
                    parentFragmentManager.popBackStack()
                    BrowserViewModel.instance?.openPageEvent?.value =
                        WebTab(item.url, item.title, item.faviconBitmap())
                }
            }
        }

        override fun onHistoryDeleteClicked(view: View, id: String) {
        }

        override fun onMenuClicked(view: View, id: String) {
            showPopupMenu(view, false, R.style.PopupMenu,requireContext(),id)
        }

        override fun onAllHistoryDeleteClicked() {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        historyModel.start()

        dataBinding.waBack.setOnClickListener {
            try {
                val currentFragment = this
                val activityFragmentContainer =
                    currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                activityFragmentContainer?.let {
                    val transaction =
                        currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(it.id, NewBrowserFragment.newInstance())
                    transaction.addToBackStack("history")
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    transaction.commit()
                }
            } catch (e: ClassCastException) {
                AppLogger.d("Can't get the fragment manager with this")
            }
//            requireActivity().supportFragmentManager.popBackStackImmediate()
        }
        dataBinding.historyContainer.setOnClickListener {
           //Don't remove this empty click listener. it avoids the previous screen icons click listeners
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        historyModel.stop()
        try {
            host?.showBottomBar()
            host = null
        } catch (e: Exception) {
            Log.e("FragmentLifecycle", "Error in onDetach: ${e.localizedMessage}", e)
        }
    }

    fun showPopupMenu(anchor: View, isWithIcons: Boolean, style: Int, context: Context, url: String) {
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
                        historyModel.historyItems.get()?.find { it.id == url }
                            ?.let { copyUrlToClipboard(requireContext(),it.url) }
                    }
                    R.id.delete -> {
                        historyModel.historyItems.get()?.find { it.id == url }
                            ?.let { historyModel.deleteHistory(it) }
                    }
                }
                true
            }
            popup.show()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun copyUrlToClipboard(context: Context, url: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied URL", url)
            clipboard.setPrimaryClip(clip)

            // Show a toast message to indicate that the URL has been copied
            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun showWarningDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.tag_warning))
            .setMessage(getString(R.string.are_u_sure_delete))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                historyModel.clearHistory()
                dialog.dismiss()
            } .setNegativeButton(getString(R.string.cancel2)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



}