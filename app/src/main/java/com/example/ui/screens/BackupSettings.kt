package com.example.ui.screens

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettingsContent(
    repository: IptvRepository,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }

    fun payload(): String = SettingsBackupManager.buildJson(context, repository)

    fun status(text: String, error: Boolean = false) {
        message = text
        messageIsError = error
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) { payload() }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("No se pudo crear el archivo.")
                }
                status("Respaldo exportado correctamente.")
                warnings = emptyList()
            } catch (t: Throwable) {
                status(t.localizedMessage ?: "No se pudo exportar el respaldo.", true)
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
                                val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                exportLauncher.launch("blooders-respaldo-$date.json")
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
