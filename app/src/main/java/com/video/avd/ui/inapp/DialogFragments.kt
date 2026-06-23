package com.video.avd.ui.inapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.video.avd.R
import com.video.avd.databinding.ActivityInAppBinding
import com.video.avd.ui.MainActivity
import com.video.avd.utils.AppPreference.isFromSplash
import com.video.avd.utils.AppUtils
import com.video.avd.utils.InAppPurchases
import com.video.avd.utils.InAppPurchases.Companion.SKU_WEEKLY
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.ToastUtils
import com.avd.util.AdBlockerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DialogFragments : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var binding: ActivityInAppBinding? = null
    private val viewModel: ProViewModel by viewModels()
    private val args: DialogFragmentsArgs by navArgs()
    private var nullvalue: Boolean? = false
    var mActivity: FragmentActivity? = null
    private lateinit var sharedPreferences: SharedPreferences
    var sku: String? = null
    private var trialDays = 0
    private var monthlyPrice = ""
    private var freeTrailc = ""
    private var trialString = ""
    var isEverSubscribe = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityInAppBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner
        lifecycleScope.launchWhenStarted {
            activity?.onBackPressedDispatcher?.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback
            )
            delay(2000)
            binding?.closeButton?.visibility = View.VISIBLE
        }
        AppUtils.firebaseUserAction("pro_view", "inappscreen")
        return binding?.root
    }

    private fun prepareNavigation() {
        lifecycleScope.launchWhenStarted {
            mActivity?.let {
                viewModel.inAppPurchases.getPrice(it)
            }
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Handle the back button event
            try {
                if (isFromSplash) {
                    if (args.isShowAd) {
                        val intent = Intent(mActivity, MainActivity::class.java)
                        mActivity?.let { activity ->
                            activity.startActivity(intent)
                        }
                    } else {
                        mActivity?.let {
                            val intent = Intent(it, MainActivity::class.java)
                            startActivity(intent)
                            it.finish()
                        }
                    }
                } else {
                    if (mActivity is MainActivity) findNavController().popBackStack()
                    else mActivity?.finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //dialog view is ready
    @SuppressLint("StringFormatMatches")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated-DialogFragments", "InAppPanel")
        mActivity?.let {
            sharedPreferences = it.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            // Register this fragment as a listener for SharedPreferences changes
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            viewModel.inAppPurchases.getInAppPrefs(it)
            viewModel.inAppPurchases.getSubscriptionPref(it)
            isEverSubscribe = viewModel.inAppPurchases.wasEverSubscribed(it)
            InAppPurchases.isAvailable.observe(viewLifecycleOwner) {
                if (it) {
                    if (!InAppPurchases.subPurchaseList.isNullOrEmpty()) {
                        handleClicks()
                    } else {
                        if (NetworkUtils.isOnline(requireActivity())) {
                            lifecycleScope.launch {
                                binding?.tvPriceInfo?.text = "Loading..."
                                delay(3000)
                                prepareNavigation()
                                mActivity?.let {
                                    viewModel.inAppPurchases.getInAppPrefs(it)
                                    viewModel.inAppPurchases.getSubscriptionPref(it)
                                }
                            }
                        } else {
                            binding?.tvPriceInfo?.text =
                                getString(R.string.no_internet_connection)
                        }
                    }
                } else {
                    if (NetworkUtils.isOnline(requireActivity())) {
                        lifecycleScope.launch {
                            binding?.tvPriceInfo?.text = "Loading..."
                            prepareNavigation()
                            mActivity?.let {
                                viewModel.inAppPurchases.getSubscriptionPref(it)
                                viewModel.inAppPurchases.getInAppPrefs(it)
                            }
                        }
                    } else {
                        binding?.tvPriceInfo?.text = getString(R.string.no_internet_connection)
                    }
                }
            }
            mActivity?.let {
                viewModel.inAppPurchases.getInAppPrefs(it)
                viewModel.inAppPurchases.getSubscriptionPref(it)
            }
            ///////////////
            binding?.closeButton?.setOnClickListener {
                AppUtils.firebaseUserAction("pro_close_click", "inappscreen")
                try {
                    // Reset flag when closing pro panel, with a delay to ensure normal behavior resumes
                    lifecycleScope.launch {
                        delay(3000) // Wait 3 seconds after closing pro panel before allowing app open ads
                        AdBlockerHelper.setinterstitialshown(false)
                    }
                    if (isFromSplash) {

                        mActivity?.let {
                            val intent = Intent(it, MainActivity::class.java)
                            startActivity(intent)
                            it.finish()
                        }
                    } else {
                        if (mActivity is MainActivity) findNavController().popBackStack()
                        else mActivity?.finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            if (!InAppPurchases.subPurchaseList.isNullOrEmpty()) {
                handleClicks()
            } else {
                if (NetworkUtils.isOnline(requireActivity())) {
                    binding?.tvPriceInfo?.text = "Loading..."
                } else {
                    binding?.tvPriceInfo?.text = getString(R.string.no_internet_connection)
                }
            }
        }
        AdBlockerHelper.isProVersion.observe(viewLifecycleOwner) {
            it?.let {
                if (it) {
                    lifecycleScope.launch {
                        delay(1000)
                        try {
                            if (isFromSplash) {
                                mActivity?.let {
                                    val intent = Intent(it, MainActivity::class.java)
                                    startActivity(intent)
                                    it.finish()
                                }
                            } else {
//                        if (!isEverSubscribe){
//                            mActivity?.let { activity->
//                                showOfferDialog(activity)
//                            }
//                        }else{
                                if (mActivity is MainActivity) findNavController().popBackStack()
                                else mActivity?.finish()
//                        }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        animatRightArrow()
    }
    private fun animatRightArrow(){
        val pulseAnim = AnimationUtils.loadAnimation(mActivity, R.anim.button_pulse)
        binding?.ivRightArrow?.startAnimation(pulseAnim)
    }

    private fun handleClicks() {
        binding?.privacy?.setOnClickListener {
            AppUtils.firebaseUserAction("privacyBtnClicked_DialogFragments", "InAppPanel")
            try {
                // Prevent app open ad when returning from Privacy Policy
                AdBlockerHelper.setinterstitialshown(true)
                mActivity?.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.xilliapps.com/privacy-policy.html")
                    )
                )
            } catch (e: Exception) {
                mActivity?.let { it1 -> ToastUtils.showErrorToast(it1) }
            }
        }
        binding?.term?.setOnClickListener {
            AppUtils.firebaseUserAction("termBtnClicked_DialogFragments", "InAppPanel")
            try {
                // Prevent app open ad when returning from Terms of Use
                AdBlockerHelper.setinterstitialshown(true)
                mActivity?.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.terafort.com/T&C.html")
                    )
                )
            } catch (e: Exception) {
                mActivity?.let { it1 -> ToastUtils.showErrorToast(it1) }
            }
        }
        try {
            if (!InAppPurchases.subPurchaseList.isNullOrEmpty() && NetworkUtils.isOnline(
                    requireContext()
                )
            ) {
                selectRadio1()
                InAppPurchases.subPurchaseList?.let {
                    if (!InAppPurchases.durationList.isNullOrEmpty()) {

                    }
                    for (i in it) {
                        if (i.name.contains("W")) {
                            freeTrailc = i.freeTrial
                            if (freeTrailc.isNotEmpty()) {
                                trialDays = getFreeTrialDays(freeTrailc)
                                trialString = "after Free trial of ${trialDays} days"
                            }
                            monthlyPrice = i.price
                            Log.d("FirestObserve", "${i.price}")
                            val priceString = i.price
                            val parts = priceString.split("\\s+".toRegex())
                            val currency = parts[0] // "Rs"
                            val amount = parts[1].replace(",", "").toDouble() // 200.00
                            InAppPurchases.currencyCode = currency
                            InAppPurchases.price = amount
                            viewModel.isWeeklyPurchasedAlready.observe(this) { v->
                                Log.d("FirestObserve", "$v")
                                if (v == true) {
                                    binding?.tvPriceInfo?.text = "$monthlyPrice/Weekly"
                                    binding?.btnContinue?.text = "Subscribe Now"
                                } else {
                                    binding?.tvPriceInfo?.text = "$priceString/Weekly after FREE trial of 03 days"
                                    binding?.btnContinue?.text = "Start Free Trial"
                                }
                            }
                        }
                    }
                }
            } else {
                nullvalue = true
            }
            if (!InAppPurchases.inAppPrice.isNullOrEmpty() && NetworkUtils.isOnline(requireContext())) {
            } else {
                nullvalue = true
            }
        } catch (e: java.lang.Exception) {
            nullvalue = true
        }

        try {
            binding?.btnContinue?.setOnClickListener {
                binding?.btnContinue?.isEnabled = false
                binding?.btnContinue?.postDelayed({
                    binding?.btnContinue?.isEnabled = true
                }, 2000)
                AppUtils.firebaseUserAction("subscription_clicked_splash", "InAppPanel")
                try {
                    if (sku == null) {
                        mActivity?.let { it1 ->
                            ToastUtils.showToast(
                                it1,
                                "Please select one of above options"
                            )
                        }
                    } else {
                        if (NetworkUtils.isOnline(requireContext())) {
                            if (sku == SKU_WEEKLY) {
                                viewModel.inAppPurchases.requestSubBilling(
                                    activity,
                                    SKU_WEEKLY
                                )
                            }
                        } else {
                            ToastUtils.showToast(
                                requireContext(),
                                getString(R.string.internet_issue_values_are_not_fetched)
                            )
                        }
                    }
                } catch (e: Exception) {
                    ToastUtils.showToast(requireContext(), "Please come back later")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun selectRadio1() {
        viewModel.inAppPurchases.checkHistory { purchasesList ->
            Log.d("Purchase history", "${purchasesList}")
            if (purchasesList.isNotEmpty()) {
                for (i in purchasesList) {
                    Log.d("Purchase history", "${i}")
                    if (i.products.toString() == "[weekly_trail_free]") {
                        viewModel.isWeeklyPurchasedAlready.postValue(true)
                        Log.d("Purchase history", "1:${i}")
                        break
                    } else {
                        viewModel.isWeeklyPurchasedAlready.postValue(false)
                        Log.d("Purchase history", "2:${i}")
                    }
                }
            } else {
                viewModel.isWeeklyPurchasedAlready.postValue(false)
                Log.d("Purchase history", "23")
            }
        }
        sku = SKU_WEEKLY
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onResume() {
        super.onResume()
        // Prevent app open ad when pro panel is visible
        AdBlockerHelper.setinterstitialshown(true)
    }

    override fun onPause() {
        super.onPause()
        // Reset flag when pro panel is paused, with a delay to ensure normal behavior resumes
        lifecycleScope.launch {
            delay(3000) // Wait 3 seconds after pro panel is paused before allowing app open ads
            AdBlockerHelper.setinterstitialshown(false)
        }
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
        nullvalue = false
        // Reset flag when fragment is detached to ensure normal behavior resumes
        lifecycleScope.launch {
            delay(3000) // Wait 3 seconds after detaching before allowing app open ads
            AdBlockerHelper.setinterstitialshown(false)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "price_list") {
            // Update the list of strings and notify the adapter of the changes
            if (!InAppPurchases.subPurchaseList.isNullOrEmpty()) {
//                handleClicks()
            } else {
                if (NetworkUtils.isOnline(requireActivity())) {
                    binding?.tvPriceInfo?.text = "Loading..."
                } else {
                    binding?.tvPriceInfo?.text = getString(R.string.no_internet_connection)
                }
            }
        }
    }


    fun getFreeTrialDays(freeTrialPeriod: String): Int {
        val regex = Regex("P(\\d+)D")
        val matchResult = regex.find(freeTrialPeriod)
        return matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }


}