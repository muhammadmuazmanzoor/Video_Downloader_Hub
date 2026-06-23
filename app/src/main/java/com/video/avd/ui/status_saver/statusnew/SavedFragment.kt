package com.video.avd.ui.status_saver.statusnew

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.video.avd.constent.isFileSave
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentSavedBinding
import com.video.avd.ui.MainActivity
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.status_saver.CommonStatusUtils
import com.video.avd.ui.status_saver.model.Status
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class SavedFragment : Fragment(), SavedVideoAdapter.SavedVideoClickListener {
    private var binding: FragmentSavedBinding? = null
    private var mActivity: FragmentActivity? = null
    private val savedFilesList: MutableList<Status> = mutableListOf()

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
        binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.firebaseUserAction("onViewCreated whatsapp saved fragment", "SavedFragment")
        mActivity?.let {
            if (it is MainActivity) {
                AppUtils.getMain(it).hidebottombar()
            }
        }
        mActivity?.let {
            binding?.recyclerViewVideo?.layoutManager = GridLayoutManager(requireContext(), 2)
            isFileSave.observe(requireActivity()) {
                if (it) {
                    recyclerview()
                } else {
                    recyclerview()
                }
            }
        }
    }

    private fun recyclerview() {
        lifecycleScope.launch {
            getFiles()
            withContext(Dispatchers.Main){
                val filesAdapter = SavedVideoAdapter(savedFilesList, this@SavedFragment)
                binding?.recyclerViewVideo?.adapter = filesAdapter
                if (filesAdapter.itemCount == 0) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
                        binding?.savedFolder?.visibility = View.VISIBLE
                        binding?.savedText?.visibility = View.VISIBLE
                    }
                } else {
                    binding?.savedFolder?.visibility = View.GONE
                    binding?.savedText?.visibility = View.GONE
                }
                filesAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun onStatusVideoClick(list: List<Status>, position: Int, status: Status) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                AppUtils.firebaseUserAction("onStatusVideoClick_SavedFragment", "SavedFragment")

                val url = if (status.isApi30) {
                    status.documentFile.uri
                } else {
                    Uri.fromFile(status.file)
                }
                videolistglobal = emptyList()
                val video = Video(contentUri = url.toString())
                videolistglobal = listOf(video)

                val result = Bundle().apply {
                    putString("id", position.toString())
                    putBoolean("isliveuri", false)
                    putString("fragmentName", "Status")
                }

                mActivity?.let { activity ->
                    if (activity is MainActivity) {
                        AppUtils.getMain(activity).hidebottombar()
                    }
                    val intent = Intent(activity, PlayerVideoActivity::class.java).apply {
                        putExtras(result)
                    }
                    try {
                        activity.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            } catch (e: Exception) {
                mActivity?.let {
                    Toast.makeText(it, e.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

   suspend fun getFiles() {
        savedFilesList.clear()
        val appDir: File? = CommonStatusUtils.APP_DIR?.let { File(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val statusSaverDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + File.separator + "zmstatus_saver"
            )
            val filteredFiles = statusSaverDir.listFiles { _, name ->
                !name.startsWith(".trashed-")
            }

            handleFiles(filteredFiles)
        } else {
            val filteredFiles = appDir?.listFiles { _, name ->
         //       !(name == ".Shared" || name == "Media" || name.startsWith(".trashed-") || name.startsWith(".Thumbs"))
                name.endsWith(".mp4")
            }
            handleFiles(filteredFiles)
        }
    }


    private fun handleFiles(files: Array<File>?) {
        if (!files.isNullOrEmpty()) {
            files.sort()
            val statuses = files.map { file ->
                Status(file, file.name, file.absolutePath)
            }
            savedFilesList.addAll(statuses)
        } else {
            // Handle the case where files is null or empty, if needed.
        }
    }

}