package com.aphoneus

import com.aphoneus.discovery.ClusterDiscovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterDiscoveryTest {

    @Test
    fun testParseStandardCluster() {
        val cluster = ClusterDiscovery.parseClusterFromStrings(
            policyName = "policy0",
            relatedCpusStr = "0 1 2 3",
            availFreqsStr = "300000 576000 748800 998400 1200000 1516800",
            cpuinfoMinStr = "300000",
            cpuinfoMaxStr = "1516800",
            governorsStr = "schedutil performance powersave",
            scalingMinStr = "576000",
            scalingMaxStr = "1200000",
            curGovStr = "schedutil",
            curFreqStr = "998400"
        )

        assertEquals("policy0", cluster.policy)
        assertEquals(listOf(0, 1, 2, 3), cluster.cpus)
        assertEquals(6, cluster.freqsKHz.size)
        assertEquals(300000, cluster.freqsKHz.first())
        assertEquals(1516800, cluster.freqsKHz.last())
        assertEquals(576000, cluster.minFreqKHz)
        assertEquals(1200000, cluster.maxFreqKHz)
        assertEquals("schedutil", cluster.currentGovernor)
        assertEquals(998400, cluster.curFreqKHz)
    }

    @Test
    fun testParseClusterMissingAvailableFrequenciesFallback() {
        // Many modern EAS / Pixel devices omit scaling_available_frequencies
        val cluster = ClusterDiscovery.parseClusterFromStrings(
            policyName = "policy4",
            relatedCpusStr = "4 5 6",
            availFreqsStr = "", // Empty available freqs
            cpuinfoMinStr = "800000",
            cpuinfoMaxStr = "2400000",
            governorsStr = "schedutil performance",
            scalingMinStr = "800000",
            scalingMaxStr = "2400000",
            curGovStr = "schedutil",
            curFreqStr = "1500000"
        )

        assertEquals("policy4", cluster.policy)
        assertEquals(listOf(4, 5, 6), cluster.cpus)
        // Should fall back to cpuinfo min & max
        assertEquals(listOf(800000, 2400000), cluster.freqsKHz)
        assertEquals(800000, cluster.minFreqKHz)
        assertEquals(2400000, cluster.maxFreqKHz)
    }

    @Test
    fun testParseTriClusterTopology() {
        val policies = listOf("policy0", "policy4", "policy7")
        val parsed = policies.map { pol ->
            ClusterDiscovery.parseClusterFromStrings(
                policyName = pol,
                relatedCpusStr = if (pol == "policy7") "7" else if (pol == "policy4") "4 5 6" else "0 1 2 3",
                availFreqsStr = "1000000 2000000 3000000",
                cpuinfoMinStr = "1000000",
                cpuinfoMaxStr = "3000000",
                governorsStr = "schedutil",
                scalingMinStr = "1000000",
                scalingMaxStr = "3000000",
                curGovStr = "schedutil",
                curFreqStr = "2000000"
            )
        }

        assertEquals(3, parsed.size)
        assertEquals(listOf(0, 1, 2, 3), parsed[0].cpus)
        assertEquals(listOf(4, 5, 6), parsed[1].cpus)
        assertEquals(listOf(7), parsed[2].cpus)
    }
}
