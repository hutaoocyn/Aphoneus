package com.aphoneus.discovery

import com.aphoneus.model.Cluster
import com.aphoneus.root.ShellExecutor
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ClusterDiscovery {

    suspend fun discoverClusters(fs: FileSystemManager? = null): List<Cluster> = withContext(Dispatchers.IO) {
        if (fs != null) {
            discoverViaFs(fs)
        } else {
            discoverViaShell()
        }
    }

    fun parseClusterFromStrings(
        policyName: String,
        relatedCpusStr: String,
        availFreqsStr: String,
        cpuinfoMinStr: String,
        cpuinfoMaxStr: String,
        governorsStr: String,
        scalingMinStr: String,
        scalingMaxStr: String,
        curGovStr: String,
        curFreqStr: String
    ): Cluster {
        val cpus = relatedCpusStr.split(Regex("\\s+"))
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty {
                // If related_cpus is missing, extract from policy number
                val id = policyName.removePrefix("policy").toIntOrNull() ?: 0
                listOf(id)
            }

        val freqs = availFreqsStr.split(Regex("\\s+"))
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty {
                listOfNotNull(
                    cpuinfoMinStr.toIntOrNull(),
                    cpuinfoMaxStr.toIntOrNull()
                )
            }.distinct().sorted()

        val governors = governorsStr.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("schedutil", "performance", "powersave") }

        val minFreq = scalingMinStr.toIntOrNull() ?: freqs.firstOrNull() ?: 0
        val maxFreq = scalingMaxStr.toIntOrNull() ?: freqs.lastOrNull() ?: 0
        val curGov = curGovStr.ifBlank { "schedutil" }
        val curFreq = curFreqStr.toIntOrNull() ?: minFreq

        return Cluster(
            policy = policyName,
            cpus = cpus,
            freqsKHz = freqs,
            governors = governors,
            minFreqKHz = minFreq,
            maxFreqKHz = maxFreq,
            currentGovernor = curGov,
            curFreqKHz = curFreq
        )
    }

    private fun discoverViaFs(fs: FileSystemManager): List<Cluster> {
        val cpufreqDir = fs.getFile("/sys/devices/system/cpu/cpufreq")
        if (!cpufreqDir.exists()) return emptyList()

        return cpufreqDir.listFiles().orEmpty()
            .filter { it.name.startsWith("policy") }
            .sortedBy { it.name.removePrefix("policy").toIntOrNull() ?: 0 }
            .map { dir ->
                fun read(n: String) = runCatching {
                    val f = fs.getFile(dir, n)
                    if (f.exists()) f.newInputStream().bufferedReader().use { it.readText().trim() } else ""
                }.getOrDefault("")

                parseClusterFromStrings(
                    policyName = dir.name,
                    relatedCpusStr = read("related_cpus").ifEmpty { read("affected_cpus") },
                    availFreqsStr = read("scaling_available_frequencies").ifEmpty { read("scaling_boost_frequencies") },
                    cpuinfoMinStr = read("cpuinfo_min_freq"),
                    cpuinfoMaxStr = read("cpuinfo_max_freq"),
                    governorsStr = read("scaling_available_governors"),
                    scalingMinStr = read("scaling_min_freq"),
                    scalingMaxStr = read("scaling_max_freq"),
                    curGovStr = read("scaling_governor"),
                    curFreqStr = read("scaling_cur_freq")
                )
            }
    }

    private suspend fun discoverViaShell(): List<Cluster> {
        val policiesOut = ShellExecutor.readLine("ls -d /sys/devices/system/cpu/cpufreq/policy* 2>/dev/null")
        if (policiesOut.isBlank()) return emptyList()

        val policyDirs = policiesOut.split(Regex("\\s+")).filter { it.isNotBlank() }
            .sortedBy { it.substringAfterLast("policy").toIntOrNull() ?: 0 }

        val pathsToRead = mutableListOf<String>()
        for (dir in policyDirs) {
            pathsToRead.add("$dir/related_cpus")
            pathsToRead.add("$dir/scaling_available_frequencies")
            pathsToRead.add("$dir/cpuinfo_min_freq")
            pathsToRead.add("$dir/cpuinfo_max_freq")
            pathsToRead.add("$dir/scaling_available_governors")
            pathsToRead.add("$dir/scaling_min_freq")
            pathsToRead.add("$dir/scaling_max_freq")
            pathsToRead.add("$dir/scaling_governor")
            pathsToRead.add("$dir/scaling_cur_freq")
        }

        val batchMap = ShellExecutor.readBatch(pathsToRead)

        return policyDirs.map { dir ->
            val policyName = dir.substringAfterLast("/")
            parseClusterFromStrings(
                policyName = policyName,
                relatedCpusStr = batchMap["$dir/related_cpus"].orEmpty(),
                availFreqsStr = batchMap["$dir/scaling_available_frequencies"].orEmpty(),
                cpuinfoMinStr = batchMap["$dir/cpuinfo_min_freq"].orEmpty(),
                cpuinfoMaxStr = batchMap["$dir/cpuinfo_max_freq"].orEmpty(),
                governorsStr = batchMap["$dir/scaling_available_governors"].orEmpty(),
                scalingMinStr = batchMap["$dir/scaling_min_freq"].orEmpty(),
                scalingMaxStr = batchMap["$dir/scaling_max_freq"].orEmpty(),
                curGovStr = batchMap["$dir/scaling_governor"].orEmpty(),
                curFreqStr = batchMap["$dir/scaling_cur_freq"].orEmpty()
            )
        }
    }
}
