package com.stepcounter.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepcounter.app.data.Calc
import com.stepcounter.app.data.DailySteps
import com.stepcounter.app.data.UserSettings
import com.stepcounter.app.data.fmt
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

data class PeriodStats(
    val total: Long,
    val days: Int,
    val average: Int,
    val best: DailySteps?,
    val goalDays: Int
)

fun computeStats(
    rows: List<DailySteps>,
    from: LocalDate,
    to: LocalDate,
    firstDate: LocalDate?,
    today: LocalDate
): PeriodStats {
    val start = if (firstDate != null && firstDate.isAfter(from)) firstDate else from
    val end = if (to.isAfter(today)) today else to
    val days = if (firstDate == null || end.isBefore(start)) 0
    else ChronoUnit.DAYS.between(start, end).toInt() + 1
    val total = rows.sumOf { it.steps.toLong() }
    return PeriodStats(
        total = total,
        days = days,
        average = if (days > 0) (total / days).toInt() else 0,
        best = rows.maxByOrNull { it.steps }?.takeIf { it.steps > 0 },
        goalDays = rows.count { it.steps > 0 && it.steps >= it.goal }
    )
}

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val first by vm.firstDate.collectAsStateWithLifecycle()
    val mode = vm.mode
    val sel = vm.selected

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("HISTORY", fontSize = 12.sp, letterSpacing = 2.sp, color = Pal.InkSoft, fontWeight = FontWeight.SemiBold)
            Text("Your steps", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
        }
        Segmented(listOf("Day", "Month", "Year"), mode.ordinal, { vm.mode = HistoryMode.values()[it] })

        when (mode) {
            HistoryMode.DAY -> DayView(vm, s, today, sel)
            HistoryMode.MONTH -> MonthView(vm, s, today, first, sel)
            HistoryMode.YEAR -> YearView(vm, s, today, first, sel)
        }
    }
}

@Composable
private fun PeriodHeader(title: String, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowButton(true, true, onPrev)
        Text(
            title, Modifier.weight(1f), textAlign = TextAlign.Center,
            fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink
        )
        ArrowButton(false, canNext, onNext)
    }
}

@Composable
private fun ArrowButton(left: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Glass(Modifier.size(44.dp), shape = CircleShape, onClick = if (enabled) onClick else null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                if (left) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                contentDescription = if (left) "Previous" else "Next",
                tint = if (enabled) Pal.Ink else Pal.InkSoft.copy(alpha = 0.35f)
            )
        }
    }
}

// ---------------------------------------------------------------- DAY

