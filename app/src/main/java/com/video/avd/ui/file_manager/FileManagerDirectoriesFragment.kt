package com.video.avd.ui.file_manager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.video.avd.R
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentFileManagerDirectoriesBinding
import com.video.avd.extension.backNavigateTo
import com.video.avd.extension.nextNavigateTo
import com.video.avd.ui.allvideo.VideoOptionBottomSheetFragment
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.videos.model.Video
import com.video.avd.ads.AppOpenManager
import com.video.avd.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


private const val DELETE_PERMISSION_REQUEST_VIDEO = 777


@AndroidEntryPoint
class FileManagerDirectoriesFragment : Fragment(), FileManagerAdapter.FileManagerMenuClickListener, FileManagerAdapter.FileMangerClickListener {

    private var binding: FragmentFileManagerDirectoriesBinding? = null
    private var mActivity: FragmentActivity? = null

    private val mViewModel: FileManagerDirectoriesViewModel by viewModels()

    private var subFolderPath = ""
    private var sourcePath = ""


    private var allAdapter : FileManagerAdapter ?=null

    private var hasFile = true

    private var tempTitle = ""
    private var deletedPosition = 0

    private var directoriesList = arrayListOf<MediaResources>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            val args = it?.let { it1 -> FileManagerDirectoriesFragmentArgs.fromBundle(it1) }
            if (!args?.directoryPath.isNullOrEmpty()) {
                subFolderPath = args?.directoryPath.toString()
                sourcePath = args?.directorySource.toString()
                hasFile = args?.hasFile == true
            }

