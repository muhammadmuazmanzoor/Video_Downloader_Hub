package com.video.avd.ui.status_saver.statusnew

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
import com.video.avd.R
import com.video.avd.databinding.FragmentBottomsheetStatusBinding


class BottomsheetStatusFragment : BottomSheetDialogFragment() {
    private var binding : FragmentBottomsheetStatusBinding? =null

    private var mActivity: FragmentActivity? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentBottomsheetStatusBinding.inflate(inflater,container,false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.let {
            clicklistener()
        }
    }

    private fun clicklistener() {
        binding?.btnGotit?.setOnClickListener {
            dialog?.cancel()
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }


    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }
    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet = dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }
}