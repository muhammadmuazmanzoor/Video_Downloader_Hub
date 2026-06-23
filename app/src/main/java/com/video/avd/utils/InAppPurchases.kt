package com.video.avd.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.avd.util.AdBlockerHelper.isProVersion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.singular.sdk.Singular
import com.video.avd.constent.isDataInitialized
import com.video.avd.ui.inapp.model.InAppPricess
import com.video.avd.ui.inapp.new_.inapppurchases.IapManager.skuKeyWeekly
import com.video.avd.utils.GlobalValues.is24hourEnabled
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import javax.inject.Singleton


@Singleton
class InAppPurchases constructor(private val context: Context) : PurchasesUpdatedListener, AcknowledgePurchaseResponseListener, BillingClientStateListener {

    var listsku = listOf(skuKeyWeekly)
//    , SKU_ITEM_OFFER_VRSN

    companion object {
        /** Change Keys as per app requirement */
        var SKU_WEEKLY = "weekly_pro"
        var subPurchaseList: List<InAppPricess>? = null
        var isAvailable = MutableLiveData<Boolean>()
        var inAppPrice: String? = null
        var durationList: List<String>? = null
        var billingClient: BillingClient? = null
        var isSubscribed = MutableLiveData<Boolean>()
        var currencyCode=""
        var price=0.0
    }

    /** Change License Key as per app requirement */

    var LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9BEKUw5WYg2a6g+20+Wj8W/xQu1e0d3K3G9yvQx1qmN6s24zC+m+btDgIWuRSEYjEw8Mkw1gucLkZfO3DXcjWwjFKD3nVwXDJ2O2eW1BLXDDGJaFLKP7C/Dk21nekPmzh0GRoKaRQvDGS/lbjNFA08f7LTVt4UnJhzN1yMFO+fQW07FJ/6wVKS9tiPkxskGQMbkbEME0P5F7muyKsr4xndvmvWF5V4hpUQfy/gUl7FPe2HdUvuOgX0qdM8yK0crudMK/my4iU81Q1HCnSxIKXQF8wrI7RQyn/qJP0ZL2ns+QgHUXLF9sDwLyA2iQhnhPodge2PavA2LjVnEP24qM0wIDAQAB"

