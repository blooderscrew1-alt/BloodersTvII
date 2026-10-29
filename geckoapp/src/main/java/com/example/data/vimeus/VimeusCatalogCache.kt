package com.example.data.vimeus

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * In-memory, five-minute cache for the three Vimeus listings. Keeping it here
 * avoids a network request every time the user changes tabs.
 */
object VimeusCatalogCache {

    private const val TTL_MS = 5 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val states = mutableMapOf<VimeusType, MutableStateFlow<VimeusCatalogUiState>>()
    private val fingerprints = mutableMapOf<VimeusType, String>()

    private fun flowFor(type: VimeusType): MutableStateFlow<VimeusCatalogUiState> =
        states.getOrPut(type) { MutableStateFlow(VimeusCatalogUiState()) }

    fun state(type: VimeusType): StateFlow<VimeusCatalogUiState> = flowFor(type).asStateFlow()

    @Synchronized
    fun ensureLoaded(context: Context, type: VimeusType, force: Boolean = false) {
        val repository = VimeusRepository(context.applicationContext)
        val flow = flowFor(type)
        val fingerprint = repository.configFingerprint()
        val sameSource = fingerprint == fingerprints[type]
        val fresh = System.currentTimeMillis() - flow.value.loadedAt < TTL_MS
        if (!force && sameSource && fresh && flow.value.items.isNotEmpty()) return
        if (flow.value.isLoading) return

        flow.value = flow.value.copy(
            items = if (sameSource) flow.value.items else emptyList(),
            isLoading = true,
            error = null
        )

        scope.launch {
            try {
                val result = repository.getCatalog(type, 1)
                fingerprints[type] = fingerprint
                flow.value = VimeusCatalogUiState(
                    items = result.items,
                    page = result.page,
                    isLoading = false,
                    error = null,
                    hasNext = result.hasNext,
                    loadedAt = System.currentTimeMillis()
                )
            } catch (e: VimeusException) {
                flow.value = flow.value.copy(isLoading = false, error = e.message)
            } catch (_: Throwable) {
                flow.value = flow.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar el catálogo de Vimeus."
                )
            }
        }
    }

    @Synchronized
    fun loadMore(context: Context, type: VimeusType) {
        val flow = flowFor(type)
        val current = flow.value
        if (current.isLoading || !current.hasNext || current.items.isEmpty()) return
        val repository = VimeusRepository(context.applicationContext)
        flow.value = current.copy(isLoading = true, error = null)

        scope.launch {
            try {
                val result = repository.getCatalog(type, current.page + 1)
                flow.value = flow.value.copy(
                    items = flow.value.items + result.items,
                    isLoading = false,
                    error = null,
                    page = result.page,
                    hasNext = result.hasNext,
                    loadedAt = System.currentTimeMillis()
                )
            } catch (e: VimeusException) {
                flow.value = flow.value.copy(isLoading = false, error = e.message)
            } catch (_: Throwable) {
                flow.value = flow.value.copy(
                    isLoading = false,
                    error = "No se pudieron cargar más títulos de Vimeus."
                )
            }
        }
    }

    @Synchronized
    fun invalidate() {
        fingerprints.clear()
        states.values.forEach { it.value = VimeusCatalogUiState() }
    }
}
