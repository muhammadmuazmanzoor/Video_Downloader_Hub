package com.video.avd.ui.status_saver.statusnew.guidance

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.databinding.FragmentWhatsappGuidanceHolderBinding
import com.video.avd.ui.MainActivity
import com.video.avd.utils.AppUtils


class WhatsappGuidanceHolderFragment : Fragment() {

    private var binding: FragmentWhatsappGuidanceHolderBinding? = null
    private var mActivity: FragmentActivity? = null

    override fun onResume() {
        super.onResume()
        if (mActivity is MainActivity){
            AppUtils.getMain(mActivity).hidebottombar()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWhatsappGuidanceHolderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated", "WhatsappGuidanceHolderFragment")
        val adapter = WhatsappGuidancePagerAdapter(childFragmentManager, lifecycle)
        adapter.addFragment(WhatsappGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 0))
        adapter.addFragment(WhatsappGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 1))
        adapter.addFragment(WhatsappGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 2))
        binding?.vpDownloadGuidance?.adapter = adapter
        binding?.vpDownloadGuidance?.isUserInputEnabled = false
        binding?.vpDownloadGuidance?.isUserInputEnabled = true

        binding?.vpDownloadGuidance?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                setCustomBackground(position)
                if (position == 2){
                    binding?.btnNext?.text = getString(R.string.got_it)
                    binding?.tvStep?.visibility = View.INVISIBLE
                    binding?.tvDesc?.visibility = View.INVISIBLE
                }else{
                    binding?.btnNext?.text = getString(R.string.next)
                    binding?.tvStep?.visibility = View.VISIBLE
                    binding?.tvDesc?.visibility = View.VISIBLE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })


        binding?.btnNext?.setOnClickListener {
            AppUtils.firebaseUserAction("nextButtonClicked", "WhatsappGuidanceHolderFragment")
            if (binding?.vpDownloadGuidance?.currentItem == 2){
                parentFragmentManager.popBackStack()
            }else binding?.vpDownloadGuidance?.currentItem = binding?.vpDownloadGuidance?.currentItem?.plus(1) ?: 1
        }
        binding?.tvSkip?.setOnClickListener {
            AppUtils.firebaseUserAction("skipButtonClicked", "WhatsappGuidanceHolderFragment")
            if(interHome!=null) {
                interHome?.let {
                    showInterstitial(true, it, mActivity?:requireActivity(), {
                        parentFragmentManager.popBackStack()
                    },inter_home)
                }

            }
            else{
                loadFallbackInterstitialAd(mActivity?:requireActivity(), BuildConfig.inter_home_high, BuildConfig.inter_home,inter_home_high,inter_home_normal,{
                    interHome=it
                },{
                    interHome=it
                })
                parentFragmentManager.popBackStack()
            }

        }

    }

    private fun setCustomBackground(currentItem: Int) {
        mActivity?.let {
            when (currentItem) {
                0 -> {
                    binding?.view1?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.gSelector_light)
                    binding?.view2?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view3?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.tvStep?.text = getString(R.string.step_1)
                    binding?.tvDesc?.text = getString(R.string.open_whatsapp)

                }

                1 -> {
                    binding?.view2?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.gSelector_light)
                    binding?.view1?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view3?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)

                    binding?.tvStep?.text = getString(R.string.step_2)
                    binding?.tvDesc?.text = getString(R.string.watch_whatsapp_status)

                }


                2 -> {
                    binding?.view3?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.gSelector_light)
                    binding?.view2?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view1?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                }

                else -> Log.e("", "")
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        mActivity = null
        super.onDetach()
    }
}