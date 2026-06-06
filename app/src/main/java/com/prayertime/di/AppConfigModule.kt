package com.prayertime.di

import com.prayertime.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WidgetScope

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Named("networkModeAvailable")
    fun provideNetworkModeAvailable(): Boolean = BuildConfig.NETWORK_MODE_AVAILABLE

    @Provides
    @Singleton
    @WidgetScope
    fun provideWidgetCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
