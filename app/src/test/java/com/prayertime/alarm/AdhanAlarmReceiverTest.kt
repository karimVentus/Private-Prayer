package com.prayertime.alarm

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.prayertime.domain.model.Prayer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdhanAlarmReceiverTest {
    @Test
    fun `onReceive does not throw for valid prayer extra`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent =
            Intent(context, AdhanAlarmReceiver::class.java).apply {
                action = AdhanAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(AdhanAlarmReceiver.EXTRA_PRAYER, Prayer.DHUHR.name)
            }
        AdhanAlarmReceiver().onReceive(context, intent)
    }

    @Test
    fun `onReceive ignores invalid prayer extra`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent =
            Intent(context, AdhanAlarmReceiver::class.java).apply {
                action = AdhanAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(AdhanAlarmReceiver.EXTRA_PRAYER, "not-a-prayer")
            }
        AdhanAlarmReceiver().onReceive(context, intent)
    }
}
