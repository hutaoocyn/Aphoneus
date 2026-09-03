package com.aphoneus

import com.aphoneus.model.NodeSnapshot
import com.aphoneus.model.SnapshotBundle
import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotRestoreTest {

    @Test
    fun testSnapshotReverseOrdering() {
        val snapshots = listOf(
            NodeSnapshot("/sys/devices/system/cpu/cpufreq/policy0/scaling_governor", "schedutil"),
            NodeSnapshot("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", "1800000"),
            NodeSnapshot("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", "600000")
        )

        val bundle = SnapshotBundle(
            timestamp = 1000L,
            description = "Test Snapshot",
            snapshots = snapshots
        )

        // Restore unwinds writes in reverse order
        val reversed = bundle.snapshots.reversed()

        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", reversed[0].path)
        assertEquals("600000", reversed[0].value)

        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", reversed[1].path)
        assertEquals("1800000", reversed[1].value)

        assertEquals("/sys/devices/system/cpu/cpufreq/policy0/scaling_governor", reversed[2].path)
        assertEquals("schedutil", reversed[2].value)
    }
}
