package com.video.avd.ui.player.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.databinding.FragmentGeneralBinding
import com.video.avd.extension.openLink
import com.video.avd.ui.splash_flow.activities.LanguageActivity
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.avd.util.AdBlockerHelper


class GeneralFragment : Fragment() {

    lateinit var binding: FragmentGeneralBinding
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
        binding=FragmentGeneralBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
        val packageName = mActivity?.packageName
        binding.layoutLanguage.setOnClickListener {
            startActivity(LanguageActivity.createIntent(requireContext(), true))
        }
       val isHistory= mActivity?.let { AppPreference.isHistoryOn(it) } == true
       binding.simpleSwitch.isChecked=isHistory
        if (isHistory){
            binding.name2.text="On"
        }else{
            binding.name2.text="Off"
        }
        binding.simpleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // The switch is on/checked
                binding.name2.text="On"
                mActivity?.let { AppPreference.setHistoryOn(it, isFirstLaunch = true) }
            } else {
                // The switch is off/unchecked
                mActivity?.let { AppPreference.setHistoryOn(it, isFirstLaunch = false) }
                binding.name2.text="Off"
            }
        }

        binding.layoutRate.setOnClickListener {
            AppUtils.firebaseUserAction("reteUsBtnClicked_GeneralFragment", "GeneralFragment")
            // Prevent app open ad when returning from Play Store
            // Set flag synchronously BEFORE opening link to ensure it's set before activity pauses
            AdBlockerHelper.setinterstitialshown(true)
            requireActivity() openLink "https://play.google.com/store/apps/details?id=$packageName"
        }

        binding.layoutShare.setOnClickListener {
            AppUtils.firebaseUserAction("shareBtnClicked_GeneralFragment", "GeneralFragment")
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBody = "Download " + resources.getString(R.string.app_name) + " using https://play.google.com/store/apps/details?id=$packageName"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Download link")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
            AppUtils.firebaseUserAction("shareAppClicked_GeneralFragment", "GeneralFragment")
        }

        binding.layoutPrivacy.setOnClickListener {
            AppUtils.firebaseUserAction(
                "privacy_policyBtnClicked_GeneralFragment",
                "GeneralFragment"
            )
            // Prevent app open ad when returning from Privacy Policy
            // Set flag synchronously BEFORE opening link to ensure it's set before activity pauses
            AdBlockerHelper.setinterstitialshown(true)
            requireActivity() openLink("https://tflsignatureapps.terafort.com/privacy-policy.html")
        }
        binding.version.text = "Version: ${BuildConfig.VERSION_NAME}"

    }

}
