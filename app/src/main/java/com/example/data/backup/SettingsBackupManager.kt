package com.example.data.backup

import android.content.Context
import com.example.data.model.BufferMode
import com.example.data.model.ItemType
import com.example.data.model.PlaylistSource
import com.example.data.model.SourceType
import com.example.data.model.ThemeAccent
import com.example.data.nsrplay.NsrPlayConfigStore
import com.example.data.parser.IptvDownloader
import com.example.data.parser.M3uParser
import com.example.data.parser.XtreamCodesClient
import com.example.data.repository.IptvRepository
import com.example.data.unlimplay.UnlimplayConfigStore
import com.example.data.vimeus.VimeusConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BackupImportResult(
    val playlistsAdded: Int,
    val playlistsSkipped: Int,
    val playlistsFailed: Int,
    val warnings: List<String>
)

/**
 * Portable configuration backup: user playlists and app/provider settings.
 *
 * M3U URL and Xtream lists are exported as definitions and re-downloaded on the
 * target device. Pasted M3U text is not portable (the original text is not kept
 * in storage), so those entries are reported as warnings.
 */
object SettingsBackupManager {

    private const val SCHEMA = 1

    fun buildJson(context: Context, repository: IptvRepository): String {
        val nsrStore = NsrPlayConfigStore(context.applicationContext)
        val vimeusStore = VimeusConfigStore(context.applicationContext)
        val unlimplayStore = UnlimplayConfigStore(context.applicationContext)

        val playlists = JSONArray()
        val warnings = JSONArray()
        repository.playlists.value
            .filter { it.type != SourceType.DEMO }
            .forEach { playlist ->
                if (playlist.type == SourceType.M3U_TEXT || playlist.type == SourceType.LOCAL) {
                    warnings.put(
                        "La lista \"${playlist.name}\" se pegó como texto y no se puede transferir; agrégala por URL o Xtream."
                    )
                }
                playlists.put(
                    JSONObject().apply {
                        put("name", playlist.name)
                        put("type", playlist.type.name)
                        put("url", playlist.url)
                        put("username", playlist.username)
                        put("password", playlist.password)
                        put("selectedCategories", JSONArray(playlist.selectedCategories.toList()))
                        put(
                            "categoryEdits",
                            buildCategoryEdits(
                                repository.items.value.filter { it.sourceId == playlist.id }
                            )
                        )
                    }
                )
            }

        val settings = JSONObject().apply {
            put("themeAccent", repository.themeAccent.value.name)
            put("bufferMode", repository.bufferMode.value.name)
            put("searchPosterScale", repository.searchPosterScale.value)
            put("blockGamblingAds", repository.blockGamblingAds.value)
            put("parentalPin", repository.parentalPin.value)
            put("lockedCategories", JSONArray(repository.lockedCategories.value.toList()))
            put("favorites", JSONArray(repository.favorites.value.toList()))
            put(
                "nsrPlay",
                JSONObject().apply {
                    put("apiKey", nsrStore.apiKey())
                    put("baseUrl", nsrStore.baseUrl())
                }
            )
            put(
                "vimeus",
                JSONObject().apply {
                    put("apiKey", vimeusStore.apiKey())
                    put("viewKey", vimeusStore.viewKey())
                    put("baseUrl", vimeusStore.baseUrl())
                }
            )
            put(
                "unlimplay",
                JSONObject().apply {
                    put("apiKey", unlimplayStore.apiKey())
                    put("baseUrl", unlimplayStore.baseUrl())
                }
            )
        }

        return JSONObject().apply {
            put("app", "BloodersTv")
            put("schema", SCHEMA)
            put("exportedAt", System.currentTimeMillis())
            put("settings", settings)
            put("playlists", playlists)
            put("warnings", warnings)
        }.toString(2)
    }

