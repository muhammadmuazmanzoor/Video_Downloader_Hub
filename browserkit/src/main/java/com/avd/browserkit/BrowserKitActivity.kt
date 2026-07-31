package com.avd.browserkit

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.avd.browserkit.api.BrowserKit
import com.avd.browserkit.api.BrowserLaunchMode
import com.avd.browserkit.databinding.ActivityBrowserKitBinding
import com.avd.browserkit.ui.browser.BrowserHostFragment
import com.avd.browserkit.ui.browser.BrowserViewModel
import com.avd.browserkit.util.AdultSiteBlocker
import com.avd.browserkit.util.BrowserKitLog
import com.avd.browserkit.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BrowserKitActivity : AppCompatActivity() {

    /** Same store as [BrowserHostFragment]'s `activityViewModels()` — shared tab state. */
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        // Warm Safe Browsing before first loadUrl when possible.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(applicationContext) { success ->
                BrowserKitLog.i("SafeBrowsing", "activity init success=$success")
            }
        }
        enableEdgeToEdge()
        val binding = ActivityBrowserKitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            val mode = intent.getStringExtra(BrowserKit.EXTRA_MODE)
                ?.let { runCatching { BrowserLaunchMode.valueOf(it) }.getOrNull() }
                ?: BrowserLaunchMode.BLANK
            val query = intent.getStringExtra(BrowserKit.EXTRA_QUERY)
            val url = intent.getStringExtra(BrowserKit.EXTRA_URL)

            // Show browser UI immediately — do not wait on yt-dlp native init (ANR risk).
            supportFragmentManager.beginTransaction()
                .replace(
                    binding.browserContainer.id,
                    BrowserHostFragment.newInstance(mode, query, url),
                )
                .commit()
            if (intent.getBooleanExtra(BrowserKit.EXTRA_OPEN_TABS, false)) {
                browserViewModel.showTabsSwitcher()
            }
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { BrowserKitInitializer.initializeAwait(applicationContext) }
            }
        }
    }

    /**
     * The activity is `singleTask`, so relaunching it reuses the live browser session instead of
     * starting a second one with an empty tab list. [BrowserHostFragment] already exists here, so
     * the new intent is applied through the shared [BrowserViewModel] rather than fragment args.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra(BrowserKit.EXTRA_OPEN_TABS, false)) {
            browserViewModel.showTabsSwitcher()
            return
        }

        val mode = intent.getStringExtra(BrowserKit.EXTRA_MODE)
            ?.let { runCatching { BrowserLaunchMode.valueOf(it) }.getOrNull() }
            ?: BrowserLaunchMode.BLANK
        val searchTemplate = BrowserKit.getConfig().searchUrlTemplate
        val target = when (mode) {
            BrowserLaunchMode.SEARCH ->
                UrlUtils.normalizeInput(intent.getStringExtra(BrowserKit.EXTRA_QUERY).orEmpty(), searchTemplate)
            BrowserLaunchMode.URL ->
                UrlUtils.normalizeInput(intent.getStringExtra(BrowserKit.EXTRA_URL).orEmpty(), searchTemplate)
            BrowserLaunchMode.BLANK -> null
        }
        if (target.isNullOrBlank() || target == "about:blank") return
        if (AdultSiteBlocker.isBlocked(target)) {
            BrowserKitLog.w("Adult", "newIntent blocked ${BrowserKitLog.shortUrl(target)}")
            return
        }

        browserViewModel.hideTabsSwitcher()
        browserViewModel.addNewTab(target)
    }

    override fun onPause() {
        runCatching { CookieManager.getInstance().flush() }
        super.onPause()
    }

    override fun onStop() {
        runCatching { CookieManager.getInstance().flush() }
        super.onStop()
    }
}
