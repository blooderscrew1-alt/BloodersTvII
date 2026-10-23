package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.player.SmartPlayerManager
import com.example.data.repository.IptvRepository
import com.example.ui.components.VideoPlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun FullscreenPlayerScreen(
    currentItem: PlaylistItem,
    repository: IptvRepository,
    playerManager: SmartPlayerManager,
    onClose: () -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allItems by repository.items.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPos by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()

    var activeItem by remember { mutableStateOf(currentItem) }
    var showControls by remember { mutableStateOf(true) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    fun switchTo(item: PlaylistItem) {
        val resumePosition = if (item.type != ItemType.LIVE) {
            repository.history.value.firstOrNull { it.itemId == item.id }?.positionMs ?: 0L
        } else {
            0L
        }
        activeItem = item
        playerManager.playStream(item.streamUrl, resumePosition)
    }

    // Related channels in same category
    val channelList = remember(allItems, activeItem) {
        allItems.filter { it.type == activeItem.type && it.category == activeItem.category }
            .ifEmpty { allItems.filter { it.type == activeItem.type } }
    }

    // Auto record watch history periodically. Throttled so a 500 ms position
    // tick does not write SharedPreferences twice per second.
    LaunchedEffect(activeItem) {
        if (activeItem.type != ItemType.LIVE) {
            while (true) {
                delay(1500)
                if (duration > 0L) {
                    repository.recordWatchHistory(activeItem, currentPos, duration)
                }
            }
        }
    }

    // Auto hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4500)
            showControls = false
        }
    }

    // Handle back button
    BackHandler {
        if (showChannelDrawer) {
            showChannelDrawer = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            showControls = !showControls
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            // Fast Channel Next
                            val idx = channelList.indexOfFirst { it.id == activeItem.id }
                            if (idx != -1 && idx > 0) {
                                val next = channelList[idx - 1]
                                switchTo(next)
                            }
                            showControls = true
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            // Fast Channel Prev
                            val idx = channelList.indexOfFirst { it.id == activeItem.id }
                            if (idx != -1 && idx < channelList.size - 1) {
                                val prev = channelList[idx + 1]
                                switchTo(prev)
                            }
                            showControls = true
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            playerManager.togglePlayPause()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .testTag("fullscreen_player_container")
    ) {
        // Video Player View
        VideoPlayerView(
            playerManager = playerManager,
            resizeMode = resizeMode,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls (Matches Screenshot 2!)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAA000000),
                                Color.Transparent,
                                Color(0xDD000000)
                            )
                        )
                    )
            ) {
                // Top Bar: Back & Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = activeItem.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeItem.category,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                    }
                }

                // Center Play/Pause & Rewind/Forward for VOD
                if (activeItem.type != ItemType.LIVE) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { playerManager.seekTo(maxOf(0L, currentPos - 10_000L)) },
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000))
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Retroceder 10s", tint = Color.White)
                        }

                        IconButton(
                            onClick = { playerManager.togglePlayPause() },
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(primaryColor)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pausa",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { playerManager.seekTo(minOf(duration, currentPos + 10_000L)) },
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x66000000))
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Adelantar 10s", tint = Color.White)
                        }
                    }
                }

                // Bottom Controls Bar (Matches Screenshot 2!)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Continuous Red line for Live or Slider for VOD
                    if (activeItem.type == ItemType.LIVE) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(Color(0xFFE50914), RoundedCornerShape(2.dp))
                        )
                    } else {
                        Slider(
                            value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { percent ->
                                playerManager.seekTo((percent * duration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth().height(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: "• EN VIVO" + Channel Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        ) {
                            if (activeItem.type == ItemType.LIVE) {
                                Text(
                                    text = "• EN VIVO",
                                    color = Color(0xFFFF2A42),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            Text(
                                text = activeItem.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Horizontally scrollable action buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Lista de canales",
                                color = Color(0xFFC0CAD5),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { showChannelDrawer = !showChannelDrawer }
                                    .padding(end = 4.dp)
                            )

                            // Quick Channel List button with lime pill accent (Screenshot 2!)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (showChannelDrawer) primaryColor else primaryColor.copy(alpha = 0.9f))
                                    .clickable { showChannelDrawer = !showChannelDrawer },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = "Lista de canales",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Aspect Ratio Mode toggle (Fit / Zoom / Fill)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2833))
                                    .clickable {
                                        resizeMode = when (resizeMode) {
                                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Relación de aspecto",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // HD badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E2833))
                                    .padding(horizontal = 7.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "HD",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // PIP / Fullscreen Icon
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2833))
                                    .clickable { onClose() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Modo Ventana",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Side Drawer Channel List (Matches Screenshot 2 "Lista de canales")
        AnimatedVisibility(
            visible = showChannelDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(280.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00B1319))
                    .border(BorderStroke(1.dp, Color(0xFF1E2F3F)))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Canales (${channelList.size})",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        IconButton(onClick = { showChannelDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channelList) { ch ->
                            val isSelected = ch.id == activeItem.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) primaryColor else Color(0xFF131E28))
                                    .clickable {
                                        switchTo(ch)
                                        showChannelDrawer = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (ch.channelNumber ?: "").toString(),
                                    color = if (isSelected) Color.Black else Color(0xFF8F9DAA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(30.dp)
                                )
                                Text(
                                    text = ch.name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun BorderStroke(dp: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(dp, color)
