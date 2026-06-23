package com.video.avd.ui.status_saver.statusnew.guidance

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.video.avd.R
import com.video.avd.databinding.FragmentWhatsappGuidanceBinding

class WhatsappGuidanceFragment(private val currentPosition:Int) : Fragment() {
    private var binding:FragmentWhatsappGuidanceBinding?=null
    private var mActivity: FragmentActivity? = null

    constructor() : this(0)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWhatsappGuidanceBinding.inflate(inflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let {
            when(currentPosition){
                0-> {
                    binding?.img?.setImageDrawable(ContextCompat.getDrawable(it, R.drawable.whatsapp_guide1))
                    binding?.img?.visibility=View.VISIBLE
                    binding?.tvDisclaimer?.visibility=View.GONE
                    binding?.tvGetPermission?.visibility=View.GONE
                }
                1-> {
                    binding?.img?.setImageDrawable(ContextCompat.getDrawable(it,R.drawable.whatsapp_guide2))
                    binding?.img?.visibility=View.VISIBLE
                    binding?.tvDisclaimer?.visibility=View.GONE
                    binding?.tvGetPermission?.visibility=View.GONE

                }

                2-> {
                    binding?.img?.visibility=View.GONE
                    binding?.tvDisclaimer?.visibility=View.VISIBLE
                    binding?.tvGetPermission?.visibility=View.VISIBLE
                }
                else -> Log.e("ERROR", "invalid position in DownloadGuidanceFramgment on line 65")
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }
    override fun onDetach() {
        mActivity = null
        super.onDetach()
    }
}