package com.aphoneus.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes commands using a single cached root shell instance off the main thread.
 * Guarantees zero per-read su spawning and atomic batched transactions.
 */
object ShellExecutor {

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching { Shell.getShell().isRoot }.getOrDefault(false)
    }

    /**
     * Executes a single write verified by an immediate read-back.
     */
    suspend fun writeVerified(path: String, value: String): WriteResult = withContext(Dispatchers.IO) {
        val r = Shell.cmd(
            "[ -e $path ] || { echo __MISSING__; exit 0; }",
            "chmod u+w $path 2>/dev/null",
            "echo '$value' > $path 2>/dev/null",
            "cat $path 2>/dev/null"
        ).exec()

        val out = r.out.lastOrNull()?.trim().orEmpty()
        when {
            r.out.any { it.contains("__MISSING__") } -> WriteResult.Failed("node absent: $path")
            out == value -> WriteResult.Ok
            out.isEmpty() -> WriteResult.Failed("unreadable after write: $path")
            else -> WriteResult.Clamped(requested = value, actual = out)
        }
    }

    /**
     * Executes a batch of writes in a single shell transaction via an sh heredoc.
     * Returns a map of path to WriteResult.
     */
    suspend fun writeBatchVerified(pairs: List<Pair<String, String>>): Map<String, WriteResult> = withContext(Dispatchers.IO) {
        if (pairs.isEmpty()) return@withContext emptyMap()

        val script = buildString {
            appendLine("sh << 'EOF'")
            for ((path, value) in pairs) {
                appendLine("echo '---BEGIN $path---'")
                appendLine("if [ ! -e \"$path\" ]; then")
                appendLine("  echo '__MISSING__'")
                appendLine("else")
                appendLine("  chmod u+w \"$path\" 2>/dev/null")
                appendLine("  echo '$value' > \"$path\" 2>/dev/null")
                appendLine("  cat \"$path\" 2>/dev/null")
                appendLine("fi")
                appendLine("echo '---END $path---'")
            }
            appendLine("EOF")
        }

        val result = Shell.cmd(script).exec()
        val resultsMap = mutableMapOf<String, WriteResult>()

        var currentPath: String? = null
        val currentLines = mutableListOf<String>()

        for (line in result.out) {
            val trimmed = line.trim()
            if (trimmed.startsWith("---BEGIN ") && trimmed.endsWith("---")) {
                currentPath = trimmed.removePrefix("---BEGIN ").removeSuffix("---")
                currentLines.clear()
            } else if (trimmed.startsWith("---END ") && trimmed.endsWith("---")) {
                val path = currentPath
                if (path != null) {
                    val requested = pairs.find { it.first == path }?.second ?: ""
                    val output = currentLines.lastOrNull()?.trim().orEmpty()
                    val writeRes = when {
                        currentLines.any { it.contains("__MISSING__") } -> WriteResult.Failed("node absent")
                        output == requested -> WriteResult.Ok
                        output.isEmpty() -> WriteResult.Failed("unreadable after write")
                        else -> WriteResult.Clamped(requested, output)
                    }
                    resultsMap[path] = writeRes
                }
                currentPath = null
                currentLines.clear()
            } else if (currentPath != null) {
                currentLines.add(trimmed)
            }
        }

        // Fill any missing keys with failure
        for ((p, _) in pairs) {
            if (!resultsMap.containsKey(p)) {
                resultsMap[p] = WriteResult.Failed("transaction interrupted")
            }
        }

        resultsMap
    }

    /**
     * Batched read of multiple sysfs paths in a single execution.
     */
    suspend fun readBatch(paths: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext emptyMap()

        val script = buildString {
            for (p in paths) {
                appendLine("echo '###$p###'")
                appendLine("cat \"$p\" 2>/dev/null || echo ''")
            }
        }

        val out = Shell.cmd(script).exec().out
        val resultMap = mutableMapOf<String, String>()

        var currentKey: String? = null
        val currentContent = StringBuilder()

        for (line in out) {
            if (line.startsWith("###") && line.endsWith("###")) {
                if (currentKey != null) {
                    resultMap[currentKey] = currentContent.toString().trim()
                }
                currentKey = line.removePrefix("###").removeSuffix("###")
                currentContent.clear()
            } else {
                if (currentContent.isNotEmpty()) currentContent.append("\n")
                currentContent.append(line)
            }
        }
        if (currentKey != null) {
            resultMap[currentKey] = currentContent.toString().trim()
        }

        resultMap
    }

    suspend fun readLine(path: String): String = withContext(Dispatchers.IO) {
        Shell.cmd("cat \"$path\" 2>/dev/null").exec().out.firstOrNull()?.trim().orEmpty()
    }
}
