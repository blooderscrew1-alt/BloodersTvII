package com.example.data.parser

import com.example.data.model.CategoryInfo
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import java.io.BufferedReader
import java.io.File
import java.io.Reader
import java.io.StringReader
import java.util.UUID

object M3uParser {

    // Regex to match attributes with double quotes, single quotes, or no quotes
    private val GROUP_TITLE_REGEX = Regex("""group-title\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\r\n]+))""", RegexOption.IGNORE_CASE)
    private val TVG_NAME_REGEX = Regex("""tvg-name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\r\n]+))""", RegexOption.IGNORE_CASE)
    private val TVG_LOGO_REGEX = Regex("""tvg-logo\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,\r\n]+))""", RegexOption.IGNORE_CASE)
    private val TVG_ID_REGEX = Regex("""tvg-id\s*=\s*(?:"([^"]*)"|'([^']*)'|([^,\r\n]+))""", RegexOption.IGNORE_CASE)
    private val TVG_CHNO_REGEX = Regex("""tvg-chno\s*=\s*(?:"(\d+)"|'(\d+)'|(\d+))""", RegexOption.IGNORE_CASE)
    private val SERIES_EPISODE_REGEX = Regex(
        """(?:\bs\d{1,2}\s*[-_. ]?\s*e\d{1,3}\b|\b\d{1,2}x\d{1,3}\b|\b(?:temporada|cap[ií]tulo|episodio)\s*\d+)""",
        RegexOption.IGNORE_CASE
    )

    private const val DEFAULT_MAX_CHANNELS = 120_000

    fun isAdultCategory(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("18+") ||
                lower.contains("adult") ||
                lower.contains("xxx") ||
                lower.contains("erotic") ||
                lower.contains("playboy") ||
                lower.contains("venus") ||
                lower.contains("hustler") ||
                lower.contains("penthouse")
    }

    private fun extractRegexValue(regex: Regex, line: String): String? {
        val match = regex.find(line) ?: return null
        return (match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]?.value)?.trim()
    }

    fun extractGroupTitle(line: String): String {
        val value = extractRegexValue(GROUP_TITLE_REGEX, line)
        return value?.ifBlank { "General" } ?: "General"
    }

    fun extractCategories(content: String): List<CategoryInfo> {
        return extractCategoriesFromReader(StringReader(content))
    }

    fun extractCategoriesFromFile(file: File): List<CategoryInfo> {
        return try {
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                extractCategoriesFromReader(reader)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            emptyList()
        }
    }

    fun extractCategoriesFromReader(reader: Reader): List<CategoryInfo> {
        val categoryCounts = mutableMapOf<String, Int>()
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader, 16384)
        var line: String?

        try {
            while (bufferedReader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                    val category = extractGroupTitle(trimmed)
                    categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return categoryCounts.map { (cat, count) ->
            CategoryInfo(
                name = cat,
                itemCount = count,
                isSelected = true,
                isLocked = isAdultCategory(cat)
            )
        }.sortedByDescending { it.itemCount }
    }

    fun parseFiltered(
        content: String,
        allowedCategories: Set<String>? = null,
        sourceId: String = "m3u",
        maxChannelsLimit: Int = DEFAULT_MAX_CHANNELS
    ): List<PlaylistItem> {
        return parseFilteredFromReader(StringReader(content), allowedCategories, sourceId, maxChannelsLimit)
    }

    fun parseFilteredFromFile(
        file: File,
        allowedCategories: Set<String>? = null,
        sourceId: String = "m3u",
        maxChannelsLimit: Int = DEFAULT_MAX_CHANNELS
    ): List<PlaylistItem> {
        return try {
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                parseFilteredFromReader(reader, allowedCategories, sourceId, maxChannelsLimit)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            emptyList()
        }
    }

    fun parseFilteredFromReader(
        reader: Reader,
        allowedCategories: Set<String>? = null,
        sourceId: String = "m3u",
        maxChannelsLimit: Int = DEFAULT_MAX_CHANNELS
    ): List<PlaylistItem> {
        val items = mutableListOf<PlaylistItem>()
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader, 16384)
        var currentLine: String?
        var pendingExtInf: String? = null
        var channelCounter = 1

        try {
            while (bufferedReader.readLine().also { currentLine = it } != null) {
                val line = currentLine?.trim() ?: continue
                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    pendingExtInf = line
                } else if (!line.startsWith("#") && pendingExtInf != null) {
                    val streamUrl = line
                    val extInf = pendingExtInf

                    val category = extractGroupTitle(extInf)

                    if (allowedCategories == null || allowedCategories.isEmpty() || allowedCategories.contains(category)) {
                        val tvgName = extractRegexValue(TVG_NAME_REGEX, extInf)
                        val tvgLogo = extractRegexValue(TVG_LOGO_REGEX, extInf)?.ifBlank { null }
                        val tvgId = extractRegexValue(TVG_ID_REGEX, extInf)?.ifBlank { null }
                        val chno = extractRegexValue(TVG_CHNO_REGEX, extInf)?.toIntOrNull() ?: channelCounter

                        val commaIndex = extInf.lastIndexOf(',')
                        val parsedName = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
                            extInf.substring(commaIndex + 1).trim()
                        } else {
                            tvgName ?: "Canal $channelCounter"
                        }

                        val name = if (parsedName.isNotBlank()) parsedName else (tvgName ?: "Canal $channelCounter")
                        val isLocked = isAdultCategory(category)
                        val type = detectType(name, category, streamUrl)

                        items.add(
                            PlaylistItem(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = tvgLogo,
                                category = category,
                                channelNumber = chno,
                                type = type,
                                groupTitle = category,
                                tvgId = tvgId,
                                isFavorite = false,
                                isLocked = isLocked,
                                sourceId = sourceId
                            )
                        )

                        if (items.size >= maxChannelsLimit) {
                            break
                        }
                    }

                    channelCounter++
                    pendingExtInf = null
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return items
    }

    fun detectType(name: String, category: String, url: String): ItemType {
        val lowerCat = category.lowercase()
        val lowerUrl = url.lowercase()

        return when {
            lowerCat.contains("series") || lowerCat.contains("serie") ||
                    lowerCat.contains("tv show") || lowerCat.contains("anime") ||
                    lowerCat.contains("novela") ||
                    SERIES_EPISODE_REGEX.containsMatchIn(name) ||
                    lowerUrl.contains("/series/") || lowerUrl.contains("/serie/") ->
                ItemType.SERIES

            lowerCat.contains("pelicula") || lowerCat.contains("películas") ||
                    lowerCat.contains("movie") || lowerCat.contains("cine") ||
                    lowerCat.contains("vod") || lowerUrl.contains("/movie/") ||
                    lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".avi") -> ItemType.MOVIE

            else -> ItemType.LIVE
        }
    }
}
