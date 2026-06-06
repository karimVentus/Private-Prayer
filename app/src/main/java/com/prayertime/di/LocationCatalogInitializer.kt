package com.prayertime.di

import android.content.Context
import com.prayertime.data.LocationDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Kicks off async location catalog loading at app startup (not from [LocalLocationRepository]). */
@Singleton
class LocationCatalogInitializer
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        init {
            LocationDataSource.initialize(context)
        }
    }
