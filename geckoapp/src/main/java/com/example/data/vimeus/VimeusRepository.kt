package com.example.data.vimeus

import android.content.Context
import com.example.data.network.HttpClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/**
 * Client for the Vimeus listing API and its embed URLs.
 *
 * The API has no text-search endpoint, so search uses the public IMDb
 * suggestions endpoint (the same tolerant partial-title path used by NSRPlay)
 * and maps the returned IMDb id directly to Vimeus' /e/... embeds.
 */
class VimeusRepository(context: Context) {

    private val config = VimeusConfigStore(context.applicationContext)

    private val client = HttpClients.api
    private val imdbClient = HttpClients.suggestions

    val isConfigured: Boolean
        get() = config.apiKey().isNotBlank() && config.viewKey().isNotBlank()

    fun configFingerprint(): String =
        config.baseUrl() + "|" + config.apiKey().hashCode() + "|" + config.viewKey().hashCode()

    fun viewKey(): String = config.viewKey()

    fun embedUrl(
        media: VimeusMedia,
        season: Int? = null,
        episode: Int? = null
    ): String = media.embedUrl(config.baseUrl(), config.viewKey(), season, episode)

    suspend fun getCatalog(type: VimeusType, page: Int = 1): VimeusPage = withContext(Dispatchers.IO) {
        val json = get(
            when (type) {
                VimeusType.MOVIE -> "/api/listing/movies"
                VimeusType.SERIES -> "/api/listing/series"
                VimeusType.ANIME -> "/api/listing/animes"
            },
            mapOf("page" to page.toString())
        )
        val data = json.optJSONObject("data") ?: JSONObject()
        val array = data.optJSONArray("result")
            ?: when (type) {
                VimeusType.MOVIE -> data.optJSONArray("movies")
                VimeusType.SERIES -> data.optJSONArray("series")
                VimeusType.ANIME -> data.optJSONArray("animes")
            }
        val items = parseListing(array, type)
        val pages = data.optInt("pages", data.optJSONObject("pagination")?.optInt("total_pages", page) ?: page)
        VimeusPage(
            items = items,
            page = data.optJSONObject("pagination")?.optInt("current_page", page) ?: page,
            totalPages = pages,
            hasNext = data.optJSONObject("pagination")?.optBoolean("has_next", page < pages)
                ?: (page < pages)
        )
    }

