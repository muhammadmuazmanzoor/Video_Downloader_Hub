package com.video.avd.ui.inapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.video.avd.utils.InAppPurchases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ProViewModel @Inject constructor(
    val inAppPurchases: InAppPurchases
): ViewModel(){
    var isWeeklyPurchasedAlready = MutableLiveData<Boolean>()
    var position: Int? = null
    var buttonClick = MutableLiveData<Boolean>()
    private val _priceFlow = MutableStateFlow<String?>(null)
    val priceFlow: StateFlow<String?>
    init {
        priceFlow=_priceFlow
    }
    fun onClick(mpPosition: Int) {
        position = mpPosition
        buttonClick.value = true
    }
}