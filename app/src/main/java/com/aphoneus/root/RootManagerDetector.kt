package com.aphoneus.root

import java.io.File

/**
 * Detects the active root manager: Magisk, KernelSU, APatch, or generic su.
 */
enum class RootManagerType {
    KERNEL_SU,
    MAGISK,
    APATCH,
    GENERIC_SU,
    UNKNOWN_NO_ROOT
}

data class RootEnvironment(
    val manager: RootManagerType,
    val version: String,
    val isSELinuxEnforcing: Boolean,
    val serviceDir: String
)

object RootManagerDetector {

    fun detect(): RootEnvironment {
        // 1. Check KernelSU
        val ksuVersion = readKsuVersion()
        if (ksuVersion != null) {
            return RootEnvironment(
                manager = RootManagerType.KERNEL_SU,
                version = "KernelSU (Kernel v$ksuVersion)",
                isSELinuxEnforcing = isSELinuxEnforcing(),
                serviceDir = "/data/adb/service.d"
            )
        }

        // 2. Check APatch
        if (File("/data/adb/ap").exists() || File("/data/adb/apd").exists()) {
            return RootEnvironment(
                manager = RootManagerType.APATCH,
                version = "APatch",
                isSELinuxEnforcing = isSELinuxEnforcing(),
                serviceDir = "/data/adb/service.d"
            )
        }

        // 3. Check Magisk
        val magiskVer = getSystemProperty("ro.magisk.version")
        if (magiskVer.isNotEmpty() || File("/sbin/.magisk").exists() || File("/data/adb/magisk").exists()) {
            return RootEnvironment(
                manager = RootManagerType.MAGISK,
                version = if (magiskVer.isNotEmpty()) "Magisk $magiskVer" else "Magisk (Universal)",
                isSELinuxEnforcing = isSELinuxEnforcing(),
                serviceDir = "/data/adb/service.d"
            )
        }

        // 4. Check generic su binaries
        val commonSuPaths = listOf("/system/bin/su", "/system/xbin/su", "/vendor/bin/su")
        if (commonSuPaths.any { File(it).exists() }) {
            return RootEnvironment(
                manager = RootManagerType.GENERIC_SU,
                version = "Generic Su",
                isSELinuxEnforcing = isSELinuxEnforcing(),
                serviceDir = "/data/adb/service.d"
            )
        }

        return RootEnvironment(
            manager = RootManagerType.UNKNOWN_NO_ROOT,
            version = "Unrooted / Unknown",
            isSELinuxEnforcing = isSELinuxEnforcing(),
            serviceDir = "/data/local/tmp"
        )
    }

    private fun readKsuVersion(): String? {
        val ksuFile = File("/sys/kernel/tracing/ksu")
        if (ksuFile.exists()) return "Active"
        val procKsu = File("/proc/ksu_version")
        if (procKsu.exists()) {
            return runCatching { procKsu.readText().trim() }.getOrNull()
        }
        return null
    }

    private fun isSELinuxEnforcing(): Boolean {
        val enforceFile = File("/sys/fs/selinux/enforce")
        if (enforceFile.exists()) {
            return runCatching { enforceFile.readText().trim() == "1" }.getOrDefault(true)
        }
        return true
    }

    private fun getSystemProperty(key: String): String {
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            process.inputStream.bufferedReader().readLine()?.trim().orEmpty()
        }.getOrDefault("")
    }
}
