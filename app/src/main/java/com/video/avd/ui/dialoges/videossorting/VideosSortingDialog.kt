package com.video.avd.ui.dialoges.videossorting

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.avd.R
import com.video.avd.databinding.FragmentVideosSortingDialogBinding
import com.video.avd.utils.AppUtils.hideNavigationBarFromDialog

class VideosSortingDialog() : BottomSheetDialogFragment() {
    private var binding: FragmentVideosSortingDialogBinding? = null
    private val args: VideosSortingDialogArgs by navArgs()
    private var sortingType = 2
    private var isSub1Checked = true
    private var isSub2Checked = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVideosSortingDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //   setStyle(STYLE_NORMAL,R.style.AppBottomSheetDialogThemes)
        sortingType = args.sortingType
        initValuesSetup()
        intialUiSetUp()
        radioChecks()
        buttonsClickListners()

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            v.setPadding(0, 0, 0, 0) // Remove padding for system bars
            WindowInsetsCompat.CONSUMED // Indicate that insets have been consumed
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBarFromDialog()
    }
    private fun intialUiSetUp() {
        when (args.sortingType) {
            0 -> {
                binding?.radioName?.isChecked = true
                binding?.sub1?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_a_to_z)
                binding?.sub2?.text = resources.getText(R.string.from_z_to_a)
            }

            1 -> {
                binding?.radioName?.isChecked = true
                binding?.sub2?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_a_to_z)
                binding?.sub2?.text = resources.getText(R.string.from_z_to_a)
            }

            2 -> {
                binding?.radioDate?.isChecked = true
                binding?.sub1?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_new_to_old)
                binding?.sub2?.text = resources.getText(R.string.from_old_to_new)
            }

            3 -> {
                binding?.radioDate?.isChecked = true
                binding?.sub2?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_new_to_old)
                binding?.sub2?.text = resources.getText(R.string.from_old_to_new)
            }

            4 -> {
                binding?.radioSize?.isChecked = true
                binding?.sub1?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_big_to_small)
                binding?.sub2?.text = resources.getText(R.string.from_small_to_big)
            }

            5 -> {
                binding?.radioSize?.isChecked = true
                binding?.sub2?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_big_to_small)
                binding?.sub2?.text = resources.getText(R.string.from_small_to_big)
            }

            6 -> {
                binding?.radioLength?.isChecked = true
                binding?.sub1?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_long_to_short)
                binding?.sub2?.text = resources.getText(R.string.from_short_to_long)
            }

            7 -> {
                binding?.radioLength?.isChecked = true
                binding?.sub2?.isChecked = true
                binding?.sub1?.text = resources.getText(R.string.from_long_to_short)
                binding?.sub2?.text = resources.getText(R.string.from_short_to_long)
            }
        }
    }

    private fun initValuesSetup() {
        when (args.sortingType) {
            0 -> {
                isSub1Checked = true
                isSub2Checked = false
            }

            1 -> {
                isSub1Checked = false
                isSub2Checked = true
            }

            2 -> {
                isSub1Checked = true
                isSub2Checked = false
            }

            3 -> {
                isSub1Checked = false
                isSub2Checked = true
            }

            4 -> {
                isSub1Checked = true
                isSub2Checked = false
            }

            5 -> {
                isSub1Checked = false
                isSub2Checked = true
            }

            6 -> {
                isSub1Checked = true
                isSub2Checked = false
            }

            7 -> {
                isSub1Checked = false
                isSub2Checked = true
            }
        }
    }

    private fun radioChecks() {
        binding?.radioGroupMainSorting?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioName -> {
                    binding?.sub1?.text = resources.getText(R.string.from_a_to_z)
                    binding?.sub2?.text = resources.getText(R.string.from_z_to_a)
                    if (isSub1Checked) {
                        sortingType = 0
                    } else if (isSub2Checked) {
                        sortingType = 1
                    }
                }

                R.id.radioDate -> {
                    binding?.sub1?.text = resources.getText(R.string.from_new_to_old)
                    binding?.sub2?.text = resources.getText(R.string.from_old_to_new)
                    if (isSub1Checked) {
                        sortingType = 2
                    } else if (isSub2Checked) {
                        sortingType = 3
                    }
                }

                R.id.radioSize -> {
                    binding?.sub1?.text = resources.getText(R.string.from_big_to_small)
                    binding?.sub2?.text = resources.getText(R.string.from_small_to_big)
                    if (isSub1Checked) {
                        sortingType = 4
                    } else if (isSub2Checked) {
                        sortingType = 5
                    }
                }
                R.id.radioLength -> {
                    binding?.sub1?.text = resources.getText(R.string.from_long_to_short)
                    binding?.sub2?.text = resources.getText(R.string.from_short_to_long)
                    if (isSub1Checked) {
                        sortingType = 6
                    } else if (isSub2Checked) {
                        sortingType = 7
                    }
                }
            }
        }

        binding?.radioGroupSubSorting?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.sub1 -> {
                    isSub1Checked = true
                    isSub2Checked = false
                    val mainRadioCheckedId = binding?.radioGroupMainSorting?.checkedRadioButtonId

                    when (mainRadioCheckedId) {
                        R.id.radioName -> {
                            sortingType = 0
                        }

                        R.id.radioDate -> {
                            sortingType = 2
                        }

                        R.id.radioSize -> {
                            sortingType = 4
                        }
                        R.id.radioLength -> {
                            sortingType = 6
                        }

                    }

                }

                R.id.sub2 -> {
                    isSub2Checked = true
                    isSub1Checked = false
                    val mainRadioCheckedId = binding?.radioGroupMainSorting?.checkedRadioButtonId
                    when (mainRadioCheckedId) {
                        R.id.radioName -> {
                            sortingType = 1
                        }

                        R.id.radioDate -> {
                            sortingType = 3
                        }

                        R.id.radioSize -> {
                            sortingType = 5
                        }

                        R.id.radioLength -> {
                            sortingType = 7
                        }


                    }


                }
            }
        }
    }

    private fun buttonsClickListners() {
        binding?.tvOk?.setOnClickListener {
            args.sortingType != sortingType
            args.listener.onSortChanged(true, sortingType)
            dismiss()
        }

        binding?.tvCancel?.setOnClickListener {
            dismiss()
        }
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

}