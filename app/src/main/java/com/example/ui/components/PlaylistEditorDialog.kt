package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSource
import com.example.data.repository.IptvRepository

private data class EditableCategory(
    val name: String,
    val count: Int,
    val type: ItemType,
    val hidden: Boolean
)

@Composable
fun PlaylistEditorDialog(
    source: PlaylistSource,
    repository: IptvRepository,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val allItems by repository.items.collectAsState()
    val sourceItems = remember(allItems, source.id) {
        allItems.filter { it.sourceId == source.id }
    }
    val categories = remember(sourceItems) {
        sourceItems.groupBy { it.category }
            .map { (name, items) ->
                val dominant = items.groupingBy { it.type }.eachCount()
                    .maxByOrNull { it.value }?.key ?: ItemType.LIVE
                EditableCategory(
                    name = name,
                    count = items.size,
                    type = dominant,
                    hidden = items.all { it.isHidden }
                )
            }
            .sortedBy { it.name.lowercase() }
    }
    val wholeType = remember(sourceItems) {
        sourceItems.map { it.type }.distinct().singleOrNull()
    }
    val link = remember(source) { repository.playlistLink(source) }

    var name by remember(source.id) { mutableStateOf(source.name) }
    var renamingCategory by remember(source.id) { mutableStateOf<String?>(null) }
    var renameValue by remember(source.id) { mutableStateOf("") }

    AdaptiveDialogFrame(
        onDismiss = onDismiss,
        primaryColor = primaryColor,
        maxWidth = 760.dp,
        maxHeight = 780.dp,
        testTag = "playlist_editor_dialog"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Editar lista",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${sourceItems.size} elementos · ${source.type.name}",
                        color = Color(0xFF8F9DAA),
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la lista") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color(0xFF2A3A4A),
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color(0xFF8F9DAA)
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            repository.renamePlaylist(source.id, name)
                            Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar nombre", tint = primaryColor)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF0D1520))
                    .border(1.dp, Color(0xFF1E2D3C), RoundedCornerShape(7.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = link.ifBlank { "Lista local sin enlace" },
                    color = Color(0xFF93A6B8),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (link.isNotBlank()) {
                            clipboard.setText(AnnotatedString(link))
                            Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = link.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar enlace",
                        tint = if (link.isNotBlank()) primaryColor else Color(0xFF566575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reclasificar toda la lista",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("TV", wholeType == ItemType.LIVE, primaryColor) {
                    repository.setPlaylistItemsType(source.id, ItemType.LIVE)
                }
                TypeChip("Películas", wholeType == ItemType.MOVIE, primaryColor) {
                    repository.setPlaylistItemsType(source.id, ItemType.MOVIE)
                }
                TypeChip("Series", wholeType == ItemType.SERIES, primaryColor) {
                    repository.setPlaylistItemsType(source.id, ItemType.SERIES)
                }
                TypeChip("Adultos", wholeType == ItemType.ADULT, primaryColor) {
                    repository.setPlaylistItemsType(source.id, ItemType.ADULT)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Categorías (${categories.size})",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Toca el ojo para ocultar",
                    color = Color(0xFF7C8D9E),
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories, key = { it.name }) { category ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (category.hidden) Color(0xFF0B1118) else Color(0xFF101A26))
                            .border(
                                1.dp,
                                if (category.hidden) Color(0xFF22303E) else Color(0xFF1D2C3B),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    color = if (category.hidden) Color(0xFF8191A1) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${category.count} elementos" +
                                        if (category.hidden) " · OCULTA" else "",
                                    color = if (category.hidden) Color(0xFFFFB74D) else Color(0xFF7C8D9E),
                                    fontSize = 9.sp
                                )
                            }
                            IconButton(onClick = {
                                repository.setCategoryHidden(source.id, category.name, !category.hidden)
                            }) {
                                Icon(
                                    imageVector = if (category.hidden) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (category.hidden) "Mostrar categoría" else "Ocultar categoría",
                                    tint = if (category.hidden) Color(0xFFFFB74D) else Color(0xFF8F9DAA)
                                )
                            }
                            IconButton(onClick = {
                                renamingCategory = category.name
                                renameValue = category.name
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Renombrar categoría",
                                    tint = Color(0xFF8F9DAA)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TypeChip("TV", category.type == ItemType.LIVE, primaryColor) {
                                repository.setCategoryType(source.id, category.name, ItemType.LIVE)
                            }
                            TypeChip("Películas", category.type == ItemType.MOVIE, primaryColor) {
                                repository.setCategoryType(source.id, category.name, ItemType.MOVIE)
                            }
                            TypeChip("Series", category.type == ItemType.SERIES, primaryColor) {
                                repository.setCategoryType(source.id, category.name, ItemType.SERIES)
                            }
                            TypeChip("Adultos", category.type == ItemType.ADULT, primaryColor) {
                                repository.setCategoryType(source.id, category.name, ItemType.ADULT)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Las categorías ocultas dejan de aparecer en TV, Películas y Series, pero siguen aquí para volver a mostrarlas.",
                color = Color(0xFF667788),
                fontSize = 9.sp
            )
        }
    }

    renamingCategory?.let { oldName ->
        AlertDialog(
            onDismissRequest = { renamingCategory = null },
            title = { Text("Renombrar categoría") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("Nuevo nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.renameCategory(source.id, oldName, renameValue)
                    renamingCategory = null
                }) {
                    Text("Renombrar", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingCategory = null }) {
                    Text("Cancelar", color = Color(0xFF8F9DAA))
                }
            }
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
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) primaryColor else Color(0xFF182635))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color(0xFFB8C6D3),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
