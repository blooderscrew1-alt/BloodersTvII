package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.data.model.WatchHistoryItem

/**
 * Compact, non-lazy grid for "Continuar viendo".
 *
 * The watch history is small (max 8 cards here), so laying out rows manually
 * avoids a nested LazyVerticalGrid inside the page LazyColumn. That keeps
 * scrolling fluid on low-end devices and lets the card width adapt to phones,
 * tablets and TV.
 */
@Composable
fun ContinueWatchingGrid(
    history: List<WatchHistoryItem>,
    primaryColor: Color,
    onPlay: (WatchHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = remember(history) {
        history.sortedByDescending { it.lastWatchedTimestamp }
            .distinctBy { it.itemId }
            .take(8)
    }
    if (visible.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = (maxWidth / 170.dp).toInt().coerceIn(1, 5)
        val rows = visible.chunked(columns)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(primaryColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continuar viendo",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${visible.size} en historial",
                    color = Color(0xFF7F8F9F),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { entry ->
                        ContinueWatchingCard(
                            entry = entry,
                            primaryColor = primaryColor,
                            onClick = { onPlay(entry) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    entry: WatchHistoryItem,
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val percent = entry.progressPercent.coerceIn(0f, 1f)
    val remaining = (entry.durationMs - entry.positionMs).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1722))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) primaryColor else Color(0xFF1B2836),
                shape = RoundedCornerShape(10.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1A2A38), Color(0xFF101820))
                    )
                )
        ) {
            if (!entry.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.posterUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.title.take(1).uppercase(),
                        color = Color(0xFF4D6072),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x22000000),
                                Color(0xE6000000)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (entry.type == ItemType.SERIES) "SERIE" else "PELÍCULA",
                    color = Color(0xFFB8C6D3),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reanudar",
                    tint = Color.Black,
                    modifier = Modifier.size(19.dp)
                )
            }

            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp),
                color = primaryColor,
                trackColor = Color(0x66000000)
            )
        }

        Text(
            text = entry.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp)
        )

        Text(
            text = when {
                percent >= 0.99f -> "Terminado · toca para repetir"
                entry.positionMs > 0L -> "${(percent * 100).toInt()}% · ${formatRemaining(remaining)} restantes"
                else -> "Empezar"
            },
            color = Color(0xFF8F9DAA),
            fontSize = 9.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 1.dp, bottom = 7.dp)
        )
    }
}

private fun formatRemaining(ms: Long): String {
    if (ms <= 0L) return "0 min"
    val totalMinutes = (ms / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}
