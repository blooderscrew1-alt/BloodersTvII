package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.vimeus.VimeusCatalogUiState
import com.example.data.vimeus.VimeusMedia
import com.example.data.vimeus.VimeusType

val VimeusBadgeColor = Color(0xFFFF9F1C)

@Composable
fun VimeusRow(
    title: String,
    state: VimeusCatalogUiState,
    primaryColor: Color,
    watchedKeys: Set<String> = emptySet(),
    onItemClick: (VimeusMedia) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(VimeusBadgeColor)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "VIMEUS",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = primaryColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        when {
            state.items.isEmpty() && state.error != null -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.error,
                        color = Color(0xFFFF8A96),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) {
                        Text("Reintentar", color = primaryColor, fontSize = 11.sp)
                    }
                }
            }

            state.items.isEmpty() && (state.isLoading || state.loadedAt == 0L) -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cargando catálogo Vimeus...",
                        color = Color(0xFF8F9DAA),
                        fontSize = 11.sp
                    )
                }
            }

            state.items.isEmpty() -> {
                Text(
                    text = "Vimeus no devolvió títulos en este momento.",
                    color = Color(0xFF8F9DAA),
                    fontSize = 11.sp
                )
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(state.items, key = { it.type.apiValue + ":" + it.id }) { media ->
                        StandardPosterCard(
                            title = media.title,
                            posterUrl = media.posterUrl,
                            subtitle = when {
                                media.type == VimeusType.MOVIE -> "Película"
                                media.totalSeasons > 0 -> "${media.totalSeasons} temporadas"
                                else -> "Serie"
                            },
                            badge = "VIMEUS",
                            badgeColor = VimeusBadgeColor,
                            isWatched = watchedKeys.contains(media.watchKey()),
                            onClick = { onItemClick(media) },
                            primaryColor = primaryColor
                        )
                    }

                    if (state.hasNext) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(135.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF101923))
                                        .border(1.dp, Color(0xFF243343), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !state.isLoading) { onLoadMore() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(
                                            color = primaryColor,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Cargar más",
                                                color = Color(0xFFB8C6D3),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
