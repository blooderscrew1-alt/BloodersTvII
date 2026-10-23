package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PlaylistItem

@Composable
fun HeroBanner(
    item: PlaylistItem,
    onPlayClick: (PlaylistItem) -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.015f else 1.0f,
        animationSpec = tween(150),
        label = "hero_scale"
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Proportional hero: short enough on phones and TV without eating the
        // whole first screen, but never cramped.
        val bannerHeight = (maxWidth * 0.42f).coerceIn(170.dp, 320.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = if (isFocused) 2.5.dp else 1.5.dp,
                    color = if (isFocused) primaryColor else primaryColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(Color(0xFF0F171E))
                .focusable(interactionSource = interactionSource)
                .clickable(interactionSource = interactionSource, indication = null) {
                    onPlayClick(item)
                }
                .testTag("hero_banner_card")
        ) {
        // Backdrop Image (no remote stock fallback: saves data on low-end devices)
        if (!item.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.logoUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient overlay for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xEE090E13),
                            Color(0x99090E13),
                            Color(0x33000000)
                        )
                    )
                )
        )

        // Top tag
        Text(
            text = item.name,
            color = Color(0xFF8FB2C6),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 16.dp)
        )

        // Center / Bottom content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 22.dp)
        ) {
            Text(
                text = item.name.uppercase(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            item.overview?.let { desc ->
                Text(
                    text = desc,
                    color = Color(0xFFC4D1DD),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            } ?: Spacer(modifier = Modifier.height(10.dp))

            // "VER AHORA" Button
            Button(
                onClick = { onPlayClick(item) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .testTag("hero_banner_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VER AHORA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Bottom right pagination dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.White))
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0x66FFFFFF)))
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0x66FFFFFF)))
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0x66FFFFFF)))
        }
    }
}
}
