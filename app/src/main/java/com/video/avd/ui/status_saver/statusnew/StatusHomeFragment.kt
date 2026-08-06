package com.video.avd.ui.status_saver.statusnew

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.google.android.material.tabs.TabLayoutMediator
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.constent.isvideo
import com.video.avd.databinding.FragmentStatusSaverBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ui.status_saver.RecentFragment
import com.video.avd.ui.status_saver.StatusViewModel
import com.video.avd.ui.status_saver.WhatsAppNotInstalledDialog
import com.video.avd.ui.status_saver.statusnew.guidance.WhatsappGuidanceHolderFragment
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AndroidEntryPoint
class StatusHomeFragment : Fragment() {
    var binding: FragmentStatusSaverBinding? = null
    private var mActivity: FragmentActivity? = null
    private val viewModel: StatusViewModel by activityViewModels()
    var isFavourite = false
    private var isClickable = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isvideo = false
    }

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
        binding = FragmentStatusSaverBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            val messengerInstalled = isPackageInstalled(activity, WHATSAPP_PACKAGE)
            val businessInstalled = isPackageInstalled(activity, WHATSAPP_BUSINESS_PACKAGE)
            Log.d(TAG, "installedApps: messenger=$messengerInstalled, business=$businessInstalled")

            binding?.waToggle?.setOnCheckedChangeListener { _, isChecked ->
                Log.d(TAG, "toggle requested: business=$isChecked")
                if (isChecked && !businessInstalled) {
                    showNotInstalledDialog(getString(R.string.whatsapp_business_not_installed))
                    binding?.waToggle?.isChecked = false
                    return@setOnCheckedChangeListener
                }
                if (!isChecked && !messengerInstalled && businessInstalled) {
                    showNotInstalledDialog(getString(R.string.whatsapp_messenger_not_installed))
                    binding?.waToggle?.isChecked = true
                    return@setOnCheckedChangeListener
                }
                isBusinessWhatsapp.value = isChecked
                AppPreference.setWhatsappSelected(activity, isChecked)
                val text = if (isChecked) getString(R.string.wa_business) else getString(R.string.wa_saver)
                binding?.tvName?.text = text
            }

            val isBusinessSelected = when {
                !businessInstalled -> false
                !messengerInstalled -> true
                else -> AppPreference.isWhatsappBusinessSelected(activity)
            }
            Log.d(TAG, "initial selection: business=$isBusinessSelected")
            isBusinessWhatsapp.value = isBusinessSelected
            binding?.waToggle?.isChecked = isBusinessSelected
            // Keep the switch tappable so selecting an unavailable variant can
            // explain why the selection was rejected.
            binding?.waToggle?.isEnabled = true
            binding?.tvName?.text = if (isBusinessSelected) {
                getString(R.string.wa_business)
            } else {
                getString(R.string.wa_saver)
            }
            AppPreference.setWhatsappSelected(activity, isBusinessSelected)

            if (activity is MainActivity) {
                AppUtils.getMain(activity).showBannerAd()
            }

            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isClickable) {
                        isClickable = false
                        lifecycleScope.launch {
                            delay(1000)
                            isClickable = true
                        }
                        showInterstitialHome(activity = activity, forFragment = true) {
                            try {
                                findNavController().popBackStack()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
//                        showInterstitialAdonBackFragment(activity)
                    }
                }
            }
            activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

            binding?.imgReload?.visibility = View.GONE
            clickListeners()
            AppUtils.firebaseUserAction("onCreateView_StatusSaverFragment", "StatusSaverFragment")
            val adapter = HomePagerAdapterStatus(childFragmentManager, lifecycle)
            adapter.addFragment(RecentFragment())
            adapter.addFragment(SavedFragment())
            binding?.waViewPager?.adapter = adapter
            binding?.waViewPager?.isUserInputEnabled = true
            binding?.waTabLayout?.selectTab(binding?.waTabLayout?.getTabAt(0))
            binding?.waTabLayout?.tabRippleColor = null
            binding?.waTabLayout?.let {
                binding?.waViewPager?.let { it1 ->
                    TabLayoutMediator(it, it1) { tab, position ->
                        when (position) {
                            0 -> tab.text = getString(R.string.recent)
                            1 -> tab.text = getString(R.string.saved)
                        }
                    }.attach()
                }
            }
            if (activity is MainActivity){
                AppUtils.getMain(activity).hidebottombar()
            }
        }
    }

    /**
     * Shows a small centered dialog (transparent, dimmed background) in
     * place of a Toast to explain why a WhatsApp variant selection was
     * rejected.
     */
    private fun showNotInstalledDialog(message: String) {
        WhatsAppNotInstalledDialog.newInstance(message)
            .show(childFragmentManager, "wa_not_installed")
    }

    private fun clickListeners() {
        mActivity?.let { activity ->
            binding?.waBack?.setOnClickListener {
                if (activity is MainActivity) {
                    if (isClickable) {
                        isClickable = false
                        lifecycleScope.launch {
                            delay(1000)
                            isClickable = true
                        }
                        mActivity?.let { activity ->
                            if(interHome!=null) {
                                interHome?.let {
                                    showInterstitial(true, it, mActivity?:requireActivity(), {
                                        lifecycleScope.launch {
                                            delay(80)
                                            try {
                                                activity.findNavController(R.id.nav_host).popBackStack()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },inter_home)
                                }

                            }
                            else{
                                loadFallbackInterstitialAd(mActivity?:requireActivity(), BuildConfig.inter_home_high, BuildConfig.inter_home,inter_home_high,inter_home_normal,{
                                    interHome=it
                                },{
                                    interHome=it
                                })
                                lifecycleScope.launch {
                                    delay(80)
                                    try {
                                        activity.findNavController(R.id.nav_host).popBackStack()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    } else {
                    }
                } else {
                    activity.onBackPressed()
                }
            }


            binding?.imgInfo?.setOnClickListener {
                AppUtils.firebaseUserAction("helpButtonClicked", "StatusHomeFragment")

                try {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.whats_container_view, WhatsappGuidanceHolderFragment())   // your host view ID
                        .addToBackStack(WhatsappGuidanceHolderFragment::class.java.simpleName) // keep back‑stack behaviour
                        .setReorderingAllowed(true)   // recommended for Fragment 1.4.0+
                        .commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            howToDownloadClicked.observe(viewLifecycleOwner, Observer {
                if (it) {
                    howToDownloadClicked.value = false
                    AppUtils.firebaseUserAction("helpButtonClicked", "StatusHomeFragment")
                    parentFragmentManager.beginTransaction()
                        // Optional: custom enter/exit animations to mimic NavOptions
                        // .setCustomAnimations(
                        //     R.anim.slide_in_right,  // enter
                        //     R.anim.slide_out_left,  // exit
                        //     R.anim.slide_in_left,   // pop enter
                        //     R.anim.slide_out_right  // pop exit
                        // )
                        .replace(R.id.whats_container_view, WhatsappGuidanceHolderFragment())   // your host view ID
                        .addToBackStack(WhatsappGuidanceHolderFragment::class.java.simpleName) // keep back‑stack behaviour
                        .setReorderingAllowed(true)   // recommended for Fragment 1.4.0+
                        .commit()
                }
            })


        }


    }

    private fun bottomDialog() {
        val bottomSheetFragment = BottomsheetStatusFragment()
        bottomSheetFragment.show(requireActivity().supportFragmentManager, bottomSheetFragment.tag)
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        if (mActivity is MainActivity) {
            AppUtils.getMain(mActivity).showbottombar()
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "StatusDebug"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        var isBusinessWhatsapp = MutableLiveData(false)
        var howToDownloadClicked = MutableLiveData(false)
        var showInterstitialOnBack = false
    }


}