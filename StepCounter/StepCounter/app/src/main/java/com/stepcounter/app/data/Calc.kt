package com.stepcounter.app.data

import java.text.NumberFormat
import java.util.Locale

fun Int.fmt(): String = NumberFormat.getIntegerInstance().format(this)
fun Long.fmt(): String = NumberFormat.getIntegerInstance().format(this)

object Calc {
    fun strideCm(s: UserSettings): Float = if (s.strideCm > 0f) s.strideCm else s.heightCm * 0.415f

    fun distanceKm(steps: Int, s: UserSettings): Double = steps * strideCm(s) / 100_000.0

    /** Rough walking estimate: ~0.57 kcal per kg per km. */
    fun kcal(steps: Int, s: UserSettings): Double = distanceKm(steps, s) * s.weightKg * 0.57

    fun distanceText(steps: Int, s: UserSettings): String {
        val km = distanceKm(steps, s)
        return if (s.imperial) String.format(Locale.getDefault(), "%.2f mi", km * 0.621371)
        else String.format(Locale.getDefault(), "%.2f km", km)
    }

    fun kcalText(steps: Int, s: UserSettings): String = kcal(steps, s).toInt().fmt()
}
