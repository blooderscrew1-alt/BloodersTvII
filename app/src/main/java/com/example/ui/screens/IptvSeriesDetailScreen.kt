package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.iptv.IptvEpisode
import com.example.data.iptv.IptvSeries
import com.example.data.model.PlaylistItem

@Composable
fun IptvSeriesDetailScreen(
    series: IptvSeries,
    primaryColor: Color,
    onPlayItem: (PlaylistItem) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var selectedSeason by remember(series.key) {
        mutableIntStateOf(series.seasons.firstOrNull() ?: 1)
    }
    val episodes = remember(series, selectedSeason) {
        series.episodes.filter { it.season == selectedSeason }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "IPTV · ${series.episodes.size} episodios",
                    color = primaryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(158.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF172431))
                    ) {
                        if (!series.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = series.posterUrl,
                                contentDescription = series.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = series.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${series.category} · ${series.seasons.size} temporadas",
                            color = Color(0xFF8F9DAA),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecciona un episodio para reproducirlo.",
                            color = Color(0xFFB8C6D3),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (series.seasons.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(series.seasons) { season ->
                            val selected = season == selectedSeason
                            val interaction = remember(season) { MutableInteractionSource() }
                            val focused by interaction.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) primaryColor else Color(0xFF16222E))
                                    .border(
                                        if (focused) 2.dp else 0.dp,
                                        if (focused) Color.White else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .focusable(interactionSource = interaction)
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null
                                    ) { selectedSeason = season }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Temporada $season",
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            items(episodes, key = { it.item.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    primaryColor = primaryColor,
                    onClick = { onPlayItem(episode.item) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: IptvEpisode,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interaction = remember(episode.item.id) { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF0F1722))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) primaryColor else Color(0xFF1B2836),
                RoundedCornerShape(7.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(primaryColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = episode.episode.toString(),
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Temporada ${episode.season} · Episodio ${episode.episode}",
                color = Color(0xFF8F9DAA),
                fontSize = 9.sp
            )
        }
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = "Reproducir",
            tint = primaryColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
