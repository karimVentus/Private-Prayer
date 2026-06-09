package com.prayertime.testing

import com.prayertime.ui.prayer.PrayerTimesViewModel
import org.junit.runner.Description
import org.junit.runner.notification.RunListener

/** Prevents the PrayerTimesViewModel 1s delay loop from stalling [runTest] on any JVM test. */
class DisableCountdownTickerRunListener : RunListener() {
    override fun testStarted(description: Description?) {
        PrayerTimesViewModel.countdownTickerLoopEnabledOverride = false
    }

    override fun testFinished(description: Description?) {
        PrayerTimesViewModel.countdownTickerLoopEnabledOverride = null
    }
}
