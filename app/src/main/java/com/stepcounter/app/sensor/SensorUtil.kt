package com.stepcounter.app.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat

object SensorUtil {
    fun stepSensor(ctx: Context): Sensor? =
        (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** Number of times the phone has booted; changes after every restart. -1 if unavailable. */
    fun bootCount(ctx: Context): Int = try {
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT)
    } catch (e: Exception) {
        -1
    }

    /** Converts a sensor event timestamp (nanoseconds since boot) to wall-clock milliseconds. */
    fun wallTimeMs(eventTimestampNanos: Long): Long {
        val now = System.currentTimeMillis()
        val ageMs = (SystemClock.elapsedRealtimeNanos() - eventTimestampNanos) / 1_000_000L
        return if (ageMs in 0..172_800_000L) now - ageMs else now
    }
}
