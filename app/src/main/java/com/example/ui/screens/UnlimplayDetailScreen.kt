package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.unlimplay.UnlimplayMedia
import com.example.data.unlimplay.UnlimplayRepository
import com.example.data.unlimplay.UnlimplayType
import com.example.webplayer.PlayerWebActivity

@Composable
fun UnlimplayDetailScreen(
    media: UnlimplayMedia,
    repository: UnlimplayRepository,
    primaryColor: Color,
    onPlayed: (UnlimplayMedia) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    fun play() {
        onPlayed(media)
        val intent = Intent(context, PlayerWebActivity::class.java).apply {
            putExtra(PlayerWebActivity.EXTRA_URL, repository.embedUrl(media))
            putExtra(PlayerWebActivity.EXTRA_TITLE, media.title)
            putExtra(PlayerWebActivity.EXTRA_REFERER, "https://unlimplay.com/")
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (media.type) {
                        UnlimplayType.MOVIE -> "Unlimplay · Película"
                        UnlimplayType.SERIES -> "Unlimplay · Serie"
                        UnlimplayType.ANIME -> "Unlimplay · Anime"
                    },
                    color = Color(0xFF9C6BFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .height(165.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF172431))
                    ) {
                        if (!media.posterUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = media.posterUrl,
                                contentDescription = media.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = listOfNotNull(
                                media.year,
                                when (media.type) {
                                    UnlimplayType.MOVIE -> "Película"
                                    UnlimplayType.SERIES -> "Serie"
                                    UnlimplayType.ANIME -> "Anime"
                                },
                                media.rating?.let { "★ %.1f".format(it) }
                            ).joinToString(" · "),
                            color = Color(0xFF8F9DAA),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = media.overview?.takeIf { it.isNotBlank() }
                                ?: "Contenido disponible en Unlimplay.",
                            color = Color(0xFFB8C6D3),
                            fontSize = 11.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { play() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C6BFF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver en Unlimplay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Text(
                    text = "Unlimplay muestra sus servidores directamente dentro del iframe. " +
                        "El reproductor se abre en esta app con el bloqueo de anuncios y las extensiones activas.",
                    color = Color(0xFF66757F),
                    fontSize = 9.sp
                )
            }
        }
    }
}
