package com.avd

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.splitinstall.SplitInstallHelper
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

class DynamicModuleDownloader(private val activity: FragmentActivity, private val moduleName: String) {

    private var splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(activity)
    private var sessionId: Int = 0
    private var onModuleInstalled: (() -> Unit)? = null // Callback for when the module is installed

    private val listener = SplitInstallStateUpdatedListener { state: SplitInstallSessionState ->
        if (state.sessionId() == sessionId) {
            when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    Log.d("DynamicModuleDownloader", "Downloading $moduleName module")
                    // Optional: Show download progress here
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d("DynamicModuleDownloader", "$moduleName module installed")
                    SplitInstallHelper.updateAppInfo(activity.applicationContext)
                    onModuleInstalled?.invoke() // Call the callback when module is installed
                }
                SplitInstallSessionStatus.FAILED -> {
                    Log.d("DynamicModuleDownloader", "Failed to install $moduleName module")
                }
            }
        }
    }

    fun installOrLaunchModule(onModuleInstalled: () -> Unit) {
        this.onModuleInstalled = onModuleInstalled // Assign the callback
        splitInstallManager.registerListener(listener)
        if (splitInstallManager.installedModules.contains(moduleName)) {
            Log.d("DynamicModuleDownloader", "$moduleName is already installed")
            onModuleInstalled() // Invoke the callback if the module is already installed
        } else {
            installModule()
        }
    }

    private fun installModule() {
        val request = SplitInstallRequest.newBuilder().addModule(moduleName).build()
        splitInstallManager.startInstall(request)
            .addOnSuccessListener { sessionId = it }
            .addOnFailureListener {
                Log.d("DynamicModuleDownloader", "Failed to download $moduleName module")
            }
            .addOnCompleteListener {
                Log.d("DynamicModuleDownloader", "$moduleName module download complete")
            }
    }

    fun unregisterListener() {
        splitInstallManager.unregisterListener(listener)
    }

    fun registerListener() {
        splitInstallManager.registerListener(listener)
    }
}
