package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.example.data.model.BufferMode
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistSource
import com.example.data.model.SourceType
import com.example.data.model.ThemeAccent
import com.example.data.model.WatchHistoryItem
import com.example.data.nsrplay.NsrHistoryItem
import com.example.data.nsrplay.NsrMedia
import com.example.data.nsrplay.NsrMediaType
import com.example.data.nsrplay.watchKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class IptvRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("blooders_tv_prefs", Context.MODE_PRIVATE)
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _items = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val items: StateFlow<List<PlaylistItem>> = _items.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistSource>>(emptyList())
    val playlists: StateFlow<List<PlaylistSource>> = _playlists.asStateFlow()

    private val _history = MutableStateFlow<List<WatchHistoryItem>>(emptyList())
    val history: StateFlow<List<WatchHistoryItem>> = _history.asStateFlow()

    private val _watchedNsr = MutableStateFlow<Set<String>>(emptySet())
    val watchedNsr: StateFlow<Set<String>> = _watchedNsr.asStateFlow()

    private val _nsrHistory = MutableStateFlow<List<NsrHistoryItem>>(emptyList())
    val nsrHistory: StateFlow<List<NsrHistoryItem>> = _nsrHistory.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _lockedCategories = MutableStateFlow<Set<String>>(emptySet())
    val lockedCategories: StateFlow<Set<String>> = _lockedCategories.asStateFlow()

    private val _parentalPin = MutableStateFlow("0000")
    val parentalPin: StateFlow<String> = _parentalPin.asStateFlow()

    private val _themeAccent = MutableStateFlow(ThemeAccent.NEON_LIME)
    val themeAccent: StateFlow<ThemeAccent> = _themeAccent.asStateFlow()

    private val _bufferMode = MutableStateFlow(BufferMode.SMOOTH_LOW_SPEED)
    val bufferMode: StateFlow<BufferMode> = _bufferMode.asStateFlow()

    init {
        loadSettings()
        loadHistory()
        loadFavorites()
        loadWatchedNsr()
        loadNsrHistory()
        loadItems()
    }

    private fun loadNsrHistory() {
        val raw = prefs.getString("nsr_history_json", null)
        if (raw.isNullOrBlank()) return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<NsrHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val slug = obj.optString("slug")
                if (slug.isBlank()) continue
                val type = try {
                    NsrMediaType.valueOf(obj.optString("type", NsrMediaType.MOVIE.apiValue).uppercase())
                } catch (_: Exception) {
                    NsrMediaType.MOVIE
                }
                list.add(
                    NsrHistoryItem(
                        slug = slug,
                        type = type,
                        title = obj.optString("title").ifBlank { slug },
                        posterUrl = obj.optString("posterUrl").takeIf { it.isNotBlank() },
                        year = obj.optString("year").takeIf { it.isNotBlank() },
                        lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", 0L)
                    )
                )
            }
            _nsrHistory.value = list.sortedByDescending { it.lastWatchedTimestamp }.take(30)
        } catch (_: Throwable) {
        }
    }

    private fun saveNsrHistory(list: List<NsrHistoryItem>) {
        repoScope.launch {
            try {
                val array = JSONArray()
                list.forEach { entry ->
                    val obj = JSONObject().apply {
                        put("slug", entry.slug)
                        put("type", entry.type.apiValue)
                        put("title", entry.title)
                        put("posterUrl", entry.posterUrl ?: "")
                        put("year", entry.year ?: "")
                        put("lastWatchedTimestamp", entry.lastWatchedTimestamp)
                    }
                    array.put(obj)
                }
                prefs.edit().putString("nsr_history_json", array.toString()).apply()
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadWatchedNsr() {
        _watchedNsr.value = prefs.getStringSet("watched_nsr_keys", emptySet()) ?: emptySet()
    }

    private fun loadSettings() {
        val pin = prefs.getString("parental_pin", "0000") ?: "0000"
        _parentalPin.value = pin

        val locked = prefs.getStringSet("locked_categories", setOf("Adultos", "18+", "XXX")) ?: emptySet()
        _lockedCategories.value = locked

        val themeName = prefs.getString("theme_accent", ThemeAccent.NEON_LIME.name)
        _themeAccent.value = try {
            ThemeAccent.valueOf(themeName ?: ThemeAccent.NEON_LIME.name)
        } catch (_: Exception) {
            ThemeAccent.NEON_LIME
        }

        val bufferName = prefs.getString("buffer_mode", BufferMode.SMOOTH_LOW_SPEED.name)
        _bufferMode.value = try {
            BufferMode.valueOf(bufferName ?: BufferMode.SMOOTH_LOW_SPEED.name)
        } catch (_: Exception) {
            BufferMode.SMOOTH_LOW_SPEED
        }
    }

    private fun loadFavorites() {
        val favs = prefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
        _favorites.value = favs
    }

    private fun loadHistory() {
        val json = prefs.getString("watch_history_json", null)
        if (!json.isNullOrBlank()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<WatchHistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        WatchHistoryItem(
                            id = obj.getString("id"),
                            itemId = obj.getString("itemId"),
                            title = obj.getString("title"),
                            streamUrl = obj.getString("streamUrl"),
                            posterUrl = obj.optString("posterUrl").takeIf { it.isNotBlank() },
                            category = obj.optString("category", ""),
                            type = try { ItemType.valueOf(obj.getString("type")) } catch (_: Exception) { ItemType.MOVIE },
                            positionMs = obj.optLong("positionMs", 0L),
                            durationMs = obj.optLong("durationMs", 0L),
                            lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", 0L)
                        )
                    )
                }
                _history.value = list.sortedByDescending { it.lastWatchedTimestamp }
                return
            } catch (_: Throwable) {}
        }

        _history.value = emptyList()
    }

    private fun getPlaylistsFile(): File = File(context.filesDir, "user_playlists.json")
    private fun getChannelsFile(): File = File(context.filesDir, "user_channels.json")

    private fun loadItems() {
        val userPlaylists = mutableListOf<PlaylistSource>()

        // 1. Restore saved playlists from disk
        val plFile = getPlaylistsFile()
        if (plFile.exists() && plFile.length() > 0L) {
            try {
                val json = plFile.readText()
                if (json.isNotBlank()) {
                    val array = JSONArray(json)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val catArray = obj.optJSONArray("selectedCategories") ?: JSONArray()
                        val cats = mutableSetOf<String>()
                        for (c in 0 until catArray.length()) cats.add(catArray.getString(c))

                        val typeStr = obj.getString("type")
                        if (typeStr == SourceType.DEMO.name) continue

                        val source = PlaylistSource(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            type = SourceType.valueOf(typeStr),
                            url = obj.optString("url", ""),
                            username = obj.optString("username", ""),
                            password = obj.optString("password", ""),
                            selectedCategories = cats,
                            totalChannels = obj.optInt("totalChannels", 0),
                            addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                        )
                        userPlaylists.add(source)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        _playlists.value = userPlaylists

        // 2. Stream restore saved user channels from disk
        val userChannels = loadChannelsFromDisk()
        _items.value = userChannels
    }

    private fun loadChannelsFromDisk(): List<PlaylistItem> {
        val chFile = getChannelsFile()
        if (!chFile.exists() || chFile.length() == 0L) return emptyList()

        val list = mutableListOf<PlaylistItem>()
        try {
            chFile.bufferedReader(Charsets.UTF_8).use { fileReader ->
                val reader = JsonReader(fileReader)
                reader.isLenient = true

                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        reader.beginObject()
                        var id = ""
                        var name = ""
                        var streamUrl = ""
                        var logoUrl: String? = null
                        var category = "General"
                        var channelNumber: Int? = null
                        var type = ItemType.LIVE
                        var groupTitle: String? = null
                        var tvgId: String? = null
                        var overview: String? = null
                        var durationMs = 0L
                        var sourceId = "user"

                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            when (key) {
                                "id" -> id = reader.nextString()
                                "name" -> name = reader.nextString()
                                "streamUrl" -> streamUrl = reader.nextString()
                                "logoUrl" -> {
                                    if (reader.peek() != JsonToken.NULL) {
                                        logoUrl = reader.nextString().takeIf { it.isNotBlank() }
                                    } else {
                                        reader.nextNull()
                                    }
                                }
                                "category" -> category = reader.nextString()
                                "channelNumber" -> {
                                    if (reader.peek() == JsonToken.NUMBER) {
                                        channelNumber = reader.nextInt()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                "type" -> {
                                    val typeStr = reader.nextString()
                                    type = try { ItemType.valueOf(typeStr) } catch (_: Exception) { ItemType.LIVE }
                                }
                                "groupTitle" -> {
                                    if (reader.peek() != JsonToken.NULL) {
                                        groupTitle = reader.nextString().takeIf { it.isNotBlank() }
                                    } else {
                                        reader.nextNull()
                                    }
                                }
                                "tvgId" -> {
                                    if (reader.peek() != JsonToken.NULL) {
                                        tvgId = reader.nextString().takeIf { it.isNotBlank() }
                                    } else {
                                        reader.nextNull()
                                    }
                                }
                                "overview" -> {
                                    if (reader.peek() != JsonToken.NULL) {
                                        overview = reader.nextString().takeIf { it.isNotBlank() }
                                    } else {
                                        reader.nextNull()
                                    }
                                }
                                "durationMs" -> durationMs = reader.nextLong()
                                "sourceId" -> sourceId = reader.nextString()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        if (sourceId != "default_demo" && streamUrl.isNotBlank()) {
                            list.add(
                                PlaylistItem(
                                    id = if (id.isNotBlank()) id else UUID.randomUUID().toString(),
                                    name = if (name.isNotBlank()) name else "Canal",
                                    streamUrl = streamUrl,
                                    logoUrl = logoUrl,
                                    category = if (category.isNotBlank()) category else "General",
                                    channelNumber = channelNumber,
                                    type = type,
                                    groupTitle = groupTitle,
                                    tvgId = tvgId,
                                    isFavorite = _favorites.value.contains(id),
                                    isLocked = _lockedCategories.value.contains(category),
                                    overview = overview,
                                    durationMs = durationMs,
                                    sourceId = sourceId
                                )
                            )
                        }
                    }
                    reader.endArray()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return list
    }

    private fun persistPlaylistsToDisk() {
        try {
            val userPlaylists = _playlists.value.filter { it.type != SourceType.DEMO }
            val array = JSONArray()
            userPlaylists.forEach { pl ->
                val obj = JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("type", pl.type.name)
                    put("url", pl.url)
                    put("username", pl.username)
                    put("password", pl.password)
                    put("totalChannels", pl.totalChannels)
                    put("addedAt", pl.addedAt)
                    val catArr = JSONArray()
                    pl.selectedCategories.forEach { catArr.put(it) }
                    put("selectedCategories", catArr)
                }
                array.put(obj)
            }
            getPlaylistsFile().writeText(array.toString())
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun persistChannelsToDisk() {
        val targetFile = getChannelsFile()
        val tempFile = File(context.filesDir, "user_channels.json.tmp")
        try {
            val userChannels = _items.value.filter { it.sourceId != "default_demo" }
            tempFile.bufferedWriter(Charsets.UTF_8).use { bufferedWriter ->
                val jsonWriter = JsonWriter(bufferedWriter)
                jsonWriter.beginArray()
                userChannels.forEach { ch ->
                    jsonWriter.beginObject()
                    jsonWriter.name("id").value(ch.id)
                    jsonWriter.name("name").value(ch.name)
                    jsonWriter.name("streamUrl").value(ch.streamUrl)
                    if (ch.logoUrl != null) jsonWriter.name("logoUrl").value(ch.logoUrl) else jsonWriter.name("logoUrl").nullValue()
                    jsonWriter.name("category").value(ch.category)
                    if (ch.channelNumber != null) jsonWriter.name("channelNumber").value(ch.channelNumber.toLong()) else jsonWriter.name("channelNumber").nullValue()
                    jsonWriter.name("type").value(ch.type.name)
                    if (ch.groupTitle != null) jsonWriter.name("groupTitle").value(ch.groupTitle) else jsonWriter.name("groupTitle").nullValue()
                    if (ch.tvgId != null) jsonWriter.name("tvgId").value(ch.tvgId) else jsonWriter.name("tvgId").nullValue()
                    if (ch.overview != null) jsonWriter.name("overview").value(ch.overview) else jsonWriter.name("overview").nullValue()
                    jsonWriter.name("durationMs").value(ch.durationMs)
                    jsonWriter.name("sourceId").value(ch.sourceId)
                    jsonWriter.endObject()
                }
                jsonWriter.endArray()
                jsonWriter.flush()
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun addPlaylistItems(source: PlaylistSource, newItems: List<PlaylistItem>) {
        val currentPlaylists = _playlists.value.toMutableList()
        currentPlaylists.removeAll { it.id == source.id }
        currentPlaylists.add(source.copy(totalChannels = newItems.size))
        _playlists.value = currentPlaylists

        val current = _items.value.toMutableList()
        current.removeAll { it.sourceId == source.id }
        current.addAll(newItems)
        _items.value = current

        // Persist on background thread to keep UI completely responsive
        repoScope.launch {
            persistPlaylistsToDisk()
            persistChannelsToDisk()
        }
    }

    fun removePlaylist(sourceId: String) {
        _playlists.value = _playlists.value.filter { it.id != sourceId }
        _items.value = _items.value.filter { it.sourceId != sourceId }

        repoScope.launch {
            persistPlaylistsToDisk()
            persistChannelsToDisk()
        }
    }

    fun toggleFavorite(itemId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorite_ids", current).apply()

        _items.value = _items.value.map {
            if (it.id == itemId) it.copy(isFavorite = current.contains(itemId)) else it
        }
    }

    fun recordWatchHistory(item: PlaylistItem, positionMs: Long, durationMs: Long) {
        val currentList = _history.value.toMutableList()
        currentList.removeAll { it.itemId == item.id }

        val newEntry = WatchHistoryItem(
            itemId = item.id,
            title = item.name,
            streamUrl = item.streamUrl,
            posterUrl = item.logoUrl,
            category = item.category,
            type = item.type,
            positionMs = positionMs,
            durationMs = durationMs,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        currentList.add(0, newEntry)

        val trimmed = currentList.take(50)
        _history.value = trimmed

        repoScope.launch {
            try {
                val array = JSONArray()
                trimmed.forEach { entry ->
                    val obj = JSONObject().apply {
                        put("id", entry.id)
                        put("itemId", entry.itemId)
                        put("title", entry.title)
                        put("streamUrl", entry.streamUrl)
                        put("posterUrl", entry.posterUrl ?: "")
                        put("category", entry.category)
                        put("type", entry.type.name)
                        put("positionMs", entry.positionMs)
                        put("durationMs", entry.durationMs)
                        put("lastWatchedTimestamp", entry.lastWatchedTimestamp)
                    }
                    array.put(obj)
                }
                prefs.edit().putString("watch_history_json", array.toString()).apply()
            } catch (_: Throwable) {}
        }
    }

    fun markNsrWatched(media: NsrMedia) {
        val key = media.watchKey()
        if (!_watchedNsr.value.contains(key)) {
            val updated = _watchedNsr.value + key
            _watchedNsr.value = updated
            prefs.edit().putStringSet("watched_nsr_keys", updated).apply()
        }

        val entry = NsrHistoryItem(
            slug = media.slug,
            type = media.type,
            title = media.title,
            posterUrl = media.poster,
            year = media.year,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        val updatedHistory = (_nsrHistory.value.filterNot { it.watchKey == key } + entry)
            .sortedByDescending { it.lastWatchedTimestamp }
            .take(30)
        _nsrHistory.value = updatedHistory
        saveNsrHistory(updatedHistory)
    }

    fun isNsrWatched(media: NsrMedia): Boolean = _watchedNsr.value.contains(media.watchKey())

    fun clearHistory() {
        _history.value = emptyList()
        _watchedNsr.value = emptySet()
        _nsrHistory.value = emptyList()
        prefs.edit()
            .remove("watch_history_json")
            .remove("watched_nsr_keys")
            .remove("nsr_history_json")
            .apply()
    }

    fun setParentalPin(pin: String) {
        _parentalPin.value = pin
        prefs.edit().putString("parental_pin", pin).apply()
    }

    fun toggleCategoryLock(category: String) {
        val set = _lockedCategories.value.toMutableSet()
        if (set.contains(category)) {
            set.remove(category)
        } else {
            set.add(category)
        }
        _lockedCategories.value = set
        prefs.edit().putStringSet("locked_categories", set).apply()

        _items.value = _items.value.map {
            it.copy(isLocked = set.contains(it.category))
        }
    }

    fun setThemeAccent(theme: ThemeAccent) {
        _themeAccent.value = theme
        prefs.edit().putString("theme_accent", theme.name).apply()
    }

    fun setBufferMode(mode: BufferMode) {
        _bufferMode.value = mode
        prefs.edit().putString("buffer_mode", mode.name).apply()
    }
}
