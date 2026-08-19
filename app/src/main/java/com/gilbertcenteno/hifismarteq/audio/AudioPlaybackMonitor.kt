package com.gilbertcenteno.hifismarteq.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAudioSession(
    val audioSessionId: Int
)

class AudioPlaybackMonitor(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _sessions = MutableStateFlow<List<ActiveAudioSession>>(emptyList())
    val sessions: StateFlow<List<ActiveAudioSession>> = _sessions.asStateFlow()

    private val callback: AudioManager.AudioPlaybackCallback? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
                super.onPlaybackConfigChanged(configs)
                updateFromConfigs(configs)
            }
        }
    } else null

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && callback != null) {
            runCatching {
                audioManager.registerAudioPlaybackCallback(callback!!, null)
                updateFromConfigs(audioManager.activePlaybackConfigurations)
            }
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && callback != null) {
            runCatching {
                audioManager.unregisterAudioPlaybackCallback(callback!!)
            }
        }
    }

    private fun updateFromConfigs(configs: List<AudioPlaybackConfiguration>?) {
        if (configs.isNullOrEmpty()) {
            _sessions.value = emptyList()
            return
        }

        val detected = mutableListOf<ActiveAudioSession>()
        for (config in configs) {
            val sessionId = extractSessionId(config)
            if (sessionId > 0) {
                detected.add(ActiveAudioSession(sessionId))
            }
        }
        _sessions.value = detected.distinctBy { it.audioSessionId }
    }

    private fun extractSessionId(config: AudioPlaybackConfiguration): Int {
        return runCatching {
            val method = config.javaClass.getMethod("getAudioSessionId")
            val id = method.invoke(config) as? Int
            if (id != null && id > 0) id else -1
        }.getOrDefault(-1)
    }

    fun addBroadcastSession(sessionId: Int, pkg: String) {
        addSession(sessionId)
    }

    fun removeBroadcastSession(sessionId: Int) {
        removeSession(sessionId)
    }

    fun addSession(sessionId: Int) {
        if (sessionId <= 0) return
        val current = _sessions.value.toMutableList()
        if (current.none { it.audioSessionId == sessionId }) {
            current.add(ActiveAudioSession(sessionId))
            _sessions.value = current
        }
    }

    fun removeSession(sessionId: Int) {
        if (sessionId <= 0) return
        _sessions.value = _sessions.value.filter { it.audioSessionId != sessionId }
    }
}


