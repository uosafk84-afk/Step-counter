package com.stepcounter.app.ui

import android.Manifest
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepcounter.app.sensor.SensorUtil
import com.stepcounter.app.sensor.StepService
import com.stepcounter.app.stepApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SensorEventListener {

    private val vm: MainViewModel by viewModels()
    private var hasPermission by mutableStateOf(false)
    private var permanentlyDenied by mutableStateOf(false)
    private val sensorAvailable by lazy { SensorUtil.stepSensor(this) != null }
    private var listening = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            hasPermission = SensorUtil.hasPermission(this)
            if (hasPermission) {
                startTracking()
            } else {
                permanentlyDenied = Build.VERSION.SDK_INT >= 29 &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        hasPermission = SensorUtil.hasPermission(this)
        setContent {
            StepTheme {
                AppRoot(
                    vm = vm,
                    hasPermission = hasPermission,
                    permanentlyDenied = permanentlyDenied,
                    sensorAvailable = sensorAvailable,
                    onAllow = { if (permanentlyDenied) openAppSettings() else requestPermissions() },
                    onOpenBattery = { openBatterySettings() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hasPermission = SensorUtil.hasPermission(this)
        if (hasPermission) permanentlyDenied = false
        if (hasPermission && sensorAvailable) {
            startTracking()
            registerLive()
        }
    }

    override fun onStop() {
        unregisterLive()
        super.onStop()
    }

    private fun requestPermissions() {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 29) list += Manifest.permission.ACTIVITY_RECOGNITION
        if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.POST_NOTIFICATIONS
        if (list.isEmpty()) {
            hasPermission = true
            startTracking()
        } else {
            permissionLauncher.launch(list.toTypedArray())
        }
    }

    private fun startTracking() {
        if (sensorAvailable && SensorUtil.hasPermission(this)) {
            StepService.start(this)
            registerLive()
        }
    }

    /** While the app is on screen, read the sensor with no batching so the numbers are live. */
    private fun registerLive() {
        if (listening) return
        val sensor = SensorUtil.stepSensor(this) ?: return
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        listening = sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterLive() {
        if (!listening) return
        (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values[0]
        val ts = event.timestamp
        val app = stepApp
        app.appScope.launch { app.repository.record(raw, ts) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }

    private fun openBatterySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure { openAppSettings() }
    }
}

@Composable
private fun AppRoot(
    vm: MainViewModel,
    hasPermission: Boolean,
    permanentlyDenied: Boolean,
    sensorAvailable: Boolean,
    onAllow: () -> Unit,
    onOpenBattery: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var introDismissed by rememberSaveable { mutableStateOf(false) }

    AppBackground {
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            if (sensorAvailable && !hasPermission && !introDismissed) {
                PermissionScreen(permanentlyDenied, onAllow) { introDismissed = true }
            } else {
                Crossfade(tab, label = "tab") { t ->
                    when (t) {
                        0 -> HomeScreen(vm, hasPermission, permanentlyDenied, sensorAvailable, onAllow)
                        1 -> HistoryScreen(vm)
                        else -> SettingsScreen(vm, onOpenBattery)
                    }
                }
                BottomBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Rounded.Home to "Today",
        Icons.Rounded.DateRange to "History",
        Icons.Rounded.Settings to "Settings"
    )
    Glass(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(36.dp)
    ) {
        Row(Modifier.padding(6.dp)) {
            items.forEachIndexed { i, (icon, label) ->
                val sel = i == selected
                val bg by animateColorAsState(if (sel) Pal.Coral.copy(alpha = 0.14f) else Color.Transparent, label = "bg")
                val fg by animateColorAsState(if (sel) Pal.Coral else Pal.InkSoft, label = "fg")
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(30.dp))
                        .background(bg)
                        .clickable { onSelect(i) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
                    if (sel) {
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(permanentlyDenied: Boolean, onAllow: () -> Unit, onLater: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgressRing(0.72f, Modifier.size(150.dp), stroke = 14.dp) {
            Text("👣", fontSize = 40.sp)
        }
        Spacer(Modifier.height(28.dp))
        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Text("Let's count your steps", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Android calls the step sensor part of \"Physical activity\" (Activity recognition). " +
                        "Step Counter needs this permission only to read your phone's built-in step counter.",
                    fontSize = 15.sp, color = Pal.InkSoft
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "• No GPS or location is used\n• Your steps never leave this phone\n• The sensor is very low-power, so it can count with the screen off",
                    fontSize = 15.sp, color = Pal.Ink
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "On Android 13+ you'll also be asked about notifications. That only lets Android show the small \"counting\" indicator; counting works either way.",
                    fontSize = 13.sp, color = Pal.InkSoft
                )
                Spacer(Modifier.height(20.dp))
                GradientButton(
                    if (permanentlyDenied) "Open app settings" else "Allow",
                    Modifier.fillMaxWidth(), onAllow
                )
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                    Text("Not now", color = Pal.InkSoft)
                }
            }
        }
    }
}
