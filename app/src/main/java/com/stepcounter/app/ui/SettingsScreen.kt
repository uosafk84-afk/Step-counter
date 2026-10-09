package com.stepcounter.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.util.Locale

private const val LB_PER_KG = 2.20462f
private const val CM_PER_IN = 2.54f

private fun num(v: Float): String =
    String.format(Locale.US, "%.1f", v).removeSuffix(".0")

private fun parse(t: String): Float? = t.trim().replace(',', '.').toFloatOrNull()

@Composable
fun SettingsScreen(vm: MainViewModel, onOpenBattery: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val imp = s.imperial

    var goalText by remember { mutableStateOf(s.goal.toString()) }
    var weightText by remember(imp) { mutableStateOf(num(if (imp) s.weightKg * LB_PER_KG else s.weightKg)) }
    var heightText by remember(imp) { mutableStateOf(num(if (imp) s.heightCm / CM_PER_IN else s.heightCm)) }
    var strideText by remember(imp) {
        mutableStateOf(if (s.strideCm > 0f) num(if (imp) s.strideCm / CM_PER_IN else s.strideCm) else "")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            vm.exportTo(uri) { ok ->
                Toast.makeText(ctx, if (ok) "History exported" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("SETTINGS", fontSize = 12.sp, letterSpacing = 2.sp, color = Pal.InkSoft, fontWeight = FontWeight.SemiBold)
            Text("Make it yours", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
        }

        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Units", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                Segmented(listOf("Metric", "Imperial"), if (imp) 1 else 0, { vm.setImperial(it == 1) })
            }
        }

        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Goal & body", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                Field(goalText, { goalText = it.filter(Char::isDigit).take(6) }, "Daily step goal", "500 – 100,000", false)
                Field(weightText, { weightText = it }, if (imp) "Weight (lb)" else "Weight (kg)", "Used for calorie estimate")
                Field(heightText, { heightText = it }, if (imp) "Height (in)" else "Height (cm)", null)
                Field(
                    strideText, { strideText = it },
                    if (imp) "Stride length (in)" else "Stride length (cm)",
                    "Leave empty to calculate from your height"
                )
                GradientButton("Save changes", Modifier.fillMaxWidth()) {
                    val goal = goalText.toIntOrNull()
                    val w = parse(weightText)?.let { if (imp) it / LB_PER_KG else it }
                    val h = parse(heightText)?.let { if (imp) it * CM_PER_IN else it }
                    val strideRaw = strideText.trim()
                    val st = if (strideRaw.isEmpty()) 0f
                    else parse(strideRaw)?.let { if (imp) it * CM_PER_IN else it }
                    val ok = goal != null && goal in 500..100_000 &&
                        w != null && w in 20f..300f &&
                        h != null && h in 100f..250f &&
                        st != null && (st == 0f || st in 20f..150f)
                    if (ok) {
                        vm.saveSettings(goal!!, w!!, h!!, st!!)
                        Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "Please check the values", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Start new year automatically", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                        Text(
                            "History opens on today's date, so a new month or year starts by itself. Turn off to reopen where you left off. Old years are never deleted.",
                            fontSize = 13.sp, color = Pal.InkSoft
                        )
                    }
                    Switch(
                        checked = s.autoNewYear,
                        onCheckedChange = { vm.setAutoNewYear(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Pal.Coral, checkedThumbColor = Color.White)
                    )
                }
            }
        }

        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your data", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                Text(
                    "Everything is stored only on this phone. Export every recorded day as a CSV file you can open in Excel or Sheets.",
                    fontSize = 13.sp, color = Pal.InkSoft
                )
                GradientButton("Export history (CSV)", Modifier.fillMaxWidth()) {
                    exportLauncher.launch("step_history_${LocalDate.now()}.csv")
                }
            }
        }

        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reliable counting", fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                Text(
                    "Some phones (Xiaomi, Oppo, Vivo, Tecno, Huawei…) stop background apps to save battery. If steps seem to pause, allow Step Counter to run without battery restrictions.",
                    fontSize = 13.sp, color = Pal.InkSoft
                )
                GradientButton("Battery settings", Modifier.fillMaxWidth(), onOpenBattery)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    hint: String?,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { if (hint != null) Text(hint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Pal.Coral,
            unfocusedBorderColor = Pal.Ink.copy(alpha = 0.15f),
            focusedLabelColor = Pal.Coral,
            focusedContainerColor = Color.White.copy(alpha = 0.5f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
