package com.example.data.unlimplay

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * TMDB metadata + Unlimplay embed URLs.
 *
 * Only the metadata is queried through TMDB; playback uses the public
 * /f/embed/{type}/{tmdb_id} iframe as requested, so no stream/server scraping.
 */
class UnlimplayRepository(context: Context) {

    private val config = UnlimplayConfigStore(context.applicationContext)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val isConfigured: Boolean
        get() = config.apiKey().isNotBlank()

    fun configFingerprint(): String = config.baseUrl() + "|" + config.apiKey().hashCode()

    fun embedUrl(media: UnlimplayMedia, season: Int? = null, episode: Int? = null): String =
        media.embedUrl(config.baseUrl(), season, episode)

    suspend fun getSeasons(tmdbId: Long): List<UnlimplaySeason> = withContext(Dispatchers.IO) {
        if (tmdbId <= 0L) return@withContext emptyList()
        val json = get("/3/tv/$tmdbId", mapOf("language" to "es-MX"))
        val array = json.optJSONArray("seasons") ?: return@withContext emptyList()
        val seasons = mutableListOf<UnlimplaySeason>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val number = obj.optInt("season_number", 0)
            if (number <= 0) continue
            seasons.add(
                UnlimplaySeason(
                    number = number,
                    name = obj.optString("name").ifBlank { "Temporada $number" },
                    episodeCount = obj.optInt("episode_count", 0)
                )
            )
        }
        seasons.sortedBy { it.number }
    }

    suspend fun getEpisodes(tmdbId: Long, season: Int): List<UnlimplayEpisode> =
        withContext(Dispatchers.IO) {
            if (tmdbId <= 0L || season <= 0) return@withContext emptyList()
            val cacheKey = "$tmdbId:$season"
            val now = System.currentTimeMillis()
            synchronized(episodeCache) {
                episodeCache[cacheKey]?.let { (timestamp, cached) ->
                    if (now - timestamp < EPISODE_CACHE_TTL_MS) return@withContext cached
                }
            }
            val json = get(
                "/3/tv/$tmdbId/season/$season",
                mapOf("language" to "es-MX")
            )
            val array = json.optJSONArray("episodes") ?: JSONArray()
            val episodes = mutableListOf<UnlimplayEpisode>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val number = obj.optInt("episode_number", 0)
                if (number <= 0) continue
                episodes.add(
                    UnlimplayEpisode(
                        season = obj.optInt("season_number", season),
                        episode = number,
                        title = obj.optString("name").ifBlank { "Episodio $number" }
                    )
                )
            }
            val result = episodes.sortedBy { it.episode }
            synchronized(episodeCache) {
                episodeCache[cacheKey] = System.currentTimeMillis() to result
            }
            result
        }

    suspend fun getCatalog(type: UnlimplayType, page: Int = 1): UnlimplayPage =
        withContext(Dispatchers.IO) {
            val params = mutableMapOf(
                "page" to page.toString(),
                "language" to "es-MX",
                "include_adult" to "false"
            )
            val path = when (type) {
                UnlimplayType.MOVIE -> "/3/trending/movie/week"
                UnlimplayType.SERIES -> "/3/trending/tv/week"
                UnlimplayType.ANIME -> {
                    params["with_genres"] = "16"
                    params["with_origin_country"] = "JP"
                    params["sort_by"] = "popularity.desc"
                    "/3/discover/tv"
                }
            }
            val json = get(path, params)
            val results = parseResults(json.optJSONArray("results"), type)
            val current = json.optInt("page", page)
            val totalPages = json.optInt("total_pages", current)
            UnlimplayPage(
                items = results,
                page = current,
                totalPages = totalPages,
                hasNext = current < totalPages
            )
        }

    suspend fun search(query: String, preferredType: UnlimplayType? = null): List<UnlimplayMedia> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.isBlank()) return@withContext emptyList()
            val json = get(
                "/3/search/multi",
                mapOf(
                    "query" to clean,
                    "page" to "1",
                    "language" to "es-MX",
                    "include_adult" to "false"
                )
            )
            val raw = json.optJSONArray("results") ?: return@withContext emptyList()
            val items = mutableListOf<UnlimplayMedia>()
            for (i in 0 until raw.length()) {
                val obj = raw.optJSONObject(i) ?: continue
                val mediaType = obj.optString("media_type")
                if (mediaType != "movie" && mediaType != "tv") continue
                val genres = obj.optJSONArray("genre_ids").toIntList()
                val type = UnlimplayType.fromMedia(
                    mediaType = mediaType,
                    genreIds = genres,
                    originalLanguage = obj.optString("original_language")
                )
                if (preferredType == UnlimplayType.MOVIE && type != UnlimplayType.MOVIE) continue
                if (preferredType != null && preferredType != UnlimplayType.MOVIE &&
                    type == UnlimplayType.MOVIE
                ) continue
                items.add(parseItem(obj, type) ?: continue)
            }
            items
        }

    private fun parseResults(array: JSONArray?, forcedType: UnlimplayType): List<UnlimplayMedia> {
        if (array == null) return emptyList()
        val items = mutableListOf<UnlimplayMedia>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            parseItem(obj, forcedType)?.let(items::add)
        }
        return items
    }

    private fun parseItem(obj: JSONObject, forcedType: UnlimplayType): UnlimplayMedia? {
        val id = obj.optLong("id", 0L)
        if (id <= 0L) return null
        val title = obj.optString("title").ifBlank { obj.optString("name") }
        if (title.isBlank()) return null
        val date = obj.optString("release_date").ifBlank { obj.optString("first_air_date") }
        return UnlimplayMedia(
            id = id,
            title = title,
            posterPath = obj.optString("poster_path").takeIf { it.isNotBlank() },
            backdropPath = obj.optString("backdrop_path").takeIf { it.isNotBlank() },
            type = forcedType,
            year = date.take(4).takeIf { it.length == 4 },
            overview = obj.optString("overview").takeIf { it.isNotBlank() },
            rating = obj.optDouble("vote_average", 0.0).takeIf { it > 0.0 }
        )
    }

    private fun get(path: String, params: Map<String, String>): JSONObject {
        val builder = "https://api.themoviedb.org$path".toHttpUrlOrNull()?.newBuilder()
            ?: throw UnlimplayException("URL de TMDB inválida.")
        builder.addQueryParameter("api_key", config.apiKey())
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        val request = Request.Builder()
            .url(builder.build())
            .header("Accept", "application/json")
            .header("User-Agent", "BloodersTv/Android")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val json = try {
                if (body.isBlank()) JSONObject() else JSONObject(body)
            } catch (_: Throwable) {
                throw UnlimplayException("TMDB devolvió una respuesta inválida.")
            }
            if (!response.isSuccessful) {
                val message = json.optString("status_message").ifBlank {
                    if (response.code == 401) {
                        "TMDB rechazó la API Key."
                    } else {
                        "TMDB respondió con error ${response.code}."
                    }
                }
                throw UnlimplayException(message, response.code)
            }
            return json
        }
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        val result = mutableListOf<Int>()
        for (i in 0 until length()) result.add(optInt(i))
        return result
    }

    companion object {
        private const val EPISODE_CACHE_TTL_MS = 5 * 60 * 1000L
        private val episodeCache =
            mutableMapOf<String, Pair<Long, List<UnlimplayEpisode>>>()
    }
}