    //Google Billing Initialization
    fun setBillingClient() {
        try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                )
                .build()
            billingClient?.startConnection(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun checkHistory(result: (purchasesList: List<Purchase>) -> Unit) {
        val billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val allPurchases = mutableListOf<Purchase>()

                        listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS)
                            .forEach { productType ->
                                val params = QueryPurchasesParams.newBuilder()
                                    .setProductType(productType)
                                    .build()
                                // queryPurchasesAsync is the suspend function in 8.0.0
                                billingClient.queryPurchasesAsync(params
                                ) { purchasesResult, purchasesList ->
                                    Log.d(
                                        "History_data",
                                        "$productType purchases: ${purchasesList}"
                                    )

                                    if (purchasesResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                        allPurchases.addAll(purchasesList)
                                    }
                                }


                            }

                        withContext(Dispatchers.Main) {
                            result(allPurchases)
                            billingClient.endConnection()
                        }
                    }
                } else {
                    result(emptyList())
                    billingClient.endConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                result(emptyList())
            }
        })
    }


    ///Google Billing
    fun handlePurchase(purchase: Purchase) {
        try {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val listener = ConsumeResponseListener { billingResult, s ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                }
            }
            billingClient?.consumeAsync(consumeParams, listener)
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                purchaseSubscriptionPref(context)
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams, this)
                    purchaseSubscriptionPref(context)
                    isProVersion.value = true
                    is24hourEnabled.value = true
                    Log.e("InAppCheck","true142")
                    ToastUtils.showToast(context, "Subscribed")
                } else {
                    ToastUtils.showToast(context, "Already Subscribed")
                }
                if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                    return
                }
            } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                ToastUtils.showToast(context, "Subscription Pending")
            } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                ToastUtils.showToast(context, "UnSpecified State")
            }
        } catch (e: Exception) {
            ToastUtils.showToast(context, "Billing Error")
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            val security = Security()
            security.verifyPurchase(LICENSE_KEY, signedData, signature)
        } catch (e: java.lang.Exception) {
            false
        }
    }


    fun requestSubBilling(activity: Activity?, SKU_ITEM: String) {
        try {
            if (NetworkUtils.isOnline(context)) {
                billingClient?.startConnection(object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                    }

                    override fun onBillingSetupFinished(billingresult: BillingResult) {
                        val productlist = listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(SKU_ITEM)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                        val params = QueryProductDetailsParams.newBuilder()
                            .setProductList(productlist)
                        billingClient?.queryProductDetailsAsync(params.build()) { billingresult, productDetailsList ->
                            for (productDetails in productDetailsList.productDetailsList) {
                                val offerToken =
                                    productDetails.subscriptionOfferDetails?.get(0)?.offerToken
                                val productDetailsParamsList = listOf(
                                    offerToken?.let {
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(it)
                                            .build()
                                    }
                                )
                                val billingFlowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productDetailsParamsList)
                                    .build()
                                billingClient?.launchBillingFlow(
                                    activity!!,
                                    billingFlowParams
                                )
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            ToastUtils.showToast(context, "Billing Error")
        }
    }

    private var isPurchaseProcessed = false

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            AppUtils.firebaseUserAction("INAPPPURUPDATE", "${billingResult.responseCode}")
            for (purchase in purchases) {
                if ( purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    AppUtils.firebaseUserAction("INAPPPURUPDATE", "${purchase.purchaseState}")
                    handlePurchase(purchase)
                    if (!isPurchaseProcessed){
                        isPurchaseProcessed = true
                        Singular.revenue(currencyCode, price, purchase)
                    }
                } else {
                    //
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            context?.let { purchaseSubscriptionPref(it) }
            ToastUtils.showToast(context, "Already Subscribed")
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            ToastUtils.showToast(context, "Not Supported")
        } else {
//                ToastUtils.showToast(context,"Error "+billingResult.debugMessage)
        }
    }


    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            ToastUtils.showToast(context, "Already Subscribed")
            isProVersion.value = true
            is24hourEnabled.value = true
            Log.e("CheckRefund","${billingResult.responseCode}")
            AppUtils.firebaseUserAction("INAPPAcknowledge", "${billingResult.responseCode}")
        }
        if (billingResult.responseCode == BillingClient.BillingResponseCode.ERROR) {
            ToastUtils.showToast(context, "Billing Error")
            AppUtils.firebaseUserAction("INAPPAcknowledge", "${billingResult.responseCode}")
        }
        if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
//            context?.let { cancelSubscriptionPref(it) }
            AppUtils.firebaseUserAction("INAPPAcknowledge", "${billingResult.responseCode}")
        }
        if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_NOT_OWNED) {
//            context?.let { cancelSubscriptionPref(it) }
            AppUtils.firebaseUserAction("INAPPAcknowledge", "${billingResult.responseCode}")
        }
    }

    override fun onBillingServiceDisconnected() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(1000)
                billingClient?.startConnection(this@InAppPurchases)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }


    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // Billing client is ready
        }
    }

    /** Get Price */
    @SuppressLint("SetTextI18n")
    fun getPrice(context: Context) {
        val prices = mutableListOf<InAppPricess>()
        val durations = mutableListOf<String>()
        val pricesLock = Any()
        val durationsLock = Any()

        synchronized(pricesLock) { prices.clear() }
        synchronized(durationsLock) { durations.clear() }

        val productList = listsku.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.productDetailsList.isNotEmpty()
            ) {
                for (productDetails in productDetailsList.productDetailsList) {
                    val productId = productDetails.productId
                    val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
                    val pricingPhases = offerDetails?.pricingPhases?.pricingPhaseList

                    val regularPricePhase = pricingPhases?.lastOrNull()
                    val trialPhase = pricingPhases?.firstOrNull {
                        it.priceAmountMicros == 0L // free trial usually has price = 0
                    }

                    val priceCurrency = regularPricePhase?.formattedPrice ?: ""
                    val billingPeriod = regularPricePhase?.billingPeriod ?: ""
                    val freeTrialPeriod =
                        trialPhase?.billingPeriod ?: "" // ✅ this replaces old freeTrialPeriod

                    Log.e("checkSku", "Product ID: $productId")
                    Log.e("checkSku", "Price: $priceCurrency")
                    Log.e("checkSku", "Free Trial Period: $freeTrialPeriod")

                    synchronized(pricesLock) {
                        prices.add(
                            InAppPricess(
                                name = billingPeriod,
                                price = priceCurrency,
                                key = productId,
                                freeTrial = freeTrialPeriod
                            )
                        )
                    }

                    saveList(context, prices)
                    saveDurationPrefs(context, durations)
                }
                isAvailable.postValue(true)
            } else {
                isAvailable.postValue(false)
            }
        }
    }


    //Check if Subscribed or Not
    /** Check if Subscribed or Not **/
    suspend fun checkSubscription() : Boolean = withContext(Dispatchers.IO){
        val deferred = CompletableDeferred<Boolean>()
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .setListener { billingResult: BillingResult?, list: List<Purchase?>? -> }
            .build()
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                deferred.complete(false)
            }
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClient?.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS).build()
                    ) { billingResult1: BillingResult, list: List<Purchase> ->
                        if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (list.isNotEmpty()) {
                                purchaseSubscriptionPref(context)
                                isSubscribed.postValue(true)
                                isDataInitialized.postValue(true)
                                isProVersion.postValue(true)
                                deferred.complete(true)
                                for ((i, purchase) in list.withIndex()) {
                                    //Here you can manage each product, if you have multiple subscription
                                }
                            } else {
                                isProVersion.postValue(false)
                                deferred.complete(false)
                                isSubscribed.postValue(false)
                                isDataInitialized.postValue(true)
                                cancelSubscriptionPref(context)
                            }
                        }
                    }
                }
            }

        })
        deferred.await()
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun hasEverSubscribed(context: Context): Boolean = suspendCancellableCoroutine { cont ->
        val billing = BillingClient.newBuilder(context)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .setListener { _, _ -> /* do nothing here */ }
            .build()

        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.tryResume(false)?.let { cont.completeResume(it) }
                    return
                }

                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                billing.queryPurchasesAsync(params) { br, history ->
                    cont.tryResume(
                        br.responseCode == BillingClient.BillingResponseCode.OK && !history.isNullOrEmpty()
                    )?.let { cont.completeResume(it) }
                    billing.endConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                cont.tryResume(false)?.let { cont.completeResume(it) }
            }
        })
    }

    /** Save and Retrieve Prefs **/

    fun cancelSubscriptionPref(context: Context) {
        val sharedPref = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isSubscribed", false)
        editor.apply()
        Log.e("InAppCheck","falsecancel")
        isProVersion.postValue(false)
    }

    fun purchaseSubscriptionPref(context: Context) {
        val sharedPref = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isSubscribed", true)
        editor.apply()
        Log.e("InAppCheck","truepurchase")
        isProVersion.postValue(true)
        is24hourEnabled.postValue(true)
    }

    fun getSubscriptionPref(activity: Context) {
        val sharedPref = activity.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        Log.e("InAppCheck","falsegetsub")
        isProVersion.postValue(sharedPref.getBoolean("isSubscribed", false))
    }

    fun savePricePrefs(context: Context, pricelist: List<InAppPricess>) {
        val editor = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).edit()
        editor.putString("price_list", pricelist.toString())
        editor.apply()
    }

    fun saveDurationPrefs(context: Context, duration: List<String>) {
        val editor = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).edit()
        editor.putStringSet("duration_list", duration.toSet())
        editor.apply()
    }

    fun saveInAppPricePrefs(context: Context, price: String) {
        val editor = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE).edit()
        editor.putString("price_inapp", price)
        editor.apply()
    }

    fun getInAppPrefs(context: Context) {
        val stringSet = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            .getStringSet("price_list", emptySet())
        val inapppref = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            .getString("price_inapp", "")
        val durationpref = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            .getStringSet("duration_list", emptySet())
        inAppPrice = inapppref
        subPurchaseList = getList(context) as List<InAppPricess>?
        durationList = durationpref?.toList()
    }

    fun getList(context: Context): List<InAppPricess?>? {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val jsonList = sharedPreferences.getString("price_list", null)
        if (jsonList != null) {
            val gson = Gson()
            val listType: Type = object : TypeToken<List<InAppPricess?>?>() {}.type
            return gson.fromJson<List<InAppPricess?>>(jsonList, listType)
        }
        return null
    }

    fun saveList(context: Context, myList: List<InAppPricess?>?) {
        val gson = Gson()
        val jsonList = gson.toJson(myList)
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("price_list", jsonList)
        editor.apply()
    }

    fun saveOffer(context: Context, offer: InAppPricess?) {
        val gson = Gson()
        val jsonList = gson.toJson(offer)
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("offer", jsonList)
        editor.apply()
    }

    fun saveEverSubscribed (context: Context,value:Boolean){
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("eversubscribe",value)
        editor.apply()
    }

    fun wasEverSubscribed (context: Context) :Boolean{
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val value = sharedPreferences.getBoolean("eversubscribe",false)
        return value
    }
}