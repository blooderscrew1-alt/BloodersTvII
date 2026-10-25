package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.extensions.ExtensionEngine
import com.example.extensions.ExtensionNetworkFilter
import com.example.extensions.WebExtension
import com.example.extensions.ZipExtensionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExtensionEngineTest {

    private fun buildExtensionZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            put(
                "manifest.json",
                """
                {
                  "manifest_version": 3,
                  "name": "Test Ad Blocker",
                  "description": "Prueba",
                  "version": "1.0",
                  "content_scripts": [{
                    "matches": ["<all_urls>"],
                    "js": ["content.js"],
                    "css": ["style.css"]
                  }],
                  "declarative_net_request": {
                    "rule_resources": [{
                      "id": "ruleset_1",
                      "enabled": true,
                      "path": "rules.json"
                    }]
                  }
                }
                """.trimIndent()
            )
            put("content.js", "window.__testExtensionLoaded = true;")
            put("style.css", "body { outline: 0; }")
            put(
                "rules.json",
                """
                [
                  {"id":1,"action":{"type":"block"},"condition":{"urlFilter":"||ads.example.com^"}},
                  {"id":2,"action":{"type":"redirect","redirect":{"url":"https://example.com/clean"}},"condition":{"urlFilter":"||tracker.example.com^"}},
                  {"id":3,"action":{"type":"upgradeScheme"},"condition":{"urlFilter":"||insecure.example.com^"}}
                ]
                """.trimIndent()
            )
        }
        return output.toByteArray()
    }

    @Test
    fun `zip parser extracts scripts css and network rules`() {
        val result = ZipExtensionParser.parseZip(ByteArrayInputStream(buildExtensionZip()))
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()

        assertEquals("Test Ad Blocker", parsed.name)
        assertTrue(parsed.scriptContent.contains("__testExtensionLoaded"))
        assertTrue(parsed.scriptContent.contains("GM_addStyle"))
        assertTrue(parsed.rulesJson.contains("ads.example.com"))
        assertTrue(parsed.matchPatterns.contains("<all_urls>"))
    }

    @Test
    fun `all frame injection includes enabled script without url filtering`() {
        val extension = WebExtension(
            id = "iframe-test",
            name = "Iframe test",
            description = "Injects inside provider iframes",
            scriptContent = "window.__iframeExtensionLoaded = true;",
            matchPatterns = listOf("https://specific.example/*")
        )

        val script = ExtensionEngine.generateAllFramesScript(listOf(extension))

        assertTrue(script.contains("__iframeExtensionLoaded"))
        assertTrue(script.contains("__bloodersExtensionsInjected"))
    }

    @Test
    fun `zip parser accepts a file larger than the old four megabyte limit`() {
        val bigScript = "/*" + "x".repeat(5 * 1024 * 1024) + "*/window.__bigExtension = true;"
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put(
                "manifest.json",
                """{"manifest_version":3,"name":"Big Extension","version":"1.0","content_scripts":[{"matches":["<all_urls>"],"js":["big.js"]}]}"""
            )
            put("big.js", bigScript)
        }

        val result = ZipExtensionParser.parseZip(ByteArrayInputStream(output.toByteArray()))

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().scriptContent.length > 4 * 1024 * 1024)
    }

    @Test
    fun `crx3 package is parsed after its header`() {
        val zip = buildExtensionZip()
        val header = ByteArray(12 + 64)
        header[0] = 'C'.code.toByte()
        header[1] = 'r'.code.toByte()
        header[2] = '2'.code.toByte()
        header[3] = '4'.code.toByte()
        header[4] = 3
        header[8] = 64
        val crx = header + zip

        val result = ZipExtensionParser.parseZip(ByteArrayInputStream(crx))
        assertTrue(result.isSuccess)
        assertEquals("Test Ad Blocker", result.getOrThrow().name)
    }

    @Test
    fun `network filter blocks domains and redirects`() {
        val extension = WebExtension(
            name = "Test",
            description = "Test",
            scriptContent = "console.log('x')",
            rulesJson = """
                [
                  {"id":1,"action":{"type":"block"},"condition":{"urlFilter":"||ads.example.com^"}},
                  {"id":2,"action":{"type":"redirect","redirect":{"url":"https://example.com/clean"}},"condition":{"urlFilter":"||tracker.example.com^"}},
                  {"id":3,"action":{"type":"upgradeScheme"},"condition":{"urlFilter":"||insecure.example.com^"}}
                ]
            """.trimIndent()
        )
        val filter = ExtensionNetworkFilter(listOf(extension))

        assertTrue(filter.shouldBlock("https://ads.example.com/banner.js"))
        assertTrue(filter.shouldBlock("https://sub.ads.example.com/banner.js"))
        assertFalse(filter.shouldBlock("https://example.com/banner.js"))
        assertEquals("https://example.com/clean", filter.redirectFor("https://tracker.example.com/a"))
        assertTrue(filter.shouldUpgradeToHttps("http://insecure.example.com/a"))
        assertFalse(filter.shouldUpgradeToHttps("https://insecure.example.com/a"))
    }

    @Test
    fun `injection script includes matching extensions`() {
        val extension = WebExtension(
            name = "All pages",
            description = "Test",
            matchPatterns = listOf("<all_urls>"),
            scriptContent = "window.__allPages = true;"
        )
        val script = ExtensionEngine.generateInjectionScript(
            listOf(extension),
            "https://example.com/watch"
        )
        assertTrue(script.contains("__bloodersExtensionShimInjected"))
        assertTrue(script.contains("__allPages"))
    }

    @Test
    fun `built in extensions are available and persistent`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val extensions = ExtensionEngine.loadExtensions(context)
        assertTrue(extensions.any { it.id == "ext_ublock_video" })
        assertTrue(extensions.any { it.id == "ext_popup_redirect_shield" })
        assertTrue(extensions.any { it.id == "ext_ad_accelerator" && it.isEnabled })
        assertTrue(extensions.none { it.id == "ext_dark_reader" })
    }
}
