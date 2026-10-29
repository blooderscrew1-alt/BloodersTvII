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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.nsrplay.NsrPlayCatalogCache
import com.example.data.nsrplay.NsrPlayConfigStore
import com.example.data.nsrplay.NsrPlayException
import com.example.data.nsrplay.NsrPlayRepository
import kotlinx.coroutines.launch

@Composable
fun NsrPlaySettingsContent(
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { NsrPlayConfigStore(context.applicationContext) }
    val repository = remember { NsrPlayRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf(store.storedApiKey().orEmpty()) }
    var baseUrl by remember { mutableStateOf(store.baseUrl()) }
    var showKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var lastLatencyMs by remember { mutableLongStateOf(0L) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = primaryColor,
        unfocusedBorderColor = Color(0xFF243343),
        focusedLabelColor = primaryColor,
        unfocusedLabelColor = Color(0xFF8F9DAA),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = primaryColor
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(
                title = "NSRPlay · Catálogo de películas y series",
                subtitle = "Cambia la API Key y la URL del servicio sin tocar el código ni recompilar.",
                icon = Icons.Default.PlayArrow,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Current state
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    if (store.apiKey().isNotBlank()) primaryColor
                                    else Color(0xFFFF5252),
                                    CircleShape
                                )
                        )
                        Text(
                            text = when {
                                store.hasCustomApiKey() -> "Clave personalizada guardada en este dispositivo"
                                store.apiKey().isNotBlank() -> "Usando la clave incluida en la app"
                                else -> "Sin API Key configurada"
                            },
                            color = Color(0xFFB6C3CE),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key de NSRPlay") },
                        placeholder = { Text("np_live_... (vacío = clave incluida)") },
                        singleLine = true,
                        visualTransformation = if (showKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (showKey) "Ocultar clave" else "Mostrar clave",
                                    tint = Color(0xFF8F9DAA)
                                )
                            }
                        },
                        colors = fieldColors,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .widthIn(max = 620.dp)
                            .fillMaxWidth()
                            .testTag("nsrplay_api_key_field")
                    )

                    Text(
                        text = "Déjala vacía si quieres seguir usando la clave compilada en el APK. Se guarda solo en este dispositivo.",
                        color = Color(0xFF788896),
                        fontSize = 10.sp
                    )

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("URL del servicio NSRPlay") },
                        placeholder = { Text("https://nsrplay.space") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        colors = fieldColors,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .widthIn(max = 620.dp)
                            .fillMaxWidth()
                            .testTag("nsrplay_base_url_field")
                    )

                    Text(
                        text = "Puedes apuntar a tu propio proxy (por ejemplo, si no quieres exponer la clave en el APK).",
                        color = Color(0xFF788896),
                        fontSize = 10.sp
                    )

                    Button(
                        onClick = {
                            val cleanBase = baseUrl.trim()
                            if (cleanBase.isNotBlank() && !store.isValidBaseUrl(cleanBase)) {
                                feedback = "La URL debe empezar con http:// o https://"
                                feedbackIsError = true
                                return@Button
                            }
                            store.save(apiKey, cleanBase)
                            NsrPlayCatalogCache.invalidate()
                            apiKey = store.storedApiKey().orEmpty()
                            baseUrl = store.baseUrl()
                            feedback = "Configuración guardada. Se aplicará al abrir Películas, Series o buscar."
                            feedbackIsError = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .widthIn(max = 620.dp)
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("nsrplay_save_button")
                    ) {
                        Text("Guardar configuración", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier
                            .widthIn(max = 620.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isTesting) return@OutlinedButton
                                val cleanBase = baseUrl.trim()
                                if (cleanBase.isNotBlank() && !store.isValidBaseUrl(cleanBase)) {
                                    feedback = "La URL debe empezar con http:// o https://"
                                    feedbackIsError = true
                                    return@OutlinedButton
                                }
                                isTesting = true
                                feedback = null
                                scope.launch {
                                    val started = System.currentTimeMillis()
                                    try {
                                        val count = repository.testConnection(apiKey, cleanBase)
                                        lastLatencyMs = System.currentTimeMillis() - started
                                        feedback = "Conexión correcta · $count títulos en $lastLatencyMs ms"
                                        feedbackIsError = false
                                    } catch (e: NsrPlayException) {
                                        feedback = e.message ?: "NSRPlay rechazó la conexión."
                                        feedbackIsError = true
                                    } catch (_: Throwable) {
                                        feedback = "No se pudo conectar. Revisa la URL, la clave y tu red."
                                        feedbackIsError = true
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("nsrplay_test_button")
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    color = primaryColor,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Probando...", fontSize = 11.sp)
                            } else {
                                Text("Probar conexión", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = {
                                store.reset()
                                NsrPlayCatalogCache.invalidate()
                                apiKey = store.storedApiKey().orEmpty()
                                baseUrl = store.baseUrl()
                                lastLatencyMs = 0L
                                feedback = "Configuración restablecida a los valores del APK."
                                feedbackIsError = false
                            },
                            modifier = Modifier.testTag("nsrplay_reset_button")
                        ) {
                            Text("Restablecer", color = Color(0xFF9FB0C0), fontSize = 11.sp)
                        }
                    }

                    feedback?.let { message ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = if (feedbackIsError) Icons.Default.CloudOff
                                else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (feedbackIsError) Color(0xFFFF6B7A) else primaryColor,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(15.dp)
                            )
                            Text(
                                text = message,
                                color = if (feedbackIsError) Color(0xFFFF8A96) else Color(0xFFB6C3CE),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                title = "Cómo funciona NSRPlay",
                subtitle = "Qué se envía y qué se guarda en el teléfono.",
                icon = Icons.Default.CheckCircle,
                primaryColor = primaryColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SettingsBullet(primaryColor, "La app envía tu API Key en el header X-API-Key solo a la URL configurada.")
                    SettingsBullet(primaryColor, "La clave y la URL se guardan en SharedPreferences privadas de esta app.")
                    SettingsBullet(primaryColor, "No se guarda el catálogo ni los enlaces de video en el dispositivo.")
                    SettingsBullet(primaryColor, "Cada servidor abre el reproductor web con bloqueo de anuncios y pop-ups.")
                    SettingsBullet(primaryColor, "Si publicas la app, usa un proxy propio: cualquier clave dentro de un APK se puede extraer.")
                }
            }
        }
    }
}

@Composable
private fun SettingsBullet(accent: Color, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(5.dp)
                .background(accent, CircleShape)
        )
        Text(
            text = text,
            color = Color(0xFFAFC0CE),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

