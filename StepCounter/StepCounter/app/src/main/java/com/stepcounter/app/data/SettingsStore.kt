package com.stepcounter.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val goal: Int = 8000,
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    /** 0 = calculate automatically from height. */
    val strideCm: Float = 0f,
    val imperial: Boolean = false,
    val autoNewYear: Boolean = true
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<UserSettings> = _flow.asStateFlow()

    private fun load() = UserSettings(
        goal = prefs.getInt("goal", 8000),
        weightKg = prefs.getFloat("weight", 70f),
        heightCm = prefs.getFloat("height", 170f),
        strideCm = prefs.getFloat("stride", 0f),
        imperial = prefs.getBoolean("imperial", false),
        autoNewYear = prefs.getBoolean("auto_new_year", true)
    )

    fun update(block: (UserSettings) -> UserSettings) {
        val n = block(_flow.value)
        prefs.edit()
            .putInt("goal", n.goal)
            .putFloat("weight", n.weightKg)
            .putFloat("height", n.heightCm)
            .putFloat("stride", n.strideCm)
            .putBoolean("imperial", n.imperial)
            .putBoolean("auto_new_year", n.autoNewYear)
            .apply()
        _flow.value = n
    }

    var lastViewed: String?
        get() = prefs.getString("last_viewed", null)
        set(value) { prefs.edit().putString("last_viewed", value).apply() }
}
