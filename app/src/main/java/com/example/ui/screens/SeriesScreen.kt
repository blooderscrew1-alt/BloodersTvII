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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.repository.IptvRepository
import com.example.ui.components.HeroBanner
import com.example.ui.components.NumberedPosterCard
import com.example.ui.components.StandardPosterCard

@Composable
fun SeriesScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    onNavigateToPlaylists: (() -> Unit)? = null,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val history by repository.history.collectAsState()

    val series = remember(allItems) {
        allItems.filter { it.type == ItemType.SERIES }
    }

    val seriesHistory = remember(history) {
        history.filter { it.type == ItemType.SERIES }
    }

    if (series.isEmpty()) {
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
                    text = "No hay series cargadas",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tus series de televisión de listas IPTV o cuentas Xtream aparecerán aquí automáticamente.",
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

    val featuredSeries = remember(series) { series.firstOrNull() }

    val categories = remember(series) {
        series.map { it.category }.distinct()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (featuredSeries != null) {
            item {
                HeroBanner(
                    item = featuredSeries,
                    onPlayClick = { onPlayItem(featuredSeries) },
                    primaryColor = primaryColor
                )
            }
        }

        if (seriesHistory.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(primaryColor))
                        Text(
                            text = " Continuar viendo (Series)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(seriesHistory) { entry ->
                            StandardPosterCard(
                                title = entry.title,
                                posterUrl = entry.posterUrl,
                                subtitle = "Reanudar",
                                progressPercent = entry.progressPercent,
                                onClick = {
                                    val item = PlaylistItem(
                                        id = entry.itemId,
                                        name = entry.title,
                                        streamUrl = entry.streamUrl,
                                        logoUrl = entry.posterUrl,
                                        type = entry.type
                                    )
                                    onPlayItem(item)
                                },
                                primaryColor = primaryColor
                            )
                        }
                    }
                }
            }
        }

        // Section: "■ Destacadas en tus Listas"
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).background(primaryColor))
                    Text(
                        text = " Destacadas en tus Listas",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    itemsIndexed(series.take(10)) { index, item ->
                        NumberedPosterCard(
                            item = item,
                            rank = index + 1,
                            onClick = { onPlayItem(item) },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }

        // Categories sections
        categories.take(8).forEach { catName ->
            val catSeries = series.filter { it.category == catName }
            if (catSeries.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(primaryColor))
                            Text(
                                text = " $catName (${catSeries.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            items(catSeries.take(15)) { s ->
                                StandardPosterCard(
                                    title = s.name,
                                    posterUrl = s.logoUrl,
                                    subtitle = s.category,
                                    onClick = { onPlayItem(s) },
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
