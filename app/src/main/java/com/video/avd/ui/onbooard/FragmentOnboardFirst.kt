package com.video.avd.ui.onbooard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.displayNative
import com.video.avd.ads.AdsHelper.native_ob1
import com.video.avd.ads.AdsHelper.obNative1Enabled
import com.video.avd.ads.AdsHelper.obNativeHigh1Enabled
import com.video.avd.utils.FirebaseLogUtils
import com.video.avd.databinding.FragmentOnbaordingFirstBinding
import com.video.avd.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentOnboardFirst : Fragment() {

    private var binding: FragmentOnbaordingFirstBinding? = null

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
        binding = FragmentOnbaordingFirstBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FirebaseLogUtils.logEvent("onboarding_1_view", "")
        AppUtils.fbEvents("onboarding_1_view", "Onboarding",mActivity)
        try {
            if (!obNativeHigh1Enabled && !obNative1Enabled) {
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
            }
            native_ob1.observe(viewLifecycleOwner){
                if(it==true){
                    if (AdBlockerHelper.isProVersion.value != true) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                if (AdsHelper.obNativeAdHigh1 != null) {
                                    displayNative(
                                        AdsHelper.obNativeAdHigh1,
                                        binding?.nativeAdView,
                                        mActivity,
                                        binding?.shimmerContainerNative?.shimmerContainerNative!!
                                    )
                                } else if (AdsHelper.obNativeAd1 != null) {
                                    displayNative(
                                        AdsHelper.obNativeAd1,
                                        binding?.nativeAdView,
                                        mActivity,
                                        binding?.shimmerContainerNative?.shimmerContainerNative!!
                                    )
                                } else {
                                    binding?.apply {
                                        shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                                            shimmerLayout.stopShimmer()
                                            shimmerLayout.visibility = View.INVISIBLE
                                        }
                                    }
                                }

                            }
                        }
                        binding?.apply {
                            shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                                shimmerLayout.startShimmer()
                            }
                        }
                        if (!AdsHelper.obNativeHigh1Enabled && !AdsHelper.obNative1Enabled) {
                            binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                        }
                    } else {
                        binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                    }
                }
                else if(it==false){
                    binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                }
            }
        } catch (e: Exception) {
            if (AdBlockerHelper.isProVersion.value != true) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        if (AdsHelper.obNativeAdHigh1 != null) {
                            displayNative(
                                AdsHelper.obNativeAdHigh1,
                                binding?.nativeAdView,
                                mActivity,
                                binding?.shimmerContainerNative?.shimmerContainerNative!!
                            )
                        } else if (AdsHelper.obNativeAd1 != null) {
                            displayNative(
                                AdsHelper.obNativeAd1,
                                binding?.nativeAdView,
                                mActivity,
                                binding?.shimmerContainerNative?.shimmerContainerNative!!
                            )
                        } else {
                            binding?.apply {
                                shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                                    shimmerLayout.stopShimmer()
                                    shimmerLayout.visibility = View.INVISIBLE
                                }
                            }
                        }

                    }
                }
                binding?.apply {
                    shimmerContainerNative.shimmerContainerNative.let { shimmerLayout ->
                        shimmerLayout.startShimmer()
                    }
                }
                if (!AdsHelper.obNativeHigh1Enabled && !AdsHelper.obNative1Enabled) {
                    binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                }
            } else {
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
            }
            e.printStackTrace()
        }

        binding?.btnNextOnboarding?.setOnClickListener {
            AppUtils.fbEvents("onboarding_1_next", "Onboarding",mActivity)
//            FirebaseLogUtils.logEvent("onboarding_1_next", "")
            nav.goNext()
           // OnboardingActivity.selectedPosition.value = 1
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