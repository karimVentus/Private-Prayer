package com.prayertime

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.di.LocationCatalogInitializer
import com.prayertime.domain.repository.LocationRepository
import com.prayertime.locale.AppLocale
import com.prayertime.worker.PrayerRefreshWork
import com.prayertime.worker.WidgetRefreshWork
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class PrayerTimeApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferences: AppPreferencesDataSource

    @Inject
    lateinit var locationCatalogInitializer: LocationCatalogInitializer

    @Inject
    lateinit var locationRepository: LocationRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val languageTag = preferences.resolveLanguageTagForStartupSync()
        AppLocale.apply(languageTag)
        PrayerRefreshWork.enqueue(this)
        WidgetRefreshWork.enqueue(this)
        appScope.launch(Dispatchers.IO) {
            val persisted = preferences.resolveLanguageTagForStartup()
            if (persisted != languageTag) {
                withContext(Dispatchers.Main) { AppLocale.apply(persisted) }
            }
            preferences.warmAppThemeCache()
            preferences.warmAppLanguageCache()
            runCatching { locationRepository.awaitReady() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
