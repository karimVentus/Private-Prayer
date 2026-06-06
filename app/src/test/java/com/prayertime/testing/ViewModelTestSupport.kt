package com.prayertime.testing

import androidx.lifecycle.ViewModel

/** Invokes [ViewModel.onCleared] for unit tests without a lifecycle owner. */
internal fun ViewModel.clearViewModelForTest() {
    val method = ViewModel::class.java.getDeclaredMethod("onCleared")
    method.isAccessible = true
    method.invoke(this)
}
