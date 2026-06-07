package com.prayertime.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.prayertime.data.local.AppPreferencesDataSource
import com.prayertime.worker.WidgetUpdateWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: AppPreferencesDataSource,
        private val loader: WidgetSnapshotLoader,
        private val viewsBuilder: WidgetRemoteViewsBuilder,
    ) {
        suspend fun updateAll() {
            val manager = AppWidgetManager.getInstance(context)
            val ids = allWidgetIds(manager)
            if (ids.isEmpty()) return

            val snapshot = loader.load()
            val density = context.resources.displayMetrics.densityDpi.toFloat()

            ids.forEach { id ->
                val size = widgetSizeFor(manager, id, density)
                val views = viewsBuilder.build(snapshot, size)
                manager.updateAppWidget(id, views)
            }
        }

        /** Sync theme chrome before async snapshot load — avoids wrong-theme flash on add. */
        fun applyThemeChrome(
            appWidgetIds: IntArray,
            manager: AppWidgetManager,
        ) {
            if (appWidgetIds.isEmpty()) return
            val theme = preferences.readAppThemeSync()
            val density = context.resources.displayMetrics.densityDpi.toFloat()
            appWidgetIds.forEach { id ->
                val size = widgetSizeFor(manager, id, density)
                val views = viewsBuilder.buildThemeChrome(size, theme)
                manager.updateAppWidget(id, views)
            }
        }

        fun requestImmediateUpdate() {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun allWidgetIds(manager: AppWidgetManager): IntArray {
            val ids = mutableSetOf<Int>()
            for (comp in providerComponents) {
                ids.addAll(manager.getAppWidgetIds(comp).toList())
            }
            return ids.toIntArray()
        }

        private fun widgetSizeFor(
            manager: AppWidgetManager,
            appWidgetId: Int,
            @Suppress("UNUSED_PARAMETER") density: Float,
        ): WidgetSize {
            val info = runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()
            if (info != null) {
                return when (info.provider.className) {
                    PrayerTimeWidgetProviderLarge::class.java.name -> WidgetSize.LARGE
                    else -> WidgetSize.MEDIUM
                }
            }
            return WidgetSize.MEDIUM
        }

        private val providerComponents by lazy {
            listOf(
                ComponentName(context, PrayerTimeWidgetProvider::class.java),
                ComponentName(context, PrayerTimeWidgetProviderLarge::class.java),
            )
        }

        internal companion object {
            const val IMMEDIATE_WORK_NAME = "widget_immediate_update"
        }
    }
