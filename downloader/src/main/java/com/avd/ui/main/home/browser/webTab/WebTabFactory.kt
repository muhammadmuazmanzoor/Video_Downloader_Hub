package com.avd.ui.main.home.browser.webTab

import android.util.Patterns

class WebTabFactory {
//    companion object {
//        fun createWebTabFromInput(input: String): WebTab {
//            if (input.isNotEmpty()) {
//                return if (input.startsWith("http://") || input.startsWith("https://")) {
//                    WebTab(input, null, null, emptyMap())
//                } else if (Patterns.WEB_URL.matcher(input).matches()) {
//                    WebTab("https://$input", null, null, emptyMap())
//                } else {
//                    WebTab(
//                        String.format(BrowserViewModel.SEARCH_URL, input),
//                        null,
//                        null,
//                        emptyMap())
//                }
//            }
//
//            return WebTab.HOME_TAB
//        }
//    }

    companion object {
        fun createWebTabFromInput(input: String, searchEngine: String): WebTab {
            if (input.isNotEmpty()) {
                val url = when {
                    input.startsWith("http://") || input.startsWith("https://") -> input
                    Patterns.WEB_URL.matcher(input).matches() -> "https://$input"
                    else -> {
                        val searchUrl = when (searchEngine.lowercase()) {
                            "google" -> "https://www.google.com/search?q=%s"
                            "bing" -> "https://www.bing.com/search?q=%s"
                            "duckduckgo" -> "https://duckduckgo.com/?q=%s"
                            "yahoo" -> "https://search.yahoo.com/search?p=%s"
                            "yandex" -> "https://yandex.com/search/?text=%s"
                            "baidu" -> "https://www.baidu.com/s?wd=%s"
                            "coc coc" -> "https://coccoc.com/search?query=%s"
                            else -> "https://www.google.com/search?q=%s" // Default to Google
                        }
                        String.format(searchUrl, input)
                    }
                }
                return WebTab(url, null, null, emptyMap())
            }

            return WebTab.HOME_TAB
        }
    }
}
