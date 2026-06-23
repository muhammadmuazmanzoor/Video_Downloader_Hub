package com.video.avd.ui.player.chromecastplaylist

import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.avd.databinding.FragmentChromeCastPlaylistBinding
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.chromecast.constent.CastConstant.CHROME_CAST_PLAYSLIST_POSITION


class ChromeCastPlaylist : BottomSheetDialogFragment(),
    ChromeCastPlaylistAdapter.ChromeCastPlayListItemClickListener {
    private var binding: FragmentChromeCastPlaylistBinding? = null
    private var currentPosition = 0
    private var adapter: ChromeCastPlaylistAdapter? = null
    private var listener: ChromeCastPlaylistAdapter.ChromeCastPlayListItemClickListener? = null

    companion object {
        var listvideos = listOf<Video>()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChromeCastPlaylistBinding.inflate(
            inflater, container, false
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = this.arguments
        currentPosition = bundle?.getInt(CHROME_CAST_PLAYSLIST_POSITION) ?: 0
        Log.d("positionss","$currentPosition  in adapter")
        adapter = listener?.let {
            ChromeCastPlaylistAdapter(requireContext(), listvideos, currentPosition,
                it
            )
        }
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.rvPlaylist?.layoutManager = layoutManager
        binding?.rvPlaylist?.adapter = adapter
    }

    override fun onItemClick(position: Int, list: List<Video>) {
      dismiss()
    }

    fun setChromePlayItemClickListener(listener: ChromeCastPlaylistAdapter.ChromeCastPlayListItemClickListener) {
        this.listener = listener
    }

}