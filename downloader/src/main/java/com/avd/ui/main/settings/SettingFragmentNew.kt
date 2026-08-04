package com.avd.ui.main.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.facebook.shimmer.ShimmerFrameLayout
import com.avd.R
import com.avd.databinding.FragmentSettingNewBinding
import com.avd.ui.main.home.browser.webTab.BrowserTabFragment
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.browser_native
import com.avd.util.AdBlockerHelper.exit_native
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_browser
import com.avd.util.AdBlockerHelper.refreshAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.AppLogger
import com.avd.util.CommunicateWithActivity
import com.avd.util.SharedPrefHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragmentNew : Fragment() {

    companion object {
        fun newInstance() = SettingFragmentNew()
    }

    @Inject
    lateinit var sharedPrefHelper :SharedPrefHelper

    private var host: CommunicateWithActivity? = null

    private var binding : FragmentSettingNewBinding ?= null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("FragmentLifecycle", "onAttach called")
        try {
            host = context as? CommunicateWithActivity ?: error("Activity must implement HostActions")
        } catch (e: Exception) {
            Log.e("FragmentLifecycle", "Error in onAttach: ${e.localizedMessage}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        host?.hideBottomBar()
        Log.d("FragmentLifecycle", "onResume: Bottom bar hidden")
        // Prevent app open ad when settings fragment is visible
        AdBlockerHelper.setinterstitialshown(true)
    }

    override fun onPause() {
        super.onPause()
        // Reset flag when settings fragment is paused, with a delay to ensure normal behavior resumes
        lifecycleScope.launch {
            delay(3000) // Wait 3 seconds after settings fragment is paused before allowing app open ads
            AdBlockerHelper.setinterstitialshown(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingNewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AdBlockerHelper.isProVersion.observe(viewLifecycleOwner, Observer { it ->
            if (it == true) {
                binding?.clPremimum?.visibility = View.GONE
                binding?.flAdplace?.visibility = View.GONE
            } else {
                showShimmer(true) // Show shimmer before loading ad
                refreshAd(this@SettingFragmentNew, requireContext(), true, binding?.flAdplace,exit_native)
            }
        })


        binding?.waBack?.setOnClickListener {
//            parentFragmentManager.popBackStack()
            navigateToHome()
            host?.showBottomBar()
        }

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateToHome()
//                    parentFragmentManager.popBackStack()
                }
            }
        )

        binding?.toggleButton1?.isChecked = sharedPrefHelper.getDownloadWifi()

        binding?.toggleButton1?.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefHelper.saveDownloadWifi(isChecked)
        }

        binding?.langGroup?.setOnClickListener {
            try {
                val activityClass = Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                val intent = Intent(requireContext(), activityClass)
                intent.putExtra("where", "language")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }catch (e : Exception){
                e.printStackTrace()
            }
        }

        binding?.clPremimum?.setOnClickListener {
            try {
                val activityClass = Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                val intent = Intent(requireContext(), activityClass)
                intent.putExtra("where", "propanel")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }catch (e : Exception){
                e.printStackTrace()
            }
        }

        binding?.clHowDownload?.setOnClickListener {
            try {
                val activityClass = Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
                val intent = Intent(requireContext(), activityClass)
                intent.putExtra("where", "download")
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }catch (e : Exception){
                e.printStackTrace()
            }
        }

        binding?.labelshare?.setOnClickListener {
            Log.e("checkSetting","click: shareGroup")
            // Prevent app open ad when returning from Share
            // Set flag synchronously BEFORE opening share dialog to ensure it's set before activity pauses
            AdBlockerHelper.setinterstitialshown(true)
            requireContext().shareApp()
        }

        binding?.labelrate?.setOnClickListener {
            Log.e("checkSetting","click: rateGroup")
            // Prevent app open ad when returning from Play Store
            // Set flag synchronously BEFORE opening Play Store to ensure it's set before activity pauses
            AdBlockerHelper.setinterstitialshown(true)
            requireContext().rateApp()
        }

        binding?.labelprivacy?.setOnClickListener {
            Log.e("checkSetting","click: privacyGroup")
            // Prevent app open ad when returning from Privacy Policy
            // Set flag synchronously BEFORE opening link to ensure it's set before activity pauses
            AdBlockerHelper.setinterstitialshown(true)
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tflsignatureapps.terafort.com/privacy-policy.html"))
                startActivity(browserIntent)
                Log.d("checkSetting","browserIntent Fun")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showShimmer(show: Boolean) {
        if (show) {
            AdBlockerHelper.setupNativeShimmer(binding?.flAdplace, layoutInflater)
        } else {
            AdBlockerHelper.hideNativeShimmer(binding?.flAdplace)
        }
    }
    fun navigateToHome() {
        fun returnToPreviousBrowser() {
            val hostActivity = activity ?: return
            val fragmentManager = hostActivity.supportFragmentManager

            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded || hostActivity.isFinishing || hostActivity.isDestroyed) return@launch

                val popped = try {
                    fragmentManager.popBackStackImmediate(
                        "settings",
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                } catch (e: Exception) {
                    AppLogger.d("Failed to pop settings back stack: $e")
                    false
                }

                if (popped) {
                    return@launch
                }

                try {
                    val activityFragmentContainer =
                        hostActivity.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                    activityFragmentContainer?.let {
                        val transaction = fragmentManager.beginTransaction()
                            .replace(
                                it.id,
                                BrowserTabFragment.newInstance()
                            )
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

                        if (fragmentManager.isStateSaved) {
                            transaction.commitAllowingStateLoss()
                        } else {
                            transaction.commit()
                        }
                        fragmentManager.executePendingTransactions()
                    }
                } catch (e: Exception) {
                    AppLogger.d("Can't get the fragment manager with this: $e")
                }
            }
        }

        if(interHome!=null) {
            interHome?.let {
                showInterstitial(true, it, requireActivity(), {
                    returnToPreviousBrowser()
                },inter_browser)
            }

        }
        else{
            returnToPreviousBrowser()
        }

    }
    fun Context.shareApp() {
        val appPackage = packageName
        val shareText = "Check out this awesome app:\\n\" + \"https://play.google.com/store/apps/details?id=$appPackage"
        Log.d("checkSetting","shareApp Fun")
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Video Downloader Browser Hub")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val packageManager = packageManager
        val resolveInfos = packageManager.queryIntentActivities(sendIntent, 0)

        // Filter and convert to mutable list
        val targetIntents = resolveInfos
            .filter { it.activityInfo.packageName != appPackage } // Exclude your own app
            .map {
                Intent(sendIntent).apply {
                    `package` = it.activityInfo.packageName
                    setClassName(it.activityInfo.packageName, it.activityInfo.name)
                }
            }.toMutableList() // Make mutable for removeAt

        if (targetIntents.isNotEmpty()) {
            val chooserIntent = Intent.createChooser(targetIntents.removeAt(0), "Share via")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toTypedArray())
            startActivity(chooserIntent)
        } else {
            Toast.makeText(this, "No app available to share.", Toast.LENGTH_SHORT).show()
        }
    }


    fun Context.rateApp() {
        val appPackage = packageName
        val uriMarket = Uri.parse("market://details?id=$appPackage")
        val uriWeb    = Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")
        Log.d("checkSetting","rateApp Fun")
        // First try Google Play app
        val goToMarket = Intent(Intent.ACTION_VIEW, uriMarket).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }

        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            // Google Play not installed – open in browser
            startActivity(Intent(Intent.ACTION_VIEW, uriWeb))
        }
    }

    override fun onDetach() {
        super.onDetach()
        // Reset flag when fragment is detached to ensure normal behavior resumes
        lifecycleScope.launch {
            delay(3000) // Wait 3 seconds after detaching before allowing app open ads
            AdBlockerHelper.setinterstitialshown(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        showShimmer(false) // Stop shimmer when fragment is destroyed
        try {
            host?.showBottomBar()
            host = null
        } catch (e: Exception) {
            Log.e("FragmentLifecycle", "Error in onDestroy: ${e.localizedMessage}", e)
        }
    }
}
