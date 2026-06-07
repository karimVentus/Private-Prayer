package com.prayertime.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.prayertime.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class PrayerTimeWidgetProvider : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val updater = widgetUpdater(context)
        updater.applyThemeChrome(appWidgetIds, appWidgetManager)
        val pendingResult = goAsync()
        val scope = coroutineScopeForUpdate(context)
        scope.launch {
            try {
                performUpdate(context)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        widgetUpdater(context).requestImmediateUpdate()
    }

    override fun onDisabled(context: Context) {
        WidgetPrayerBoundaryScheduler.cancel(context)
    }

    /** Shared update path; `internal open` lets Robolectric tests inject a fake [WidgetUpdater]. */
    internal open suspend fun performUpdate(context: Context) {
        widgetUpdater(context).updateAll()
    }

    internal open fun widgetUpdater(context: Context): WidgetUpdater = widgetEntryPoint(context).widgetUpdater()

    /** Robolectric tests override with [Dispatchers.Unconfined] — production uses IO-backed Hilt scope. */
    internal open fun coroutineScopeForUpdate(context: Context): CoroutineScope = widgetEntryPoint(context).widgetCoroutineScope()

    private fun widgetEntryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
}
