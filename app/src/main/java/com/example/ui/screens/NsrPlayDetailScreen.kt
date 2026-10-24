package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.nsrplay.NsrEpisode
import com.example.data.nsrplay.NsrMedia
import com.example.data.nsrplay.NsrMediaType
import com.example.data.nsrplay.NsrPlayException
import com.example.data.nsrplay.NsrPlayRepository
import com.example.data.nsrplay.NsrSeason
import com.example.data.nsrplay.NsrServer
import com.example.webplayer.PlayerWebActivity
import kotlinx.coroutines.launch

private val NsrGenres = listOf(
    "Acción" to "accion",
    "Comedia" to "comedia",
    "Drama" to "drama",
    "Terror" to "terror",
    "Romance" to "romance",
    "Ciencia Ficción" to "ciencia-ficcion",
    "Animación" to "animacion",
    "Suspense" to "suspense",
    "Crimen" to "crimen",
    "Aventura" to "aventura",
    "Documental" to "documental",
    "Familia" to "familia",
    "Fantasía" to "fantasia",
    "Misterio" to "misterio"
)


@Composable
fun NsrPlayDetailScreen(
    media: NsrMedia,
    repository: NsrPlayRepository,
    primaryColor: Color,
    onPlayed: (NsrMedia) -> Unit = {},
    onBack: () -> Unit
) {
    // Results that came from /content/info already bring their servers or
    // seasons. Show them immediately instead of making the user wait for the
    // same request a second time.
    val hasLoadedDetail = media.servers.isNotEmpty() || media.seasons.isNotEmpty()
    var detail by remember(media.slug) {
        mutableStateOf(if (hasLoadedDetail) media else null)
    }
    var isLoading by remember(media.slug) { mutableStateOf(!hasLoadedDetail) }
    var error by remember(media.slug) { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    var selectedSeasonNumber by remember { mutableIntStateOf(1) }

    val context = LocalContext.current

    BackHandler { onBack() }

    LaunchedEffect(media.slug, media.type, reloadTick) {
        if (reloadTick == 0 && hasLoadedDetail) {
            selectedSeasonNumber = media.seasons.firstOrNull()?.number ?: 1
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        try {
            val info = repository.getInfo(media.slug, media.type)
            detail = info.copy(
                poster = info.poster?.takeIf { it.isNotBlank() } ?: media.poster,
                title = info.title.ifBlank { media.title },
                year = info.year ?: media.year,
                rating = info.rating ?: media.rating,
                synopsis = info.synopsis ?: media.synopsis
            )
            selectedSeasonNumber = info.seasons.firstOrNull()?.number ?: 1
        } catch (e: NsrPlayException) {
            error = e.message
        } catch (_: Throwable) {
            error = "No se pudo cargar el detalle desde NSRPlay."
        } finally {
            isLoading = false
        }
    }

    fun openNsrPage(
        item: NsrMedia,
        season: Int? = null,
        episode: Int? = null,
        label: String = item.title
    ) {
        onPlayed(item)
        val intent = Intent(context, PlayerWebActivity::class.java).apply {
            putExtra(PlayerWebActivity.EXTRA_URL, repository.webPlayerUrl(item, season, episode))
            putExtra(PlayerWebActivity.EXTRA_TITLE, label)
            putExtra(PlayerWebActivity.EXTRA_REFERER, "https://nsrplay.space/")
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
            Text(
                text = detail?.title ?: media.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (isLoading) {
            LoadingState("Cargando detalle...")
        } else if (error != null) {
            ErrorState(
                message = error ?: "Error desconocido",
                primaryColor = primaryColor,
                onRetry = { reloadTick += 1 }
            )
        } else {
            val info = detail ?: media

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(104.dp)
                                .height(156.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF131B24)),
                        ) {
                            if (info.posterOrNull != null) {
                                AsyncImage(
                                    model = info.posterOrNull,
                                    contentDescription = info.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color(0xFF40505E),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOfNotNull(
                                    info.year,
                                    info.rating?.let { "★ $it" },
                                    if (info.type == NsrMediaType.SERIES) "Serie" else "Película"
                                ).joinToString("  •  "),
                                color = Color(0xFFAFC0CE),
                                fontSize = 10.sp
                            )
                            info.provider?.let {
                                Text(
                                    text = "Fuente: $it",
                                    color = Color(0xFF778795),
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = info.synopsis ?: "Sin sinopsis disponible.",
                                color = Color(0xFFB6C3CE),
                                fontSize = 11.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (info.genres.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            info.genres.take(10).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF172430))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = Color(0xFF9FB1C1),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (info.type == NsrMediaType.MOVIE) {
                    item {
                        Text(
                            text = "Reproductor",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                openNsrPage(info, label = info.title)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver en NSRPlay",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // SERIES
                    item {
                        Text(
                            text = "Temporadas",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (info.seasons.isEmpty()) {
                        item {
                            Text(
                                text = "No se encontraron temporadas para esta serie.",
                                color = Color(0xFF8F9DAA),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(info.seasons) { season ->
                                    GenreChip(
                                        label = season.name,
                                        selected = selectedSeasonNumber == season.number,
                                        primaryColor = primaryColor,
                                        onClick = {
                                            selectedSeasonNumber = season.number
                                        }
                                    )
                                }
                            }
                        }

                        val season: NsrSeason? = info.seasons.firstOrNull {
                            it.number == selectedSeasonNumber
                        } ?: info.seasons.firstOrNull()

                        item {
                            Text(
                                text = "Episodios · ${season?.name ?: "Temporada $selectedSeasonNumber"}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(season?.episodes ?: emptyList()) { episode ->
                            NsrEpisodeRow(
                                episode = episode,
                                selected = false,
                                primaryColor = primaryColor,
                                onClick = {
                                    openNsrPage(
                                        item = info,
                                        season = episode.season,
                                        episode = episode.number,
                                        label = "${info.title} T${episode.season}E${episode.number}"
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Contenido indexado por NSRPlay desde proveedores públicos. BloodersTv no aloja ni distribuye video.",
                        color = Color(0xFF66757F),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun NsrServerRow(
    server: NsrServer,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF1A2835) else Color(0xFF0F1722))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) primaryColor else Color(0xFF1B2836),
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.server.ifBlank { "Servidor" }
                    .replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${NsrPlayRepository.languageLabel(server.language)} · Abrir reproductor",
                color = Color(0xFF8F9DAA),
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF63727F),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun NsrEpisodeRow(
    episode: NsrEpisode,
    selected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) Color(0xFF1A2833) else Color(0xFF0E161F))
            .border(
                width = if (isFocused) 2.dp else if (selected) 1.5.dp else 0.dp,
                color = if (isFocused) Color.White else if (selected) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (selected) primaryColor else Color(0xFF1B2834)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = episode.number.toString(),
                color = if (selected) Color.Black else Color(0xFFAFC0CE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = episode.title,
            color = if (selected) Color.White else Color(0xFFC4D1DC),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Ver episodio",
            tint = if (selected) primaryColor else Color(0xFF7B8B99),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) primaryColor else Color(0xFF141F2B))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color(0xFF9FB0C0),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun GenreChip(
    label: String,
    selected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) primaryColor.copy(alpha = 0.18f) else Color(0xFF101923))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White
                else if (selected) primaryColor else Color(0xFF1D2B39),
                shape = RoundedCornerShape(6.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) primaryColor else Color(0xFF93A4B3),
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun LoadingState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Color(0xFFCCFF00),
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = message, color = Color(0xFF8F9DAA), fontSize = 11.sp)
    }
}

@Composable
private fun ErrorState(
    message: String,
    primaryColor: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = Color(0xFFFF7A8A),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFFC4D1DC),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Reintentar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
