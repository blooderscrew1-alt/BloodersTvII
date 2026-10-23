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
fun NsrPlayScreen(
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { NsrPlayRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val compactHeader = LocalConfiguration.current.screenHeightDp < 600

    var mediaType by remember { mutableStateOf(NsrMediaType.MOVIE) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var reloadTick by remember { mutableIntStateOf(0) }

    var items by remember { mutableStateOf<List<NsrMedia>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasNextPage by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detailMedia by remember { mutableStateOf<NsrMedia?>(null) }

    LaunchedEffect(mediaType, selectedGenre, submittedQuery, reloadTick) {
        if (!repository.isConfigured) {
            error = "Falta la API Key de NSRPlay. Configúrala desde Ajustes > NSRPlay sin tocar el código."
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        items = emptyList()
        page = 1
        hasNextPage = false

        try {
            if (submittedQuery.isBlank()) {
                val result = repository.getCatalog(mediaType, page = 1, genre = selectedGenre)
                items = result.items
                page = result.page
                hasNextPage = result.hasNextPage
            } else {
                items = repository.search(submittedQuery)
                hasNextPage = false
            }
        } catch (e: NsrPlayException) {
            error = e.message
        } catch (t: Throwable) {
            error = "No se pudo conectar con NSRPlay. Revisa tu conexión."
        } finally {
            isLoading = false
        }
    }

    fun retry() {
        reloadTick += 1
    }

    fun loadMore() {
        if (isLoadingMore || isLoading || !hasNextPage || submittedQuery.isNotBlank()) return
        isLoadingMore = true
        scope.launch {
            try {
                val result = repository.getCatalog(
                    type = mediaType,
                    page = page + 1,
                    genre = selectedGenre
                )
                items = items + result.items
                page = result.page
                hasNextPage = result.hasNextPage
            } catch (e: NsrPlayException) {
                error = e.message
            } catch (_: Throwable) {
                error = "No se pudieron cargar más resultados."
            } finally {
                isLoadingMore = false
            }
        }
    }

    detailMedia?.let { media ->
        NsrPlayDetailScreen(
            media = media,
            repository = repository,
            primaryColor = primaryColor,
            onBack = { detailMedia = null }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1400.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (compactHeader) 30.dp else 34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(if (compactHeader) 18.dp else 21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NSRPlay",
                            color = Color.White,
                            fontSize = if (compactHeader) 16.sp else 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (!compactHeader) {
                            Text(
                                text = "Películas y series de proveedores públicos",
                                color = Color(0xFF8F9DAA),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (compactHeader) 6.dp else 10.dp))

                // Search is explicit: NSRPlay limits searches to 5/min, so we
                // never fire a request on every keystroke.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text("Buscar película o serie...", fontSize = 12.sp, maxLines = 1)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = Color(0xFF8F9DAA),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                submittedQuery = query.trim()
                                if (submittedQuery.isNotBlank()) reloadTick += 1
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF22303E),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F1722),
                            unfocusedContainerColor = Color(0xFF0F1722),
                            cursorColor = primaryColor
                        ),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nsrplay_search_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(primaryColor)
                            .clickable {
                                submittedQuery = query.trim()
                                if (submittedQuery.isNotBlank()) reloadTick += 1
                            }
                            .focusable()
                            .testTag("nsrplay_search_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar en NSRPlay",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compactHeader) 6.dp else 8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeChip(
                        label = "Películas",
                        selected = mediaType == NsrMediaType.MOVIE,
                        primaryColor = primaryColor,
                        onClick = {
                            if (mediaType != NsrMediaType.MOVIE) {
                                mediaType = NsrMediaType.MOVIE
                                selectedGenre = null
                                submittedQuery = ""
                                query = ""
                            }
                        }
                    )
                    TypeChip(
                        label = "Series",
                        selected = mediaType == NsrMediaType.SERIES,
                        primaryColor = primaryColor,
                        onClick = {
                            if (mediaType != NsrMediaType.SERIES) {
                                mediaType = NsrMediaType.SERIES
                                selectedGenre = null
                                submittedQuery = ""
                                query = ""
                            }
                        }
                    )

                    if (submittedQuery.isNotBlank()) {
                        Text(
                            text = "Resultados: \"$submittedQuery\"",
                            color = Color(0xFFAAB8C5),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Limpiar",
                            color = primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    query = ""
                                    submittedQuery = ""
                                }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compactHeader) 5.dp else 7.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 10.dp)
                ) {
                    item {
                        GenreChip(
                            label = "Todos",
                            selected = selectedGenre == null,
                            primaryColor = primaryColor,
                            onClick = {
                                selectedGenre = null
                                if (submittedQuery.isNotBlank()) {
                                    submittedQuery = ""
                                    query = ""
                                }
                            }
                        )
                    }
                    items(NsrGenres) { (label, slug) ->
                        GenreChip(
                            label = label,
                            selected = selectedGenre == slug,
                            primaryColor = primaryColor,
                            onClick = {
                                selectedGenre = if (selectedGenre == slug) null else slug
                                if (submittedQuery.isNotBlank()) {
                                    submittedQuery = ""
                                    query = ""
                                }
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                !repository.isConfigured -> InfoState(
                    title = "NSRPlay no está configurado",
                    message = "Abre Ajustes > NSRPlay y pega tu API Key. Se guarda en este dispositivo; no necesitas editar código ni recompilar.",
                    primaryColor = primaryColor
                )

                isLoading && items.isEmpty() -> LoadingState("Buscando en NSRPlay...")

                error != null && items.isEmpty() -> ErrorState(
                    message = error ?: "Error desconocido",
                    primaryColor = primaryColor,
                    onRetry = { retry() }
                )

                items.isEmpty() -> InfoState(
                    title = if (submittedQuery.isBlank()) "Sin resultados en este momento" else "Sin coincidencias",
                    message = if (submittedQuery.isBlank()) {
                        "El índice de NSRPlay se genera al momento y puede variar. Prueba otro género o busca un título."
                    } else {
                        "No encontramos \"$submittedQuery\". Prueba con otro nombre o revisa la ortografía."
                    },
                    primaryColor = primaryColor
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier
                        .widthIn(max = 1500.dp)
                        .fillMaxSize()
                        .testTag("nsrplay_grid"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = items,
                        key = { "${it.type.apiValue}:${it.slug}" }
                    ) { media ->
                        NsrPosterCard(
                            media = media,
                            primaryColor = primaryColor,
                            onClick = { detailMedia = media }
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    color = primaryColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (hasNextPage && submittedQuery.isBlank()) {
                                Button(
                                    onClick = { loadMore() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1A2632),
                                        contentColor = primaryColor
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cargar más", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "Índice generado por NSRPlay. El contenido pertenece a sus proveedores.",
                                    color = Color(0xFF66757F),
                                    fontSize = 9.sp,
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

@Composable
private fun NsrPosterCard(
    media: NsrMedia,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF0F1722))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) primaryColor else Color(0xFF1B2836),
                shape = RoundedCornerShape(9.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(Color(0xFF131B24))
        ) {
            if (media.posterOrNull != null) {
                AsyncImage(
                    model = media.posterOrNull,
                    contentDescription = media.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (media.type == NsrMediaType.MOVIE) Icons.Default.Movie
                        else Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color(0xFF40505E),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            media.rating?.let { rating ->
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = rating,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = media.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun NsrPlayDetailScreen(
    media: NsrMedia,
    repository: NsrPlayRepository,
    primaryColor: Color,
    onBack: () -> Unit
) {
    var detail by remember(media.slug) { mutableStateOf<NsrMedia?>(null) }
    var isLoading by remember(media.slug) { mutableStateOf(true) }
    var error by remember(media.slug) { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    var selectedSeasonNumber by remember { mutableIntStateOf(1) }
    var selectedEpisode by remember { mutableStateOf<NsrEpisode?>(null) }
    var episodeServers by remember { mutableStateOf<List<NsrServer>>(emptyList()) }
    var isLoadingServers by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    LaunchedEffect(media.slug, media.type, reloadTick) {
        isLoading = true
        error = null
        try {
            val info = repository.getInfo(media.slug, media.type)
            detail = info
            selectedSeasonNumber = info.seasons.firstOrNull()?.number ?: 1
        } catch (e: NsrPlayException) {
            error = e.message
        } catch (_: Throwable) {
            error = "No se pudo cargar el detalle desde NSRPlay."
        } finally {
            isLoading = false
        }
    }

    fun openPlayer(server: NsrServer, label: String) {
        val intent = Intent(context, PlayerWebActivity::class.java).apply {
            putExtra(PlayerWebActivity.EXTRA_URL, server.embedUrl)
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
                            text = "Servidores disponibles",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (info.servers.isEmpty()) {
                        item {
                            Text(
                                text = "NSRPlay no devolvió servidores para este título. Suele pasar cuando el proveedor retiró el enlace; prueba otro título.",
                                color = Color(0xFF8F9DAA),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        items(info.servers) { server ->
                            NsrServerRow(
                                server = server,
                                primaryColor = primaryColor,
                                onClick = {
                                    openPlayer(server, "${info.title} · ${server.language}")
                                }
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
                                            selectedEpisode = null
                                            episodeServers = emptyList()
                                            serverError = null
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
                                selected = selectedEpisode?.number == episode.number &&
                                    selectedEpisode?.season == episode.season,
                                primaryColor = primaryColor,
                                onClick = {
                                    selectedEpisode = episode
                                    episodeServers = emptyList()
                                    serverError = null
                                    isLoadingServers = true
                                    scope.launch {
                                        try {
                                            episodeServers = repository.getServers(
                                                info.slug,
                                                episode.season,
                                                episode.number
                                            )
                                            serverError = null
                                        } catch (e: NsrPlayException) {
                                            serverError = e.message
                                        } catch (_: Throwable) {
                                            serverError = "No se pudieron obtener los servidores."
                                        } finally {
                                            isLoadingServers = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    val episode = selectedEpisode
                    if (episode != null) {
                        item {
                            Text(
                                text = "Servidores · T${episode.season} E${episode.number}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (isLoadingServers) {
                            item { LoadingState("Buscando servidores...") }
                        } else if (serverError != null) {
                            item {
                                Text(
                                    text = serverError ?: "",
                                    color = Color(0xFFFF7A8A),
                                    fontSize = 11.sp
                                )
                            }
                        } else if (episodeServers.isEmpty()) {
                            item {
                                Text(
                                    text = "Este episodio no tiene servidores disponibles ahora mismo.",
                                    color = Color(0xFF8F9DAA),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            items(episodeServers) { server ->
                                NsrServerRow(
                                    server = server,
                                    primaryColor = primaryColor,
                                    onClick = {
                                        openPlayer(
                                            server,
                                            "${info.title} T${episode.season}E${episode.number} · ${server.language}"
                                        )
                                    }
                                )
                            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) Color(0xFF1A2833) else Color(0xFF0E161F))
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) primaryColor else Color(0xFF141F2B))
            .clickable(onClick = onClick)
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) primaryColor.copy(alpha = 0.18f) else Color(0xFF101923))
            .border(
                1.dp,
                if (selected) primaryColor else Color(0xFF1D2B39),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
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

@Composable
private fun InfoState(
    title: String,
    message: String,
    primaryColor: Color
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
            tint = primaryColor.copy(alpha = 0.7f),
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = message,
            color = Color(0xFF8F9DAA),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
