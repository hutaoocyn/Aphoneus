package com.aphoneus.model

import kotlinx.serialization.Serializable

/**
 * A bit-for-bit snapshot of a sysfs/procfs node for safe reversible application.
 */
@Serializable
data class NodeSnapshot(
    val path: String,
    val value: String
)

@Serializable
data class SnapshotBundle(
    val timestamp: Long,
    val description: String,
    val snapshots: List<NodeSnapshot>
)
