package com.aphoneus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aphoneus.discovery.ClusterDiscovery
import com.aphoneus.discovery.GpuDiscovery
import com.aphoneus.model.PrimaryMode
import com.aphoneus.modes.ModeManager
import com.aphoneus.root.RootSession
import com.aphoneus.service.ModeForegroundService
import com.aphoneus.service.ThermalWatchdog
import com.aphoneus.state.ProfileRepository
import com.aphoneus.ui.MainNavHost
import com.aphoneus.ui.theme.AphoneusTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var profileRepo: ProfileRepository
    private lateinit var modeManager: ModeManager
    private lateinit var thermalWatchdog: ThermalWatchdog

    override fun onCreate(savedInstanceState: Bundle?) {
        // TargetSdk 36 Mandatory Edge-to-Edge window enforcement
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        profileRepo = ProfileRepository(applicationContext)
        modeManager = ModeManager(applicationContext, profileRepo)
        thermalWatchdog = ThermalWatchdog(modeManager, lifecycleScope)

        // Initialize root session off main thread
        lifecycleScope.launch {
            RootSession.initialize(applicationContext)
            val clusters = ClusterDiscovery.discoverClusters()
            val gpu = GpuDiscovery.discoverGpu()

            // Start hardware thermal watchdog
            thermalWatchdog.start(clusters, gpu)

            // Check if launched from Panic Revert notification action
            if (intent?.action == ModeForegroundService.ACTION_PANIC_REVERT) {
                modeManager.panicReset(clusters, gpu)
                ModeForegroundService.start(applicationContext, PrimaryMode.BALANCED)
            }
        }

        setContent {
            AphoneusTheme {
                MainNavHost(
                    context = applicationContext,
                    modeManager = modeManager,
                    profileRepo = profileRepo
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        thermalWatchdog.setScreenState(true)
    }

    override fun onPause() {
        super.onPause()
        thermalWatchdog.setScreenState(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        thermalWatchdog.stop()
    }
}
