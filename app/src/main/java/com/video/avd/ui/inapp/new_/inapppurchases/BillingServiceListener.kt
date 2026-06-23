package com.video.avd.ui.inapp.new_.inapppurchases

interface BillingServiceListener {
    /**
     * Callback will be triggered upon obtaining information about product prices
     *
     * @param iapKeyPrices - a map with available products
     */
    fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>)
}