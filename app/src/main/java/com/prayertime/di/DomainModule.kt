package com.prayertime.di

import com.prayertime.domain.repository.LocationRepository
import com.prayertime.domain.usecase.SearchLocationsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideSearchLocationsUseCase(locationRepository: LocationRepository): SearchLocationsUseCase =
        SearchLocationsUseCase(locationRepository)
}
