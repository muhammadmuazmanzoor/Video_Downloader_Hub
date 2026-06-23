package com.video.avd.ui.player.subtitle

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.masterwok.opensubtitlesandroid.models.OpenSubtitleItem
import com.video.avd.R
import com.video.avd.databinding.DialogSubtitlesTodownloadBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AvailableSubtitlesDialog(private val currentVideoTitle: String) : DialogFragment(),
    AvailableSubtitlesAdapter.SubTitleClickListener {

    private var binding: DialogSubtitlesTodownloadBinding? = null
    private var adapter: AvailableSubtitlesAdapter? = null
    private val list: ArrayList<SubModel>?= null
    lateinit var searchResults: Array<OpenSubtitleItem>
    private var listener: AvailableSubtitlesAdapter.SubTitleClickListener? = null
    private var selectedPosition = 0
    private var selectedItem: SubModel? = null
    var subtitleList :ArrayList<SubModel> = ArrayList()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout using binding
        binding = DialogSubtitlesTodownloadBinding.inflate(LayoutInflater.from(context))

        // Create the dialog using AlertDialog.Builder
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding?.root)
        val dialog = builder.create()

        // Set up the views and listeners
        setupView()

        return dialog
    }

    private fun setupView() {
        subtitleList.clear() // Clear the list at the start

        try {
            lifecycleScope.launch(Dispatchers.IO) {
                val batchSize = searchResults.size  // Define how often you want to update the UI
                var counter = 0

                // Process each item and add to the list
                searchResults.forEach { item ->
                    try {
                        val size = convertSize(item.SubSize.toDouble())
                        subtitleList.add(SubModel(item.SubFileName, size))
                        counter++

                        // Update the UI every 'batchSize' items
                        if (counter >= batchSize) {
                            withContext(Dispatchers.Main) {
                                updateUI(subtitleList)
                            }
                            counter = 0 // Reset the counter after each batch update
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Final update to ensure all items are shown
                withContext(Dispatchers.Main) {
                    updateUI(subtitleList)
                }
            }


            // Cancel and OK button listeners
            binding?.btnCancel?.setOnClickListener { dismiss() }
            binding?.btnOk?.setOnClickListener { dismiss() }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Function to update UI based on list content
    private fun updateUI(subtitleList: List<SubModel>) {
        binding?.apply {
            if (subtitleList.isNotEmpty()) {
                adapter = AvailableSubtitlesAdapter(this@AvailableSubtitlesDialog).also {
                    it.submitList(subtitleList)
                }
                rv.layoutManager = LinearLayoutManager(requireContext())
                rv.adapter = adapter
                btnDownload.setTextColor(requireContext().getColor(R.color.green02))
                btnDownload.setOnClickListener {
                    listener?.onSubtitleClick(selectedItem, selectedPosition)
                    dismiss()
                }
            } else {
                tvUnit.text = getString(R.string.no_files_found)
                btnDownload.visibility = View.VISIBLE
                btnDownload.setTextColor(requireContext().getColor(R.color.grey01))
                btnCancel.visibility = View.VISIBLE
                btnOk.visibility = View.INVISIBLE
            }
        }
    }
    fun setSubtitleClickListener(listener: AvailableSubtitlesAdapter.SubTitleClickListener?) {
        this.listener = listener
    }

    private fun convertSize(fileSizeInBytes: Double): String {
        return try {
            val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
            if (fileSizeInMB >= 1) {
                String.format("%.2f mb", fileSizeInMB)
            } else {
                val fileSizeInKB = fileSizeInBytes / 1024
                String.format("%.2f kb", fileSizeInKB)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "100 kb"
        }
    }


    override fun onSubtitleClick(item: SubModel?, position: Int) {
        selectedItem = item
        selectedPosition = position
    }
}
