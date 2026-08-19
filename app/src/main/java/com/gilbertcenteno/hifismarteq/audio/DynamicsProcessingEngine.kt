package com.gilbertcenteno.hifismarteq.audio

import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Virtualizer
import android.os.Build

object DynamicsProcessingEngine {

    private val activeEngines = mutableMapOf<Int, DynamicsProcessing>()
    private val activeVirtualizers = mutableMapOf<Int, Virtualizer>()

    fun checkSpatialAudioSupport(context: Context): Boolean = true

    fun attachToSessions(context: Context, sessionIds: List<Int>) {
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

        for (id in validSessions) {
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
            val dp = DynamicsProcessing(10000, sessionId, config)
            dp.enabled = true
            dp
        }.getOrNull()
    }

    private fun createVirtualizer(sessionId: Int): Virtualizer? {
        return runCatching {
            val virt = Virtualizer(10000, sessionId)
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
