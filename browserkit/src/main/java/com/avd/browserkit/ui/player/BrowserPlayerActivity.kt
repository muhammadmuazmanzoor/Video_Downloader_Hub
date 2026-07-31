package com.avd.browserkit.ui.player

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.avd.browserkit.databinding.ActivityBrowserPlayerBinding
import java.io.File

class BrowserPlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBrowserPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(EXTRA_PATH).orEmpty()
        if (path.isBlank()) {
            finish()
            return
        }
        val uri = if (path.startsWith("content://")) Uri.parse(path) else Uri.fromFile(File(path))
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.playWhenReady = true
        }
        binding.btnClosePlayer.setOnClickListener { finish() }
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PATH = "extra_player_path"
    }
}
