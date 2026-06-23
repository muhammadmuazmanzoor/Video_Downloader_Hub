package com.video.avd.ui.download_guidance

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
import com.video.avd.R
import com.video.avd.databinding.FragmentDownloadGuidanceHolderBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ui.video_downloader.guidance.DownloadGuidancePagerAdapter
import com.video.avd.utils.AppUtils


class DownloadGuidanceHolderFragment : Fragment() {

    private var binding: FragmentDownloadGuidanceHolderBinding? = null
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
        binding = FragmentDownloadGuidanceHolderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated", "DownloadGuidanceHolderFragment")
        val adapter = DownloadGuidancePagerAdapter(childFragmentManager, lifecycle)
        adapter.addFragment(DownloadGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 0))
        adapter.addFragment(DownloadGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 1))
        adapter.addFragment(DownloadGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 2))
        adapter.addFragment(DownloadGuidanceFragment(binding?.vpDownloadGuidance?.currentItem ?: 3))
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
                if (position == 3){
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
            AppUtils.firebaseUserAction("nextBtnClicked_DownloadsTutorial", "DownloadTutorial")
            if (binding?.vpDownloadGuidance?.currentItem == 3){
                mActivity?.let {
                    mActivity?.let { it.finish() }
                   // it.nextNavigateTo(DownloadGuidanceHolderFragmentDirections.actionDownloadGuidanceFragmentToHomeDownloadFragment())
                }
            }else binding?.vpDownloadGuidance?.currentItem = binding?.vpDownloadGuidance?.currentItem?.plus(1) ?: 1
        }
        binding?.tvSkip?.setOnClickListener {
            AppUtils.firebaseUserAction("skipBtnClicked_DownloadTutorial", "DownloadTutorial")
            mActivity?.let {
                mActivity?.let { it.finish() }
              //  it.nextNavigateTo(DownloadGuidanceHolderFragmentDirections.actionDownloadGuidanceFragmentToHomeDownloadFragment())
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
                    binding?.view4?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)

                    binding?.tvStep?.text = getString(R.string.step_1)
                    binding?.tvDesc?.text = getString(R.string.go_website)

                }

                1 -> {
                    binding?.view2?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.gSelector_light)
                    binding?.view1?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view3?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view4?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)

                    binding?.tvStep?.text = getString(R.string.step_2)
                    binding?.tvDesc?.text = getString(R.string.find_videos)

                }

                2 -> {
                    binding?.view3?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.gSelector_light)
                    binding?.view2?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view1?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view4?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.tvStep?.text = getString(R.string.step_3)
                    binding?.tvDesc?.text = getString(R.string.click_download_btn)

                }

                3 -> {
                    binding?.view4?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.gSelector_light)
                    binding?.view2?.backgroundTintList =
                        ContextCompat.getColorStateList(it, R.color.nonSelectedColor)
                    binding?.view3?.backgroundTintList =
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