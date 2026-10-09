package com.stepcounter.app.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts step counting after the phone reboots or the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if (a == Intent.ACTION_BOOT_COMPLETED || a == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SnapshotWorker.schedule(context)
            SnapshotWorker.runNow(context)
            if (SensorUtil.hasPermission(context) && SensorUtil.stepSensor(context) != null) {
                StepService.start(context)
            }
        }
    }
}
