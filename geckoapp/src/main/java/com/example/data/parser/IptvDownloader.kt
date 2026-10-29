package com.example.data.parser

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object IptvDownloader {

    private const val MAX_DOWNLOAD_BYTES = 80 * 1024 * 1024L // 80 MB safety limit
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 BloodersTv/2.0 VLC/3.0.18"

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        // Lenient SSL for IPTV providers that often have self-signed or expired certificates
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

    fun cleanUrl(rawUrl: String): String {
        var trimmed = rawUrl.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed = "http://$trimmed"
        }
        return trimmed
    }

    suspend fun downloadToFile(context: Context, rawUrl: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedUrl = cleanUrl(rawUrl)
            val httpUrl = sanitizedUrl.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("La URL ingresada no tiene un formato válido.")

            val request = Request.Builder()
                .url(httpUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "text/plain, text/html, audio/x-mpegurl, application/vnd.apple.mpegurl, application/octet-stream, */*")
                .build()

            val tempFile = File(context.cacheDir, "iptv_download_${System.currentTimeMillis()}.m3u")

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    throw IOException(
                        when (code) {
                            401 -> "Error 401: No autorizado. Verifica usuario y contraseña."
                            403 -> "Error 403: Acceso denegado o bloqueado por el servidor IPTV."
                            404 -> "Error 404: La lista no fue encontrada en el servidor."
                            500, 502, 503, 504 -> "Error $code: El servidor IPTV no está disponible en este momento."
                            else -> "Error de servidor HTTP $code (${response.message})"
                        }
                    )
                }

                val body = response.body
                    ?: throw IOException("La respuesta del servidor no contiene datos.")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                            if (totalBytes > MAX_DOWNLOAD_BYTES) {
                                tempFile.delete()
                                throw IOException("La lista excede el tamaño máximo permitido (80 MB).")
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                throw IOException("El archivo descargado está vacío.")
            }

            tempFile
        }
    }

    suspend fun saveTextToFile(context: Context, text: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File(context.cacheDir, "iptv_pasted_${System.currentTimeMillis()}.m3u")
            tempFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(text)
                writer.flush()
            }
            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                throw IOException("El contenido ingresado está vacío.")
            }
            tempFile
        }
    }
}
