package com.video.avd.ui.splash_flow.activities

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.proCrossTimer
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.databinding.ActivityInAppBinding
import com.video.avd.extension.shake
import com.video.avd.ui.splash_flow.utils.AppUtils.hideNavigationBar
import com.video.avd.ui.splash_flow.utils.AppUtils.shouldAllObDisable
import com.video.avd.ui.splash_flow.utils.AppUtils.shouldNavigateToLanguage
import com.video.avd.ui.MainActivity
import com.video.avd.ui.inapp.ProViewModel
import com.video.avd.ui.inapp.new_.inapppurchases.BillingClientConnectionListener
import com.video.avd.ui.inapp.new_.inapppurchases.DataWrappers
import com.video.avd.ui.inapp.new_.inapppurchases.IapConnector
import com.video.avd.ui.inapp.new_.inapppurchases.IapManager
import com.video.avd.ui.inapp.new_.inapppurchases.SubscriptionServiceListener
import com.video.avd.ui.onbooard.OnboardingActivity
import com.video.avd.ui.onbooard.SurveyActivity
import com.video.avd.utils.AppUtils
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.GlobalValues.fromSplash
import com.video.avd.utils.InAppPurchases
import com.video.avd.utils.InAppPurchases.Companion.SKU_WEEKLY
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InAppActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    var binding: ActivityInAppBinding? = null

    private val viewModel: ProViewModel by viewModels()
    private lateinit var iapConnector: IapConnector
    val isBillingClientConnected: MutableLiveData<Boolean> = MutableLiveData()
    private var nullvalue: Boolean? = false
    private lateinit var sharedPreferences: SharedPreferences
    var sku: String = SKU_WEEKLY
    private var trialDays = 0
    private var monthlyPrice = ""
    private var freeTrailc = ""
    private var trialString = ""
    var isEverSubscribe = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInAppBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        startShineSweep(binding?.shineView!!)
        AppUtils.changeStatusBarColor(R.color.black, this@InAppActivity, true)
        AppUtils.fbEvents("pro_pannel_view", "ProPanel",this)
        try {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if(fromSplash){
            lifecycleScope.launch(Dispatchers.Main){
                delay(500)
                binding?.adLayout?.visibility=View.GONE
            }
        }
        else{
            binding?.adLayout?.visibility=View.GONE
        }
        isBillingClientConnected.value = false
        if (!NetworkUtils.isOnline(this)) {
            binding?.textFetchingPrices?.text = "No Network Connection, Please Try Again"
        }
        iapConnector = IapManager.getIapConnector(this)
        initInAppListeners()
        AdBlockerHelper.isProVersion.observe(this) {
            it?.let {
                if (it) {
                    lifecycleScope.launch {

                        AdsManager.rewardedAd=null
                        AdsManager.nativeAd=null
                        AdsManager.nativeAdNow=null
                        AdsManager.nativeAdLarge=null
                        AdsManager.nativeAFrameLayout=null
                        finish()
                    }
                }
            }
        }
        sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        // Register this fragment as a listener for SharedPreferences changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        viewModel.inAppPurchases.getInAppPrefs(this)
        viewModel.inAppPurchases.getSubscriptionPref(this)
        isEverSubscribe = viewModel.inAppPurchases.wasEverSubscribed(this)
        InAppPurchases.isAvailable.observe(this) {
            if (it) {
                if (!InAppPurchases.subPurchaseList.isNullOrEmpty()) {
                    handleClicks()
                } else {
                    if (NetworkUtils.isOnline(this)) {
                        lifecycleScope.launch {
                            binding?.tvPriceInfo?.text = "Loading..."
                            delay(3000)
                            prepareNavigation()
                            viewModel.inAppPurchases.getInAppPrefs(this@InAppActivity)
                            viewModel.inAppPurchases.getSubscriptionPref(this@InAppActivity)

                        }
                    } else {
                        binding?.tvPriceInfo?.text = getString(R.string.no_internet_connection)
                    }
                }
            } else {
                if (NetworkUtils.isOnline(this)) {
                    lifecycleScope.launch {
                        binding?.tvPriceInfo?.text = "Loading..."
                        prepareNavigation()

                        viewModel.inAppPurchases.getSubscriptionPref(this@InAppActivity)
                        viewModel.inAppPurchases.getInAppPrefs(this@InAppActivity)

                    }
                } else {
                    binding?.tvPriceInfo?.text = getString(R.string.no_internet_connection)
                }
            }
        }

        viewModel.inAppPurchases.getInAppPrefs(this)
        viewModel.inAppPurchases.getSubscriptionPref(this)


        binding?.closeButton?.setOnClickListener {
            navToNext()
        }



        if (!InAppPurchases.subPurchaseList.isNullOrEmpty()) {
            handleClicks()
        } else {
            if (NetworkUtils.isOnline(this)) {
                binding?.tvPriceInfo?.text = "Loading..."
            } else {
                binding?.tvPriceInfo?.text = getString(R.string.no_internet_connection)
            }
        }

        AdBlockerHelper.isProVersion.observe(this) {
            it?.let {
                if (it) {
                    lifecycleScope.launch {
                        delay(1000)
                        startActivity(Intent(this@InAppActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }

        animatRightArrow()
    }
    private fun initInAppListeners() {
        iapConnector.addBillingClientConnectionListener(object : BillingClientConnectionListener {
            override fun onConnected(status: Boolean, billingResponseCode: Int) {
                isBillingClientConnected.value = status
            }
        })
        iapConnector.addSubscriptionListener(object : SubscriptionServiceListener {
            override fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                // will be triggered upon fetching owned subscription upon initialization
                when (purchaseInfo.sku) {
                    IapManager.skuKeyWeekly -> {
                        AdBlockerHelper.isProVersion.postValue(true)
                        AdsManager.rewardedAd = null
                        purchaseSubscriptionPref(this@InAppActivity)
                    }
                    else -> {
                        AdBlockerHelper.isProVersion.value = false
                    }
                }
            }

            override fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                // will be triggered whenever subscription succeeded
                when (purchaseInfo.sku) {
                    IapManager.skuKeyWeekly -> {
                        AdBlockerHelper.isProVersion.postValue(true)
                        AdsManager.rewardedAd = null
                        ToastUtils.showToast(
                            this@InAppActivity,
                            "You've successfully subscribed " + resources.getString(R.string.app_name) + "weekly Pro"
                        )
                        purchaseSubscriptionPref(this@InAppActivity)
                        if(!fromSplash) {
                            finish()
                        }
                        else{
                            startActivity(Intent(this@InAppActivity, MainActivity::class.java))
                        }
                        AppUtils.fbEvents("trial_sucessfull", "ProPanel",this@InAppActivity)
                    }

                    else -> {
                        AdBlockerHelper.isProVersion.value = false
                    }
                }

            }

            override fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>) {
                // list of available products will be received here, so you can update UI with prices if needed
                for (product in iapKeyPrices){
                            for (productDetail in product.value) {
                                if (productDetail!=null)
                                {
                                    if (product.value[0].billingPeriod!="P3D") {
                                        binding?.noPayments?.visibility=View.GONE
                                        binding?.tvPriceInfo?.text = "${productDetail.price.toString()}/Weekly"
                                        binding?.btnContinue?.text = "Subscribe Now"
                                    } else {
                                        binding?.noPayments?.visibility=View.VISIBLE
                                        binding?.tvPriceInfo?.text = "${productDetail.price.toString()}/Weekly after FREE trial of 03 days"
                                        binding?.btnContinue?.text = "Start Free Trial"
                                    }
                                }
                                else {
                                    binding?.textFetchingPrices?.text = "Unable to fetch plan details"
                                    binding?.textFetchingPrices?.visibility=View.VISIBLE
                                    binding?.btnContinue?.visibility= View.INVISIBLE
                                }
                            }
                }

            }
        })
    }
    fun navToNext() {
        AppUtils.fbEvents("pro_pannel_cross", "ProPanel",this)
        if(shouldNavigateToLanguage() && fromSplash){
            AppUtils.fbEvents("first_user_language", "Language",this)
        }
        Log.d("navToNext", "navToNext: ${shouldAllObDisable()}")
        val nextActivity = when {
            shouldNavigateToLanguage() && fromSplash -> LanguageActivity::class.java
            !shouldNavigateToLanguage() && fromSplash -> MainActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, nextActivity))
        finish()
    }
    private fun handleClicks() {
        binding?.privacy?.setOnClickListener {
            AppUtils.firebaseUserAction("privacyBtnClicked_DialogFragments", "InAppPanel")
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://tflsignatureapps.terafort.com/privacy-policy.html")
                    )
                )
            } catch (e: Exception) {
                ToastUtils.showErrorToast(this)
            }
        }
        binding?.term?.setOnClickListener {
            AppUtils.firebaseUserAction("termBtnClicked_DialogFragments", "InAppPanel")
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://aspire.pics/aspire-terms-of-use.html")
                    )
                )
            } catch (e: Exception) {
                ToastUtils.showErrorToast(this)
            }
        }
        try {
            if (!InAppPurchases.subPurchaseList.isNullOrEmpty() && NetworkUtils.isOnline(this)) {
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
                        }
                    }
                }
            } else {
                nullvalue = true
            }
            if (!InAppPurchases.inAppPrice.isNullOrEmpty() && NetworkUtils.isOnline(this)) {
            } else {
                nullvalue = true
            }
        } catch (e: java.lang.Exception) {
            nullvalue = true
        }

        try {
            binding?.ctaContainer?.let { container ->
                startShakeAnimation(container)
            }
            binding?.btnContinue?.setOnClickListener {
//                binding?.btnContinue?.isEnabled = false
                if(binding?.btnContinue?.text=="Start Free Trial"){
                    AppUtils.fbEvents("pro_pannel_trial_click", "ProPanel",this)
                }
                else{
                    AppUtils.fbEvents("pro_pannel_purchase_click", "ProPanel",this)
                }
                iapConnector.subscribe(this, IapManager.skuKeyWeekly)
  /*  binding?.btnContinue?.postDelayed({
                    binding?.btnContinue?.isEnabled = true
                }, 2000)
                AppUtils.firebaseUserAction("subscription_clicked_splash", "InAppPanel")
                try {
                    if (NetworkUtils.isOnline(this)) {
                        viewModel.inAppPurchases.requestSubBilling(
                            this,
                            SKU_WEEKLY)
                    } else {
                        ToastUtils.showToast(
                            this,
                            getString(R.string.internet_issue_values_are_not_fetched)
                        )
                    }
                } catch (e: Exception) {
                    ToastUtils.showToast(this, "Please come back later")
                }
*/
            }
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding?.btnContinue?.startAnimation(shake)
//            binding?.btnContinue?.post { binding?.btnContinue?.shake() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onPause() {
        super.onPause()
        binding?.btnContinue?.clearAnimation()
    }

    override fun onResume() {
        super.onResume()
        binding?.closeButton?.visibility = View.INVISIBLE
        binding?.btnContinue?.let { startShakeAnimation(it) }
        lifecycleScope.launchWhenStarted {
            onBackPressedDispatcher.addCallback(
                this@InAppActivity,
                onBackPressedCallback
            )
            try {
                if(proCrossTimer<1){
                    delay(2000)
                }
                else{
                    delay(proCrossTimer*1000L)
                }
            } catch (e: Exception) {
                delay(2000)
                e.printStackTrace()
            }
            binding?.closeButton?.visibility = View.VISIBLE
        }
    }
    private fun startShakeAnimation(view: View) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val animator =
                    ObjectAnimator.ofFloat(view, "translationX", 0f, 10f, -10f, 10f, -10f, 0f)
                animator.duration = 500
                animator.start()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)
    }
    private fun prepareNavigation() {
        lifecycleScope.launchWhenStarted {
            viewModel.inAppPurchases.getPrice(this@InAppActivity)

        }
    }

    private fun animatRightArrow(){
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.button_pulse)
        binding?.ivRightArrow?.startAnimation(pulseAnim)
    }
    private fun startShineSweep(shineView: View) {
        val shineAnim = AnimationUtils.loadAnimation(
            shineView.context,
            R.anim.shine_sweep
        )
        shineView.startAnimation(shineAnim)
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navToNext()
        }
    }

    private fun selectRadio1() {
        viewModel.inAppPurchases.checkHistory { purchasesList ->
            if (purchasesList.isNotEmpty()) {
                Log.d("History_data", "onBillingSetupFinished:1 $purchasesList")
                for (i in purchasesList) {
                    Log.d("Purchase history", "${i.purchaseToken}")
                    if (i.products.toString() == "[weekly_pro]") {
                        viewModel.isWeeklyPurchasedAlready.postValue(true)
                        Log.d("History_data", "onBillingSetupFinished:2 $purchasesList")
                        break
                    } else {
                        viewModel.isWeeklyPurchasedAlready.postValue(false)
                        Log.d("History_data", "onBillingSetupFinished:3 $purchasesList")
                    }
                }
            } else {
                viewModel.isWeeklyPurchasedAlready.postValue(false)
                Log.d("History_data", "onBillingSetupFinished:4 $purchasesList")
            }
        }
        sku = SKU_WEEKLY
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "price_list") {
            // Update the list of strings and notify the adapter of the changes
            if (!InAppPurchases.subPurchaseList.isNullOrEmpty()) {
//                handleClicks()
            } else {
                if (NetworkUtils.isOnline(this)) {
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
    fun purchaseSubscriptionPref(context: Context) {
        val sharedPref = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isSubscribed", true)
        editor.apply()
        Log.e("InAppCheck","truepurchase")
        AdBlockerHelper.isProVersion.postValue(true)
        GlobalValues.is24hourEnabled.postValue(true)
    }
}
