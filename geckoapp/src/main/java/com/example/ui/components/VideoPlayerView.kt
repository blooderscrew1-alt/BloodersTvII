package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.player.SmartPlayerManager

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    playerManager: SmartPlayerManager,
    modifier: Modifier = Modifier,
    showNativeControls: Boolean = false,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    val context = LocalContext.current
    val isBuffering by playerManager.isBuffering.collectAsState()
    val errorMessage by playerManager.errorMessage.collectAsState()
    val isMp2Audio by playerManager.mp2Audio.collectAsState()

    val playerView = remember {
        PlayerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.useController = showNativeControls
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) // handled in compose
            setResizeMode(resizeMode)
            player = playerManager.getPlayer()
        }
    }

    DisposableEffect(resizeMode) {
        playerView.setResizeMode(resizeMode)
        onDispose { }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { playerView },
            modifier = Modifier.fillMaxSize()
        )

        if (isMp2Audio) {
            Text(
                text = "Audio MP2 · pantalla completa abre VLC",
                color = Color(0xFFFFD54F),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFCCFF00),
                    modifier = Modifier.size(42.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "Error de transmisión",
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp
                )
            }
        }
    }
}
