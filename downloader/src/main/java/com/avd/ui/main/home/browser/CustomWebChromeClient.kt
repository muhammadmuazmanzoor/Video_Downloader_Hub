package com.avd.ui.main.home.browser

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import com.avd.databinding.FragmentWebTabBinding
import com.avd.ui.main.home.browser.webTab.WebTab
import com.avd.ui.main.home.browser.webTab.WebTabViewModel
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.AppLogger
import com.avd.util.AppUtil
import com.avd.util.SingleLiveEvent

class CustomWebChromeClient(
    private val tabViewModel: WebTabViewModel,
    private val settingsViewModel: SettingsViewModel,
    private val updateTabEvent: SingleLiveEvent<WebTab>,
    private val pageTabProvider: PageTabProvider,
    private val dataBinding: FragmentWebTabBinding,
    private val appUtil: AppUtil,
    private val mainActivity: Activity
) : WebChromeClient() {


    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        if (view != null && view.handler != null) {
            val href = view.handler.obtainMessage()
            view.requestFocusNodeHref(href)
            val url = href.data.getString("url") ?: ""
            val isAd = if (settingsViewModel.isAdBlocker.get()) {
                tabViewModel.isAd(url)
            } else {
                false
            }
            AppLogger.d("ON_CREATE_WINDOW::************* $url ${view.url} isAd:: $isAd  $isUserGesture")
            if (url.isEmpty() || !url.startsWith("http") || isAd || !isUserGesture) {
                return false
            }

            val transport = resultMsg!!.obj as WebView.WebViewTransport
            transport.webView = WebView(view.context)

            tabViewModel.openPageEvent.value =
                WebTab(
                    webview = transport.webView,
                    resultMsg = resultMsg,
                    url = "url",
                    title = view.title,
                    iconBytes = null
                )
            return true
        }
        return false
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String,
        result: JsResult
    ): Boolean {
        // Log the alert message to debug the issue
        Log.d("WebView", "JavaScript Alert: $message")
        // Return true to indicate we have handled the alert
        result.confirm()
        return true
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())

        val headers = pageTab.getHeaders() ?: emptyMap()
        val updateTab = WebTab(
            pageTab.getUrl(),
            pageTab.getTitle(),
            icon ?: pageTab.getFavicon(),
            headers,
            view,
            id = pageTab.id
        )
        updateTabEvent.value = updateTab
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        tabViewModel.setProgress(newProgress)
        if (newProgress == 100) {
            tabViewModel.isShowProgress.set(false)
        } else {
            tabViewModel.isShowProgress.set(true)
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        (mainActivity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        dataBinding.webviewContainer.visibility = View.GONE
        (mainActivity).window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dataBinding.customView.apply {
            removeAllViews()
            addView(view)
            visibility = View.VISIBLE
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            requestLayout()
        }
        dataBinding.constraintLayout4.visibility = View.GONE
        dataBinding.containerBrowser.visibility = View.GONE
        appUtil.hideSystemUI(mainActivity.window, dataBinding.customView)
    }


    override fun onHideCustomView() {
        super.onHideCustomView()
        dataBinding.customView.removeAllViews()
        dataBinding.webviewContainer.visibility = View.VISIBLE
        dataBinding.customView.visibility = View.GONE
        (mainActivity).window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dataBinding.containerBrowser.visibility = View.VISIBLE
        (mainActivity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        dataBinding.webviewContainer.requestLayout()
        appUtil.showSystemUI(mainActivity.window, dataBinding.customView)
        dataBinding.constraintLayout4.visibility=View.VISIBLE
    }


}
