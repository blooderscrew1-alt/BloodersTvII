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
import com.example.data.nsrplay.NsrHistoryItem
import com.example.data.nsrplay.NsrMediaType
import com.example.data.vimeus.VimeusHistoryItem
import com.example.data.vimeus.VimeusType

private data class CardEntry(
    val key: String,
    val title: String,
    val posterUrl: String?,
    val type: ItemType,
    val progress: Float?,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedTimestamp: Long,
    val nsr: NsrHistoryItem? = null,
    val vimeus: VimeusHistoryItem? = null,
    val iptv: WatchHistoryItem? = null
) {
    val providerLabel: String?
        get() = when {
            nsr != null -> "NSRPLAY"
            vimeus != null -> "VIMEUS"
            else -> null
        }
}

/**
 * Compact, non-lazy grid for "Continuar viendo".
 *
 * IPTV entries keep their playback position. NSRPlay entries are included too
 * (without a minute mark, because the provider's web player does not expose
 * one); tapping them reopens their NSRPlay detail/server list.
 *
 * The history is small (max 8 cards here), so laying out rows manually avoids a
 * nested LazyVerticalGrid inside the page LazyColumn. That keeps scrolling
 * fluid on low-end devices and lets the card width adapt to phones, tablets
 * and TV.
 */
@Composable
fun ContinueWatchingGrid(
    iptvHistory: List<WatchHistoryItem>,
    nsrHistory: List<NsrHistoryItem>,
    vimeusHistory: List<VimeusHistoryItem>,
    primaryColor: Color,
    onPlayIptv: (WatchHistoryItem) -> Unit,
    onPlayNsr: (NsrHistoryItem) -> Unit,
    onPlayVimeus: (VimeusHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = remember(iptvHistory, nsrHistory, vimeusHistory) {
        val merged = ArrayList<CardEntry>(iptvHistory.size + nsrHistory.size + vimeusHistory.size)
        iptvHistory.forEach { entry ->
            merged.add(
                CardEntry(
                    key = "iptv:${entry.itemId}",
                    title = entry.title,
                    posterUrl = entry.posterUrl,
                    type = entry.type,
                    progress = entry.progressPercent.coerceIn(0f, 1f),
                    positionMs = entry.positionMs,
                    durationMs = entry.durationMs,
                    lastWatchedTimestamp = entry.lastWatchedTimestamp,
                    iptv = entry
                )
            )
        }
        nsrHistory.forEach { entry ->
            merged.add(
                CardEntry(
                    key = entry.watchKey,
                    title = entry.title,
                    posterUrl = entry.posterUrl,
                    type = if (entry.type == NsrMediaType.SERIES) ItemType.SERIES else ItemType.MOVIE,
                    progress = null,
                    positionMs = 0L,
                    durationMs = 0L,
                    lastWatchedTimestamp = entry.lastWatchedTimestamp,
                    nsr = entry
                )
            )
        }
        vimeusHistory.forEach { entry ->
            merged.add(
                CardEntry(
                    key = entry.watchKey,
                    title = entry.title,
                    posterUrl = entry.posterUrl,
                    type = if (entry.type == VimeusType.MOVIE) ItemType.MOVIE else ItemType.SERIES,
                    progress = null,
                    positionMs = 0L,
                    durationMs = 0L,
                    lastWatchedTimestamp = entry.lastWatchedTimestamp,
                    vimeus = entry
                )
            )
        }
        merged.sortedByDescending { it.lastWatchedTimestamp }
            .distinctBy { it.key }
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
                            onClick = {
                                when {
                                    entry.nsr != null -> onPlayNsr(entry.nsr)
                                    entry.vimeus != null -> onPlayVimeus(entry.vimeus)
                                    else -> entry.iptv?.let(onPlayIptv)
                                }
                            },
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
    entry: CardEntry,
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val percent = entry.progress?.coerceIn(0f, 1f)
    val remaining = (entry.durationMs - entry.positionMs).coerceAtLeast(0L)
    val provider = entry.providerLabel
    val isProvider = provider != null

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
                    .background(
                        when (provider) {
                            "NSRPLAY" -> Color(0xFF00E5FF)
                            "VIMEUS" -> Color(0xFFFF9F1C)
                            else -> Color(0xCC000000)
                        }
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = provider ?: if (entry.type == ItemType.SERIES) "SERIE" else "PELÍCULA",
                    color = if (isProvider) Color.Black else Color(0xFFB8C6D3),
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
                    contentDescription = if (isProvider) "Abrir" else "Reanudar",
                    tint = Color.Black,
                    modifier = Modifier.size(19.dp)
                )
            }

            if (percent != null) {
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
                isProvider -> "En $provider · Toca para abrir"
                percent != null && percent >= 0.99f -> "Terminado · toca para repetir"
                entry.positionMs > 0L && percent != null ->
                    "${(percent * 100).toInt()}% · ${formatRemaining(remaining)} restantes"
                else -> "Empezar"
            },
            color = when (provider) {
                "NSRPLAY" -> Color(0xFF66D9E8)
                "VIMEUS" -> Color(0xFFFFC46B)
                else -> Color(0xFF8F9DAA)
            },
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
