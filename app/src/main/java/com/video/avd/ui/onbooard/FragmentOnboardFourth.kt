package com.video.avd.ui.onbooard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper.isProVersion
import com.avd.util.DataStoreManager
import com.video.avd.ads.AdsHelper.displayNative
import com.video.avd.ads.AdsHelper.native_ob4
import com.video.avd.ads.AdsHelper.obNative1Enabled
import com.video.avd.ads.AdsHelper.obNative4Enabled
import com.video.avd.ads.AdsHelper.obNativeAd4
import com.video.avd.ads.AdsHelper.obNativeAdHigh4
import com.video.avd.ads.AdsHelper.obNativeHigh1Enabled
import com.video.avd.ads.AdsHelper.obNativeHigh4Enabled
import com.video.avd.databinding.FragmentOnbaordingFourthBinding
import com.video.avd.utils.AppUtils
import com.video.avd.utils.FirebaseLogUtils

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FragmentOnboardFourth : Fragment() {

    private var binding: FragmentOnbaordingFourthBinding? = null

    private val nav: PagerNav by lazy {
        (parentFragment as? PagerNav)
            ?: (activity as? PagerNav)
            ?: error(
                "Host must implement OnboardingFragment.PagerNav " +
                        "(either the parent fragment or the activity)."
            )
    }

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnbaordingFourthBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.fbEvents("onboarding_4_view", "Onboarding",mActivity)
        try {
            if (!obNativeHigh4Enabled && !obNative4Enabled) {
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
            }
            native_ob4.observe(viewLifecycleOwner){
                if(it==true){
                    if (isProVersion.value != true) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                if (obNativeAdHigh4 != null) {
                                    displayNative(
                                        obNativeAdHigh4,
                                        binding?.nativeAdView,
                                        mActivity,
                                        binding?.shimmerContainerNative?.shimmerContainerNative!!
                                    )
                                } else if (obNativeAd4 != null) {
                                    displayNative(
                                        obNativeAd4,
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
                        if (!obNativeHigh1Enabled && !obNative1Enabled) {
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
            if (isProVersion.value != true) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        if (obNativeAdHigh4 != null && obNativeHigh4Enabled) {
                            displayNative(
                                obNativeAdHigh4,
                                binding?.nativeAdView,
                                mActivity,
                                binding?.shimmerContainerNative?.shimmerContainerNative!!
                            )
                        } else if (obNativeAd4 != null && obNative4Enabled) {
                            displayNative(
                                obNativeAd4,
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
                if (!obNativeHigh4Enabled && !obNative4Enabled) {
                    binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
                }
            } else {
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.INVISIBLE
            }
            e.printStackTrace()
        }
        binding?.btnNextOnboarding?.setOnClickListener {
            AppUtils.fbEvents("onboarding_4_start", "Onboarding",mActivity)
            nav.goNext()
            // OnboardingActivity.selectedPosition.value = 4
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