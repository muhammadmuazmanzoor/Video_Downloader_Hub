package com.avd.ui.main.home.bottomsheet

import com.avd.ui.main.home.browser.webTab.WebTab


interface TabBottomSheetCallback {
    fun onTabSelected(tab: WebTab)
    fun newTabInsert()
    fun deleteall()
    fun tabDelete(tab:WebTab)
}