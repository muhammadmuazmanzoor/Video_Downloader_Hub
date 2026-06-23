package com.video.avd.ui.inapp.new_.inapppurchases

import android.content.Context
import com.video.avd.R

object IapManager {
    private var iapConnector: IapConnector? = null
    const val skuKeyWeekly = "weekly_pro"
   // const val skuKeyMonthly = "monthly_plan"
    const val skuKeyLifetime = "ads_remove"
    fun getIapConnector(context: Context): IapConnector {
        return if (iapConnector != null) {
            iapConnector as IapConnector
        } else {
            val nonConsumablesList = listOf(skuKeyLifetime)
            // val consumablesList = listOf("")
            val subsList = listOf(skuKeyWeekly)
            IapConnector(
                context = context,
                nonConsumableKeys = nonConsumablesList,
                subscriptionKeys = subsList,
                key = context.getString(R.string.licenseKey),
                enableLogging = true
            )
        }
    }
}