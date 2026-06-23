package com.video.avd.ui.player.settings

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.video.avd.R
import com.video.avd.databinding.FragmentVideoSettingBinding


class VideoSettingFragment : Fragment() {

    lateinit var binding : FragmentVideoSettingBinding

    private var mActivity: FragmentActivity? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentVideoSettingBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = mActivity?.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.textView.setOnClickListener {
            showtogetusersetting()
        }
        binding.name.setOnClickListener {
           showtogetusersetting()
        }
        if (sharedPreferences != null) {
            fastforward(sharedPreferences)
            speed2x(sharedPreferences)
            autoplay(sharedPreferences)
            orientation(sharedPreferences)
            brightness(sharedPreferences)
            speed(sharedPreferences)
        }
    }

    fun autoplay(sharedPreferences:SharedPreferences){
        val isFastForward = sharedPreferences?.getBoolean("autoplay", true)
        binding.autoSwitch.isChecked = isFastForward == true
        // Set a listener on the switch to save its new state whenever toggled
        binding.autoSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences?.edit()
            editor?.putBoolean("autoplay", isChecked)
            editor?.apply()
        }
    }

    fun fastforward(sharedPreferences:SharedPreferences){
        val isFastForward = sharedPreferences?.getBoolean("FastForward", true)
        binding.forwardSwitch.isChecked = isFastForward == true
        // Set a listener on the switch to save its new state whenever toggled
        binding.forwardSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences?.edit()
            editor?.putBoolean("FastForward", isChecked)
            editor?.apply()
        }
    }

    fun speed2x(sharedPreferences:SharedPreferences){
        val is2xSpeedEnabled = sharedPreferences?.getBoolean("2xSpeedFeatureEnabled", true)
        binding.switch2xSpeed.isChecked = is2xSpeedEnabled == true
        // Set a listener on the switch to save its new state whenever toggled
        binding.switch2xSpeed.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences?.edit()
            editor?.putBoolean("2xSpeedFeatureEnabled", isChecked)
            editor?.apply()
        }
    }

    fun orientation(sharedPreferences:SharedPreferences){
        val isFastForward = sharedPreferences?.getBoolean("orientationlock", true)
        binding.orientSwitch.isChecked = isFastForward == true
        // Set a listener on the switch to save its new state whenever toggled
        binding.orientSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences?.edit()
            editor?.putBoolean("orientationlock", isChecked)
            editor?.apply()
        }
    }


    fun brightness(sharedPreferences:SharedPreferences){
        val isFastForward = sharedPreferences?.getBoolean("brightnesslock", true)
        binding.brightSwitch.isChecked = isFastForward == true
        // Set a listener on the switch to save its new state whenever toggled
        binding.brightSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences?.edit()
            editor?.putBoolean("brightnesslock", isChecked)
            editor?.apply()
        }
    }

    fun speed(sharedPreferences:SharedPreferences){
        val isFastForward = sharedPreferences?.getBoolean("speedlock", true)
        binding.playspeedSwitch.isChecked = isFastForward == true
        // Set a listener on the switch to save its new state whenever toggled
        binding.playspeedSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences?.edit()
            editor?.putBoolean("speedlock", isChecked)
            editor?.apply()
        }
    }

    fun showtogetusersetting() {
        //exitAlertDialog
        var alertDialog: AlertDialog? = null
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(mActivity)
        val inflater: LayoutInflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.resume_control_setting, null)
        dialogBuilder.setView(dialogView)
        alertDialog = dialogBuilder.create()
        val sharedPreferences = mActivity?.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val savedRadioButtonId = sharedPreferences?.getInt("SelectedOptionPosition", -1)

        if (savedRadioButtonId != -1) {
            // We saved positions as 1, 2, 3, so we need to subtract 1 to get the index
            val index = savedRadioButtonId?.minus(1)
            val radioGroup = dialogView.findViewById<RadioGroup>(R.id.myRadioGroup)
            if (index in 0..2) { // Ensure the index is within the range of the radio buttons
                val radioButtonId = index?.let { radioGroup.getChildAt(it).id }
                if (radioButtonId != null) {
                    radioGroup.check(radioButtonId)
                }
            }
        }
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog?.show()
        dialogView.findViewById<TextView>(R.id.cancel).setOnClickListener {
            alertDialog?.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.okay).setOnClickListener {
            // Get the RadioGroup instance
            val radioGroup = dialogView.findViewById<RadioGroup>(R.id.myRadioGroup)
            // Determine the position of the selected RadioButton
            val radioButtonID = radioGroup.checkedRadioButtonId
            val radioButton = radioGroup.findViewById<RadioButton>(radioButtonID)
            val position = radioGroup.indexOfChild(radioButton) + 1 // Adding 1 so the position starts from 1,2,3

            // Save the selected radio button position in SharedPreferences
            val sharedPreferences = mActivity?.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences?.edit()
            editor?.putInt("SelectedOptionPosition", position)
            editor?.apply()
            alertDialog.dismiss()
        }
    }

}