package com.example.ui.screens

import android.content.Intent
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
import com.example.data.tmdbembed.TmdbEmbedSource
import com.example.data.unlimplay.UnlimplayEpisode
import com.example.data.unlimplay.UnlimplayMedia
import com.example.data.unlimplay.UnlimplayRepository
import com.example.data.unlimplay.UnlimplaySeason
import com.example.data.unlimplay.UnlimplayType
import com.example.data.webepisodes.WebEpisodeWatchStore
import com.example.webplayer.PlayerWebActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TmdbEmbedDetailScreen(
    source: TmdbEmbedSource,
    media: UnlimplayMedia,
    repository: UnlimplayRepository,
    primaryColor: Color,
    onPlayed: (UnlimplayMedia) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var seasons by remember { mutableStateOf<List<UnlimplaySeason>>(emptyList()) }
    var allEpisodes by remember { mutableStateOf<List<UnlimplayEpisode>>(emptyList()) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(media.type != UnlimplayType.MOVIE) }

    LaunchedEffect(media.id, media.type) {
        if (media.type == UnlimplayType.MOVIE) return@LaunchedEffect
        loading = true
        try {
            val loaded = withContext(Dispatchers.IO) {
                repository.getSeasons(media.id)
            }
            seasons = loaded
            selectedSeason = loaded.firstOrNull()?.number ?: 1
            val episodes = withContext(Dispatchers.IO) {
                repository.getEpisodes(media.id, selectedSeason)
            }
            allEpisodes = episodes
            loading = false
            val all = mutableListOf<UnlimplayEpisode>()
            for (season in loaded) {
                try {
                    all += withContext(Dispatchers.IO) {
                        repository.getEpisodes(media.id, season.number)
                    }
                } catch (_: Throwable) {
                }
            }
            allEpisodes = all.distinctBy { it.season to it.episode }
                .sortedWith(compareBy({ it.season }, { it.episode }))
        } catch (_: Throwable) {
            loading = false
        }
    }

    fun play(season: Int? = null, episode: Int? = null) {
        onPlayed(media)
        val currentSeason = season ?: selectedSeason
        val currentEpisode = episode ?: 1
        val queue = if (media.type == UnlimplayType.MOVIE) emptyList() else allEpisodes
        val urls = queue.map {
            source.embedUrl(media, it.season, it.episode)
        }.toCollection(ArrayList())
        val titles = queue.map {
            "${media.title} T${it.season}E${it.episode}"
        }.toCollection(ArrayList())
        val keys = queue.map {
            WebEpisodeWatchStore.key(
                source.id,
                media.id.toString(),
                it.season,
                it.episode
            )
        }.toCollection(ArrayList())
        val index = queue.indexOfFirst {
            it.season == currentSeason && it.episode == currentEpisode
        }
        val label = if (media.type == UnlimplayType.MOVIE) {
            media.title
        } else {
            "${media.title} T${currentSeason}E${currentEpisode}"
        }
        val intent = Intent(context, PlayerWebActivity::class.java).apply {
            putExtra(PlayerWebActivity.EXTRA_URL, source.embedUrl(media, currentSeason, currentEpisode))
            putExtra(PlayerWebActivity.EXTRA_TITLE, label)
            putExtra(PlayerWebActivity.EXTRA_REFERER, source.baseUrl + "/")
            putExtra(PlayerWebActivity.EXTRA_USE_IFRAME, true)
            if (urls.isNotEmpty() && index >= 0) {
                putStringArrayListExtra(PlayerWebActivity.EXTRA_EPISODE_URLS, urls)
                putStringArrayListExtra(PlayerWebActivity.EXTRA_EPISODE_TITLES, titles)
                putStringArrayListExtra(PlayerWebActivity.EXTRA_EPISODE_WATCH_KEYS, keys)
                putExtra(PlayerWebActivity.EXTRA_EPISODE_INDEX, index)
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
                Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
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
                    text = "${source.label} · ${if (media.type == UnlimplayType.MOVIE) "Película" else "Serie"}",
                    color = Color(0xFF8F9DAA),
                    fontSize = 10.sp
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(100.dp)
                            .height(145.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF101923))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = listOfNotNull(media.year, media.rating?.let { "�~. %.1f".format(it) })
                                .joinToString(" · "),
                            color = Color(0xFF8F9DAA),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { play() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (media.type != UnlimplayType.MOVIE) {
                item {
                    Text(
                        "Temporadas",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons, key = { it.number }) { season ->
                            val selected = season.number == selectedSeason
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) primaryColor else Color(0xFF1A2430))
                                    .border(
                                        1.dp,
                                        if (selected) primaryColor else Color(0xFF2A3A4A),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedSeason = season.number
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "T${season.number}",
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Episodios",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (loading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cargando episodios...", color = Color(0xFF8F9DAA), fontSize = 11.sp)
                        }
                    }
                } else {
                    val seasonEpisodes = allEpisodes.filter { it.season == selectedSeason }
                    if (seasonEpisodes.isEmpty()) {
                        item {
                            Text(
                                "Este proveedor no devolvió episodios para esta temporada.",
                                color = Color(0xFF8F9DAA),
                                fontSize = 11.sp
                            )
                        }
                    }
                    items(seasonEpisodes, key = { "${it.season}:${it.episode}" }) { episode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF131C26))
                                .clickable {
                                    play(episode.season, episode.episode)
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "E${episode.episode}",
                                color = primaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                episode.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                tint = Color(0xFF8F9DAA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

