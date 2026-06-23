package com.avd.util.proxy_utils

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.avd.data.local.model.Proxy
import com.avd.util.SharedPrefHelper
import com.avd.util.scheduler.BaseSchedulers
import io.reactivex.rxjava3.core.Observable
import okhttp3.OkHttpClient
import java.net.Authenticator
import java.net.PasswordAuthentication
import javax.inject.Inject

class CustomProxyController @Inject constructor(
    private val sharedPrefHelper: SharedPrefHelper,
    private val schedulers: BaseSchedulers,
    private var okHttpClient: OkHttpClient
) {

    // TODO store api key securely
    private val apiKey = "qwerty"
    
    // Lazy initialization flag to prevent blocking operations during Hilt dependency injection
    @Volatile
    private var initialized = false

    // Removed init block - proxy initialization is now lazy to prevent ANR during Hilt injection
    // Proxy will be initialized when actually needed instead of during construction
    
    /**
     * Ensures proxy controller is initialized. Uses double-checked locking pattern
     * to prevent multiple initialization attempts while ensuring thread safety.
     * Note: Since getCurrentRunningProxy() always returns Proxy.noProxy() currently,
     * this initialization is mainly for future-proofing and doesn't perform blocking operations.
     */
    private fun ensureInitialized() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    // Mark as initialized - no blocking operations needed since
                    // getCurrentRunningProxy() always returns Proxy.noProxy() currently
                    initialized = true
                }
            }
        }
    }
    
    /**
     * Internal method to set proxy without triggering initialization check
     * to avoid circular dependency during lazy initialization.
     */
    private fun setCurrentProxyInternal(proxy: Proxy) {
        if (proxy == Proxy.noProxy()) {
            System.setProperty("http.proxyUser", "")
            System.setProperty("http.proxyPassword", "")
            System.setProperty("https.proxyUser", "")
            System.setProperty("https.proxyPassword", "")

            Authenticator.setDefault(object : Authenticator() {})

            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride({ }) {}
            }
        } else {
            sharedPrefHelper.setIsProxyOn(true)

            System.setProperty("http.proxyUser", proxy.user.trim())
            System.setProperty("http.proxyPassword", proxy.password.trim())
            System.setProperty("https.proxyUser", proxy.user.trim())
            System.setProperty("https.proxyPassword", proxy.password.trim())

            System.setProperty("http.proxyHost", proxy.host.trim())
            System.setProperty("http.proxyPort", proxy.port.trim())

            System.setProperty("https.proxyHost", proxy.host.trim())
            System.setProperty("https.proxyPort", proxy.port.trim())
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")

            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(proxy.user, proxy.password.toCharArray())
                }
            })

            val proxyConfig =
                ProxyConfig.Builder().addProxyRule("${proxy.host}:${proxy.port}").build()
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().setProxyOverride(proxyConfig, { }) {}
            }
        }

        sharedPrefHelper.setCurrentProxy(proxy)
    }

    fun setClient(client: OkHttpClient) {
        this.okHttpClient = client
    }

    fun getClient(): OkHttpClient? {
        return okHttpClient
    }

    fun getCurrentRunningProxy(): Proxy {
        // Lazy initialization - only initialize when actually needed
        ensureInitialized()
        return Proxy.noProxy()

//        return if (isProxyOn()) {
//            sharedPrefHelper.getCurrentProxy()
//        } else {
//            Proxy.noProxy()
//        }
    }

    fun getCurrentSavedProxy(): Proxy {
        ensureInitialized()
        return sharedPrefHelper.getCurrentProxy()
    }

    fun getProxyCredentials(): Pair<String, String> {
        ensureInitialized()
        val currProx = getCurrentRunningProxy()
        return Pair(currProx.user, currProx.password)
    }

    fun setCurrentProxy(proxy: Proxy) {
        ensureInitialized()
        setCurrentProxyInternal(proxy)
    }

    fun fetchProxyList(): Observable<List<Proxy>> {
        val proxiesText =
            "127.0.0.1 \t3000 \tProxies!!! \tloginhere \tpasswordhere \tCity \tCountry \t0.0.0.0 \t0.0.0.0 \t0.0.0.0 \texample.domen.com \t96767\n" +
                    "127.0.0.1 \t3000 \tProxies!!! \tloginhere \tpasswordhere \tCity \tCountry \t0.0.0.0 \t0.0.0.0 \t0.0.0.0 \texample.domen.com \t96767"

        return Observable.create<List<Proxy>> { emitter ->
            val result = arrayListOf<Proxy>()
            val proxyLines = proxiesText.split("\n")
            for (proxyLine in proxyLines) {
                val proxDataArr = proxyLine.split(Regex(" \t"))
                val host = proxDataArr[0].trim()
                val port = proxDataArr[1].trim()
                val user = proxDataArr[3].trim()
                val password = proxDataArr[4].trim()
                val city = proxDataArr[5].trim()
                val country = proxDataArr[6].trim()

                result.add(
                    Proxy(
                        host = host,
                        port = port,
                        countryCode = country,
                        cityName = city,
                        user = user,
                        password = password
                    )
                )
            }


            emitter.onNext(result)
            emitter.onComplete()
        }.doOnError {}.subscribeOn(schedulers.io)
    }

    fun isProxyOn(): Boolean {
        ensureInitialized()
        return sharedPrefHelper.getIsProxyOn()
    }

    fun setIsProxyOn(isOn: Boolean) {
        ensureInitialized()
        if (isOn) {
            setCurrentProxy(sharedPrefHelper.getCurrentProxy())
        } else {
            System.setProperty("http.proxyUser", "")
            System.setProperty("http.proxyPassword", "")
            System.setProperty("https.proxyUser", "")
            System.setProperty("https.proxyPassword", "")

            System.setProperty("http.proxyHost", "")
            System.setProperty("http.proxyPort", "")

            System.setProperty("https.proxyHost", "")
            System.setProperty("https.proxyPort", "")
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")

            Authenticator.setDefault(object : Authenticator() {})

            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride({ }) {}
            }
        }

        sharedPrefHelper.setIsProxyOn(isOn)
    }
}