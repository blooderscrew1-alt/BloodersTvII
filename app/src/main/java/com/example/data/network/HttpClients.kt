package com.example.data.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Shared clients tuned for slow TV boxes.
 *
 * - IPv4 addresses are tried first. Some Android TV boxes advertise broken
 *   IPv6 routes and otherwise wait for the OS timeout before falling back.
 * - Idempotent GET requests are retried once after a connection/timeout error.
 */
object HttpClients {

    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            return addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
        }
    }

    private fun baseBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                var attempt = 0
                while (true) {
                    try {
                        return@addInterceptor chain.proceed(request)
                    } catch (e: IOException) {
                        attempt++
                        if (attempt >= 2 || request.method != "GET") throw e
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                throw IOException("No se pudo completar la solicitud.")
            }

    val api: OkHttpClient by lazy {
        baseBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .build()
    }

    val suggestions: OkHttpClient by lazy {
        baseBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }
}
