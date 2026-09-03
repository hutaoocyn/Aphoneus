package com.aphoneus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aphoneus.MainActivity
import com.aphoneus.R
import com.aphoneus.model.PrimaryMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service with type "specialUse" (Android 14-16 API 34-36).
 * Maintains user-selected CPU/GPU power profile and continuous thermal watchdog.
 */
class ModeForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        const val CHANNEL_ID = "aphoneus_system_guard"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PANIC_REVERT = "com.aphoneus.action.PANIC_REVERT"
        const val EXTRA_ACTIVE_MODE = "extra_active_mode"

        fun start(context: Context, mode: PrimaryMode) {
            val intent = Intent(context, ModeForegroundService::class.java).apply {
                putExtra(EXTRA_ACTIVE_MODE, mode.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ModeForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PANIC_REVERT) {
            // Trigger global panic revert
            val panicIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_PANIC_REVERT
            }
            startActivity(panicIntent)
            return START_NOT_STICKY
        }

        val modeName = intent?.getStringExtra(EXTRA_ACTIVE_MODE) ?: PrimaryMode.BALANCED.name
        val notification = buildNotification(modeName)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Defensive handling for Android 15/16 FGS timeouts
    fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(modeName: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val panicIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ModeForegroundService::class.java).apply { action = ACTION_PANIC_REVERT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aphoneus Guard: $modeName")
            .setContentText("Active power profile monitored. Thermal safety watchdog enabled.")
            .setSmallIcon(R.drawable.ic_tile_speed)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_tile_speed, "Panic Reset", panicIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
