package com.example.data.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.data.model.BufferMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class SmartPlayerManager(private val context: Context) {

    companion object {
        private const val CACHE_SIZE_BYTES = 150L * 1024L * 1024L // 150 MB smart disk cache
        private const val USER_AGENT = "BloodersTv/2.0 (Linux; Android TV; ExoPlayer)"

        @Volatile
        private var simpleCacheInstance: SimpleCache? = null

        @Synchronized
        fun getCache(context: Context): SimpleCache {
            if (simpleCacheInstance == null) {
                val cacheDir = File(context.cacheDir, "iptv_media_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
                simpleCacheInstance = SimpleCache(cacheDir, evictor)
            }
            return simpleCacheInstance!!
        }

        fun getCacheSizeBytes(context: Context): Long {
            val cacheDir = File(context.cacheDir, "iptv_media_cache")
            return if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else 0L
        }

        fun clearCache(context: Context) {
            try {
                val cacheDir = File(context.cacheDir, "iptv_media_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                }
            } catch (_: Exception) {}
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private val progressScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var currentBufferMode: BufferMode = BufferMode.SMOOTH_LOW_SPEED
    private var currentStreamUrl: String? = null

    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            buildPlayer(currentBufferMode)
        }
        return exoPlayer!!
    }

    fun setBufferMode(bufferMode: BufferMode) {
        if (currentBufferMode != bufferMode) {
            currentBufferMode = bufferMode
            val wasPlaying = exoPlayer?.isPlaying == true
            val currentUrl = currentStreamUrl
            val pos = exoPlayer?.currentPosition ?: 0L
            releasePlayer()
            buildPlayer(bufferMode)
            if (currentUrl != null) {
                playStream(currentUrl, pos, wasPlaying)
            }
        }
    }

    private fun buildPlayer(bufferMode: BufferMode) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferMode.minBufferMs,
                bufferMode.maxBufferMs,
                bufferMode.playbackBufferMs,
                bufferMode.playbackBufferMs * 2
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isBuffering.value = playbackState == Player.STATE_BUFFERING
                        _isPlaying.value = playbackState == Player.STATE_READY && playWhenReady

                        if (playbackState == Player.STATE_READY) {
                            _errorMessage.value = null
                            val d = this@apply.duration
                            _duration.value = if (d > 0) d else 0L
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _errorMessage.value = error.localizedMessage ?: "Error de reproducción. Reintentando..."
                        // Auto retry once for transient network drop
                        prepare()
                        play()
                    }
                })
            }
    }

    fun playStream(url: String, startPositionMs: Long = 0L, autoPlay: Boolean = true) {
        currentStreamUrl = url
        _errorMessage.value = null
        val player = getPlayer()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .build()

        player.setMediaItem(mediaItem, startPositionMs)
        player.prepare()
        player.playWhenReady = autoPlay
        _currentPosition.value = startPositionMs
        startProgressUpdates()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = progressScope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null) {
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    val d = player.duration
                    if (d > 0L) _duration.value = d
                    _isPlaying.value = player.isPlaying
                }
                delay(500)
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun stop() {
        exoPlayer?.stop()
        _isPlaying.value = false
        _isBuffering.value = false
    }

    fun releasePlayer() {
        progressJob?.cancel()
        progressJob = null
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
        _isBuffering.value = false
    }
}
