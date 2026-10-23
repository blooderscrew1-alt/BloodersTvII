package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumScreen(
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090E13))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(primaryColor, Color(0xFF00E5FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        item {
            Text(
                text = "BloodersTv II VIP",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Tu reproductor IPTV cuenta con el motor de aceleración y decodificación activo.",
                color = Color(0xFFA0B0C0),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101924))
                    .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumBenefitRow("Decodificación de Hardware Ultra HD (HEVC/H.265, AV1, 4K)")
                    PremiumBenefitRow("Sistema de Caché Inteligente de 150 MB para conexiones lentas")
                    PremiumBenefitRow("Filtro previo anti-basura para listas M3U y Xtream Codes")
                    PremiumBenefitRow("Control parental con PIN protegido")
                    PremiumBenefitRow("Historial de reproducción con reanudación automática")
                    PremiumBenefitRow("Soporte nativo para control remoto D-Pad de TV Box y pantallas táctiles")
                    PremiumBenefitRow("Gestión de listas M3U, M3U8 y Xtream Codes ilimitadas")
                }
            }
        }

        item {
            Button(
                onClick = { /* All features active */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("TODAS LAS FUNCIONES DESBLOQUEADAS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PremiumBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFCCFF00),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}
