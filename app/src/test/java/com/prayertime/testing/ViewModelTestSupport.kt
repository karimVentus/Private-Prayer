package com.prayertime.testing

import androidx.lifecycle.ViewModel
import com.prayertime.ui.prayer.PrayerTimesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** Clears any prior main dispatcher, then installs [dispatcher] for ViewModel tests. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun installTestMainDispatcher(dispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    uninstallTestMainDispatcher()
    Dispatchers.setMain(dispatcher)
}

/** Clears the test main dispatcher when one is installed; no-op otherwise. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun uninstallTestMainDispatcher() {
    try {
        Dispatchers.resetMain()
    } catch (_: IllegalStateException) {
        // Main was not set — safe when tests mix runTest { } and manual setMain.
    }
}

/** Disables the 1s countdown loop for the duration of [block] (must wrap construction). */
internal inline fun <T> withCountdownTickerLoopDisabled(block: () -> T): T {
    PrayerTimesViewModel.countdownTickerLoopEnabledOverride = false
    return try {
        block()
    } finally {
        PrayerTimesViewModel.countdownTickerLoopEnabledOverride = null
    }
}

/** Invokes [ViewModel.onCleared] for unit tests without a lifecycle owner. */
internal fun ViewModel.clearViewModelForTest() {
    val method = ViewModel::class.java.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(this)
}
