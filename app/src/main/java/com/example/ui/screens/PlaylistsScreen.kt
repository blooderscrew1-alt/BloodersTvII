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
        // Top action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_playlist_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Agregar Lista", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1722),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(1.5.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .testTag("add_playlist_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Añadir Lista de Contenido",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF141E28),
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = primaryColor
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Lista M3U / M3U8", fontSize = 12.sp, color = if (selectedTab == 0) primaryColor else Color(0xFF8F9DAA)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Xtream Codes API", fontSize = 12.sp, color = if (selectedTab == 1) primaryColor else Color(0xFF8F9DAA)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Nombre de la lista (Opcional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF243343),
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color(0xFF8F9DAA),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // M3U URL or Text Paste
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it },
                        label = { Text(if (isPasteMode) "Contenido M3U (#EXTM3U...)" else "URL de la lista M3U/M3U8") },
                        placeholder = { Text(if (isPasteMode) "#EXTM3U..." else "http://servidor.com/lista.m3u8") },
                        minLines = if (isPasteMode) 4 else 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF243343),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color(0xFF8F9DAA),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isPasteMode) "Cambiar a enlace URL" else "¿Pegar texto M3U completo? Toca aquí",
                        color = primaryColor,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { isPasteMode = !isPasteMode }
                            .padding(vertical = 4.dp)
                    )
                } else {
                    // Xtream Codes inputs
                    OutlinedTextField(
                        value = xtreamHost,
                        onValueChange = { xtreamHost = it },
                        label = { Text("Servidor URL (ej: http://dominio.tv:8080)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF243343),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = Color(0xFF8F9DAA),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("Usuario") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF243343),
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = Color(0xFF8F9DAA),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("Contraseña") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF243343),
                                focusedLabelColor = primaryColor,
                                unfocusedLabelColor = Color(0xFF8F9DAA),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2833))
                    ) {
                        Text("Cancelar", color = Color(0xFFC0CAD5))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                if (m3uUrl.isNotBlank()) {
                                    onSubmitM3u(playlistName, m3uUrl.trim(), !isPasteMode)
                                }
                            } else {
                                if (xtreamHost.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()) {
                                    onSubmitXtream(playlistName, xtreamHost.trim(), xtreamUser.trim(), xtreamPass.trim())
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Continuar y Filtrar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
