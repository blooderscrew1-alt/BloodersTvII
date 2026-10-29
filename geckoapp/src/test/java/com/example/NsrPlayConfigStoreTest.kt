package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.nsrplay.NsrPlayConfigStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NsrPlayConfigStoreTest {

    @Test
    fun `saves custom key and base url then resets to build defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NsrPlayConfigStore(context)
        store.reset()

        assertFalse(store.hasCustomApiKey())

        store.save("np_custom_test", "http://10.0.2.2:8765/")

        assertEquals("np_custom_test", store.apiKey())
        assertEquals("http://10.0.2.2:8765", store.baseUrl())
        assertTrue(store.hasCustomApiKey())
        assertTrue(store.hasCustomBaseUrl())

        store.reset()

        assertEquals(BuildConfig.NSRPLAY_API_KEY, store.apiKey())
        assertEquals(BuildConfig.NSRPLAY_BASE_URL, store.baseUrl())
        assertFalse(store.hasCustomApiKey())
    }

    @Test
    fun `blank values fall back to the key compiled in the app`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NsrPlayConfigStore(context)
        store.reset()
        store.save("", "")

        assertEquals(BuildConfig.NSRPLAY_API_KEY, store.apiKey())
        assertEquals(BuildConfig.NSRPLAY_BASE_URL, store.baseUrl())
    }
}
