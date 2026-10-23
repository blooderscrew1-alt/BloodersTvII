package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BloodersDarkScheme = darkColorScheme(
    primary = Color(0xFFCCFF00),
    secondary = Color(0xFF00E5FF),
    tertiary = Color(0xFFFF2A42),
    background = Color(0xFF090E13),
    surface = Color(0xFF0F1722),
    surfaceVariant = Color(0xFF162330),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BloodersDarkScheme,
        typography = Typography,
        content = content
    )
}
