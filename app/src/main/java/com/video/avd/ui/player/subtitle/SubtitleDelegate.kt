package com.video.avd.ui.player.subtitle

import android.content.Context
import androidx.media3.ui.SubtitleView
import com.video.avd.ui.player.PlayerViewModel
import com.video.avd.ui.videos.model.Video

interface SubtitleDelegate {

    fun showSubtitleDialog(listVides:List<Video>,context: Context, subtitleView: SubtitleView,viewModel: PlayerViewModel, hasSubtitle : Boolean,subtitleToggle: Boolean)

    fun setSubTitle(listVides:List<Video>,path : String, subtitleView : SubtitleView, position : Long)

    var currentVideoTitle : String
}