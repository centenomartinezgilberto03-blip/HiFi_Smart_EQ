package com.gilbertcenteno.hifismarteq.audio

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSession(
    val packageName: String,
    val audioSessionId: Int,
    val uid: Int
)

class AudioPlaybackMonitor(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val _sessions = MutableStateFlow<List<PlaybackSession>>(emptyList())
    val sessions: StateFlow<List<PlaybackSession>> = _sessions.asStateFlow()

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            update(configs)
        }
    }

    fun start() {
        if (Build.VERSION.SDK_INT < 26) return
        update(audioManager.activePlaybackConfigurations)
        audioManager.registerAudioPlaybackCallback(callback, null)
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= 26) {
            runCatching { audioManager.unregisterAudioPlaybackCallback(callback) }
        }
    }

    private fun update(configs: List<AudioPlaybackConfiguration>) {
        if (Build.VERSION.SDK_INT < 26) return
        val list = configs.filter {
            it.playerState == AudioPlaybackConfiguration.PLAYER_STATE_STARTED &&
            it.audioSessionId > 0
        }.map { cfg ->
            PlaybackSession(
                packageName = packageForUid(cfg.clientUid),
                audioSessionId = cfg.audioSessionId,
                uid = cfg.clientUid
            )
        }.distinctBy { it.audioSessionId }
        _sessions.value = list
    }

    private fun packageForUid(uid: Int): String {
        val packages = context.packageManager.getPackagesForUid(uid)
        return packages?.firstOrNull() ?: "UID $uid"
    }

    private val context: Context = context.applicationContext
}
