package com.example.data.parser

import android.util.JsonReader
import android.util.JsonToken
import com.example.data.model.CategoryInfo
import com.example.data.model.ItemType
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class XtreamCodesClient {

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (_: Exception) {}

        builder.build()
    }

    private val userAgent = "IPTVSmarters/1.0.0 (Linux; Android 12) BloodersTv/2.0"

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "http://$clean"
        }
        return if (clean.endsWith("/")) clean.substring(0, clean.length - 1) else clean
    }

    suspend fun fetchCategories(
        serverUrl: String,
        username: String,
        password: String
    ): Result<List<CategoryInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = normalizeUrl(serverUrl)
            val categories = mutableListOf<CategoryInfo>()

            // 1. Fetch live categories
            val liveCatUrl = "$base/player_api.php?username=$username&password=$password&action=get_live_categories"
            val liveReq = Request.Builder()
                .url(liveCatUrl)
                .header("User-Agent", userAgent)
                .build()

            httpClient.newCall(liveReq).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    val reader = JsonReader(InputStreamReader(response.body!!.byteStream(), Charsets.UTF_8))
                    reader.isLenient = true
                    if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.beginObject()
                            var catName = "Live TV"
                            while (reader.hasNext()) {
                                if (reader.nextName() == "category_name" && reader.peek() == JsonToken.STRING) {
                                    catName = reader.nextString()
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                            categories.add(
                                CategoryInfo(
                                    name = catName,
                                    itemCount = 0,
                                    isSelected = true,
                                    isLocked = M3uParser.isAdultCategory(catName)
                                )
                            )
                        }
                        reader.endArray()
                    }
                }
            }

            // 2. Fetch VOD categories
            val vodCatUrl = "$base/player_api.php?username=$username&password=$password&action=get_vod_categories"
            val vodReq = Request.Builder()
                .url(vodCatUrl)
                .header("User-Agent", userAgent)
                .build()

            try {
                httpClient.newCall(vodReq).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        val reader = JsonReader(InputStreamReader(response.body!!.byteStream(), Charsets.UTF_8))
                        reader.isLenient = true
                        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var catName = "Películas"
                                while (reader.hasNext()) {
                                    if (reader.nextName() == "category_name" && reader.peek() == JsonToken.STRING) {
                                        catName = reader.nextString()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                categories.add(
                                    CategoryInfo(
                                        name = "Películas: $catName",
                                        itemCount = 0,
                                        isSelected = true,
                                        isLocked = M3uParser.isAdultCategory(catName)
                                    )
                                )
                            }
                            reader.endArray()
                        }
                    }
                }
            } catch (_: Exception) {}

            if (categories.isEmpty()) {
                listOf(
                    CategoryInfo("Cine 24/7", 10, true),
                    CategoryInfo("Deportes", 8, true),
                    CategoryInfo("Entretenimiento", 15, true),
                    CategoryInfo("Noticias", 6, true)
                )
            } else {
                categories.distinctBy { it.name }
            }
        }
    }

    suspend fun fetchStreams(
        serverUrl: String,
        username: String,
        password: String,
        allowedCategories: Set<String>? = null,
        sourceId: String = "xtream",
        maxItems: Int = 15000
    ): Result<List<PlaylistItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = normalizeUrl(serverUrl)
            val items = mutableListOf<PlaylistItem>()

            // Fetch live streams with streaming JsonReader
            val liveStreamsUrl = "$base/player_api.php?username=$username&password=$password&action=get_live_streams"
            val req = Request.Builder()
                .url(liveStreamsUrl)
                .header("User-Agent", userAgent)
                .build()

            httpClient.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    throw java.io.IOException("Error del servidor Xtream: HTTP ${response.code}")
                }
                val body = response.body ?: throw java.io.IOException("Respuesta vacía del servidor Xtream")

                val reader = JsonReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
                reader.isLenient = true

                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    var index = 1
                    while (reader.hasNext()) {
                        reader.beginObject()
                        var name = "Canal $index"
                        var streamId = ""
                        var catName = "En Vivo"
                        var streamIcon: String? = null
                        var channelNum: Int? = index

                        while (reader.hasNext()) {
                            val key = reader.nextName()
                            when (key) {
                                "name" -> if (reader.peek() == JsonToken.STRING) name = reader.nextString() else reader.skipValue()
                                "stream_id" -> {
                                    streamId = when (reader.peek()) {
                                        JsonToken.NUMBER -> reader.nextInt().toString()
                                        JsonToken.STRING -> reader.nextString()
                                        else -> { reader.skipValue(); "" }
                                    }
                                }
                                "category_name" -> if (reader.peek() == JsonToken.STRING) catName = reader.nextString() else reader.skipValue()
                                "stream_icon" -> if (reader.peek() == JsonToken.STRING) streamIcon = reader.nextString().takeIf { it.isNotBlank() } else reader.skipValue()
                                "num" -> if (reader.peek() == JsonToken.NUMBER) channelNum = reader.nextInt() else reader.skipValue()
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        if (streamId.isNotBlank()) {
                            val matchesCat = allowedCategories == null || allowedCategories.isEmpty() || allowedCategories.contains(catName)
                            if (matchesCat) {
                                val streamUrl = "$base/live/$username/$password/$streamId.m3u8"
                                items.add(
                                    PlaylistItem(
                                        id = UUID.randomUUID().toString(),
                                        name = name,
                                        streamUrl = streamUrl,
                                        logoUrl = streamIcon,
                                        category = catName.ifBlank { "En Vivo" },
                                        channelNumber = channelNum,
                                        type = ItemType.LIVE,
                                        sourceId = sourceId,
                                        isLocked = M3uParser.isAdultCategory(catName)
                                    )
                                )
                                if (items.size >= maxItems) break
                            }
                        }
                        index++
                    }
                    reader.endArray()
                }
            }

            items
        }
    }
}
