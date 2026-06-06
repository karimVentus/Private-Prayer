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
        PrayerRefreshWork.enqueue(this)
        WidgetRefreshWork.enqueue(this)
        appScope.launch(Dispatchers.IO) {
            val tag =
                preferences.readAppLanguageTagOnce()
            AppLocale.apply(AppLocale.normalizeStoredTag(tag))
            preferences.warmAppThemeCache()
            runCatching { locationRepository.awaitReady() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
