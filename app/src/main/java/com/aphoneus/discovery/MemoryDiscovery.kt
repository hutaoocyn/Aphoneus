package com.aphoneus.discovery

import com.aphoneus.root.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MemoryDiscovery {

    suspend fun selectBestZramAlgorithm(): String = withContext(Dispatchers.IO) {
        val algos = ShellExecutor.readLine("cat /sys/block/zram0/comp_algorithm 2>/dev/null")
        if (algos.isBlank()) return@withContext "lz4"

        // Hierarchy preference: lz4 > zstd > lz4hc > lzo-rle > lzo > deflate
        val preference = listOf("lz4", "zstd", "lz4hc", "lzo-rle", "lzo", "deflate")
        for (pref in preference) {
            if (algos.contains(pref)) return@withContext pref
        }
        "lz4"
    }

    suspend fun getSwappiness(): Int = withContext(Dispatchers.IO) {
        ShellExecutor.readLine("cat /proc/sys/vm/swappiness 2>/dev/null").toIntOrNull() ?: 60
    }
}
