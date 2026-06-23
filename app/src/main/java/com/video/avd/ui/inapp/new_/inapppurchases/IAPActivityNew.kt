package com.video.avd.ui.inapp.new_.inapppurchases

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.avd.util.AdBlockerHelper
import com.video.avd.R
import com.video.avd.ads.AdsManager
import com.video.avd.databinding.ActivityIapNewBinding
import com.video.avd.extension.openLink
import com.video.avd.ui.MainActivity
import com.video.avd.utils.AppPreference.isFromSplash
import com.video.avd.utils.AppUtils.transparentStausBar
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@AndroidEntryPoint
class IAPActivityNew : AppCompatActivity(), SubscriptionPlanCallback {
    private lateinit var iapConnector: IapConnector
    private lateinit var binding: ActivityIapNewBinding
    val isBillingClientConnected: MutableLiveData<Boolean> = MutableLiveData()
    private var adapter: AdapterSubscriptionPlans? = null
    private var currentPlan = "weekly"
    var isShowad=false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transparentStausBar(this)
        try {
            binding = ActivityIapNewBinding.inflate(layoutInflater)
            setContentView(binding.root)
            isShowad=intent.getBooleanExtra("isSplash",false)
            onClickListeners()
            unSelectContinue()
            setUpPlansRecycler()
            isBillingClientConnected.value = false
            if (!NetworkUtils.isOnline(this)) {
                binding.textFetchingPrices.text = "No Network Connection, Please Try Again"
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
        } catch (e: Exception) {
            finish()
        }
    }

//    private fun initImageSlider() {
//        val imageList = ArrayList<SlideModel>()
//        imageList.add(SlideModel(R.drawable.pro_guy2))
//        imageList.add(SlideModel(R.drawable.pro_guy2))
//        imageList.add(SlideModel(R.drawable.pro_guy2))
//        binding.imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)
//    }

    private fun setUpPlansRecycler() {
        adapter = AdapterSubscriptionPlans(this@IAPActivityNew, this)
        binding.rvPlans.adapter = adapter
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
                        AdBlockerHelper.isProVersion.value = true
                        AdsManager.rewardedAd = null
                        purchaseSubscriptionPref(this@IAPActivityNew)
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
                        AdBlockerHelper.isProVersion.value = true
                        AdsManager.rewardedAd = null
                        ToastUtils.showToast(
                            this@IAPActivityNew,
                            "You've successfully subscribed " + resources.getString(R.string.app_name) + "weekly Pro"
                        )
                        purchaseSubscriptionPref(this@IAPActivityNew)
                        finish()
                    }

