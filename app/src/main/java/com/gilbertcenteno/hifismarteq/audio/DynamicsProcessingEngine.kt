package com.gilbertcenteno.hifismarteq.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.BassBoost
import android.os.Build

object DynamicsProcessingEngine {

    private var globalEq: Equalizer? = null
    private var globalLoudness: LoudnessEnhancer? = null
    private var globalVirtualizer: Virtualizer? = null
    private var globalBassBoost: BassBoost? = null

    private val activeEngines = mutableMapOf<Int, DynamicsProcessing>()
    private val activeVirtualizers = mutableMapOf<Int, Virtualizer>()
    private val activeBassBoost = mutableMapOf<Int, BassBoost>()

    fun checkSpatialAudioSupport(context: Context): Boolean = true

    private fun ensureGlobalEffects() {
        if (globalEq == null) {
            runCatching { globalEq = Equalizer(0, 0).apply { enabled = true } }
        }
        if (globalLoudness == null) {
            runCatching { globalLoudness = LoudnessEnhancer(0).apply { enabled = false } }
        }
        if (globalVirtualizer == null) {
            runCatching { globalVirtualizer = Virtualizer(0, 0).apply { enabled = false } }
        }
        if (globalBassBoost == null) {
            runCatching { globalBassBoost = BassBoost(0, 0).apply { enabled = true } }
        }
    }

    fun attachToSessions(context: Context, sessionIds: List<Int>) {
        ensureGlobalEffects()
        val validSessions = sessionIds.filter { it > 0 }.toSet()

        val engineIter = activeEngines.iterator()
        while (engineIter.hasNext()) {
            val (id, dp) = engineIter.next()
            if (!validSessions.contains(id)) {
                runCatching { dp.enabled = false; dp.release() }
                engineIter.remove()
            }
        }

        val virtIter = activeVirtualizers.iterator()
        while (virtIter.hasNext()) {
            val (id, virt) = virtIter.next()
            if (!validSessions.contains(id)) {
                runCatching { virt.enabled = false; virt.release() }
                virtIter.remove()
            }
        }

        val bassIter = activeBassBoost.iterator()
        while (bassIter.hasNext()) {
            val (id, bass) = bassIter.next()
            if (!validSessions.contains(id)) {
                runCatching { bass.enabled = false; bass.release() }
                bassIter.remove()
            }
        }

        for (id in validSessions) {
            if (!activeEngines.containsKey(id)) {
                val dp = createEngine(id)
                if (dp != null) activeEngines[id] = dp
            }
            if (!activeVirtualizers.containsKey(id)) {
                val virt = createVirtualizer(id)
                if (virt != null) activeVirtualizers[id] = virt
            }
            if (!activeBassBoost.containsKey(id)) {
                val bass = createBassBoost(id)
                if (bass != null) activeBassBoost[id] = bass
            }
        }
    }

    private fun createEngine(sessionId: Int): DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < 28) return null
        return runCatching {
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2, true, 32, false, 1, true, 1, true
            )
            val config = builder.build()
            val dp = DynamicsProcessing(0, sessionId, config)
            dp.enabled = true
            dp
        }.getOrNull()
    }

    private fun createVirtualizer(sessionId: Int): Virtualizer? {
        return runCatching {
            val virt = Virtualizer(0, sessionId)
            virt.enabled = false
            virt
        }.getOrNull()
    }

    private fun createBassBoost(sessionId: Int): BassBoost? {
        return runCatching {
            val bass = BassBoost(0, sessionId)
            bass.enabled = true
            bass.setStrength(0)
            bass
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
        ensureGlobalEffects()

        runCatching {
            globalEq?.enabled = enabled
            if (enabled && globalEq != null) {
                val numBands = globalEq!!.numberOfBands
                for (i in 0 until numBands) {
                    val gain = bandGainsDb.getOrElse(i) { 0f }
                    val levelmB = (gain * 100).toInt().coerceIn(-1500, 1500)
                    globalEq!!.setBandLevel(i.toShort(), levelmB.toShort())
                }
            }
        }

        runCatching {
            globalVirtualizer?.enabled = spatialAudioEnabled && enabled
            if (spatialAudioEnabled && enabled && globalVirtualizer?.strengthSupported == true) {
                globalVirtualizer?.setStrength(spatialStrength)
            }
        }

        if (Build.VERSION.SDK_INT >= 28) {
            for ((_, dp) in activeEngines) {
                runCatching {
                    dp.enabled = enabled
                    if (enabled) {
                        val eq = DynamicsProcessing.Eq(true, true, bandCenterFreqsHz.size)
                        for (i in bandCenterFreqsHz.indices) {
                            val eqBand = DynamicsProcessing.EqBand(
                                true, bandCenterFreqsHz[i], bandGainsDb.getOrElse(i) { 0f }
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
                virt.enabled = spatialAudioEnabled && enabled
                if (spatialAudioEnabled && enabled && virt.strengthSupported) {
                    virt.setStrength(spatialStrength)
                }
            }
        }
    }

    fun release() {
        runCatching { globalEq?.release() }
        runCatching { globalLoudness?.release() }
        runCatching { globalVirtualizer?.release() }
        runCatching { globalBassBoost?.release() }
        globalEq = null
        globalLoudness = null
        globalVirtualizer = null
        globalBassBoost = null

        for ((_, dp) in activeEngines) {
            runCatching { dp.enabled = false; dp.release() }
        }
        activeEngines.clear()

        for ((_, virt) in activeVirtualizers) {
            runCatching { virt.enabled = false; virt.release() }
        }
        activeVirtualizers.clear()

        for ((_, bass) in activeBassBoost) {
            runCatching { bass.enabled = false; bass.release() }
        }
        activeBassBoost.clear()
    }
}
