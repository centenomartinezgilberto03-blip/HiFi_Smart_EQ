package com.gilbertcenteno.hifismarteq

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import com.gilbertcenteno.hifismarteq.audio.EqState
import com.gilbertcenteno.hifismarteq.service.HifiEqService

class MainActivity : ComponentActivity() {

    private lateinit var monitor: AudioPlaybackMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, HifiEqService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        monitor = AudioPlaybackMonitor(this)
        monitor.start()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by EqRepository.state.collectAsState()
                    val sessions by monitor.sessions.collectAsState()

                    MainScreen(
                        state = state,
                        sessionsCount = sessions.size,
                        onToggleEnabled = { EqRepository.setEnabled(it) },
                        onPreampChange = { EqRepository.setPreamp(it) },
                        onBandGainChange = { index, gain -> EqRepository.setBandGain(index, gain) },
                        onSpatialAudioToggle = { EqRepository.setSpatialAudioEnabled(it) },
                        onOpenNotificationListenerSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        monitor.stop()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: EqState,
    sessionsCount: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    onSpatialAudioToggle: (Boolean) -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HiFi Smart EQ") },
                actions = {
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sesiones detectadas: $sessionsCount",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (sessionsCount == 0) {
                            Text(
                                text = "Reproduce música o abre tu reproductor. Si no detecta, se aplicará a la sesión global (0).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onOpenNotificationListenerSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir acceso a Notification Listener")
                }
            }

            if (state.spatialAudioSupported) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Sonido Espacial 3D",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Efecto envolvente tridimensional",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = state.isSpatialAudioEnabled,
                                onCheckedChange = onSpatialAudioToggle,
                                enabled = state.isEnabled
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Preamplificador: ${"%.1f".format(state.preampGainDb)} dB",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = state.preampGainDb,
                            onValueChange = onPreampChange,
                            valueRange = -12f..12f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            item {
                Text(
                    text = "10 Bandas de Ecualización",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            itemsIndexed(EqRepository.FREQUENCIES.toList()) { index, freq ->
                val gain = state.bandGains.getOrElse(index) { 0f }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Banda ${index + 1}: ${freq.toInt()} Hz")
                        Text(text = "Ganancia: ${"%.1f".format(gain)} dB")
                        Slider(
                            value = gain,
                            onValueChange = { onBandGainChange(index, it) },
                            valueRange = -12f..12f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }
        }
    }
}
