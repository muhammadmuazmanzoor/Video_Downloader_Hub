package com.video.avd.ui.player.subtitle

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.video.avd.R
import com.video.avd.databinding.DialogSubtitleNewBinding
import com.video.avd.ui.player.PlayerViewModel
import com.video.avd.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubtitleDialog(var ls: SubTitleClickListener? = null) : DialogFragment() {

    private var binding: DialogSubtitleNewBinding? = null
    var listener: SubTitleClickListener? = null
    private var subtitleCustomizationListener: SubtitleCustomizationsListener? = null
    private val viewModel: PlayerViewModel by activityViewModels()
    companion object{
        var hasSubtitledg: Boolean? = null
        var subtitleTurnOn: Boolean? = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSubtitleNewBinding.inflate(LayoutInflater.from(context))
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding?.root)
        val dialog = builder.create()

        if (ls != null) {
            listener = ls
        }
        setupClickListeners()
        initializeState()
        return dialog
    }

    private fun initializeState() {
        binding?.toggle?.isChecked=subtitleTurnOn?:false
        viewModel.showSubtitleView.observe(this) { isSubtitleOn ->
            updateUI(isSubtitleOn)
        }
    }

    private fun updateUI(isSubtitleOn: Boolean) {
        if (hasSubtitledg == true && isSubtitleOn) {
//                binding?.toggle?.isChecked = true
                showSubtitleCustomizationUI()
                Log.e("SearchObserver", "Show Customization: subtitle turnOn$isSubtitleOn hasSubtitle: $hasSubtitledg")
        } else {
            Log.e("SearchObserver", "Show Customization: subtitle turnOn$isSubtitleOn hasSubtitle: $hasSubtitledg")
            hideSubtitleCustomizationUI()
        }
    }

    private fun showSubtitleCustomizationUI() {
        binding?.view2?.visibility = View.VISIBLE
        binding?.layoutSubtitle?.visibility = View.VISIBLE
        binding?.clCustomization?.visibility = View.VISIBLE
        binding?.detailSubtitle?.visibility = View.GONE
    }

    private fun hideSubtitleCustomizationUI() {
        binding?.view2?.visibility = View.GONE
        binding?.layoutSubtitle?.visibility = View.VISIBLE
        binding?.clDownload?.visibility = View.VISIBLE
        binding?.clCustomization?.visibility = View.GONE
        binding?.detailSubtitle?.visibility = View.GONE
        Log.e("SearchObserver", "hideSubtitleCustomizationUI: $subtitleTurnOn")
    }

    private fun handleNoSubtitleState() {
        if (subtitleTurnOn == true) {
            binding?.view2?.visibility = View.GONE
            binding?.layoutSubtitle?.visibility = View.VISIBLE
            binding?.clDownload?.visibility = View.VISIBLE
            binding?.clCustomization?.visibility = View.GONE
            binding?.detailSubtitle?.visibility = View.GONE
        } else {
            binding?.layoutSubtitle?.visibility = View.INVISIBLE
            binding?.detailSubtitle?.visibility = View.VISIBLE
        }
        Log.e("SearchObserver", "No subtitles available")
    }

    private fun setupClickListeners() {
        binding?.clOpen?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 31) {
                openFile()
            } else {
                listener?.onClick("offline")
            }
        }

        binding?.clDownload?.setOnClickListener {
            if (NetworkUtils.isOnline(requireContext())) {
                listener?.onClick("online")
            } else {
                Toast.makeText(requireContext(), "Please connect to internet first!", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.toggle?.setOnCheckedChangeListener { _, isChecked ->
//            listener?.onClick("toggle")
            if (isChecked) {
//                subtitleTurnOn = isChecked
                viewModel.showSubtitleView.postValue(true)
            } else {
//                subtitleTurnOn = false
                viewModel.showSubtitleView.postValue(false)
            }
        }

        binding?.clCustomization?.setOnClickListener {
            dismiss()
            showCustomizationDialog()
        }

        binding?.ivBack?.setOnClickListener { dismiss() }
    }

    private fun showCustomizationDialog() {
        val customizationDialog = listener?.let { CustomizationDialog(it) }
        customizationDialog?.show(parentFragmentManager, "CustomizationDialog")
        customizationDialog?.isCancelable = true
        customizationDialog?.setCustomizationListener(object : SubtitleCustomizationsListener {
            override fun onSetAlignment(alignment: String) {
                subtitleCustomizationListener?.onSetAlignment(alignment)
            }

            override fun onSetTextSize(textSize: String) {
                subtitleCustomizationListener?.onSetTextSize(textSize)
            }

            override fun onSetColor(colorCode: String) {
                subtitleCustomizationListener?.onSetColor(colorCode)
            }

            override fun onSetTextShadow(position: String) {
                subtitleCustomizationListener?.onSetTextShadow(position)
            }
        })
    }

    private fun openFile() {
        try {
            val intent = Intent(requireContext(), MediaStoreChooserActivity::class.java).apply {
                putExtra(MediaStoreChooserActivity.SUBTITLES, true)
            }
            startActivityForResult(intent, 21)
//            listener?.onClick("offline")
            dismiss()
        } catch (e: Exception) {
            Log.e("SearchDialog", "Error opening file: ${e.message}")
            e.printStackTrace()
        }
    }

    fun setSelected(listener: SubTitleClickListener?) {
        this.listener = listener
    }

    fun setSubtitleCustomizationListener(listener: SubtitleCustomizationsListener) {
        this.subtitleCustomizationListener = listener
    }

    interface SubTitleClickListener {
        fun onClick(which: String?)
    }
}

