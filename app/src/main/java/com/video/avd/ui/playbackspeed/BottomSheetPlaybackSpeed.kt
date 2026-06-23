package com.video.avd.ui.playbackspeed

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.IndicatorType
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import com.warkiz.widget.TickMarkType
import com.video.avd.R
import com.video.avd.databinding.FragmentPlaybackSpeedBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.annotation.Nullable

@AndroidEntryPoint
class BottomSheetPlaybackSpeed : BottomSheetDialogFragment(), PlayBackSpeedButtonListener {

    private var isFullScreen = false

    companion object {
        var playBackSpeed = 1.0f
    }

    interface PlaybackSpeedListener {
        fun onPlaybackSpeedChange(playbackSpeedValue: Float)
    }

    private var mActivity: FragmentActivity? = null
    private lateinit var binding: FragmentPlaybackSpeedBinding
    private lateinit var playbackSpeedListener: PlaybackSpeedListener
    private lateinit var adapter: AdapterPlaybackSpeedSelection
    private lateinit var seekBar: IndicatorSeekBar

    private val defaultSpeed = 1.0f
    private val speedInterval = 0.05f

    private var speedList = mutableListOf(
        ModelPlayBackSpeed(0, 0.5f, "0.5x"),
        ModelPlayBackSpeed(1, 1.0f, "1.0x"),
        ModelPlayBackSpeed(2, 1.5f, "1.5x"),
        ModelPlayBackSpeed(3, 2.0f, "2.0x"),
        ModelPlayBackSpeed(4, 2.5f, "2.5x"),
        ModelPlayBackSpeed(5, 3.0f, "3.0x"),
        ModelPlayBackSpeed(6, 3.5f, "3.5x"),
        ModelPlayBackSpeed(7, 4.0f, "4.0x"),
    )

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
        val bundle = this.arguments
        isFullScreen = bundle?.getBoolean("isFullScreen") ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaybackSpeedBinding.inflate(inflater, container, false)
        binding.bgh.setOnClickListener {

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            setUpRecyclerview()
            setButtonClickListeners()
            setupSeekbar(activity)
        }

    }

    private fun setupSeekbar(activity: FragmentActivity) {
        try {
            seekBar = IndicatorSeekBar
                .with(activity)
                .max(speedList.last().speed)
                .min(speedList.first().speed)
                //.progress(speedList.find { it.speed == playBackSpeed }?.speed ?: defaultSpeed)
                .progress(playBackSpeed)
                .trackProgressColor(ContextCompat.getColor(activity, R.color.dark_mode_green))
                .trackBackgroundColor(ContextCompat.getColor(activity, R.color.trackbgColor))
                .thumbColor(ContextCompat.getColor(activity, R.color.dark_mode_green))
                .showIndicatorType(IndicatorType.NONE)
                .tickCount(32)
                .tickTextsArray(speedList.map { it.speedText }.toTypedArray())
                .tickMarksColor(ContextCompat.getColor(activity, R.color.white))
                .tickTextsColor(ContextCompat.getColor(activity, R.color.text_color_1))
                .tickTextsSize(0)
                .tickMarksSize(0)
                .showTickMarksType(TickMarkType.OVAL)
                .showTickTexts(true)
                .build()

            binding.seekbarLayout.addView(seekBar)

            seekBar.onSeekChangeListener = object : OnSeekChangeListener {
                override fun onSeeking(seekParams: SeekParams) {

                }

                override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
                override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                    updateSyncUI(seekBar.progressFloat)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setButtonClickListeners() {
        if (playBackSpeed == defaultSpeed) binding.textPlayBackSpeed.text =
            "$defaultSpeed" + "x" else binding.textPlayBackSpeed.text = "$playBackSpeed" + "x"

        binding.minusPlayBackSpeed.setOnClickListener {
            if (playBackSpeed > speedList.first().speed) {
                playBackSpeed -= speedInterval
                updateSyncUI(playBackSpeed)
            }
        }
        binding.plusPlayBackSpeed.setOnClickListener {
            if (playBackSpeed < speedList.last().speed) {
                playBackSpeed += speedInterval
                updateSyncUI(playBackSpeed)
            }
        }

        binding.resetPlayBackSpeed.setOnClickListener {
            updateSyncUI(defaultSpeed)
        }

        binding.playBackArrow.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun updateSyncUI(speed: Float) {
        try {
            val roundedSpeed = String.format(Locale.US,"%.2f", speed).toFloat()
            playBackSpeed = roundedSpeed
            binding.textPlayBackSpeed.text = "$playBackSpeed" + "x"
            val index = speedList.indexOfFirst { it.speed == playBackSpeed }
            adapter.selectMode(index)
            seekBar.setProgress(playBackSpeed)
            playbackSpeedListener.onPlaybackSpeedChange(playBackSpeed)
        }catch (e:Exception){
            e.printStackTrace()
        }catch (e:NumberFormatException){
            e.printStackTrace()
        }
    }

    private fun setUpRecyclerview() {
        adapter = AdapterPlaybackSpeedSelection(requireActivity(), this)
        binding.rvPlaybackSpeed.adapter = adapter
        subscribeUi(adapter)
    }

    private fun subscribeUi(adapter: AdapterPlaybackSpeedSelection) {
        adapter.submitList(speedList)
    }

    override fun onPlayBackButtonClick(position: Int, modelPlayBackSpeed: ModelPlayBackSpeed) {
        adapter.selectMode(position)
        playbackSpeedListener.onPlaybackSpeedChange(modelPlayBackSpeed.speed)
        playBackSpeed = modelPlayBackSpeed.speed
        binding.textPlayBackSpeed.text = modelPlayBackSpeed.speedText
        seekBar.setProgress(playBackSpeed)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
        if (context is PlaybackSpeedListener) {
            playbackSpeedListener = context
        } else {
            throw RuntimeException("$context must implement PlaybackSpeedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }


//    override fun onResume() {
//        super.onResume()
//        if (isFullScreen) {
//            dialog?.let { //in landscape mode set height to full screen
//                val sheet = it as BottomSheetDialog
//                sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
//            }
//            //set width to 50 percent of screen and align to Right
//            dialog?.window?.setLayout(dpToPx(), -1)
//            dialog?.window?.setGravity(Gravity.END)
//        }
//    }

    private fun dpToPx(): Int {
        val density: Float = Resources.getSystem().displayMetrics.density
        return (450 * density + 0.5f).toInt()
    }
}