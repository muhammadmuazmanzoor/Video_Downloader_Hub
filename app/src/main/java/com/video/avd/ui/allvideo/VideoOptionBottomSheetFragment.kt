package com.video.avd.ui.allvideo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.avd.R
import com.video.avd.databinding.FragmentVideoOptionBotomSheetBinding
import com.video.avd.ui.allvideo.adapter.VideoOptionAdapter
import com.video.avd.ui.allvideo.adapter.VideoOptions
import com.video.avd.utils.AppUtils.hideNavigationBarFromDialog


class VideoOptionBottomSheetFragment(
    private val isFromVideo: Boolean,
    private val isAlreadyInFavourite: Boolean = false,
    private val isFileManager: Boolean = false
) :
    BottomSheetDialogFragment(),
    VideoOptionAdapter.VideoOptionsClickListener {

    private var binding: FragmentVideoOptionBotomSheetBinding? = null

    private var mActivity: FragmentActivity? = null


    private var listener: OptionSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVideoOptionBotomSheetBinding.inflate(inflater, container, false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.let {
            if (it != null) {
                val list = arrayListOf<VideoOptions>()

                if (isFileManager) {
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_menu)
                        ?.let { it1 -> VideoOptions(0, getString(R.string.delete), it1) }
                        ?.let { it2 -> list.add(it2) }

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_share_menu)
                        ?.let { it1 -> VideoOptions(1, getString(R.string.share), it1) }
                        ?.let { it2 -> list.add(it2) }
                    val adapter = VideoOptionAdapter(it, list, this)
                    binding?.rvVideoOption?.layoutManager = LinearLayoutManager(requireContext())
                    binding?.rvVideoOption?.adapter = adapter
                } else if (isFromVideo) {
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_menu)
                        ?.let { it1 -> VideoOptions(3, getString(R.string.delete), it1) }
                        ?.let { it2 -> list.add(it2) }

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_share_menu)
                        ?.let { it1 -> VideoOptions(4, getString(R.string.share), it1) }
                        ?.let { it2 -> list.add(it2) }

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_rename)
                        ?.let { it1 -> VideoOptions(5, getString(R.string.rename), it1) }
                        ?.let { it2 -> list.add(it2) }


                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_info_menu)
                        ?.let { it1 -> VideoOptions(6, getString(R.string.file_info), it1) }
                        ?.let { it2 -> list.add(it2) }

                    /*  ContextCompat.getDrawable(requireContext(), R.drawable.ic_video_cut)
                          ?.let { it1 -> VideoOptions(5, "Add to Private", it1) }
                          ?.let { it2 -> list.add(it2) }*/
                    val adapter = VideoOptionAdapter(it, list, this)
                    binding?.rvVideoOption?.layoutManager = LinearLayoutManager(requireContext())
                    binding?.rvVideoOption?.adapter = adapter
                } else {
                    val musicOptionsList = arrayListOf<VideoOptions>()

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_menu)
                        ?.let { it1 -> VideoOptions(0, getString(R.string.play), it1) }
                        ?.let { it2 -> musicOptionsList.add(it2) }

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_rename)
                        ?.let { it1 -> VideoOptions(3, getString(R.string.rename), it1) }
                        ?.let { it2 -> musicOptionsList.add(it2) }

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_menu)
                        ?.let { it1 -> VideoOptions(4, getString(R.string.delete), it1) }
                        ?.let { it2 -> musicOptionsList.add(it2) }

                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_share_menu)
                        ?.let { it1 -> VideoOptions(5, getString(R.string.share), it1) }
                        ?.let { it2 -> musicOptionsList.add(it2) }


                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_info_menu)
                        ?.let { it1 -> VideoOptions(6, getString(R.string.file_info), it1) }
                        ?.let { it2 -> musicOptionsList.add(it2) }

                    val adapter = VideoOptionAdapter(it, musicOptionsList, this)
                    binding?.rvVideoOption?.layoutManager = LinearLayoutManager(requireContext())
                    binding?.rvVideoOption?.adapter = adapter
                }

                ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                    v.setPadding(0, 0, 0, 0) // Remove padding for system bars
                    WindowInsetsCompat.CONSUMED // Indicate that insets have been consumed
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBarFromDialog()
    }
    override fun onVideoOptionClick(item: VideoOptions, position: Int) {
        listener?.onOptionSelected(position)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Set window flags and hide system bars before the dialog is shown
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )

            val insetsController = WindowCompat.getInsetsController(this, decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        return dialog
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }


    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    fun setOptionSelected(listener: OptionSelectedListener) {
        this.listener = listener
    }

    interface OptionSelectedListener {
        fun onOptionSelected(selectedPosition: Int)
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