package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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

        repository.clearHistory()
        assertFalse(repository.isNsrWatched(item))
        assertTrue(repository.watchedNsr.value.isEmpty())
    }
}
