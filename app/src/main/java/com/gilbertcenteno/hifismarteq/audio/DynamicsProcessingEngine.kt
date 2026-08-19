package com.gilbertcenteno.hifismarteq.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.DynamicsProcessing
import android.os.Build

object DynamicsProcessingEngine {

    private var fallbackEq: Equalizer? = null
    private var fallbackLoudness: LoudnessEnhancer? = null
    private var fallbackVirtualizer: Virtualizer? = null

    private val activeEngines = mutableMapOf<Int, DynamicsProcessing>()
    private val activeVirtualizers = mutableMapOf<Int, Virtualizer>()

    fun checkSpatialAudioSupport(context: Context): Boolean = true

    private fun ensureFallbackEffects() {
        if (fallbackEq == null) {
            runCatching { fallbackEq = Equalizer(0, 0).apply { enabled = true } }
        }
        if (fallbackLoudness == null) {
            runCatching { fallbackLoudness = LoudnessEnhancer(0).apply { enabled = true } }
        }
        if (fallbackVirtualizer == null) {
            runCatching { fallbackVirtualizer = Virtualizer(0, 0).apply { enabled = false } }
        }
    }

    fun attachToSessions(context: Context, sessionIds: List<Int>) {
        ensureFallbackEffects()
        val currentSessions = sessionIds.filter { it > 0 }.toSet()

        val engineIter = activeEngines.iterator()
        while (engineIter.hasNext()) {
            val (id, dp) = engineIter.next()
            if (!currentSessions.contains(id)) {
                runCatching { dp.enabled = false; dp.release() }
                engineIter.remove()
            }
        }

        val virtIter = activeVirtualizers.iterator()
        while (virtIter.hasNext()) {
            val (id, virt) = virtIter.next()
            if (!currentSessions.contains(id)) {
                runCatching { virt.enabled = false; virt.release() }
                virtIter.remove()
            }
        }

        for (id in currentSessions) {
            if (!activeEngines.containsKey(id)) {
                val dp = createEngine(id)
                if (dp != null) activeEngines[id] = dp
            }
            if (!activeVirtualizers.containsKey(id)) {
                val virt = createVirtualizer(id)
                if (virt != null) activeVirtualizers[id] = virt
            }
        }
    }

    private fun createEngine(sessionId: Int): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < 28) return null
        return runCatching {
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                1,
                false, 0,
                true, 10,
                false, 0,
                true
            )
            val config = builder.build()
            val dp = DynamicsProcessing(1000, sessionId, config)
            dp.enabled = true
            dp
        }.getOrNull()
    }

    private fun createVirtualizer(sessionId: Int): Virtualizer? {
        return runCatching {
            val virt = Virtualizer(1000, sessionId)
            virt.enabled = false
            virt
        }.getOrNull()
    }

    fun applySettings(
        enabled: Boolean,
        preampDb: Float,
        bandCenterFreqsHz: FloatArray,
        bandGainsDb: FloatArray,
        spatialAudioEnabled: Boolean,
        spatialStrength: Short
    ) {
        ensureFallbackEffects()

        // 1. Aplicar a la sesión global (0) como respaldo
        runCatching {
            fallbackEq?.enabled = enabled
            if (enabled && fallbackEq != null) {
                val numBands = fallbackEq!!.numberOfBands
                for (i in 0 until numBands) {
                    val gain = bandGainsDb.getOrElse(i) { 0f }
                    val levelmB = (gain * 100).toInt().coerceIn(-1500, 1500)
                    fallbackEq!!.setBandLevel(i.toShort(), levelmB.toShort())
                }
            }
        }

        runCatching {
            fallbackLoudness?.enabled = enabled
            if (enabled && fallbackLoudness != null) {
                val gainmB = (preampDb * 100).toInt().coerceIn(0, 2000)
                fallbackLoudness!!.setTargetGain(gainmB)
            }
        }

        runCatching {
            if (fallbackVirtualizer != null) {
                fallbackVirtualizer!!.enabled = spatialAudioEnabled && enabled
                if (spatialAudioEnabled && enabled && fallbackVirtualizer!!.strengthSupported) {
                    fallbackVirtualizer!!.setStrength(spatialStrength)
                }
            }
        }

        // 2. Aplicar a las sesiones activas detectadas (> 0)
        if (Build.VERSION.SDK_INT >= 28) {
            for ((_, dp) in activeEngines) {
                runCatching {
                    dp.enabled = enabled
                    if (enabled) {
                        val eq = DynamicsProcessing.Eq(true, true, bandCenterFreqsHz.size)
                        for (i in bandCenterFreqsHz.indices) {
                            val eqBand = DynamicsProcessing.EqBand(
                                true,
                                bandCenterFreqsHz[i],
                                preampDb + bandGainsDb.getOrElse(i) { 0f }
                            )
                            eq.setBand(i, eqBand)
                        }
                        dp.setPreEqAllChannelsTo(eq)
                    }
                }
            }
        }

        for ((_, virt) in activeVirtualizers) {
            runCatching {
                if (spatialAudioEnabled && enabled) {
                    virt.enabled = true
                    if (virt.strengthSupported) {
                        virt.setStrength(spatialStrength)
                    }
                } else {
                    virt.enabled = false
                }
            }
        }
    }

    fun release() {
        runCatching { fallbackEq?.release() }
        runCatching { fallbackLoudness?.release() }
        runCatching { fallbackVirtualizer?.release() }
        fallbackEq = null
        fallbackLoudness = null
        fallbackVirtualizer = null

        for ((_, dp) in activeEngines) {
            runCatching { dp.enabled = false; dp.release() }
        }
        activeEngines.clear()

        for ((_, virt) in activeVirtualizers) {
            runCatching { virt.enabled = false; virt.release() }
        }
        activeVirtualizers.clear()
    }
}
