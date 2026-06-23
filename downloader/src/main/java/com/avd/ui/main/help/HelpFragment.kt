package com.avd.ui.main.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import com.avd.databinding.FragmentHelpBinding
import com.avd.ui.main.base.BaseFragment

class HelpFragment : BaseFragment() {

    private lateinit var dataBinding: FragmentHelpBinding


    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val color = getThemeBackgroundColor()
        dataBinding = FragmentHelpBinding.inflate(inflater, container, false).apply {
            this.getStartedOkButton.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            this.helpContainer.setBackgroundColor(color)
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }
        return dataBinding.root
    }


}