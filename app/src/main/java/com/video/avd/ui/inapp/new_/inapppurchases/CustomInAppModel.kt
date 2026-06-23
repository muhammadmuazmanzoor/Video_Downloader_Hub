package com.video.avd.ui.inapp.new_.inapppurchases

data class CustomInAppModel(
    val id: Int,
    val durationPlan: String,
    val description: String,
    val totalPrice: String,
    val discountedPrice: String,
    val discountPercent: String,
    val showDiscount: Boolean,
    val showHotOffer: Boolean = false
) {

    fun getDiscountedAmount(): String {
        val price: String = when (durationPlan) {
            "weekly" -> "$discountedPrice/week"
            "yearly" -> "$discountedPrice/year"
            "lifetime" -> discountedPrice
            else -> discountedPrice
        }
        return price
    }
}
