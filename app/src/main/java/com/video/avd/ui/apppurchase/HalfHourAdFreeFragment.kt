package com.video.avd.ui.apppurchase

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.constent.is2Adwatched
import com.video.avd.databinding.FragmentHalfHourAdFreeBinding
import com.video.avd.ui.MainActivity
import com.video.avd.utils.AdDismissedListener
import com.video.avd.utils.AppPreference
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HalfHourAdFreeFragment : DialogFragment(), OnUserEarnedRewardListener, AdDismissedListener {

    private lateinit var binding: FragmentHalfHourAdFreeBinding

    private var mActivity: FragmentActivity? = null


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHalfHourAdFreeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        saveShownTime()
        binding.ivCross.setOnClickListener {
            dismiss()
        }
        binding.tvUnlock.setOnClickListener {
            if (mActivity?.let { NetworkUtils.isOnline(it) } == true) {
                binding.progressRewarded.visibility = View.VISIBLE
                viewLifecycleOwner.lifecycleScope.launch {
                    val maxAttempts = 4
                    var attempts = 0

                    var adShown = false

                    while (attempts < maxAttempts && !adShown) {
                        attempts++

                        mActivity?.let { activity ->

                            when(AdsManager.adSdkChoice){
                                "admob"->{
                                    // Check if the ad is already loaded
                                    if (AdsManager.rewardedAd != null) {
                                        AdsManager.showRewardedVideo(
                                            activity,
                                            activity,
                                            this@HalfHourAdFreeFragment,
                                        )
                                        adShown = true
                                    } else {
                                        mActivity?.let { AdsManager.loadRewardedAd(it) }
                                    }
                                }
                                "applovin"->{
                                    AdsManager.showRewardedVideoAppLovin(
                                        context = requireContext(),
                                        activity = requireActivity(),
                                        onUserEarnedRewardListener = {
                                            adShown = true
                                            mActivity?.let {
                                                AppPreference.save30MinutesEnabledValue(
                                                    it,
                                                    true
                                                )
                                            }
                                            val currentTime = System.currentTimeMillis()
                                            mActivity?.let {
                                                AppPreference.save24HoursEnabledTime(
                                                    it, currentTime
                                                )
                                            }
                                            GlobalValues.is24hourEnabled.value = true
                                            Toast.makeText(
                                                mActivity,
                                                getString(R.string.your_30_minutes_ad_free_reward_has_been_collected_successfully),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            binding.progressRewarded.visibility = View.GONE
                                            is2Adwatched = 0
                                            startActivity(
                                                Intent(
                                                    requireActivity(),
                                                    MainActivity::class.java
                                                )
                                            )
                                            mActivity?.finish()
                                            MainActivity.shouldProcessIntent = false
                                            dismiss()
                                            // Reward the user
                                            Log.d("AppLovin", "User earned reward!")
                                        },
                                        rewardAdDismissListener = {
                                            // Handle ad dismissal
                                            Log.d("AppLovin", "Ad dismissed!")
                                        }
                                    )
                                }
                            }
                            if (!adShown) {
                                delay(4000)
                            }
                        }
                    }
                    if (!adShown && isAdded) {
                        mActivity?.let {
                            binding.progressRewarded.visibility = View.GONE
                            Toast.makeText(it, "Failed to show ad, please try again.", Toast.LENGTH_SHORT).show()
                        }

                    }
                }
            } else {
                if (isAdded){
                    mActivity?.let {
                        Toast.makeText(it, getString(R.string.please_turn_your_internet_on), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveShownTime() {
        val currentTime = System.currentTimeMillis()
        mActivity?.let { AppPreference.save30MinutesShownTime(it, currentTime) }
    }

    override fun onUserEarnedReward(p0: RewardItem) {
        mActivity?.let { AppPreference.save30MinutesEnabledValue(it, true) }
        val currentTime = System.currentTimeMillis()
        mActivity?.let {
            AppPreference.save24HoursEnabledTime(
                it, currentTime
            )
        }
        GlobalValues.is24hourEnabled.value = true
        Toast.makeText(mActivity, getString(R.string.your_30_minutes_ad_free_reward_has_been_collected_successfully), Toast.LENGTH_SHORT).show()
        binding.progressRewarded.visibility = View.GONE
        is2Adwatched=0
        startActivity(Intent(requireActivity(), MainActivity::class.java))
        mActivity?.finish()
        MainActivity.shouldProcessIntent = false
        dismiss()
    }

    override fun onAdDismissed() {
          dismiss()
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