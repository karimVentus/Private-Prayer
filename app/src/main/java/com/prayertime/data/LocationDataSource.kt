package com.prayertime.data

import android.content.Context
import android.util.Log
import com.prayertime.domain.model.CityResolutionResult
import com.prayertime.locale.TextNormalizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

object LocationDataSource {
    private const val TAG = "LocationDataSource"
    private val awaitReadyTimeout = 30.seconds

    enum class CatalogLoadState {
        NOT_STARTED,
        LOADING,
        READY,
        FAILED,
    }

    private val lock = Any()
    private var loadState = CatalogLoadState.NOT_STARTED
    private var catalog: LocationCatalog? = null
    private var ready = CompletableDeferred<Unit>()
    private var loadGeneration = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Kicks off async JSON parsing on [Dispatchers.IO]. Returns immediately. */
    fun initialize(context: Context) {
        val generation =
            synchronized(lock) {
                if (loadState != CatalogLoadState.NOT_STARTED) return
                loadState = CatalogLoadState.LOADING
                loadGeneration++
                loadGeneration
            }
        startAsyncLoad(generation) { LocationCatalogLoader.loadFromAssets(context.assets) }
    }

    private fun startAsyncLoad(
        generation: Int,
        loadAction: () -> LocationCatalog,
    ) {
        scope.launch {
            val loaded =
                try {
                    loadAction()
                } catch (e: Exception) {
                    logLoadFailure(e)
                    null
                }
            synchronized(lock) {
                if (generation != loadGeneration) return@synchronized
                if (loaded != null) {
                    catalog = loaded
                    loadState = CatalogLoadState.READY
                } else if (loadState == CatalogLoadState.LOADING) {
                    catalog = null
                    loadState = CatalogLoadState.FAILED
                }
                if (!ready.isCompleted) {
                    ready.complete(Unit)
                }
            }
        }
    }

    private fun logLoadFailure(error: Throwable) {
        try {
            Log.e(TAG, "Failed to load location catalog", error)
        } catch (_: RuntimeException) {
            // android.util.Log is not mocked in plain JVM unit tests.
        }
    }

    /** Test hook: async load failure must still unblock [awaitReady]. */
    internal fun simulateFailedLoadForTests() {
        val generation =
            synchronized(lock) {
                if (loadState != CatalogLoadState.NOT_STARTED) return
                loadState = CatalogLoadState.LOADING
                loadGeneration++
                loadGeneration
            }
        startAsyncLoad(generation) { error("simulated asset load failure") }
    }

    internal fun initializeForTests(catalog: LocationCatalog) {
        synchronized(lock) {
            this.catalog = catalog
            loadState = CatalogLoadState.READY
            if (!ready.isCompleted) ready.complete(Unit)
        }
    }

    internal fun resetForTests() {
        synchronized(lock) {
            loadGeneration++
            catalog = null
            loadState = CatalogLoadState.NOT_STARTED
            ready = CompletableDeferred()
        }
    }

    fun catalogLoadState(): CatalogLoadState = synchronized(lock) { loadState }

    /**
     * Suspend until loading finishes (success or failure).
     * Throws if [initialize] was never called, or if loading exceeds [awaitReadyTimeout].
     */
    suspend fun awaitReady() {
        val state =
            synchronized(lock) {
                if (loadState == CatalogLoadState.NOT_STARTED) {
                    error("LocationDataSource.initialize(context) must be called before awaitReady()")
                }
                loadState
            }
        if (state == CatalogLoadState.LOADING) {
            withTimeout(awaitReadyTimeout) {
                ready.await()
            }
        }
    }

    /** Loaded catalog snapshot; null while loading or after failure. */
    internal fun loadedCatalog(): LocationCatalog? = synchronized(lock) { catalog }

    fun arabicCityName(
        countryCode: String,
        englishName: String,
    ): String? = loadedCatalog()?.citiesArByCountry?.get(countryCode)?.get(englishName)

    fun resolveCanonicalCityName(
        countryCode: String,
        input: String,
    ): String {
        val cat = loadedCatalog() ?: error("LocationDataSource.initialize(context) must be called before use")
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return trimmed
        val cities = cat.citiesByCountry[countryCode] ?: return trimmed
        cities.firstOrNull { TextNormalizer.foldForLookup(it) == TextNormalizer.foldForLookup(trimmed) }
            ?.let { return it }
        val arMap = cat.citiesArByCountry[countryCode] ?: emptyMap()
        arMap.entries
            .firstOrNull { TextNormalizer.foldForLookup(it.value) == TextNormalizer.foldForLookup(trimmed) }
            ?.let { return it.key }
        return trimmed
    }

    fun resolveCityCoordinates(
        countryCode: String,
        cityName: String,
    ): CityResolutionResult {
        val cat = loadedCatalog() ?: error("LocationDataSource.initialize(context) must be called before use")
        val canonical = resolveCanonicalCityName(countryCode, cityName)
        val folded = TextNormalizer.foldForLookup(canonical)
        val coords =
            cat.knownCityCoords["${countryCode}_$canonical"]
                ?: cat.knownCityCoords.entries.firstOrNull { (k, _) ->
                    k.startsWith("${countryCode}_") &&
                        TextNormalizer.foldForLookup(k.removePrefix("${countryCode}_")) == folded
                }?.value
        return if (coords != null) {
            CityResolutionResult.Found(coords)
        } else {
            cat.countryDefaults[countryCode]?.let { CityResolutionResult.Fallback(it, canonical, countryCode) }
                ?: CityResolutionResult.InvalidCountry(countryCode)
        }
    }
}
