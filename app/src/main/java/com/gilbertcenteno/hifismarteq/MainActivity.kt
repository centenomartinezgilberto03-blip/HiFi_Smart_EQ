package com.gilbertcenteno.hifismarteq

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import com.gilbertcenteno.hifismarteq.audio.EqState
import com.gilbertcenteno.hifismarteq.model.*
import com.gilbertcenteno.hifismarteq.service.HifiEqService
import kotlin.math.roundToInt

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
                        onBassBoostChange = { EqRepository.setBassBoost(it) },
                        onSpatialAudioToggle = { EqRepository.setSpatialAudioEnabled(it) },
                        onSpatialStrengthChange = { EqRepository.setSpatialStrength(it.toShort()) },
                        onCompressorChange = { EqRepository.setCompressor(it) },
                        onLimiterChange = { EqRepository.setLimiter(it) },
                        onReverbChange = { EqRepository.setReverb(it) },
                        onStereoEnhanceChange = { EqRepository.setStereoEnhance(it) },
                        onBassTrebleChange = { EqRepository.setBassTreble(it) },
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
    onBassBoostChange: (Int) -> Unit,
    onSpatialAudioToggle: (Boolean) -> Unit,
    onSpatialStrengthChange: (Int) -> Unit,
    onCompressorChange: (CompressorSettings) -> Unit,
    onLimiterChange: (LimiterSettings) -> Unit,
    onReverbChange: (ReverbSettings) -> Unit,
    onStereoEnhanceChange: (StereoEnhanceSettings) -> Unit,
    onBassTrebleChange: (BassTrebleSettings) -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HiFi Smart EQ Pro") },
                actions = {
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.padding(end = 8.dp)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Medidores
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Medidores", style = MaterialTheme.typography.titleMedium)
                        Text("Peak: L ${"%.1f".format(state.metering.peakLeft)} dB | R ${"%.1f".format(state.metering.peakRight)} dB")
                        Text("RMS: L ${"%.1f".format(state.metering.rmsLeft)} dB | R ${"%.1f".format(state.metering.rmsRight)} dB")
                        if (state.metering.clipping) {
                            Text("⚠️ CLIPPING DETECTADO", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Ecualizador
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ecualizador de 32 Bandas", style = MaterialTheme.typography.titleMedium)
                        Text("Preamplificador: ${"%.1f".format(state.preampGainDb)} dB")
                        Slider(
                            value = state.preampGainDb,
                            onValueChange = onPreampChange,
                            valueRange = -20f..20f,
                            enabled = state.isEnabled
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Grid de bandas en 2 columnas
                        state.bandGains.chunked(16).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                chunk.forEach { gain ->
                                    val index = state.bandGains.indexOf(gain)
                                    if (index >= 0) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("${EqRepository.FREQUENCIES[index].roundToInt()}Hz", style = MaterialTheme.typography.labelSmall)
                                            Slider(
                                                value = gain,
                                                onValueChange = { onBandGainChange(index, it) },
                                                valueRange = -15f..15f,
                                                enabled = state.isEnabled,
                                                modifier = Modifier.height(100.dp)
                                            )
                                            Text("${"%.1f".format(gain)}dB", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bass Boost
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bass Boost: ${state.bassBoostPercent}%", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = state.bassBoostPercent.toFloat(),
                            onValueChange = { onBassBoostChange(it.roundToInt()) },
                            valueRange = 0f..100f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            // Compresor
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Compresor", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.compressor.enabled,
                                onCheckedChange = { onCompressorChange(state.compressor.copy(enabled = it)) }
                            )
                            Text("Activado")
                        }
                        if (state.compressor.enabled) {
                            Text("Threshold: ${state.compressor.thresholdDb} dB")
                            Slider(
                                value = state.compressor.thresholdDb,
                                onValueChange = { onCompressorChange(state.compressor.copy(thresholdDb = it)) },
                                valueRange = -60f..0f
                            )
                            Text("Ratio: ${state.compressor.ratio}:1")
                            Slider(
                                value = state.compressor.ratio,
                                onValueChange = { onCompressorChange(state.compressor.copy(ratio = it)) },
                                valueRange = 1f..20f
                            )
                            Text("Attack: ${state.compressor.attackMs} ms")
                            Slider(
                                value = state.compressor.attackMs,
                                onValueChange = { onCompressorChange(state.compressor.copy(attackMs = it)) },
                                valueRange = 1f..100f
                            )
                            Text("Release: ${state.compressor.releaseMs} ms")
                            Slider(
                                value = state.compressor.releaseMs,
                                onValueChange = { onCompressorChange(state.compressor.copy(releaseMs = it)) },
                                valueRange = 10f..500f
                            )
                        }
                    }
                }
            }

            // Limitador
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Limitador", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.limiter.enabled,
                                onCheckedChange = { onLimiterChange(state.limiter.copy(enabled = it)) }
                            )
                            Text("Activado")
                        }
                        if (state.limiter.enabled) {
                            Text("Threshold: ${state.limiter.thresholdDb} dB")
                            Slider(
                                value = state.limiter.thresholdDb,
                                onValueChange = { onLimiterChange(state.limiter.copy(thresholdDb = it)) },
                                valueRange = -30f..0f
                            )
                            Text("Ceiling: ${state.limiter.ceilingDb} dB")
                            Slider(
                                value = state.limiter.ceilingDb,
                                onValueChange = { onLimiterChange(state.limiter.copy(ceilingDb = it)) },
                                valueRange = -10f..0f
                            )
                        }
                    }
                }
            }

            // Sonido Espacial
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sonido Espacial", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.isSpatialAudioEnabled,
                                onCheckedChange = onSpatialAudioToggle
                            )
                            Text("Activado")
                        }
                        if (state.isSpatialAudioEnabled) {
                            Text("Intensidad: ${state.spatialStrength.toInt() / 10}%")
                            Slider(
                                value = state.spatialStrength.toFloat(),
                                onValueChange = { onSpatialStrengthChange(it.toInt()) },
                                valueRange = 0f..1000f
                            )
                        }
                    }
                }
            }

            // Stereo Enhance
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stereo Enhance", style = MaterialTheme.typography.titleMedium)
                        Text("Balance: ${"%.1f".format(state.stereoEnhance.balance)}")
                        Slider(
                            value = state.stereoEnhance.balance,
                            onValueChange = { onStereoEnhanceChange(state.stereoEnhance.copy(balance = it)) },
                            valueRange = -1f..1f
                        )
                        Text("Stereo Width: ${"%.0f".format(state.stereoEnhance.stereoWidth)}%")
                        Slider(
                            value = state.stereoEnhance.stereoWidth,
                            onValueChange = { onStereoEnhanceChange(state.stereoEnhance.copy(stereoWidth = it)) },
                            valueRange = 0f..200f
                        )
                        Row {
                            Switch(
                                checked = state.stereoEnhance.monoEnabled,
                                onCheckedChange = { onStereoEnhanceChange(state.stereoEnhance.copy(monoEnabled = it)) }
                            )
                            Text("Mono")
                        }
                    }
                }
            }

            // Reverb
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reverb", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.reverb.enabled,
                                onCheckedChange = { onReverbChange(state.reverb.copy(enabled = it)) }
                            )
                            Text("Activado")
                        }
                        if (state.reverb.enabled) {
                            Text("Room Size: ${state.reverb.roomSize}%")
                            Slider(
                                value = state.reverb.roomSize.toFloat(),
                                onValueChange = { onReverbChange(state.reverb.copy(roomSize = it.toInt())) },
                                valueRange = 0f..100f
                            )
                            Text("Wet/Dry: ${"%.0f".format(state.reverb.wetDryMix)}%")
                            Slider(
                                value = state.reverb.wetDryMix,
                                onValueChange = { onReverbChange(state.reverb.copy(wetDryMix = it)) },
                                valueRange = 0f..100f
                            )
                        }
                    }
                }
            }

            // Configuración
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sesiones: $sessionsCount", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = onOpenNotificationListenerSettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Configurar Notification Listener")
                        }
                    }
                }
            }
        }
    }
}
