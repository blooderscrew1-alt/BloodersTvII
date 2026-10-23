package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.vimeus.VimeusEpisode
import com.example.data.vimeus.VimeusException
import com.example.data.vimeus.VimeusMedia
import com.example.data.vimeus.VimeusRepository
import com.example.data.vimeus.VimeusType
import com.example.webplayer.PlayerWebActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VimeusDetailScreen(
    media: VimeusMedia,
    repository: VimeusRepository,
    primaryColor: Color,
    onPlayed: (VimeusMedia) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var episodes by remember(media.id) { mutableStateOf<List<VimeusEpisode>>(emptyList()) }
    var loadingEpisodes by remember(media.id) { mutableStateOf(false) }
    var episodesError by remember(media.id) { mutableStateOf<String?>(null) }
    var selectedSeason by remember(media.id) { mutableIntStateOf(1) }

    BackHandler { onBack() }

    LaunchedEffect(media.id, media.tmdbId) {
        if (media.type == VimeusType.MOVIE || media.tmdbId == null) return@LaunchedEffect
        loadingEpisodes = true
        episodesError = null
        try {
            val result = repository.getEpisodes(media.tmdbId)
            episodes = result
            selectedSeason = result.firstOrNull()?.season ?: 1
        } catch (e: VimeusException) {
            episodesError = e.message
        } catch (_: Throwable) {
            episodesError = "No se pudieron cargar los episodios."
        } finally {
            loadingEpisodes = false
        }
    }

    fun play(season: Int? = null, episode: Int? = null) {
        onPlayed(media)
        val label = if (season != null && episode != null) {
            "${media.title} T${season}E${episode}"
        } else {
            media.title
        }
        val intent = Intent(context, PlayerWebActivity::class.java).apply {
            putExtra(PlayerWebActivity.EXTRA_URL, repository.embedUrl(media, season, episode))
            putExtra(PlayerWebActivity.EXTRA_TITLE, label)
            putExtra(PlayerWebActivity.EXTRA_REFERER, "https://vimeus.com/")
        }
        context.startActivity(intent)
    }

    val seasons = remember(episodes) { episodes.map { it.season }.distinct().sorted() }
    val seasonEpisodes = remember(episodes, selectedSeason) {
        episodes.filter { it.season == selectedSeason }
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
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (media.type) {
                        VimeusType.MOVIE -> "Vimeus · Película"
                        VimeusType.SERIES -> "Vimeus · Serie"
                        VimeusType.ANIME -> "Vimeus · Anime"
                    },
                    color = Color(0xFFFF9F1C),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(165.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF172431))
                    ) {
                        if (!media.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = media.posterUrl,
                                contentDescription = media.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = listOfNotNull(
                                media.year,
                                when (media.type) {
                                    VimeusType.MOVIE -> "Película"
                                    VimeusType.SERIES -> "Serie"
                                    VimeusType.ANIME -> "Anime"
                                },
                                media.totalSeasons.takeIf { it > 0 }?.let { "$it temporadas" },
                                media.totalEpisodes.takeIf { it > 0 }?.let { "$it episodios" }
                            ).joinToString(" · "),
                            color = Color(0xFF8F9DAA),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = media.synopsis?.takeIf { it.isNotBlank() }
                                ?: "Contenido agregado por Vimeus desde proveedores públicos.",
                            color = Color(0xFFB8C6D3),
                            fontSize = 11.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (media.type == VimeusType.MOVIE) "Reproducir" else "Temporadas y episodios",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Button(
                    onClick = { play() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (media.type == VimeusType.MOVIE) Color(0xFFFF9F1C) else Color(0xFF243343),
                        contentColor = if (media.type == VimeusType.MOVIE) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (media.type == VimeusType.MOVIE) "Ver en Vimeus" else "Ver serie completa",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (media.type != VimeusType.MOVIE && loadingEpisodes) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF9F1C),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargando episodios...", color = Color(0xFF8F9DAA), fontSize = 11.sp)
                    }
                }
            }

            if (media.type != VimeusType.MOVIE && episodesError != null && episodes.isEmpty()) {
                item {
                    Text(
                        text = episodesError ?: "",
                        color = Color(0xFFFF8A96),
                        fontSize = 11.sp
                    )
                }
            }

            if (seasons.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { season ->
                            val selected = season == selectedSeason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFFFF9F1C) else Color(0xFF16222E))
                                    .clickable { selectedSeason = season }
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

            if (seasonEpisodes.isNotEmpty()) {
                items(seasonEpisodes, key = { "${it.season}-${it.episode}" }) { episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF0F1722))
                            .border(1.dp, Color(0xFF1B2836), RoundedCornerShape(7.dp))
                            .clickable {
                                play(episode.season, episode.episode)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0x22FF9F1C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = episode.episode.toString(),
                                color = Color(0xFFFF9F1C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = episode.title ?: "Episodio ${episode.episode}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temporada ${episode.season} · Episodio ${episode.episode}",
                                color = Color(0xFF8F9DAA),
                                fontSize = 9.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFFF9F1C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (media.type == VimeusType.MOVIE) {
                item {
                    Text(
                        text = "El reproductor de Vimeus se abre dentro de esta app con el bloqueo de anuncios y las extensiones activas.",
                        color = Color(0xFF66757F),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
