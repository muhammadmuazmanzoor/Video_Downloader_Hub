package com.avd.ui.main.proxies

import androidx.databinding.ObservableField
import com.avd.data.local.model.Proxy
import com.avd.ui.main.base.BaseViewModel
import com.avd.util.proxy_utils.CustomProxyController
import com.avd.util.scheduler.BaseSchedulers
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject


@HiltViewModel
class ProxiesViewModel @Inject constructor(
    private val proxyController: CustomProxyController,
    private val baseSchedulers: BaseSchedulers
) : BaseViewModel() {
    val currentProxy = ObservableField(Proxy.noProxy())

    val proxiesList: ObservableField<MutableList<Proxy>> = ObservableField(mutableListOf())

    val isProxyOn = ObservableField(false)

    private val compositeDisposable = CompositeDisposable()

    override fun start() {
        fetchProxies()
        currentProxy.set(proxyController.getCurrentSavedProxy())
        isProxyOn.set(proxyController.isProxyOn())
    }

    private fun fetchProxies() {
        val disposable = proxyController.fetchProxyList().subscribeOn(baseSchedulers.io)
            .observeOn(baseSchedulers.computation).subscribe {
                proxiesList.set(it.toMutableList())
            }
        compositeDisposable.add(disposable)
    }

    override fun stop() {
        compositeDisposable.clear()
    }


    fun setProxy(proxy: Proxy) {
        proxyController.setCurrentProxy(proxy)
        currentProxy.set(proxy)
        isProxyOn.set(true)

        refreshList()
    }

    fun turnOffProxy() {
        proxyController.setIsProxyOn(false)
        isProxyOn.set(false)
    }

    fun turnOnProxy() {
        proxyController.setIsProxyOn(true)
        isProxyOn.set(true)
    }

    private fun refreshList() {
        val refreshed = proxiesList.get()?.toMutableList()
        proxiesList.set(refreshed)
    }
}