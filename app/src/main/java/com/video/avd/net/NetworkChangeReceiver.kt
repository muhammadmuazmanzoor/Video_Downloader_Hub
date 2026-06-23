package com.video.avd.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.video.avd.utils.NetworkUtils

interface NetworkStateListener {
    fun onNetworkStateChanged(isOnline: Boolean)
}

class NetworkChangeReceiver : BroadcastReceiver() {
    private var networkStateListener: NetworkStateListener? = null

    fun setNetworkStateListener(listener: NetworkStateListener) {
        networkStateListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (context != null) {
            if (NetworkUtils.isOnline(context)) {
                // Internet connection is available, load the AdMob banner
                networkStateListener?.onNetworkStateChanged(true)
            }
        }
    }
}