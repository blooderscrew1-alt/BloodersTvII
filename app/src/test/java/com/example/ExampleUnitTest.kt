package com.example

import com.example.data.parser.IptvDownloader
import com.example.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    private val sampleM3u = """
        #EXTM3U
        #EXTINF:-1 tvg-id="1" tvg-name="Cine Marvel" tvg-logo="http://logo.com/1.png" group-title="Cine 24/7",Cine Marvel
        http://stream.example.com/live/1.m3u8
        #EXTINF:-1 tvg-id="2" tvg-name="Deportes VIP" tvg-logo="http://logo.com/2.png" group-title="Deportes",Deportes VIP
        http://stream.example.com/live/2.m3u8
        #EXTINF:-1 tvg-id="3" tvg-name="Canal Basura 1" group-title="Basura",Canal Basura 1
        http://stream.example.com/live/3.m3u8
        #EXTINF:-1 tvg-id="4" tvg-name="Canal Adultos" group-title="Adultos 18+",Canal Adultos
        http://stream.example.com/live/4.m3u8
        #EXTINF:-1 group-title=SinComillas,Canal Sin Comillas
        http://stream.example.com/live/5.m3u8
        #EXTINF:-1 group-title='ComillasSimples',Canal Comillas Simples
        http://stream.example.com/live/6.m3u8
    """.trimIndent()

    @Test
    fun m3uParser_extractCategories_countsCorrectly() {
        val categories = M3uParser.extractCategories(sampleM3u)
        assertEquals(6, categories.size)
        val adultCat = categories.find { it.name == "Adultos 18+" }
        assertTrue(adultCat != null && adultCat.isLocked)
        assertTrue(categories.any { it.name == "SinComillas" })
        assertTrue(categories.any { it.name == "ComillasSimples" })
    }

    @Test
    fun m3uParser_categoryFiltering_filtersOutUnwantedJunk() {
        val allowed = setOf("Cine 24/7", "Deportes")
        val items = M3uParser.parseFiltered(sampleM3u, allowedCategories = allowed)

        assertEquals(2, items.size)
        assertTrue(items.any { it.name == "Cine Marvel" })
        assertTrue(items.any { it.name == "Deportes VIP" })
        assertFalse(items.any { it.name == "Canal Basura 1" })
        assertFalse(items.any { it.name == "Canal Adultos" })
    }

    @Test
    fun iptvDownloader_cleanUrl_handlesMissingProtocol() {
        assertEquals("http://servidor.com/lista.m3u8", IptvDownloader.cleanUrl("servidor.com/lista.m3u8"))
        assertEquals("https://servidor.com/lista.m3u8", IptvDownloader.cleanUrl("  https://servidor.com/lista.m3u8  "))
    }

    @Test
    fun m3uParser_streamingFromFile_handlesLargePlaylistsSafely() {
        val tempFile = File.createTempFile("large_test", ".m3u")
        try {
            tempFile.bufferedWriter().use { writer ->
                writer.write("#EXTM3U\n")
                for (i in 1..2500) {
                    val cat = if (i % 2 == 0) "Deportes" else "Cine"
                    writer.write("#EXTINF:-1 tvg-id=\"$i\" group-title=\"$cat\",Canal $i\n")
                    writer.write("http://stream.tv/ch$i.m3u8\n")
                }
            }

            val categories = M3uParser.extractCategoriesFromFile(tempFile)
            assertEquals(2, categories.size)

            val parsedItems = M3uParser.parseFilteredFromFile(tempFile, allowedCategories = setOf("Deportes"))
            assertEquals(1250, parsedItems.size)
            assertTrue(parsedItems.all { it.category == "Deportes" })
        } finally {
            tempFile.delete()
        }
    }
}
