package com.prayertime.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.prayertime.R
import com.prayertime.testing.WidgetTestSupport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAppWidgetManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrayerTimeWidgetProviderTest {
    private lateinit var context: Context
    private lateinit var stack: WidgetTestSupport.Stack
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var shadowWidgets: ShadowAppWidgetManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        appWidgetManager = AppWidgetManager.getInstance(context)
        shadowWidgets = Shadows.shadowOf(appWidgetManager) as ShadowAppWidgetManager
        // Force full view inflate (apply) instead of reapply. WidgetRefreshWorkTest leaves
        // cached RemoteViews state in the shared ShadowAppWidgetManager; reapply() then
        // crashes with ArrayIndexOutOfBoundsException in the SparseArray tag cache.
        shadowWidgets.setAlwaysRecreateViewsDuringUpdate(true)
        stack = runBlocking { WidgetTestSupport.create(context) }
    }

    @After
    fun teardown() {
        shadowWidgets.setAlwaysRecreateViewsDuringUpdate(false)
        stack.close()
    }

    @Test
    fun performUpdate_pushesPrayerTimesToAppWidgetManager() =
        runBlocking {
            val widgetId = WidgetTestSupport.registerMediumWidget(context)
            val provider =
                object : PrayerTimeWidgetProvider() {
                    override fun widgetUpdater(context: Context) = stack.updater
                }

            provider.performUpdate(context)

            val view = shadowWidgets.getViewFor(widgetId)
            assertNotNull(view)
            val prayerLabel = view.findViewById<TextView>(R.id.widget_prayer_0)?.text?.toString().orEmpty()
            assertTrue(prayerLabel.isNotBlank())
        }

    @Test
    fun onUpdate_finishesAsyncRefreshToAppWidgetManager() =
        runBlocking {
            val widgetId = WidgetTestSupport.registerMediumWidget(context)
            val updateFinished = CompletableDeferred<Unit>()
            val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val provider =
                object : PrayerTimeWidgetProvider() {
                    override fun widgetUpdater(context: Context) = stack.updater

                    override fun coroutineScopeForUpdate(context: Context): CoroutineScope = testScope

                    override suspend fun performUpdate(context: Context) {
                        super.performUpdate(context)
                        updateFinished.complete(Unit)
                    }
                }

            provider.onUpdate(context, appWidgetManager, intArrayOf(widgetId))
            updateFinished.await()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            val view = shadowWidgets.getViewFor(widgetId)
            assertNotNull(view)
            val prayerLabel = view.findViewById<TextView>(R.id.widget_prayer_0)?.text?.toString().orEmpty()
            assertTrue(prayerLabel.isNotBlank())
        }
}
