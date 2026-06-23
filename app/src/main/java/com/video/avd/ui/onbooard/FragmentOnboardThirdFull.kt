package com.video.avd.ui.onbooard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper
import com.avd.util.DataStoreManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.obsez.android.lib.filechooser.permissions.PermissionActivity
import com.video.avd.MyApplication
import com.video.avd.ads.interstitialOb
import com.video.avd.databinding.FragmentOnbaordingFifthBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ads.AppOpenManager.Companion.isShowingAd
import com.video.avd.utils.AppUtils
import com.video.avd.utils.FirebaseLogUtils
import com.video.avd.utils.GlobalLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FragmentOnboardThirdFull : Fragment() {

    private var binding: FragmentOnbaordingFifthBinding? = null

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
        binding = FragmentOnbaordingFifthBinding.inflate(inflater, container, false)
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity->

            FirebaseLogUtils.logEvent("onboarding_5_view", "")
            AppUtils.fbEvents("onboarding_3_view", "Onboarding",mActivity)
            binding?.btnNextOnboarding?.setOnClickListener {
                FirebaseLogUtils.logEvent("onboarding_5_next", "")
                nav.goNext()
                AppUtils.fbEvents("onboarding_3_next", "Onboarding",mActivity)
                //OnboardingActivity.selectedPosition.value = 3
//                showInterOb(activity)
            }
        }
    }

    fun showInterOb(
        currentActivity: FragmentActivity,
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (AdBlockerHelper.isProVersion.value != true) {

                    if (interstitialOb != null) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        interstitialOb?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isShowingAd = true
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                      /*  LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialOb?.adUnitId.toString()
                                        )*/
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialOb = null
                                   /* LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialOb?.adUnitId.toString()
                                    )*/
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialOb = null
                                    isShowingAd = false
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialOb = null

                                }
                            }

                       navigateNext(currentActivity)

                        if (interstitialOb != null) {
                            interstitialOb?.show(currentActivity)
                        } else {
                            GlobalLoader.hide(currentActivity)

                        }
                        interstitialOb = null
                    } else {
                        interstitialOb = null

                        navigateNext(currentActivity)

                    }


                } else {
                    navigateNext(currentActivity)

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun navigateNext(currentActivity: FragmentActivity){
        if (MyApplication.isShowPermission){
            //  startActivity(Intent(currentActivity, MainActivity::class.java))
            startActivity(Intent(currentActivity, PermissionActivity::class.java))
            currentActivity.finish()
        }else{
            startActivity(Intent(currentActivity, MainActivity::class.java))
            currentActivity.finish()
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