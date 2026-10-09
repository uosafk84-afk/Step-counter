package com.stepcounter.app.data

import android.content.Context
import android.os.SystemClock
import androidx.room.withTransaction
import com.stepcounter.app.sensor.SensorUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToLong

class StepRepository(
    private val context: Context,
    private val db: StepDatabase,
    private val settings: SettingsStore
) {
    private val dao = db.dao()
    private val mutex = Mutex()

    fun observeDay(date: LocalDate): Flow<DailySteps?> = dao.observeDay(date.toString())

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DailySteps>> =
        dao.observeRange(from.toString(), to.toString())

    fun observeFirstDate(): Flow<LocalDate?> =
        dao.observeFirstDate().map { it?.let { s -> LocalDate.parse(s) } }

    suspend fun stepsToday(): Int = dao.stepsOn(LocalDate.now().toString()) ?: 0

    suspend fun applyGoal(goal: Int) = dao.setGoal(LocalDate.now().toString(), goal)

    /**
     * Core step logic. The sensor value is cumulative since the phone booted, so we NEVER reset it
     * at midnight. We remember the last raw value and add only the difference (delta) to the day
     * the steps actually happened on (taken from the sensor event's own timestamp).
     */
    suspend fun record(rawValue: Float, eventTimestampNanos: Long) {
        mutex.withLock {
            val raw = rawValue.roundToLong()
            val nowElapsed = SystemClock.elapsedRealtime()
            val eventElapsed = eventTimestampNanos / 1_000_000L
            val boot = SensorUtil.bootCount(context)
            val date = Instant.ofEpochMilli(SensorUtil.wallTimeMs(eventTimestampNanos))
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()

            db.withTransaction<Unit> {
                val s = dao.getState()
                dao.insertIgnore(DailySteps(date, 0, settings.flow.value.goal))

                if (s == null) {
                    // First ever reading: this becomes the baseline. Steps before install are unknown.
                    dao.saveState(StepState(0, raw, eventElapsed, nowElapsed, boot))
                    return@withTransaction
                }

                val bootChanged = boot >= 0 && s.bootCount >= 0 && boot != s.bootCount
                // elapsedRealtime going backwards also proves the phone restarted.
                val rebooted = bootChanged || nowElapsed < s.lastRecordElapsedMs

                val delta: Long = when {
                    // After a restart the counter starts again from 0, so raw = steps since boot.
                    rebooted -> raw
                    raw >= s.lastRaw -> raw - s.lastRaw
                    // Lower value with an older timestamp = late/batched event; ignore it.
                    eventElapsed < s.lastEventElapsedMs -> return@withTransaction
                    // Lower value, newer timestamp, no restart = sensor reset itself.
                    else -> raw
                }

                if (delta > 0) dao.addSteps(date, delta.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

                val newEvent = if (rebooted) eventElapsed else maxOf(s.lastEventElapsedMs, eventElapsed)
                dao.saveState(StepState(0, raw, newEvent, nowElapsed, boot))
            }
        }
    }

    /** Writes every day from the first recorded day until today (including zero-step days). */
    suspend fun writeCsv(out: OutputStream) {
        val rows = dao.getAll().associateBy { it.date }
        val s = settings.flow.value
        val first = rows.keys.minOrNull()?.let { LocalDate.parse(it) }
        val w = out.bufferedWriter()
        w.write("date,steps,distance_km,calories_kcal,goal,goal_reached\n")
        if (first != null) {
            var d: LocalDate = first
            val end = LocalDate.now()
            while (!d.isAfter(end)) {
                val r = rows[d.toString()]
                val steps = r?.steps ?: 0
                val goal = r?.goal ?: s.goal
                val km = String.format(Locale.US, "%.2f", Calc.distanceKm(steps, s))
                val kcal = String.format(Locale.US, "%.0f", Calc.kcal(steps, s))
                w.write("$d,$steps,$km,$kcal,$goal,${if (steps >= goal) "yes" else "no"}\n")
                d = d.plusDays(1)
            }
        }
        w.flush()
    }
}
