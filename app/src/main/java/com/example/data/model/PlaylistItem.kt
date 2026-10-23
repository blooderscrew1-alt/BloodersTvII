package com.example.data.model

import java.util.UUID

enum class ItemType {
    LIVE,
    MOVIE,
    SERIES
}

data class PlaylistItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "General",
    val channelNumber: Int? = null,
    val type: ItemType = ItemType.LIVE,
    val groupTitle: String? = null,
    val tvgId: String? = null,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val overview: String? = null,
    val durationMs: Long = 0L,
    val sourceId: String = "default",
    val isHidden: Boolean = false
)

enum class SourceType {
    M3U_URL,
    M3U_TEXT,
    XTREAM,
    LOCAL,
    DEMO
}

data class PlaylistSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SourceType,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val totalChannels: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

data class CategoryInfo(
    val name: String,
    val itemCount: Int,
    val isSelected: Boolean = true,
    val isLocked: Boolean = false
)

data class WatchHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val category: String = "",
    val type: ItemType = ItemType.MOVIE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class ThemeAccent(val label: String, val primaryHex: Long, val surfaceHex: Long) {
    NEON_LIME("Neon Lime (Original)", 0xFFCCFF00, 0xFF0D151D),
    CYBER_CYAN("Cyber Cyan", 0xFF00E5FF, 0xFF0A131A),
    RUBY_RED("Ruby Red", 0xFFFF2A6D, 0xFF180A10),
    AMBER_GOLD("Amber Gold", 0xFFFFB300, 0xFF141108),
    MIDNIGHT_AMOLED("Midnight AMOLED", 0xFFCCFF00, 0xFF000000)
}

enum class BufferMode(val label: String, val description: String, val minBufferMs: Int, val maxBufferMs: Int, val playbackBufferMs: Int) {
    SMOOTH_LOW_SPEED(
        "Baja Velocidad (Ultra Fluido)",
        "Mayor almacenamiento en caché previo para conexiones lentas o inestables",
        20_000,
        60_000,
        1_200
    ),
    STANDARD(
        "Equilibrado (Recomendado)",
        "Arranque rápido y estabilidad de buffer estándar",
        15_000,
        35_000,
        800
    ),
    FAST_ZAPPING(
        "Zapping Rápido",
        "Prioriza cambio instantáneo de canales con menor buffering",
        8_000,
        20_000,
        400
    )
}
