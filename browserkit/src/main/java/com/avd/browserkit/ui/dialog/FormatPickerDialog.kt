package com.avd.browserkit.ui.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avd.browserkit.R
import com.avd.browserkit.databinding.BottomSheetDownloadQualityBinding
import com.avd.browserkit.databinding.ItemDownloadQualityBinding
import com.avd.browserkit.detection.DetectedVideoInfo
import com.avd.browserkit.detection.StreamFormat
import com.avd.browserkit.detection.StreamType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

object FormatPickerDialog {
    private const val TAG = "FormatPickerDialog"

    fun show(
        manager: FragmentManager,
        info: DetectedVideoInfo,
        onSelected: (StreamFormat) -> Unit,
    ) {
        if (manager.findFragmentByTag(TAG) != null) return
        FormatPickerDialogFragment.newInstance(info).apply {
            this.onSelected = onSelected
        }.show(manager, TAG)
    }
}

@UnstableApi
class FormatPickerDialogFragment : BottomSheetDialogFragment() {
    var onSelected: ((StreamFormat) -> Unit)? = null
    private var _binding: BottomSheetDownloadQualityBinding? = null
    private val binding get() = _binding!!
    private lateinit var info: DetectedVideoInfo
    private var player: ExoPlayer? = null
    private lateinit var qualityAdapter: DownloadQualityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info = requireArguments().getParcelable(ARG_INFO) ?: DetectedVideoInfo("", "", emptyList())
    }

    override fun getTheme(): Int = R.style.Theme_BrowserKit_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetDownloadQualityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvVideoLink.text = info.pageUrl
        binding.btnCloseQuality.setOnClickListener { dismiss() }

        val items = QualityUiMapper.buildItems(info.formats, requireContext())
        qualityAdapter = DownloadQualityAdapter(items) { selected ->
            playFormat(selected.format)
        }
        val span = items.size.coerceIn(1, 3)
        binding.rvQualities.layoutManager = GridLayoutManager(requireContext(), span)
        binding.rvQualities.adapter = qualityAdapter

        binding.btnDownloadVideo.setOnClickListener {
            val selected = qualityAdapter.getSelectedFormat() ?: return@setOnClickListener
            onSelected?.invoke(selected)
            dismiss()
        }

        val initial = qualityAdapter.getSelectedFormat()
        if (initial != null) {
            playFormat(initial)
        }

        (dialog as? BottomSheetDialog)?.let { sheet ->
            sheet.setOnShowListener {
                val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                bottomSheet?.let { panel ->
                    panel.background = null
                    BottomSheetBehavior.from(panel).apply {
                        skipCollapsed = true
                        isFitToContents = false
                        state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            }
        }
    }

    private fun playFormat(format: StreamFormat) {
        val exo = player ?: ExoPlayer.Builder(requireContext()).build().also {
            player = it
            binding.playerView.player = it
        }
        exo.stop()
        exo.clearMediaItems()
        exo.setMediaSource(buildMediaSource(format))
        exo.prepare()
        exo.playWhenReady = true
    }

    private fun buildMediaSource(format: StreamFormat): MediaSource {
        val headers = info.headers + format.headers
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
        val mediaItem = MediaItem.fromUri(Uri.parse(format.url))
        return when (format.streamType) {
            StreamType.HLS_M3U8 -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_INFO = "arg_info"

        fun newInstance(info: DetectedVideoInfo): FormatPickerDialogFragment {
            return FormatPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_INFO, info)
                }
            }
        }
    }
}

private data class QualityItem(
    val format: StreamFormat,
    val resolution: String,
    val subtitle: String,
    val sizeLabel: String,
)

private object QualityUiMapper {
    private val heightRegex = Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE)

    fun buildItems(formats: List<StreamFormat>, context: android.content.Context): List<QualityItem> {
        val videoFormats = formats.filter { it.streamType != StreamType.AUDIO }
        val source = if (videoFormats.isNotEmpty()) videoFormats else formats
        return source
            .distinctBy { parseHeight(it.label) to it.label }
            .sortedByDescending { parseHeight(it.label) }
            .map { format ->
                val height = parseHeight(format.label)
                QualityItem(
                    format = format,
                    resolution = if (height > 0) "${height}p" else format.label,
                    subtitle = subtitleForHeight(height, context),
                    sizeLabel = sizeEstimate(height),
                )
            }
    }

    private fun parseHeight(label: String): Int {
        return heightRegex.find(label)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun subtitleForHeight(height: Int, context: android.content.Context): String {
        return when {
            height >= 1080 -> context.getString(R.string.bk_quality_full_hd)
            height >= 720 -> context.getString(R.string.bk_quality_hd)
            height > 0 -> context.getString(R.string.bk_quality_standard)
            else -> ""
        }
    }

    private fun sizeEstimate(height: Int): String {
        return when {
            height >= 1080 -> "~2.4 GB"
            height >= 720 -> "~450 MB"
            height >= 480 -> "~120 MB"
            height > 0 -> ""
            else -> ""
        }
    }
}

private class DownloadQualityAdapter(
    items: List<QualityItem>,
    private val onSelected: (QualityItem) -> Unit,
) : RecyclerView.Adapter<DownloadQualityAdapter.Holder>() {
    private val items: List<QualityItem> = items
    private var selectedIndex = 0

    fun getSelectedFormat(): StreamFormat? = items.getOrNull(selectedIndex)?.format

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDownloadQualityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(
        private val binding: ItemDownloadQualityBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: QualityItem, selected: Boolean) {
            binding.tvQualityResolution.text = item.resolution
            binding.tvQualitySubtitle.text = item.subtitle
            binding.tvQualitySubtitle.isVisible = item.subtitle.isNotBlank()
            binding.tvQualitySize.text = item.sizeLabel
            binding.tvQualitySize.isVisible = item.sizeLabel.isNotBlank()
            binding.ivQualitySelected.isVisible = selected
            binding.qualityCardRoot.setBackgroundResource(
                if (selected) R.drawable.bk_bg_quality_card_selected else R.drawable.bk_bg_quality_card,
            )
            binding.root.setOnClickListener {
                val previous = selectedIndex
                selectedIndex = bindingAdapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedIndex)
                onSelected(item)
            }
        }
    }
}
