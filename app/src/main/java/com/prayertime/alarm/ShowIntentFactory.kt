package com.prayertime.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.prayertime.PendingIntentRequestCodes
import com.prayertime.ui.MainActivity

/** Shared factory for alarm-clock show intents pointing to [MainActivity]. Request codes: [PendingIntentRequestCodes]. */
internal object ShowIntentFactory {
    /** Creates a [PendingIntent] that opens [MainActivity] for the lock-screen alarm display. */
    fun create(
        context: Context,
        requestCode: Int,
    ): PendingIntent {
        val launch =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            requestCode,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
