package com.avd.browserkit.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.avd.browserkit.R
import com.avd.browserkit.databinding.DialogYoutubeBlockedBinding

object YoutubeBlockedDialog {
    private const val TAG = "YoutubeBlockedDialog"

    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) != null) return
        YoutubeBlockedDialogFragment().show(manager, TAG)
    }
}

class YoutubeBlockedDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogYoutubeBlockedBinding.inflate(LayoutInflater.from(requireContext()))
        binding.btnYoutubeBlockedOk.setOnClickListener { dismiss() }
        return Dialog(requireContext(), R.style.Theme_BrowserKit_Dialog).apply {
            setContentView(binding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(true)
        }
    }
}
