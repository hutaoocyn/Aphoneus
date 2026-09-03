package com.aphoneus

import com.aphoneus.modes.RangeOrderHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class RangeOrderingTest {

    @Test
    fun testWideningRangeOrdersMaxFirst() {
        // Scenario: Current max is 1,200 MHz. Target range is 600 MHz to 1,800 MHz.
        // Since target max (1800) >= curMax (1200), we MUST raise scaling_max_freq FIRST
        // to avoid transiently setting min > max (which causes kernel -EINVAL).
        val seq = RangeOrderHelper.determineWriteSequence(
            policy = "policy0",
            minKHz = 600000,
            maxKHz = 1800000,
            curMaxKHz = 1200000
        )

        assertEquals(2, seq.size)
        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", seq[0].first)
        assertEquals("1800000", seq[0].second)

        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", seq[1].first)
        assertEquals("600000", seq[1].second)
    }

    @Test
    fun testNarrowingRangeOrdersMinFirst() {
        // Scenario: Current max is 2,400 MHz. Target range is 400 MHz to 1,000 MHz.
        // Since target max (1000) < curMax (2400), we MUST lower scaling_min_freq FIRST
        // so that lowering max doesn't violate min <= max.
        val seq = RangeOrderHelper.determineWriteSequence(
            policy = "policy0",
            minKHz = 400000,
            maxKHz = 1000000,
            curMaxKHz = 2400000
        )

        assertEquals(2, seq.size)
        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", seq[0].first)
        assertEquals("400000", seq[0].second)

        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", seq[1].first)
        assertEquals("1000000", seq[1].second)
    }
}
