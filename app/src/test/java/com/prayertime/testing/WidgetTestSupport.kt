package com.prayertime.testing

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.room.Room
import com.prayertime.R
import com.prayertime.data.LocationDataSourceTestSupport
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.data.local.InMemoryCityConfigDataSource
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.data.repository.LocalPrayerTimesRepository
import com.prayertime.domain.model.CityConfig
import com.prayertime.domain.model.SaveCityResult
import com.prayertime.widget.PrayerTimeWidgetProvider
import com.prayertime.widget.PrayerTimeWidgetProviderLarge
import com.prayertime.widget.WidgetPrayerBoundaryScheduler
import com.prayertime.widget.WidgetRemoteViewsBuilder
import com.prayertime.widget.WidgetSnapshotLoader
import com.prayertime.widget.WidgetUpdater
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAppWidgetManager
import java.util.concurrent.Executor

object WidgetTestSupport {
    data class Stack(
        val database: AppDatabase,
        val citySource: InMemoryCityConfigDataSource,
        val repository: LocalPrayerTimesRepository,
        val preferences: AppPreferencesDataSource,
        val loader: WidgetSnapshotLoader,
        val updater: WidgetUpdater,
        val boundaryScheduler: WidgetPrayerBoundaryScheduler,
    ) {
        fun close() {
            database.close()
        }
    }

    suspend fun create(
        context: Context,
        seedCity: Boolean = true,
    ): Stack {
        LocationDataSourceTestSupport.initializeFromTestResource()
        val citySource = InMemoryCityConfigDataSource()
        val syncExecutor = Executor { command -> command.run() }
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .setQueryExecutor(syncExecutor)
                .setTransactionExecutor(syncExecutor)
                .allowMainThreadQueries()
                .build()
        val repository = LocalPrayerTimesRepository(citySource, database)
        if (seedCity) {
            val saved =
                repository.saveCityConfig(
                    CityConfig("Hameln", "DE", "Europe/Berlin"),
                )
            check(saved is SaveCityResult.Success)
        }
        val preferences = AppPreferencesDataSource(context)
        val loader = WidgetSnapshotLoader(repository, preferences, LocalLocationRepository())
        val builder = WidgetRemoteViewsBuilder(context, preferences)
        val updater = WidgetUpdater(context, preferences, loader, builder)
        val boundaryScheduler = WidgetPrayerBoundaryScheduler(loader)
        return Stack(
            database = database,
            citySource = citySource,
            repository = repository,
            preferences = preferences,
            loader = loader,
            updater = updater,
            boundaryScheduler = boundaryScheduler,
        )
    }

    fun registerMediumWidget(context: Context): Int =
        registerWidget(context, PrayerTimeWidgetProvider::class.java, R.layout.widget_prayer_times_medium)

    fun registerLargeWidget(context: Context): Int =
        registerWidget(context, PrayerTimeWidgetProviderLarge::class.java, R.layout.widget_prayer_times_large)

    fun registerWidget(
        context: Context,
        providerClass: Class<out PrayerTimeWidgetProvider>,
        layoutRes: Int,
    ): Int {
        val manager = AppWidgetManager.getInstance(context)
        val shadow = Shadows.shadowOf(manager) as ShadowAppWidgetManager
        return shadow.createWidget(providerClass, layoutRes)
    }
}