@Composable
private fun DayView(vm: MainViewModel, s: UserSettings, today: LocalDate, sel: LocalDate) {
    val ym = YearMonth.from(sel)
    val rows by remember(ym) { vm.repo.observeRange(ym.atDay(1), ym.atEndOfMonth()) }
        .collectAsStateWithLifecycle(initialValue = emptyList<DailySteps>())
    val byDate = remember(rows) { rows.associateBy { it.date } }

    PeriodHeader(
        ym.format(DateTimeFormatter.ofPattern("LLLL yyyy")),
        canNext = ym.isBefore(YearMonth.from(today)),
        onPrev = { vm.select(sel.minusMonths(1)) },
        onNext = { vm.select(sel.plusMonths(1)) }
    )

    val firstDow = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val offset = (ym.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val len = ym.lengthOfMonth()
    val rowCount = (offset + len + 6) / 7

    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    Text(
                        firstDow.plus(i.toLong()).getDisplayName(JTextStyle.NARROW, Locale.getDefault()),
                        Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontSize = 12.sp, color = Pal.InkSoft, fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            for (r in 0 until rowCount) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - offset + 1
                        if (day in 1..len) {
                            val date = ym.atDay(day)
                            val row = byDate[date.toString()]
                            DayCell(
                                day = day,
                                steps = row?.steps ?: 0,
                                goal = row?.goal ?: s.goal,
                                selected = date == sel,
                                isToday = date == today,
                                future = date.isAfter(today),
                                onClick = { vm.select(date) }
                            )
                        } else {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }

    val selRow = byDate[sel.toString()]
    val st = selRow?.steps ?: 0
    val goal = selRow?.goal ?: s.goal
    val reached = st > 0 && st >= goal
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                sel.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                fontSize = 14.sp, color = Pal.InkSoft, fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(st.fmt(), fontSize = 44.sp, fontWeight = FontWeight.Light, color = Pal.Ink)
                Spacer(Modifier.width(8.dp))
                Text("steps", fontSize = 16.sp, color = Pal.InkSoft, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            GradientBar(if (goal > 0) st.toFloat() / goal else 0f, done = reached)
            Spacer(Modifier.height(8.dp))
            Text(
                if (reached) "Goal achieved (${goal.fmt()})" else "${if (goal > 0) st * 100 / goal else 0}% of ${goal.fmt()} goal",
                fontSize = 14.sp, color = if (reached) Pal.Mint else Pal.InkSoft, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Row {
                Text(Calc.distanceText(st, s), fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                Spacer(Modifier.width(20.dp))
                Text("${Calc.kcalText(st, s)} kcal", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: Int, steps: Int, goal: Int, selected: Boolean, isToday: Boolean, future: Boolean, onClick: () -> Unit
) {
    val frac = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val reached = steps > 0 && steps >= goal
    val bg = when {
        future -> Color.Transparent
        reached -> Pal.Mint.copy(alpha = 0.45f)
        steps > 0 -> Pal.Coral.copy(alpha = 0.08f + 0.32f * frac)
        else -> Pal.Ink.copy(alpha = 0.04f)
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .weight(1f)
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(shape)
            .background(bg)
            .then(if (selected) Modifier.border(2.dp, Pal.Coral, shape) else Modifier)
            .clickable(enabled = !future, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.toString(), fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (future) Pal.InkSoft.copy(alpha = 0.4f) else Pal.Ink
            )
            if (steps > 0 && !future) {
                Text(shortK(steps), fontSize = 9.sp, color = Pal.InkSoft)
            }
        }
    }
}

private fun shortK(n: Int): String =
    if (n >= 1000) String.format(Locale.US, "%.1fk", n / 1000.0) else n.toString()

// ---------------------------------------------------------------- MONTH

@Composable
private fun MonthView(vm: MainViewModel, s: UserSettings, today: LocalDate, first: LocalDate?, sel: LocalDate) {
    val ym = YearMonth.from(sel)
    val rows by remember(ym) { vm.repo.observeRange(ym.atDay(1), ym.atEndOfMonth()) }
        .collectAsStateWithLifecycle(initialValue = emptyList<DailySteps>())
    val stats = remember(rows, first, today, ym) { computeStats(rows, ym.atDay(1), ym.atEndOfMonth(), first, today) }
    val byDate = remember(rows) { rows.associateBy { it.date } }

    PeriodHeader(
        ym.format(DateTimeFormatter.ofPattern("LLLL yyyy")),
        canNext = ym.isBefore(YearMonth.from(today)),
        onPrev = { vm.select(sel.minusMonths(1)) },
        onNext = { vm.select(sel.plusMonths(1)) }
    )
    StatsGrid(stats, "Month total")

    val len = ym.lengthOfMonth()
    val values = remember(byDate, ym) { (1..len).map { byDate[ym.atDay(it).toString()]?.steps ?: 0 } }
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Steps per day", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            Text("Dashed line = your goal", fontSize = 12.sp, color = Pal.InkSoft)
            Spacer(Modifier.height(12.dp))
            DayBars(values, s.goal, Modifier.fillMaxWidth().height(160.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1", fontSize = 11.sp, color = Pal.InkSoft)
                Text(len.toString(), fontSize = 11.sp, color = Pal.InkSoft)
            }
        }
    }
}

@Composable
private fun DayBars(values: List<Int>, goal: Int, modifier: Modifier) {
    val maxV = maxOf(values.maxOrNull() ?: 0, goal, 1)
    Canvas(modifier) {
        val n = maxOf(values.size, 1)
        val gap = 3.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        values.forEachIndexed { i, v ->
            val h = maxOf(size.height * v / maxV, 2.dp.toPx())
            val color = when {
                v <= 0 -> Pal.Ink.copy(alpha = 0.08f)
                v >= goal -> Pal.Mint
                else -> Pal.Coral.copy(alpha = 0.85f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(minOf(bw / 2f, 4.dp.toPx()))
            )
        }
        val gy = size.height - size.height * goal / maxV
        drawLine(
            Pal.Mint, Offset(0f, gy), Offset(size.width, gy),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
        )
    }
}

// ---------------------------------------------------------------- YEAR

@Composable
private fun YearView(vm: MainViewModel, s: UserSettings, today: LocalDate, first: LocalDate?, sel: LocalDate) {
    val year = sel.year
    val rows by remember(year) { vm.repo.observeRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)) }
        .collectAsStateWithLifecycle(initialValue = emptyList<DailySteps>())
    val stats = remember(rows, first, today, year) {
        computeStats(rows, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), first, today)
    }
    val months = remember(rows, first, today, year) {
        (1..12).map { m ->
            val ym = YearMonth.of(year, m)
            val prefix = ym.toString()
            ym to computeStats(rows.filter { it.date.startsWith(prefix) }, ym.atDay(1), ym.atEndOfMonth(), first, today)
        }
    }
    val maxAvg = maxOf(months.maxOf { it.second.average }, s.goal, 1)

    PeriodHeader(
        year.toString(),
        canNext = year < today.year,
        onPrev = { vm.select(sel.minusYears(1)) },
        onNext = { vm.select(sel.plusYears(1)) }
    )
    StatsGrid(stats, "Year total")

    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Months", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            months.forEach { (ym, st) ->
                val future = ym.isAfter(YearMonth.from(today))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ym.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault()),
                        Modifier.width(44.dp), fontWeight = FontWeight.Medium,
                        color = if (future) Pal.InkSoft.copy(alpha = 0.4f) else Pal.Ink
                    )
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        GradientBar(st.average.toFloat() / maxAvg, done = st.average >= s.goal && st.average > 0)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(st.total.fmt(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Pal.Ink)
                        Text("avg ${st.average.fmt()}/day", fontSize = 11.sp, color = Pal.InkSoft)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shared

@Composable
private fun StatsGrid(st: PeriodStats, totalLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(totalLabel, st.total.fmt(), "steps", Modifier.weight(1f))
            StatTile("Daily average", st.average.fmt(), "over ${st.days} days", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                "Best day",
                st.best?.steps?.fmt() ?: "—",
                st.best?.let { LocalDate.parse(it.date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) },
                Modifier.weight(1f)
            )
            StatTile("Goal reached", st.goalDays.toString(), if (st.goalDays == 1) "day" else "days", Modifier.weight(1f))
        }
    }
}
