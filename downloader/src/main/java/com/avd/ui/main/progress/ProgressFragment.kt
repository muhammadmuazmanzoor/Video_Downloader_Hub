package com.avd.ui.main.progress

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.avd.R
import com.avd.databinding.FragmentProgressBinding
import com.avd.ui.component.adapter.ProgressAdapter
import com.avd.ui.component.adapter.ProgressListener
import com.avd.ui.main.base.BaseFragment
import com.avd.ui.main.home.MainViewModel
import com.avd.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ProgressFragment : BaseFragment() {
    companion object {
        fun newInstance() = ProgressFragment()
    }
//    lateinit var mainActivity: MainActivityDownloader
    private  val progressViewModel: ProgressViewModel by activityViewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var dataBinding: FragmentProgressBinding
    private lateinit var progressAdapter: ProgressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        progressAdapter = ProgressAdapter(emptyList(), progressListener)
        dataBinding = FragmentProgressBinding.inflate(inflater, container, false).apply {
            val managerL = WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            this.viewModel = progressViewModel
            this.rvProgress.layoutManager = managerL
            this.rvProgress.adapter = progressAdapter
            // Optimize RecyclerView to reduce GC pressure during layout
            this.rvProgress.setItemViewCacheSize(10) // Cache more views to reduce view creation
            this.rvProgress.setHasFixedSize(true)
            this.rvProgress.itemAnimator = null
            this.rvProgress.setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
                setMaxRecycledViews(0, 15) // Increase pool size for view type 0
            })
        }
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressViewModel.start()
        handleDownloadVideoEvent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressViewModel.stop()
    }

    private fun handleDownloadVideoEvent() {
        mainViewModel.downloadVideoEvent.observe(viewLifecycleOwner, Observer { videoInfo ->
            val currentOriginal = videoInfo.originalUrl
            mainViewModel.currentOriginal.set(currentOriginal)
            progressViewModel.downloadVideo(videoInfo)
        })
    }

    private val progressListener = object : ProgressListener {
        override fun onMenuClicked(view: View, downloadId: Long, isRegular: Boolean) {
            val dg = ProgressBottomSheetFragment()
            val menuCandidate = progressViewModel.progressInfos.get()?.find { it.downloadId == downloadId }
            menuCandidate?.let { dg.setDownloadStatus(it.downloadStatus) }
            dg.show(parentFragmentManager, "")
            dg.setMenuListeners(object:ProgressBottomListner{
                override fun onProgressMenuClick(which: String) {
                  when(which){
                      "cancel" ->{
                          progressViewModel.cancelDownload(downloadId, true)
                          dg.dismiss()
                      }
                      "pause" ->{
                          progressViewModel.pauseDownload(downloadId)
                          dg.dismiss()
                      }
                      "resume" ->{
                          progressViewModel.resumeDownload(downloadId)
                          dg.dismiss()
                      }
                  }
                }
            })
        }
    }

    private fun showPopupMenu(view: View, downloadId: Long, isRegular: Boolean) {
        val wrapper = ContextThemeWrapper(view.context,R.style.PopupMenuStyle)
        val menuCandidate = progressViewModel.progressInfos.get()?.find { it.downloadId == downloadId }
        val popupMenu = PopupMenu(wrapper, view)
        popupMenu.menuInflater.inflate(R.menu.menu_progress, popupMenu.menu)
        popupMenu.menu.getItem(3).isVisible = menuCandidate?.isLive == true
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { arg0 ->
            when (arg0.itemId) {
                R.id.item_cancel -> {
                    progressViewModel.cancelDownload(downloadId, true)
                    true
                }
                R.id.item_pause -> {
                    progressViewModel.pauseDownload(downloadId)
                    true
                }
                R.id.item_resume -> {
                    progressViewModel.resumeDownload(downloadId)
                    true
                }
                R.id.item_stop_save -> {
                    progressViewModel.stopAndSaveDownload(downloadId)
                    true
                }
                else -> false
            }
        }
    }

}

class WrapContentLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            AppLogger.e("meet a IOOBE in RecyclerView")
        }
    }
}
