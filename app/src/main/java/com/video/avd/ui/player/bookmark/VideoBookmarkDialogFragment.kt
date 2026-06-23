package com.video.avd.ui.player.bookmark

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.TimeBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.video.avd.R
import com.video.avd.databinding.DialogVideoBookmarkBinding
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.player.PlayerViewModel
import com.video.avd.utils.CustomAlertDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoBookmarkDialogFragment : DialogFragment(), VideoBookmarkItemClickListener {

    private var binding: DialogVideoBookmarkBinding? = null
    private lateinit var mViewModel: PlayerViewModel
    private var bookmarkRemoveListener: BookmarkRemoveListener? = null
    private var observeDuration = true
    var mActivity: FragmentActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogVideoBookmarkBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = this
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            mViewModel = ViewModelProvider(activity)[PlayerViewModel::class.java]
            clickListeners()
            setBookmarkAdapter(activity)
            setExoProgress(activity)
        }
    }


    private fun setBookmarkAdapter(activity: FragmentActivity) {

        lifecycleScope.launch {
            val positions = ArrayList<Long>()
            mViewModel.getVideoBookmarksByUri(PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri.toString())
                ?.collectLatest { list ->

                    val sortedList = list.sortedBy { it.position }

                    val adapter = VideoBookmarkAdapter(sortedList, this@VideoBookmarkDialogFragment)
                    binding?.rvVideoBookmark?.layoutManager = LinearLayoutManager(activity)
                    binding?.rvVideoBookmark?.adapter = adapter

                    positions.clear()
                    list.forEach {
                        positions.add(it.position)
                    }
                    val duration = PlayerVideoActivity.player?.duration
                    binding?.exoProgress?.setBookmarkPositions(positions, duration ?: 1000)
                }
        }

    }

    private fun setExoProgress(activity: FragmentActivity) {
        if (PlayerVideoActivity.player?.isPlaying == true) {
            binding?.exoPlayPause?.let {
                Glide.with(activity).load(R.drawable.ic_bookmark_dg_pause).into(it)
            }
        } else {
            binding?.exoPlayPause?.let {
                Glide.with(activity).load(R.drawable.ic_bookmark_dg_play).into(it)
            }
        }

        val currentPosition = PlayerVideoActivity.player?.currentPosition
        val duration = PlayerVideoActivity.player?.duration

        binding?.exoProgress?.setDuration(duration ?: 1000)
        binding?.exoProgress?.setPosition(currentPosition ?: 1000)
        binding?.exoDuration?.text = formatDuration(duration ?: 1000)
        binding?.exoPosition?.text = formatDuration(currentPosition ?: 1000)

        CoroutineScope(Dispatchers.Main).launch {
            while (observeDuration) {

                val currentPosition = PlayerVideoActivity.player?.currentPosition
                val duration = PlayerVideoActivity.player?.duration

                //binding?.exoProgress?.setDuration(duration?:1000)
                // binding?.exoDuration?.text= formatDuration(duration?:1000)
                binding?.exoProgress?.setPosition(currentPosition ?: 1000)
                binding?.exoPosition?.text = formatDuration(currentPosition ?: 1000)
                delay(1000)
            }
        }

        binding?.exoProgress?.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                // Handle scrub start if needed
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                binding?.exoPosition?.text = formatDuration(position)
                PlayerVideoActivity.player?.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                PlayerVideoActivity.player?.seekTo(position)
            }
        })
    }

    override fun onVideoBookmarkClick(
        item: VideoBookmark,
        position: Int,
        which: String,
        anchorView: View
    ) {
        mActivity?.let { activity ->
            when (which) {
                "delete" -> {
                    deleteBookmark(item)
                }
                "rename" -> {
                    showRenameDialogue(item)
                }
                else -> {
                    PlayerVideoActivity.player?.seekTo(item.position)
                    binding?.exoProgress?.setPosition(item.position)
                    binding?.exoPosition?.text = formatDuration(item.position)
                }
            }
        }
    }

    private fun clickListeners() {

        binding?.clButton?.setOnClickListener {
            val position = PlayerVideoActivity.player?.currentPosition
            val timeStamp = System.currentTimeMillis()
            val currentVideoUri =
                PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri.toString()
            val bookmark = VideoBookmark(currentVideoUri, position ?: 0, timeStamp)
            mViewModel.addVideoBookmark(bookmark)
        }

        binding?.exoPlayPause?.setOnClickListener {
            mActivity?.let { activity ->
                if (PlayerVideoActivity.player?.isPlaying == true) {
                    binding?.exoPlayPause?.let {
                        Glide.with(activity).load(R.drawable.ic_bookmark_dg_play).into(it)
                    }
                    PlayerVideoActivity.player?.pause()
                } else {
                    binding?.exoPlayPause?.let {
                        Glide.with(activity).load(R.drawable.ic_bookmark_dg_pause).into(it)
                    }
                    PlayerVideoActivity.player?.play()
                }
            }

        }

        binding?.exoProgress?.setOnMarkerClickListener { pos ->
            PlayerVideoActivity.player?.seekTo(pos)
            binding?.exoProgress?.setPosition(pos)
        }

        binding?.btnBack?.setOnClickListener {
            dismiss()
        }
    }

    private fun deleteBookmark(item: VideoBookmark) {
        lifecycleScope.launch {
            // Delete the bookmark
            mViewModel.deleteVideoBookmark(
                PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri.toString(),
                item.timeStamp
            )
            delay(100)
            // Fetch updated bookmarks and refresh the CustomTimeBar
            val updatedBookmarks = mViewModel.getVideoBookmarksByUri(
                PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri.toString()
            )?.firstOrNull() ?: emptyList()

            binding?.exoProgress?.removeMarker(item.position)
            val duration = PlayerVideoActivity.player?.duration
            binding?.exoProgress?.setBookmarkPositions(
                updatedBookmarks.map { it.position },
                duration ?: 1000
            )

            bookmarkRemoveListener?.onBookmarkRemove()
        }

    }

    /*private fun showRenameDialogueOld(item: VideoBookmark) {
        mActivity?.let { activity ->
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.rename_dailog_bookmark, null)
            val btnCancel = view.findViewById<TextView>(R.id.tvCancell)
            val btnOk = view.findViewById<TextView>(R.id.tvOk)
            val editText = view.findViewById<EditText>(R.id.etName)
            val bookmarkName =
                item.bookmarkName.ifEmpty { "Bookmark at ${formatDuration(item.position)}" }
            editText.setText(bookmarkName)
            editText.setSelection(editText.text.length)
            val builder = android.app.AlertDialog.Builder(mActivity)
            builder.setView(view)
            val alertDialog = builder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set the width to 80% of the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams

            alertDialog.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            btnOk.setOnClickListener {
                    val newName = editText.text.toString()
                    mViewModel.renameBookmark(PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri.toString(), item.timeStamp, newName)
                alertDialog.dismiss()
            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }
            alertDialog.show()
        }
    }*/

    private fun showRenameDialogue(item: VideoBookmark) {
        mActivity?.let { activity ->
            val inflater = LayoutInflater.from(mActivity)
            val view = inflater.inflate(R.layout.rename_dailog_bookmark, null)
            val btnCancel = view.findViewById<TextView>(R.id.tvCancell)
            val btnOk = view.findViewById<TextView>(R.id.tvOk)
            val editText = view.findViewById<EditText>(R.id.etName)

            val bookmarkName = item.bookmarkName.ifEmpty { "Bookmark at ${formatDuration(item.position)}" }

            editText.setText(bookmarkName)
            editText.setSelection(editText.text.length)
            val alertDialog= CustomAlertDialog(activity)
            alertDialog.setView(view)
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Set the width to 80% of the screen
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(alertDialog.window?.attributes)
            layoutParams.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            alertDialog.window?.attributes = layoutParams

            btnOk.setOnClickListener {
                val newName = editText.text.toString()
                mViewModel.renameBookmark(PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri.toString(), item.timeStamp, newName)
                alertDialog.dismiss()
               // hideKeyboard()


                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val view = requireActivity().currentFocus ?: requireActivity().window?.decorView ?: View(requireContext())
                // Clear focus BEFORE hiding the keyboard to ensure it doesn't reopen
                view.clearFocus()
                inputMethodManager?.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                Handler(Looper.getMainLooper()).postDelayed({
                  //  super.onDismiss(dialog)
                }, 50) // Small delay allows the keyboard to fully close before dismissing the dialog
            }
            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()

        }
    }

    fun setBookmarkRemoveListener(listener: BookmarkRemoveListener) {
        bookmarkRemoveListener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onResume() {
        super.onResume()
        mActivity?.let {
            if (dialog != null && dialog!!.window != null) {
                dialog!!.window!!.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
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

    override fun onDestroy() {
        super.onDestroy()
        observeDuration = false
        bookmarkRemoveListener?.onDialogDismiss()
    }

    private fun formatDuration(durationInMillis: Long): String {
        return try {
            val totalSeconds = durationInMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        } catch (e: Exception) {
            // Handle any potential exception and return a default value or error message
            "00:00"
        }
    }
}

interface BookmarkRemoveListener {
    fun onBookmarkRemove()

    fun onDialogDismiss()

}