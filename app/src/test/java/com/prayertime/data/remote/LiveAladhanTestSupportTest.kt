package com.prayertime.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveAladhanTestSupportTest {
    @Test
    fun isLiveHttpEnabled_defaults_false_when_unset() {
        assertFalse(LiveAladhanTestSupport.isLiveHttpEnabled(null))
    }

    @Test
    fun isLiveHttpEnabled_true_only_for_explicit_opt_in() {
        assertTrue(LiveAladhanTestSupport.isLiveHttpEnabled("1"))
        assertTrue(LiveAladhanTestSupport.isLiveHttpEnabled("true"))
        assertTrue(LiveAladhanTestSupport.isLiveHttpEnabled("TRUE"))
    }

    @Test
    fun isLiveHttpEnabled_false_for_disabled_and_unknown_values() {
        assertFalse(LiveAladhanTestSupport.isLiveHttpEnabled("0"))
        assertFalse(LiveAladhanTestSupport.isLiveHttpEnabled("false"))
        assertFalse(LiveAladhanTestSupport.isLiveHttpEnabled("yes"))
    }
}
