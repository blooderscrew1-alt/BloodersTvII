package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.SettingsBackupManager
import com.example.data.model.BufferMode
import com.example.data.model.ThemeAccent
import com.example.data.repository.IptvRepository
import com.example.data.vimeus.VimeusHistoryItem
import com.example.data.vimeus.VimeusMedia
import com.example.data.vimeus.VimeusType
import com.example.data.vimeus.embedUrl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VimeusBackupTest {

    @Test
    fun `vimeus embed URLs preserve API view key and add episode params`() {
        val movie = VimeusMedia(
            id = "550",
            tmdbId = 550L,
            imdbId = "tt0137523",
            title = "Fight Club",
            poster = "/poster.jpg",
            type = VimeusType.MOVIE
        )
        assertEquals(
            "https://vimeus.com/e/movie?imdb=tt0137523&view_key=abc",
            movie.embedUrl("https://vimeus.com", "abc")
        )
        assertEquals(
            "https://vimeus.com/e/movie?imdb=tt0137523&se=1&ep=2&view_key=abc",
            movie.embedUrl("https://vimeus.com", "abc", season = 1, episode = 2)
        )

        val withApi = movie.copy(
            apiEmbedUrl = "https://vimeus.com/e/movie?tmdb=550&view_key=official"
        )
        assertEquals(
            "https://vimeus.com/e/movie?tmdb=550&view_key=official",
            withApi.embedUrl("https://vimeus.com", "ignored")
        )
        assertTrue(
            withApi.embedUrl("https://vimeus.com", "ignored", 2, 4)
                .endsWith("view_key=official&se=2&ep=4")
        )
    }

    @Test
    fun `vimeus history rebuilds a searchable media object`() {
        val item = VimeusHistoryItem(
            id = "tt3032476",
            type = VimeusType.SERIES,
            title = "Better Call Saul",
            posterUrl = "https://image.tmdb.org/t/p/original/poster.jpg",
            year = "2015"
        )
        val media = item.toMedia()
        assertEquals("tt3032476", media.imdbId)
        assertEquals(VimeusType.SERIES, media.type)
        assertEquals("vimeus:series:tt3032476", item.watchKey)
    }

    @Test
    fun `settings backup round trip applies preferences`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = IptvRepository(context)

        val json = """
            {
              "app": "BloodersTv",
              "schema": 1,
              "settings": {
                "themeAccent": "CYBER_CYAN",
                "bufferMode": "FAST_ZAPPING",
                "parentalPin": "1234",
                "lockedCategories": ["Adultos"],
                "favorites": ["abc"]
              },
              "playlists": []
            }
        """.trimIndent()

        val result = SettingsBackupManager.importJson(context, repository, json)
        assertEquals(0, result.playlistsAdded)
        assertEquals(ThemeAccent.CYBER_CYAN, repository.themeAccent.value)
        assertEquals(BufferMode.FAST_ZAPPING, repository.bufferMode.value)
        assertEquals("1234", repository.parentalPin.value)
        assertTrue(repository.lockedCategories.value.contains("Adultos"))
        assertTrue(repository.favorites.value.contains("abc"))

        val exported = SettingsBackupManager.buildJson(context, repository)
        assertTrue(exported.contains("\"schema\": 1"))
        assertTrue(exported.contains("\"themeAccent\": \"CYBER_CYAN\""))
    }
}
