package com.video.avd.ui.apppurchase

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.databinding.DialogPremiumBinding


class PremiumDialog : DialogFragment() {

    private lateinit var binding: DialogPremiumBinding
    private lateinit var lottieAnimationView: LottieAnimationView

    companion object {
        var isMp3 = false
    }

    var listener: PremiumDialogListener? = null

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DialogPremiumBinding.inflate(inflater, container, false)

        AdsManager.loadRewardedAd(requireContext())
       //  loadAnimColor()
        if (isMp3) {
            binding.tvWatchVideo.text = getString(R.string.watch_a_video_ad_to_convert_video_to_mp3)
           // binding.clPro.text = "Unlock the Full Experience - Go Premium!"
            isMp3 = false
        } else {

        }
        binding.clPro.setOnClickListener {
            listener?.onWatchVideoClick()
            dismiss()
        }

        binding.tvUnlock.setOnClickListener {
            listener?.onUnlockThemeClick()
            dismiss()
        }

        binding.ivCross.setOnClickListener { dismiss() }

        return binding.root
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)


        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val back = ColorDrawable(Color.TRANSPARENT)
            val inset = InsetDrawable(back, 35)
            dialog.window?.setBackgroundDrawable(inset)
        }
    }

    fun setListner(listener: PremiumDialogListener) {
        this.listener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.rewardAnim?.cancelAnimation()
    }

}