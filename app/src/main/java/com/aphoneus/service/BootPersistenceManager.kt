package com.aphoneus.service

import com.aphoneus.model.Cluster
import com.aphoneus.model.PrimaryMode
import com.aphoneus.root.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates an idempotent POSIX sh script in /data/adb/service.d/aphoneus.sh.
 * Works uniformly across Magisk, KernelSU, and APatch.
 * Includes a self-disabling panic trap: if the previous boot crashed, it aborts.
 */
object BootPersistenceManager {

    private const val SCRIPT_PATH = "/data/adb/service.d/aphoneus.sh"
    private const val CRASH_SENTINEL = "/data/adb/aphoneus_crash_sentinel"

    suspend fun installBootScript(mode: PrimaryMode, clusters: List<Cluster>): Boolean = withContext(Dispatchers.IO) {
        val scriptContent = buildString {
            appendLine("#!/system/bin/sh")
            appendLine("# Aphoneus Idempotent Boot Persistence Service")
            appendLine("# Auto-generated. Works across Magisk, KernelSU, APatch.")
            appendLine("")
            appendLine("LOG=\"/data/local/tmp/aphoneus_boot.log\"")
            appendLine("exec > \"\$LOG\" 2>&1")
            appendLine("echo \"[+] Aphoneus boot script invoked: \$(date)\"")
            appendLine("")
            appendLine("# Crash Sentinel Watchdog: if sentinel exists, previous boot failed -> self-disable")
            appendLine("if [ -f \"$CRASH_SENTINEL\" ]; then")
            appendLine("  echo \"[!] Boot crash sentinel detected! Aborting to prevent bootloop.\"")
            appendLine("  exit 0")
            appendLine("fi")
            appendLine("")
            appendLine("# Set sentinel flag (cleared after successful boot stabilization)")
            appendLine("touch \"$CRASH_SENTINEL\"")
            appendLine("")
            appendLine("# Wait for Android framework to finish booting")
            appendLine("until [ \"\$(getprop sys.boot_completed)\" = \"1\" ]; do")
            appendLine("  sleep 2")
            appendLine("done")
            appendLine("echo \"[+] System boot completed. Applying mode: ${mode.name}\"")
            appendLine("")

            when (mode) {
                PrimaryMode.PERFORMANCE -> {
                    for (c in clusters) {
                        val highest = c.freqsKHz.lastOrNull() ?: c.maxFreqKHz
                        appendLine("echo $highest > /sys/devices/system/cpu/cpufreq/${c.policy}/scaling_max_freq 2>/dev/null")
                        appendLine("echo $highest > /sys/devices/system/cpu/cpufreq/${c.policy}/scaling_min_freq 2>/dev/null")
                        appendLine("echo performance > /sys/devices/system/cpu/cpufreq/${c.policy}/scaling_governor 2>/dev/null")
                    }
                }
                PrimaryMode.BATTERY_SAVER -> {
                    for (c in clusters) {
                        val lowest = c.freqsKHz.firstOrNull() ?: c.minFreqKHz
                        appendLine("echo $lowest > /sys/devices/system/cpu/cpufreq/${c.policy}/scaling_min_freq 2>/dev/null")
                        appendLine("echo $lowest > /sys/devices/system/cpu/cpufreq/${c.policy}/scaling_max_freq 2>/dev/null")
                        appendLine("echo powersave > /sys/devices/system/cpu/cpufreq/${c.policy}/scaling_governor 2>/dev/null")
                    }
                }
                PrimaryMode.BALANCED, PrimaryMode.CUSTOM -> {
                    // Balanced requires no boot overrides
                }
            }

            appendLine("")
            appendLine("# Wait 45 seconds of stable operation, then clear sentinel")
            appendLine("sleep 45")
            appendLine("rm -f \"$CRASH_SENTINEL\"")
            appendLine("echo \"[+] Boot stabilization confirmed. Sentinel cleared.\"")
        }

        val commands = listOf(
            "mkdir -p /data/adb/service.d",
            "cat << 'SH_EOF' > $SCRIPT_PATH\n$scriptContent\nSH_EOF",
            "chmod 755 $SCRIPT_PATH"
        )

        val res = ShellExecutor.readLine(commands.joinToString(" && ") + " && echo 'OK'")
        res == "OK"
    }

    suspend fun removeBootScript(): Boolean = withContext(Dispatchers.IO) {
        val res = ShellExecutor.readLine("rm -f $SCRIPT_PATH $CRASH_SENTINEL 2>/dev/null && echo 'OK'")
        res == "OK"
    }
}
