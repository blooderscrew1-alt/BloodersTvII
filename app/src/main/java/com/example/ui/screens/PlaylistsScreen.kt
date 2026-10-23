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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryInfo
import com.example.data.model.PlaylistSource
import com.example.data.model.SourceType
import com.example.data.parser.IptvDownloader
import com.example.data.parser.M3uParser
import com.example.data.parser.XtreamCodesClient
import com.example.data.repository.IptvRepository
import com.example.ui.components.AdaptiveDialogFrame
import com.example.ui.components.CategoryFilterDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun PlaylistsScreen(
    repository: IptvRepository,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlists by repository.playlists.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var detectedCategories by remember { mutableStateOf<List<CategoryInfo>?>(null) }
    var pendingPlaylistData by remember { mutableStateOf<Pair<PlaylistSource, String>?>(null) }
    var isAnalyzingList by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Dialog for Category Filter before final import!
    detectedCategories?.let { categories ->
        CategoryFilterDialog(
            categories = categories,
            onDismiss = {
                detectedCategories = null
                pendingPlaylistData?.second?.let { path ->
                    try { File(path).delete() } catch (_: Throwable) {}
                }
                pendingPlaylistData = null
            },
            onConfirmSelection = { selectedCats ->
                val (source, filePath) = pendingPlaylistData ?: return@CategoryFilterDialog
                detectedCategories = null
                pendingPlaylistData = null

                isAnalyzingList = true
                statusMessage = "Importando canales seleccionados..."

                val tempFile = File(filePath)
                scope.launch(Dispatchers.IO) {
                    try {
                        val filteredItems = M3uParser.parseFilteredFromFile(
                            file = tempFile,
                            allowedCategories = selectedCats,
                            sourceId = source.id
                        )
                        repository.addPlaylistItems(
                            source = source.copy(selectedCategories = selectedCats, totalChannels = filteredItems.size),
                            newItems = filteredItems
                        )
                        withContext(Dispatchers.Main) {
                            isAnalyzingList = false
                            statusMessage = "Lista importada con éxito (${filteredItems.size} canales sincronizados)."
                        }
                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            isAnalyzingList = false
                            statusMessage = "Error al procesar la lista: ${t.localizedMessage ?: "Formato no válido"}"
                        }
                    } finally {
                        if (tempFile.exists()) tempFile.delete()
                    }
                }
            },
            primaryColor = primaryColor
        )
    }

    // Add Playlist Modal Dialog
    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            primaryColor = primaryColor,
            onSubmitM3u = { name, contentOrUrl, isUrl ->
                showAddDialog = false
                isAnalyzingList = true
                statusMessage = if (isUrl) "Descargando lista IPTV..." else "Procesando contenido M3U..."

                scope.launch(Dispatchers.IO) {
                    val fileResult = if (isUrl) {
                        IptvDownloader.downloadToFile(context, contentOrUrl)
                    } else {
                        IptvDownloader.saveTextToFile(context, contentOrUrl)
                    }

                    fileResult.fold(
                        onSuccess = { tempFile ->
                            val categories = M3uParser.extractCategoriesFromFile(tempFile)
                            withContext(Dispatchers.Main) {
                                isAnalyzingList = false
                                if (categories.isEmpty()) {
                                    tempFile.delete()
                                    statusMessage = "No se encontraron canales válidos (#EXTINF) en la lista."
                                } else {
                                    val source = PlaylistSource(
                                        id = UUID.randomUUID().toString(),
                                        name = name.ifBlank { "Mi Lista M3U" },
                                        type = if (isUrl) SourceType.M3U_URL else SourceType.M3U_TEXT,
                                        url = if (isUrl) contentOrUrl.trim() else ""
                                    )
                                    pendingPlaylistData = Pair(source, tempFile.absolutePath)
                                    detectedCategories = categories
                                }
                            }
                        },
                        onFailure = { err ->
                            withContext(Dispatchers.Main) {
                                isAnalyzingList = false
                                statusMessage = "Error: ${err.message ?: "No se pudo descargar la lista"}"
                            }
                        }
                    )
                }
            },
            onSubmitXtream = { name, host, user, pass ->
                showAddDialog = false
                isAnalyzingList = true
                statusMessage = "Conectando con Xtream Codes API..."

                scope.launch(Dispatchers.IO) {
                    val xtreamClient = XtreamCodesClient()
                    val catResult = xtreamClient.fetchCategories(host, user, pass)

                    catResult.fold(
                        onSuccess = { cats ->
                            val source = PlaylistSource(
                                id = UUID.randomUUID().toString(),
                                name = name.ifBlank { "Xtream ($user)" },
                                type = SourceType.XTREAM,
                                url = host.trim(),
                                username = user.trim(),
                                password = pass.trim()
                            )
                            val streamResult = xtreamClient.fetchStreams(host, user, pass, sourceId = source.id)
                            withContext(Dispatchers.Main) {
                                isAnalyzingList = false
                                streamResult.fold(
                                    onSuccess = { items ->
                                        if (items.isEmpty()) {
                                            statusMessage = "Conexión exitosa, pero no se encontraron canales en vivo en esta cuenta."
                                        } else {
                                            repository.addPlaylistItems(source, items)
                                            statusMessage = "Conexión Xtream exitosa (${items.size} canales cargados)."
                                        }
                                    },
                                    onFailure = { err ->
                                        statusMessage = "Error obteniendo canales: ${err.message}"
                                    }
                                )
                            }
                        },
                        onFailure = { err ->
                            withContext(Dispatchers.Main) {
                                isAnalyzingList = false
                                statusMessage = "Error de autenticación Xtream: ${err.message}"
                            }
                        }
                    )
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .padding(20.dp)
    ) {
        // Top action bar. On narrow screens the title and the button stack, so
        // the button can never be squeezed into a thin vertical strip.
        val compactHeader = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 560
        if (compactHeader) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlaylistsHeaderTitle()
                Spacer(modifier = Modifier.height(10.dp))
                AddPlaylistButton(
                    primaryColor = primaryColor,
                    onClick = { showAddDialog = true }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistsHeaderTitle(modifier = Modifier.weight(1f))
                AddPlaylistButton(
                    primaryColor = primaryColor,
                    onClick = { showAddDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading or status banner
        if (isAnalyzingList) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14202C))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusMessage ?: "Procesando lista...",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (statusMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14202C))
                    .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = statusMessage ?: "",
                    color = primaryColor,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // List of registered playlists
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sin listas de reproducción",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No tienes ninguna lista agregada. Agrega tu lista M3U, URL remota o credenciales Xtream Codes para cargar tus canales y contenidos.",
                        color = Color(0xFF8F9DAA),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar Lista Ahora", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1722))
                            .border(1.dp, Color(0xFF1C2B3A), RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A2735)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (playlist.type) {
                                        SourceType.XTREAM -> Icons.Default.Dns
                                        SourceType.M3U_URL -> Icons.Default.Link
                                        else -> Icons.Default.PlaylistPlay
                                    },
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = playlist.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${playlist.totalChannels} canales sincronizados • ${playlist.type.name}",
                                    color = Color(0xFF8F9DAA),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { repository.removePlaylist(playlist.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar lista",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsHeaderTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Gestor de Listas IPTV",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Soporte M3U, M3U8, Xtream Codes API y filtrado previo de contenido basura.",
            color = Color(0xFF8F9DAA),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AddPlaylistButton(
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.testTag("add_playlist_button")
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Agregar Lista", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
fun AddPlaylistDialog(
    onDismiss: () -> Unit,
    onSubmitM3u: (name: String, content: String, isUrl: Boolean) -> Unit,
    onSubmitXtream: (name: String, host: String, user: String, pass: String) -> Unit,
    primaryColor: Color
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var playlistName by remember { mutableStateOf("") }

    // M3U Tab
    var m3uUrl by remember { mutableStateOf("") }
    var isPasteMode by remember { mutableStateOf(false) }

    // Xtream Tab
    var xtreamHost by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    val canSubmit = if (selectedTab == 0) {
        m3uUrl.isNotBlank()
    } else {
        xtreamHost.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()
    }

    AdaptiveDialogFrame(
        onDismiss = onDismiss,
        primaryColor = primaryColor,
        maxWidth = 560.dp,
        maxHeight = 780.dp,
        testTag = "add_playlist_modal"
    ) {
        val compact = maxWidth < 430.dp
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = Color(0xFF243343),
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = Color(0xFF8F9DAA),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
            // Scrollable body. The action buttons live outside it, so they are
            // always visible no matter how small the screen or how many fields.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(if (compact) 16.dp else 20.dp)
            ) {
                Text(
                    text = "Añadir Lista de Contenido",
                    color = Color.White,
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Compact custom segmented control (lighter than TabRow and never clips).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141E28))
                        .padding(3.dp)
                ) {
                    listOf("Lista M3U / M3U8", "Xtream Codes").forEachIndexed { index, label ->
                        val selected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else Color(0xFF9FB0C0),
                                fontSize = if (compact) 11.sp else 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nombre de la lista (opcional)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = {
                            Text(
                                if (isPasteMode) "Contenido M3U (#EXTM3U...)"
                                else "URL de la lista M3U / M3U8"
                            )
                        },
                        placeholder = {
                            Text(if (isPasteMode) "#EXTM3U..." else "http://servidor.com/lista.m3u8")
                        },
                        singleLine = !isPasteMode,
                        minLines = if (isPasteMode) 3 else 1,
                        maxLines = if (isPasteMode) 6 else 1,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isPasteMode) "Cambiar a enlace URL"
                        else "¿Tienes el texto M3U? Toca aquí para pegarlo",
                        color = primaryColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .clickable { isPasteMode = !isPasteMode }
                            .padding(vertical = 8.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = xtreamHost,
                        onValueChange = { xtreamHost = it },
                        label = { Text("Servidor (ej: http://dominio.tv:8080)") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("Usuario") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("Clave") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Sticky footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C141D))
                    .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2833)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Cancelar", color = Color(0xFFC0CAD5), fontSize = 12.sp, maxLines = 1)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            if (m3uUrl.isNotBlank()) {
                                onSubmitM3u(playlistName, m3uUrl.trim(), !isPasteMode)
                            }
                        } else {
                            if (canSubmit) {
                                onSubmitXtream(
                                    playlistName,
                                    xtreamHost.trim(),
                                    xtreamUser.trim(),
                                    xtreamPass.trim()
                                )
                            }
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF26323D),
                        disabledContentColor = Color(0xFF7F8C99)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Importar", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
