package com.example.data.nativeplayer

import com.example.data.network.HttpClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Resolves the direct HLS URL for providers that expose it in their embed
 * page. This is used only on old WebViews where the provider's JavaScript
 * cannot run.
 */
object NativeHlsResolver {
    private const val UA =
        "Mozilla/5.0 (Linux; Android 9) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36"

    fun supports(url: String): Boolean {
        val host = runCatching { android.net.Uri.parse(url).host.orEmpty().lowercase() }
            .getOrDefault("")
        val path = runCatching { android.net.Uri.parse(url).path.orEmpty() }
            .getOrDefault("")
        return host.endsWith("tukiplay.com") && path.startsWith("/embed/") ||
            host.endsWith("vimeus.com")
    }

    suspend fun resolve(pageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val host = runCatching { android.net.Uri.parse(pageUrl).host.orEmpty().lowercase() }
                .getOrDefault("")
            if (host.endsWith("vimeus.com")) resolveVimeus(pageUrl) else resolveTuki(pageUrl)
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveTuki(pageUrl: String): String? {
        return try {
            val html = get(pageUrl, pageUrl) ?: return null
            val configMatch = Regex(
                """window\.PELIX_CONFIG\s*=\s*(\{.*?\});\s*</script>""",
                RegexOption.DOT_MATCHES_ALL
            ).find(html) ?: return null
            val config = JSONObject(configMatch.groupValues[1])
            val langs = config.optJSONObject("langs") ?: return null
            val keys = langs.keys()
            while (keys.hasNext()) {
                val lang = langs.optJSONObject(keys.next()) ?: continue
                val servers = lang.optJSONArray("servers") ?: continue
                for (index in 0 until servers.length()) {
                    val server = servers.optJSONObject(index) ?: continue
                    val play = server.optString("play").trim()
                    if (play.isBlank()) continue
                    val clean = get(play, pageUrl) ?: continue
                    val data = Regex("""data-config="([^"]+)"""")
                        .find(clean)?.groupValues?.getOrNull(1)
                        ?: Regex("""data-config='([^']+)'""")
                            .find(clean)?.groupValues?.getOrNull(1)
                        ?: continue
                    val decoded = android.text.Html.fromHtml(
                        data.replace("&quot;", "\"").replace("&amp;", "&"),
                        android.text.Html.FROM_HTML_MODE_LEGACY
                    ).toString()
                    val json = runCatching { JSONObject(decoded) }.getOrNull() ?: continue
                    val src = json.optString("src").trim()
                    if (src.startsWith("http")) return src
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveVimeus(pageUrl: String): String? {
        val html = get(pageUrl, pageUrl) ?: return null
        val data = Regex(
            """<script[^>]*type="text/json"[^>]*id="data"[^>]*>(.*?)</script>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.groupValues?.getOrNull(1) ?: return null
        val json = runCatching { JSONObject(data) }.getOrNull() ?: return null
        val embed = json.optJSONArray("embeds")?.optJSONObject(0)?.optString("url") ?: return null
        if (!embed.startsWith("http")) return null
        val raw = get(embed, pageUrl) ?: return null
        val unpacked = unpackPacker(raw) ?: raw
        return Regex("""https?[^"'\\\s]+\.m3u8[^"'\\\s]*""")
            .find(unpacked)?.value?.replace("\\/", "/")
    }

    private fun unpackPacker(source: String): String? {
        val match = Regex(
            """eval\(function\(p,a,c,k,e,d\).*?\}\('(.*?)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""",
            RegexOption.DOT_MATCHES_ALL
        ).find(source) ?: return null
        fun unescape(value: String): String = value
            .replace("\\'", "'")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\/", "/")
        var payload = unescape(match.groupValues[1])
        val radix = match.groupValues[2].toIntOrNull() ?: 62
        val count = match.groupValues[3].toIntOrNull() ?: return null
        val words = unescape(match.groupValues[4]).split("|")
        fun encode(value: Int): String {
            if (value < radix) {
                return if (value > 35) (value + 29).toChar().toString()
                else value.toString(36)
            }
            return encode(value / radix) +
                (value % radix).let { if (it > 35) (it + 29).toChar().toString() else it.toString(36) }
        }
        for (index in 0 until count) {
            val word = words.getOrNull(index) ?: continue
            if (word.isEmpty()) continue
            payload = Regex("\\b${Regex.escape(encode(index))}\\b")
                .replace(payload) { word }
        }
        return payload
    }

    private fun get(url: String, referer: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", referer)
            .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
            .get()
            .build()
        return HttpClients.api.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.bytes() ?: return null
            if (body.size > 4 * 1024 * 1024) return null
            body.toString(Charsets.UTF_8)
        }
    }
}

