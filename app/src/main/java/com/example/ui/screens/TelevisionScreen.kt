package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.player.SmartPlayerManager
import com.example.data.repository.IptvRepository
import com.example.ui.components.ParentalPinDialog
import com.example.ui.components.VideoPlayerView

@Composable
fun TelevisionScreen(
    repository: IptvRepository,
    playerManager: SmartPlayerManager,
    onEnterFullscreen: (PlaylistItem) -> Unit,
    onNavigateToPlaylists: (() -> Unit)? = null,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val lockedCategories by repository.lockedCategories.collectAsState()
    val parentalPin by repository.parentalPin.collectAsState()

    // Available categories in live TV
    val liveItems = remember(allItems) {
        allItems.filter { it.type == ItemType.LIVE }
    }

    if (liveItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090E13))
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
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No hay canales de televisión",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Aún no tienes canales sincronizados. Agrega una lista M3U o Xtream Codes en la sección 'Listas' para comenzar a ver televisión en vivo.",
                    color = Color(0xFF8F9DAA),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                if (onNavigateToPlaylists != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToPlaylists,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ir a Listas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        return
    }

    val categories = remember(liveItems) {
        val cats = liveItems.map { it.category }.distinct().toMutableList()
        if (cats.isNotEmpty()) {
            if (!cats.contains("Populares")) cats.add(0, "Populares")
            if (!cats.contains("Favoritos")) cats.add(1, "Favoritos")
        }
        cats
    }

    var selectedCategory by remember(categories) {
        mutableStateOf(categories.firstOrNull() ?: "Populares")
    }
    var selectedChannel by remember { mutableStateOf<PlaylistItem?>(null) }
    var showPinDialogForCategory by remember { mutableStateOf<String?>(null) }
    var unlockedCategories by remember { mutableStateOf(setOf<String>()) }

    // Filter channels for selected category
    val currentChannels = remember(liveItems, selectedCategory, favorites) {
        when (selectedCategory) {
            "Populares" -> liveItems.take(15)
            "Favoritos" -> liveItems.filter { favorites.contains(it.id) }
            else -> liveItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    // Auto select first channel if none selected
    LaunchedEffect(currentChannels) {
        if (selectedChannel == null || !currentChannels.any { it.id == selectedChannel?.id }) {
            val first = currentChannels.firstOrNull()
            if (first != null) {
                selectedChannel = first
                playerManager.playStream(first.streamUrl)
            }
        }
    }

    // Category click handler with parental control check
    fun selectCategory(category: String) {
        val isLocked = lockedCategories.contains(category) && !unlockedCategories.contains(category)
        if (isLocked) {
            showPinDialogForCategory = category
        } else {
            selectedCategory = category
        }
    }

    // Parental PIN Dialog
    showPinDialogForCategory?.let { cat ->
        ParentalPinDialog(
            correctPin = parentalPin,
            title = "Categoría Protegida ($cat)",
            onSuccess = {
                unlockedCategories = unlockedCategories + cat
                selectedCategory = cat
                showPinDialogForCategory = null
            },
            onDismiss = {
                showPinDialogForCategory = null
            },
            primaryColor = primaryColor
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .padding(8.dp)
    ) {
        val isPortrait = maxWidth < 650.dp

        if (isPortrait) {
            // PORTRAIT (Vertical) MODE: Stacked Layout (Player on top, Categories middle, Channels bottom)
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Live Player (16:9)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .clickable { selectedChannel?.let { onEnterFullscreen(it) } }
                ) {
                    VideoPlayerView(playerManager = playerManager, modifier = Modifier.fillMaxSize())

                    // Live indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xDDFB163B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "• EN VIVO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    // Fullscreen hint button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x99000000))
                            .clickable { selectedChannel?.let { onEnterFullscreen(it) } }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expandir", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }

                // Channel Info Bar underneath Player
                selectedChannel?.let { ch ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = ch.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = ch.overview ?: ch.category, color = Color(0xFF8F9DAA), fontSize = 10.sp, maxLines = 1)
                        }

                        val isFav = favorites.contains(ch.id)
                        IconButton(onClick = { repository.toggleFavorite(ch.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (isFav) Color(0xFFFF334B) else Color(0xFF90A1B2)
                            )
                        }
                    }
                }

                // Categories Pills (Horizontally scrollable)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    items(categories) { category ->
                        CategoryPill(
                            category = category,
                            isSelected = selectedCategory.equals(category, ignoreCase = true),
                            isLocked = lockedCategories.contains(category) && !unlockedCategories.contains(category),
                            primaryColor = primaryColor,
                            onClick = { selectCategory(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Full-width Channels List (Vertically scrollable)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0C131A), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(currentChannels) { index, channel ->
                        ChannelItemRow(
                            channel = channel,
                            index = index,
                            isSelected = selectedChannel?.id == channel.id,
                            isFavorite = favorites.contains(channel.id),
                            primaryColor = primaryColor,
                            onClick = {
                                selectedChannel = channel
                                playerManager.playStream(channel.streamUrl)
                            }
                        )
                    }
                }
            }
        } else {
            // LANDSCAPE / TV BOX MODE: Side-by-Side Split Layout (Screenshot 3)
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Category Pills
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Categorías",
                        color = primaryColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(categories) { category ->
                            CategoryPill(
                                category = category,
                                isSelected = selectedCategory.equals(category, ignoreCase = true),
                                isLocked = lockedCategories.contains(category) && !unlockedCategories.contains(category),
                                primaryColor = primaryColor,
                                onClick = { selectCategory(category) }
                            )
                        }
                    }
                }

                // Split: Left Channels List, Right Video Preview & Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Column Channels
                    Box(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .background(Color(0xFF0C131A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF192531), RoundedCornerShape(10.dp))
                    ) {
                        if (currentChannels.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay canales en esta categoría", color = Color(0xFF7F8F9F), fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(currentChannels) { index, channel ->
                                    ChannelItemRow(
                                        channel = channel,
                                        index = index,
                                        isSelected = selectedChannel?.id == channel.id,
                                        isFavorite = favorites.contains(channel.id),
                                        primaryColor = primaryColor,
                                        onClick = {
                                            selectedChannel = channel
                                            playerManager.playStream(channel.streamUrl)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Right Column Video Preview & Info Bar
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.5.dp, Color(0xFF1F2E3D), RoundedCornerShape(12.dp))
                                .clickable { selectedChannel?.let { onEnterFullscreen(it) } }
                        ) {
                            VideoPlayerView(playerManager = playerManager, modifier = Modifier.fillMaxSize())

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x99000000))
                                    .clickable { selectedChannel?.let { onEnterFullscreen(it) } }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pantalla Completa", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xDDFB163B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "• EN VIVO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        selectedChannel?.let { ch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F1722))
                                    .border(1.dp, Color(0xFF1A2735), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF182635)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Tv, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(text = ch.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = ch.overview ?: "Transmisión en directo", color = Color(0xFF7F8F9F), fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                val isFav = favorites.contains(ch.id)
                                IconButton(onClick = { repository.toggleFavorite(ch.id) }) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorito",
                                        tint = if (isFav) Color(0xFFFF334B) else Color(0xFF90A1B2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    category: String,
    isSelected: Boolean,
    isLocked: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isSelected -> primaryColor
                    isFocused -> Color(0xFF1E2D3B)
                    else -> Color(0xFF131D26)
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) primaryColor else Color(0xFF1E2C38),
                shape = RoundedCornerShape(6.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isLocked) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = if (isSelected) Color.Black else Color(0xFFFF5252), modifier = Modifier.size(12.dp))
            }
            Text(
                text = category,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ChannelItemRow(
    channel: PlaylistItem,
    index: Int,
    isSelected: Boolean,
    isFavorite: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1.0f,
        animationSpec = tween(120),
        label = "ch_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> primaryColor
                    isFocused -> Color(0xFF192735)
                    else -> Color(0xFF101922)
                }
            )
            .border(
                width = if (isFocused && !isSelected) 2.dp else 0.dp,
                color = if (isFocused && !isSelected) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) Color(0xFF111A04) else Color(0xFF1B2835))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = (channel.channelNumber ?: (index + 1)).toString(),
                color = if (isSelected) primaryColor else Color(0xFF9FB2C4),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) Color(0x33000000) else Color(0xFF1B2734)),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(model = channel.logoUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Tv, contentDescription = null, tint = if (isSelected) Color.Black else primaryColor, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            channel.overview?.let { desc ->
                Text(
                    text = desc,
                    color = if (isSelected) Color(0xFF333333) else Color(0xFF7E8E9F),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isFavorite) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = if (isSelected) Color.Red else Color(0xFFFF4081), modifier = Modifier.size(14.dp))
        }
    }
}
