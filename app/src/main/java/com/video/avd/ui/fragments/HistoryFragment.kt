package com.video.avd.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.avd.util.AdBlockerHelper
import com.avd.util.ads.InterstitialManagerA.showInterstitialHome
import com.video.avd.R
import com.video.avd.adapter.WatchHistoryRecyclerView
import com.video.avd.constent.GRID_ITEM_SPAN_COUNT
import com.video.avd.constent.VIEW_TYPE
import com.video.avd.constent.isClickedForCasting
import com.video.avd.constent.shouldUpdateRecyclerView
import com.video.avd.constent.videolistglobal
import com.video.avd.databinding.FragmentHistoryBinding
import com.video.avd.ui.MainActivityViewModel
import com.video.avd.ui.player.ChromeCastDelegate
import com.video.avd.ui.player.ChromeCastDelegateImp
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.GlobalValues
import com.video.avd.utils.GridSpacingItemDecoration
import com.video.avd.utils.NetworkUtils
import com.video.avd.utils.chromecast.ChromecastConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HistoryFragment : Fragment(), ChromeCastDelegate by ChromeCastDelegateImp(),
    WatchHistoryRecyclerView.OnHistoryCardClickListener {

    private var binding: FragmentHistoryBinding? = null
    var mActivity: FragmentActivity? = null
    private val viewModel: MainActivityViewModel by activityViewModels()
    var isAdAvailable = false

    var adapterHistory: WatchHistoryRecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(
            inflater,
            container,
            false
        )
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shouldUpdateRecyclerView.value = false
        mActivity?.let { activity ->
            AppUtils.firebaseUserAction("onViewCreated_HistoryFragment", "HistoryFragment")
            setAdapter(activity)
            viewLifecycleOwner.lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    getVideosList(activity) { videosList ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (videosList.isNotEmpty()) {
                                binding?.recyclerHistory?.visibility = View.VISIBLE
                                binding?.clearall?.visibility = View.VISIBLE
                                adapterHistory?.updateList(videosList)
                                adapterHistory?.notifyDataSetChanged()
                            } else {
                                binding?.recyclerHistory?.visibility = View.GONE
                                binding?.clearall?.visibility = View.INVISIBLE
//                                mActivity?.resources?.getColor(R.color.text_color_1)?.let { it1 -> binding?.clearall?.setTextColor(it1) }
                            }
                        }

                    }
                }
            }
        }
        castWaitObserver()
        binding?.clearall?.setOnClickListener {
            lifecycleScope.launch {
                mActivity?.resources?.getColor(R.color.text_color_1)?.let { it1 -> binding?.clearall?.setTextColor(it1) }
                viewModel.clearAllRecentVideos()
            }
        }
        GlobalValues.is24hourEnabled.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it) {
                    adapterHistory?.isNativeAdLoaded = false
                    binding?.recyclerHistory?.adapter?.notifyDataSetChanged()
                }
            }
        }
    }
    private fun castWaitObserver() {
        isClickedForCasting.observe(viewLifecycleOwner) {
            binding?.videoProgress?.visibility = if (it) View.VISIBLE else View.GONE
        }
    }
    private fun setAdapter(activity: FragmentActivity) {
        adapterHistory = WatchHistoryRecyclerView(emptyList(), activity, this@HistoryFragment)
        VIEW_TYPE.observe(viewLifecycleOwner) {
            val currentState = binding?.recyclerHistory?.layoutManager?.onSaveInstanceState()
            if (it == 0) {
                binding?.recyclerHistory?.layoutManager = LinearLayoutManager(
                    activity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            } else {
                var itemgrid = GridLayoutManager(requireContext(), GRID_ITEM_SPAN_COUNT)
                itemgrid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val viewType: Int? = adapterHistory?.getItemViewType(position)
                        return if (viewType == adapterHistory?.VIEW_TYPE_AD) {
                            // Make ad items span across all columns, or adjust as needed
                            GRID_ITEM_SPAN_COUNT
                        } else {
                            // Content items span 1
                            1
                        }
                    }
                }
                binding?.recyclerHistory?.layoutManager = itemgrid
            }
            binding?.recyclerHistory?.recycledViewPool?.clear()
            binding?.recyclerHistory?.layoutManager?.onRestoreInstanceState(currentState)
            for (i in 0 until binding?.recyclerHistory?.itemDecorationCount!!) {
                binding?.recyclerHistory?.removeItemDecorationAt(i)
            }
            binding?.recyclerHistory?.addItemDecoration(GridSpacingItemDecoration(20))
            binding?.recyclerHistory?.adapter = adapterHistory
        }

        if (NetworkUtils.isOnline(activity) && AdBlockerHelper.isProVersion.value != true) {
            if (GlobalValues.is24hourEnabled.value == false) {
                adapterHistory?.loadNativeAd(activity)
            }
        }

    }

    private fun getVideosList(
        activity: FragmentActivity,
        listPrepared: (List<Video>) -> Unit
    ) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val videosListFlow = viewModel.getEntitiesWithUpdatedTimeStump()
                videosListFlow?.collectLatest { videos ->
                    // Do something with the list of videos
                    if (videos.isEmpty()) {
                        binding?.noVideos?.visibility = View.VISIBLE
                        binding?.noVideosImg?.visibility = View.VISIBLE
                        listPrepared(emptyList())
                        Log.d("historyfragment","ïf empty part is executed ")
                    } else {
                        binding?.noVideos?.visibility = View.GONE
                        binding?.noVideosImg?.visibility = View.GONE
                        withContext(Dispatchers.IO) {
                            placeAdsInList(
                                activity,
                                videos.toMutableList()
                            ) { adsAvailable, videosList ->
                                isAdAvailable = adsAvailable
                                if (videosList.isNotEmpty()) {
                                    listPrepared(videosList)
                                    Log.d("historyfragment","listPrepared is executed ${videosList.size} ")
                                }
                            }
                        }
                        Log.d("historyfragment","ïf else part is executed ")
                    }
                }
            }
        }
    }

    private fun placeAdsInList(
        activity: FragmentActivity,
        videos: MutableList<Video>,
        listWithAds: (Boolean, List<Video>) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            listWithAds(false, videos)
        }
    }

    override fun onDeleteClick(entities: Video) {
        AppUtils.firebaseUserAction("onDeleteClick_HistoryFragment", "HistoryFragment")
        lifecycleScope.launch {
            viewModel.deleteVideoFromDb(entities) { deleted ->
                if (deleted) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding?.recyclerHistory?.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCardClick(position: Int, entities: List<Video>) {
        if (PlayerVideoActivity.getInstance() != null && PlayerVideoActivity.isPipMode) {
            PlayerVideoActivity.getInstance()?.finishAndRemoveTask()
        }
        mActivity?.let { activity ->
            showInterstitialHome(activity = activity) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Log.d("videoposition", "pos is $position")
                    AppUtils.firebaseUserAction("onCardClick_HistoryFragment", "HistoryFragment")
                    videolistglobal = emptyList()
                    videolistglobal = ArrayList(entities)
                    if (ChromeCastDelegate.mChromecastConnection?.isChromeCastConnect == true) {
                        setupChromecastConnection(entities, position)
                    } else {
                        val result = Bundle()
                        position?.let {
                            result.putString("id", it.toString())
                            result.putBoolean("isliveuri", false)
                            result.putString("uri", "")
                            result.putBoolean("isPlaybackCount", true)
                            result.putString("fragmentName", getString(R.string.history))
                            mActivity?.let { activity ->
                                val intent = Intent(activity, PlayerVideoActivity::class.java)
                                intent.putExtras(result)
                                try {
                                    activity.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                }
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

    private fun setupChromecastConnection(entities: List<Video>, position: Int) {
        try {
            val item = entities[position]
            val isMp4 = item.contentUri?.let { uri ->
                mActivity?.let {
                    AppUtils.isSupportedVideoFile(
                        it,
                        Uri.parse(uri)
                    )
                }
            }
            if (isMp4 == true) {
                if (binding?.videoProgress?.visibility == View.VISIBLE) {
                    Log.d("loadingStatus", "happening")
                } else {
                    isClickedForCasting.value = true
                    mSelectedMedia = entities as java.util.ArrayList<Video>
                    ChromecastConnection.position = position
                    updateSelectedPosition(position)
                    loadRemoteMediaFromPlaylist(mActivity as Activity)
                }
            } else {
                Toast.makeText(
                    mActivity,
                    "sorry this file format is not supported by chromse cast",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}