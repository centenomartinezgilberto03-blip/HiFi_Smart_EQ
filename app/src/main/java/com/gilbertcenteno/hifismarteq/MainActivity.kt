package com.gilbertcenteno.hifismarteq

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import com.gilbertcenteno.hifismarteq.audio.EqState
import com.gilbertcenteno.hifismarteq.model.*
import com.gilbertcenteno.hifismarteq.profile.ProfileImporter
import com.gilbertcenteno.hifismarteq.profile.ProfileManager
import com.gilbertcenteno.hifismarteq.service.HifiEqService
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var monitor: AudioPlaybackMonitor
    private lateinit var profileManager: ProfileManager

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val result = ProfileImporter.importFromCsv(this, it) ?: ProfileImporter.importFromTxt(this, it)
            result?.let { (name, gains) ->
                EqRepository.setAllBandGains(gains)
                profileManager.saveProfile(name, 0f, gains, 0, 0)
            }
        }
    }

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
            var isDarkMode by remember { mutableStateOf(false) }
            
            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by EqRepository.state.collectAsState()
                    val sessions by monitor.sessions.collectAsState()
                    var isEditing by remember { mutableStateOf(false) }
                    var showSaveDialog by remember { mutableStateOf(false) }
                    var showLoadDialog by remember { mutableStateOf(false) }
                    var profileName by remember { mutableStateOf("") }

                    MainScreen(
                        state = state,
                        sessionsCount = sessions.size,
                        isEditing = isEditing,
                        showSaveDialog = showSaveDialog,
                        showLoadDialog = showLoadDialog,
                        profileName = profileName,
                        isDarkMode = isDarkMode,
                        profileManager = profileManager,
                        onToggleDarkMode = { isDarkMode = !isDarkMode },
                        onToggleEditing = { isEditing = !isEditing },
                        onShowSaveDialog = { showSaveDialog = !showSaveDialog },
                        onShowLoadDialog = { showLoadDialog = !showLoadDialog },
                        onProfileNameChange = { profileName = it },
                        onImportProfile = { filePicker.launch("*/*") },
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
                            showLoadDialog = false
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
    showSaveDialog: Boolean,
    showLoadDialog: Boolean,
    profileName: String,
    isDarkMode: Boolean,
    profileManager: ProfileManager,
    onToggleDarkMode: () -> Unit,
    onToggleEditing: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onShowLoadDialog: () -> Unit,
    onProfileNameChange: (String) -> Unit,
    onImportProfile: () -> Unit,
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
                    IconButton(onClick = onToggleDarkMode) {
                        Text(if (isDarkMode) "☀️" else "🌙", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onImportProfile) {
                        Text("📁", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onShowLoadDialog) {
                        Text("📂", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onShowSaveDialog) {
                        Text("💾", style = MaterialTheme.typography.titleLarge)
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEditing) Color(0xFF2A2A2A) else Color(0xFF1A1A1A)
                    )
                ) {
                    Text(
                        text = if (isEditing) "🔓 Modo Edición - Desliza las bandas" else "🔒 Bloqueado - Toca el candado",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎛️ Ecualizador de 32 Bandas",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Desliza horizontalmente para ver todas las bandas",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ScrollableEqualizer(
                            gains = state.bandGains,
                            enabled = state.isEnabled && isEditing,
                            onGainChange = onBandGainChange
                        )
                    }
                }
            }

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

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Creado por Gilbert Centeno", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                        Text("Versión 1.1.0", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Text("📱 Sesiones: $sessionsCount", style = MaterialTheme.typography.bodySmall)
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
            onDismissRequest = onShowSaveDialog,
            title = { Text("💾 Guardar Configuración") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onProfileNameChange,
                    label = { Text("Nombre de la configuración") }
                )
            },
            confirmButton = {
                Button(onClick = onSaveProfile) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = onShowSaveDialog) { Text("Cancelar") }
            }
        )
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = onShowLoadDialog,
            title = { Text("📂 Configuraciones Guardadas") },
            text = {
                if (profiles.isNotEmpty()) {
                    LazyColumn {
                        profiles.forEach { profile ->
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = { onLoadProfile(profile) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(profile)
                                    }
                                    TextButton(onClick = { onDeleteProfile(profile) }) {
                                        Text("🗑️", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("No hay configuraciones guardadas")
                }
            },
            confirmButton = {
                TextButton(onClick = onShowLoadDialog) { Text("Cerrar") }
            }
        )
    }
}

@Composable
fun ScrollableEqualizer(
    gains: List<Float>,
    enabled: Boolean,
    onGainChange: (Int, Float) -> Unit
) {
    val frequencies = EqRepository.FREQUENCIES
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .height(380.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gains.forEachIndexed { index, gain ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(45.dp)
            ) {
                Text(
                    text = "${"%.0f".format(gain)}",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
                
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
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF666666))
                    )
                    
                    val barHeight = ((gain + 15) / 30) * 320
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(barHeight.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                color = if (gain > 0) Color(0xFF00FF00) else Color(0xFFFF6600),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
                
                Text(
                    text = if (frequencies[index] >= 1000) 
                        "${"%.1f".format(frequencies[index] / 1000)}k" 
                    else 
                        "${frequencies[index].roundToInt()}",
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

