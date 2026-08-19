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

    private val callback: AudioManager.AudioPlaybackCallback? = if (Build.VERSION.SDK_INT >= 26) {
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                update(configs)
            }
        }
    } else null

    fun start() {
        if (Build.VERSION.SDK_INT >= 26 && callback != null && audioManager != null) {
            val activeConfigs = runCatching { audioManager.activePlaybackConfigurations }.getOrNull() ?: emptyList()
            update(activeConfigs)
            audioManager.registerAudioPlaybackCallback(callback, null)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= 26 && callback != null && audioManager != null) {
            runCatching { audioManager.unregisterAudioPlaybackCallback(callback) }
        }
    }

    private fun update(configs: List<AudioPlaybackConfiguration>) {
        if (Build.VERSION.SDK_INT < 26) return
        val list = configs.mapNotNull { cfg ->
            val sessionId = getAudioSessionIdReflection(cfg)
            if (sessionId > 0) {
                val uid = getClientUidReflection(cfg)
                PlaybackSession(
                    packageName = packageForUid(uid),
                    audioSessionId = sessionId,
                    uid = uid
                )
            } else null
        }.distinctBy { it.audioSessionId }
        _sessions.value = list
    }

    private fun getAudioSessionIdReflection(cfg: AudioPlaybackConfiguration): Int {
        return runCatching {
            val method = cfg.javaClass.methods.firstOrNull { 
                it.name == "getAudioSessionId" || it.name == "getClientAudioSessionId" || it.name == "getPlayerSessionId"
            }
            if (method != null) {
                (method.invoke(cfg) as? Int) ?: 0
            } else {
                val field = cfg.javaClass.declaredFields.firstOrNull { 
                    it.name == "mPlayerSessionId" || it.name == "mAudioSessionId" || it.name == "mSessionId"
                }
                field?.isAccessible = true
                (field?.get(cfg) as? Int) ?: 0
            }
        }.getOrDefault(0)
    }

    private fun getClientUidReflection(cfg: AudioPlaybackConfiguration): Int {
        return runCatching {
            val method = cfg.javaClass.methods.firstOrNull { it.name == "getClientUid" || it.name == "getUid" }
            if (method != null) {
                (method.invoke(cfg) as? Int) ?: -1
            } else {
                val field = cfg.javaClass.declaredFields.firstOrNull { it.name == "mClientUid" || it.name == "mUid" }
                field?.isAccessible = true
                (field?.get(cfg) as? Int) ?: -1
            }
        }.getOrDefault(-1)
    }

    private fun packageForUid(uid: Int): String {
        if (uid <= 0) return "Sesión Global"
        val packages = appContext.packageManager.getPackagesForUid(uid)
        return packages?.firstOrNull() ?: "UID $uid"
    }
}
