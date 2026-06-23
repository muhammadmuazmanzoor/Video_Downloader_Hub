package com.video.avd.ui.inapp.model

import androidx.annotation.Keep

@Keep
data class InAppPricess(
    var name: String = "",
    var price: String = "",
    var key: String = "",
    var freeTrial: String = "",
    var offerPrice  : String = "",
    var priceAmountmicrose : Long =0,
    var introductoryPriceAmountMicros : Long =0
)