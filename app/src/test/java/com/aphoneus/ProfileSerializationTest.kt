package com.aphoneus

import com.aphoneus.model.ClusterConfig
import com.aphoneus.model.CustomProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSerializationTest {

    @Test
    fun testCustomProfileJsonRoundTrip() {
        val original = CustomProfile(
            id = "profile_gaming_01",
            name = "Sustained Gaming",
            clusters = listOf(
                ClusterConfig("policy0", 1200000, 1800000, "schedutil"),
                ClusterConfig("policy4", 1500000, 2400000, "performance")
            ),
            gpuMinFreqKHz = 600000,
            gpuMaxFreqKHz = 900000,
            gpuGovernor = "msm-adreno-tz",
            uclampMin = 512,
            uclampMax = 1024
        )

        val jsonString = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<CustomProfile>(jsonString)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.clusters.size, deserialized.clusters.size)
        assertEquals(original.clusters[0].policy, deserialized.clusters[0].policy)
        assertEquals(original.clusters[0].maxFreqKHz, deserialized.clusters[0].maxFreqKHz)
        assertEquals(original.gpuGovernor, deserialized.gpuGovernor)
        assertEquals(original.uclampMin, deserialized.uclampMin)
    }
}
