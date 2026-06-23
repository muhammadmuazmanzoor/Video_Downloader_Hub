package com.avd.ui.main.home.browser.detectedVideos

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.avd.R
import com.avd.databinding.FragmentDetectedVideosTabBinding
import com.avd.ui.component.adapter.VideoInfoAdapter
import com.avd.ui.component.dialog.DownloadTabListener
import com.avd.ui.main.progress.WrapContentLinearLayoutManager
import com.avd.util.AppUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetectedVideosTabFragment : BottomSheetDialogFragment() {

    var detectedVideosTabViewModel: DetectedVideosTabViewModel? = null

    var candidateFormatListener: DownloadTabListener? = null

    @Inject
    lateinit var appUtil: AppUtil

    private lateinit var binding: FragmentDetectedVideosTabBinding

    private lateinit var layoutMngr: WrapContentLinearLayoutManager

    companion object {
        fun newInstance() = DetectedVideosTabFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (detectedVideosTabViewModel == null || candidateFormatListener == null) {
            Toast.makeText(context, "Something went wrong, try again.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        val adapter = detectedVideosTabViewModel?.let {
            candidateFormatListener?.let { it1 ->
                detectedVideosTabViewModel?.detectedVideosList?.get()?.toMutableList()?.let { it2 ->
                    VideoInfoAdapter(
                        it2,
                        it,
                        it1,
                        appUtil,
                    ){
                        dismiss()
                    }
                }
            }
        }
        layoutMngr = WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding = FragmentDetectedVideosTabBinding.inflate(inflater, container, false).apply {
            title.text = getString(R.string.found_videos_from, detectedVideosTabViewModel?.webTabModel?.getTabTextInput()?.get()).split("?").firstOrNull()
            viewModel = detectedVideosTabViewModel
            videoInfoList.layoutManager = layoutMngr
            videoInfoList.isNestedScrollingEnabled = true
            videoInfoList.adapter = adapter
            dialogListener = candidateFormatListener
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
        dialog?.let { binding.root.let { it1 -> makeBottomSheetRounded(it1, it) } }
        return binding.root
    }

    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet =
                    dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }


}
