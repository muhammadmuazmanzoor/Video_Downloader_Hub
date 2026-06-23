package com.video.avd.ui.inapp.new_.inapppurchases

interface BillingClientConnectionListener {
    fun onConnected(status: Boolean, billingResponseCode: Int)
}