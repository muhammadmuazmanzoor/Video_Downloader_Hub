package com.video.avd.ui.player.subtitle

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.video.avd.databinding.DialogLoaderBinding

class LoaderDialog : DialogFragment() {
    private var binding: DialogLoaderBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogLoaderBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isDownloading) {
            binding?.tvDownload?.text = "Downloading..."
        }else{
            binding?.tvDownload?.text = " Searching... "
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        try {
            val dialog = dialog
            if (dialog != null) {
                val width = (resources.displayMetrics.widthPixels * 0.75).toInt()
                dialog.window!!.setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val back = ColorDrawable(Color.TRANSPARENT)
                val inset = InsetDrawable(back, 35)
                dialog.window!!.setBackgroundDrawable(inset)
            }
        }catch (e:Exception){
            e.printStackTrace()
        }

    }
    companion object{
        var isDownloading = false
    }
}