            if (subFolderPath.isEmpty()){
                val internalStorageDir = Environment.getExternalStorageDirectory()
                activity?.contentResolver?.let { it1 ->
                    mViewModel.loadData(internalStorageDir.toString(),
                        it1
                    )
                }
            }else{
                activity?.contentResolver?.let { it1 ->
                    mViewModel.loadData(subFolderPath,
                        it1
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFileManagerDirectoriesBinding.inflate(inflater, container, false)
        if (sourcePath.isNotEmpty()) {
            binding?.tvPath?.text = sourcePath
            if (hasFile) binding?.emptyLayout?.visibility = View.GONE
            else{
                binding?.emptyLayout?.visibility = View.VISIBLE
                binding?.loader?.visibility=View.GONE
            }

        } else {
            binding?.tvPath?.visibility = View.GONE
            binding?.emptyLayout?.visibility = View.GONE
        }

        allAdapter = FileManagerAdapter(emptyList(), this, this)

        return binding?.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observers()
        clickListeners()

        mViewModel.allItems.observe(viewLifecycleOwner){
            if (it.isNotEmpty()){
                directoriesList.addAll(it)
             lifecycleScope.launch {
                 val sortedList=mViewModel.sortDirectoryList(1, it)
                 allAdapter?.updateList(sortedList)

                 binding?.rvDirectories?.adapter=allAdapter
             }
                binding?.loader?.visibility=View.GONE
            }
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }


    private fun menuClick() {
        val popup = mActivity?.let { it1 ->
            binding?.imgMenu?.let { it4 -> PopupMenu(it1, it4) }
        }
        popup?.menuInflater?.inflate(R.menu.file_manager_menu, popup.menu)

        if (FileManagerFragment.showHiddenFiles) popup?.menu?.getItem(0)?.title =
            "hide hidden files"
        else popup?.menu?.getItem(0)?.title = "show hidden files"


        popup?.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.show_hidden_file -> {
                    FileManagerFragment.showHiddenFiles = !FileManagerFragment.showHiddenFiles
                    showDataToAdapterAfter()
                }
            }
            false
        }
        popup?.show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST_VIDEO) {
            mViewModel.newUri?.let {
                mActivity?.let { it1 ->
                    try {
                        AppUtils.deleteVideoFile(it1, it)
                        mViewModel.deleteVideoFromDB(tempTitle)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                AppOpenManager.isShowingAd = false
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                AppOpenManager.isShowingAd = false
            }
        }
    }

    private fun deleteVideo(uri: Uri, context: Context) {
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.alert_dailog, null)
            view.findViewById<TextView>(R.id.alertMessage).text =
                getString(R.string.do_you_want_to_delete_this)
            val icon = view.findViewById<ImageView>(R.id.alertImage)
            val btnCancel = view.findViewById<Button>(R.id.btnCancel)
            val btnOk = view.findViewById<Button>(R.id.btnOk)
            btnOk.text = getString(R.string.delete)
            icon.setImageResource(R.drawable.ci_delete_p)
            val builder = android.app.AlertDialog.Builder(mActivity)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams
            btnOk.setOnClickListener {
                lifecycleScope.launch {
                    mViewModel.deleteVideo(uri, context)
                }
                alertDialog.dismiss()
            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.show()

    }


    private fun showDataToAdapterAfter() {
        lifecycleScope.launch {
//            val directories = if (FileManagerFragment.showHiddenFiles) {
//                directoriesList
//            } else {
//                directoriesList.asSequence()  // Convert to sequence for lazy evaluation
//                    .filter {item ->
//                        when (item) {
//                            is ListItem.DirectoryItems -> !item.item.name.startsWith(".")
//                            else -> true // Keep non-directory items
//                        }
//                    }  // Filter out hidden directories
//                    .toList()  // Convert back to list
//            }

            val directories = if (FileManagerFragment.showHiddenFiles) {
                directoriesList // Assuming 'list' is your original list of ListItem objects
            } else {
                directoriesList.asSequence() // Convert to sequence for lazy evaluation
                    .filter { item ->
                        when (item) {
                            is MediaResources.DirectoryItems -> !item.item.name.startsWith(".")
                            else -> true // Keep non-directory items
                        }
                    }
                    .toList() // Convert back to list
            }


            val sortedList = mViewModel.sortDirectoryList(FileManagerFragment.directorySortType, directories)
            allAdapter?.updateList(sortedList)

            binding?.clDir?.visibility = View.VISIBLE
            binding?.loader?.visibility = View.GONE
        }
    }



    private fun showCreateFolderDialog() {
        mActivity?.let { activity ->
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.rename_dailog, null)
            val btnCancel = view.findViewById<TextView>(R.id.tvCancell)
            val btnOk = view.findViewById<TextView>(R.id.tvOk)
            view.findViewById<TextView>(R.id.tvRename).text = "Create new folder"
            val editText = view.findViewById<EditText>(R.id.etName)
            val builder = AlertDialog.Builder(mActivity)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams
            btnOk.setOnClickListener {
                val newName = editText.text.toString()
                if (newName.isEmpty()) {
                    Toast.makeText(mActivity, "please enter folder name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val result = if (subFolderPath.isEmpty()) {
                    val storageDir = Environment.getExternalStorageDirectory()
                    mViewModel.createNewFolder(newName, storageDir.toString())
                } else {
                    mViewModel.createNewFolder(newName, subFolderPath)
                }
                if (result == "folder created successfully"){
                    binding?.emptyLayout?.visibility=View.GONE
                }
                Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.show()

        }
    }


    private fun observers() {
        mViewModel.isVideoForDelete.observe(viewLifecycleOwner) {
            if (it == true) {
                allAdapter?.removeItemAt(deletedPosition)
                mViewModel.deleteVideoFromDB(tempTitle)
            }
        }

        mViewModel.isAudioForDelete.observe(viewLifecycleOwner) {
            if (it == true) {
                allAdapter?.removeItemAt(deletedPosition)
            }
        }

        mViewModel.permissionNeededForDeleteVideo.observe(viewLifecycleOwner) { intentSender ->
            intentSender?.let {
                AppOpenManager.isShowingAd = true
//                isSplash = true
                startIntentSenderForResult(
                    intentSender,
                    DELETE_PERMISSION_REQUEST_VIDEO,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }
        }


    }


    private fun clickListeners() {
        mActivity?.let { activity ->
            binding?.back?.setOnClickListener {
                activity.backNavigateTo()
            }
            binding?.imgMenu?.setOnClickListener {
                menuClick()
            }
            binding?.addFolder?.setOnClickListener {
                showCreateFolderDialog()
            }
        }
    }

    override fun onDirectoryClick(position: Int, item: MediaResources.DirectoryItems) {
        val hasFile =
            item.item.subFolderCount != "Directory is empty" || item.item.videoCount.toInt() > 0 || item.item.audioCount.toInt() > 0

        val source = if (subFolderPath.isNotEmpty()) "$subFolderPath/${item.item.name}" else item.item.path
        mActivity?.nextNavigateTo(
            FileManagerDirectoriesFragmentDirections.actionDirectoriesFragmentToItself(
                item.item.path,
                source, hasFile
            )
        )
    }

    override fun onVideoClick(position: Int, item: MediaResources.VideoItems) {
        if (PlayerVideoActivity.getInstance() != null && PlayerVideoActivity.isPipMode) {
            PlayerVideoActivity.getInstance()?.finishAndRemoveTask()
        }
        val singleItemList = arrayListOf<Video>()
        val video=mViewModel.convertVideoItemsToVideo(item)
        singleItemList.add(video)
        videolistglobal = ArrayList(singleItemList)
        val result = Bundle()
        result.putString("id", "0")
        result.putBoolean("isliveuri", false)
        result.putString("uri", "")
        result.putBoolean("isPlaybackCount", true)
        result.putString("fragmentName", "File Manager")
        mActivity?.let { activity ->
            val intent =
                Intent(activity, PlayerVideoActivity::class.java)
            intent.putExtras(result)
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }



    override fun onVideoMenuClick(position: Int, item: MediaResources.VideoItems) {
        val dg = VideoOptionBottomSheetFragment(isFromVideo = false, isFileManager = true)
        dg.show(parentFragmentManager, "")
        dg.setOptionSelected(object : VideoOptionBottomSheetFragment.OptionSelectedListener {
            override fun onOptionSelected(selectedPosition: Int) {
                dg.dismiss()
                when (selectedPosition) {
                    0 -> {
                        lifecycleScope.launch {
                            tempTitle = item.item.title.toString()
                            deletedPosition = position
                            item.item.contentUri?.let {
                                mActivity?.let { it1 ->
                                    deleteVideo(Uri.parse(it), it1)
                                }
                            }
                        }
                    }

                    1 -> {
                        mActivity?.let { activity ->
                            item.item.contentUri?.let { uri ->
                                AppUtils.getFilePathFromContentUri(Uri.parse(uri), activity)
                                    ?.let { path ->
                                        File(path)
                                    }?.let { file -> mViewModel.shareVideo(activity, file) }
                            }
                        }
                    }

                }
            }
        })

    }


}