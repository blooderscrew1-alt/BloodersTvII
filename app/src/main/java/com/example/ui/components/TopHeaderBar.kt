package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val label: String) {
    TELEVISION("Televisión"),
    PELICULAS("Películas"),
    SERIES("Series"),
    NSRPLAY("NSRPlay"),
    PLAYLISTS("Playlists"),
    AJUSTES("Ajustes"),
    PREMIUM("Premium")
}

@Composable
fun TopHeaderBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onSearchClick: () -> Unit,
    primaryColor: Color = Color(0xFFCCFF00),
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf("") }
    val tabsScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))
        while (true) {
            val now = Date()
            val time = timeFormat.format(now).lowercase(Locale.getDefault())
            val date = dateFormat.format(now)
            currentTimeString = "$time • $date"
            // Minute precision is enough: updating every second recomposed the
            // whole header (and all tabs) 60x more often on low-end devices.
            delay(30_000)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090E13))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        val compact = maxWidth < 520.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 54.dp else 58.dp)
                .padding(horizontal = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Left: Logo & Search
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Badge Logo
            Box(
                modifier = Modifier
                    .size(if (compact) 30.dp else 34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14202B))
                    .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "Logo BloodersTv II",
                    tint = primaryColor,
                    modifier = Modifier.size(if (compact) 17.dp else 19.dp)
                )
            }

            // Search Icon Button
            val searchInteraction = remember { MutableInteractionSource() }
            val isSearchFocused by searchInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .size(if (compact) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSearchFocused) primaryColor.copy(alpha = 0.2f) else Color(0xFF131D27))
                    .border(
                        if (isSearchFocused) 2.dp else 1.dp,
                        if (isSearchFocused) primaryColor else Color(0xFF223141),
                        RoundedCornerShape(8.dp)
                    )
                    .focusable(interactionSource = searchInteraction)
                    .clickable(
                        interactionSource = searchInteraction,
                        indication = null,
                        onClick = onSearchClick
                    )
                    .testTag("search_icon_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar canales y películas",
                    tint = if (isSearchFocused) primaryColor else Color(0xFFC0CAD5),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Center: Horizontally scrollable Navigation Tabs (Fixes menu clipping on all orientations!)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(tabsScrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                val tabInteraction = remember { MutableInteractionSource() }
                val isTabFocused by tabInteraction.collectIsFocusedAsState()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isTabFocused) Color(0xFF1A2633) else Color.Transparent)
                        .focusable(interactionSource = tabInteraction)
                        .clickable(
                            interactionSource = tabInteraction,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 6.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        color = when {
                            isSelected -> Color.White
                            isTabFocused -> primaryColor
                            else -> Color(0xFF8F9CA8)
                        },
                        fontSize = if (compact) 13.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Green underline indicator for selected tab (as in screenshot)
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 32.dp else 0.dp)
                            .height(2.5.dp)
                            .background(if (isSelected) primaryColor else Color.Transparent, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right: Live Clock & Date badge. Hidden on narrow phones so the nav tabs
        // always keep a usable width instead of being squeezed off-screen.
        if (!compact) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF101720))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = currentTimeString,
                    color = Color(0xFFA0B0C0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("live_clock_text")
                )
            }
        }
        }
    }
}
