package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.nsrplay.NsrMedia
import com.example.data.nsrplay.NsrMediaType
import com.example.data.nsrplay.NsrPlayCatalogCache
import com.example.data.nsrplay.NsrPlayRepository
import com.example.data.repository.IptvRepository
import com.example.data.vimeus.VimeusCatalogCache
import com.example.data.vimeus.VimeusMedia
import com.example.data.vimeus.VimeusRepository
import com.example.data.vimeus.VimeusType
import com.example.ui.components.ContinueWatchingGrid
import com.example.ui.components.NsrPlayRow
import com.example.ui.components.VimeusRow
import com.example.ui.components.NumberedPosterCard
import com.example.ui.components.StandardPosterCard

@Composable
fun SeriesScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    onNavigateToPlaylists: (() -> Unit)? = null,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val history by repository.history.collectAsState()
    val nsrHistory by repository.nsrHistory.collectAsState()
    val vimeusHistory by repository.vimeusHistory.collectAsState()
    val watchedNsr by repository.watchedNsr.collectAsState()
    val watchedVimeus by repository.vimeusWatched.collectAsState()
    val watchedIds = remember(history) { history.map { it.itemId }.toSet() }

    val context = LocalContext.current
    val nsrRepository = remember { NsrPlayRepository(context.applicationContext) }
    val vimeusRepository = remember { VimeusRepository(context.applicationContext) }
    val nsrState by NsrPlayCatalogCache.state(NsrMediaType.SERIES).collectAsState()
    val vimeusState by VimeusCatalogCache.state(VimeusType.SERIES).collectAsState()
    val vimeusAnimeState by VimeusCatalogCache.state(VimeusType.ANIME).collectAsState()
    var selectedNsr by remember { mutableStateOf<NsrMedia?>(null) }
    var selectedVimeus by remember { mutableStateOf<VimeusMedia?>(null) }

    LaunchedEffect(Unit) {
        NsrPlayCatalogCache.ensureLoaded(context, NsrMediaType.SERIES)
        VimeusCatalogCache.ensureLoaded(context, VimeusType.SERIES)
        VimeusCatalogCache.ensureLoaded(context, VimeusType.ANIME)
    }

    selectedVimeus?.let { media ->
        VimeusDetailScreen(
            media = media,
            repository = vimeusRepository,
            primaryColor = primaryColor,
            onPlayed = { repository.markVimeusWatched(it) },
            onBack = { selectedVimeus = null }
        )
        return
    }

    selectedNsr?.let { media ->
        NsrPlayDetailScreen(
            media = media,
            repository = nsrRepository,
            primaryColor = primaryColor,
            onPlayed = { repository.markNsrWatched(it) },
            onBack = { selectedNsr = null }
        )
        return
    }

    val series = remember(allItems) {
        allItems.filter { it.type == ItemType.SERIES }
    }

    val seriesHistory = remember(history) {
        history.filter { it.type == ItemType.SERIES }
    }

    val nsrSeriesHistory = remember(nsrHistory) {
        nsrHistory.filter { it.type == NsrMediaType.SERIES }
    }

    val vimeusSeriesHistory = remember(vimeusHistory) {
        vimeusHistory.filter { it.type.section == ItemType.SERIES }
    }

    if (series.isEmpty() && seriesHistory.isEmpty() && nsrSeriesHistory.isEmpty() &&
        vimeusSeriesHistory.isEmpty() && nsrState.items.isEmpty() &&
        vimeusState.items.isEmpty() && vimeusAnimeState.items.isEmpty() &&
        !nsrState.isLoading && !vimeusState.isLoading && !vimeusAnimeState.isLoading &&
        nsrState.error == null && vimeusState.error == null && vimeusAnimeState.error == null &&
        nsrState.loadedAt > 0L && vimeusState.loadedAt > 0L && vimeusAnimeState.loadedAt > 0L
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090E13))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131D27)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No hay series cargadas",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tus series de listas IPTV, cuentas Xtream y NSRPlay aparecerán aquí automáticamente.",
                    color = Color(0xFF8F9DAA),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                if (onNavigateToPlaylists != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToPlaylists,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ir a Listas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        return
    }

    val categories = remember(series) {
        series.map { it.category }.distinct()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Dynamic "Continuar viendo" grid replaces the old hero banner.
        if (seriesHistory.isNotEmpty() || nsrSeriesHistory.isNotEmpty() || vimeusSeriesHistory.isNotEmpty()) {
            item {
                ContinueWatchingGrid(
                    iptvHistory = seriesHistory,
                    nsrHistory = nsrSeriesHistory,
                    vimeusHistory = vimeusSeriesHistory,
                    primaryColor = primaryColor,
                    onPlayIptv = { entry ->
                        onPlayItem(
                            PlaylistItem(
                                id = entry.itemId,
                                name = entry.title,
                                streamUrl = entry.streamUrl,
                                logoUrl = entry.posterUrl,
                                type = entry.type
                            )
                        )
                    },
                    onPlayNsr = { entry -> selectedNsr = entry.toMedia() },
                    onPlayVimeus = { entry -> selectedVimeus = entry.toMedia() }
                )
            }
        }

        // NSRPlay titles live in the same Series tab, not in a separate screen.
        item {
            NsrPlayRow(
                title = "NSRPlay · Series",
                state = nsrState,
                primaryColor = primaryColor,
                watchedKeys = watchedNsr,
                onItemClick = { selectedNsr = it },
                onLoadMore = { NsrPlayCatalogCache.loadMore(context, NsrMediaType.SERIES) },
                onRetry = {
                    NsrPlayCatalogCache.ensureLoaded(
                        context = context,
                        type = NsrMediaType.SERIES,
                        force = true
                    )
                }
            )
        }

        item {
            VimeusRow(
                title = "Vimeus · Series",
                state = vimeusState,
                primaryColor = primaryColor,
                watchedKeys = watchedVimeus,
                onItemClick = { selectedVimeus = it },
                onLoadMore = { VimeusCatalogCache.loadMore(context, VimeusType.SERIES) },
                onRetry = {
                    VimeusCatalogCache.ensureLoaded(
                        context = context,
                        type = VimeusType.SERIES,
                        force = true
                    )
                }
            )
        }

        item {
            VimeusRow(
                title = "Vimeus · Anime",
                state = vimeusAnimeState,
                primaryColor = primaryColor,
                watchedKeys = watchedVimeus,
                onItemClick = { selectedVimeus = it },
                onLoadMore = { VimeusCatalogCache.loadMore(context, VimeusType.ANIME) },
                onRetry = {
                    VimeusCatalogCache.ensureLoaded(
                        context = context,
                        type = VimeusType.ANIME,
                        force = true
                    )
                }
            )
        }

        // Section: "■ Destacadas en tus Listas"
        if (series.isNotEmpty()) {
            item {
                Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).background(primaryColor))
                    Text(
                        text = " Destacadas en tus Listas",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    itemsIndexed(series.take(10)) { index, item ->
                        NumberedPosterCard(
                            item = item,
                            rank = index + 1,
                            isWatched = watchedIds.contains(item.id),
                            onClick = { onPlayItem(item) },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
            }
        }

        // Categories sections
        categories.take(8).forEach { catName ->
            val catSeries = series.filter { it.category == catName }
            if (catSeries.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(primaryColor))
                            Text(
                                text = " $catName (${catSeries.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            items(catSeries.take(15)) { s ->
                                StandardPosterCard(
                                    title = s.name,
                                    posterUrl = s.logoUrl,
                                    subtitle = s.category,
                                    isWatched = watchedIds.contains(s.id),
                                    onClick = { onPlayItem(s) },
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
