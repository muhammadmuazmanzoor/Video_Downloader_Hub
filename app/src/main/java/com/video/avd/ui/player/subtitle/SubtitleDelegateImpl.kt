package com.video.avd.ui.player.subtitle

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.text.Cue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.obsez.android.lib.filechooser.ChooserDialog
import com.video.avd.R
import com.video.avd.ui.player.PlayerVideoActivity
import com.video.avd.ui.player.PlayerViewModel
import com.video.avd.ui.player.subtitle.SubtitleDialog.Companion.hasSubtitledg
import com.video.avd.ui.player.subtitle.SubtitleDialog.Companion.subtitleTurnOn
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppVaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SubtitleDelegateImpl : SubtitleDelegate {

    override var currentVideoTitle = ""

    override fun showSubtitleDialog(listVides:List<Video>,context: Context, subtitleView: SubtitleView,viewModel: PlayerViewModel, hasSubtitle: Boolean,subtitleToggle: Boolean) {

        val dg = SubtitleDialog()
        val fragmentManager: FragmentManager = (context as FragmentActivity).supportFragmentManager
        dg.isCancelable=true
        hasSubtitledg=hasSubtitle
        subtitleTurnOn=subtitleToggle
        dg.show(fragmentManager, "tag")
        dg.setSelected(object  : SubtitleDialog.SubTitleClickListener {
            override fun onClick(which: String?) {
                if (which=="offline"){
                    Log.e("SearchDialog", "Offline Click")
                    dg.dismiss()
                    if (Build.VERSION.SDK_INT >= 31){
                        dismissAllDialogs(subtitleView.context)
                     //   viewModel.playbackPosition= PlayerVideoActivity.player?.currentPosition!!
                     //   setSubTitle(PlayerVideoActivity.finalUri,subtitleView, viewModel.playbackPosition)
                    }else{
                        PlayerVideoActivity.isShowFileChooser = true
                        ChooserDialog(context)
                            .withFilter(false, false, "srt")
                            .withResources(R.string.title_choose_file, R.string.title_choose, R.string.dialog_cancel)
                            .withChosenListener(ChooserDialog.Result {
                                    path: String, pathFile: File? ->
                                viewModel.newPos= PlayerVideoActivity.player?.currentPosition!!
//                                dismissAllDialogs(subtitleView.context)
                                setSubTitle(listVides,path,subtitleView, viewModel.newPos)

                                PlayerVideoActivity.isShowFileChooser = false
                                Log.e("SearchDialog", "Set Subtitle")
                                val subtitleState=SubtitleState(videoId = viewModel.currentVideo?.id?:0, subtitlePath = path, hasSubtitle = true, toggle = true)
                                viewModel.insertSubtitleWithVideoId(subtitleState)
                                viewModel.getSubtitleFromDB()
                                viewModel.checkIFSubtitleTurnOn()
                                Log.e("SearchObserver", "Show Customization: subtitlePath: $path")
                                Log.e("SearchObserver", "Show Customization: subtitlePath: delegate")
                             /*   val searchDialog = SearchSubtitleDialog.newInstance(currentVideoTitle)
                                searchDialog.dismiss()*/
                            })
                            .withOnCancelListener(object : DialogInterface.OnCancelListener{
                                override fun onCancel(p0: DialogInterface?) {
                                    PlayerVideoActivity.isShowFileChooser = false
                                }
                            })
                            .withNegativeButtonListener { dialogInterface, i ->
                                PlayerVideoActivity.isShowFileChooser = false
                            }
                            .build().show()
                    }

                }
                else if (which=="online"){
                    dg.dismiss()
                    val searchDialog = SearchSubtitleDialog.newInstance(currentVideoTitle)
                    val fragmentManager: FragmentManager = (context as FragmentActivity).supportFragmentManager
                    searchDialog.show(fragmentManager,"")
                    searchDialog.setDownloadedListener(object : SearchSubtitleDialog.DownloadListener{
                        override fun isDownloaded(
                            isDownloaded: Boolean?,
                            filePath: String?
                        ) {
                            CoroutineScope(Dispatchers.Main).launch {
                                if (isDownloaded == true){
                                    if (filePath != null) {
                                        if (filePath.endsWith(".srt")){
                                            subtitleTurnOn=true
                                            hasSubtitledg=true
                                            Toast.makeText(context, "downloaded successfully", Toast.LENGTH_SHORT).show()
                                            PlayerVideoActivity.player?.currentPosition?.let {
                                                viewModel.newPos=it
                                            }
                                            val videoID=viewModel.currentVideo?.id
                                            viewModel.showSubtitleView.postValue(true)
                                            setSubTitle(listVides,filePath, subtitleView, viewModel.newPos)
                                            val subtitleState= videoID?.let { SubtitleState(videoId = it, subtitlePath = filePath, hasSubtitle = true, toggle = true) }
                                            if (subtitleState != null) {
                                                viewModel.insertSubtitleWithVideoId(subtitleState)
                                            }
                                            viewModel.getSubtitleFromDB()
                                            viewModel.checkIFSubtitleTurnOn()
                                            Log.e("SearchObserver", "Show Customization: subtitlePath: $filePath")
                                            Log.e("SearchObserver", "Show Customization: subtitlePath: delegate")
//                                            dismissAllDialogs(subtitleView.context)

                                        }else{
                                            searchDialog.dismissAllowingStateLoss()
                                            Toast.makeText(context, "file not supported", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    else{

                                        searchDialog.dismissAllowingStateLoss()
                                    }
                                } else {
                                    searchDialog.dismissAllowingStateLoss()
                                    Toast.makeText(context, "error occurred", Toast.LENGTH_SHORT).show()
                                }
                            }

                        }
                    })
                }
                else if (which=="toggle"){
                    viewModel.updateSubtitleState(videoId = viewModel.currentVideo?.id?:0, !viewModel.currentVideoSubtitleTurnOn)
//                    viewModel.checkIFSubtitleTurnOn()
                }
            }
        })

        dg.setSubtitleCustomizationListener(object :SubtitleCustomizationsListener{
            override fun onSetAlignment(alignment: String) {
                when(alignment){
                    "Lower"->    subtitleView.setBottomPaddingFraction(0.20f)
                    "Middle"->    subtitleView.setBottomPaddingFraction(0.50f)
                    "Upper"->    subtitleView.setBottomPaddingFraction(0.70f)
                    else ->    subtitleView.setBottomPaddingFraction(0.20f)
                }
            }

            override fun onSetTextSize(textSize: String) {
                val size = textSize.toFloat() + 15
                subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            }

            override fun onSetColor(colorCode: String) {
                val textColor=AppUtils.getSubtitleTextColor(colorCode)
                val style = CaptionStyleCompat(
                    textColor,  // Foreground text color
                    Color.TRANSPARENT,  // Background color
                    Color.TRANSPARENT,  // Window color
                    CaptionStyleCompat.EDGE_TYPE_NONE,  // Edge type
                    Color.BLACK,  // Edge color
                    Typeface.DEFAULT  // Typeface
                )
                subtitleView.setStyle(style)
            }

            override fun onSetTextShadow(position: String) {
                val cueBuilder = Cue.Builder()
                    .apply {
                        when (position.lowercase()) {
                            "top" -> onSetTextShadow(position.lowercase())
                            "bottom" -> onSetTextShadow(position.lowercase())
                            "center" -> onSetTextShadow(position.lowercase())
                            else -> {onSetTextShadow(position.lowercase())}
                        }
                    }
                    .build()
                subtitleView.setCues(listOf(cueBuilder))
            }
        })
    }
 /*  *//* override fun setSubTitle(path: String, subtitleView: SubtitleView, position : Long) {
        try {
            val subtitle = MediaItem.SubtitleConfiguration.Builder(path.toUri()).setMimeType(MimeTypes.APPLICATION_SUBRIP).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
          Log.d("wesa", "first ${path.toString()}")
            val assetVideoUri =  PlayerVideoActivity.player?.currentMediaItem?.localConfiguration?.uri
            val mediaItem = MediaItem.Builder().setUri(assetVideoUri).setSubtitleConfigurations(ImmutableList.of(subtitle)).build()
            PlayerVideoActivity.player?.setMediaItem(mediaItem, position)
            //setting customization values
            val sharedPref=AppVaultManager(subtitleView.context)
            val values=sharedPref.getSubtitleValues()
            val size = values?.size?.toFloat()?.plus(15)
            subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, size ?: 40F)
            when(values?.position){
                "Lower"->    subtitleView.setBottomPaddingFraction(0.20f)
                "Middle"->    subtitleView.setBottomPaddingFraction(0.50f)
                "Upper"->    subtitleView.setBottomPaddingFraction(0.70f)
                else ->    subtitleView.setBottomPaddingFraction(0.20f)
            }
            val textColor=AppUtils.getSubtitleTextColor(values?.textColor ?: "FFFFF")
            val style = CaptionStyleCompat(
                textColor,  // Foreground text color
                Color.TRANSPARENT,  // Background color
                Color.TRANSPARENT,  // Window color
                CaptionStyleCompat.EDGE_TYPE_NONE,  // Edge type
                Color.BLACK,  // Edge color
                Typeface.DEFAULT  // Typeface
            )
            subtitleView.setStyle(style)
        }catch (e:Exception){
            e.printStackTrace()
        }
    }*/
 private val mediaItemList = mutableListOf<MediaItem>()

    // Initialize this list based on videolistglobals
    fun initializeMediaItemList(videoList: List<Video>) {
        mediaItemList.clear()
        mediaItemList.addAll(videoList.map { video ->
            MediaItem.Builder()
                .setUri(video.contentUri?.toUri())
                .build()
        })
    }
    override fun setSubTitle(listVides:List<Video>, path: String, subtitleView: SubtitleView, position: Long) {
        try {
            Log.e("SearchDialog", "Set Subtitle fun")
            initializeMediaItemList(listVides)
            // Step 1: Build the subtitle configuration
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(path.toUri())
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            // Step 2: Update the media item at the current position
            val currentIndex = PlayerVideoActivity.player?.currentWindowIndex ?: 0
            val updatedMediaItem = mediaItemList[currentIndex].buildUpon()
                .setSubtitleConfigurations(listOf(subtitleConfig))
                .build()
            mediaItemList[currentIndex] = updatedMediaItem // Replace the item in the list

            // Step 3: Cache playback position and play state
            val playbackPosition = PlayerVideoActivity.player?.currentPosition ?: position
            val playWhenReady = PlayerVideoActivity.player?.playWhenReady ?: true

            // Step 4: Reset the playlist in ExoPlayer with the updated media item list
            PlayerVideoActivity.player?.clearMediaItems()
            PlayerVideoActivity.player?.setMediaItems(mediaItemList)
            PlayerVideoActivity.player?.seekTo(currentIndex, playbackPosition)
            PlayerVideoActivity.player?.playWhenReady = playWhenReady
            PlayerVideoActivity.player?.prepare()

            // Step 5: Customize subtitle view appearance
            val sharedPref = AppVaultManager(subtitleView.context)
            val values = sharedPref.getSubtitleValues()
            val size = values?.size?.toFloat()?.plus(15)
            subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, size ?: 40F)

            when (values?.position) {
                "Lower" -> subtitleView.setBottomPaddingFraction(0.20f)
                "Middle" -> subtitleView.setBottomPaddingFraction(0.50f)
                "Upper" -> subtitleView.setBottomPaddingFraction(0.70f)
                else -> subtitleView.setBottomPaddingFraction(0.20f)
            }

            val textColor = AppUtils.getSubtitleTextColor(values?.textColor ?: "FFFFF")
            val style = CaptionStyleCompat(
                textColor,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE,
                Color.BLACK,
                Typeface.DEFAULT
            )
            subtitleView.setStyle(style)
            dismissAllDialogs(subtitleView.context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissAllDialogs(context: Context) {
        try {
            // Ensure the context is a FragmentActivity to access the FragmentManager
            if (context is FragmentActivity) {
                val fragmentManager = context.supportFragmentManager

                // Iterate through all fragments
                for (fragment in fragmentManager.fragments) {
                    if (fragment is DialogFragment && fragment.isVisible()) {
                        (fragment as DialogFragment).dismissAllowingStateLoss()
                    }
                }
                Log.e("SearchDialog", "All visible dialogs dismissed")
            }
        } catch (e: java.lang.Exception) {
            Log.e("SearchDialog", "Error while dismissing dialogs: " + e.message)
            e.printStackTrace()
        }
    }


}