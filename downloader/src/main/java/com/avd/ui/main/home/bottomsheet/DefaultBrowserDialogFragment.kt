package com.avd.ui.main.home.bottomsheet

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
import com.avd.databinding.FragmentDefaultBrowserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DefaultBrowserDialogFragment : BottomSheetDialogFragment() {

    private var binding : FragmentDefaultBrowserBinding?=null
    private var mActivity: FragmentActivity? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()

    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    companion object {
        var browserCallbackfragment: DefaultBrowserCallback? = null
        fun registercallback(callback: DefaultBrowserCallback){
            browserCallbackfragment=callback
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding=FragmentDefaultBrowserBinding.inflate(inflater,container,false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let {
            binding?.btnNext?.setOnClickListener {
                browserCallbackfragment?.onBrowserButtonClicked(true)
                dismiss()
            }
            binding?.btnLater?.setOnClickListener {
//                browserCallbackfragment?.onButtonClicked(false)
                dismiss()
            }

        }
    }


    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet =
                    dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_white)
            }
        })
    }
    interface DefaultBrowserCallback {
        fun onBrowserButtonClicked(result: Boolean)
    }

}