                    else -> {
                        AdBlockerHelper.isProVersion.value = false
                    }
                }

            }

            override fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>) {
                // list of available products will be received here, so you can update UI with prices if needed
            }
        })
    }

    private fun selectContinue() {
        binding.btnContinue.isEnabled = true
        binding.btnContinue.setOnClickListener {
            when(currentPlan){
                "weekly" -> {
                    iapConnector.subscribe(this, IapManager.skuKeyWeekly)
                }
                "lifetime" -> {
                    iapConnector.purchase(this, IapManager.skuKeyLifetime)
                }
                }

        }
    }

    private fun unSelectContinue() {
//        binding.btnContinue.backgroundTintList = ColorStateList.valueOf(
//                ContextCompat.getColor(
//                        this,
//                        R.color.colorIAPLight
//                )
//        )
        binding.btnContinue.isEnabled = false
    }

    private fun onClickListeners() {
        binding.ivClose.setOnClickListener {
            if (isFromSplash){
                if (isShowad){
                    val intent=Intent(this,MainActivity::class.java)
                    startActivity(intent)

                }else{
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
           else {
                finish()
           }
        }

        binding.privacy.setOnClickListener {
            this.openLink("https://tflsignatureapps.terafort.com/privacy-policy.html")
        }

        binding.textCancelAnytime.setOnClickListener {
            try {
                when (currentPlan) {
                    "weekly" -> {
                        iapConnector.unsubscribe(this, IapManager.skuKeyWeekly)
                    }
                }
            } catch (e: Exception) {

            }
        }

        binding.terms.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.terafort.com/T&C.html")
                    )
                )
            } catch (e: Exception) {

            }
        }
    }

    private fun subscribeUi(list: List<DataWrappers.ProductDetails>) {
        if (list.isEmpty()) {
            binding.textFetchingPrices.text = "Unable to fetch plan details"
            binding.textFetchingPrices.visibility= View.VISIBLE
            binding.rvPlans.visibility= View.GONE
            binding.btnContinue.visibility= View.GONE
        } else {
//            binding.textFetchingPrices.visibility= View.GONE
//            binding.rvPlans.visibility= View.VISIBLE
//            binding.btnContinue.visibility= View.VISIBLE

            val tempPriceList = mutableListOf<CustomInAppModel>()


            if (list.isNotEmpty()) {
                tempPriceList.add(
                    CustomInAppModel(
                        0,
                        "weekly",
                        "",
                        list[0].price.toString(),
                        list[0].price.toString(),
                        "",
                        false
                    )
                )
            }


            if (list.size > 1) {
                tempPriceList.add(
                    1,
                    calculateYearlyPercentage(
                        list[0].priceAmount!!,
                        list[1].priceAmount!!,
                        list[1].price!!
                    )
                )
            }

            if (list.size > 2) {
                tempPriceList.add(
                    CustomInAppModel(
                        2,
                        "lifetime",
                        "",
                        list[2].price.toString(),
                        list[2].price.toString(),
                        "",
                        false, showHotOffer = true
                    )
                )
            }

            adapter?.submitList(tempPriceList)
            if (tempPriceList.size>0){
                binding.textFetchingPrices.visibility= View.GONE
                binding.rvPlans.visibility= View.VISIBLE
                binding.btnContinue.visibility= View.VISIBLE
            }else{
                binding.textFetchingPrices.text = "Unable to fetch plan details"
                binding.textFetchingPrices.visibility= View.VISIBLE
                binding.rvPlans.visibility= View.GONE
                binding.btnContinue.visibility= View.GONE
            }
        }
    }


    fun calculateMonthlyPercentage(
        weeklyPrice: Double,
        monthlyPrice: Double,
        consolePrice: String
    ): CustomInAppModel {
        // Calculate monthly discount percentage and discounted price based on weekly price
        val monthlyDiscountPercentage = ((weeklyPrice * 4 - monthlyPrice) / (weeklyPrice * 4)) * 100

        // Calculate monthly and yearly total prices without any discount
        val monthlyTotalWithoutDiscount = weeklyPrice * 4

        return CustomInAppModel(
            1,
            "Monthly",
            "" + getPriceUnit(consolePrice)?.first + "$monthlyTotalWithoutDiscount /month",
            monthlyTotalWithoutDiscount.toString(),
            consolePrice,
            monthlyDiscountPercentage.roundToInt().toString() + "% off",
            true
        )
    }

    fun calculateYearlyPercentage(
        weeklyPrice: Double,
        yearlyPrice: Double,
        consolePrice: String
    ): CustomInAppModel {
        // Calculate monthly discount percentage and discounted price based on weekly price
        // Calculate yearly discount percentage and discounted price based on weekly price
        val yearlyDiscountPercentage = ((weeklyPrice * 52 - yearlyPrice) / (weeklyPrice * 52)) * 100

        // Calculate monthly and yearly total prices without any discount
        val yearlyTotalWithoutDiscount = weeklyPrice * 52

        return CustomInAppModel(
            1,
            "yearly",
            "" + getPriceUnit(consolePrice)?.first + " $yearlyTotalWithoutDiscount /year",
            yearlyTotalWithoutDiscount.toString(),
            consolePrice,
            yearlyDiscountPercentage.roundToInt().toString() + "% off",
            true
        )
    }

    fun calculateLifetimePercentage(
        weeklyPrice: Double,
        lifetimePrice: Double,
        consolePrice: String
    ): CustomInAppModel {

        val yearlyDiscountPercentage = ((weeklyPrice * 260 - lifetimePrice) / (weeklyPrice * 260)) * 100
        val yearlyTotalWithoutDiscount = weeklyPrice * 260
  Log.d("ddddddd", yearlyTotalWithoutDiscount.toString())
        return CustomInAppModel(
            2,
            "lifetime",
            "" + getPriceUnit(consolePrice)?.first + " $yearlyTotalWithoutDiscount",
            yearlyTotalWithoutDiscount.toString(),
            consolePrice,
            yearlyDiscountPercentage.roundToInt().toString() + "% off",
            true
        )
    }


    fun getPriceUnit(input: String): Pair<String, String>? {
        // Check if the input string contains a digit
        val digitIndex = input.indexOfFirst { it.isDigit() }

        return if (digitIndex != -1) {
            // Extracting the unit and price based on the digit index
            val unit = input.substring(0, digitIndex)
            val price = input.substring(digitIndex)

            Pair(unit, price)
        } else {
            // If no digit is found, return null
            null
        }
    }

    override fun onPlanClick(position: Int, customInAppModel: CustomInAppModel) {
        adapter?.selectPlan(position)
        binding.btnContinue.setOnClickListener {
            currentPlan = when (customInAppModel.durationPlan) {
                "weekly" -> {
                    iapConnector.subscribe(this, IapManager.skuKeyWeekly)
                    "weekly"
                }

                "lifetime" -> {
                    iapConnector.subscribe(this, IapManager.skuKeyLifetime)
                    "lifetime"
                }


                else -> {
                    iapConnector.subscribe(this, IapManager.skuKeyWeekly)
                    "weekly"
                }
            }
        }
    }




    override fun onBackPressed() {
        if (isFromSplash){
            if (isShowad){
                val intent=Intent(this,MainActivity::class.java)
                startActivity(intent)
            }else{
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        else {
            finish()
        }
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