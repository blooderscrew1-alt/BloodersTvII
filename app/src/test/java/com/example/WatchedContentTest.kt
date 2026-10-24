package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import com.example.data.nsrplay.NsrMedia
import com.example.data.nsrplay.NsrMediaType
import com.example.data.nsrplay.watchKey
import com.example.data.repository.IptvRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WatchedContentTest {

    private fun media() = NsrMedia(
        id = "better-call-saul",
        slug = "better-call-saul",
        title = "Better Call Saul",
        poster = null,
        rating = null,
        type = NsrMediaType.SERIES,
        provider = "mock",
        url = null
    )

    @Test
    fun `marking NSR content persists a stable watched key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        repository.clearHistory()

        val item = media()
        repository.markNsrWatched(item)

        assertTrue(repository.isNsrWatched(item))
        assertTrue(repository.watchedNsr.value.contains("nsr:series:better-call-saul"))
        assertEquals("nsr:series:better-call-saul", item.watchKey())

        assertEquals(1, repository.nsrHistory.value.size)
        assertEquals("Better Call Saul", repository.nsrHistory.value.first().title)
        assertEquals("nsr:series:better-call-saul", repository.nsrHistory.value.first().watchKey)

        repository.clearHistory()
        assertFalse(repository.isNsrWatched(item))
        assertTrue(repository.watchedNsr.value.isEmpty())
        assertTrue(repository.nsrHistory.value.isEmpty())
    }

    @Test
    fun `playlist editor reclassifies hides and renames categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        val source = com.example.data.model.PlaylistSource(
            id = "editor-test",
            name = "Editor test",
            type = com.example.data.model.SourceType.M3U_TEXT
        )
        repository.addPlaylistItems(
            source,
            listOf(
                PlaylistItem(
                    id = "editor-item",
                    name = "Canal de prueba",
                    streamUrl = "https://example.com/stream.m3u8",
                    category = "Cine",
                    type = ItemType.LIVE,
                    sourceId = source.id
                )
            )
        )

        repository.setCategoryType(source.id, "Cine", ItemType.MOVIE)
        repository.setCategoryHidden(source.id, "Cine", true)
        repository.renameCategory(source.id, "Cine", "Películas")

        val item = repository.items.value.first { it.id == "editor-item" }
        assertEquals(ItemType.MOVIE, item.type)
        assertEquals(true, item.isHidden)
        assertEquals("Películas", item.category)

        repository.setCategoryType(source.id, "Películas", ItemType.ADULT)
        assertEquals(
            ItemType.ADULT,
            repository.items.value.first { it.id == "editor-item" }.type
        )

        val backup = org.json.JSONObject(
            com.example.data.backup.SettingsBackupManager.buildJson(context, repository)
        )
        val backupPlaylist = (0 until backup.getJSONArray("playlists").length())
            .map { backup.getJSONArray("playlists").getJSONObject(it) }
            .first { it.optString("name") == "Editor test" }
        val edit = backupPlaylist.getJSONArray("categoryEdits").getJSONObject(0)
        assertEquals("Cine", edit.optString("original"))
        assertEquals("Películas", edit.optString("name"))
        assertEquals("ADULT", edit.optString("type"))
        assertTrue(edit.optBoolean("hidden"))
    }

    @Test
    fun `iptv series episodes are grouped into one series`() {
        fun episode(name: String) = PlaylistItem(
            name = name,
            streamUrl = "https://example.com/$name.mp4",
            category = "Series",
            type = ItemType.SERIES,
            sourceId = "series-test"
        )

        val groups = com.example.data.iptv.IptvSeriesGrouping.group(
            listOf(
                episode("Breaking Bad S01E01"),
                episode("Breaking Bad S02E01"),
                episode("Breaking Bad S02E02"),
                episode("Breaking Bad S03 Breaking Bad"),
                episode("Breaking Bad S04 Breaking Bad")
            )
        )

        assertEquals(1, groups.size)
        assertEquals("Breaking Bad", groups.first().title)
        assertEquals(5, groups.first().episodes.size)
        assertEquals(listOf(1, 2, 3, 4), groups.first().seasons)
    }

    @Test
    fun `series history keeps only the latest episode per series`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        repository.clearHistory()

        fun episode(number: Int) = PlaylistItem(
            id = "history-episode-$number",
            name = "History Show S01E0$number",
            streamUrl = "https://example.com/history-$number.mp4",
            category = "Series",
            type = ItemType.SERIES,
            sourceId = "history-test"
        )

        repository.recordWatchHistory(episode(1), 1_000L, 2_000L)
        repository.recordWatchHistory(episode(2), 1_000L, 2_000L)

        assertEquals(
            1,
            repository.history.value.count {
                it.type == ItemType.SERIES && it.title.contains("History Show")
            }
        )
        assertTrue(repository.watchedEpisodeIds.value.contains("history-episode-1"))
        assertTrue(repository.watchedEpisodeIds.value.contains("history-episode-2"))
    }

    @Test
    fun `m3u detects common series episode formats`() {
        val parser = com.example.data.parser.M3uParser
        assertEquals(
            ItemType.SERIES,
            parser.detectType("Breaking Bad 1x01", "NETFLIX", "https://example.com/vod/1.mp4")
        )
        assertEquals(
            ItemType.SERIES,
            parser.detectType("Better Call Saul S01E02", "VOD", "https://example.com/2.mkv")
        )
        assertEquals(
            ItemType.SERIES,
            parser.detectType("Temporada 2 - Episodio 5", "Series", "https://example.com/3.mp4")
        )
        assertEquals(
            ItemType.MOVIE,
            parser.detectType("Breaking Bad: El Camino", "CINE", "https://example.com/movie.mp4")
        )
    }

    @Test
    fun `m3u keeps series that appear after twenty thousand entries`() {
        val content = buildString {
            repeat(20_001) { index ->
                append("#EXTINF:-1 group-title=\"Cine\",Película $index\n")
                append("https://example.com/movie/$index.mp4\n")
            }
            append("#EXTINF:-1 group-title=\"Series\",Breaking Bad 1x01\n")
            append("https://example.com/series/breaking-bad-1x01.mp4\n")
        }

        val items = com.example.data.parser.M3uParser.parseFiltered(content)

        assertEquals(20_002, items.size)
        assertEquals(ItemType.SERIES, items.last().type)
        assertEquals("Breaking Bad 1x01", items.last().name)
    }

    @Test
    fun `adult unlock lasts for the current session only`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        assertFalse(repository.adultUnlocked.value)

        repository.unlockAdultContent()
        assertTrue(repository.adultUnlocked.value)

        val freshSession = IptvRepository(context)
        assertFalse(freshSession.adultUnlocked.value)
    }

    @Test
    fun `last live TV channel is persisted`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        val channel = PlaylistItem(
            id = "live-42",
            name = "Canal 42",
            streamUrl = "https://example.com/live.m3u8",
            category = "Deportes",
            type = ItemType.LIVE
        )

        repository.rememberTvChannel(channel, "Deportes")

        assertEquals("live-42", repository.lastTvChannelId)
        assertEquals("Deportes", repository.lastTvCategory)
        val reloaded = IptvRepository(context)
        assertEquals("live-42", reloaded.lastTvChannelId)
        assertEquals("Deportes", reloaded.lastTvCategory)
    }

    @Test
    fun `search poster size is persisted`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        repository.setSearchPosterScale(0.8f)

        assertEquals(0.8f, repository.searchPosterScale.value, 0.001f)
        val reloaded = IptvRepository(context)
        assertEquals(0.8f, reloaded.searchPosterScale.value, 0.001f)
    }

    @Test
    fun `NSR history keeps the latest title once per key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)
        repository.clearHistory()

        val first = media().copy(title = "Better Call Saul")
        val second = media().copy(title = "Better Call Saul (2015)")
        repository.markNsrWatched(first)
        repository.markNsrWatched(second)

        assertEquals(1, repository.nsrHistory.value.size)
        assertEquals("Better Call Saul (2015)", repository.nsrHistory.value.first().title)
    }
}
