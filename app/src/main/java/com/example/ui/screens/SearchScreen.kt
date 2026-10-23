package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.nsrplay.NsrMedia
import com.example.data.nsrplay.NsrMediaType
import com.example.data.nsrplay.NsrPlayException
import com.example.data.nsrplay.NsrPlayRepository
import com.example.data.nsrplay.watchKey
import com.example.data.repository.IptvRepository
import com.example.data.vimeus.VimeusMedia
import com.example.data.vimeus.VimeusRepository
import com.example.data.vimeus.VimeusType
import com.example.ui.components.NsrBadgeColor
import com.example.ui.components.SearchPosterCarousel
import com.example.ui.components.SearchPosterItem
import com.example.ui.components.VimeusBadgeColor
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val history by repository.history.collectAsState()
    val watchedNsr by repository.watchedNsr.collectAsState()
    val watchedVimeus by repository.vimeusWatched.collectAsState()
    val watchedIds = remember(history) { history.map { it.itemId }.toSet() }
    val context = LocalContext.current
    val nsrRepository = remember { NsrPlayRepository(context.applicationContext) }
    val vimeusRepository = remember { VimeusRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val filterFocusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todo") }

    var nsrSubmittedQuery by remember { mutableStateOf("") }
    var nsrResults by remember { mutableStateOf<List<NsrMedia>>(emptyList()) }
    var nsrLoading by remember { mutableStateOf(false) }
    var nsrError by remember { mutableStateOf<String?>(null) }
    var selectedNsr by remember { mutableStateOf<NsrMedia?>(null) }

    var vimeusSubmittedQuery by remember { mutableStateOf("") }
    var vimeusResults by remember { mutableStateOf<List<VimeusMedia>>(emptyList()) }
    var vimeusLoading by remember { mutableStateOf(false) }
    var vimeusError by remember { mutableStateOf<String?>(null) }
    var selectedVimeus by remember { mutableStateOf<VimeusMedia?>(null) }

    fun submitNsrSearch() {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2 || selectedFilter == "Canales") return
        if (cleanQuery == nsrSubmittedQuery && nsrLoading) return

        nsrSubmittedQuery = cleanQuery
        nsrLoading = true
        nsrError = null
        nsrResults = emptyList()
        keyboard?.hide()

        scope.launch {
            try {
                val result = nsrRepository.searchWithCatalogFallback(
                    query = cleanQuery,
                    preferredType = when (selectedFilter) {
                        "Películas" -> NsrMediaType.MOVIE
                        "Series" -> NsrMediaType.SERIES
                        else -> null
                    }
                )
                if (nsrSubmittedQuery == cleanQuery) {
                    nsrResults = result
                    nsrError = null
                }
            } catch (e: NsrPlayException) {
                if (nsrSubmittedQuery == cleanQuery) {
                    nsrError = e.message ?: "NSRPlay rechazó la búsqueda."
                }
            } catch (_: Throwable) {
                if (nsrSubmittedQuery == cleanQuery) {
                    nsrError = "No se pudo buscar en NSRPlay."
                }
            } finally {
                if (nsrSubmittedQuery == cleanQuery) {
                    nsrLoading = false
                }
            }
        }
    }

    fun submitVimeusSearch() {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2 || selectedFilter == "Canales") return
        if (cleanQuery == vimeusSubmittedQuery && vimeusLoading) return

        vimeusSubmittedQuery = cleanQuery
        vimeusLoading = true
        vimeusError = null
        vimeusResults = emptyList()

        scope.launch {
            try {
                val result = vimeusRepository.search(
                    query = cleanQuery,
                    preferredType = when (selectedFilter) {
                        "Películas" -> VimeusType.MOVIE
                        "Series" -> VimeusType.SERIES
                        else -> null
                    }
                )
                if (vimeusSubmittedQuery == cleanQuery) {
                    vimeusResults = result
                    vimeusError = null
                }
            } catch (e: Exception) {
                if (vimeusSubmittedQuery == cleanQuery) {
                    vimeusError = e.message ?: "Vimeus rechazó la búsqueda."
                }
            } finally {
                if (vimeusSubmittedQuery == cleanQuery) {
                    vimeusLoading = false
                }
            }
        }
    }

    val filteredItems = remember(query, allItems, selectedFilter) {
        if (query.isBlank()) emptyList()
        else {
            allItems.filter { item ->
                val matchesQuery = item.name.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true)

                val matchesType = when (selectedFilter) {
                    "Canales" -> item.type == ItemType.LIVE
                    "Películas" -> item.type == ItemType.MOVIE
                    "Series" -> item.type == ItemType.SERIES
                    else -> true
                }
                matchesQuery && matchesType
            }.take(150)
        }
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

    val showNsr = selectedFilter != "Canales" &&
        nsrSubmittedQuery.isNotBlank() &&
        nsrSubmittedQuery == query.trim()

    val showVimeus = selectedFilter != "Canales" &&
        vimeusSubmittedQuery.isNotBlank() &&
        vimeusSubmittedQuery == query.trim()

    val nsrVisibleResults = remember(nsrResults, selectedFilter) {
        when (selectedFilter) {
            "Películas" -> nsrResults.filter { it.type == NsrMediaType.MOVIE }
            "Series" -> nsrResults.filter { it.type == NsrMediaType.SERIES }
            else -> nsrResults
        }
    }

    val vimeusVisibleResults = remember(vimeusResults, selectedFilter) {
        when (selectedFilter) {
            "Películas" -> vimeusResults.filter { it.type == VimeusType.MOVIE }
            "Series" -> vimeusResults.filter { it.type != VimeusType.MOVIE }
            else -> vimeusResults
        }
    }

    val noLocal = filteredItems.isEmpty()
    val noNsr = !showNsr || (nsrVisibleResults.isEmpty() && !nsrLoading && nsrError == null)
    val noVimeus = !showVimeus ||
        (vimeusVisibleResults.isEmpty() && !vimeusLoading && vimeusError == null)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                query = newValue
                if (newValue.trim() != nsrSubmittedQuery) {
                    nsrResults = emptyList()
                    nsrError = null
                    nsrLoading = false
                }
                if (newValue.trim() != vimeusSubmittedQuery) {
                    vimeusResults = emptyList()
                    vimeusError = null
                    vimeusLoading = false
                }
            },
            placeholder = {
                Text(
                    "Buscar en IPTV, NSRPlay y Vimeus...",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = Color(0xFF8F9DAA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            submitNsrSearch()
                            submitVimeusSearch()
                        },
                        modifier = Modifier.testTag("search_nsrplay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar en NSRPlay y Vimeus",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                submitNsrSearch()
                submitVimeusSearch()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFF22303E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0F1722),
                unfocusedContainerColor = Color(0xFF0F1722),
                cursorColor = primaryColor
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                        focusManager.clearFocus(force = true)
                        runCatching { filterFocusRequester.requestFocus() }
                        true
                    } else {
                        false
                    }
                }
                .testTag("iptv_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(listOf("Todo", "Canales", "Películas", "Series")) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedFilter = filter
                        if (filter == "Canales") {
                            nsrResults = emptyList()
                            nsrError = null
                            nsrLoading = false
                        }
                    },
                    modifier = if (filter == "Todo") {
                        Modifier.focusRequester(filterFocusRequester)
                    } else {
                        Modifier
                    },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF141F2B),
                        labelColor = Color(0xFF9FB0C0)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) primaryColor else Color(0xFF233242),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        if (query.trim().length >= 2 &&
            selectedFilter != "Canales" &&
            query.trim() != nsrSubmittedQuery
        ) {
            Text(
                text = "Toca la lupa para buscar también en NSRPlay y Vimeus",
                color = Color(0xFF7F8F9F),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131D28)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Búsqueda unificada",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Busca canales de tus listas y películas o series de NSRPlay y Vimeus. Pulsa la lupa para incluir los proveedores.",
                        color = Color(0xFF8F9DAA),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (noLocal && noNsr && noVimeus) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sin resultados",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (showNsr || showVimeus) {
                            "No se encontraron coincidencias para \"$query\" en tus listas, NSRPlay ni Vimeus."
                        } else {
                            "No se encontraron coincidencias para \"$query\" en tus listas IPTV."
                        },
                        color = Color(0xFF8F9DAA),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    if (selectedFilter != "Canales" && query.trim().length >= 2) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                submitNsrSearch()
                                submitVimeusSearch()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showNsr || showVimeus) {
                                    "Reintentar búsqueda en proveedores"
                                } else {
                                    "Buscar también en NSRPlay y Vimeus"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (filteredItems.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NsrBadgeColor)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "IPTV",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "En tus listas IPTV · ${filteredItems.size} resultados",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    item {
                        SearchPosterCarousel(
                            items = filteredItems.map { media ->
                                SearchPosterItem(
                                    key = "iptv:${media.id}",
                                    title = media.name,
                                    posterUrl = media.logoUrl,
                                    typeLabel = when (media.type) {
                                        ItemType.LIVE -> "Canal"
                                        ItemType.MOVIE -> "Película"
                                        ItemType.SERIES -> "Serie"
                                    },
                                    subtitle = media.category,
                                    isWatched = watchedIds.contains(media.id)
                                )
                            },
                            accentColor = NsrBadgeColor,
                            onOpen = { index -> onPlayItem(filteredItems[index]) }
                        )
                    }
                }

                if (showVimeus) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp)
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
                                text = if (vimeusVisibleResults.isNotEmpty()) {
                                    "Vimeus · ${vimeusVisibleResults.size} resultados"
                                } else {
                                    "Vimeus"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { submitVimeusSearch() }) {
                                Text("Actualizar", color = primaryColor, fontSize = 10.sp)
                            }
                        }
                    }

                    if (vimeusLoading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = primaryColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buscando en Vimeus...",
                                    color = Color(0xFF8F9DAA),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    vimeusError?.let { message ->
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = message,
                                    color = Color(0xFFFF8A96),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { submitVimeusSearch() }) {
                                    Text("Reintentar", color = primaryColor, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (!vimeusLoading && vimeusError == null && vimeusVisibleResults.isEmpty()) {
                        item {
                            Text(
                                text = "Vimeus no devolvió títulos para \"$vimeusSubmittedQuery\".",
                                color = Color(0xFF7F8F9F),
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (vimeusVisibleResults.isNotEmpty()) {
                        item {
                            SearchPosterCarousel(
                                items = vimeusVisibleResults.map { media ->
                                    SearchPosterItem(
                                        key = "vimeus:${media.id}",
                                        title = media.title,
                                        posterUrl = media.posterUrl,
                                        typeLabel = when (media.type) {
                                            VimeusType.MOVIE -> "Película"
                                            VimeusType.SERIES -> "Serie"
                                            VimeusType.ANIME -> "Anime"
                                        },
                                        subtitle = listOfNotNull(
                                            media.year,
                                            media.totalSeasons.takeIf { it > 0 }?.let { "${it} temp." }
                                        ).joinToString(" · ").ifBlank { null },
                                        isWatched = watchedVimeus.contains(media.watchKey())
                                    )
                                },
                                accentColor = VimeusBadgeColor,
                                onOpen = { index ->
                                    keyboard?.hide()
                                    selectedVimeus = vimeusVisibleResults[index]
                                }
                            )
                        }
                    }
                }

                if (showNsr) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NsrBadgeColor)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NSRPLAY",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (nsrVisibleResults.isNotEmpty()) {
                                    "NSRPlay · ${nsrVisibleResults.size} resultados"
                                } else {
                                    "NSRPlay"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { submitNsrSearch() }) {
                                Text("Actualizar", color = primaryColor, fontSize = 10.sp)
                            }
                        }
                    }

                    if (nsrLoading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = primaryColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buscando en NSRPlay...",
                                    color = Color(0xFF8F9DAA),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    nsrError?.let { message ->
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = message,
                                    color = Color(0xFFFF8A96),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { submitNsrSearch() }) {
                                    Text("Reintentar", color = primaryColor, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (!nsrLoading && nsrError == null && nsrVisibleResults.isEmpty()) {
                        item {
                            Text(
                                text = "NSRPlay no devolvió títulos para \"$nsrSubmittedQuery\".",
                                color = Color(0xFF7F8F9F),
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (nsrVisibleResults.isNotEmpty()) {
                        item {
                            SearchPosterCarousel(
                                items = nsrVisibleResults.map { media ->
                                    SearchPosterItem(
                                        key = "nsr:${media.slug}",
                                        title = media.title,
                                        posterUrl = media.posterOrNull,
                                        typeLabel = if (media.type == NsrMediaType.SERIES) "Serie" else "Película",
                                        subtitle = listOfNotNull(
                                            media.year,
                                            media.rating?.let { "★ $it" }
                                        ).joinToString(" · ").ifBlank { null },
                                        isWatched = watchedNsr.contains(media.watchKey())
                                    )
                                },
                                accentColor = NsrBadgeColor,
                                onOpen = { index ->
                                    keyboard?.hide()
                                    selectedNsr = nsrVisibleResults[index]
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

