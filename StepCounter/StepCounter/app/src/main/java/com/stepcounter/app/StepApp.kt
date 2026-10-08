package com.stepcounter.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.stepcounter.app.data.SettingsStore
import com.stepcounter.app.data.StepDatabase
import com.stepcounter.app.data.StepRepository
import com.stepcounter.app.sensor.SnapshotWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class StepApp : Application() {
    val settings by lazy { SettingsStore(this) }
    val database by lazy { Room.databaseBuilder(this, StepDatabase::class.java, "steps.db").build() }
    val repository by lazy { StepRepository(this, database, settings) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Safety net: a tiny periodic snapshot of the sensor, in case the OS kills the service.
        SnapshotWorker.schedule(this)
    }
}

val Context.stepApp: StepApp get() = applicationContext as StepApp
