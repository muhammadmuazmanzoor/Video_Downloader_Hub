package com.avd.ui.component.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.avd.data.local.room.entity.VideoFormatEntity
import com.avd.data.local.room.entity.VideoInfo
import com.avd.databinding.DownloadCandidateItemBinding
import com.avd.ui.component.dialog.CandidateFormatListener
import kotlin.math.abs


class CandidatesListRecyclerViewAdapter(
    private val downloadCandidates: VideoInfo,
    private val selectedFormat: ObservableField<Map<String, String>>,
    private val downloadDialogListener: CandidateFormatListener
) : RecyclerView.Adapter<CandidatesListRecyclerViewAdapter.CandidatesViewHolder>() {

    private var formats: List<VideoFormatEntity> = arrayListOf()

    init {
        val allFormats = downloadCandidates.formats.formats
        formats = getShortenFormats(allFormats)
    }

    class CandidatesViewHolder(val binding: DownloadCandidateItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DownloadCandidateItemBinding.inflate(inflater, parent, false)
        return CandidatesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CandidatesViewHolder, position: Int) {
        with(holder.binding) {
            val candidate = formats[position].format ?: "error"
            listener = object : CandidateFormatListener {
                override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
                    downloadDialogListener.onSelectFormat(videoInfo, format)
                    notifyDataSetChanged() // Consider using more efficient update methods
                }
            }
            val selected = selectedFormat.get()?.get(downloadCandidates.id)
            this.videoInfo = downloadCandidates
            this.downloadCandidate = candidate
            this.isCandidateSelected = candidate == selected
            this.tvTitle.text = getShortOfFormat(candidate)
            val fileSizeText = when {
                formats[position].fileSize > 0 -> formatSize(formats[position].fileSize)
                formats[position].fileSizeApproximate > 0 -> formatSize(formats[position].fileSizeApproximate)
                formats[position].tbr > 0 -> {
                    // Estimate file size if `tbr` (bit rate) is available
                    val estimatedDurationInSeconds = downloadCandidates.duration // Replace with actual duration if available
                    val estimatedSizeInBytes = abs((formats[position].tbr * 1000) * estimatedDurationInSeconds) / 8 // Convert kbps to bytes
                    formatSize(estimatedSizeInBytes.toLong())
                }
                else -> "Unknown Size"
            }
            this.tvSize.text= fileSizeText
            this.executePendingBindings()
        }
    }

    override fun getItemCount(): Int = formats.size

    fun setData(formats: List<VideoFormatEntity>) {
        this.formats = formats
        notifyDataSetChanged()
    }

    private fun makeVideoFormatHumanReadable(input: String): String {
        return input.replace(Regex("-\\w+"), "")
    }

    private fun getShortenFormats(allFormats: List<VideoFormatEntity>): List<VideoFormatEntity> {
        val formatsMap = mutableMapOf<String, VideoFormatEntity>()
        for (format in allFormats) {
            formatsMap[getShortOfFormat(format.format)] = format
        }

        formatsMap.remove("")

        formatsMap.toSortedMap()

        return formatsMap.toSortedMap().values.toList()
    }

    private fun getShortOfFormat(format: String?): String {
        val formattedFormat = makeVideoFormatHumanReadable(format ?: "error")
        if (formattedFormat != "error") {
            return if (formattedFormat.contains("x")) {
                "${formattedFormat.split("x").last().replace(Regex("\\D"), "")}P"
            } else if (!formattedFormat.contains("x") && !formattedFormat.contains("audio only")
                && formattedFormat.contains("-")
            ) {
                val leftSide = formattedFormat.split("-").first()
                if (leftSide.lowercase().contains("hd") || leftSide.contains("sd")) {
                    return leftSide.trim()
                }
                val rightSide = formattedFormat.split("-").last()
                rightSide.replace("p", "P").trim()
            } else if (formattedFormat.contains("audio only")) {
                ""
            } else {
                formattedFormat
            }
        }

        return "Error"
    }


    // Helper function to format file size in KB, MB, etc.
    @SuppressLint("DefaultLocale")
    private fun formatSize(sizeInBytes: Long): String {
        val kiloBytes = sizeInBytes / 1024.0
        val megaBytes = kiloBytes / 1024.0
        return if (megaBytes < 1) {
            "${String.format("%.2f", kiloBytes)} KB"
        } else {
            "${String.format("%.2f", megaBytes)} MB"
        }
    }
}