    suspend fun importJson(
        context: Context,
        repository: IptvRepository,
        jsonText: String,
        onProgress: (String) -> Unit = {}
    ): BackupImportResult = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(jsonText)
        } catch (_: Throwable) {
            throw IllegalArgumentException("El archivo no es un respaldo válido de BloodersTv.")
        }
        if (root.optString("app") != "BloodersTv") {
            throw IllegalArgumentException("El archivo no pertenece a BloodersTv.")
        }
        if (root.optInt("schema", 0) > SCHEMA) {
            throw IllegalArgumentException("El respaldo fue creado por una versión más nueva de la app.")
        }

        root.optJSONObject("settings")?.let { settings ->
            settings.optString("themeAccent").let { name ->
                runCatching { ThemeAccent.valueOf(name) }.getOrNull()?.let { repository.setThemeAccent(it) }
            }
            settings.optString("bufferMode").let { name ->
                runCatching { BufferMode.valueOf(name) }.getOrNull()?.let { repository.setBufferMode(it) }
            }
            if (settings.has("searchPosterScale")) {
                repository.setSearchPosterScale(
                    settings.optDouble("searchPosterScale", 0.5).toFloat()
                )
            }
            if (settings.has("blockGamblingAds")) {
                repository.setBlockGamblingAds(settings.optBoolean("blockGamblingAds", true))
            }
            settings.optString("parentalPin").takeIf { it.matches(Regex("\\d{4}")) }
                ?.let { repository.setParentalPin(it) }
            settings.optJSONArray("lockedCategories")?.toStringSet()
                ?.let { repository.setLockedCategories(it) }
            settings.optJSONArray("favorites")?.toStringSet()
                ?.let { repository.setFavorites(it) }

            settings.optJSONObject("nsrPlay")?.let { nsr ->
                NsrPlayConfigStore(context.applicationContext).save(
                    apiKey = nsr.optString("apiKey").takeIf { it.isNotBlank() },
                    baseUrl = nsr.optString("baseUrl").takeIf { it.isNotBlank() }
                )
            }
            settings.optJSONObject("vimeus")?.let { vimeus ->
                VimeusConfigStore(context.applicationContext).save(
                    apiKey = vimeus.optString("apiKey").takeIf { it.isNotBlank() },
                    viewKey = vimeus.optString("viewKey").takeIf { it.isNotBlank() },
                    baseUrl = vimeus.optString("baseUrl").takeIf { it.isNotBlank() }
                )
            }
            settings.optJSONObject("unlimplay")?.let { unlimplay ->
                UnlimplayConfigStore(context.applicationContext).save(
                    apiKey = unlimplay.optString("apiKey").takeIf { it.isNotBlank() },
                    baseUrl = unlimplay.optString("baseUrl").takeIf { it.isNotBlank() }
                )
            }
        }

        val existing = repository.playlists.value.mapNotNull { playlist ->
            val key = when (playlist.type) {
                SourceType.M3U_URL -> "url:${playlist.url.trim()}"
                SourceType.XTREAM -> "xtream:${playlist.url.trim()}:${playlist.username.trim()}"
                else -> null
            }
            key
        }.toMutableSet()

        var added = 0
        var skipped = 0
        var failed = 0
        val warnings = mutableListOf<String>()
        val array = root.optJSONArray("playlists") ?: JSONArray()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name").ifBlank { "Lista importada" }
            val type = runCatching {
                SourceType.valueOf(obj.optString("type", SourceType.M3U_URL.name))
            }.getOrDefault(SourceType.M3U_URL)
            val url = obj.optString("url").trim()
            val username = obj.optString("username").trim()
            val password = obj.optString("password")
            val selected = obj.optJSONArray("selectedCategories")?.toStringSet() ?: emptySet()
            val sourceId = UUID.randomUUID().toString()

            when (type) {
                SourceType.M3U_URL -> {
                    val key = "url:$url"
                    if (url.isBlank() || key in existing) {
                        skipped++
                        continue
                    }
                    onProgress("Descargando \"$name\"...")
                    val downloaded = IptvDownloader.downloadToFile(context, url)
                    downloaded.fold(
                        onSuccess = { file ->
                            try {
                                val categories = M3uParser.extractCategoriesFromFile(file)
                                    .map { it.name }
                                    .toSet()
                                val allowed: Set<String> = if (selected.isEmpty()) categories else selected
                                val items = M3uParser.parseFilteredFromFile(file, allowed, sourceId)
                                if (items.isEmpty()) {
                                    failed++
                                    warnings.add("\"$name\" no devolvió canales.")
                                } else {
                                    repository.addPlaylistItems(
                                        PlaylistSource(
                                            id = sourceId,
                                            name = name,
                                            type = SourceType.M3U_URL,
                                            url = url,
                                            selectedCategories = allowed,
                                            totalChannels = items.size
                                        ),
                                        items
                                    )
                                    applyCategoryEdits(
                                        repository = repository,
                                        sourceId = sourceId,
                                        edits = obj.optJSONArray("categoryEdits")
                                    )
                                    existing.add(key)
                                    added++
                                }
                            } catch (t: Throwable) {
                                failed++
                                warnings.add("\"$name\": ${t.localizedMessage ?: "formato no válido"}")
                            } finally {
                                file.delete()
                            }
                        },
                        onFailure = {
                            failed++
                            warnings.add("\"$name\": ${it.localizedMessage ?: "no se pudo descargar"}")
                        }
                    )
                }

                SourceType.XTREAM -> {
                    val key = "xtream:$url:$username"
                    if (url.isBlank() || username.isBlank() || key in existing) {
                        skipped++
                        continue
                    }
                    onProgress("Conectando con \"$name\"...")
                    XtreamCodesClient().fetchStreams(
                        serverUrl = url,
                        username = username,
                        password = password,
                        allowedCategories = selected.takeIf { it.isNotEmpty() },
                        sourceId = sourceId
                    ).fold(
                        onSuccess = { items ->
                            if (items.isEmpty()) {
                                failed++
                                warnings.add("\"$name\" no devolvió canales.")
                            } else {
                                repository.addPlaylistItems(
                                    PlaylistSource(
                                        id = sourceId,
                                        name = name,
                                        type = SourceType.XTREAM,
                                        url = url,
                                        username = username,
                                        password = password,
                                        selectedCategories = selected,
                                        totalChannels = items.size
                                    ),
                                    items
                                )
                                applyCategoryEdits(
                                    repository = repository,
                                    sourceId = sourceId,
                                    edits = obj.optJSONArray("categoryEdits")
                                )
                                existing.add(key)
                                added++
                            }
                        },
                        onFailure = {
                            failed++
                            warnings.add("\"$name\": ${it.localizedMessage ?: "autenticación fallida"}")
                        }
                    )
                }

                SourceType.M3U_TEXT, SourceType.LOCAL -> {
                    warnings.add("\"$name\" se pegó como texto y debe volver a agregarse manualmente.")
                    skipped++
                }

                SourceType.DEMO -> skipped++
            }
        }

        onProgress("Respaldo importado.")
        BackupImportResult(
            playlistsAdded = added,
            playlistsSkipped = skipped,
            playlistsFailed = failed,
            warnings = warnings
        )
    }

    private fun JSONArray.toStringSet(): Set<String> {
        val result = mutableSetOf<String>()
        for (i in 0 until length()) {
            optString(i).takeIf { it.isNotBlank() }?.let(result::add)
        }
        return result
    }

    private fun buildCategoryEdits(
        items: List<com.example.data.model.PlaylistItem>
    ): JSONArray {
        val edits = JSONArray()
        items.groupBy { it.originalCategory ?: it.category }.forEach { (original, categoryItems) ->
            if (original.isBlank() || categoryItems.isEmpty()) return@forEach
            val currentName = categoryItems
                .groupingBy { it.category }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: original
            val type = categoryItems
                .groupingBy { it.type }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: ItemType.LIVE
            edits.put(
                JSONObject().apply {
                    put("original", original)
                    put("name", currentName)
                    put("type", type.name)
                    put("hidden", categoryItems.all { it.isHidden })
                }
            )
        }
        return edits
    }

    private fun applyCategoryEdits(
        repository: IptvRepository,
        sourceId: String,
        edits: JSONArray?
    ) {
        if (edits == null) return
        for (i in 0 until edits.length()) {
            val edit = edits.optJSONObject(i) ?: continue
            val original = edit.optString("original").trim()
            val current = edit.optString("name").trim().ifBlank { original }
            if (original.isBlank() || current.isBlank()) continue

            if (original != current) {
                repository.renameCategory(sourceId, original, current)
            }
            val type = runCatching {
                ItemType.valueOf(edit.optString("type", ItemType.LIVE.name))
            }.getOrDefault(ItemType.LIVE)
            repository.setCategoryType(sourceId, current, type)
            repository.setCategoryHidden(sourceId, current, edit.optBoolean("hidden", false))
        }
    }
}
