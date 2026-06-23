package com.video.avd.ui.player.subtitle

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.masterwok.opensubtitlesandroid.OpenSubtitlesUrlBuilder
import com.masterwok.opensubtitlesandroid.models.OpenSubtitleItem
import com.masterwok.opensubtitlesandroid.services.OpenSubtitlesService
import com.masterwok.opensubtitlesandroid.services.OpenSubtitlesService.Companion.TemporaryUserAgent
import com.video.avd.R
import com.video.avd.databinding.DialogSearchSubtitleBinding
import com.video.avd.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SearchSubtitleDialog : DialogFragment() {
    private var binding: DialogSearchSubtitleBinding? = null

    private var downloadListener: DownloadListener? = null
    private var videoTitle = ""



    // Use this static method to create new instances of this fragment using the provided parameters.
    companion object {
        fun newInstance(currentVideoTitle: String): SearchSubtitleDialog {
            val fragment = SearchSubtitleDialog()
            val args = Bundle()
            Log.e("SearchDialog", "Instance Created SearchDialog")
            args.putString("currentVideoTitle", currentVideoTitle)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogSearchSubtitleBinding.inflate(inflater, container, false)
        videoTitle = arguments?.getString("currentVideoTitle", "") ?: ""
        Log.e("SearchDialog", "onCreateView SearchDialog")
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            Log.e("SearchDialog", "ViewCreated SearchDialog")
            binding?.edsearch?.setText(videoTitle)
            binding?.edsearch?.requestFocus()
            binding?.edsearch?.text?.length?.let { binding?.edsearch?.setSelection(it) } // set cursor on end
            clickListeners()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

/*    private fun download(query: String, languageId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadLocation =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val subtitleService = OpenSubtitlesService()
                val loaderDialog = LoaderDialog()
                withContext(Dispatchers.Main) {
                    dialog?.hide()
                    loaderDialog.show(parentFragmentManager, "")
                }
                // Build the URL with both query and languageId
                val url = OpenSubtitlesUrlBuilder()
                    .query(query) // Use the provided query
                    .subLanguageId(languageId) // Use the provided languageId
                    .build()
                val searchResults: Array<OpenSubtitleItem> = try {
                    subtitleService.search(
                        TemporaryUserAgent,
                        url
                    )
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), ex.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                    Log.e("LOGG", "Subtitle search failed.", ex)
                    return@launch
                }

                val availableSubtitlesDialog = AvailableSubtitlesDialog(videoTitle)
                availableSubtitlesDialog.searchResults = searchResults
                availableSubtitlesDialog.isCancelable = false
                withContext(Dispatchers.Main) {
                    loaderDialog.dismiss()
                    availableSubtitlesDialog.show(parentFragmentManager, "")
                    availableSubtitlesDialog.setSubtitleClickListener(object :
                        AvailableSubtitlesAdapter.SubTitleClickListener {
                        override fun onSubtitleClick(item: SubModel?, position: Int) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val firstSubtitleItem: OpenSubtitleItem = searchResults[position]
                                val outputFile = File(
                                    downloadLocation,
                                    firstSubtitleItem?.SubFileName ?: ""
                                )
                                if (requireContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1111)
                                } else {
                                    // Permission is already granted, continue with download
                                    try {
                                        if (firstSubtitleItem != null) {
                                            availableSubtitlesDialog.dismiss()
                                            LoaderDialog.isDownloading = true
                                            loaderDialog.show(parentFragmentManager, "")
                                            subtitleService.downloadSubtitle(
                                                requireContext(),
                                                firstSubtitleItem,
                                                Uri.fromFile(outputFile)
                                            )
                                            loaderDialog.dismiss()
                                            downloadListener?.isDownloaded(true, outputFile.toString())
                                            dismiss()
                                        }
                                    } catch (ex: Exception) {
                                        loaderDialog.dismiss()
                                        dismiss()
                                        downloadListener?.isDownloaded(true, outputFile.toString())
                                        Log.e("LOGG", "Failed to download subtitles", ex)
                                    }
                                }

                            }
                        }
                    })
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }*/
private fun download(query: String, languageId: String) {
    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        try {
            val downloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subtitleService = OpenSubtitlesService()
            val loaderDialog = LoaderDialog()

            withContext(Dispatchers.Main) {
                dialog?.hide()
                loaderDialog.show(parentFragmentManager, "")
            }

            // Normalize the query and languageId
            val sanitizedQuery = query.trim().lowercase()
            val sanitizedLanguageId = languageId.trim().lowercase()

            // Build the URL
            val url = OpenSubtitlesUrlBuilder()
                .query(sanitizedQuery)
                .subLanguageId(sanitizedLanguageId)
                .build()

            // Search for subtitles
            val searchResults: Array<OpenSubtitleItem> = try {
                subtitleService.search(TemporaryUserAgent, url)
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), ex.message.toString(), Toast.LENGTH_SHORT).show()
                }
                Log.e("LOGG", "Subtitle search failed.", ex)
                return@launch
            }

            // Show available subtitles dialog
            val availableSubtitlesDialog = AvailableSubtitlesDialog(videoTitle)
            availableSubtitlesDialog.searchResults = searchResults
            availableSubtitlesDialog.isCancelable = false

            withContext(Dispatchers.Main) {
                loaderDialog.dismiss()
                availableSubtitlesDialog.show(parentFragmentManager, "")
                availableSubtitlesDialog.setSubtitleClickListener(object :
                    AvailableSubtitlesAdapter.SubTitleClickListener {
                    override fun onSubtitleClick(item: SubModel?, position: Int) {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val firstSubtitleItem = searchResults[position]
                                val outputFile = File(downloadLocation, firstSubtitleItem.SubFileName ?: "")

                                // Check storage permission
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                                    requireContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    downloadSubtitle(firstSubtitleItem, outputFile, loaderDialog)
                                } else {
                                    requestPermissions(
                                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1111
                                    )
                                }
                            } catch (ex: Exception) {
                                Log.e("LOGG", "Failed to download subtitles", ex)
                                withContext(Dispatchers.Main) {
                                    loaderDialog.dismiss()
                                    downloadListener?.isDownloaded(false, null)
                                    dismissAllowingStateLoss()
                                }
                            }
                        }
                    }
                })
            }

        } catch (e: Exception) {
            Log.e("LOGG", "Unexpected error in download", e)
        }
    }
}

    // Helper function to download subtitles
    private suspend fun downloadSubtitle(
        subtitleItem: OpenSubtitleItem,
        outputFile: File,
        loaderDialog: LoaderDialog
    ) {
        withContext(Dispatchers.Main) {
            LoaderDialog.isDownloading = true
            loaderDialog.show(parentFragmentManager, "")
        }

        try {
            OpenSubtitlesService().downloadSubtitle(requireContext(), subtitleItem, Uri.fromFile(outputFile))
            withContext(Dispatchers.Main) {
                loaderDialog.dismiss()
                downloadListener?.isDownloaded(true, outputFile.toString())
                dismissAllDialogs(requireContext())
            }
        } catch (ex: Exception) {
            Log.e("LOGG", "Subtitle download failed", ex)
            withContext(Dispatchers.Main) {
                loaderDialog.dismiss()
                downloadListener?.isDownloaded(false, null)
                dismissAllDialogs(requireContext())
            }
        }
    }
    private fun dismissAllDialogs(context: Context) {
        try {
            // Ensure the context is a FragmentActivity to access the FragmentManager
            if (context is FragmentActivity) {
                val fragmentManager = context.supportFragmentManager

                // Iterate through all fragments
                for (fragment in fragmentManager.fragments) {
                    if (fragment is DialogFragment && fragment.isVisible()) {
                        (fragment as DialogFragment).dismissAllowingStateLoss()
                    }
                }
                Log.e("SearchDialog", "All visible dialogs dismissed")
            }
        } catch (e: java.lang.Exception) {
            Log.e("SearchDialog", "Error while dismissing dialogs: " + e.message)
            e.printStackTrace()
        }
    }
    private fun clickListeners() {
        binding?.tvLanguage?.setOnClickListener { view ->
            dialog?.hide()
            val dg = LanguageDialog()
            dg.isCancelable = false
            dg.show(parentFragmentManager, "tag")

            dg.setLanguageClickListener(object : LanguageAdapter.LanguageClickListener {
                override fun onLanguageClick(which: String?) {
                    dg.dismiss()
                    dialog!!.show()
                    if (which != "cancel") binding?.tvLanguage?.text = which
                }

            })
        }
        binding?.clearText?.setOnClickListener { view ->
            binding?.edsearch?.setText("")
            binding?.edsearch?.requestFocus()
        }
        try {
            if (binding?.edsearch?.text?.isNullOrEmpty()==true){
                binding?.btnSearch?.setTextColor(Color.GRAY)
            }
            binding?.edsearch?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.isNullOrEmpty()) {
                        // Change the text color of another TextView
                        binding?.btnSearch?.setTextColor(Color.GRAY)
                    } else {
                        // Set the text color of the TextView back to the default
                        binding?.btnSearch?.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Check if the EditText is empty
                    if (s.isNullOrEmpty()) {
                        // Change the text color of another TextView
                        binding?.btnSearch?.setTextColor(Color.GRAY)
                    } else {
                        // Set the text color of the TextView back to the default
                        binding?.btnSearch?.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding?.btnSearch?.setOnClickListener { view ->
            if(NetworkUtils.isOnline(requireContext())){
                if (binding?.edsearch?.text.toString().isEmpty()) {
                    return@setOnClickListener
                }
                val query = binding?.edsearch?.text.toString().trim()
                videoTitle=query
                val language = binding?.tvLanguage?.text.toString().trim()
                val languageId = getLanguageId(language)
                download(query, languageId)
            }
            else{
                Toast.makeText(requireContext(), "Please connect to internet first!", Toast.LENGTH_SHORT).show()
            }
        }
        binding?.btnCancel?.setOnClickListener { view ->
            dismissAllowingStateLoss() }
        binding?.tvLink?.setOnClickListener { view ->
            val url =
                "https://www.opensubtitles.org/en/search/subs" // Replace with the URL you want to open
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
            if (requestCode == 1111) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with download
                    Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }

        var permissionWasDenied = false
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionWasDenied = true
                break
            }
        }
        if (permissionWasDenied) {
            throw RuntimeException("WRITE_EXTERNAL_STORAGE permission must be granted to run demo.")
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /*
    * for other language to get languageID, VISIT
    * https://www.opensubtitles.org/addons/export_languages.php
    * */
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

    interface DownloadListener {
        fun isDownloaded(isDownloaded: Boolean?, filePath: String?)
    }

    fun setDownloadedListener(downloadedListener: DownloadListener?) {
        downloadListener = downloadedListener
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
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val back = ColorDrawable(Color.TRANSPARENT)
                val inset = InsetDrawable(back, 35)
                dialog.window?.setBackgroundDrawable(inset)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onDetach() {
        super.onDetach()
        dismissAllowingStateLoss()
        Log.e("SearchDialog", "Detached SearchDialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAllowingStateLoss()
        Log.e("SearchDialog", "Destroyed SearchDialog")
    }

    override fun onDestroyView() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = requireActivity().currentFocus ?: requireActivity().window?.decorView ?: View(requireContext())

        view.clearFocus()
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // Get the InputMethodManager
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

        // Get the currently focused view or fallback to decorView
        val view = requireActivity().currentFocus ?: requireActivity().window?.decorView ?: View(requireContext())

        // Clear focus BEFORE hiding the keyboard to ensure it doesn't reopen
        view.clearFocus()

        // Hide the keyboard immediately before the dialog starts dismissing
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        // Delay dismiss slightly to ensure keyboard fully hides first
        Handler(Looper.getMainLooper()).postDelayed({
            super.onDismiss(dialog)
        }, 50) // Small delay allows the keyboard to fully close before dismissing the dialog
    }


}