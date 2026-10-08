package com.stepcounter.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One row per calendar day. date = ISO "yyyy-MM-dd". goal = the goal that applied on that day. */
@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey val date: String,
    val steps: Int,
    val goal: Int
)

/** Single-row table (id = 0) holding the sensor baseline so restarts/reboots are handled correctly. */
@Entity(tableName = "step_state")
data class StepState(
    @PrimaryKey val id: Int = 0,
    val lastRaw: Long,
    val lastEventElapsedMs: Long,
    val lastRecordElapsedMs: Long,
    val bootCount: Int
)

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: String, to: String): Flow<List<DailySteps>>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun observeDay(date: String): Flow<DailySteps?>

    @Query("SELECT MIN(date) FROM daily_steps")
    fun observeFirstDate(): Flow<String?>

    @Query("SELECT * FROM daily_steps ORDER BY date")
    suspend fun getAll(): List<DailySteps>

    @Query("SELECT steps FROM daily_steps WHERE date = :date")
    suspend fun stepsOn(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(row: DailySteps): Long

    @Query("UPDATE daily_steps SET steps = steps + :delta WHERE date = :date")
    suspend fun addSteps(date: String, delta: Int)

    @Query("UPDATE daily_steps SET goal = :goal WHERE date = :date")
    suspend fun setGoal(date: String, goal: Int)

    @Query("SELECT * FROM step_state WHERE id = 0")
    suspend fun getState(): StepState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: StepState)
}

@Database(entities = [DailySteps::class, StepState::class], version = 1, exportSchema = false)
abstract class StepDatabase : RoomDatabase() {
    abstract fun dao(): StepDao
}
