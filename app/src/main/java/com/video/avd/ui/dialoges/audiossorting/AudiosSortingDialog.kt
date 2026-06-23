package com.video.avd.ui.dialoges.audiossorting

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.avd.R
import com.video.avd.databinding.AudiosSortingDialogBinding
import com.video.avd.ui.dialoges.audiossorting.listners.OnAudiosSortChangedListner


class AudiosSortingDialog(private var sortType: Int) : BottomSheetDialogFragment() {
    private var binding: AudiosSortingDialogBinding? = null
    private var sortingType = 2
    private var isSub1Checked = true
    private var isSub2Checked = false

    companion object {
        var listner: OnAudiosSortChangedListner? = null
        fun setlistner(listnern: OnAudiosSortChangedListner?) {
            this.listner = listnern
        }
    }
    var onDismissListener: (() -> Unit)? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = AudiosSortingDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()  // Notify listener when dismissed
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sortingType = sortType
        initValuesSetup()
        intialUiSetUp()
        radioChecks()
        buttonsClickListners()
    }

    private fun buttonsClickListners() {
        binding?.tvOk?.setOnClickListener {
            val isChanged = if (sortingType != sortingType) true else false
            listner?.onSortChanged(true, sortingType)
            dismiss()
        }

        binding?.tvCancel?.setOnClickListener {
            dismiss()
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
                /*
                                R.id.radioLength -> {
                                    binding?.sub1?.text = resources.getText(R.string.from_long_to_short)
                                    binding?.sub2?.text = resources.getText(R.string.from_short_to_long)
                                    if (isSub1Checked) {
                                        sortingType = 6
                                    } else if (isSub2Checked) {
                                        sortingType = 7
                                    }
                                }*/
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

                        /*  R.id.radioLength -> {
                              sortingType = 6
                          }*/
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

                        /* R.id.radioLength -> {
                             sortingType = 7
                         }*/
                    }


                }
            }
        }
    }

    private fun intialUiSetUp() {
        when (sortingType) {
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

        }
    }

    private fun initValuesSetup() {
        when (sortingType) {
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

        }
    }


}