package com.aphoneus.modes

import android.content.Context
import com.aphoneus.discovery.ClusterDiscovery
import com.aphoneus.discovery.GpuDiscovery
import com.aphoneus.model.Cluster
import com.aphoneus.model.CustomProfile
import com.aphoneus.model.GpuInfo
import com.aphoneus.model.PrimaryMode
import com.aphoneus.root.WriteResult
import com.aphoneus.state.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Orchestrates application of power modes and tracks active mode state.
 */
class ModeManager(
    private val context: Context,
    private val profileRepo: ProfileRepository
) {
    private val _activeMode = MutableStateFlow(PrimaryMode.BALANCED)
    val activeMode: StateFlow<PrimaryMode> = _activeMode.asStateFlow()

    private val _lastApplyResults = MutableStateFlow<Map<String, WriteResult>>(emptyMap())
    val lastApplyResults: StateFlow<Map<String, WriteResult>> = _lastApplyResults.asStateFlow()

    suspend fun applyMode(
        mode: PrimaryMode,
        customProfile: CustomProfile? = null,
        clusters: List<Cluster>,
        gpu: GpuInfo
    ): Map<String, WriteResult> = withContext(Dispatchers.IO) {
        val results = when (mode) {
            PrimaryMode.PERFORMANCE -> PerformanceModeStrategy.apply(clusters, gpu)
            PrimaryMode.BALANCED -> BalancedModeStrategy.apply(context, clusters, gpu)
            PrimaryMode.BATTERY_SAVER -> BatterySaverModeStrategy.apply(clusters, gpu)
            PrimaryMode.CUSTOM -> {
                if (customProfile != null) {
                    CustomModeStrategy.apply(customProfile, clusters, gpu)
                } else {
                    BalancedModeStrategy.apply(context, clusters, gpu)
                }
            }
        }

        _activeMode.value = mode
        _lastApplyResults.value = results
        profileRepo.setActiveMode(mode)
        results
    }

    suspend fun panicReset(clusters: List<Cluster>, gpu: GpuInfo): Map<String, WriteResult> {
        return applyMode(PrimaryMode.BALANCED, null, clusters, gpu)
    }
}
