package com.video.avd.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.extension.openLink
import com.video.avd.utils.AppUtils
import com.avd.util.AdBlockerHelper
import kotlinx.coroutines.launch


class SettingsFragment : Fragment() {

    private var mActivity: FragmentActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated_SettingsFragment", "SettingsFragment")
        val packageName = requireContext().packageName
        val activity = requireActivity()

        val menuIcon = view.findViewById<ImageView>(R.id.menuIcon)
        val shareText = view.findViewById<TextView>(R.id.shareText)
        val shareIcon = view.findViewById<ImageView>(R.id.imageView2)
        val privacyPolicyText = view.findViewById<TextView>(R.id.privacyPolicyText)
        val privacyIcon = view.findViewById<ImageView>(R.id.imageView4)
        val rateUsText = view.findViewById<TextView>(R.id.rateUsText)
        val rateUsIcon = view.findViewById<ImageView>(R.id.imageView3)

        menuIcon?.setOnClickListener {
            findNavController().popBackStack()
        }

        val shareClick = {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBody = "Download " + resources.getString(R.string.app_name) + " using https://play.google.com/store/apps/details?id=$packageName"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Download link")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }
        shareText?.setOnClickListener { shareClick() }
        shareIcon?.setOnClickListener { shareClick() }
        shareText?.isClickable = true
        shareText?.isFocusable = true

        val privacyClick = {
            AdBlockerHelper.setinterstitialshown(true)
            activity openLink "https://www.xilliapps.com/privacy-policy.html"
        }
        privacyPolicyText?.setOnClickListener { privacyClick() }
        privacyIcon?.setOnClickListener { privacyClick() }
        privacyPolicyText?.isClickable = true
        privacyPolicyText?.isFocusable = true

        val rateUsClick = {
            AdBlockerHelper.setinterstitialshown(true)
            activity openLink "https://play.google.com/store/apps/details?id=$packageName"
        }
        rateUsText?.setOnClickListener { rateUsClick() }
        rateUsIcon?.setOnClickListener { rateUsClick() }
        rateUsText?.isClickable = true
        rateUsText?.isFocusable = true

        lifecycleScope.launch {
            mActivity?.let {
                if(!it.isFinishing){
                    AdsManager.refreshAd(
                        view,
                        view.findViewById<ImageView>(R.id.ourgraphic),
                        it,
                        isDetached,
                        this@SettingsFragment,
                        view.findViewById<FrameLayout>(R.id.fl_adplace)
                    )
                }
            }
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
}