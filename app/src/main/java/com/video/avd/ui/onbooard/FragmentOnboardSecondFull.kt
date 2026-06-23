package com.video.avd.ui.onbooard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.video.avd.utils.FirebaseLogUtils
import com.video.avd.databinding.FragmentOnbaordingSecondBinding
import com.video.avd.utils.AppUtils

class FragmentOnboardSecondFull : Fragment() {

    private var binding : FragmentOnbaordingSecondBinding? = null

    private val nav: PagerNav by lazy {
        (parentFragment as? PagerNav)
            ?: (activity as? PagerNav)
            ?: error(
                "Host must implement OnboardingFragment.PagerNav " +
                        "(either the parent fragment or the activity)."
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnbaordingSecondBinding.inflate(inflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.fbEvents("onboarding_2_view", "Onboarding",mActivity)
        binding?.btnNextOnboarding?.setOnClickListener {
            AppUtils.fbEvents("onboarding_2_next", "Onboarding",mActivity)
           // OnboardingActivity.selectedPosition.value = 2
            nav.goNext()
        }
    }

    private var mActivity: FragmentActivity? = null
          override fun onAttach(context: Context) {
             super.onAttach(context)
             mActivity = requireActivity()
         }

         override fun onDetach() {
             super.onDetach()
             mActivity = null
         }
}