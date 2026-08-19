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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import com.gilbertcenteno.hifismarteq.audio.EqState
import com.gilbertcenteno.hifismarteq.model.Preset
import com.gilbertcenteno.hifismarteq.model.PresetLibrary
import com.gilbertcenteno.hifismarteq.profile.ProfileManager
import com.gilbertcenteno.hifismarteq.service.HifiEqService

class MainActivity : ComponentActivity() {

    private lateinit var monitor: AudioPlaybackMonitor
    private lateinit var profileManager: ProfileManager

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
        profileManager = ProfileManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by EqRepository.state.collectAsState()
                    val sessions by monitor.sessions.collectAsState()
                    var selectedPreset by remember { mutableStateOf("Plano") }
                    var customProfileName by remember { mutableStateOf("") }
                    var bassBoost by remember { mutableStateOf(0) }
                    var spatialStrength by remember { mutableStateOf(50) }

                    MainScreen(
                        state = state,
                        sessionsCount = sessions.size,
                        selectedPreset = selectedPreset,
                        bassBoost = bassBoost,
                        spatialStrength = spatialStrength,
                        customProfileName = customProfileName,
                        profileManager = profileManager,
                        onPresetSelected = { preset ->
                            selectedPreset = preset.name
                            preset.bandGains.forEachIndexed { index, gain ->
                                EqRepository.setBandGain(index, gain)
                            }
                            EqRepository.setPreamp(preset.preampDb)
                            EqRepository.setSpatialStrength((preset.virtualizerPercent * 10).toShort())
                            bassBoost = preset.bassBoostPercent
                            spatialStrength = preset.virtualizerPercent
                        },
                        onCustomProfileNameChange = { customProfileName = it },
                        onSaveProfile = {
                            if (customProfileName.isNotBlank()) {
                                profileManager.saveProfile(
                                    customProfileName,
                                    state.preampGainDb,
                                    state.bandGains,
                                    spatialStrength,
                                    bassBoost
                                )
                                customProfileName = ""
                            }
                        },
                        onLoadProfile = { name ->
                            profileManager.loadProfile(name)?.let { (preamp, bands) ->
                                EqRepository.setPreamp(preamp)
                                bands.forEachIndexed { index, gain ->
                                    EqRepository.setBandGain(index, gain)
                                }
                            }
                        },
                        onDeleteProfile = { name ->
                            profileManager.deleteProfile(name)
                        },
                        onBassBoostChange = { bassBoost = it },
                        onSpatialStrengthChange = { spatialStrength = it },
                        onToggleEnabled = { EqRepository.setEnabled(it) },
                        onPreampChange = { EqRepository.setPreamp(it) },
                        onBandGainChange = { index, gain -> EqRepository.setBandGain(index, gain) },
                        onSpatialAudioToggle = { EqRepository.setSpatialAudioEnabled(it) },
                        onSpatialStrengthUpdate = { EqRepository.setSpatialStrength(it.toShort()) },
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
    selectedPreset: String,
    bassBoost: Int,
    spatialStrength: Int,
    customProfileName: String,
    profileManager: ProfileManager,
    onPresetSelected: (Preset) -> Unit,
    onCustomProfileNameChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onLoadProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onSpatialStrengthChange: (Int) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    onSpatialAudioToggle: (Boolean) -> Unit,
    onSpatialStrengthUpdate: (Int) -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    val profiles = remember { mutableStateListOf<String>() }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        profiles.clear()
        profiles.addAll(profileManager.getAllProfiles())
    }

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
            // Presets
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Presets", style = MaterialTheme.typography.titleLarge)
                        Text("Selecciona un preset predefinido", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        PresetLibrary.presets.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset.name,
                                onClick = { onPresetSelected(preset) },
                                label = { Text(preset.name) },
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Perfiles personalizados
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Perfiles Personalizados", style = MaterialTheme.typography.titleLarge)
                        if (profiles.isNotEmpty()) {
                            profiles.forEach { profile ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = { onLoadProfile(profile) }) {
                                        Text(profile)
                                    }
                                    TextButton(onClick = {
                                        onDeleteProfile(profile)
                                        profiles.remove(profile)
                                    }) {
                                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            Text("No hay perfiles guardados", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showSaveDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Guardar configuración actual")
                        }
                    }
                }
            }

            // Control de graves
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Refuerzo de Graves: $bassBoost%", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            // Sonido Espacial con control de intensidad
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sonido Espacial 3D", style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = state.isSpatialAudioEnabled,
                                onCheckedChange = onSpatialAudioToggle,
                                enabled = state.isEnabled
                            )
                        }
                        if (state.isSpatialAudioEnabled) {
                            Text("Intensidad: $spatialStrength%", style = MaterialTheme.typography.bodyMedium)
                            Slider(
                                value = spatialStrength.toFloat(),
                                onValueChange = { 
                                    onSpatialStrengthChange(it.toInt())
                                    onSpatialStrengthUpdate(it.toInt())
                                },
                                valueRange = 0f..100f,
                                enabled = state.isEnabled
                            )
                        }
                    }
                }
            }

            // Preamplificador
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Preamplificador: ${"%.1f".format(state.preampGainDb)} dB", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = state.preampGainDb,
                            onValueChange = onPreampChange,
                            valueRange = -12f..12f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            // Bandas de ecualización
            item {
                Text("10 Bandas de Ecualización", style = MaterialTheme.typography.titleLarge)
            }

            itemsIndexed(EqRepository.FREQUENCIES.toList()) { index, freq ->
                val gain = state.bandGains.getOrElse(index) { 0f }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Banda ${index + 1}: ${freq.toInt()} Hz")
                        Text("Ganancia: ${"%.1f".format(gain)} dB")
                        Slider(
                            value = gain,
                            onValueChange = { onBandGainChange(index, it) },
                            valueRange = -12f..12f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            // Configuración
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sesiones detectadas: $sessionsCount", style = MaterialTheme.typography.titleMedium)
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

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar Perfil") },
            text = {
                OutlinedTextField(
                    value = customProfileName,
                    onValueChange = onCustomProfileNameChange,
                    label = { Text("Nombre del perfil") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSaveProfile()
                    showSaveDialog = false
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
