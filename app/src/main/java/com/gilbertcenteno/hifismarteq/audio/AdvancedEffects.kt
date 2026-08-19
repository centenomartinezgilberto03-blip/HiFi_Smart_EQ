package com.gilbertcenteno.hifismarteq.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.os.Build

class AdvancedEffects {
    private var bassBoost: BassBoost? = null
    private var reverb: PresetReverb? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    fun init(sessionId: Int): Boolean {
        release()
        return try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
                setStrength(500)
            }
            
            reverb = PresetReverb(0, sessionId).apply {
                enabled = false
                preset = PresetReverb.PRESET_NONE
            }
            
            if (Build.VERSION.SDK_INT >= 28) {
                dynamicsProcessing = DynamicsProcessing(0, sessionId).apply {
                    enabled = true
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setBassBoost(strengthPercent: Int) {
        bassBoost?.setStrength((strengthPercent * 10).coerceIn(0, 1000).toShort())
    }

    fun setReverb(preset: Short, enabled: Boolean) {
        reverb?.let {
            it.enabled = enabled
            if (enabled) it.preset = preset
        }
    }

    fun setStereoWidening(width: Float) {
        // Usando DynamicsProcessing para widening
        if (Build.VERSION.SDK_INT >= 28) {
            dynamicsProcessing?.let { dp ->
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2, true, 10, false, 1, false, 1, true
                ).build()
                dp.updateConfig(config)
            }
        }
    }

    fun release() {
        bassBoost?.release()
        reverb?.release()
        dynamicsProcessing?.release()
        bassBoost = null
        reverb = null
        dynamicsProcessing = null
    }
}
