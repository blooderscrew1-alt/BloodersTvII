package com.example.data.nsrplay

import android.content.Context
import android.net.Uri
import com.example.data.network.HttpClients
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

class NsrPlayException(message: String) : Exception(message)

/**
 * Small, dependency-free client for the NSRPlay public API.
 *
 * We deliberately do not cache responses on disk: their terms forbid building a
 * local mirror of the index. The UI keeps only the current page in memory.
 */
class NsrPlayRepository(context: Context) {

    private val config = NsrPlayConfigStore(context.applicationContext)

    private val client = HttpClients.api
    private val imdbClient = HttpClients.suggestions

    val isConfigured: Boolean
        get() = config.apiKey().isNotBlank()

    /** Identifies the active key + URL without exposing the key itself. */
    fun configFingerprint(): String = config.baseUrl() + "|" + config.apiKey().hashCode()

    suspend fun getCatalog(
        type: NsrMediaType,
        page: Int = 1,
        genre: String? = null
    ): NsrCatalogPage = withContext(Dispatchers.IO) {
        val params = mutableMapOf("type" to type.apiValue, "page" to page.toString())
        if (!genre.isNullOrBlank()) params["genre"] = genre

        val json = get("/api/v1/content/catalog", params)
        val data = json.optJSONObject("data") ?: json
        val items = parseItemArray(data.optJSONArray("items"))
        NsrCatalogPage(
            items = items,
            page = data.optInt("page", page),
            hasNextPage = data.optBoolean("hasNextPage", items.size >= 20)
        )
    }

    fun webPlayerUrl(
        media: NsrMedia,
        tmdbId: Long? = null,
        season: Int? = null,
        episode: Int? = null
    ): String {
        val base = config.baseUrl().trimEnd('/')
        val id = tmdbId?.toString() ?: media.slug
        return if (media.type == NsrMediaType.SERIES) {
            "$base/embed/tv/$id/${season ?: 1}/${episode ?: 1}"
        } else {
            "$base/embed/movie/$id"
        }
    }

