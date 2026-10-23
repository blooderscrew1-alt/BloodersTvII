package com.example.data.vimeus

import android.content.Context
import com.example.BuildConfig

/**
 * Runtime configuration for Vimeus.
 *
 * The keys compiled from .env are only defaults. They can be changed from
 * Ajustes > Vimeus so a user can move to another account without rebuilding.
 */
class VimeusConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun storedApiKey(): String? =
        prefs.getString(KEY_API_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun storedViewKey(): String? =
        prefs.getString(KEY_VIEW_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun storedBaseUrl(): String? =
        prefs.getString(KEY_BASE_URL, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun apiKey(): String = storedApiKey() ?: BuildConfig.VIMEUS_API_KEY

    fun viewKey(): String = storedViewKey() ?: BuildConfig.VIMEUS_VIEW_KEY

    fun baseUrl(): String = normalizeBaseUrl(storedBaseUrl() ?: BuildConfig.VIMEUS_BASE_URL)

    fun save(apiKey: String?, viewKey: String?, baseUrl: String?) {
        val cleanKey = apiKey?.trim().orEmpty()
        val cleanView = viewKey?.trim().orEmpty()
        val cleanBase = normalizeBaseUrl(baseUrl?.trim().orEmpty())
        val editor = prefs.edit()

        if (cleanKey.isNotEmpty()) editor.putString(KEY_API_KEY, cleanKey) else editor.remove(KEY_API_KEY)
        if (cleanView.isNotEmpty()) editor.putString(KEY_VIEW_KEY, cleanView) else editor.remove(KEY_VIEW_KEY)
        if (cleanBase.isNotEmpty() && cleanBase != normalizeBaseUrl(BuildConfig.VIMEUS_BASE_URL)) {
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
        private const val PREFS_NAME = "vimeus_config"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_VIEW_KEY = "view_key"
        private const val KEY_BASE_URL = "base_url"

        fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')
    }
}
