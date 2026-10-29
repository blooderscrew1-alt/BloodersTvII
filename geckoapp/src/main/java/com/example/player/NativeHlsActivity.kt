package com.example.player

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

/**
 * Native HLS player used as a fallback for old WebViews. It avoids loading
 * the provider's JavaScript at all.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class NativeHlsActivity : Activity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(playerView)
        if (title.isNotBlank()) {
            val label = TextView(this).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                setShadowLayer(6f, 0f, 2f, Color.BLACK)
                setPadding(28, 22, 28, 12)
            }
            root.addView(
                label,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.START
                )
            )
        }
        setContentView(root)

        val exo = ExoPlayer.Builder(this).build()
        player = exo
        playerView.player = exo
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 9) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"
            )
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://vimeos.net/",
                    "Origin" to "https://vimeos.net"
                )
            )
        val source = HlsMediaSource.Factory(http)
            .createMediaSource(MediaItem.fromUri(url))
        exo.setMediaSource(source)
        exo.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("NativeHls", "error", error)
            }
            override fun onPlaybackStateChanged(state: Int) {
                android.util.Log.d("NativeHls", "state=" + state)
            }
        })
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }
}
