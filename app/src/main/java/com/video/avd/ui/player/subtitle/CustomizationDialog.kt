package com.video.avd.ui.player.subtitle

import android.app.Dialog
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.video.avd.R
import com.video.avd.databinding.DialogSubtitleCustomizationBinding
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppVaultManager
import com.video.avd.utils.SharedPreferencesManager

class CustomizationDialog(var ls: SubtitleDialog.SubTitleClickListener) : DialogFragment() {

    private var binding: DialogSubtitleCustomizationBinding? = null
    private var sharedPref: AppVaultManager? = null
    private var subtitleCustomizationValues: SubtitleCustomizationValues? = null
    private var listener: SubtitleCustomizationsListener? = null
    private var customizeValues = SubtitleCustomizationValues()
    var sharedPreferencesManager: SharedPreferencesManager?=null
    val colorList = listOf(
        ColorItem("#FFFFFF"), // white
        ColorItem("#0075FF"), // grey
        ColorItem("#FF0000"), // red
        ColorItem("#008000"), // green
        ColorItem("#FFFF00"), // yellow
        ColorItem("#00FFFF"), // cyan
        ColorItem("#FF00FF")  // magenta
    )
    val adapter = ColorAdapter(colorList, object : ColorAdapter.SubtitleCustomizationsListener {
        override fun onSetColor(colorHex: String) {
            // Handle color selection here
            val textColor= AppUtils.getSubtitleTextColor(colorHex)
            binding?.sample?.setTextColor(textColor)
            customizeValues.textColor = colorHex
            listener?.onSetColor(colorHex)
        }
    })
    private lateinit var items: Array<String>
 private val positionAdapter by lazy {
     PositionAdapter(items, object : PositionAdapter.PositionSelectionListener {
         override fun onPositionSelected(selectedItem: String) {
             try {
                 sharedPreferencesManager = requireActivity().let { SharedPreferencesManager(it) }
                 sharedPreferencesManager?.saveSubtitlePosition(requireContext(),"subtitlePosition",selectedItem)
             } catch (e: Exception) {
                e.printStackTrace()
             }
             binding?.sample?.gravity = when (selectedItem.lowercase()) {
                 "upper" -> Gravity.TOP
                 "lower" -> Gravity.BOTTOM
                 "middle" -> Gravity.CENTER
                 else -> Gravity.BOTTOM // Default alignment if input is unrecognized
             }
             listener?.onSetAlignment(selectedItem)
             customizeValues.position = selectedItem
         }
     })
 }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout using binding
        binding = DialogSubtitleCustomizationBinding.inflate(LayoutInflater.from(context))
        try {
            if(sharedPreferencesManager==null){
                sharedPreferencesManager = requireActivity().let { SharedPreferencesManager(it) }
                setDefaultPosition()
            }
            else{
                setDefaultPosition()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Create AlertDialog using AlertDialog.Builder
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding?.root)
        val dialog = builder.create()
        items = arrayOf(
            getString(R.string.upper),
            getString(R.string.lower),
            getString(R.string.middle)
        )
        // Initialize shared preferences
        sharedPref = AppVaultManager(requireContext())
        subtitleCustomizationValues = sharedPref?.getSubtitleValues()

        // Set the progress for sizeBar
        binding?.sizeBar?.progress = subtitleCustomizationValues?.size?.toInt() ?: 40 // 40 is initial value
        setAdapter()
        // Set click listeners for customization
        clickListeners()

        return dialog
    }
    private fun setDefaultPosition(){
        val selectedItem=sharedPreferencesManager?.getSubtitlePosition(requireContext(),"subtitlePosition","lower")
        binding?.sample?.gravity = when (selectedItem?.lowercase()) {
            "upper" -> Gravity.TOP
            "lower" -> Gravity.BOTTOM
            "middle" -> Gravity.CENTER
            else -> Gravity.BOTTOM // Default alignment if input is unrecognized
        }
        positionAdapter.updatePosition(when (selectedItem?.lowercase()) {
            "upper" -> 0
            "lower" -> 1
            "middle" -> 2
            else -> 1 // Default alignment if input is unrecognized
        })
        if (selectedItem != null) {
            listener?.onSetAlignment(selectedItem)
        }
        if (selectedItem != null) {
            customizeValues.position = selectedItem
        }
    }

    private fun setAdapter() {
        binding?.recyclerView?.adapter = adapter
        binding?.rvPosition?.adapter = positionAdapter
    }

    private fun clickListeners() {
        binding?.sizeBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding?.sample?.setTextSize(TypedValue.COMPLEX_UNIT_SP, progress.toFloat())
                listener?.onSetTextSize(progress.toString())
                customizeValues.size = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Done button handling
        binding?.ivBack?.setOnClickListener {
            sharedPref?.saveSubtitleValues(customizeValues)
//            val subtitleDialog = SubtitleDialog(ls)
//            subtitleDialog.show(parentFragmentManager, "")
            dismiss()
        }
    }

    fun setCustomizationListener(listener: SubtitleCustomizationsListener) {
        this.listener = listener
    }
/*    override fun onSetTextShadow(position: String) {
        val defaultColor = Color.parseColor("#000000") // Default shadow color (black)
        val defaultRadius = 8f // Default shadow radius

        when (position.lowercase()) {
            "top" -> binding?.tvLayout?.setShadowLayer(defaultRadius, 0f, -defaultRadius, defaultColor)
            "bottom" -> binding?.tvLayout?.setShadowLayer(defaultRadius, 0f, defaultRadius, defaultColor)
            "left" -> binding?.tvLayout?.setShadowLayer(defaultRadius, -defaultRadius, 0f, defaultColor)
            "right" -> binding?.tvLayout?.setShadowLayer(defaultRadius, defaultRadius, 0f, defaultColor)
            "center" -> binding?.tvLayout?.setShadowLayer(defaultRadius, 0f, 0f, defaultColor)
            else -> {
                // Default to center shadow if position is unrecognized
                binding?.tvLayout?.setShadowLayer(defaultRadius, 0f, 0f, defaultColor)
            }
        }
    }*/

}
