package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.SettingsBackupManager
import com.example.data.repository.IptvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BACKUP_PREFS = "blooders_tv_prefs"
private const val LAST_BACKUP_URI = "last_backup_uri"

private fun writeBackupFile(context: Context, uri: Uri, json: String) {
    val resolver = context.contentResolver
    val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
        ?: resolver.openOutputStream(uri)
        ?: throw IllegalStateException("No se pudo abrir el archivo de respaldo.")
    stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
}

private fun rememberBackupUri(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
    context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(LAST_BACKUP_URI, uri.toString())
        .apply()
}

@Composable
fun BackupSettingsContent(
    repository: IptvRepository,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupPrefs = remember {
        context.getSharedPreferences(BACKUP_PREFS, Context.MODE_PRIVATE)
    }

    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasSavedLocation by remember {
        mutableStateOf(!backupPrefs.getString(LAST_BACKUP_URI, null).isNullOrBlank())
    }

    fun payload(): String = SettingsBackupManager.buildJson(context, repository)

    fun status(text: String, error: Boolean = false) {
        message = text
        messageIsError = error
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) { payload() }
                withContext(Dispatchers.IO) {
                    writeBackupFile(context, uri, json)
                }
                rememberBackupUri(context, uri)
                hasSavedLocation = true
                status("Respaldo exportado. Las próximas exportaciones reemplazarán este archivo.")
                warnings = emptyList()
            } catch (t: Throwable) {
                status(t.localizedMessage ?: "No se pudo exportar el respaldo.", true)
            } finally {
                isWorking = false
            }
        }
    }

    fun startExport() {
        val savedUri = backupPrefs.getString(LAST_BACKUP_URI, null)
        if (savedUri.isNullOrBlank()) {
            exportLauncher.launch("blooders-respaldo.json")
            return
        }
        isWorking = true
        status("Actualizando el respaldo existente...")
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) { payload() }
                withContext(Dispatchers.IO) {
                    writeBackupFile(context, Uri.parse(savedUri), json)
                }
                status("Respaldo actualizado: se reemplazó el archivo anterior.")
                warnings = emptyList()
            } catch (t: Throwable) {
                // The document may have been deleted or its permission revoked;
                // ask for a new location and remember it for future exports.
                backupPrefs.edit().remove(LAST_BACKUP_URI).apply()
                hasSavedLocation = false
                status("No se pudo actualizar el respaldo. Elige de nuevo dónde guardarlo.", true)
                exportLauncher.launch("blooders-respaldo.json")
            } finally {
                isWorking = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        status("Importando respaldo...")
        warnings = emptyList()
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("No se pudo leer el archivo.")
                }
                val result = SettingsBackupManager.importJson(
                    context = context,
                    repository = repository,
                    jsonText = text,
                    onProgress = { step ->
                        scope.launch(Dispatchers.Main) { status(step) }
                    }
                )
                warnings = result.warnings
                status(
                    "Importación lista: ${result.playlistsAdded} listas agregadas, " +
                        "${result.playlistsSkipped} omitidas, ${result.playlistsFailed} con error."
                )
            } catch (t: Throwable) {
                status(t.localizedMessage ?: "No se pudo importar el respaldo.", true)
            } finally {
                isWorking = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(
                title = "Respaldo y traslado a otro dispositivo",
                subtitle = "Exporta listas y ajustes, o compártelos para configurar otro equipo sin escribirlos de nuevo.",
                icon = Icons.Default.CloudUpload,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Incluye listas M3U por URL, cuentas Xtream, tema, buffer, PIN, " +
                            "categorías bloqueadas, favoritos y claves de NSRPlay/Vimeus.",
                        color = Color(0xFFAFC0CE),
                        fontSize = 11.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                startExport()
                            },
                            enabled = !isWorking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_export_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val json = payload()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Respaldo BloodersTv")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Compartir respaldo de BloodersTv")
                                )
                            },
                            enabled = !isWorking,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir", color = primaryColor, fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "Cada exportación reemplaza el mismo archivo para no acumular copias. " +
                            "La primera vez eliges la ubicación.",
                        color = Color(0xFF7F91A1),
                        fontSize = 10.sp
                    )

                    if (hasSavedLocation) {
                        TextButton(
                            onClick = {
                                backupPrefs.edit().remove(LAST_BACKUP_URI).apply()
                                hasSavedLocation = false
                                status("Elige la nueva ubicación del respaldo.")
                                exportLauncher.launch("blooders-respaldo.json")
                            },
                            enabled = !isWorking
                        ) {
                            Text(
                                text = "Cambiar ubicación del respaldo",
                                color = primaryColor,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        enabled = !isWorking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A2A38),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("backup_import_button")
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Procesando...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar desde archivo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    message?.let { text ->
                        Text(
                            text = text,
                            color = if (messageIsError) Color(0xFFFF8A96) else primaryColor,
                            fontSize = 11.sp
                        )
                    }

                    if (warnings.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1B1410), RoundedCornerShape(7.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    color = Color(0xFFFFC46B),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = "Qué se transfiere",
                subtitle = "Todo se guarda en un archivo JSON local.",
                icon = Icons.Default.Share,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BackupBullet("Listas M3U por URL y cuentas Xtream (se vuelven a descargar al importar).")
                    BackupBullet("Tema, modo de buffer, PIN parental y categorías bloqueadas.")
                    BackupBullet("Favoritos y claves de NSRPlay/Vimeus.")
                    BackupBullet("Las listas pegadas como texto no se pueden transferir; usa URL o Xtream.")
                    BackupBullet("El historial y las extensiones instaladas no se incluyen.")
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1B1410), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFF9F1C), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "El respaldo contiene contraseñas de listas y API keys. Compártelo solo por canales seguros.",
                    color = Color(0xFFFFC46B),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun BackupBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = "•", color = Color(0xFFCCFF00), fontSize = 11.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color(0xFFAFC0CE), fontSize = 11.sp)
    }
}
