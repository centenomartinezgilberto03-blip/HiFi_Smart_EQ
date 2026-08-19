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
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val _sessions = MutableStateFlow<List<PlaybackSession>>(emptyList())
    val sessions: StateFlow<List<PlaybackSession>> = _sessions.asStateFlow()

    private val callback = if (Build.VERSION.SDK_INT >= 26) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                update(configs)
            }
        }
    } else null

    fun start() {
        if (Build.VERSION.SDK_INT >= 26 && callback != null) {
            update(audioManager.activePlaybackConfigurations)
            audioManager.registerAudioPlaybackCallback(callback, null)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= 26 && callback != null) {
            runCatching { audioManager.unregisterAudioPlaybackCallback(callback) }
        }
    }

    private fun update(configs: List<AudioPlaybackConfiguration>) {
        if (Build.VERSION.SDK_INT < 26) return
        val list = configs.filter { cfg ->
            cfg.audioSessionId > 0
        }.map { cfg ->
            val uid = getClientUidReflection(cfg)
            PlaybackSession(
                packageName = packageForUid(uid),
                audioSessionId = cfg.audioSessionId,
                uid = uid
            )
        }.distinctBy { it.audioSessionId }
        _sessions.value = list
    }

    private fun getClientUidReflection(cfg: AudioPlaybackConfiguration): Int {
        return runCatching {
            val method = cfg.javaClass.getMethod("getClientUid")
            method.invoke(cfg) as Int
        }.getOrDefault(-1)
    }

    private fun packageForUid(uid: Int): String {
        if (uid <= 0) return "Sesión Global"
        val packages = appContext.packageManager.getPackagesForUid(uid)
        return packages?.firstOrNull() ?: "UID $uid"
    }
}
