package com.stepcounter.app.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stepcounter.app.R
import com.stepcounter.app.data.fmt
import com.stepcounter.app.stepApp
import com.stepcounter.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Low-power foreground service. It only keeps a listener on the hardware step counter with a
 * 60-second batching window, so the CPU is woken at most about once a minute while the screen is off.
 */
class StepService : Service(), SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorManager: SensorManager
    private var registered = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val started = runCatching {
            val n = buildNotification(null)
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIF_ID, n)
            }
        }.isSuccess

        val sensor = SensorUtil.stepSensor(this)
        if (!started || sensor == null || !SensorUtil.hasPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!registered) {
            registered = sensorManager.registerListener(
                this, sensor, SensorManager.SENSOR_DELAY_NORMAL, REPORT_LATENCY_US
            )
        }
        refreshNotification()
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values[0]
        val ts = event.timestamp
        scope.launch {
            stepApp.repository.record(raw, ts)
            refreshNotification()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun refreshNotification() {
        scope.launch {
            val steps = stepApp.repository.stepsToday()
            getSystemService(NotificationManager::class.java)
                .notify(NOTIF_ID, buildNotification(steps))
        }
    }

    private fun buildNotification(steps: Int?): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (steps == null) "Counting your steps" else "${steps.fmt()} steps today")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        if (registered) sensorManager.unregisterListener(this)
        registered = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "step_counting"
        private const val NOTIF_ID = 1001
        private const val REPORT_LATENCY_US = 60_000_000

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, StepService::class.java))
            }
        }
    }
}
