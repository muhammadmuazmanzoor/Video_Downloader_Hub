package com.avd.ui.dialog

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Safe dialog manager that prevents crashes and ANRs
 * Handles download completion dialogs in the downloader module
 */
object DownloadDialogManager {
    var defaultTabPos: Int = 0
    private const val TAG = "TESTdialogue"
    private var isDialogShowing = false
    private val handledCompletionKeys = mutableSetOf<String>()

    fun resetCompletionSession() {
        isDialogShowing = false
        handledCompletionKeys.clear()
    }

    private fun completionKey(info: DownloadCompletionInfo): String =
        "${info.filePath}|${info.videoTitle}|${info.videoName}"
    
    /**
     * Shows the download completion dialog safely
     * Only shows in downloader module and prevents multiple dialogs
     */
    fun showDownloadCompletionDialog(
        context: Context,
        completionInfo: DownloadCompletionInfo = DownloadCompletionInfo(),
        onDownloadMore: (() -> Unit)? = null,
        onGoToDownloads: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        try {
            Log.d(TAG, "=== DownloadDialogManager.showDownloadCompletionDialog called ===")
            Log.d(TAG, "Context type: ${context::class.java.simpleName}")
            
            // Check if we're in the right context
            if (context !is FragmentActivity) {
                Log.w(TAG, "Context is not a FragmentActivity, cannot show dialog")
                return
            }
            
            Log.d(TAG, "Context is FragmentActivity: true")
            
            val key = completionKey(completionInfo)
            if (handledCompletionKeys.contains(key)) {
                Log.d(TAG, "Completion dialog already shown for: $key")
                return
            }

            if (isDialogShowing) {
                Log.d(TAG, "Dialog already showing, ignoring request")
                return
            }

            context.lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    try {
                        if (context.isFinishing || context.isDestroyed) {
                            Log.w(TAG, "Activity is finishing or destroyed, cannot show dialog")
                            return@withContext
                        }

                        isDialogShowing = true

                        val dialog = DownloadCompletionDialog.newInstance(completionInfo)
                            .setOnDownloadMoreClickListener {
                                onDownloadMore?.invoke()
                            }
                            .setOnGoToDownloadsClickListener {
                                onGoToDownloads?.invoke()
                            }
                            .setOnDismissListener {
                                isDialogShowing = false
                                onDismiss?.invoke()
                            }

                        Log.d(TAG, "Showing dialog for: ${completionInfo.videoTitle}")
                        dialog.show(context.supportFragmentManager, "DownloadCompletionDialog")
                        handledCompletionKeys.add(key)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error showing download completion dialog: ${e.message}")
                        isDialogShowing = false
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in showDownloadCompletionDialog: ${e.message}")
            isDialogShowing = false
        }
    }
    
    /**
     * Dismisses any showing dialog safely
     */
    fun dismissDialog(context: Context) {
        try {
            if (context is FragmentActivity) {
                context.lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        try {
                            val fragment = context.supportFragmentManager
                                .findFragmentByTag("DownloadCompletionDialog")
                            
                            if (fragment is DownloadCompletionDialog) {
                                fragment.dismissAllowingStateLoss()
                            }
                            isDialogShowing = false
                        } catch (e: Exception) {
                            Log.e(TAG, "Error dismissing dialog: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dismissDialog: ${e.message}")
        }
    }
    
    /**
     * Checks if dialog is currently showing
     */
    fun isDialogShowing(): Boolean = isDialogShowing
}
