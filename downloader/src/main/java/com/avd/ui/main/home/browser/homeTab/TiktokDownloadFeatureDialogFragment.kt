package com.avd.ui.main.home.browser.homeTab

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.avd.R
import com.avd.databinding.DialogTiktokDownloadFeatureBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TiktokDownloadFeatureDialogFragment(
    private val onTryNowClick: () -> Unit
) : DialogFragment() {

    private var binding: DialogTiktokDownloadFeatureBinding? = null
    private var mActivity: FragmentActivity? = null

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogTiktokDownloadFeatureBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()

        binding?.btnClose?.setOnClickListener {
            dismiss()
        }

        binding?.btnTryNow?.setOnClickListener {
            onTryNowClick()
            dismiss()
        }
    }

    private fun setupViewPager() {
        val images = listOf(
            R.drawable.ic_download1image,
            R.drawable.ic_download2image
        )
        val adapter = PermissionIllustrationAdapter(images)
        binding?.vpFeatureSteps?.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
