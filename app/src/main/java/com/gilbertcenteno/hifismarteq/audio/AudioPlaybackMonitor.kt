package com.gilbertcenteno.hifismarteq.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAudioSession(
    val audioSessionId: Int,
    val clientPackageName: String = "Desconocido"
)

class AudioPlaybackMonitor(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _sessions = MutableStateFlow<List<ActiveAudioSession>>(emptyList())
    val sessions: StateFlow<List<ActiveAudioSession>> = _sessions.asStateFlow()

    private val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                super.onPlaybackConfigChanged(configs)
                updateSessions(configs)
            }
        }
    } else null

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && callback != null) {
            runCatching {
                audioManager.registerAudioPlaybackCallback(callback, null)
                updateSessions(audioManager.activePlaybackConfigurations)
            }
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && callback != null) {
            runCatching {
                audioManager.unregisterAudioPlaybackCallback(callback)
            }
        }
    }

    private fun updateSessions(configs: List<AudioPlaybackConfiguration>?) {
        if (configs == null) {
            _sessions.value = emptyList()
            return
        }

        val detectedList = mutableListOf<ActiveAudioSession>()
        for (config in configs) {
            val sessionId = extractAudioSessionId(config)
            if (sessionId > 0) {
                detectedList.add(ActiveAudioSession(audioSessionId = sessionId))
            }
        }

        _sessions.value = detectedList.distinctBy { it.audioSessionId }
    }

    private fun extractAudioSessionId(config: AudioPlaybackConfiguration): Int {
        // Método 1: Reflexión directa sobre getAudioSessionId()
        runCatching {
            val method = config.javaClass.getMethod("getAudioSessionId")
            val id = method.invoke(config) as? Int
            if (id != null && id > 0) return id
        }

        // Método 2: Extracción del dump de configuración en caso de bloqueo por el SO
        runCatching {
            val str = config.toString()
            val regex = Regex("""session:\s*(\d+)""", RegexOption.IGNORE_CASE)
            val match = regex.find(str)
            if (match != null) {
                val id = match.groupValues[1].toIntOrNull()
                if (id != null && id > 0) return id
            }
        }

        return -1
    }
}
