package com.gilbertcenteno.hifismarteq.audio

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import com.gilbertcenteno.hifismarteq.service.HiFiNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAudioSession(
    val audioSessionId: Int,
    val packageName: String
)

class AudioPlaybackMonitor(private val context: Context) {

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val _sessions = MutableStateFlow<List<ActiveAudioSession>>(emptyList())
    val sessions: StateFlow<List<ActiveAudioSession>> = _sessions.asStateFlow()

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        processControllers(controllers)
    }

    fun start() {
        runCatching {
            val componentName = ComponentName(context, HiFiNotificationListener::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            val initialControllers = mediaSessionManager.getActiveSessions(componentName)
            processControllers(initialControllers)
        }
    }

    fun stop() {
        runCatching {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        }
    }

    private fun processControllers(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            return
        }
        val detected = mutableListOf<ActiveAudioSession>()
        for (controller in controllers) {
            val pkg = controller.packageName ?: "App de Audio"
            // Intentar extraer session ID del token de la sesion de medios
            val sessionId = runCatching {
                val token = controller.sessionToken
                val method = token.javaClass.getMethod("getAudioSessionId")
                method.invoke(token) as? Int
            }.getOrNull() ?: -1

            if (sessionId > 0) {
                detected.add(ActiveAudioSession(sessionId, pkg))
            }
        }
        if (detected.isNotEmpty()) {
            _sessions.value = detected.distinctBy { it.audioSessionId }
        }
    }

    fun addBroadcastSession(sessionId: Int, pkg: String = "Reproductor Multimedia") {
        if (sessionId <= 0) return
        val current = _sessions.value.toMutableList()
        if (current.none { it.audioSessionId == sessionId }) {
            current.add(ActiveAudioSession(sessionId, pkg))
            _sessions.value = current
        }
    }

    fun removeBroadcastSession(sessionId: Int) {
        if (sessionId <= 0) return
        val current = _sessions.value.filter { it.audioSessionId != sessionId }
        _sessions.value = current
    }
}
