package com.video.avd.ui.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.video.avd.R
import com.video.avd.databinding.FragmentRateusBinding
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import com.video.avd.utils.ToastUtils
import com.avd.util.AdBlockerHelper


class RateUsFragment : BottomSheetDialogFragment() {

    var binding: FragmentRateusBinding? = null
    var mActivity: FragmentActivity? = null
    private var ratingBarValue = 0.0f

    private var headingList: List<String> = emptyList()
    private var descriptionList: List<String> = emptyList()

//    private val headingList = listOf(
//        getString(R.string.ur_ideas),
//        getString(R.string.unhappy_expe),
//        getString(R.string.seeking_insights),
//        getString(R.string.valueble_feedback),
//        getString(R.string.elevate_performance),
//        getString(R.string.thanks_for_stars)
//        )
//
//    private val descriptionList = listOf(
//        getString(R.string.appreciate),
//        getString(R.string.detailed_feedback),
//        getString(R.string.help_enhance),
//        getString(R.string.feedback_shapes),
//        getString(R.string.help_maintain),
//        getString(R.string.ongoing_support)
//    )



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRateusBinding.inflate(inflater, container, false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }


        mActivity?.let { context ->
            headingList = listOf(





















                context.getString(R.string.ur_ideas),
                context.getString(R.string.unhappy_expe),
                context.getString(R.string.seeking_insights),
                context.getString(R.string.valueble_feedback),
                context.getString(R.string.elevate_performance),
                context.getString(R.string.thanks_for_stars)
            )

            descriptionList = listOf(
                context.getString(R.string.appreciate),
                context.getString(R.string.detailed_feedback),
                context.getString(R.string.help_enhance),
                context.getString(R.string.feedback_shapes),
                context.getString(R.string.help_maintain),
                context.getString(R.string.ongoing_support)
            )
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let {
            AppUtils.firebaseUserAction("onViewCreated_RateUsFragment", "RateUsFragment")
            binding?.title?.text = headingList[0]
            binding?.textlayout?.text = descriptionList[0]
            binding?.textlayout?.text= Html.fromHtml( "If you like this app, please rate us " +"<br>"+"<font color=\"#FFB800\">5 </font>" + " stars")
            binding?.ratingBar?.let { animateStar() }
            val ratingBarListener = RatingBar.OnRatingBarChangeListener { Rating, rating, _ ->
                when (Rating?.id) {
                    R.id.ratingBar -> ratingBarValue = rating

                }
                emojiAnimation()
                checkEnableSubmitButton()
                val drawableRes = when (ratingBarValue) {
                    in 0f..1f -> {
                        binding?.title?.text = headingList[1]
                        binding?.textlayout?.text = descriptionList[1]
                        R.drawable.sad
                    }
                    in 1f..2f -> {
                        binding?.title?.text = headingList[2]
                        binding?.textlayout?.text = descriptionList[2]
                        R.drawable.littlesad
                    }
                    in 2f..3f -> {
                        binding?.title?.text = headingList[3]
                        binding?.textlayout?.text = descriptionList[3]
                        R.drawable.normal
                    }
                    in 3f..4f -> {
                        binding?.title?.text = headingList[4]
                        binding?.textlayout?.text = descriptionList[4]
                        R.drawable.happy
                    }
                    else -> {
                        binding?.title?.text = headingList[5]
                        binding?.textlayout?.text = descriptionList[5]
                        R.drawable.excited
                    }

                }
                binding?.emoji?.setImageResource(drawableRes)
            }
            binding?.ratingBar?.onRatingBarChangeListener = ratingBarListener

        }
    }

    private fun checkEnableSubmitButton() {
//        binding?.submitBtn?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey)
   /*     val enableSubmitButton = ratingBarValue > 4.0f
        val checkStar = ratingBarValue > 0.0f && ratingBarValue <5.0f
        binding?.submitButton?.isEnabled = enableSubmitButton*/
            if(ratingBarValue > 0.0f && ratingBarValue <5.0f){
                binding?.submitButton?.isEnabled=true
                binding?.submitButton?.text="Give Feedback"
                binding?.hintext?.visibility=View.INVISIBLE
                binding?.submitButton?.setOnClickListener {
                    feedbackDialog()
                }
        }
        else if(ratingBarValue > 4.0f){
                binding?.submitButton?.isEnabled=true
            binding?.hintext?.visibility=View.INVISIBLE
//            binding?.pointingimage?.visibility=View.INVISIBLE
                binding?.submitButton?.text="Rate on Google Play"
                binding?.submitButton?.setOnClickListener {
                    AppPreference.setPlaystorePrefs(requireContext())
                    // Prevent app open ad when returning from Play Store after rating
                    AdBlockerHelper.setinterstitialshown(true)
                    AppUtils.rateus(requireContext())
                    findNavController().popBackStack()
//                    ToastUtils.showToast(requireActivity(),"Submit")
                }
        }
            else{
                ToastUtils.showToast(requireContext(),"Please select at least one star to continue")
                binding?.submitButton?.text="Rate on Google Play"
                binding?.submitButton?.isEnabled=false
            }

    }
    private fun emojiAnimation(){
        val animationSet = AnimationSet(true)
        val scaleAnimation = ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 200
        scaleAnimation.interpolator = OvershootInterpolator()
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 200
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)
        binding?.emoji?.startAnimation(animationSet)
    }

    private fun feedbackDialog(){
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.feedbackdialog)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent) // Set the dialog background as transparent
        window?.decorView?.setBackgroundResource(R.drawable.feedbackcorner)
        val editText = dialog.findViewById<TextInputEditText>(R.id.edit_query)
        val submit=dialog.findViewById<AppCompatButton>(R.id.submit)
        submit.isEnabled = false
        submit.background = ContextCompat.getDrawable(requireContext(),R.drawable.exit_btn_rate)
        submit.setTextColor(ContextCompat.getColor(requireContext(),R.color.gSelector_light))
        val exit=dialog.findViewById<AppCompatButton>(R.id.exit)
        exit.setOnClickListener { dialog.dismiss() }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s.isNullOrEmpty()){
                    submit.isEnabled = false
                    submit.background = ContextCompat.getDrawable(requireContext(),R.drawable.exit_btn_rate)
                    submit.setTextColor(ContextCompat.getColor(requireContext(),R.color.gSelector_light))
                }else{
                    submit.isEnabled = true
                    submit.background = ContextCompat.getDrawable(requireContext(),R.drawable.cancle_btn_rate)
                    submit.setTextColor(ContextCompat.getColor(requireContext(),R.color.brand_text_primary))
                }

            }
            override fun afterTextChanged(s: Editable?) {
            }
        })

        submit.setOnClickListener {
            if (submit.isEnabled){
                Toast.makeText(requireContext(),getString(R.string.submitted),Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                this.dismiss()
            }

        }
        dialog.show()
    }

    private fun animateStar() {
        val animationSet = AnimationSet(true)
        val scaleAnimation = ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        scaleAnimation.duration = 500
        scaleAnimation.interpolator = OvershootInterpolator()
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 500
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)
        // Start the filled stars animation
        binding?.ratingBar?.startAnimation(animationSet)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }
    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet =
                    dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }
}