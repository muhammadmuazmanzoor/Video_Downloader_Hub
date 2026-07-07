package com.avd.ui.main.video

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
import com.avd.databinding.DialogBottomMenuBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MenuBottomsheetFragment : BottomSheetDialogFragment() {

    private var binding : DialogBottomMenuBinding?=null
    private var mActivity: FragmentActivity? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()

    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DialogBottomMenuBinding.inflate(inflater,container,false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.clOpen?.setOnClickListener {
            listener?.onMenuCLick("play")
        }
        binding?.clRename?.setOnClickListener {
            listener?.onMenuCLick("rename")
        }
        binding?.clShare?.setOnClickListener {
            listener?.onMenuCLick("share")
        }
        binding?.clDelete?.setOnClickListener {
            listener?.onMenuCLick("delete")
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
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }


    private var listener : CustomMenuListener? = null

    fun  setMenuListeners(listener: CustomMenuListener){
        this.listener = listener
    }

    interface CustomMenuListener{
        fun onMenuCLick(which : String)
    }
}
