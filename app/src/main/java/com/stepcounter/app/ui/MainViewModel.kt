package com.stepcounter.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepcounter.app.StepApp
import com.stepcounter.app.data.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class HistoryMode { DAY, MONTH, YEAR }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val sa = app as StepApp
    val repo = sa.repository
    val settings: StateFlow<UserSettings> = sa.settings.flow

    /** Re-checks the date every 20 s so the screen rolls over to a new day by itself. */
    val today: StateFlow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(20_000)
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todaySteps: StateFlow<Int> = today
        .flatMapLatest { repo.observeDay(it) }
        .map { it?.steps ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val firstDate: StateFlow<LocalDate?> = repo.observeFirstDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var mode by mutableStateOf(HistoryMode.DAY)
    var selected by mutableStateOf(initialSelected())

    private fun initialSelected(): LocalDate {
        val now = LocalDate.now()
        val saved = sa.settings.lastViewed?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return if (settings.value.autoNewYear || saved == null) now
        else if (saved.isAfter(now)) now else saved
    }

    fun select(date: LocalDate) {
        val now = LocalDate.now()
        selected = if (date.isAfter(now)) now else date
        sa.settings.lastViewed = selected.toString()
    }

    fun setGoal(goal: Int) {
        sa.settings.update { it.copy(goal = goal) }
        viewModelScope.launch { repo.applyGoal(goal) }
    }

    fun saveSettings(goal: Int, weightKg: Float, heightCm: Float, strideCm: Float) {
        sa.settings.update { it.copy(goal = goal, weightKg = weightKg, heightCm = heightCm, strideCm = strideCm) }
        viewModelScope.launch { repo.applyGoal(goal) }
    }

    fun setImperial(value: Boolean) = sa.settings.update { it.copy(imperial = value) }
    fun setAutoNewYear(value: Boolean) = sa.settings.update { it.copy(autoNewYear = value) }

    fun exportTo(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?.use { repo.writeCsv(it) } ?: error("Could not open file")
                }.isSuccess
            }
            onDone(ok)
        }
    }
}
