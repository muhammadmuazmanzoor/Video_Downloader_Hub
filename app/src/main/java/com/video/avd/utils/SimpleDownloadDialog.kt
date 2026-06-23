package com.video.avd.utils

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.avd.ui.dialog.DownloadCompletionInfo
import com.avd.ui.dialog.DownloadDialogManager
import com.avd.util.SharedPrefHelper

/**
 * Simple, direct approach to show download completion dialog
 * No broadcast receivers or services needed
 * Just call this when download completes
 */

object SimpleDownloadDialog {

    private const val TAG = "TESTdialogue"
    private var sharedPrefHelper: SharedPrefHelper? = null

    /**
     * Shows download completion dialog directly
     * Call this method when download completes successfully
     *
     * @param context - Activity context
     * @param videoName - Name of the downloaded video
     * @param isFromHomeScreen - Whether user came from home screen (prevents dialog)
     */
    fun showOnDownloadComplete(
        context: Context,
        completionInfo: DownloadCompletionInfo = DownloadCompletionInfo(),
        isFromHomeScreen: Boolean = false,
        onDownloadMore: (() -> Unit)? = null,
        onGoToDownloads: (() -> Unit)? = null,
    ) {
        try {
            Log.d(TAG, "=== SimpleDownloadDialog.showOnDownloadComplete called ===")
            Log.d(TAG, "Context type: ${context::class.java.simpleName}")
            Log.d(TAG, "Video title: ${completionInfo.videoTitle}")
            Log.d(TAG, "Is from home screen: $isFromHomeScreen")

            // Check if context is valid
            if (context !is FragmentActivity) {
                Log.w(TAG, "Context is not a FragmentActivity, cannot show dialog")
                return
            }

            Log.d(TAG, "Context is FragmentActivity: true")
            Log.d(TAG, "Activity isFinishing: ${context.isFinishing}")
            Log.d(TAG, "Activity isDestroyed: ${context.isDestroyed}")

            // Check if activity is still valid
            if (context.isFinishing || context.isDestroyed) {
                Log.w(TAG, "Activity is finishing or destroyed, cannot show dialog")
                return
            }

            // Initialize SharedPrefHelper if not already done
            if (sharedPrefHelper == null) {
                sharedPrefHelper = SharedPrefHelper(context)
            }

            if (isFromHomeScreen) {
                Log.d(TAG, "User came from home screen, not showing dialog")
                return
            }

            DownloadDialogManager.showDownloadCompletionDialog(
                context = context,
                completionInfo = completionInfo,
                onDownloadMore = {
                    resetDialogSession(context)
                    Log.d(TAG, "User clicked 'Download More Videos'")
                    // You can add custom navigation here
                    onDownloadMore?.invoke()

                },
                onGoToDownloads = {
                    resetDialogSession(context)
                    Log.d(TAG, "User clicked 'Go to Downloads'")
                    // You can add custom navigation here
                    onGoToDownloads?.invoke()

                },
                onDismiss = {
                    resetDialogSession(context)
                    Log.d(TAG, "Download completion dialog dismissed")
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error showing download completion dialog: ${e.message}")
        }
    }

    /**
     * Reset the dialog session - call this when app starts or when you want to allow dialog again
     */
    fun resetDialogSession(context: Context) {
        if (sharedPrefHelper == null) {
            sharedPrefHelper = SharedPrefHelper(context)
        }
        sharedPrefHelper?.resetDownloadDialogSession()
        Log.d(TAG, "Dialog session reset - dialog can be shown again")
    }
}