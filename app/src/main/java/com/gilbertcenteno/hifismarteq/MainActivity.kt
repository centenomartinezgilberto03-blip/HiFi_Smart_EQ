package com.gilbertcenteno.hifismarteq

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gilbertcenteno.hifismarteq.service.HifiEqService
import com.gilbertcenteno.hifismarteq.ui.EqViewModel
import com.gilbertcenteno.hifismarteq.ui.theme.HiFiTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        startForegroundService(Intent(this, HifiEqService::class.java))
        setContent {
            HiFiTheme {
                val vm: EqViewModel = viewModel()
                EqScreen(vm) {
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqScreen(vm: EqViewModel, openListenerSettings: () -> Unit) {
    val bands by vm.bands.collectAsState()
    val smart by vm.smart.collectAsState()
    val enabled by vm.enabled.collectAsState()
    val sessions by vm.sessions().collectAsState()
    var preamp by remember { mutableFloatStateOf(0f) }

    Scaffold(topBar = { TopAppBar(title = { Text("HiFi Smart EQ") }) }) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Creado por Gilbert Centeno", style = MaterialTheme.typography.labelMedium)
                Text("Procesamiento por sesión · APIs públicas de Android")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = smart, onClick = { vm.setSmart(!smart) }, label = { Text("SMART") })
                    FilterChip(selected = !smart, onClick = { vm.setSmart(false) }, label = { Text("MANUAL") })
                    Switch(checked = enabled, onCheckedChange = vm::setEnabled)
                }
            }
            item {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("Sesiones detectadas: ${sessions.size}")
                        sessions.take(3).forEach {
                            Text("${it.packageName} · session ${it.audioSessionId}")
                        }
                        Text(
                            if (sessions.isEmpty()) "Reproduce música para que Android exponga una sesión."
                            else "El servicio intentará asociar el DSP a la sesión activa."
                        )
                    }
                }
            }
            item {
                OutlinedButton(onClick = openListenerSettings) {
                    Text("Abrir acceso a Notification Listener")
                }
            }
            item { EqGraph(bands) }
            item {
                Text("Preamplificador: %.1f dB".format(preamp))
                Slider(value = preamp, onValueChange = { preamp = it }, valueRange = -12f..12f)
            }
            item { Text("10 bandas", style = MaterialTheme.typography.titleLarge) }
            itemsIndexed(bands) { index, band ->
                BandCard(index, band) { vm.updateBand(index, it) }
            }
        }
    }
}

@Composable
private fun BandCard(index: Int, band: com.gilbertcenteno.hifismarteq.model.EqBand, onChange: (com.gilbertcenteno.hifismarteq.model.EqBand) -> Unit) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text("Banda ${index + 1}")
            Text("Frecuencia %.0f Hz".format(band.frequencyHz))
            Slider(value = band.frequencyHz, onValueChange = { onChange(band.copy(frequencyHz = it)) }, valueRange = 20f..20000f)
            Text("Ganancia %.1f dB".format(band.gainDb))
            Slider(value = band.gainDb, onValueChange = { onChange(band.copy(gainDb = it)) }, valueRange = -12f..12f)
            Text("Q %.2f".format(band.q))
            Slider(value = band.q, onValueChange = { onChange(band.copy(q = it)) }, valueRange = 0.1f..10f)
        }
    }
}

@Composable
private fun EqGraph(bands: List<com.gilbertcenteno.hifismarteq.model.EqBand>) {
    Card(Modifier.fillMaxWidth().height(190.dp)) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val sorted = bands.sortedBy { it.frequencyHz }
            val path = Path()
            sorted.forEachIndexed { i, band ->
                val x = i.toFloat() / sorted.lastIndex.coerceAtLeast(1) * size.width
                val y = size.height / 2f - band.gainDb / 12f * size.height / 2f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = Color.Cyan, radius = 5f, center = Offset(x, y))
            }
            drawPath(path = path, color = Color.Cyan, style = Stroke(width = 3f))
        }
    }
}
