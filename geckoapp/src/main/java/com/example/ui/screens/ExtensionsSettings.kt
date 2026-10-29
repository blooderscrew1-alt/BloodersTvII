package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.extensions.ExtensionEngine
import com.example.extensions.WebExtension
import com.example.extensions.ZipExtensionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun ExtensionsSettingsContent(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var extensions by remember { mutableStateOf(ExtensionEngine.loadExtensions(context)) }
    var isInstalling by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }

    fun persist(updated: List<WebExtension>) {
        extensions = updated
        ExtensionEngine.saveExtensions(context, updated)
    }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        isInstalling = true
        feedback = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ZipExtensionParser.parseZip(stream)
                    } ?: Result.failure(Exception("No se pudo abrir el archivo."))
                } catch (t: Throwable) {
                    Result.failure(t)
                }
            }

            result.onSuccess { parsed ->
                val displayName = if (parsed.name.startsWith("__MSG_")) {
                    "Extensión importada"
                } else {
                    parsed.name
                }
                val extension = WebExtension(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    description = parsed.description,
                    author = "Importada",
                    version = parsed.version,
                    matchPatterns = parsed.matchPatterns,
                    scriptContent = parsed.scriptContent,
                    rulesJson = parsed.rulesJson,
                    isBuiltIn = false
                )
                persist(extensions + extension)
                feedback = "Extensión instalada. Se aplicará al abrir el reproductor web."
                feedbackIsError = false
            }.onFailure { error ->
                feedback = error.message ?: "No se pudo instalar la extensión."
                feedbackIsError = true
            }
            isInstalling = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(
                title = "Extensiones de Chrome / Edge",
                subtitle = "Instala paquetes .zip o .crx para bloquear anuncios, dominios, redirecciones y pop-ups.",
                icon = Icons.Default.Extension,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "El paquete debe contener manifest.json. Se admiten content scripts (JS/CSS), " +
                            "background/service worker y reglas estáticas de declarativeNetRequest " +
                            "(block, redirect y upgradeScheme).",
                        color = Color(0xFFAFC0CE),
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = { installLauncher.launch(arrayOf("*/*")) },
                        enabled = !isInstalling,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("install_extension_button")
                    ) {
                        if (isInstalling) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instalando...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Instalar extensión desde archivo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    feedback?.let { message ->
                        Text(
                            text = message,
                            color = if (feedbackIsError) Color(0xFFFF8A96) else primaryColor,
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "Las extensiones se ejecutan dentro del reproductor web de esta app; " +
                            "no se concede acceso al resto del dispositivo.",
                        color = Color(0xFF788896),
                        fontSize = 10.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extensiones (${extensions.count { it.isEnabled }}/${extensions.size} activas)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Los cambios se aplican al reabrir el reproductor",
                    color = Color(0xFF7F8F9F),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(extensions, key = { it.id }) { extension ->
            ExtensionRow(
                extension = extension,
                primaryColor = primaryColor,
                onToggle = { enabled ->
                    persist(
                        extensions.map {
                            if (it.id == extension.id) it.copy(isEnabled = enabled) else it
                        }
                    )
                    feedback = null
                },
                onDelete = {
                    persist(extensions.filterNot { it.id == extension.id })
                    feedback = "Extensión eliminada."
                    feedbackIsError = false
                }
            )
        }

        item {
            SettingsCard(
                title = "Cómo se aplican",
                subtitle = "Orden de protección del reproductor web.",
                icon = Icons.Default.Refresh,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExtensionBullet("1. Filtro de dominios publicitarios propio de la app.")
                    ExtensionBullet("2. Reglas declarativeNetRequest de cada extensión activa.")
                    ExtensionBullet("3. Content scripts y CSS de las extensiones en la página.")
                    ExtensionBullet("4. Bloqueo de window.open, pop-ups y redirecciones automáticas.")
                    ExtensionBullet("5. Script de auto-skip de anuncios en vídeo.")
                }
            }
        }
    }
}

@Composable
private fun ExtensionRow(
    extension: WebExtension,
    primaryColor: Color,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1722))
            .border(1.dp, Color(0xFF1B2836), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (extension.isEnabled) primaryColor.copy(alpha = 0.16f)
                    else Color(0xFF1A242E)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = extension.iconEmoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = extension.name,
                    color = if (extension.isEnabled) Color.White else Color(0xFF8F9DAA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (extension.isBuiltIn) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INTEGRADA",
                        color = primaryColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(
                text = extension.description,
                color = Color(0xFF8F9DAA),
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "v${extension.version} · ${extension.author}",
                color = Color(0xFF66757F),
                fontSize = 9.sp,
                maxLines = 1
            )
        }

        Switch(
            checked = extension.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = primaryColor,
                uncheckedThumbColor = Color(0xFF8F9DAA),
                uncheckedTrackColor = Color(0xFF243343)
            ),
            modifier = Modifier.testTag("extension_toggle_${extension.id}")
        )

        if (!extension.isBuiltIn) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar extensión",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExtensionBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = "•", color = Color(0xFFCCFF00), fontSize = 11.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color(0xFFAFC0CE), fontSize = 11.sp)
    }
}
