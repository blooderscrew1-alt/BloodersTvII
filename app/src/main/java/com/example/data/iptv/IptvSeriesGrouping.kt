package com.example.data.iptv

import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
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
 *
 * The parser is deliberately a single regex scan per entry: IPTV lists can
 * contain more than 20 000 items and running several regexes per item made the
 * Series tab take seconds on a TV box.
 */
object IptvSeriesGrouping {

    private data class Marker(
        val start: Int,
        val end: Int,
        val season: Int,
        val episode: Int
    )

    fun group(items: List<PlaylistItem>): List<IptvSeries> {
        val grouped = LinkedHashMap<String, MutableList<ParsedEpisode>>()

        for ((index, item) in items.withIndex()) {
            if (item.type != ItemType.SERIES) continue
            val parsed = parse(item, index)
            val key = item.sourceId + "|" + normalize(parsed.title)
            grouped.getOrPut(key) { mutableListOf() }.add(parsed)
        }

        // Sort once and assign episode numbers only where the M3U name did
        // not contain one.
        return grouped.map { (key, parsedEpisodes) ->
            val ordered = parsedEpisodes.sortedWith(
                compareBy({ it.season }, { it.episode }, { it.order })
            )
            val used = mutableMapOf<Int, Int>()
            val episodes = ArrayList<IptvEpisode>(ordered.size)
            for (parsed in ordered) {
                var number = parsed.episode
                if (number <= 0) {
                    number = (used[parsed.season] ?: 0) + 1
                    while (ordered.any { it.season == parsed.season && it.episode == number }) {
                        number++
                    }
                }
                used[parsed.season] = maxOf(used[parsed.season] ?: 0, number)
                episodes.add(
                    IptvEpisode(
                        item = parsed.item,
                        season = parsed.season,
                        episode = number,
                        title = parsed.episodeTitle
                    )
                )
            }
            val first = parsedEpisodes.first()
            IptvSeries(
                key = key,
                title = first.title,
                posterUrl = parsedEpisodes.firstNotNullOfOrNull {
                    it.item.logoUrl?.takeIf { url -> url.isNotBlank() }
                },
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

    private var cachedFingerprint: Int = 0
    private var cachedGroups: List<IptvSeries> = emptyList()

    /**
     * The Series tab is recreated every time the user switches tabs. Cache the
     * result for the same channel list so returning to Series is instant.
     */
    @Synchronized
    fun groupCached(items: List<PlaylistItem>): List<IptvSeries> {
        val fingerprint = items.hashCode()
        if (fingerprint == cachedFingerprint && cachedGroups.isNotEmpty()) {
            return cachedGroups
        }
        val result = group(items)
        cachedFingerprint = fingerprint
        cachedGroups = result
        return result
    }

    private fun parse(item: PlaylistItem, order: Int): ParsedEpisode {
        val name = item.name.trim()
        var season = 1
        var episode = 0
        var markerStart = -1
        var markerEnd = -1
        var rest = ""

        val marker = findMarker(name, 0)
        if (marker != null) {
            season = marker.season
            episode = marker.episode
            markerStart = marker.start
            markerEnd = marker.end
            rest = name.substring(markerEnd).trim()

            // Names such as "Temporada 2 - Episodio 5" have the episode
            // marker after the season marker; do one extra lookup only in
            // that case.
            if (episode <= 0) {
                findMarker(rest, 0)?.takeIf { it.episode > 0 }?.let {
                    episode = it.episode
                }
            }
        }

        val base = if (markerStart > 0) {
            name.substring(0, markerStart).trim().trimEnd('-', '–', '|', ':', '.', ' ')
        } else {
            name.trim()
        }
        val restClean = rest.trim().trimStart('-', '–', '|', ':', '.', ' ')
        val title = if (markerStart <= 0 || base.isBlank()) {
            restClean.ifBlank { name.trim() }
        } else {
            base.ifBlank { name.trim() }
        }.trim()

        val episodeTitle = if (restClean.isNotBlank() &&
            !restClean.equals(title, ignoreCase = true) &&
            !restClean.matches(Regex("""\d{1,3}"""))
        ) {
            restClean
        } else if (episode > 0) {
            "Episodio $episode"
        } else {
            title
        }

        return ParsedEpisode(
            item = item,
            title = title,
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            order = order
        )
    }

    private fun findMarker(text: String, from: Int): Marker? {
        var i = from
        while (i < text.length) {
            val c = text[i]
            if (!isBoundary(text, i)) {
                i++
                continue
            }

            if (c == 'S' || c == 's') {
                val season = readDigits(text, i + 1, 2)
                if (season != null) {
                    var end = season.end
                    var j = end
                    while (j < text.length && (text[j] == ' ' || text[j] == '-' ||
                            text[j] == '_' || text[j] == '.')) j++
                    var episode = 0
                    if (j < text.length && (text[j] == 'E' || text[j] == 'e')) {
                        val ep = readDigits(text, j + 1, 3)
                        if (ep != null) {
                            episode = ep.value
                            end = ep.end
                        }
                    }
                    return Marker(i, end, season.value, episode)
                }
            }

            if (c == 'T' || c == 't') {
                var j = i + 1
                if (text.startsWithAt(i, "temporada")) {
                    j = i + 9
                }
                while (j < text.length && text[j] == ' ') j++
                val season = readDigits(text, j, 2)
                if (season != null) {
                    return Marker(i, season.end, season.value, 0)
                }
            }

            if (c.isDigit()) {
                val season = readDigits(text, i, 2)
                if (season != null) {
                    var j = season.end
                    while (j < text.length && text[j] == ' ') j++
                    if (j < text.length && (text[j] == 'x' || text[j] == 'X')) {
                        j++
                        while (j < text.length && text[j] == ' ') j++
                        val ep = readDigits(text, j, 3)
                        if (ep != null) {
                            return Marker(i, ep.end, season.value, ep.value)
                        }
                    }
                }
            }

            if (c == 'E' || c == 'e') {
                val ep = readDigits(text, i + 1, 3)
                if (ep != null) return Marker(i, ep.end, 1, ep.value)
            }

            if (text.startsWithAt(i, "episodio") ||
                text.startsWithAt(i, "capitulo") ||
                text.startsWithAt(i, "capítulo") ||
                text.startsWithAt(i, "cap.") ||
                text.startsWithAt(i, "ep.")
            ) {
                var j = i
                while (j < text.length && (text[j].isLetter() || text[j] == '.')) j++
                while (j < text.length && text[j] == ' ') j++
                val ep = readDigits(text, j, 3)
                if (ep != null) return Marker(i, ep.end, 1, ep.value)
            }

            i++
        }
        return null
    }

    private fun readDigits(text: String, start: Int, maxDigits: Int): Digits? {
        var j = start
        var value = 0
        var count = 0
        while (j < text.length && text[j].isDigit() && count < maxDigits) {
            value = value * 10 + (text[j] - '0')
            j++
            count++
        }
        return if (count == 0) null else Digits(value, j)
    }

    private fun isBoundary(text: String, index: Int): Boolean {
        if (index <= 0) return true
        val previous = text[index - 1]
        return !previous.isLetterOrDigit()
    }

    private fun String.startsWithAt(index: Int, value: String): Boolean =
        regionMatches(index, value, 0, value.length, ignoreCase = true)

    private data class Digits(val value: Int, val end: Int)

    private fun normalize(value: String): String {
        val out = StringBuilder(value.length)
        var lastSpace = true
        for (ch in value) {
            val lower = ch.lowercaseChar()
            if (lower.isLetterOrDigit()) {
                out.append(lower)
                lastSpace = false
            } else if (!lastSpace) {
                out.append(' ')
                lastSpace = true
            }
        }
        return out.toString().trim()
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
