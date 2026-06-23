package com.video.avd.ui.file_manager

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.video.avd.R
import com.video.avd.databinding.FragmentFileManagerBinding
import com.video.avd.extension.backNavigateTo
import com.video.avd.extension.nextNavigateTo
import com.video.avd.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class FileManagerFragment : Fragment(), FileManagerDirectoriesAdapter.DirectoryClickListener,
    DirAdapter.DirectoryClickListener {
    private var binding: FragmentFileManagerBinding? = null

    private val mViewModel: FileManagerViewModel by viewModels()

    private var mActivity: FragmentActivity? = null

    private var directoriesAdapter: FileManagerDirectoriesAdapter? = null

    //  private var directoriesAdapter: DirAdapter? = null

    private var directoriesList = arrayListOf<DirectoryModel>()




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFileManagerBinding.inflate(inflater, container, false)
        AppUtils.getMain(mActivity).hidebottombar()
        setAdapters()

        return binding?.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.directories.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {

                directoriesAdapter?.updateList(it)
                binding?.loader?.visibility = View.GONE
                binding?.clInternal?.visibility = View.VISIBLE
            }
        }
        if (binding?.rvDirectories?.adapter == null) {
            binding?.rvDirectories?.adapter = directoriesAdapter
        }
        getStorageInfo()
        clickListeners()

    }

    override fun onDirectoryClick(item: DirectoryModel, position: Int) {
        mActivity?.nextNavigateTo(
            FileManagerFragmentDirections.actionFileManagerToDirectoriesFragment(
                item.path,
                item.path, item.subFolderCount != "Directory is empty"
            )
        )
    }


    private fun setAdapters() {
        directoriesAdapter = FileManagerDirectoriesAdapter(listener = this, isFavourite = true)
        binding?.rvDirectories?.setHasFixedSize(true)
        binding?.rvDirectories?.adapter = directoriesAdapter
    }

    companion object {
        /**
        0  for name a to z
        1  for name z to a
        2  for date new to old
        3  for date old to new
        4  for size big to small
        5  for size small to big
         */
        var directorySortType = 0
        var showHiddenFiles = false
        // var isPermissionShow=true
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    private fun setSortedData() {
        lifecycleScope.launch {
            val sortedList = directoriesAdapter?.getList()
                ?.let { it1 -> mViewModel.sortDirectoryList(directorySortType, it1) }
            if (sortedList != null) {
                directoriesAdapter?.updateList(sortedList)
            }
        }

    }

    private fun getStorageInfo() {
        try {
            val info = mViewModel.getStorageInfo()
            Log.d("bbbb", "free ${info?.freeSpace}")
            Log.d("bbbb", "used ${info?.usedSpace}")
            val freeSpaces = info?.freeSpace?.let { mViewModel.bytesToGB(it) }
            //val usedSpaces = info?.usedSpace?.let { mViewModel.bytesToGB(it) }
            val totalSpace = info?.totalSpace?.let { mViewModel.bytesToGB(it) }
            binding?.tvSize?.text = "Free $freeSpaces GB of $totalSpace GB"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class StorageInfo(val freeSpace: Long, val usedSpace: Long, val totalSpace: Long)

    private fun showSortingDialog(anchorView: View) {
        try {
            val inflater =
                mActivity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val customView = inflater.inflate(R.layout.dialog_directory_sorting, null)
            val width = (resources.displayMetrics.widthPixels * 0.55).toInt()
            val popupWindow = PopupWindow(
                customView,
                width,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            popupWindow.animationStyle = R.style.PopupAnimation
            // Set the content view for the custom dialog
            val name = customView.findViewById<TextView>(R.id.tv_name)
            val date = customView.findViewById<TextView>(R.id.tv_date)
            val size = customView.findViewById<TextView>(R.id.tv_size)
            val none = customView.findViewById<TextView>(R.id.tv_none)

            val ascending = customView.findViewById<RadioButton>(R.id.sub1)
            val descending = customView.findViewById<RadioButton>(R.id.sub2)
            ascending.isChecked =
                directorySortType == 0 || directorySortType == 2 || directorySortType == 4
            descending.isChecked = !ascending.isChecked

            name.setOnClickListener {
                if (ascending.isChecked) {
                    directorySortType = 0
                    setSortedData()
                } else {
                    directorySortType = 1
                    setSortedData()
                }
                popupWindow.dismiss()
            }
            date.setOnClickListener {
                if (ascending.isChecked) {
                    directorySortType = 2
                    setSortedData()
                } else {
                    directorySortType = 3
                    setSortedData()
                }
                popupWindow.dismiss()
            }
            size.setOnClickListener {
                if (ascending.isChecked) {
                    directorySortType = 4
                    setSortedData()
                } else {
                    directorySortType = 5
                    setSortedData()
                }
                popupWindow.dismiss()
            }

            none.setOnClickListener {
                if (ascending.isChecked) {
                    directorySortType = 0
                    setSortedData()
                } else {
                    directorySortType = 1
                    setSortedData()
                }
                popupWindow.dismiss()
            }

            val xOff = 0 // Horizontal offset (if needed)
            val yOff = 20 // Vertical offset (adjust as needed)
            popupWindow.showAsDropDown(anchorView, xOff, yOff)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clickListeners() {
        binding?.back?.setOnClickListener {
            mActivity?.backNavigateTo()
        }

        binding?.clInternal?.setOnClickListener {
            mActivity?.nextNavigateTo(FileManagerFragmentDirections.actionFileManagerToDirectoriesFragment())
        }

        binding?.clSort?.setOnClickListener {
               showSortingDialog(it)
        }

    }


}