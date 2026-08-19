package com.gilbertcenteno.hifismarteq

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import com.gilbertcenteno.hifismarteq.audio.EqState
import com.gilbertcenteno.hifismarteq.model.Preset
import com.gilbertcenteno.hifismarteq.model.PresetLibrary
import com.gilbertcenteno.hifismarteq.profile.ProfileManager
import com.gilbertcenteno.hifismarteq.service.HifiEqService
import kotlin.math.abs

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
                    var showPresets by remember { mutableStateOf(false) }

                    MainScreen(
                        state = state,
                        sessionsCount = sessions.size,
                        selectedPreset = selectedPreset,
                        bassBoost = bassBoost,
                        spatialStrength = spatialStrength,
                        customProfileName = customProfileName,
                        profileManager = profileManager,
                        showPresets = showPresets,
                        onShowPresets = { showPresets = !showPresets },
                        onPresetSelected = { preset ->
                            selectedPreset = preset.name
                            preset.bandGains.forEachIndexed { index, gain ->
                                EqRepository.setBandGain(index, gain)
                            }
                            EqRepository.setPreamp(preset.preampDb)
                            EqRepository.setSpatialStrength((preset.virtualizerPercent * 10).toShort())
                            bassBoost = preset.bassBoostPercent
                            spatialStrength = preset.virtualizerPercent
                            showPresets = false
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
                        onBassBoostChange = { 
                            bassBoost = it
                            EqRepository.setBassBoost(it)
                        },
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
    showPresets: Boolean,
    onShowPresets: () -> Unit,
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
                    IconButton(onClick = onShowPresets) {
                        Text("⚙️", style = MaterialTheme.typography.titleLarge)
                    }
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        if (showPresets) {
            // Diálogo de presets
            AlertDialog(
                onDismissRequest = onShowPresets,
                title = { Text("Presets de Ecualización") },
                text = {
                    LazyColumn {
                        PresetLibrary.presets.forEach { preset ->
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    onClick = { onPresetSelected(preset) }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(preset.name, style = MaterialTheme.typography.titleMedium)
                                        Text(preset.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onShowPresets) {
                        Text("Cerrar")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ecualizador vertical
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ecualizador de 10 Bandas", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        VerticalEqualizer(
                            gains = state.bandGains,
                            enabled = state.isEnabled,
                            onGainChange = onBandGainChange
                        )
                    }
                }
            }

            // Control de graves mejorado
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎸 Refuerzo de Graves: $bassBoost%", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            // Sonido Espacial
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🎧 Sonido Espacial 3D", style = MaterialTheme.typography.titleMedium)
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
                        Text("📊 Preamplificador: ${"%.1f".format(state.preampGainDb)} dB", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = state.preampGainDb,
                            onValueChange = onPreampChange,
                            valueRange = -12f..12f,
                            enabled = state.isEnabled
                        )
                    }
                }
            }

            // Perfiles personalizados
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💾 Perfiles Personalizados", style = MaterialTheme.typography.titleMedium)
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
                                        Text("🗑️", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            Text("No hay perfiles guardados", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { showSaveDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Guardar configuración actual")
                        }
                    }
                }
            }

            // Configuración
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📱 Sesiones detectadas: $sessionsCount", style = MaterialTheme.typography.titleMedium)
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

@Composable
fun VerticalEqualizer(
    gains: List<Float>,
    enabled: Boolean,
    onGainChange: (Int, Float) -> Unit
) {
    val frequencies = EqRepository.FREQUENCIES
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        gains.forEachIndexed { index, gain ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Valor de ganancia
                Text(
                    text = "${"%.1f".format(gain)}",
                    style = MaterialTheme.typography.labelSmall
                )
                
                // Slider vertical personalizado
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(150.dp)
                        .pointerInput(enabled) {
                            if (enabled) {
                                detectDragGestures { change, _ ->
                                    val height = size.height
                                    val newGain = ((height / 2 - change.position.y) / (height / 2) * 12f)
                                        .coerceIn(-12f, 12f)
                                    onGainChange(index, newGain)
                                }
                            }
                        }
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    // Barra de nivel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF00FF00))
                    )
                    
                    // Indicador de ganancia
                    val indicatorHeight = ((gain + 12) / 24) * 150
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .offset(y = (75 - indicatorHeight).dp)
                            .background(Color(0xFF00BFFF))
                    )
                }
                
                // Frecuencia
                Text(
                    text = if (frequencies[index] >= 1000) 
                        "${"%.0f".format(frequencies[index] / 1000)}k" 
                    else 
                        "${frequencies[index].toInt()}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
