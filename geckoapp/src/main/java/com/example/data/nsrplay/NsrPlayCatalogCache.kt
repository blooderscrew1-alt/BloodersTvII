package com.example.data.nsrplay

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NsrCatalogUiState(
    val items: List<NsrMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val loadedAt: Long = 0L
)

/**
 * Small in-memory cache for the NSRPlay catalog.
 *
 * Movies and Series both need the same first page. Without this cache every tab
 * switch would hit the API again and the user would see a spinner while simply
 * navigating. The cache is intentionally memory-only (no mirroring on disk) and
 * expires after five minutes, matching the volatile nature of the provider index.
 */
object NsrPlayCatalogCache {

    private const val TTL_MS = 5 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val movieState = MutableStateFlow(NsrCatalogUiState())
    private val seriesState = MutableStateFlow(NsrCatalogUiState())

    private var movieFingerprint: String = ""
    private var seriesFingerprint: String = ""

    fun state(type: NsrMediaType): StateFlow<NsrCatalogUiState> =
        if (type == NsrMediaType.MOVIE) movieState.asStateFlow() else seriesState.asStateFlow()

    @Synchronized
    fun ensureLoaded(context: Context, type: NsrMediaType, force: Boolean = false) {
        val repository = NsrPlayRepository(context.applicationContext)
        val flow = if (type == NsrMediaType.MOVIE) movieState else seriesState
        val fingerprint = repository.configFingerprint()
        val cachedFingerprint = if (type == NsrMediaType.MOVIE) movieFingerprint else seriesFingerprint
        val current = flow.value

        val sameSource = fingerprint == cachedFingerprint
        val fresh = System.currentTimeMillis() - current.loadedAt < TTL_MS
        if (!force && sameSource && fresh && current.items.isNotEmpty()) return
        if (current.isLoading) return

        flow.value = current.copy(
            items = if (sameSource) current.items else emptyList(),
            isLoading = true,
            error = null
        )

        scope.launch {
            try {
                val result = repository.getCatalog(type = type, page = 1, genre = null)
                if (type == NsrMediaType.MOVIE) {
                    movieFingerprint = fingerprint
                } else {
                    seriesFingerprint = fingerprint
                }
                flow.value = NsrCatalogUiState(
                    items = result.items,
                    isLoading = false,
                    error = null,
                    page = result.page,
                    hasNextPage = result.hasNextPage,
                    loadedAt = System.currentTimeMillis()
                )
            } catch (e: NsrPlayException) {
                flow.value = flow.value.copy(isLoading = false, error = e.message)
            } catch (_: Throwable) {
                flow.value = flow.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar el catálogo NSRPlay."
                )
            }
        }
    }

    @Synchronized
    fun loadMore(context: Context, type: NsrMediaType) {
        val flow = if (type == NsrMediaType.MOVIE) movieState else seriesState
        val current = flow.value
        if (current.isLoading || !current.hasNextPage || current.items.isEmpty()) return

        val repository = NsrPlayRepository(context.applicationContext)
        flow.value = current.copy(isLoading = true, error = null)

        scope.launch {
            try {
                val result = repository.getCatalog(type = type, page = current.page + 1, genre = null)
                flow.value = flow.value.copy(
                    items = flow.value.items + result.items,
                    isLoading = false,
                    error = null,
                    page = result.page,
                    hasNextPage = result.hasNextPage,
                    loadedAt = System.currentTimeMillis()
                )
            } catch (e: NsrPlayException) {
                flow.value = flow.value.copy(isLoading = false, error = e.message)
            } catch (_: Throwable) {
                flow.value = flow.value.copy(
                    isLoading = false,
                    error = "No se pudieron cargar más títulos NSRPlay."
                )
            }
        }
    }

    @Synchronized
    fun invalidate() {
        movieFingerprint = ""
        seriesFingerprint = ""
        movieState.value = NsrCatalogUiState()
        seriesState.value = NsrCatalogUiState()
    }
}
