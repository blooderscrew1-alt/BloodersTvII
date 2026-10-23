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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.repository.IptvRepository

@Composable
fun SearchScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todo") }

    val filteredItems = remember(query, allItems, selectedFilter) {
        if (query.isBlank()) emptyList()
        else {
            allItems.filter { item ->
                val matchesQuery = item.name.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true)

                val matchesType = when (selectedFilter) {
                    "Canales" -> item.type == ItemType.LIVE
                    "Películas" -> item.type == ItemType.MOVIE
                    "Series" -> item.type == ItemType.SERIES
                    else -> true
                }
                matchesQuery && matchesType
            }.take(150)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Search Input Field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar canales, películas o series en tus listas IPTV...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar búsqueda",
                            tint = Color(0xFF8F9DAA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFF22303E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0F1722),
                unfocusedContainerColor = Color(0xFF0F1722),
                cursorColor = primaryColor
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("iptv_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Type Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Todo", "Canales", "Películas", "Series").forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF141F2B),
                        labelColor = Color(0xFF9FB0C0)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) primaryColor else Color(0xFF233242),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (query.isBlank()) {
            // Empty query state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131D28)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Búsqueda en Listas IPTV",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Escribe el nombre de un canal, película o serie para buscar en tus listas IPTV y Xtream sincronizadas.",
                        color = Color(0xFF8F9DAA),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredItems.isEmpty()) {
            // No matches found state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Sin resultados",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No se encontraron coincidencias para \"$query\" en tus listas IPTV.",
                        color = Color(0xFF8F9DAA),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Results List
            Text(
                text = "${filteredItems.size} resultados encontrados en tus listas IPTV",
                color = Color(0xFF8F9DAA),
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    IptvSearchResultCard(
                        item = item,
                        primaryColor = primaryColor,
                        onClick = { onPlayItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IptvSearchResultCard(
    item: PlaylistItem,
    primaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101923))
            .border(1.dp, Color(0xFF1B2836), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail or Icon
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF172432)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            } else {
                Icon(
                    imageVector = when (item.type) {
                        ItemType.LIVE -> Icons.Default.Tv
                        ItemType.MOVIE -> Icons.Default.Movie
                        ItemType.SERIES -> Icons.Default.Tv
                    },
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (item.type) {
                                ItemType.LIVE -> primaryColor.copy(alpha = 0.2f)
                                ItemType.MOVIE -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ItemType.SERIES -> Color(0xFFFF9100).copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = when (item.type) {
                            ItemType.LIVE -> "EN VIVO"
                            ItemType.MOVIE -> "PELÍCULA"
                            ItemType.SERIES -> "SERIE"
                        },
                        color = when (item.type) {
                            ItemType.LIVE -> primaryColor
                            ItemType.MOVIE -> Color(0xFF00E5FF)
                            ItemType.SERIES -> Color(0xFFFF9100)
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = item.category,
                    color = Color(0xFF8F9DAA),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Play Button Action
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproducir",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
