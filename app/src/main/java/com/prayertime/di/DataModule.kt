package com.prayertime.di

import android.content.Context
import androidx.room.Room
import com.prayertime.data.local.AppDatabase
import com.prayertime.data.local.CityConfigDataSource
import com.prayertime.data.local.CityConfigSerializer
import com.prayertime.data.local.PrayerTimeMigrations
import com.prayertime.data.repository.LocalLocationRepository
import com.prayertime.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindCityConfigDataSource(impl: CityConfigSerializer): CityConfigDataSource

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocalLocationRepository): LocationRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
        ): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "prayer_times.db",
            )
                .addMigrations(PrayerTimeMigrations.MIGRATION_1_2, PrayerTimeMigrations.MIGRATION_2_3, PrayerTimeMigrations.MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
    }
}
