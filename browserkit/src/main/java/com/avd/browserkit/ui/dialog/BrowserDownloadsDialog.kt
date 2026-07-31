package com.avd.browserkit.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.avd.browserkit.R
import com.avd.browserkit.data.BrowserRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.runBlocking

object BrowserDownloadsDialog {

    private const val TAG = "BrowserDownloadsDialog"

    fun show(manager: FragmentManager, repository: BrowserRepository) {
        if (manager.findFragmentByTag(TAG) != null) return
        BrowserDownloadsDialogFragment().apply {
            this.repository = repository
        }.show(manager, TAG)
    }
}


class BrowserDownloadsDialogFragment : DialogFragment() {
    lateinit var repository: BrowserRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = runBlocking { repository.getAllDownloads() }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.bk_downloads)
            .setPositiveButton(android.R.string.ok, null)
        if (items.isEmpty()) {
            builder.setMessage(R.string.bk_no_downloads)
        } else {
            val labels = items.map { it.title.ifBlank { it.pageUrl } }.toTypedArray()
            builder.setItems(labels, null)
        }
        return builder.create()
    }

}



