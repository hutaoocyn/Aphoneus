package com.aphoneus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aphoneus.discovery.ClusterDiscovery
import com.aphoneus.discovery.GpuDiscovery
import com.aphoneus.model.PrimaryMode
import com.aphoneus.modes.ModeManager
import com.aphoneus.root.RootSession
import com.aphoneus.state.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val profileRepo = ProfileRepository(context)
            val activeMode = profileRepo.activeModeFlow.first()

            // Initialize root session
            RootSession.initialize(context)

            if (activeMode != PrimaryMode.BALANCED) {
                val clusters = ClusterDiscovery.discoverClusters()
                val gpu = GpuDiscovery.discoverGpu()
                val modeManager = ModeManager(context, profileRepo)
                modeManager.applyMode(activeMode, null, clusters, gpu)
                ModeForegroundService.start(context, activeMode)
            }
        }
    }
}
