package com.video.avd.utils.chromecast


import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.video.avd.R
import com.video.avd.ui.player.ChromeCastDelegate
import com.video.avd.ui.player.ChromeCastDelegateImp
import com.video.avd.ui.player.chromecastplaylist.ChromeCastPlaylist
import com.video.avd.ui.player.chromecastplaylist.ChromeCastPlaylistAdapter
import com.video.avd.ui.videos.model.Video
import com.video.avd.utils.AppUtils
import com.video.avd.utils.chromecast.constent.CastConstant


class ExpandedCControls : ExpandedControllerActivity(),
    ChromeCastDelegate by ChromeCastDelegateImp(),
    ChromeCastPlaylistAdapter.ChromeCastPlayListItemClickListener {
    var playListBottomSheetFragment: ChromeCastPlaylist? = null
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Log.d("Expandeddddd","being started")
        val customButtonView = getButtonImageViewAt(5)
        val myCustomUiController = MyCustomUIController(customButtonView)
        uiMediaController.bindViewToUIController(customButtonView, myCustomUiController)
        val remoteMediaClient = ChromeCastDelegate.mChromecastConnection?.session?.remoteMediaClient
        if (remoteMediaClient?.mediaStatus?.playerState == MediaStatus.PLAYER_STATE_IDLE && remoteMediaClient?.mediaStatus?.idleReason == MediaStatus.IDLE_REASON_FINISHED){
            Log.d("finishedddd","yesssss")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
      //  isExpendedRunning = true
        Log.d("Expandeddddd","being started from menus")

        menuInflater.inflate(R.menu.expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_playlist -> {
                showPlayList()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showPlayList() {
        val list = ChromecastConnection.listofvideos
        val positsion = ChromecastConnection.position

        if (!list.isNullOrEmpty()) {
            playListBottomSheetFragment = ChromeCastPlaylist()
            playListBottomSheetFragment?.let {
                val bundle = Bundle()
                ChromeCastPlaylist.listvideos = list
                bundle.putInt(CastConstant.CHROME_CAST_PLAYSLIST_POSITION, positsion)
                it.arguments = bundle
                it.show(supportFragmentManager, "")
                it.setChromePlayItemClickListener(this)
            }
        }
    }

    override fun onItemClick(position: Int, list: List<Video>) {

        /*
               // videolistglobal = list
                updatePosition(position)
                AppPreference.saveChromeListPosition(this, position)
                loadRemoteMedia(this@ExpandedCControls as Activity)
        */


        val item = list[position]
        val isMp4 = item.contentUri?.let { uri ->
            AppUtils.isSupportedVideoFile(
                this, Uri.parse(uri)
            )
        }
        if (isMp4 == true) {
            mSelectedMedia = ArrayList(list)
            ChromecastConnection.position = position
            updateSelectedPosition(position)
            // loadRemoteMedia(this)
            loadRemoteMediaFromPlaylist(this)
            // startChromeCastConnectionfromList(list, this, position)

        } else {
            Toast.makeText(
                this, "sorry this file format is not supported by chromse cast", Toast.LENGTH_SHORT
            ).show()

        }
        playListBottomSheetFragment?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        //isExpendedRunning = false
        Log.d("Expandeddddd","being destroed")
    }
}