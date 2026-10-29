package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.repository.IptvRepository
import com.example.ui.components.ParentalPinDialog
import com.example.ui.components.StandardPosterCard

@Composable
fun AdultScreen(
    repository: IptvRepository,
    onPlayItem: (PlaylistItem) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val allItems by repository.items.collectAsState()
    val parentalPin by repository.parentalPin.collectAsState()
    val adultUnlocked by repository.adultUnlocked.collectAsState()
    val adultItems = remember(allItems) {
        allItems
            .filter { it.type == ItemType.ADULT && !it.isHidden }
            .sortedWith(compareBy({ it.type.ordinal }, { it.name.lowercase() }))
    }

    var showPinDialog by remember { mutableStateOf(false) }

    if (adultItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090E13))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF8F9DAA),
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No hay contenido para adultos",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Puedes enviar categorías o listas completas aquí desde Playlists → Editar lista → Adultos.",
                    color = Color(0xFF8F9DAA),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (!adultUnlocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090E13))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1013)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFF6B7A),
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Contenido para adultos",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Esta sección está protegida. Ingresa tu PIN para continuar.",
                    color = Color(0xFF8F9DAA),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { showPinDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Ingresar PIN", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPinDialog) {
            ParentalPinDialog(
                correctPin = parentalPin,
                title = "Contenido para adultos",
                subtitle = "Ingresa el PIN de 4 dígitos para desbloquear esta sección.",
                onSuccess = {
                    repository.unlockAdultContent()
                    showPinDialog = false
                },
                onDismiss = { showPinDialog = false },
                primaryColor = primaryColor
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Adultos",
                color = Color(0xFFFF6B7A),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "${adultItems.size} elementos · Sección protegida con PIN",
                color = Color(0xFF8F9DAA),
                fontSize = 10.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(adultItems, key = { it.id }) { item ->
                StandardPosterCard(
                    title = item.name,
                    posterUrl = item.logoUrl,
                    subtitle = item.category,
                    badge = "ADULTO",
                    badgeColor = Color(0xFFFF4D6D),
                    onClick = { onPlayItem(item) },
                    primaryColor = primaryColor
                )
            }
        }
    }
}
