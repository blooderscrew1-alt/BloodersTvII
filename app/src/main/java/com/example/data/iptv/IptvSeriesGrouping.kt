package com.example.data.iptv

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import java.text.Normalizer
import java.util.Locale

data class IptvEpisode(
    val item: PlaylistItem,
    val season: Int,
    val episode: Int,
    val title: String
)

data class IptvSeries(
    val key: String,
    val title: String,
    val posterUrl: String?,
    val category: String,
    val sourceId: String,
    val episodes: List<IptvEpisode>
) {
    val seasons: List<Int> get() = episodes.map { it.season }.distinct().sorted()
}

sealed interface IptvSearchResult {
    val title: String
    val posterUrl: String?
    val subtitle: String

    data class Single(val item: PlaylistItem) : IptvSearchResult {
        override val title: String get() = item.name
        override val posterUrl: String? get() = item.logoUrl
        override val subtitle: String get() = item.category
    }

    data class SeriesGroup(val series: IptvSeries) : IptvSearchResult {
        override val title: String get() = series.title
        override val posterUrl: String? get() = series.posterUrl
        override val subtitle: String get() = series.category
    }
}

/**
 * M3U lists usually expose one entry per episode. The UI needs one card per
 * series and a detail screen with the episodes, so this groups those entries by
 * their base title while preserving the original PlaylistItem/stream URL.
 */
object IptvSeriesGrouping {

    private val seasonEpisodeRegex = Regex(
        """\bS(\d{1,2})\s*[-_. ]?\s*E(\d{1,3})\b""",
        RegexOption.IGNORE_CASE
    )
    private val crossEpisodeRegex = Regex("""\b(\d{1,2})\s*x\s*(\d{1,3})\b""")
    private val seasonOnlyRegex = Regex("""\bS(\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val wordSeasonRegex = Regex(
        """\bT(?:emporada)?\s*(\d{1,2})\b""",
        RegexOption.IGNORE_CASE
    )
    private val wordEpisodeRegex = Regex(
        """\b(?:E|Ep|Episodio|Cap[ií]tulo)\s*(\d{1,3})\b""",
        RegexOption.IGNORE_CASE
    )

    fun group(items: List<PlaylistItem>): List<IptvSeries> {
        val grouped = LinkedHashMap<String, MutableList<ParsedEpisode>>()

        items.asSequence()
            .filter { it.type == ItemType.SERIES }
            .forEachIndexed { index, item ->
                val parsed = parse(item, index)
                val key = item.sourceId + "|" + normalize(parsed.title)
                grouped.getOrPut(key) { mutableListOf() }.add(parsed)
            }

        return grouped.map { (key, parsedEpisodes) ->
            val ordered = parsedEpisodes.sortedWith(
                compareBy({ it.season }, { it.episode }, { it.order })
            )
            val used = mutableMapOf<Int, Int>()
            val episodes = ordered.map { parsed ->
                var number = parsed.episode
                if (number <= 0) {
                    number = (used[parsed.season] ?: 0) + 1
                    while (ordered.any { it.season == parsed.season && it.episode == number }) {
                        number++
                    }
                }
                used[parsed.season] = maxOf(used[parsed.season] ?: 0, number)
                IptvEpisode(
                    item = parsed.item,
                    season = parsed.season,
                    episode = number,
                    title = parsed.episodeTitle
                )
            }
            val first = parsedEpisodes.first()
            IptvSeries(
                key = key,
                title = first.title,
                posterUrl = parsedEpisodes.firstNotNullOfOrNull { it.item.logoUrl?.takeIf { url -> url.isNotBlank() } },
                category = first.item.category,
                sourceId = first.item.sourceId,
                episodes = episodes
            )
        }.sortedBy { it.title.lowercase(Locale.ROOT) }
    }

    fun mergeSearchResults(items: List<PlaylistItem>): List<IptvSearchResult> {
        val normal = items.filter { it.type != ItemType.SERIES }
            .map { IptvSearchResult.Single(it) }
        val series = group(items).map { IptvSearchResult.SeriesGroup(it) }
        return normal + series
    }

    private fun parse(item: PlaylistItem, order: Int): ParsedEpisode {
        val name = item.name.trim()
        var season = 1
        var episode = 0
        var markerStart = -1
        var markerEnd = -1
        var rest = ""

        seasonEpisodeRegex.find(name)?.let { match ->
            season = match.groupValues[1].toIntOrNull() ?: 1
            episode = match.groupValues[2].toIntOrNull() ?: 0
            markerStart = match.range.first
            markerEnd = match.range.last + 1
        }
        if (markerStart < 0) {
            crossEpisodeRegex.find(name)?.let { match ->
                season = match.groupValues[1].toIntOrNull() ?: 1
                episode = match.groupValues[2].toIntOrNull() ?: 0
                markerStart = match.range.first
                markerEnd = match.range.last + 1
            }
        }
        if (markerStart < 0) {
            seasonOnlyRegex.find(name)?.let { match ->
                season = match.groupValues[1].toIntOrNull() ?: 1
                markerStart = match.range.first
                markerEnd = match.range.last + 1
            }
        }
        if (markerStart < 0) {
            wordSeasonRegex.find(name)?.let { match ->
                season = match.groupValues[1].toIntOrNull() ?: 1
                markerStart = match.range.first
                markerEnd = match.range.last + 1
            }
        }
        if (markerStart < 0) {
            wordEpisodeRegex.find(name)?.let { match ->
                episode = match.groupValues[1].toIntOrNull() ?: 0
                markerStart = match.range.first
                markerEnd = match.range.last + 1
            }
        }

        if (markerStart >= 0) {
            rest = name.substring(markerEnd).trim()
        }
        val episodeWord = wordEpisodeRegex.find(rest) ?: wordEpisodeRegex.find(name.substring(markerStart.coerceAtLeast(0)))
        if (episode <= 0 && episodeWord != null) {
            episode = episodeWord.groupValues[1].toIntOrNull() ?: 0
        }

        val base = if (markerStart > 0) {
            name.substring(0, markerStart).trim().trimEnd('-', '–', '|', ':', '.', ' ')
        } else {
            name.trim()
        }
        val restClean = rest.trim().trimStart('-', '–', '|', ':', '.', ' ')
        val cleanBase = if (markerStart <= 0 || base.isBlank()) {
            restClean.ifBlank { name.trim() }
        } else {
            base.ifBlank { name.trim() }
        }
        val episodeTitle = if (restClean.isNotBlank() &&
            !restClean.equals(cleanBase, ignoreCase = true) &&
            !restClean.matches(Regex("""\d{1,3}"""))
        ) {
            restClean
        } else if (episode > 0) {
            "Episodio $episode"
        } else {
            cleanBase
        }

        return ParsedEpisode(
            item = item,
            title = cleanBase,
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            order = order
        )
    }

    private fun normalize(value: String): String {
        val withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
        return withoutAccents.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
    }

    private data class ParsedEpisode(
        val item: PlaylistItem,
        val title: String,
        val season: Int,
        val episode: Int,
        val episodeTitle: String,
        val order: Int
    )
}
