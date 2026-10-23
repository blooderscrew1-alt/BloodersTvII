package com.example.data.nsrplay

import android.content.Context
import com.example.BuildConfig

/**
 * Runtime configuration for NSRPlay.
 *
 * The values compiled from .env are only the defaults. Everything can be changed
 * from Ajustes > NSRPlay and is stored on this device, so nobody has to edit the
 * source or rebuild the APK just to paste a key or a proxy URL.
 */
class NsrPlayConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun storedApiKey(): String? =
        prefs.getString(KEY_API_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun storedBaseUrl(): String? =
        prefs.getString(KEY_BASE_URL, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun apiKey(): String = storedApiKey() ?: BuildConfig.NSRPLAY_API_KEY

    fun baseUrl(): String = normalizeBaseUrl(storedBaseUrl() ?: BuildConfig.NSRPLAY_BASE_URL)

    fun hasCustomApiKey(): Boolean = storedApiKey() != null

    fun hasCustomBaseUrl(): Boolean = storedBaseUrl() != null

    fun save(apiKey: String?, baseUrl: String?) {
        val cleanKey = apiKey?.trim().orEmpty()
        val cleanBase = normalizeBaseUrl(baseUrl?.trim().orEmpty())
        val editor = prefs.edit()

        if (cleanKey.isNotEmpty()) {
            editor.putString(KEY_API_KEY, cleanKey)
        } else {
            editor.remove(KEY_API_KEY)
        }

        if (cleanBase.isNotEmpty() && cleanBase != normalizeBaseUrl(BuildConfig.NSRPLAY_BASE_URL)) {
            editor.putString(KEY_BASE_URL, cleanBase)
        } else {
            editor.remove(KEY_BASE_URL)
        }

        editor.apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    fun effectiveApiKey(custom: String?): String =
        custom?.trim()?.takeIf { it.isNotEmpty() } ?: BuildConfig.NSRPLAY_API_KEY

    fun effectiveBaseUrl(custom: String?): String =
        normalizeBaseUrl(custom?.trim()?.takeIf { it.isNotEmpty() } ?: BuildConfig.NSRPLAY_BASE_URL)

    fun isValidBaseUrl(url: String): Boolean {
        val normalized = normalizeBaseUrl(url)
        return normalized.startsWith("http://") || normalized.startsWith("https://")
    }

    companion object {
        private const val PREFS_NAME = "nsrplay_config"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"

        fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')
    }
}
