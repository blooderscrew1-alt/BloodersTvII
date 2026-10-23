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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.vimeus.VimeusCatalogCache
import com.example.data.vimeus.VimeusConfigStore
import com.example.data.vimeus.VimeusRepository
import com.example.data.vimeus.VimeusType
import kotlinx.coroutines.launch

@Composable
fun VimeusSettingsContent(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { VimeusConfigStore(context.applicationContext) }
    val repository = remember { VimeusRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(store.apiKey()) }
    var viewKey by remember { mutableStateOf(store.viewKey()) }
    var baseUrl by remember { mutableStateOf(store.baseUrl()) }
    var showApi by remember { mutableStateOf(false) }
    var showView by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }

    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = Color(0xFF243343),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = Color(0xFF8F9DAA),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = primaryColor
    )

    fun save() {
        store.save(apiKey, viewKey, baseUrl)
        VimeusCatalogCache.invalidate()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(
                title = "Vimeus · Películas, series y anime",
                subtitle = "API Key para los listados y View Key para el reproductor embed.",
                icon = Icons.Default.PlayArrow,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    if (repository.isConfigured) primaryColor else Color(0xFFFF5252),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (repository.isConfigured) {
                                "Vimeus configurado en este dispositivo"
                            } else {
                                "Vimeus sin configurar: faltan API Key o View Key"
                            },
                            color = Color(0xFFB8C6D3),
                            fontSize = 11.sp
                        )
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key (X-API-Key)", fontSize = 11.sp) },
                        placeholder = { Text("ak_...", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = if (showApi) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApi = !showApi }) {
                                Icon(
                                    imageVector = if (showApi) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF8F9DAA)
                                )
                            }
                        },
                        colors = colors,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vimeus_api_key")
                    )

                    OutlinedTextField(
                        value = viewKey,
                        onValueChange = { viewKey = it },
                        label = { Text("View Key (embed)", fontSize = 11.sp) },
                        placeholder = { Text("Clave única de tu cuenta", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = if (showView) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showView = !showView }) {
                                Icon(
                                    imageVector = if (showView) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF8F9DAA)
                                )
                            }
                        },
                        colors = colors,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vimeus_view_key")
                    )

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("URL base", fontSize = 11.sp) },
                        placeholder = { Text("https://vimeus.com", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        colors = colors,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vimeus_base_url")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                save()
                                feedback = "Configuración de Vimeus guardada."
                                feedbackIsError = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                save()
                                isTesting = true
                                feedback = null
                                scope.launch {
                                    try {
                                        val page = repository.getCatalog(VimeusType.MOVIE, 1)
                                        feedback = "Conexión correcta · ${page.items.size} películas en la primera página."
                                        feedbackIsError = false
                                    } catch (e: Exception) {
                                        feedback = e.message ?: "No se pudo conectar con Vimeus."
                                        feedbackIsError = true
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = !isTesting,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    color = primaryColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text("Probar", color = primaryColor, fontSize = 11.sp)
                            }
                        }
                    }

                    feedback?.let { message ->
                        Text(
                            text = message,
                            color = if (feedbackIsError) Color(0xFFFF8A96) else primaryColor,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFF66757F),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "La API Key debe ir en el header X-API-Key. La View Key valida los embeds.",
                            color = Color(0xFF66757F),
                            fontSize = 9.sp
                        )
                    }

                    TextButton(
                        onClick = {
                            store.reset()
                            apiKey = store.apiKey()
                            viewKey = store.viewKey()
                            baseUrl = store.baseUrl()
                            VimeusCatalogCache.invalidate()
                            feedback = "Vimeus restaurado a los valores incluidos en la app."
                            feedbackIsError = false
                        }
                    ) {
                        Text("Restaurar valores por defecto", color = Color(0xFFFF8A96), fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = "Cómo se usa en la app",
                subtitle = "Vimeus aparece junto a NSRPlay en Películas y Series.",
                icon = Icons.Default.PlayArrow,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Catálogo paginado de películas, series y anime.", color = Color(0xFFAFC0CE), fontSize = 11.sp)
                    Text("• Detalle con temporadas y episodios cuando Vimeus los publica.", color = Color(0xFFAFC0CE), fontSize = 11.sp)
                    Text("• Búsqueda por título usando IDs de IMDb/TMDb.", color = Color(0xFFAFC0CE), fontSize = 11.sp)
                    Text("• Reproducción en el reproductor web con bloqueo de anuncios.", color = Color(0xFFAFC0CE), fontSize = 11.sp)
                }
            }
        }
    }
}
