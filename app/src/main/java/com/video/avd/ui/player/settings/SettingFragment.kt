package com.video.avd.ui.player.settings

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.video.avd.databinding.FragmentSettingBinding
import com.video.avd.extension.nextNavigateTo


class SettingFragment : Fragment() {

    lateinit var binding :FragmentSettingBinding

    private var mActivity: FragmentActivity? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentSettingBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTheme.setOnClickListener {

        }
        binding.layoutGeneral.setOnClickListener {
            mActivity?.let {
                it.nextNavigateTo(SettingFragmentDirections.actionSettingFragment2ToGeneralFragment())
            }
        }
        binding.layoutVideo.setOnClickListener {
            mActivity?.let {
                it.nextNavigateTo(SettingFragmentDirections.actionSettingFragment2ToVideoSettingFragment())
            }
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(mActivity)
        val themeName = pref.getString("prefTheme", "Default")
        binding.name.text = themeName
    }


}