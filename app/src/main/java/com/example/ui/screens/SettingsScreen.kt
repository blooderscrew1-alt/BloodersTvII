package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.model.BufferMode
import com.example.data.model.ThemeAccent
import com.example.data.player.SmartPlayerManager
import com.example.data.repository.IptvRepository

@Composable
fun SettingsScreen(
    repository: IptvRepository,
    playerManager: SmartPlayerManager,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by repository.themeAccent.collectAsState()
    val bufferMode by repository.bufferMode.collectAsState()
    val parentalPin by repository.parentalPin.collectAsState()
    val lockedCategories by repository.lockedCategories.collectAsState()
    val blockGamblingAds by repository.blockGamblingAds.collectAsState()
    val playerMode by repository.playerMode.collectAsState()

    var cacheBytes by remember { mutableLongStateOf(SmartPlayerManager.getCacheSizeBytes(context)) }
    var newPinText by remember { mutableStateOf("") }
    var pinFeedback by remember { mutableStateOf<String?>(null) }

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13)),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Ajustes y Personalización",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Streaming, control parental, apariencia, proveedores y respaldo.",
                color = Color(0xFF8F9DAA),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsSectionTabs(
                selected = selectedSection,
                onSelect = { selectedSection = it },
                primaryColor = primaryColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedSection == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {

        // Section 1: Themes / Dark Mode Accent
        item {
            SettingsCard(
                title = "Tema y Modo Oscuro Personalizable",
                subtitle = "Selecciona el acento de color de la interfaz de usuario.",
                icon = Icons.Default.Palette,
                primaryColor = primaryColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeAccent.values().forEach { theme ->
                        val isSelected = currentTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1B2836) else Color(0xFF111822))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(theme.primaryHex) else Color(0xFF1E2A38),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { repository.setThemeAccent(theme) }
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(theme.primaryHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = theme.label.substringBefore(" "),
                                color = if (isSelected) Color.White else Color(0xFF8F9DAA),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Smart Cache & Low Speed Buffer Control
        item {
            SettingsCard(
                title = "Sistema de Caché Inteligente y Buffer",
                subtitle = "Reproducción fluida incluso en conexiones lentas o inestables.",
                icon = Icons.Default.Speed,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BufferMode.values().forEach { mode ->
                        val isSelected = bufferMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1A2634) else Color(0xFF101720))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) primaryColor else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    repository.setBufferMode(mode)
                                    playerManager.setBufferMode(mode)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (isSelected) primaryColor else Color(0xFF5E6D7D), CircleShape)
                                    .background(if (isSelected) primaryColor else Color.Transparent)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = mode.label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.description,
                                    color = Color(0xFF8293A4),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cache size indicator & Clear button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF101821))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Espacio en disco utilizado",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            val sizeMb = cacheBytes / (1024 * 1024)
                            Text(
                                text = "$sizeMb MB / 150 MB de caché optimizado",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                SmartPlayerManager.clearCache(context)
                                cacheBytes = SmartPlayerManager.getCacheSizeBytes(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F2B38),
                                contentColor = Color(0xFFFF5252)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar Caché", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section 3: Parental Control & PIN
        item {
            SettingsCard(
                title = "Control Parental y Bloqueo de Categorías",
                subtitle = "Protege categorías con un código PIN de 4 dígitos.",
                icon = Icons.Default.Lock,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PIN actual configurado: **** (Por defecto: 0000)",
                        color = Color(0xFFA1B0BE),
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPinText,
                            onValueChange = { if (it.length <= 4) newPinText = it },
                            label = { Text("Nuevo PIN (4 números)") },
                            placeholder = { Text("ej: 1234") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF243343),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (newPinText.length == 4) {
                                    repository.setParentalPin(newPinText)
                                    pinFeedback = "PIN actualizado correctamente"
                                    newPinText = ""
                                } else {
                                    pinFeedback = "El PIN debe tener 4 dígitos"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Guardar", fontWeight = FontWeight.Bold)
                        }
                    }

                    pinFeedback?.let {
                        Text(
                            text = it,
                            color = if (it.contains("correctamente")) primaryColor else Color(0xFFFF5252),
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "Categorías bloqueadas actualmente: ${lockedCategories.joinToString().ifEmpty { "Ninguna" }}",
                        color = Color(0xFF8F9DAA),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section 4: TV Remote & D-Pad Instructions
        item {
            SettingsCard(
                title = "Controles Físicos TV Box y Táctil",
                subtitle = "Mapeo de teclas para control remoto de Android TV.",
                icon = Icons.Default.Tv,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemoteShortcutRow("OK / Enter / Centro", "Seleccionar canal o pausar/reanudar")
                    RemoteShortcutRow("Arriba / Abajo", "Zapping rápido de canales en pantalla completa")
                    RemoteShortcutRow("Izquierda / Derecha", "Moverse entre pestañas y categorías")
                    RemoteShortcutRow("Atrás (Back)", "Salir de pantalla completa o volver al menú")
                    RemoteShortcutRow("Pantalla Táctil", "Soporte completo para toques, gestos y tablets")
                }
            }
        }

        // Section 5: Watch History Cleaner
        item {
            SettingsCard(
                title = "Bloqueo de anuncios de apuestas y casino",
                subtitle = "Bloquea dominios publicitarios de apuestas, casino y clones con subdominios variables.",
                icon = Icons.Default.Block,
                primaryColor = primaryColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (blockGamblingAds) "Bloqueo reforzado activado" else "Bloqueo reforzado desactivado",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Se aplica a NSRPlay, Vimeus y Unlimplay. Puede requerir reiniciar el reproductor.",
                            color = Color(0xFF8F9DAA),
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = blockGamblingAds,
                        onCheckedChange = { repository.setBlockGamblingAds(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = primaryColor
                        )
                    )
                }
            }
        }

        item {
            SettingsCard(
                title = "Modo de dispositivo",
                subtitle = "Ajusta el reproductor para TV Box o para teléfono.",
                icon = Icons.Default.Tv,
                primaryColor = primaryColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "auto" to "Automático",
                        "tv" to "TV Box",
                        "phone" to "Teléfono"
                    ).forEach { (value, label) ->
                        val selected = playerMode == value
                        Button(
                            onClick = { repository.setPlayerMode(value) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) primaryColor else Color(0xFF1E2833),
                                contentColor = if (selected) Color.Black else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "TV Box activa la compatibilidad con WebView antiguos. Teléfono usa el modo moderno.",
                    color = Color(0xFF8F9DAA),
                    fontSize = 10.sp
                )
            }
        }

        item {
            SettingsCard(
                title = "Historial de Vista",
                subtitle = "Administra las películas y series que has comenzado a ver.",
                icon = Icons.Default.Cached,
                primaryColor = primaryColor
            ) {
                Button(
                    onClick = { repository.clearHistory() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2833),
                        contentColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Borrar Todo el Historial de Vista", fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
                }
            } else if (selectedSection == 1) {
                NsrPlaySettingsContent(
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedSection == 2) {
                VimeusSettingsContent(
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedSection == 3) {
                UnlimplaySettingsContent(
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedSection == 4) {
                ExtensionsSettingsContent(
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BackupSettingsContent(
                    repository = repository,
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF101A24))
            .padding(3.dp)
    ) {
        listOf("General", "NSRPlay", "Vimeus", "Unlimplay", "Extensiones", "Respaldo").forEachIndexed { index, label ->
            val isSelected = selected == index
            val tabInteraction = remember(index) { MutableInteractionSource() }
            val isTabFocused by tabInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        when {
                            isSelected -> primaryColor
                            isTabFocused -> Color(0xFF1A2A38)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = if (isTabFocused) 2.dp else 0.dp,
                        color = if (isTabFocused) primaryColor else Color.Transparent,
                        shape = RoundedCornerShape(7.dp)
                    )
                    .focusable(interactionSource = tabInteraction)
                    .clickable(
                        interactionSource = tabInteraction,
                        indication = null
                    ) { onSelect(index) }
                    .padding(vertical = 8.dp)
                    .testTag(
                        when (index) {
                            0 -> "settings_tab_general"
                            1 -> "settings_tab_nsrplay"
                            2 -> "settings_tab_vimeus"
                            3 -> "settings_tab_unlimplay"
                            4 -> "settings_tab_extensions"
                            else -> "settings_tab_backup"
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.Black else Color(0xFF9FB0C0),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1722))
            .border(1.dp, Color(0xFF1B2836), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = Color(0xFF8F9DAA), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        content()
    }
}

@Composable
fun RemoteShortcutRow(key: String, action: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF131B24))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = key, color = Color(0xFFCCFF00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = action,
            color = Color(0xFFC0CAD5),
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
    }
}
