package com.video.avd.ui.onbooard

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.video.avd.ads.AdsHelper.getMediationInfo
import com.video.avd.ads.AdsHelper.isShowNativeFullCross
import com.video.avd.ads.AdsHelper.nativeFullCrossDelay
import com.video.avd.ads.AdsHelper.obCtaColor
import com.video.avd.ads.AdsHelper.obCtaTextStyle
import com.video.avd.ads.AdsHelper.obNativeAdFullScr1
import com.video.avd.ads.AdsHelper.obNativeAdHighFullScr1
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.video.avd.R
import com.video.avd.databinding.FragmentOnbaordFullNativeBinding

class OnBoardingFullScr1 : Fragment() {
    private var binding: FragmentOnbaordFullNativeBinding? = null

    private var mActivity: FragmentActivity? = null
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
        binding = FragmentOnbaordFullNativeBinding.inflate(inflater, container, false)
        return binding?.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            showNativeFull(activity)
        }
    }

    private fun showNativeFull(activity: FragmentActivity) {
        try {
            val nativeFull1 = obNativeAdHighFullScr1 ?: obNativeAdFullScr1
            nativeFull1?.let {
                val type = getMediationInfo(it)
//                val type = "meta"
                val layoutResId = when (type) {
                    "meta" -> R.layout.layout_native_full_screen_meta
                    else ->  R.layout.layout_native_full_screen
                }

                val adView = LayoutInflater.from(activity)
                    .inflate(layoutResId, null) as NativeAdView

                populateNativeAdView(it, adView, activity)

                binding?.shimmerContainerNative?.shimmerContainerNative?.stopShimmer()
                binding?.shimmerContainerNative?.shimmerContainerNative?.visibility = View.GONE
                binding?.nativeAdView?.removeAllViews()
                binding?.nativeAdView?.addView(adView)
                binding?.nativeAdView?.visibility = View.VISIBLE

                val close = adView.findViewById<ImageView>(R.id.closeAd)
                if (type == "meta"){
                    lifecycleScope.launch {
                        delay(nativeFullCrossDelay.toLong()*1000)
                        close?.visibility = View.VISIBLE
                    }
                }
                else{
                    if (isShowNativeFullCross){
                        lifecycleScope.launch {
                            delay(nativeFullCrossDelay.toLong()*1000)
                            close?.visibility = View.VISIBLE
                        }
                    }
                }
                close?.setOnClickListener {
                    nav.goNext()
//                    OnboardingActivity.selectedPosition.value = 4
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun populateNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        activity: FragmentActivity
    ) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        // adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? AppCompatButton)?.text = nativeAd.callToAction
        adView.mediaView?.mediaContent = nativeAd.mediaContent
        //    (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        (adView.callToActionView as? AppCompatButton)?.backgroundTintList =
            ColorStateList.valueOf(obCtaColor.toColorInt())
        val typeface = if (obCtaTextStyle.equals(
                "bold",
                ignoreCase = true
            )
        ) ResourcesCompat.getFont(activity, R.font.poppins_bold)
        else ResourcesCompat.getFont(activity, R.font.poppins_regular)
        (adView.callToActionView as? AppCompatButton)?.typeface = typeface
        adView.setNativeAd(nativeAd)
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