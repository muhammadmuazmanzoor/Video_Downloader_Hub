package com.video.avd.ui.allvideo

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.avd.R
import com.video.avd.databinding.FragmentVideoInfoBottomSheetBinding
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.hideNavigationBarFromDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class VideoInfoBottomSheetFragment  : BottomSheetDialogFragment() {

    private var mActivity: FragmentActivity? = null
    private var binding : FragmentVideoInfoBottomSheetBinding? =null
    private var video: Video? = null
    var videolist = listOf<Video>()
    var videoSize: String? = ""
    var dateAdded: String? = ""
    var albumName = ""
    var uri:String=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        if (bundle != null) {
            video = bundle.getSerializable("video") as? Video
            uri = bundle.getString("uri", "").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentVideoInfoBottomSheetBinding.inflate(inflater,container,false)
        dialog?.let { binding?.root?.let { it1 -> makeBottomSheetRounded(it1, it) } }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        video?.let {
            setUi(it)
        }
        mActivity.let {
            clicklistener()
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                v.setPadding(0, 0, 0, 0) // Remove padding for system bars
                WindowInsetsCompat.CONSUMED // Indicate that insets have been consumed
            }
        }
    }

    private fun setUi(item: Video) {
        lifecycleScope.launch {
            try {
                // Move blocking I/O operation to background thread to prevent ANR
                val path = withContext(Dispatchers.IO) {
                    mActivity?.let { AppUtils.getPathFromUri(it, Uri.parse(uri)) }
                }
                // Update UI on main thread
                binding?.apply {
                    binding?.titleDetail?.text = item.title
                    //  Albub_detail.text = item.albumName
                    binding?.ArtistDetail?.text = item.title
                    binding?.PathDetail?.text = path
                    videoSize = item.size
                    binding?.SizeDetail?.text = videoSize
                    val formattedDate = item.date
                    binding?.DateDetail?.text = formattedDate
                    val date = Date(item.date)
                    val formattime = getTimeFromDate(date)
                    binding?.TimeDetail?.text = formattime
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    fun getTimeFromDate(date: Date): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)// Define the desired time format
        return sdf.format(date)
    }

    private fun clicklistener() {
        binding?.editTrackOkay?.setOnClickListener {
            dialog?.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBarFromDialog()
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
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet = dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }


}