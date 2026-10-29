package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import com.example.data.unlimplay.UnlimplayEpisode
import com.example.data.unlimplay.UnlimplayMedia
import com.example.data.unlimplay.UnlimplayRepository
import com.example.data.unlimplay.UnlimplaySeason
import com.example.data.unlimplay.UnlimplayType
import com.example.data.webepisodes.WebEpisodeWatchStore
import com.example.webplayer.PlayerWebActivity
import kotlinx.coroutines.launch

@Composable
fun UnlimplayDetailScreen(
    media: UnlimplayMedia,
    repository: UnlimplayRepository,
    primaryColor: Color,
    onPlayed: (UnlimplayMedia) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var seasons by remember(media.id) { mutableStateOf<List<UnlimplaySeason>>(emptyList()) }
    var episodes by remember(media.id) { mutableStateOf<List<UnlimplayEpisode>>(emptyList()) }
    var allEpisodes by remember(media.id) { mutableStateOf<List<UnlimplayEpisode>>(emptyList()) }
    var selectedSeason by remember(media.id) { mutableIntStateOf(1) }
    var loading by remember(media.id) { mutableStateOf(media.type != UnlimplayType.MOVIE) }
    var error by remember(media.id) { mutableStateOf<String?>(null) }
    val watchedWebEpisodes by WebEpisodeWatchStore.watched.collectAsState()

    BackHandler { onBack() }

    LaunchedEffect(media.id, media.type) {
        if (media.type == UnlimplayType.MOVIE) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val result = repository.getSeasons(media.id)
            seasons = result
            val first = result.firstOrNull()?.number ?: 1
            selectedSeason = first
            episodes = repository.getEpisodes(media.id, first)
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar los episodios."
        } finally {
            loading = false
        }
    }

    // Load every season in the background so auto-next can cross a season
    // boundary, not only advance inside the selected season.
    LaunchedEffect(media.id, seasons) {
        if (media.type == UnlimplayType.MOVIE || seasons.isEmpty()) return@LaunchedEffect
        val combined = mutableListOf<UnlimplayEpisode>()
        for (season in seasons) {
            try {
                combined += repository.getEpisodes(media.id, season.number)
            } catch (_: Throwable) {
            }
        }
        allEpisodes = combined
            .distinctBy { it.season to it.episode }
            .sortedWith(compareBy({ it.season }, { it.episode }))
    }

    fun play(season: Int? = null, episode: Int? = null) {
        onPlayed(media)
        val label = if (season != null && episode != null) {
            "${media.title} T${season}E${episode}"
        } else {
            media.title
        }
        val currentSeason = season ?: selectedSeason
        val currentEpisode = episode ?: 1
        val queueEpisodes = if (media.type == UnlimplayType.MOVIE) {
            emptyList()
        } else {
            allEpisodes.ifEmpty { episodes }
        }
        val queueUrls = queueEpisodes.map {
            repository.embedUrl(media, it.season, it.episode)
        }.toCollection(ArrayList())
        val queueTitles = queueEpisodes.map {
            "${media.title} T${it.season}E${it.episode}"
        }.toCollection(ArrayList())
        val currentIndex = queueEpisodes.indexOfFirst {
            it.season == currentSeason && it.episode == currentEpisode
        }
        val queueWatchKeys = queueEpisodes.map {
            WebEpisodeWatchStore.key(
                WebEpisodeWatchStore.PROVIDER_UNLIMPLAY,
                media.id.toString(),
                it.season,
                it.episode
            )
        }.toCollection(ArrayList())

        val intent = Intent(context, PlayerWebActivity::class.java).apply {
            putExtra(
                PlayerWebActivity.EXTRA_URL,
                repository.embedUrl(
                    media = media,
                    season = if (media.type == UnlimplayType.MOVIE) null else currentSeason,
                    episode = currentEpisode
                )
            )
            putExtra(PlayerWebActivity.EXTRA_TITLE, label)
            putExtra(PlayerWebActivity.EXTRA_REFERER, "https://unlimplay.com/")
            putExtra(PlayerWebActivity.EXTRA_USE_IFRAME, true)
            if (queueUrls.isNotEmpty() && currentIndex >= 0) {
                putStringArrayListExtra(PlayerWebActivity.EXTRA_EPISODE_URLS, queueUrls)
                putStringArrayListExtra(PlayerWebActivity.EXTRA_EPISODE_TITLES, queueTitles)
                putStringArrayListExtra(PlayerWebActivity.EXTRA_EPISODE_WATCH_KEYS, queueWatchKeys)
                putExtra(PlayerWebActivity.EXTRA_EPISODE_INDEX, currentIndex)
            }
        }
        context.startActivity(intent)
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
                        UnlimplayType.MOVIE -> "Unlimplay · Película"
                        UnlimplayType.SERIES -> "Unlimplay · Serie"
                        UnlimplayType.ANIME -> "Unlimplay · Anime"
                    },
                    color = Color(0xFF9C6BFF),
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
                                    UnlimplayType.MOVIE -> "Película"
                                    UnlimplayType.SERIES -> "Serie"
                                    UnlimplayType.ANIME -> "Anime"
                                },
                                media.rating?.let { "★ %.1f".format(it) }
                            ).joinToString(" · "),
                            color = Color(0xFF8F9DAA),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = media.overview?.takeIf { it.isNotBlank() }
                                ?: "Contenido disponible en Unlimplay.",
                            color = Color(0xFFB8C6D3),
                            fontSize = 11.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (media.type == UnlimplayType.MOVIE) play() else play(selectedSeason, 1)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C6BFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (media.type == UnlimplayType.MOVIE) {
                            "Ver en Unlimplay"
                        } else {
                            "Ver T${selectedSeason}E1"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color(0xFF9C6BFF),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargando temporadas...", color = Color(0xFF8F9DAA), fontSize = 11.sp)
                    }
                }
            }

            error?.let { message ->
                item {
                    Text(message, color = Color(0xFFFF8A96), fontSize = 11.sp)
                }
            }

            if (seasons.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { season ->
                            val selected = season.number == selectedSeason
                            val interaction = remember(season.number) { MutableInteractionSource() }
                            val focused by interaction.collectIsFocusedAsState()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Color(0xFF9C6BFF) else Color(0xFF16222E))
                                    .border(
                                        width = if (focused) 2.dp else 0.dp,
                                        color = if (focused) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .focusable(interactionSource = interaction)
                                    .clickable(
                                        interactionSource = interaction,
                                        indication = null
                                    ) {
                                        selectedSeason = season.number
                                        loading = true
                                        error = null
                                        scope.launch {
                                            try {
                                                episodes = repository.getEpisodes(media.id, season.number)
                                            } catch (t: Throwable) {
                                                error = t.localizedMessage
                                                    ?: "No se pudieron cargar los episodios."
                                            } finally {
                                                loading = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Temporada ${season.number}",
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            items(episodes, key = { "${it.season}-${it.episode}" }) { episode ->
                val interaction = remember(episode) { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF0F1722))
                        .border(
                            width = if (focused) 2.dp else 1.dp,
                            color = if (focused) Color(0xFF9C6BFF) else Color(0xFF1B2836),
                            shape = RoundedCornerShape(7.dp)
                        )
                        .focusable(interactionSource = interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            play(episode.season, episode.episode)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x229C6BFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = episode.episode.toString(),
                            color = Color(0xFFC3A6FF),
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
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Temporada ${episode.season} · Episodio ${episode.episode}",
                            color = Color(0xFF8F9DAA),
                            fontSize = 9.sp
                        )
                    }
                    if (watchedWebEpisodes.contains(
                            WebEpisodeWatchStore.key(
                                WebEpisodeWatchStore.PROVIDER_UNLIMPLAY,
                                media.id.toString(),
                                episode.season,
                                episode.episode
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x339C6BFF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VISTO",
                                color = Color(0xFFC3A6FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF9C6BFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Unlimplay muestra sus servidores dentro del iframe. La app no extrae ni guarda enlaces de video.",
                    color = Color(0xFF66757F),
                    fontSize = 9.sp
                )
            }
        }
    }
}
