package com.aphoneus.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.aphoneus.R
import com.aphoneus.discovery.ClusterDiscovery
import com.aphoneus.discovery.GpuDiscovery
import com.aphoneus.model.PrimaryMode
import com.aphoneus.modes.ModeManager
import com.aphoneus.state.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile for instant mode cycling:
 * Balanced -> Performance -> Battery Saver -> Balanced.
 */
class AphoneusTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var profileRepo: ProfileRepository
    private lateinit var modeManager: ModeManager

    override fun onCreate() {
        super.onCreate()
        profileRepo = ProfileRepository(applicationContext)
        modeManager = ModeManager(applicationContext, profileRepo)
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            val currentMode = profileRepo.activeModeFlow.first()
            updateTileState(currentMode)
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val currentMode = profileRepo.activeModeFlow.first()
            val nextMode = when (currentMode) {
                PrimaryMode.BALANCED -> PrimaryMode.PERFORMANCE
                PrimaryMode.PERFORMANCE -> PrimaryMode.BATTERY_SAVER
                PrimaryMode.BATTERY_SAVER, PrimaryMode.CUSTOM -> PrimaryMode.BALANCED
            }

            val clusters = ClusterDiscovery.discoverClusters()
            val gpu = GpuDiscovery.discoverGpu()

            modeManager.applyMode(nextMode, null, clusters, gpu)
            updateTileState(nextMode)
        }
    }

    private fun updateTileState(mode: PrimaryMode) {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Aphoneus"
            subtitle = mode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            icon = Icon.createWithResource(this@AphoneusTileService, R.drawable.ic_tile_speed)
            updateTile()
        }
    }
}
