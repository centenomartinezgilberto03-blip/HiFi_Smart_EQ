package com.gilbertcenteno.hifismarteq.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.gilbertcenteno.hifismarteq.dsp.DynamicsProcessingEngine
import com.gilbertcenteno.hifismarteq.model.DefaultBands
import com.gilbertcenteno.hifismarteq.model.LimiterSettings
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.audio.VisualizerAnalyzer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class HifiEqService : LifecycleService() {
    private val dsp = DynamicsProcessingEngine()
    private val analyzer = VisualizerAnalyzer()
    private lateinit var monitor: AudioPlaybackMonitor
    private var bass: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Buscando sesiones de audio compatibles"))
        monitor = AudioPlaybackMonitor(this)
        monitor.start()
        scope.launch {
            monitor.sessions.collectLatest { sessions ->
                val session = sessions.firstOrNull()
                if (session != null) attach(session.audioSessionId)
                else update("No hay sesión de audio compatible activa")
            }
        }
    }

    private fun attach(sessionId: Int) {
        if (!dsp.attach(sessionId)) {
            update("Android rechazó la asociación DSP para la sesión $sessionId")
            return
        }
        dsp.configure(DefaultBands.values, 0f, LimiterSettings())
        runCatching {
            bass?.release()
            bass = BassBoost(0, sessionId)
            bass?.strength = 0
            bass?.enabled = true
        }
        runCatching {
            virtualizer?.release()
            virtualizer = Virtualizer(0, sessionId)
            virtualizer?.strength = 0
            virtualizer?.enabled = true
        }
        analyzer.attach(sessionId)
        acquireTemporaryWakeLock()
        update("DSP activo · sesión $sessionId")
    }

    private fun acquireTemporaryWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HiFiSmartEQ::DSP")
        wakeLock?.acquire(120_000L)
    }

    private fun update(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text))
    }

    private fun notification(text: String) =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("HiFi Smart EQ")
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "HiFi Smart EQ", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        monitor.stop()
        analyzer.release()
        bass?.release()
        virtualizer?.release()
        dsp.release()
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "hifi_dsp"
        private const val NOTIFICATION_ID = 1001
    }
}