    suspend fun search(query: String): List<NsrMedia> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val json = get("/api/v1/content/search", mapOf("s" to query.trim()))
        parseDataArray(json)
    }

    /**
     * NSRPlay's /content/search currently returns an empty list for many valid
     * titles. Their /content/info endpoint does work when it receives the slug,
     * so a title search first tries the slug directly (movie and series in
     * parallel), then the official search, and finally the loaded catalog.
     */
    suspend fun searchWithCatalogFallback(
        query: String,
        preferredType: NsrMediaType? = null
    ): List<NsrMedia> {
        val cleanQuery = cleanTitle(query).trim()
        if (cleanQuery.isBlank()) return emptyList()

        val cacheKey = configFingerprint() +
            "|${preferredType?.apiValue ?: "all"}:${cleanQuery.lowercase(Locale.ROOT)}"
        cachedSearch(cacheKey)?.let { return it }

        // 1) Try the title as a provider slug. "Chappie" -> /info/chappie.
        //    Sequential and filtered by the active tab: a movie query does not
        //    wait for a slow series lookup, and vice versa.
        val types = when (preferredType) {
            NsrMediaType.MOVIE -> listOf(NsrMediaType.MOVIE)
            NsrMediaType.SERIES -> listOf(NsrMediaType.SERIES)
            null -> listOf(NsrMediaType.MOVIE, NsrMediaType.SERIES)
        }

        // 1) IMDb suggestions turn a partial query ("saul") into full titles
        //    ("Better Call Saul"). Build the cards directly from the suggestion
        //    instead of doing one slow /content/info request per title; the
        //    detail screen loads metadata lazily when the user opens it.
        val imdbResults = mutableListOf<NsrMedia>()
        for (suggestion in fetchImdbSuggestions(cleanQuery, preferredType).take(12)) {
            if (preferredType != null && suggestion.type != preferredType) continue
            if (!titleMatchesQuery(suggestion.title, cleanQuery)) continue
            val slug = slugFromTitle(suggestion.title)
            if (slug.isBlank()) continue
            imdbResults.add(
                NsrMedia(
                    id = slug,
                    slug = slug,
                    title = suggestion.title,
                    poster = suggestion.poster,
                    rating = null,
                    type = suggestion.type,
                    provider = "IMDb",
                    url = null,
                    year = suggestion.year
                )
            )
            if (imdbResults.size >= 8) break
        }
        if (imdbResults.isNotEmpty()) {
            val result = imdbResults.distinctBy { it.type.apiValue + ":" + it.slug }.take(30)
            cacheSearch(cacheKey, result)
            return result
        }

        // 2) Try the query itself as a provider slug ("chappie" -> /info/chappie).
        val slugResults = mutableListOf<NsrMedia>()
        for (slug in slugCandidates(cleanQuery)) {
            for (type in types) {
                val info = tryInfo(slug, type)
                if (info != null && titleMatchesQuery(info.title, cleanQuery)) {
                    slugResults.add(info)
                }
            }
            if (slugResults.isNotEmpty()) break
        }
        if (slugResults.isNotEmpty()) {
            val result = slugResults.distinctBy { it.type.apiValue + ":" + it.slug }.take(30)
            cacheSearch(cacheKey, result)
            return result
        }

        // 3) Official search endpoint.
        val direct = try {
            search(cleanQuery)
        } catch (_: Throwable) {
            emptyList()
        }
        if (direct.isNotEmpty()) {
            val result = if (preferredType == null) direct else direct.filter { it.type == preferredType }
            cacheSearch(cacheKey, result)
            return result
        }

        // 4) Last resort: first catalog page of movies + series.
        val movies = getCatalog(NsrMediaType.MOVIE, page = 1).items
        val series = getCatalog(NsrMediaType.SERIES, page = 1).items
        val catalogResults = movies + series

        val result = catalogResults
            .filter { it.title.contains(cleanQuery, ignoreCase = true) }
            .filter { preferredType == null || it.type == preferredType }
            .distinctBy { it.type.apiValue + ":" + it.slug }
            .take(30)
        cacheSearch(cacheKey, result)
        return result
    }

    private suspend fun fetchImdbSuggestions(
        query: String,
        preferredType: NsrMediaType?
    ): List<ImdbSuggestion> = withContext(Dispatchers.IO) {
        val url = "https://v3.sg.media-imdb.com/suggestion/x/" +
            Uri.encode(query.trim().lowercase(Locale.ROOT)) + ".json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            .build()

        try {
            imdbClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONArray("d") ?: return@withContext emptyList()
                val result = ArrayList<ImdbSuggestion>()

                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val qid = obj.optString("qid")
                    val type = when (qid) {
                        "movie", "tvMovie" -> NsrMediaType.MOVIE
                        "tvSeries", "tvMiniSeries", "tvShort", "tvSpecial" ->
                            NsrMediaType.SERIES
                        else -> null
                    } ?: continue
                    if (preferredType != null && type != preferredType) continue

                    val title = obj.optString("l").trim()
                    if (title.isBlank()) continue
                    val poster = obj.optJSONObject("i")
                        ?.optString("imageUrl")
                        ?.takeIf { it.isNotBlank() }
                    val year = obj.optString("y").takeIf { it.isNotBlank() && it != "null" }

                    result.add(ImdbSuggestion(title, year, poster, type))
                    if (result.size >= 5) break
                }
                return@withContext result
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun slugFromTitle(title: String): String = normalizeSlug(cleanTitle(title))

    private data class ImdbSuggestion(
        val title: String,
        val year: String?,
        val poster: String?,
        val type: NsrMediaType
    )

    private fun cachedSearch(key: String): List<NsrMedia>? {
        val entry = searchCache[key] ?: return null
        if (System.currentTimeMillis() - entry.first > SEARCH_CACHE_TTL_MS) {
            searchCache.remove(key)
            return null
        }
        return entry.second
    }

    private fun cacheSearch(key: String, results: List<NsrMedia>) {
        // Never cache an empty result: a transient provider failure must not
        // make the same search look "dead" for five minutes.
        if (results.isEmpty()) return
        if (searchCache.size > 40) searchCache.clear()
        searchCache[key] = System.currentTimeMillis() to results
    }

    private suspend fun tryInfo(slug: String, type: NsrMediaType): NsrMedia? {
        return try {
            getInfo(slug, type)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    private fun titleMatchesQuery(title: String, query: String): Boolean {
        val normalizedTitle = normalizeSlug(title).replace("-", "")
        val normalizedQuery = normalizeSlug(query).replace("-", "")
        if (normalizedTitle.isBlank() || normalizedQuery.isBlank()) return false
        return normalizedTitle.contains(normalizedQuery) ||
            normalizedQuery.contains(normalizedTitle)
    }

    private fun slugCandidates(query: String): List<String> {
        val candidates = linkedSetOf<String>()

        fun add(value: String) {
            val slug = normalizeSlug(value)
            if (slug.length >= 2) candidates.add(slug)
        }

        add(query)
        // Drop a trailing year: "chappie 2015" -> "chappie".
        add(query.replace(Regex("\\b(19|20)\\d{2}\\b"), " "))
        // English/Spanish articles often omitted from provider slugs.
        add(query.replace(Regex("(?i)^(the|el|la|los|las)\\s+"), " "))
        // A common separator variant: "spider man" -> "spider-man".
        add(query.replace(" ", "-"))

        return candidates.take(4).toList()
    }

    private fun normalizeSlug(value: String): String {
        val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return withoutAccents
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    suspend fun getInfo(slug: String, type: NsrMediaType): NsrMedia = withContext(Dispatchers.IO) {
        val json = get(
            "/api/v1/content/info/${slug.trim()}",
            mapOf("type" to type.apiValue)
        )
        val data = json.optJSONObject("data")
            ?: json.optJSONArray("data")?.optJSONObject(0)
            ?: throw NsrPlayException("NSRPlay no devolvió detalles para este título.")
        parseMedia(data, fallbackType = type)
            ?: throw NsrPlayException("No se pudo leer la información del título.")
    }

    suspend fun getServers(
        slug: String,
        season: Int,
        episode: Int
    ): List<NsrServer> = withContext(Dispatchers.IO) {
        val json = get(
            "/api/v1/content/servers",
            mapOf(
                "slug" to slug.trim(),
                "season" to season.toString(),
                "episode" to episode.toString()
            )
        )
        val data = json.optJSONObject("data") ?: json
        parseServers(data.optJSONArray("servers"))
    }

    /**
     * Verifies a key/base URL without saving them. Returns the number of titles
     * in the first catalog page so the UI can show that the call really worked.
     */
    suspend fun testConnection(apiKey: String, baseUrl: String): Int = withContext(Dispatchers.IO) {
        val json = get(
            path = "/api/v1/content/catalog",
            query = mapOf("type" to "movie", "page" to "1"),
            apiKeyOverride = apiKey,
            baseUrlOverride = baseUrl
        )
        val data = json.optJSONObject("data") ?: json
        parseItemArray(data.optJSONArray("items")).size
    }

    private fun get(
        path: String,
        query: Map<String, String>,
        apiKeyOverride: String? = null,
        baseUrlOverride: String? = null
    ): JSONObject {
        val apiKey = if (apiKeyOverride != null) {
            config.effectiveApiKey(apiKeyOverride)
        } else {
            config.apiKey()
        }
        val baseUrl = if (baseUrlOverride != null) {
            config.effectiveBaseUrl(baseUrlOverride)
        } else {
            config.baseUrl()
        }

        if (apiKey.isBlank()) {
            throw NsrPlayException(
                "Falta la API Key de NSRPlay. Configúrala en Ajustes > NSRPlay."
            )
        }

        val urlBuilder = (baseUrl + path).toHttpUrlOrNull()
            ?.newBuilder()
            ?: throw NsrPlayException("URL de NSRPlay no válida.")

        query.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
        val url = urlBuilder.build()

        val request = Request.Builder()
            .url(url)
            .header("X-API-Key", apiKey)
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            )
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    401, 403 -> "La API Key de NSRPlay no es válida o fue revocada."
                    404 -> "NSRPlay no encontró ese contenido."
                    429 -> "Límite de NSRPlay alcanzado. Espera un minuto e inténtalo de nuevo."
                    in 500..599 -> "NSRPlay está teniendo problemas. Inténtalo más tarde."
                    else -> "NSRPlay respondió ${response.code}."
                }
                throw NsrPlayException(message)
            }

            val json = try {
                JSONObject(body)
            } catch (_: Throwable) {
                throw NsrPlayException("Respuesta no válida de NSRPlay.")
            }

            if (json.has("success") && !json.optBoolean("success", false)) {
                val apiMessage = json.optString("message")
                    .ifBlank { json.optString("error") }
                    .ifBlank { "NSRPlay no pudo completar la solicitud." }
                throw NsrPlayException(apiMessage)
            }
            return json
        }
    }

    private fun parseDataArray(json: JSONObject): List<NsrMedia> {
        val data = json.opt("data")
        return when (data) {
            is JSONArray -> parseItemArray(data)
            is JSONObject -> parseItemArray(
                data.optJSONArray("items") ?: data.optJSONArray("results")
            )
            else -> emptyList()
        }
    }

    private fun parseItemArray(array: JSONArray?): List<NsrMedia> {
        if (array == null) return emptyList()
        val result = ArrayList<NsrMedia>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            parseMedia(item, fallbackType = null)?.let { result.add(it) }
        }
        return result
    }

    private fun parseMedia(obj: JSONObject, fallbackType: NsrMediaType?): NsrMedia? {
        val slug = obj.optString("slug")
            .ifBlank { obj.optString("id") }
            .trim()
        if (slug.isBlank()) return null

        val rawTitle = obj.optString("title")
            .ifBlank { obj.optString("name") }
            .ifBlank { slug }
        val title = cleanTitle(fixMojibake(rawTitle))
        val type = fallbackType ?: NsrMediaType.fromApi(obj.optString("type"))

        val poster = obj.optString("poster")
            .ifBlank { obj.optString("image") }
            .ifBlank { obj.optString("cover") }
            .takeIf { it.isNotBlank() }

        val rating = obj.optString("rating").takeIf {
            it.isNotBlank() && it != "null" && it.toDoubleOrNull() != null
        }

        val genres = mutableListOf<String>()
        obj.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) {
                val genreObj = arr.optJSONObject(i)
                val name = if (genreObj != null) {
                    genreObj.optString("name")
                } else {
                    arr.optString(i)
                }
                val clean = fixMojibake(name).trim()
                if (clean.isNotBlank() && genres.none { it.equals(clean, ignoreCase = true) }) {
                    genres.add(clean)
                }
            }
        }

        val seasons = mutableListOf<NsrSeason>()
        obj.optJSONArray("seasons")?.let { arr ->
            for (i in 0 until arr.length()) {
                val seasonObj = arr.optJSONObject(i) ?: continue
                val number = seasonObj.optInt("number", i + 1)
                val episodes = mutableListOf<NsrEpisode>()
                seasonObj.optJSONArray("episodes")?.let { eps ->
                    for (e in 0 until eps.length()) {
                        val ep = eps.optJSONObject(e) ?: continue
                        episodes.add(
                            NsrEpisode(
                                number = ep.optInt("number", e + 1),
                                title = fixMojibake(ep.optString("title"))
                                    .replace('\n', ' ')
                                    .replace(Regex("\\s+"), " ")
                                    .trim()
                                    .ifBlank { "Episodio ${e + 1}" },
                                season = ep.optInt("season", number)
                            )
                        )
                    }
                }
                seasons.add(
                    NsrSeason(
                        number = number,
                        name = fixMojibake(seasonObj.optString("name"))
                            .ifBlank { "Temporada $number" },
                        episodes = episodes
                    )
                )
            }
        }

        return NsrMedia(
            id = obj.optString("id").ifBlank { slug },
            slug = slug,
            title = title,
            poster = poster,
            rating = rating,
            type = type,
            provider = obj.optString("provider").takeIf { it.isNotBlank() },
            url = obj.optString("url").takeIf { it.isNotBlank() },
            year = obj.optString("year").takeIf { it.isNotBlank() && it != "null" },
            synopsis = fixMojibake(obj.optString("synopsis")).takeIf { it.isNotBlank() },
            genres = genres,
            seasons = seasons,
            servers = parseServers(obj.optJSONArray("servers"))
        )
    }

    private fun parseServers(array: JSONArray?): List<NsrServer> {
        if (array == null) return emptyList()
        val result = ArrayList<NsrServer>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val embed = obj.optString("embedUrl")
                .ifBlank { obj.optString("url") }
                .trim()
            if (embed.isBlank()) continue
            result.add(
                NsrServer(
                    name = obj.optString("name").ifBlank { obj.optString("server") },
                    server = obj.optString("server").ifBlank { obj.optString("name") },
                    language = obj.optString("language").ifBlank { "Español" },
                    embedUrl = embed
                )
            )
        }
        return result
    }

    companion object {
        private const val SEARCH_CACHE_TTL_MS = 5 * 60 * 1000L
        private val searchCache =
            java.util.Collections.synchronizedMap(
                mutableMapOf<String, Pair<Long, List<NsrMedia>>>()
            )

        private val TITLE_PREFIX = Regex("(?i)^\\s*ver\\s+")
        private val TITLE_SUFFIXES = listOf(
            Regex("(?i)\\s+online\\s+gratis\\s+hd\\s*$"),
            Regex("(?i)\\s+online\\s+gratis\\s*$"),
            Regex("(?i)\\s+gratis\\s+hd\\s*$"),
            Regex("(?i)\\s+online\\s*$"),
            Regex("(?i)\\s+hd\\s*$")
        )

        fun cleanTitle(raw: String): String {
            var title = raw.replace(Regex("\\s+"), " ").trim()
            title = title.replace(TITLE_PREFIX, "")
            TITLE_SUFFIXES.forEach { title = title.replace(it, "") }
            return title.trim().ifBlank { raw.trim() }
        }

        /**
         * Some provider pages return UTF-8 bytes already decoded as Latin-1
         * ("corazÃ³n"). Repair only when the result is cleaner, never blindly.
         */
        fun fixMojibake(value: String): String {
            if (value.isBlank()) return value
            if (!value.contains("Ã") && !value.contains("Â") && !value.contains("â")) return value
            return try {
                val repaired = String(value.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                if (repaired.contains('\uFFFD')) value else repaired
            } catch (_: Throwable) {
                value
            }
        }

        fun languageLabel(language: String): String {
            val normalized = language.lowercase(Locale.ROOT)
            return when {
                normalized.contains("latino") -> "Latino"
                normalized.contains("castellano") || normalized.contains("espa") -> "Español"
                normalized.contains("sub") -> "Subtitulado"
                normalized.contains("ingl") || normalized.contains("english") -> "Inglés"
                language.isBlank() -> "Servidor"
                else -> language
            }
        }
    }
}
