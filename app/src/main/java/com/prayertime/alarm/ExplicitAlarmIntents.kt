package com.prayertime.alarm

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Builds explicit in-app intents for alarm [android.app.PendingIntent]s (CodeQL / CWE-927). */
internal object ExplicitAlarmIntents {
    fun broadcast(
        context: Context,
        receiver: Class<out BroadcastReceiver>,
        configure: Intent.() -> Unit = {},
    ): Intent =
        Intent(context, receiver).apply {
            setPackage(context.packageName)
            configure()
        }

    fun activity(
        context: Context,
        activity: Class<out Activity>,
        configure: Intent.() -> Unit = {},
    ): Intent =
        Intent(context, activity).apply {
            setPackage(context.packageName)
            configure()
        }
}
