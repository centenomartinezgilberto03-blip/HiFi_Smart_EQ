package com.gilbertcenteno.hifismarteq.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.DynamicsProcessingEngine
import com.gilbertcenteno.hifismarteq.audio.EqRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HifiEqService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var monitor: AudioPlaybackMonitor
    private val broadcastSessions = mutableSetOf<Int>()

    private val audioSessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            if (sessionId <= 0) return

            when (action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_SESSION -> {
                    broadcastSessions.add(sessionId)
                    updateEngineSessions()
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_SESSION -> {
                    broadcastSessions.remove(sessionId)
                    updateEngineSessions()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()

        monitor = AudioPlaybackMonitor(this)
        monitor.start()

        val spatialSupported = DynamicsProcessingEngine.checkSpatialAudioSupport(this)
        EqRepository.setSpatialAudioSupported(spatialSupported)

        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_SESSION)
        }
        registerReceiver(audioSessionReceiver, filter)

        serviceScope.launch {
            monitor.sessions.collectLatest {
                updateEngineSessions()
            }
        }

        serviceScope.launch {
            EqRepository.state.collectLatest { state ->
                val freq = EqRepository.FREQUENCIES
                val gains = state.bandGains.toFloatArray()
                DynamicsProcessingEngine.applySettings(
                    enabled = state.isEnabled,
                    preampDb = state.preampGainDb,
                    bandCenterFreqsHz = freq,
                    bandGainsDb = gains,
                    spatialAudioEnabled = state.isSpatialAudioEnabled,
                    spatialStrength = state.spatialStrength
                )
            }
        }
    }

    private fun updateEngineSessions() {
        val monitorList = monitor.sessions.value.map { it.audioSessionId }
        val allSessions = (monitorList + broadcastSessions).filter { it > 0 }.distinct()

        val targetSessions = if (allSessions.isNotEmpty()) {
            allSessions
        } else {
            listOf(0)
        }

        DynamicsProcessingEngine.attachToSessions(this, targetSessions)

        val state = EqRepository.state.value
        DynamicsProcessingEngine.applySettings(
            enabled = state.isEnabled,
            preampDb = state.preampGainDb,
            bandCenterFreqsHz = EqRepository.FREQUENCIES,
            bandGainsDb = state.bandGains.toFloatArray(),
            spatialAudioEnabled = state.isSpatialAudioEnabled,
            spatialStrength = state.spatialStrength
        )
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(audioSessionReceiver) }
        monitor.stop()
        DynamicsProcessingEngine.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val channelId = "hifi_eq_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "HiFi Smart EQ Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("HiFi Smart EQ Activo")
            .setContentText("Procesando audio en tiempo real")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }
}
