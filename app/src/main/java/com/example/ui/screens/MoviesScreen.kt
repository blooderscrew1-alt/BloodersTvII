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
import androidx.compose.material.icons.filled.Movie
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
fun MoviesScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    onNavigateToPlaylists: (() -> Unit)? = null,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val history by repository.history.collectAsState()

    val movies = remember(allItems) {
        allItems.filter { it.type == ItemType.MOVIE }
    }

    val movieHistory = remember(history) {
        history.filter { it.type == ItemType.MOVIE }
    }

    if (movies.isEmpty()) {
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
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No hay películas cargadas",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tus películas de listas IPTV o cuentas Xtream aparecerán aquí automáticamente.",
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

    val heroMovie = remember(movies) { movies.firstOrNull() }

    // Group remaining movies by category
    val categories = remember(movies) {
        movies.map { it.category }.distinct()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Banner
        if (heroMovie != null) {
            item {
                HeroBanner(
                    item = heroMovie,
                    onPlayClick = { onPlayItem(heroMovie) },
                    primaryColor = primaryColor
                )
            }
        }

        // Section: "■ Continuar viendo" (Watch History)
        if (movieHistory.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(primaryColor)
                        )
                        Text(
                            text = " Continuar viendo (Historial)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(movieHistory) { entry ->
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

        // Section: "■ Destacadas hoy"
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(primaryColor)
                    )
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
                    itemsIndexed(movies.take(10)) { index, movie ->
                        NumberedPosterCard(
                            item = movie,
                            rank = index + 1,
                            onClick = { onPlayItem(movie) },
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }

        // Sections per category in IPTV list
        categories.take(8).forEach { catName ->
            val catMovies = movies.filter { it.category == catName }
            if (catMovies.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(primaryColor)
                            )
                            Text(
                                text = " $catName (${catMovies.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            items(catMovies.take(15)) { movie ->
                                StandardPosterCard(
                                    title = movie.name,
                                    posterUrl = movie.logoUrl,
                                    subtitle = movie.category,
                                    onClick = { onPlayItem(movie) },
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
