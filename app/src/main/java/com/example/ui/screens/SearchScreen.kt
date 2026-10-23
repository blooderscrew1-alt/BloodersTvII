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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.repository.IptvRepository
import com.example.ui.components.NsrBadgeColor
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val context = LocalContext.current
    val nsrRepository = remember { NsrPlayRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todo") }

    var nsrSubmittedQuery by remember { mutableStateOf("") }
    var nsrResults by remember { mutableStateOf<List<NsrMedia>>(emptyList()) }
    var nsrLoading by remember { mutableStateOf(false) }
    var nsrError by remember { mutableStateOf<String?>(null) }
    var selectedNsr by remember { mutableStateOf<NsrMedia?>(null) }

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
            onBack = { selectedNsr = null }
        )
        return
    }

    val showNsr = selectedFilter != "Canales" &&
        nsrSubmittedQuery.isNotBlank() &&
        nsrSubmittedQuery == query.trim()

    val nsrVisibleResults = remember(nsrResults, selectedFilter) {
        when (selectedFilter) {
            "Películas" -> nsrResults.filter { it.type == NsrMediaType.MOVIE }
            "Series" -> nsrResults.filter { it.type == NsrMediaType.SERIES }
            else -> nsrResults
        }
    }

    val noLocal = filteredItems.isEmpty()
    val noNsr = !showNsr || (nsrVisibleResults.isEmpty() && !nsrLoading && nsrError == null)

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
            },
            placeholder = {
                Text(
                    "Buscar en listas IPTV y NSRPlay...",
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
                        onClick = { submitNsrSearch() },
                        modifier = Modifier.testTag("search_nsrplay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar también en NSRPlay",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitNsrSearch() }),
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
                text = "Toca la lupa para buscar también en NSRPlay",
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
                        text = "Busca canales de tus listas y películas o series de NSRPlay. Pulsa la lupa para incluir NSRPlay.",
                        color = Color(0xFF8F9DAA),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (noLocal && noNsr) {
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
                        text = if (showNsr) {
                            "No se encontraron coincidencias para \"$query\" en tus listas ni en NSRPlay."
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
                            onClick = { submitNsrSearch() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (showNsr) {
                                    "Reintentar búsqueda en NSRPlay"
                                } else {
                                    "Buscar también en NSRPlay"
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
                        Text(
                            text = "En tus listas IPTV · ${filteredItems.size} resultados",
                            color = Color(0xFF8F9DAA),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(filteredItems, key = { "iptv:" + it.id }) { item ->
                        IptvSearchResultCard(
                            item = item,
                            primaryColor = primaryColor,
                            onClick = { onPlayItem(item) }
                        )
                    }
                }

                if (showNsr) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
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

                    items(nsrVisibleResults, key = { "nsr:" + it.slug }) { media ->
                        NsrSearchResultCard(
                            media = media,
                            primaryColor = primaryColor,
                            onClick = {
                                keyboard?.hide()
                                selectedNsr = media
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IptvSearchResultCard(
    item: PlaylistItem,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101923))
            .border(1.dp, Color(0xFF1B2836), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or Icon
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF172432)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            } else {
                Icon(
                    imageVector = when (item.type) {
                        ItemType.LIVE -> Icons.Default.Tv
                        ItemType.MOVIE -> Icons.Default.Movie
                        ItemType.SERIES -> Icons.Default.Tv
                    },
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (item.type) {
                                ItemType.LIVE -> primaryColor.copy(alpha = 0.2f)
                                ItemType.MOVIE -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ItemType.SERIES -> Color(0xFFFF9100).copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = when (item.type) {
                            ItemType.LIVE -> "EN VIVO"
                            ItemType.MOVIE -> "PELÍCULA"
                            ItemType.SERIES -> "SERIE"
                        },
                        color = when (item.type) {
                            ItemType.LIVE -> primaryColor
                            ItemType.MOVIE -> Color(0xFF00E5FF)
                            ItemType.SERIES -> Color(0xFFFF9100)
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = item.category,
                    color = Color(0xFF8F9DAA),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Play Button Action
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NsrSearchResultCard(
    media: NsrMedia,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101923))
            .border(1.dp, Color(0xFF1B2836), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF172432)),
            contentAlignment = Alignment.Center
        ) {
            if (media.posterOrNull != null) {
                AsyncImage(
                    model = media.posterOrNull,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (media.type == NsrMediaType.MOVIE) Icons.Default.Movie
                    else Icons.Default.Tv,
                    contentDescription = null,
                    tint = NsrBadgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = media.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(NsrBadgeColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "NSRPLAY",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = listOfNotNull(
                        media.year,
                        media.rating?.let { "★ $it" },
                        if (media.type == NsrMediaType.SERIES) "Serie" else "Película"
                    ).joinToString(" · "),
                    color = Color(0xFF8F9DAA),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(NsrBadgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Ver servidores",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
