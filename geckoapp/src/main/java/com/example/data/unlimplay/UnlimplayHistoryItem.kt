package com.example.data.unlimplay

data class UnlimplayHistoryItem(
    val id: Long,
    val type: UnlimplayType,
    val title: String,
    val posterUrl: String? = null,
    val year: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val watchKey: String
        get() = "unlimplay:${type.path}:$id"

    fun toMedia(): UnlimplayMedia = UnlimplayMedia(
        id = id,
        title = title,
        posterPath = posterUrl,
        type = type,
        year = year
    )
}
