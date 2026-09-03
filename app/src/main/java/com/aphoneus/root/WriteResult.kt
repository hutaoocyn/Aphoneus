package com.aphoneus.root

/**
 * Result of a verified write operation against a kernel sysfs/procfs node.
 * Every write MUST be verified by an immediate read-back to guarantee correctness.
 */
sealed interface WriteResult {
    /** The node was written and verified to hold the exact requested value. */
    data object Ok : WriteResult

    /** The write succeeded without error, but the kernel/vendor clamped the value. */
    data class Clamped(val requested: String, val actual: String) : WriteResult

    /** The write failed, timed out, or could not be read back. */
    data class Failed(val reason: String) : WriteResult
}
