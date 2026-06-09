package com.prayertime.di

import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.sensor.CompassSensor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CompassEntryPoint {
    fun compassSensor(): CompassSensor

    fun appPreferences(): AppPreferencesDataSource
}
