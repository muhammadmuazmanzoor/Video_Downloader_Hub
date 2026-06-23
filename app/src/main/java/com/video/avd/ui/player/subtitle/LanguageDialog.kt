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
import androidx.recyclerview.widget.LinearLayoutManager
import com.video.avd.R
import com.video.avd.databinding.DialogLanguageBinding
import com.video.avd.ui.player.subtitle.LanguageAdapter.LanguageClickListener


class LanguageDialog : DialogFragment(),
    LanguageClickListener {
    private var binding: DialogLanguageBinding? = null
    private var adapter: LanguageAdapter? = null
    private val list = ArrayList<String>()
    private var selectedLanguage = "English"
    var listener: LanguageClickListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogLanguageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            addDataToList()
            adapter = LanguageAdapter(list, this)
            val layoutManager = LinearLayoutManager(requireContext())
            binding?.rvDoseUnit?.layoutManager = layoutManager
            binding?.rvDoseUnit?.adapter = adapter
            binding?.btnOk?.setOnClickListener { listener?.onLanguageClick(selectedLanguage) }
            binding?.btnCancel?.setOnClickListener { listener?.onLanguageClick("cancel") }
        }catch (e: Exception){

        }
    }

    private fun addDataToList(){
        list.add(getString(R.string.english))
        list.add(getString(R.string.arabic))
        list.add(getString(R.string.bengali))
        list.add(getString(R.string.dutch))
        list.add(getString(R.string.french))
        list.add(getString(R.string.german))
        list.add(getString(R.string.hindi))
        list.add(getString(R.string.hungarian))
        list.add(getString(R.string.indonesian))
        list.add(getString(R.string.italian))
        list.add(getString(R.string.japanese))
        list.add(getString(R.string.korean))
        list.add(getString(R.string.chinese))
        list.add(getString(R.string.marathi))
        list.add(getString(R.string.persian))
        list.add(getString(R.string.portuguese))
        list.add(getString(R.string.russian))
        list.add(getString(R.string.spanish))
        list.add(getString(R.string.tamil))
        list.add(getString(R.string.thai))
        list.add(getString(R.string.turkish))
        list.add(getString(R.string.urdu))
        list.add(getString(R.string.vietnamese))
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
                val screenHeight = resources.displayMetrics.heightPixels
                val desiredHeight = screenHeight / 2
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    desiredHeight
                )
                val back = ColorDrawable(Color.TRANSPARENT)
                val inset = InsetDrawable(back, 35)
                dialog.window?.setBackgroundDrawable(inset)

                // dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }catch (e:  Exception){
            e.printStackTrace()
        }

    }

    fun setLanguageClickListener(listener: LanguageClickListener?) {
        this.listener = listener
    }

    private fun getLanguageId(language: String): String {
        var lan = ""
        when (language) {
            "English" -> lan = "eng"
            "Arabic" -> lan = "ara"
            "Bengali" -> lan = "ben"
            "Chinese" -> lan = "chi"
            "Dutch" -> lan = "dut"
            "French" -> lan = "fre"
            "German" -> lan = "ger"
            "Hindi" -> lan = "hin"
            "Hungarian" -> lan = "hun"
            "Indonesian" -> lan = "ind"
            "Italian" -> lan = "ita"
            "Japanese" -> lan = "jpn"
            "Korean" -> lan = "kor"
            "Marathi" -> lan = "mar"
            "Persian" -> lan = "per"
            "Portuguese" -> lan = "por"
            "Russian" -> lan = "rus"
            "Spanish" -> lan = "spa"
            "Tamil" -> lan = "tam"
            "Thai" -> lan = "tha"
            "Turkish" -> lan = "tur"
            "Urdu" -> lan = "urd"
            "Vietnamese" -> lan = "vie"
        }
        return lan
    }

    override fun onLanguageClick(which: String?) {
        selectedLanguage = which.toString()
    }
}