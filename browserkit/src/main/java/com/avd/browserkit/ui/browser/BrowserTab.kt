package com.avd.browserkit.ui.browser

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New tab",
    val url: String = "about:blank",
)
