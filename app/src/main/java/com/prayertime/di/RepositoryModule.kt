package com.prayertime.di

import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.CityConfigDataSource
import com.prayertime.data.remote.PrayerApi
import com.prayertime.data.repository.OnlinePrayerTimesRepository
import com.prayertime.data.repository.PrayerTimesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun providePrayerTimesRepository(
        cityConfigDataSource: CityConfigDataSource,
        database: AppDatabase,
        api: PrayerApi,
    ): PrayerTimesRepository = OnlinePrayerTimesRepository(cityConfigDataSource, database, api)
}
