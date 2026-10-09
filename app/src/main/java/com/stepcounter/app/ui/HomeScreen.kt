package com.stepcounter.app.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepcounter.app.data.Calc
import com.stepcounter.app.data.fmt
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    vm: MainViewModel,
    hasPermission: Boolean,
    permanentlyDenied: Boolean,
    sensorAvailable: Boolean,
    onAllow: () -> Unit
) {
    val steps by vm.todaySteps.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    var editGoal by remember { mutableStateOf(false) }

    val progress = if (s.goal > 0) steps.toFloat() / s.goal else 0f
    val shown by animateIntAsState(steps, tween(700), label = "steps")
    val reached = steps >= s.goal

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("TODAY", fontSize = 12.sp, letterSpacing = 2.sp, color = Pal.InkSoft, fontWeight = FontWeight.SemiBold)
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink
            )
        }

        if (!sensorAvailable) {
            NoticeCard(
                "No step sensor found",
                "This phone doesn't have a built-in step counter, so steps can't be counted. Your history and settings still work."
            )
        } else if (!hasPermission) {
            NoticeCard(
                "Permission needed",
                "Step Counter needs the Activity recognition permission to read your phone's step sensor.",
                if (permanentlyDenied) "Open settings" else "Allow",
                onAllow
            )
        }

        Glass(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProgressRing(progress, Modifier.size(272.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            shown.fmt(), fontSize = 58.sp, fontWeight = FontWeight.Light,
                            color = Pal.Ink, textAlign = TextAlign.Center
                        )
                        Text("steps", fontSize = 16.sp, color = Pal.InkSoft)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${(progress * 100).toInt()}%",
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            color = if (reached) Pal.Mint else Pal.Coral
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Glass(shape = RoundedCornerShape(50), onClick = { editGoal = true }) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Goal  ${s.goal.fmt()}", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit goal", tint = Pal.InkSoft, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (reached) "Goal reached — great job!" else "${(s.goal - steps).fmt()} steps to go",
                    fontSize = 14.sp, color = if (reached) Pal.Mint else Pal.InkSoft,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(Icons.Rounded.Place, "distance (est.)", Calc.distanceText(steps, s), Pal.Mint, Modifier.weight(1f))
            StatCard(Icons.Rounded.LocalFireDepartment, "kcal burned (est.)", Calc.kcalText(steps, s), Pal.Coral, Modifier.weight(1f))
        }
    }

    if (editGoal) {
        GoalDialog(s.goal, onDismiss = { editGoal = false }) {
            vm.setGoal(it)
            editGoal = false
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, tint: Color, modifier: Modifier) {
    Glass(modifier) {
        Column(Modifier.padding(18.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            Text(label, fontSize = 13.sp, color = Pal.InkSoft)
        }
    }
}

@Composable
private fun GoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val valid = text.toIntOrNull()?.let { it in 500..100_000 } ?: false
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFBF7),
        title = { Text("Daily step goal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(6) },
                singleLine = true,
                isError = !valid,
                supportingText = { Text("Between 500 and 100,000 steps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onSave(text.toInt()) }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
