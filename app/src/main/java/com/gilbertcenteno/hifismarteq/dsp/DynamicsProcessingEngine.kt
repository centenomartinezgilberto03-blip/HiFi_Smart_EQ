package com.gilbertcenteno.hifismarteq.dsp

import android.media.audiofx.DynamicsProcessing
import com.gilbertcenteno.hifismarteq.model.EqBand
import com.gilbertcenteno.hifismarteq.model.LimiterSettings

class DynamicsProcessingEngine {
    private var effect: DynamicsProcessing? = null

    fun attach(sessionId: Int): Boolean {
        if (sessionId <= 0) return false
        release()
        return runCatching {
            val cfg = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,
                true, 10,
                false, 1,
                false, 1,
                true
            ).build()
            effect = DynamicsProcessing(0, sessionId, cfg).apply { enabled = true }
            true
        }.getOrDefault(false)
    }

    fun configure(bands: List<EqBand>, preampDb: Float, limiter: LimiterSettings): Boolean {
        val dp = effect ?: return false
        if (bands.size != 10) return false
        return runCatching {
            dp.setInputGainAllChannelsTo(preampDb.coerceIn(-12f, 12f))
            val eq = DynamicsProcessing.Eq(true, true, 10)
            bands.sortedBy { it.frequencyHz }.forEachIndexed { i, b ->
                eq.setBand(
                    i,
                    DynamicsProcessing.EqBand(
                        b.enabled,
                        b.frequencyHz.coerceIn(20f, 20000f),
                        b.gainDb.coerceIn(-12f, 12f)
                    )
                )
            }
            dp.setPreEqAllChannelsTo(eq)
            dp.setLimiterAllChannelsTo(
                DynamicsProcessing.Limiter(
                    true,
                    limiter.enabled,
                    0,
                    limiter.attackMs.coerceIn(0.1f, 100f),
                    limiter.releaseMs.coerceIn(1f, 1000f),
                    20f,
                    limiter.thresholdDb.coerceIn(-40f, 0f),
                    limiter.postGainDb.coerceIn(-12f, 0f)
                )
            )
            true
        }.getOrDefault(false)
    }

    fun setEnabled(value: Boolean): Boolean =
        runCatching { effect?.enabled = value; effect != null }.getOrDefault(false)

    fun release() {
        effect?.release()
        effect = null
    }
}
