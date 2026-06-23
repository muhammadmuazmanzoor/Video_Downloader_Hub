package com.video.avd.ui.player.playlist

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.avd.R
import com.video.avd.databinding.BottomsheetPlaylistBinding
import com.video.avd.ui.player.OnVideoPlayerPlaylistOrderTypeChangeListner
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils


class PlaylistBottomSheetFragment : BottomSheetDialogFragment(),
    PlaylistAdapter.PlayListItemClickListener {
    private var binding: BottomsheetPlaylistBinding? = null
    private var adapter: PlaylistAdapter? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    var orderlistner: OnVideoPlayerPlaylistOrderTypeChangeListner? = null
    private var previousFragment = ""
    private var currentPosition = 0
    private var isFullScreen = false
    private var orderType = 0

    private var listener: PlaylistAdapter.PlayListItemClickListener? = null


    companion object {
        var listvideos = mutableListOf<Video>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        previousFragment = bundle?.getString("previousFragment", "").toString()
        currentPosition = bundle?.getInt("position") ?: 0
        isFullScreen = bundle?.getBoolean("isFullScreen") ?: false
        orderType = bundle?.getInt("video_order_type") ?: 0
    }

    fun setOrderTypeChangeListner(listner: OnVideoPlayerPlaylistOrderTypeChangeListner) {
        this.orderlistner = listner
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomsheetPlaylistBinding.inflate(inflater, container, false)
        binding?.root?.let { dialog?.let { it1 -> makeBottomSheetRoundTransparant(it, it1) } }
        binding?.tvPlaylistFoldername?.text = previousFragment
        adapter = PlaylistAdapter(requireContext(), this, currentPosition)
//        if (videolistglobal.isNullOrEmpty()) {
        adapter?.setData(listvideos)
//        } else {
//            adapter?.setData(videolistglobal)
//        }
        binding?.rvPlaylist?.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding?.rvPlaylist)

        binding?.icPlaylistBack?.setOnClickListener {
            listener?.onbackpresscalled()
        }
        initialOrderUi(orderType)
        orderClicks()

        return binding?.root
    }
    private val simpleCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // For drag-and-drop
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT  // For swipe
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            adapter?.moveItem(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            adapter?.removeItem(position)
        }
    }
    private fun orderClicks() {
        AppUtils.firebaseUserAction("repeatBtnClicked_videoplayer_playlist", "PlayerVideoActivity")
        binding?.orderImage?.setOnClickListener {
        binding?.let { binding ->
            orderType = (orderType + 1) % 4
            when (orderType) {
                0 -> {
                    binding.tvOrder.text = getString(R.string.order)
                    binding.orderImage.setImageResource(R.drawable.ic_order)
                }

                1 -> {
                    binding.tvOrder.text = getString(R.string.loop_all)
                    binding.orderImage.setImageResource(R.drawable.ic_repeatt)
                }

                2 -> {
                    binding.tvOrder.text = getString(R.string.suffle_all)
                    binding.orderImage.setImageResource(R.drawable.ic_shuffle_list)
                }

                3 -> {
                    binding.tvOrder.text = getString(R.string.repeat_current_video)
                    binding.orderImage.setImageResource(R.drawable.ic_repeat_single)
                }
            }
        }
        orderlistner?.onVideoOrderChanged(orderType)
    }  }

    private fun initialOrderUi(orderType: Int) {
        binding?.let { binding ->
            when (orderType) {

                0 -> {
                    binding.tvOrder.text = getString(R.string.order)
                    binding.orderImage.setImageResource(R.drawable.ic_order)
                }

                1 -> {
                    binding.tvOrder.text = getString(R.string.loop_all)
                    binding.orderImage.setImageResource(R.drawable.ic_loop_list)
                }

                2 -> {
                    binding.tvOrder.text = getString(R.string.suffle_all)
                    binding.orderImage.setImageResource(R.drawable.ic_shuffle_list)
                }

                3 -> {
                    binding.tvOrder.text = getString(R.string.repeat_current)
                    binding.orderImage.setImageResource(R.drawable.ic_repeat_single)
                }

                else -> getString(R.string.order)
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val bottomSheets = binding?.root
        // Get screen orientation
        val orientation = resources.configuration.orientation
        // Set custom animation based on orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait mode: Bottom to Top Animation
            try {
                bottomSheets?.let {
                    val layoutParams = it.layoutParams as FrameLayout.LayoutParams
                    layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT // Full height for portrait
                    layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT // Full width for portrait
                    it.layoutParams = layoutParams
                    // Set the BottomSheet offscreen (initial position below the screen)
                    ViewCompat.setTranslationY(it, it.height.toFloat())
                    it.animate().translationY(0f).duration = 300 // Move from bottom to top
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Landscape mode: Right to Left Animation
            try {
                dialog?.let { dialog ->
                    val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                    val widthInPixels = (400 * resources.displayMetrics.density).toInt() // Convert 400dp to pixels
                    bottomSheet?.layoutParams?.width = widthInPixels
                    // Set gravity to END
                    (bottomSheet?.layoutParams as? FrameLayout.LayoutParams)?.gravity = Gravity.END
                    (dialog as? Dialog)?.apply {
                        window?.setLayout(widthInPixels, ViewGroup.LayoutParams.MATCH_PARENT)
                        window?.setGravity(Gravity.END)
                    }
                    (dialog as? BottomSheetDialog)?.behavior?.apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        isFitToContents = false  // Allow the sheet to fill the screen even with no content
                        skipCollapsed = true     // Prevent the collapsed state
                        expandedOffset = 2       // Ensure it's fully expanded
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Re-run the animation logic when the configuration changes (e.g., orientation change)
        onStart()
    }
  /*  private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet =
                    dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bottom_sheet_background_black)
            }
        })
    }*/

    private fun makeBottomSheetRoundTransparant(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet =
                    dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bottom_sheet_background_black)
            }
        })
    }

    override fun onItemClick(position: Int, list: List<Video>) {
        listener?.onItemClick(position, list)
    }

    override fun onPlayingItemRemoved(video: Video) {
        //
//        Toast.makeText(requireContext(), "${video.title} is playing,can't removed", Toast.LENGTH_SHORT).show()
    }

    override fun onbackpresscalled() {
    }

    fun setPlayItemClickListener(listener: PlaylistAdapter.PlayListItemClickListener) {
        this.listener = listener
    }

}