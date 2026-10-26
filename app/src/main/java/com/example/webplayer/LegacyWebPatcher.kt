package com.example.webplayer

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.example.data.network.HttpClients
import okhttp3.Request
import java.io.ByteArrayInputStream

/**
 * The TV box shipped with Chromium 74. NSRPlay/Vimeus deploy modern
 * optional-chaining syntax, which makes their whole inline script fail to
 * parse and leaves the loading screen forever. This small compatibility
 * patcher rewrites the few ES2020 operators those pages use before old
 * WebViews parse them.
 */
object LegacyWebPatcher {
    private val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > 24
    }

    private val optionalProperty = Regex(
        """([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*|\[[^\]]*\])*)\?\.([A-Za-z_$][\w$]*)"""
    )
    private val optionalParenthesized = Regex(
        """(\([^()]*\))\?\.([A-Za-z_$][\w$]*)"""
    )
    private val optionalCall = Regex(
        """([A-Za-z_$][\w$]*\([^()]*\))\?\.([A-Za-z_$][\w$]*)"""
    )
    private val optionalBracket = Regex(
        """([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*|\[[^\]]*\])*)\?\.\["""
    )
    private val nullish = Regex(
        """([A-Za-z_$][\w$.\[\]]*)\s*\?\?\s*([A-Za-z0-9_$."'\[\]]+)"""
    )

    fun shouldPatch(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (!request.method.equals("GET", ignoreCase = true)) return false
        val host = request.url.host.orEmpty().lowercase()
        // Only NSRPlay is transpiled: its page is simple enough for a safe
        // rewrite. Vimeus' bundled player must not be altered.
        val playerHost = host.endsWith("nsrplay.space")
        if (!playerHost) return false
        if (request.isForMainFrame) return true
        val path = request.url.encodedPath.orEmpty().lowercase()
        return path.endsWith(".js") || path.contains("player-assets")
    }

    fun responseFor(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        return try {
            val builder = Request.Builder().url(url).get()
            request.requestHeaders.forEach { (key, value) ->
                when (key.lowercase()) {
                    "cookie", "referer", "user-agent", "accept" -> {
                        try {
                            builder.header(key, value)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
            HttpClients.api.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.bytes() ?: return null
                if (raw.size > 8 * 1024 * 1024) return null
                val contentType = response.header("Content-Type").orEmpty()
                val isHtml = contentType.contains("text/html", ignoreCase = true)
                val isJs = contentType.contains("javascript", ignoreCase = true) ||
                    contentType.contains("ecmascript", ignoreCase = true)
                if (!isHtml && !isJs) return null

                val source = raw.toString(Charsets.UTF_8)
                if (!source.contains("?.") && !source.contains("??")) return null
                val patched = if (request.isForMainFrame) {
                    patch(source)
                } else {
                    cache[url] ?: patch(source).also { cache[url] = it }
                }
                val mime = if (isHtml) "text/html" else "application/javascript"
                val headers = response.headers.toMultimap()
                    .filterKeys { it.lowercase() !in setOf("content-encoding", "content-length") }
                    .mapValues { it.value.joinToString(", ") }
                WebResourceResponse(
                    mime,
                    "UTF-8",
                    200,
                    "OK",
                    headers,
                    ByteArrayInputStream(patched.toByteArray(Charsets.UTF_8))
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun patch(source: String): String {
        var result = source
        // Four passes handle chained optional access (a?.b?.c, json?.data?.x).
        repeat(4) {
            result = optionalProperty.replace(result) { m ->
                val base = m.groupValues[1]
                "($base == null ? undefined : $base.${m.groupValues[2]})"
            }
            result = optionalParenthesized.replace(result) { m ->
                val base = m.groupValues[1]
                "($base == null ? undefined : $base.${m.groupValues[2]})"
            }
            result = optionalCall.replace(result) { m ->
                val base = m.groupValues[1]
                "($base == null ? undefined : $base.${m.groupValues[2]})"
            }
            result = optionalBracket.replace(result) { m ->
                val base = m.groupValues[1]
                "($base == null ? undefined : $base)["
            }
        }
        result = nullish.replace(result) { m ->
            val base = m.groupValues[1]
            "($base != null ? $base : ${m.groupValues[2]})"
        }
        return result
    }
}
