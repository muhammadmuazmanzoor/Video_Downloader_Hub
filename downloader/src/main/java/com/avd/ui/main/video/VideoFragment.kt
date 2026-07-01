package com.avd.ui.main.video

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.avd.R
import com.avd.data.local.model.LocalVideo
import com.avd.databinding.FragmentVideoBinding
import com.avd.ui.component.adapter.VideoAdapter
import com.avd.ui.component.adapter.VideoListener
import com.avd.ui.component.dialog.showRenameVideoDialog
import com.avd.ui.main.base.BaseFragment
import com.avd.ui.main.progress.WrapContentLinearLayoutManager
import com.avd.ui.main.video.VideoViewModel.Companion.FILE_EXIST_ERROR_CODE
import com.avd.util.AdBlockerHelper.downloaderShown
import com.avd.util.AdBlockerHelper.fromVideo
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.inter_videos
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.AppUtil
import com.avd.util.FileUtil
import com.avd.util.IntentUtil
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.inmobi.media.fa
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject


@AndroidEntryPoint
class VideoFragment : BaseFragment() {

    companion object {
        fun newInstance() = VideoFragment()
    }

    private var disposable: Disposable? = null

    @Inject
    lateinit var intentUtil: IntentUtil

    @Inject
    lateinit var fileUtil: FileUtil

    @Inject
    lateinit var appUtil: AppUtil

//    lateinit var mainActivity: MainActivityDownloader

    private lateinit var dataBinding: FragmentVideoBinding

    private  val videoViewModel: VideoViewModel by viewModels()

