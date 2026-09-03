package com.aphoneus

import android.app.Application
import com.topjohnwu.superuser.Shell

class AphoneusApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure libsu root shell flags at application start
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
    }
}
