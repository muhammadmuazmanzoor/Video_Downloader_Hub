package com.avd.browserkit.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import com.avd.browserkit.R
import com.avd.browserkit.databinding.BottomSheetFindInPageBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

object FindInPageBottomSheet {
    private const val TAG = "FindInPageBottomSheet"

    fun show(manager: FragmentManager, listener: FindInPageListener) {
        if (manager.findFragmentByTag(TAG) != null) return
        FindInPageBottomSheetFragment().apply {
            this.listener = listener
        }.show(manager, TAG)
    }
}

interface FindInPageListener {
    fun onQueryChanged(query: String)
    fun onFindNext()
    fun onFindPrevious()
    fun onClosed()
}

class FindInPageBottomSheetFragment : BottomSheetDialogFragment() {
    var listener: FindInPageListener? = null
    private var _binding: BottomSheetFindInPageBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.Theme_BrowserKit_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetFindInPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etFindQuery.doAfterTextChanged { text ->
            listener?.onQueryChanged(text?.toString().orEmpty().trim())
        }
        binding.etFindQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                listener?.onFindNext()
                true
            } else {
                false
            }
        }
        binding.btnFindNext.setOnClickListener { listener?.onFindNext() }
        binding.btnFindPrevious.setOnClickListener { listener?.onFindPrevious() }

        binding.etFindQuery.post {
            binding.etFindQuery.requestFocus()
            val imm = requireContext().getSystemService<InputMethodManager>()
            imm?.showSoftInput(binding.etFindQuery, InputMethodManager.SHOW_IMPLICIT)
        }

        (dialog as? BottomSheetDialog)?.let { sheet ->
            sheet.setOnShowListener {
                val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { panel ->
                    panel.background = null
                    BottomSheetBehavior.from(panel).apply {
                        skipCollapsed = true
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        listener?.onClosed()
        super.onDestroyView()
        _binding = null
    }
}
