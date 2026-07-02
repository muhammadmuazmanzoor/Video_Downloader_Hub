package com.avd.ui.main.progress

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.avd.R
import com.avd.databinding.ProgressBottomSheetBinding
import com.avd.util.downloaders.generic_downloader.models.VideoTaskState


class ProgressBottomSheetFragment : BottomSheetDialogFragment() {

    private var binding : ProgressBottomSheetBinding?=null

    private var mActivity: FragmentActivity? = null
    private var downloadStatus: Int? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()

    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=ProgressBottomSheetBinding.inflate(inflater,container,false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }
        updateMenuState()
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.clPause?.setOnClickListener {
            listener?.onProgressMenuClick("pause")
        }

        binding?.clResume?.setOnClickListener {
            listener?.onProgressMenuClick("resume")

        }

        binding?.clCancel?.setOnClickListener {
            listener?.onProgressMenuClick("cancel")
        }

    }


    private var listener : ProgressBottomListner? = null

    fun  setMenuListeners(listener: ProgressBottomListner){
        this.listener = listener
    }

    fun setDownloadStatus(status: Int) {
        downloadStatus = status
        updateMenuState()
    }

    private fun updateMenuState() {
        val shouldShowResume = downloadStatus == VideoTaskState.PAUSE ||
            downloadStatus == VideoTaskState.ERROR
        binding?.clPause?.visibility = if (shouldShowResume) View.GONE else View.VISIBLE
        binding?.view1?.visibility = if (shouldShowResume) View.GONE else View.VISIBLE
        binding?.clResume?.visibility = if (shouldShowResume) View.VISIBLE else View.GONE
        binding?.view2?.visibility = if (shouldShowResume) View.VISIBLE else View.GONE
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
