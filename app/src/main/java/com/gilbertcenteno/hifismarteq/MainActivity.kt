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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import com.gilbertcenteno.hifismarteq.audio.EqState
import com.gilbertcenteno.hifismarteq.model.*
import com.gilbertcenteno.hifismarteq.profile.ProfileManager
import com.gilbertcenteno.hifismarteq.service.HifiEqService
import kotlin.math.roundToInt

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
                    var isEditing by remember { mutableStateOf(false) }
                    var showPresets by remember { mutableStateOf(false) }
                    var showSaveDialog by remember { mutableStateOf(false) }
                    var profileName by remember { mutableStateOf("") }
                    var selectedPreset by remember { mutableStateOf("Personalizado") }

                    MainScreen(
                        state = state,
                        sessionsCount = sessions.size,
                        isEditing = isEditing,
                        showPresets = showPresets,
                        showSaveDialog = showSaveDialog,
                        profileName = profileName,
                        selectedPreset = selectedPreset,
                        profileManager = profileManager,
                        onToggleEditing = { isEditing = !isEditing },
                        onShowPresets = { showPresets = !showPresets },
                        onShowSaveDialog = { showSaveDialog = !showSaveDialog },
                        onProfileNameChange = { profileName = it },
                        onPresetSelected = { preset ->
                            selectedPreset = preset.name
                            preset.bandGains.forEachIndexed { index, gain ->
                                EqRepository.setBandGain(index, gain)
                            }
                            EqRepository.setPreamp(preset.preampDb)
                            showPresets = false
                        },
                        onSaveProfile = {
                            if (profileName.isNotBlank()) {
                                profileManager.saveProfile(
                                    profileName,
                                    state.preampGainDb,
                                    state.bandGains,
                                    state.spatialStrength.toInt() / 10,
                                    state.bassBoostPercent
                                )
                                profileName = ""
                                showSaveDialog = false
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
    isEditing: Boolean,
    showPresets: Boolean,
    showSaveDialog: Boolean,
    profileName: String,
    selectedPreset: String,
    profileManager: ProfileManager,
    onToggleEditing: () -> Unit,
    onShowPresets: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onProfileNameChange: (String) -> Unit,
    onPresetSelected: (com.gilbertcenteno.hifismarteq.model.Preset) -> Unit,
    onSaveProfile: () -> Unit,
    onLoadProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
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
    val profiles = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        profiles.clear()
        profiles.addAll(profileManager.getAllProfiles())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HiFi Smart EQ Pro") },
                actions = {
                    IconButton(onClick = onShowPresets) {
                        Text("🎵", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onToggleEditing) {
                        Text(if (isEditing) "🔓" else "🔒", style = MaterialTheme.typography.titleLarge)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indicador de modo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEditing) Color(0xFF2A2A2A) else Color(0xFF1A1A1A)
                    )
                ) {
                    Text(
                        text = if (isEditing) "🔓 Modo Edición - Puedes ajustar las bandas" else "🔒 Bloqueado - Toca el candado para editar",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Ecualizador vertical grande
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isEditing) "🎛️ Ecualizador de 32 Bandas (Editable)" else "🎛️ Ecualizador de 32 Bandas (Bloqueado)",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        BigVerticalEqualizer(
                            gains = state.bandGains,
                            enabled = state.isEnabled && isEditing,
                            onGainChange = onBandGainChange
                        )
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
                            valueRange = -20f..20f,
                            enabled = isEditing && state.isEnabled
                        )
                    }
                }
            }

            // Bass Boost
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎸 Bass Boost: ${state.bassBoostPercent}%", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = state.bassBoostPercent.toFloat(),
                            onValueChange = { onBassBoostChange(it.roundToInt()) },
                            valueRange = 0f..100f,
                            enabled = isEditing && state.isEnabled
                        )
                    }
                }
            }

            // Compresor
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎚️ Compresor", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.compressor.enabled,
                                onCheckedChange = { onCompressorChange(state.compressor.copy(enabled = it)) },
                                enabled = isEditing
                            )
                            Text("Activado")
                        }
                        if (state.compressor.enabled) {
                            Text("Threshold: ${state.compressor.thresholdDb} dB")
                            Slider(
                                value = state.compressor.thresholdDb,
                                onValueChange = { onCompressorChange(state.compressor.copy(thresholdDb = it)) },
                                valueRange = -60f..0f,
                                enabled = isEditing
                            )
                            Text("Ratio: ${state.compressor.ratio}:1")
                            Slider(
                                value = state.compressor.ratio,
                                onValueChange = { onCompressorChange(state.compressor.copy(ratio = it)) },
                                valueRange = 1f..20f,
                                enabled = isEditing
                            )
                        }
                    }
                }
            }

            // Limitador
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 Limitador", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.limiter.enabled,
                                onCheckedChange = { onLimiterChange(state.limiter.copy(enabled = it)) },
                                enabled = isEditing
                            )
                            Text("Activado")
                        }
                        if (state.limiter.enabled) {
                            Text("Threshold: ${state.limiter.thresholdDb} dB")
                            Slider(
                                value = state.limiter.thresholdDb,
                                onValueChange = { onLimiterChange(state.limiter.copy(thresholdDb = it)) },
                                valueRange = -30f..0f,
                                enabled = isEditing
                            )
                        }
                    }
                }
            }

            // Sonido Espacial
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎧 Sonido Espacial", style = MaterialTheme.typography.titleMedium)
                        Row {
                            Switch(
                                checked = state.isSpatialAudioEnabled,
                                onCheckedChange = onSpatialAudioToggle,
                                enabled = isEditing
                            )
                            Text("Activado")
                        }
                        if (state.isSpatialAudioEnabled) {
                            Text("Intensidad: ${state.spatialStrength.toInt() / 10}%")
                            Slider(
                                value = state.spatialStrength.toFloat(),
                                onValueChange = { onSpatialStrengthChange(it.toInt()) },
                                valueRange = 0f..1000f,
                                enabled = isEditing
                            )
                        }
                    }
                }
            }

            // Stereo Enhance
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎵 Stereo Enhance", style = MaterialTheme.typography.titleMedium)
                        Text("Balance: ${"%.1f".format(state.stereoEnhance.balance)}")
                        Slider(
                            value = state.stereoEnhance.balance,
                            onValueChange = { onStereoEnhanceChange(state.stereoEnhance.copy(balance = it)) },
                            valueRange = -1f..1f,
                            enabled = isEditing
                        )
                        Text("Stereo Width: ${"%.0f".format(state.stereoEnhance.stereoWidth)}%")
                        Slider(
                            value = state.stereoEnhance.stereoWidth,
                            onValueChange = { onStereoEnhanceChange(state.stereoEnhance.copy(stereoWidth = it)) },
                            valueRange = 0f..200f,
                            enabled = isEditing
                        )
                    }
                }
            }

            // Perfiles guardados
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💾 Perfiles Guardados", style = MaterialTheme.typography.titleMedium)
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
                        Button(
                            onClick = onShowSaveDialog,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isEditing
                        ) {
                            Text("💾 Guardar ajuste actual")
                        }
                    }
                }
            }

            // Configuración
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📱 Sesiones: $sessionsCount", style = MaterialTheme.typography.titleMedium)
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

    // Diálogo de presets
    if (showPresets) {
        AlertDialog(
            onDismissRequest = onShowPresets,
            title = { Text("Presets de Ecualización") },
            text = {
                LazyColumn {
                    item {
                        TextButton(onClick = { onPresetSelected(com.gilbertcenteno.hifismarteq.model.Preset("Plano", "Sin modificaciones", 0f, 0, 0, List(32) { 0f })) }) {
                            Text("Plano")
                        }
                    }
                    item {
                        TextButton(onClick = { onPresetSelected(com.gilbertcenteno.hifismarteq.model.Preset("Bass Boost", "Graves potentes", 3f, 80, 0, listOf(8f, 7f, 6f, 5f, 4f, 3f, 2f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)) }) {
                            Text("Bass Boost")
                        }
                    }
                    item {
                        TextButton(onClick = { onPresetSelected(com.gilbertcenteno.hifismarteq.model.Preset("Treble Boost", "Agudos brillantes", 0f, 0, 0, listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 8f, 8f, 8f, 8f)) }) {
                            Text("Treble Boost")
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

    // Diálogo para guardar
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = onShowSaveDialog,
            title = { Text("Guardar Ajuste") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    label = { Text("Nombre del ajuste") }
                )
            },
            confirmButton = {
                Button(onClick = onSaveProfile) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onShowSaveDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BigVerticalEqualizer(
    gains: List<Float>,
    enabled: Boolean,
    onGainChange: (Int, Float) -> Unit
) {
    val frequencies = EqRepository.FREQUENCIES
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        gains.forEachIndexed { index, gain ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Valor de ganancia
                Text(
                    text = "${"%.0f".format(gain)}",
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                )
                
                // Slider vertical GRANDE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .pointerInput(enabled) {
                            if (enabled) {
                                detectDragGestures { change, _ ->
                                    val height = size.height
                                    val newGain = ((height / 2 - change.position.y) / (height / 2) * 15f)
                                        .coerceIn(-15f, 15f)
                                    onGainChange(index, newGain)
                                }
                            }
                        }
                        .background(
                            color = if (enabled) Color(0xFF3A3A3A) else Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    // Línea central
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF666666))
                    )
                    
                    // Barra de ganancia
                    val barHeight = ((gain + 15) / 30) * 320
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(barHeight.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                color = if (gain > 0) Color(0xFF00FF00) else Color(0xFFFF6600),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    
                    // Indicador de posición
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .offset(y = (160 - barHeight).dp)
                            .background(Color(0xFF00BFFF))
                    )
                }
                
                // Frecuencia
                Text(
                    text = if (frequencies[index] >= 1000) 
                        "${"%.1f".format(frequencies[index] / 1000)}k" 
                    else 
                        "${frequencies[index].roundToInt()}",
                    fontSize = 7.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

