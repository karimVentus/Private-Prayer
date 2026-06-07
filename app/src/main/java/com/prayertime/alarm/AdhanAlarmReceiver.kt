package com.prayertime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.prayertime.domain.model.Prayer
import com.prayertime.notification.AdhanAlertDeliverer
import com.prayertime.notification.AdhanSoundResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdhanAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var adhanAlertDeliverer: AdhanAlertDeliverer

    @Inject
    @com.prayertime.di.WidgetScope
    lateinit var alarmScope: CoroutineScope

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val prayerName =
            intent.takeIf { it.action == ACTION_PRAYER_ALARM }
                ?.getStringExtra(EXTRA_PRAYER)
        val prayer = prayerName?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        if (prayer == null) {
            return
        }
        val pendingResult = goAsync()
        alarmScope.launch {
            try {
                adhanAlertDeliverer.deliver(prayer)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.prayertime.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER = "extra_prayer"
        const val EXTRA_ADHAN_SOUND = "extra_adhan_sound"
        const val DEFAULT_ADHAN_SOUND = AdhanSoundResolver.DEFAULT_KEY
    }
}