    suspend fun getEpisodes(
        tmdbId: Long? = null,
        imdbId: String? = null
    ): List<VimeusEpisode> = withContext(Dispatchers.IO) {
        val cacheKey = when {
            tmdbId != null && tmdbId > 0L -> "tmdb:$tmdbId"
            !imdbId.isNullOrBlank() -> "imdb:$imdbId"
            else -> return@withContext emptyList()
        }
        val now = System.currentTimeMillis()
        synchronized(episodeCache) {
            episodeCache[cacheKey]?.let { (timestamp, cached) ->
                if (now - timestamp < EPISODE_CACHE_TTL_MS) return@withContext cached
            }
        }
        val episodes = mutableListOf<VimeusEpisode>()
        var page = 1
        var totalPages = 1
        while (page <= totalPages && page <= 20) {
            val params = mutableMapOf("page" to page.toString())
            if (tmdbId != null && tmdbId > 0L) {
                params["tmdb_id"] = tmdbId.toString()
            } else {
                params["imdb_id"] = imdbId.orEmpty()
            }
            val json = get("/api/listing/episodes", params)
            val data = json.optJSONObject("data") ?: JSONObject()
            val array = data.optJSONArray("result") ?: data.optJSONArray("episodes") ?: JSONArray()
            totalPages = data.optInt("pages", data.optJSONObject("pagination")?.optInt("total_pages", 1) ?: 1)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val season = obj.optInt("season", obj.optString("season").toIntOrNull() ?: 0)
                val episode = obj.optInt("episode", obj.optString("episode").toIntOrNull() ?: 0)
                if (season > 0 && episode > 0) {
                    episodes.add(
                        VimeusEpisode(
                            season = season,
                            episode = episode,
                            title = obj.optString("episode_title").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            page++
        }
        val result = episodes.distinctBy { it.season to it.episode }
            .sortedWith(compareBy({ it.season }, { it.episode }))
        synchronized(episodeCache) {
            episodeCache[cacheKey] = System.currentTimeMillis() to result
        }
        result
    }

    /**
     * Partial-title search. First uses the already loaded catalog (cheap, no
     * network) and then IMDb suggestions to discover any title, even if its
     * Vimeus listing page has not been downloaded.
     */
    suspend fun search(query: String, preferredType: VimeusType? = null): List<VimeusMedia> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.isBlank()) return@withContext emptyList()

            val suggestions = fetchImdbSuggestions(clean)
            val mapped = suggestions.mapNotNull { suggestionToMedia(it, preferredType) }
            if (mapped.isNotEmpty()) return@withContext mapped

            // Last resort: scan the first listing page of the preferred type.
            try {
                val page = getCatalog(preferredType ?: VimeusType.MOVIE, 1)
                val normalized = normalize(clean)
                return@withContext page.items.filter { normalize(it.title).contains(normalized) }
            } catch (_: Throwable) {
                return@withContext emptyList()
            }
        }

    private fun suggestionToMedia(obj: JSONObject, preferredType: VimeusType?): VimeusMedia? {
        val imdb = obj.optString("id").takeIf { it.startsWith("tt") } ?: return null
        val title = obj.optString("l").takeIf { it.isNotBlank() } ?: return null
        val qid = obj.optString("qid").lowercase(Locale.ROOT).replace(" ", "")
        val looksSeries = qid.contains("tvseries") || qid.contains("tvmini") ||
            qid.contains("tvspecial") || qid.contains("podcast")
        val type = when {
            preferredType == VimeusType.ANIME -> VimeusType.ANIME
            preferredType == VimeusType.SERIES && looksSeries -> VimeusType.SERIES
            preferredType == VimeusType.MOVIE && !looksSeries -> VimeusType.MOVIE
            preferredType != null -> return null
            looksSeries -> VimeusType.SERIES
            else -> VimeusType.MOVIE
        }
        val image = obj.optJSONObject("i")?.optString("imageUrl")?.takeIf { it.isNotBlank() }
        return VimeusMedia(
            id = imdb,
            tmdbId = null,
            imdbId = imdb,
            title = title,
            poster = image,
            type = type,
            year = obj.optString("y").takeIf { it.isNotBlank() }
        )
    }

    private fun fetchImdbSuggestions(query: String): List<JSONObject> {
        val encoded = query.trim().replace(" ", "%20")
        val url = "https://v3.sg.media-imdb.com/suggestion/x/$encoded.json".toHttpUrlOrNull()
            ?: return emptyList()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) BloodersTv")
            .header("Accept", "application/json")
            .build()
        return try {
            imdbClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val array = JSONObject(body).optJSONArray("d") ?: return emptyList()
                (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseListing(array: JSONArray?, type: VimeusType): List<VimeusMedia> {
        if (array == null) return emptyList()
        val items = mutableListOf<VimeusMedia>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val tmdbId = obj.optLong("tmdb_id", 0L).takeIf { it > 0L }
            val imdbId = obj.optString("imdb_id").takeIf { it.isNotBlank() }
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: continue
            items.add(
                VimeusMedia(
                    id = tmdbId?.toString() ?: imdbId ?: title,
                    tmdbId = tmdbId,
                    imdbId = imdbId,
                    title = title,
                    poster = obj.optString("poster").takeIf { it.isNotBlank() },
                    backdrop = obj.optString("backdrop").takeIf { it.isNotBlank() },
                    type = type,
                    totalSeasons = obj.optInt("total_seasons", 0),
                    totalEpisodes = obj.optInt("total_episodes", 0),
                    apiEmbedUrl = obj.optString("embed_url").takeIf { it.startsWith("http") }
                )
            )
        }
        return items
    }

    private fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val base = config.baseUrl().trimEnd('/')
        val urlBuilder = "$base$path".toHttpUrlOrNull()?.newBuilder()
            ?: throw VimeusException("URL de Vimeus inválida.")
        params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("X-API-Key", config.apiKey())
            .header("Accept", "application/json")
            .header("User-Agent", "BloodersTv/Android")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val json = try {
                if (body.isBlank()) JSONObject() else JSONObject(body)
            } catch (_: Throwable) {
                throw VimeusException("Respuesta inválida de Vimeus.")
            }
            if (!response.isSuccessful || json.optBoolean("error", false)) {
                val message = json.optString("message").ifBlank {
                    "Vimeus respondió con error ${response.code}."
                }
                throw VimeusException(message, response.code)
            }
            return json
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)

    companion object {
        private const val EPISODE_CACHE_TTL_MS = 5 * 60 * 1000L
        private val episodeCache = mutableMapOf<String, Pair<Long, List<VimeusEpisode>>>()
    }
}
