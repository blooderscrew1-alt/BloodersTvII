package com.example.data.webepisodes

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared watched-episode state for the web providers. Keeping it in a single
 * object makes the VISTO badge update immediately after the player marks an
 * episode, including when auto-next advances to the following chapter.
 */
object WebEpisodeWatchStore {
    const val PROVIDER_NSR = "nsr"
    const val PROVIDER_VIMEUS = "vimeus"
    const val PROVIDER_UNLIMPLAY = "unlimplay"

    private const val PREFS_NAME = "blooders_tv_prefs"
    private const val KEY_WATCHED = "watched_web_episodes"

    private val _watched = MutableStateFlow<Set<String>>(emptySet())
    val watched: StateFlow<Set<String>> = _watched.asStateFlow()

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _watched.value = prefs.getStringSet(KEY_WATCHED, emptySet()) ?: emptySet()
            initialized = true
        }
    }

    fun key(
        provider: String,
        contentId: String,
        season: Int,
        episode: Int
    ): String = "$provider:$contentId:$season:$episode"

    fun isWatched(
        provider: String,
        contentId: String,
        season: Int,
        episode: Int
    ): Boolean = _watched.value.contains(key(provider, contentId, season, episode))

    fun mark(context: Context, key: String) {
        if (key.isBlank()) return
        init(context)
        synchronized(this) {
            if (_watched.value.contains(key)) return
            val updated = _watched.value + key
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_WATCHED, updated)
                .apply()
            _watched.value = updated
        }
    }

    fun clear(context: Context) {
        synchronized(this) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_WATCHED)
                .apply()
            _watched.value = emptySet()
            initialized = true
        }
    }
}
