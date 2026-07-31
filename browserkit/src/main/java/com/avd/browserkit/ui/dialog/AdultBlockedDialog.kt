package com.avd.browserkit.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.avd.browserkit.R
import com.avd.browserkit.databinding.DialogAdultBlockedBinding

object AdultBlockedDialog {
    private const val TAG = "AdultBlockedDialog"

    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) != null) return
        AdultBlockedDialogFragment().show(manager, TAG)
    }
}

class AdultBlockedDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAdultBlockedBinding.inflate(LayoutInflater.from(requireContext()))
        binding.btnAdultBlockedOk.setOnClickListener { dismiss() }
        return Dialog(requireContext(), R.style.Theme_BrowserKit_Dialog).apply {
            setContentView(binding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCanceledOnTouchOutside(true)
        }
    }
}
