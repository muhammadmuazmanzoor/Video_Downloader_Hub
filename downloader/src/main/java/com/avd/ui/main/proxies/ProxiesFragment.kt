package com.avd.ui.main.proxies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.avd.data.local.model.Proxy
import com.avd.databinding.FragmentProxiesBinding
import com.avd.ui.component.adapter.ProxiesAdapter
import com.avd.ui.component.adapter.ProxiesListener
import com.avd.ui.main.base.BaseFragment
import com.avd.ui.main.progress.WrapContentLinearLayoutManager
import com.avd.util.DownloaderModuleNavigator
import com.avd.util.proxy_utils.CustomProxyController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProxiesFragment : BaseFragment() {

    companion object {
        fun newInstance() = ProxiesFragment()
    }

    @Inject
    lateinit var proxyController: CustomProxyController

    private lateinit var dataBinding: FragmentProxiesBinding

    private lateinit var proxiesViewModel: ProxiesViewModel

    private lateinit var proxiesAdapter: ProxiesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        proxiesViewModel = DownloaderModuleNavigator.proxiesViewModel!!
        proxiesAdapter = ProxiesAdapter(emptyList(), proxiesListener, proxiesViewModel)
        val managerL = WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        dataBinding = FragmentProxiesBinding.inflate(inflater, container, false).apply {
            this.listener = proxiesListener
            this.viewModel = proxiesViewModel
            this.proxiesList.layoutManager = managerL
            this.proxiesList.adapter = proxiesAdapter
        }
        return dataBinding.root
    }

    private val proxiesListener = object : ProxiesListener {
        override fun onProxyClicked(view: View, proxy: Proxy) {
            setProxy(proxy)
        }

        override fun onProxyToggle(isChecked: Boolean) {
            if (isChecked) {
                proxiesViewModel.turnOnProxy()
            } else {
                proxiesViewModel.turnOffProxy()
            }
        }
    }

    fun setProxy(proxy: Proxy) {
        proxiesViewModel.setProxy(proxy)
    }
}