    private lateinit var videoAdapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        videoAdapter = VideoAdapter(emptyList(), videoListener, fileUtil)
//        mainActivity=DownloaderModuleNavigator.getMain(requireActivity())
//        mainActivity.settingsViewModel.isDarkMode.get()
        val isDark = false
        val color = if (isDark) {
            MaterialColors.getColor(requireContext(), R.attr.editTextColor, Color.YELLOW)
        } else {
            null
        }
        dataBinding = FragmentVideoBinding.inflate(inflater, container, false).apply {
            val managerL = WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            this.viewModel = videoViewModel
            this.rvVideo.layoutManager = managerL
            this.rvVideo.adapter = videoAdapter
            if (color != null) {
                this.ivEmptyIcon.setBackgroundColor(color)
            }
        }
        videoViewModel.shareEvent.observe(viewLifecycleOwner) { uri ->
            intentUtil.shareVideo(requireContext(), uri)
        }
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fromVideo=true
        videoViewModel.start()
        handleUIEvents()
        handleIfStartedFromNotification()
    }

    private fun handleUIEvents() {
        videoViewModel.apply {
            renameErrorEvent.observe(viewLifecycleOwner, Observer { errorCode ->
                val errorMessage =
                    if (errorCode == FILE_EXIST_ERROR_CODE) R.string.video_rename_exist else R.string.video_rename_invalid
                activity?.runOnUiThread {
                    Toast.makeText(context, context?.getString(errorMessage), Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoViewModel.stop()
        fromVideo= false
    }
    private fun handleIfStartedFromNotification() {
//        mainActivity.mainViewModel.openDownloadedVideoEvent.observe(viewLifecycleOwner) { downloadFilename ->
//            disposable?.dispose()
//            disposable = null
//            disposable = videoViewModel.findVideoByName(downloadFilename).subscribeOn(Schedulers.io()).observeOn(Schedulers.single()).subscribe { video ->
//                        startVideo(video)
//            }
//        }
    }


    private val videoListener = object : VideoListener {
        override fun onItemClicked(localVideo: LocalVideo) {
            startVideo(localVideo)
        }
        override fun onMenuClicked(view: View, localVideo: LocalVideo) {
           // showPopupMenu(view, localVideo)
            showCustomVideoMenu(view,localVideo)
         /*   val dg = MenuBottomsheetFragment()
            dg.show(parentFragmentManager, "")
            dg.setMenuListeners(object : MenuBottomsheetFragment.CustomMenuListener{
                override fun onMenuCLick(which: String) {
                  when(which){
                      "play"->{
                          startVideo(localVideo)
                          dg.dismiss()
                      }
                      "rename"->{
                          showRenameVideoDialog(view.context, appUtil, localVideo.name,
                              View.OnClickListener { v ->
                                  with(v as EditText) {
                                      dg.dismiss()
                                      val newName = v.text.toString().trim()
                                      videoViewModel.renameVideo(v.context, localVideo.uri, File(newName).nameWithoutExtension + ".mp4")
                                  }
                              })
                      }
                      "share" -> {
                          dg.dismiss()
                          CoroutineScope(Dispatchers.IO).launch {
                              val file =
                                  activity?.let {
                                      getFilePathFromContentUri(localVideo.uri,
                                          it
                                      )?.let { File(it) }
                                  }
                              withContext(Dispatchers.Main) {
                                  file?.let { shareVideo(it) }
                              }
                          }
                      }
                  }
                }
            })*/
        }
    }
    private fun showCustomVideoMenu(
        anchorView: View,
        localVideo: LocalVideo
    ) {
        val context = anchorView.context

        val popupView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_bottom_menu, null, false)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 12f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        popupView.findViewById<ConstraintLayout>(R.id.cl_open).setOnClickListener {
            popupWindow.dismiss()
            startVideo(localVideo)
        }

        popupView.findViewById<ConstraintLayout>(R.id.cl_rename).setOnClickListener {
            popupWindow.dismiss()

            showRenameVideoDialog(
                context,
                appUtil,
                localVideo.name,
                View.OnClickListener { v ->
                    val editText = v as EditText
                    val newName = editText.text.toString().trim()

                    if (newName.isNotEmpty()) {
                        videoViewModel.renameVideo(
                            editText.context,
                            localVideo.uri,
                            File(newName).nameWithoutExtension + ".mp4"
                        )
                    }
                }
            )
        }

        popupView.findViewById<ConstraintLayout>(R.id.cl_share).setOnClickListener {
            popupWindow.dismiss()

            CoroutineScope(Dispatchers.IO).launch {
                val file = activity?.let {
                    getFilePathFromContentUri(localVideo.uri, it)?.let { path ->
                        File(path)
                    }
                }

                withContext(Dispatchers.Main) {
                    file?.let { shareVideo(it) }
                }
            }
        }

        popupView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popupWidth = popupView.measuredWidth

        popupWindow.showAsDropDown(
            anchorView,
            -popupWidth + anchorView.width,
            8
        )
    }

    private fun showPopupMenu(view: View, video: LocalVideo) {
        // Wrap the context with your custom style
        val wrapper = ContextThemeWrapper(view.context,R.style.PopupMenuStyle)
        val popupMenu = PopupMenu(wrapper, view)
        popupMenu.menuInflater.inflate(R.menu.menu_video, popupMenu.menu)
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { arg0 ->
            when (arg0.itemId) {
                R.id.item_rename -> {
                    showRenameVideoDialog(view.context, appUtil, video.name,
                        View.OnClickListener { v ->
                            with(v as EditText) {
                                val newName = v.text.toString().trim()
                                videoViewModel.renameVideo(v.context, video.uri, File(newName).nameWithoutExtension + ".mp4")
                            }
                        })
                    false
                }
                R.id.item_open_with -> {
                    startVideo(video)
                    false
                }

//                R.id.item_delete -> {
//                    context?.let { videoViewModel.deleteVideo(it, video) }
//                    true
//                }

                R.id.item_share -> {
                    requireContext().let {
                        video.uri.let { it1 ->
                            getFilePathFromContentUri(it1, it)?.let { it1 ->
                                File(it1)
                            }?.let { it2 -> shareVideo(it2) }
                        }
                    }
                    false
                }
                R.id.item_open_in_folder -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun startVideo(localVideo: LocalVideo) {
        CoroutineScope(Dispatchers.Main).launch {
                if (!downloaderShown) {
                    activity?.let {
                        activity?.let { forFragment ->
                            showInterstitialHome(activity = forFragment) {
                                navigate(localVideo,false)
                            }
                        }
                    }
                } else {
                    navigate(localVideo,false)
                }
        }
    }

    private fun navigate(localVideo: LocalVideo,alreadyShown:Boolean){
        if(interHome!=null){
            interHome?.let {
                showInterstitial(false,it,requireActivity(),{
                    try {
                        var intent: Intent? = null
                        intent = Intent(requireContext(), Class.forName("com.video.avd.ui.player.PlayerVideoActivity"))
                        val bundle=Bundle()
                        bundle.putBoolean("isliveuri", true)
                        bundle.putBoolean("alreadyAdShown", alreadyShown)
                        bundle.putString("uri", localVideo.uri.toString())
                        intent.putExtras(bundle)
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },inter_videos)
            }
        }
        else{
            loadFallbackInterstitialAd(requireActivity(), requireActivity().resources.getString(R.string.Interstitial_Home_ID_High), requireActivity().resources.getString(R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                interHome=it
            },{
                interHome=it
            })
            try {
                var intent: Intent? = null
                intent = Intent(requireContext(), Class.forName("com.video.avd.ui.player.PlayerVideoActivity"))
                val bundle=Bundle()
                bundle.putBoolean("isliveuri", true)
                bundle.putBoolean("alreadyAdShown", alreadyShown)
                bundle.putString("uri", localVideo.uri.toString())
                intent.putExtras(bundle)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

    private fun shareVideo(videoFile: File) {
        if (videoFile.exists()) {
            requireContext().let {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "video/mp4"
                val videoUri: Uri = FileProvider.getUriForFile(
                    it,
                    "${it.packageName}.provider",
                    videoFile
                )
                shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ContextCompat.startActivity(
                    it,
                    Intent.createChooser(shareIntent, "Share video"),
                    null
                )
            }

        } else {
            // Handle case when the video file doesn't exist
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
        }
    }

    fun getFilePathFromContentUri(contentUri: Uri, context: Context): String? {
        var filePath: String? = null
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // Query the media store for the file path associated with the content URI
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                context.contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        filePath = cursor.getString(columnIndex)
                    }
                }
            } else {
                // For Android 10 and above, use openInputStream with contentResolver
                context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).use { inputStream ->
                        val tempFile = File(
                            context.cacheDir,
                            "shared_video_${System.currentTimeMillis()}.mp4"
                        )
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        filePath = tempFile.absolutePath
                    }
                }
            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
        return filePath
    }


}
