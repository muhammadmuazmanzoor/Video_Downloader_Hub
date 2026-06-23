package com.video.avd.ui.equalizer.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.db.chart.model.LineSet
import com.db.chart.view.AxisController
import com.db.chart.view.ChartView
import com.db.chart.view.LineChartView
import com.video.avd.R
import com.video.avd.ui.equalizer.EqualizerModel
import com.video.avd.ui.equalizer.Settings
import com.video.avd.utils.AppUtils
import com.video.avd.utils.GlobalValues
import java.lang.NullPointerException

/**
 * A simple [Fragment] subclass.
 */
class EqualizerFragmentVideo : Fragment() {

    lateinit var equalizerSwitch: SwitchCompat
    private lateinit var chart: LineChartView
    lateinit var backBtn: ImageView
    var y = 0
    var fragTitle: TextView? = null
    var mLinearLayout: LinearLayout? = null
    var seekBarFinal = arrayOfNulls<SeekBar>(5)
    lateinit var bassController: AnalogControllerVideo
    lateinit var reverbController: AnalogControllerVideo
    var recyclerViewPreset: RecyclerView? = null
    var equalizerBlocker: FrameLayout? = null
    var ctx: Context? = null
    var dataset: LineSet? = null
    var paint: Paint? = null
    lateinit var points: FloatArray
    var numberOfFrequencyBands: Short = 0
    private var audioSesionId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.isEditing = true
        try {
            if (arguments != null && arguments?.containsKey(ARG_AUDIO_SESSIOIN_ID) == true) {
                audioSesionId = arguments?.getInt(ARG_AUDIO_SESSIOIN_ID) ?: 0
            }
            if (Settings.equalizerModel == null) {
                Settings.equalizerModel = EqualizerModel()
                Settings.equalizerModel?.reverbPreset = PresetReverb.PRESET_NONE
                Settings.equalizerModel?.bassStrength = (1000 / 19).toShort()
            }
            mEqualizer = Equalizer(0, audioSesionId)
            bassBoost = BassBoost(0, audioSesionId)
            bassBoost?.enabled = Settings.isEqualizerEnabled
            val bassBoostSettingTemp = bassBoost?.properties
            val bassBoostSetting = BassBoost.Settings(bassBoostSettingTemp.toString())
            bassBoostSetting.strength = Settings.equalizerModel?.bassStrength!!
            bassBoost?.properties = bassBoostSetting
            presetReverb = PresetReverb(0, audioSesionId)
            presetReverb?.preset = Settings.equalizerModel?.reverbPreset!!
            presetReverb?.enabled = Settings.isEqualizerEnabled
            mEqualizer?.enabled = Settings.isEqualizerEnabled
            if (Settings.presetPos == 0) {
                for (bandIdx in 0 until mEqualizer?.numberOfBands!!) {
                    mEqualizer?.setBandLevel(
                        bandIdx.toShort(),
                        Settings.seekbarpos[bandIdx].toShort()
                    )
                }
            } else {
                mEqualizer?.usePreset(Settings.presetPos.toShort())
            }
        } catch (e: Exception) {
           // Toast.makeText(requireActivity(), "Audio Error! Unable to update Equalizer", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_equalizer_video, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated_EqualizerFragmentVideo", "EqualizerFragmentVideo")
        backBtn = view.findViewById(R.id.equalizer_back_btn)
        val textColor = context?.resources?.getColor(R.color.gSelector)
        backBtn.setOnClickListener(View.OnClickListener {
            if (activity != null) {
                requireActivity().onBackPressed()
            }
        })
        fragTitle = view.findViewById(R.id.equalizer_fragment_title)
        equalizerSwitch = view.findViewById(R.id.equalizer_switch)
        try {
            GlobalValues.disableVideoEqualizer.observe(viewLifecycleOwner){
                equalizerSwitch.isChecked = it

            }
            equalizerSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                try {
                    mEqualizer?.enabled = isChecked
                    bassBoost?.enabled = isChecked
                    presetReverb?.enabled = isChecked
                    Log.d("stateess","$isChecked")
                    GlobalValues.disableVideoEqualizer.postValue(isChecked)
                    Settings.isEqualizerEnabled = isChecked
                    Settings.equalizerModel?.isEqualizerEnabled = isChecked
                }catch (e : NullPointerException){
                    e.printStackTrace()
                }catch (e: Exception){
                    e.printStackTrace()
                }
            })
            recyclerViewPreset = view.findViewById(R.id.recyclerPreset)
            equalizerBlocker = view.findViewById(R.id.equalizerBlocker)
            chart = view.findViewById(R.id.lineChart)
            paint = Paint()
            dataset = LineSet()
            bassController = view.findViewById(R.id.controllerBass)
            reverbController = view.findViewById(R.id.controller3D)
            bassController.label = "Bass"
            reverbController.label = "3D"
            if (textColor != null) {
                bassController.circlePaint2?.color = textColor
                bassController.linePaint?.color = textColor
                // Use postInvalidateOnAnimation for smoother updates that sync with VSYNC
                bassController.postInvalidateOnAnimation()
                reverbController.circlePaint2?.color = textColor
                bassController.linePaint?.color = textColor
            }
            // Use postInvalidateOnAnimation for smoother updates that sync with VSYNC
            reverbController.postInvalidateOnAnimation()
            if (!Settings.isEqualizerReloaded) {
                var x = 0
                if (bassBoost != null) {
                    try {
                        x = (bassBoost?.roundedStrength?.times(19) ?: 0) / 1000
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (presetReverb != null) {
                    try {
                        y = (presetReverb?.preset?.times(19) ?: 0) / 6
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (x == 0) {
                    bassController.progress = 1
                } else {
                    bassController.progress = x
                }
                if (y == 0) {
                    reverbController.progress = 1
                } else {
                    reverbController.progress = y
                }
            } else {
                val x = Settings.bassStrength * 19 / 1000
                y = Settings.reverbPreset * 19 / 6
                if (x == 0) {
                    bassController.progress = 1
                } else {
                    bassController.progress = x
                }
                if (y == 0) {
                    reverbController.progress = 1
                } else {
                    reverbController.progress = y
                }
            }
            bassController.setOnProgressChangedListener(object :
                AnalogControllerVideo.onProgressChangedListenerVideo {
                override fun onProgressChanged(progress: Int) {
                    Settings.bassStrength = (1000f / 19 * progress).toInt().toShort()
                    try {
                        bassBoost?.setStrength(Settings.bassStrength)
                        Settings.equalizerModel?.bassStrength = Settings.bassStrength
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            reverbController.setOnProgressChangedListener(object :
                AnalogControllerVideo.onProgressChangedListenerVideo {
                override fun onProgressChanged(progress: Int) {
                    Settings.reverbPreset = (progress * 6 / 19).toShort()
                    Settings.equalizerModel?.reverbPreset = Settings.reverbPreset
                    try {
                        presetReverb?.preset = Settings.reverbPreset
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    y = progress
                }
            })
            mLinearLayout = view.findViewById(R.id.equalizerContainer)
            val equalizerHeading = TextView(context)
            equalizerHeading.setText(R.string.eq)
            equalizerHeading.textSize = 20f
            equalizerHeading.gravity = Gravity.CENTER_HORIZONTAL
            numberOfFrequencyBands = 5
            points = FloatArray(numberOfFrequencyBands.toInt())
            val lowerEqualizerBandLevel = mEqualizer?.bandLevelRange?.get(0)
            val upperEqualizerBandLevel = mEqualizer?.bandLevelRange?.get(1)
            for (i in 0 until numberOfFrequencyBands) {
                val equalizerBandIndex = i.toShort()
                val frequencyHeaderTextView = TextView(context)
                frequencyHeaderTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                frequencyHeaderTextView.gravity = Gravity.CENTER_HORIZONTAL
                frequencyHeaderTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                frequencyHeaderTextView.text =
                    (mEqualizer?.getCenterFreq(equalizerBandIndex)?.div(1000)).toString() + "Hz"
                val seekBarRowLayout = LinearLayout(context)
                seekBarRowLayout.orientation = LinearLayout.VERTICAL
                val lowerEqualizerBandLevelTextView = TextView(context)
                lowerEqualizerBandLevelTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                lowerEqualizerBandLevelTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                if (lowerEqualizerBandLevel != null) {
                    lowerEqualizerBandLevelTextView.text =
                        (lowerEqualizerBandLevel / 100).toString() + "dB"
                }
                val upperEqualizerBandLevelTextView = TextView(context)
                lowerEqualizerBandLevelTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                upperEqualizerBandLevelTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                if (upperEqualizerBandLevel != null) {
                    upperEqualizerBandLevelTextView.text =
                        (upperEqualizerBandLevel / 100).toString() + "dB"
                }
                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams.weight = 1f
                var seekBar = SeekBar(context)
                var textView = TextView(context)
                when (i) {
                    0 -> {
                        seekBar = view.findViewById(R.id.seekBar1)
                        textView = view.findViewById(R.id.textView1)
                    }

                    1 -> {
                        seekBar = view.findViewById(R.id.seekBar2)
                        textView = view.findViewById(R.id.textView2)
                    }

                    2 -> {
                        seekBar = view.findViewById(R.id.seekBar3)
                        textView = view.findViewById(R.id.textView3)
                    }

                    3 -> {
                        seekBar = view.findViewById(R.id.seekBar4)
                        textView = view.findViewById(R.id.textView4)
                    }

                    4 -> {
                        seekBar = view.findViewById(R.id.seekBar5)
                        textView = view.findViewById(R.id.textView5)
                    }
                }
                seekBarFinal[i] = seekBar
                seekBar.progressDrawable.colorFilter =
                    textColor?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
                seekBar.progressDrawable.colorFilter =
                    textColor?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
//            seekBar.setSecondaryProgressTintList(ColorStateList.valueOf(progressSeekbarColor));
                seekBar.thumb.colorFilter =
                    textColor?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
                seekBar.id = i
                //            seekBar.setLayoutParams(layoutParams);
                if (upperEqualizerBandLevel != null && lowerEqualizerBandLevel != null) {
                    seekBar.max = upperEqualizerBandLevel - lowerEqualizerBandLevel
                }
                textView.text = frequencyHeaderTextView.text
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
                textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                if (Settings.isEqualizerReloaded && lowerEqualizerBandLevel != null) {
                    points[i] = (Settings.seekbarpos[i] - lowerEqualizerBandLevel).toFloat()
                    dataset?.addPoint(frequencyHeaderTextView.text.toString(), points[i])
                    seekBar.progress = Settings.seekbarpos[i] - lowerEqualizerBandLevel
                } else {
                    if (lowerEqualizerBandLevel != null){
                        if ((mEqualizer?.getBandLevel(equalizerBandIndex)
                                ?.minus(lowerEqualizerBandLevel)) != null){
                            points[i] =
                                (mEqualizer?.getBandLevel(equalizerBandIndex)
                                    ?.minus(lowerEqualizerBandLevel))!!.toFloat()
                            dataset?.addPoint(frequencyHeaderTextView.text.toString(), points[i])
                            seekBar.progress =
                                mEqualizer?.getBandLevel(equalizerBandIndex)!! - lowerEqualizerBandLevel
                            Settings.seekbarpos[i] = mEqualizer!!.getBandLevel(equalizerBandIndex).toInt()
                            Settings.isEqualizerReloaded = true
                        }
                    }
                }
                seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (lowerEqualizerBandLevel != null){
                            try {
                                mEqualizer?.setBandLevel(
                                    equalizerBandIndex,
                                    (progress + lowerEqualizerBandLevel).toShort()
                                )
                                points[seekBar.id] =
                                    (mEqualizer!!.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()
                                Settings.seekbarpos[seekBar.id] = progress + lowerEqualizerBandLevel
                                Settings.equalizerModel!!.seekbarpos[seekBar.id] =
                                    progress + lowerEqualizerBandLevel
                                dataset!!.updateValues(points)
                                chart.notifyDataUpdate()
                            }catch (e: java.lang.Exception){
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        Settings.presetPos = 0
                        Settings.equalizerModel?.presetPos = 0
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            }
            equalizeSound()
            paint?.color = Color.parseColor("#555555")
            paint?.strokeWidth = (1.10 * Settings.ratio).toFloat()
            if (textColor != null) {
                dataset?.color = textColor
            }
            dataset?.isSmooth = true
            dataset?.thickness = 5f
            chart.setXAxis(false)
            chart.setYAxis(false)
            chart.setYLabels(AxisController.LabelPosition.NONE)
            chart.setXLabels(AxisController.LabelPosition.NONE)
            chart.setGrid(ChartView.GridType.NONE, 7, 10, paint)
            chart.setAxisBorderValues(-300, 3300)
            chart.addData(dataset)
            chart.show()
            val mEndButton = Button(context)
            if (textColor != null) {
                mEndButton.setBackgroundColor(textColor)
            }
            mEndButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_primary))
        } catch (e: Exception) {
           // Toast.makeText(requireActivity(), "Audio Error! Unable to update Equalizer", Toast.LENGTH_SHORT).show()
            Log.d("mTag", "error is: "+e.message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Settings.isEditing = false
    }

    class Builder {
        private var id = -1
        fun setAudioSessionId(id: Int): Builder {
            this.id = id
            return this
        }

        fun setAccentColor(color: Int): Builder {
            themeColor = color
            return this
        }

        fun setShowBackButton(show: Boolean): Builder {
            showBackButton = show
            return this
        }

        fun build(): EqualizerFragmentVideo {
            return newInstance(id)
        }
    }

    inner class PresetAdapter(context: Context?, private val itemList: List<String>) :
        RecyclerView.Adapter<PresetAdapter.PresetViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_preset_equalizer_video, parent, false)
            return PresetViewHolder(view)
        }

        @SuppressLint("SuspiciousIndentation")
        override fun onBindViewHolder(holder: PresetViewHolder, mPosition: Int) {
            val text = itemList[mPosition]
            holder.textView.text = text
            val position = holder.adapterPosition
            if (selectedPresetPosition == position) {
                holder.textView.background =
                    resources.getDrawable(R.drawable.bg_item_preset_highligted)
            } else {
                holder.textView.background = resources.getDrawable(R.drawable.bg_item_preset)
            }
            holder.itemView.setOnClickListener { view: View? ->
                try {
                    selectedPresetPosition = holder.adapterPosition
                    if (position != 0) {
                        mEqualizer?.usePreset((position - 1).toShort())
                        Settings.presetPos = position
                        val numberOfFreqBands: Short = 5
                        val lowerEqualizerBandLevel = mEqualizer?.bandLevelRange?.get(0)
                        if (lowerEqualizerBandLevel != null)
                        for (i in 0 until numberOfFreqBands) {
                            seekBarFinal[i]?.progress =
                                mEqualizer!!.getBandLevel(i.toShort()) - lowerEqualizerBandLevel
                            points[i] =
                                (mEqualizer!!.getBandLevel(i.toShort()) - lowerEqualizerBandLevel).toFloat()
                            Settings.seekbarpos[i] = mEqualizer!!.getBandLevel(
                                i.toShort()
                            ).toInt()
                            Settings.equalizerModel!!.seekbarpos[i] = mEqualizer!!.getBandLevel(
                                i.toShort()
                            ).toInt()
                        }
                        dataset?.updateValues(points)
                        chart.notifyDataUpdate()
                    }
                    notifyDataSetChanged()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Error while updating Equalizer", Toast.LENGTH_SHORT).show()
                }
                Settings.equalizerModel?.presetPos = position
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        inner class PresetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var textView: TextView

            init {
                textView = itemView.findViewById(R.id.textPreset)
            }
        }
    }

    fun equalizeSound() {
        val equalizerPresetNames = ArrayList<String>()
        val presetAdapter = PresetAdapter(ctx, equalizerPresetNames)
        equalizerPresetNames.add("Custom")
        for (i in 0 until mEqualizer?.numberOfPresets!!) {
            mEqualizer?.getPresetName(i.toShort())?.let { equalizerPresetNames.add(it) }
        }
        recyclerViewPreset?.adapter = presetAdapter
        recyclerViewPreset?.scrollToPosition(selectedPresetPosition)
    }

    companion object {
        var mEqualizer: Equalizer? = null
        var bassBoost: BassBoost? = null
        var presetReverb: PresetReverb? = null
        const val ARG_AUDIO_SESSIOIN_ID = "audio_session_idd"
        var themeColor = Color.parseColor("#FF610A")
        var progressSeekbarColor = Color.parseColor("#B8B8B8")
        var showBackButton = true
        var selectedPresetPosition = 0
        fun SetSessionID() {}
        fun newInstance(audioSessionId: Int): EqualizerFragmentVideo {
            val args = Bundle()
            args.putInt(ARG_AUDIO_SESSIOIN_ID, audioSessionId)
            val fragment = EqualizerFragmentVideo()
            fragment.arguments = args
            return fragment
        }

        fun newBuilder(): Builder {
            return Builder()
        }
    }
}


