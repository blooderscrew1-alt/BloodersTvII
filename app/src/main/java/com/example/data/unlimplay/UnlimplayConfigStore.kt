package com.example.data.unlimplay

import android.content.Context
import com.example.BuildConfig

/**
 * Unlimplay uses TMDB for search/catalog metadata and its own /f/embed pages
 * for playback. The TMDB key can be changed from Ajustes > Unlimplay.
 */
class UnlimplayConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun storedApiKey(): String? =
        prefs.getString(KEY_API_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun storedBaseUrl(): String? =
        prefs.getString(KEY_BASE_URL, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun apiKey(): String = storedApiKey() ?: BuildConfig.UNLIMPLAY_TMDB_API_KEY

    fun baseUrl(): String = normalizeBaseUrl(storedBaseUrl() ?: BuildConfig.UNLIMPLAY_BASE_URL)

    fun save(apiKey: String?, baseUrl: String?) {
        val cleanKey = apiKey?.trim().orEmpty()
        val cleanBase = normalizeBaseUrl(baseUrl?.trim().orEmpty())
        val editor = prefs.edit()
        if (cleanKey.isNotEmpty()) editor.putString(KEY_API_KEY, cleanKey) else editor.remove(KEY_API_KEY)
        if (cleanBase.isNotEmpty() && cleanBase != normalizeBaseUrl(BuildConfig.UNLIMPLAY_BASE_URL)) {
            editor.putString(KEY_BASE_URL, cleanBase)
        } else {
            editor.remove(KEY_BASE_URL)
        }
        editor.apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    fun isValidBaseUrl(url: String): Boolean {
        val normalized = normalizeBaseUrl(url)
        return normalized.startsWith("http://") || normalized.startsWith("https://")
    }

    companion object {
        private const val PREFS_NAME = "unlimplay_config"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"

        fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')
    }
}
