package com.gilbertcenteno.hifismarteq.audio

import com.gilbertcenteno.hifismarteq.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqState(
    val isEnabled: Boolean = true,
    val preampGainDb: Float = 0f,
    val bandGains: List<Float> = List(32) { 0f },
    val isSpatialAudioEnabled: Boolean = false,
    val spatialAudioSupported: Boolean = false,
    val spatialStrength: Short = 500,
    val bassBoostPercent: Int = 0,
    val compressor: CompressorSettings = CompressorSettings(),
    val limiter: LimiterSettings = LimiterSettings(),
    val reverb: ReverbSettings = ReverbSettings(),
    val stereoEnhance: StereoEnhanceSettings = StereoEnhanceSettings(),
    val bassTreble: BassTrebleSettings = BassTrebleSettings(),
    val metering: MeteringData = MeteringData()
)

object EqRepository {
    val FREQUENCIES = floatArrayOf(
        20f, 25f, 31f, 40f, 50f, 63f, 80f, 100f,
        125f, 160f, 200f, 250f, 315f, 400f, 500f, 630f,
        800f, 1000f, 1250f, 1600f, 2000f, 2500f, 3150f, 4000f,
        5000f, 6300f, 8000f, 10000f, 12500f, 16000f, 18000f, 20000f
    )

    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
    }

    fun setPreamp(gainDb: Float) {
        _state.update { it.copy(preampGainDb = gainDb.coerceIn(-20f, 20f)) }
    }

    fun setBandGain(index: Int, gainDb: Float) {
        _state.update {
            val newGains = it.bandGains.toMutableList()
            if (index in newGains.indices) {
                newGains[index] = gainDb.coerceIn(-15f, 15f)
            }
            it.copy(bandGains = newGains)
        }
    }

    fun setAllBandGains(gains: List<Float>) {
        _state.update {
            val newGains = gains.map { it.coerceIn(-15f, 15f) }.toMutableList()
            while (newGains.size < 32) newGains.add(0f)
            if (newGains.size > 32) newGains.subList(0, 32)
            it.copy(bandGains = newGains.toList())
        }
    }

    fun setSpatialAudioEnabled(enabled: Boolean) = _state.update { it.copy(isSpatialAudioEnabled = enabled) }
    fun setSpatialAudioSupported(supported: Boolean) = _state.update { it.copy(spatialAudioSupported = supported) }
    fun setSpatialStrength(strength: Short) = _state.update { it.copy(spatialStrength = strength) }
    fun setBassBoost(percent: Int) = _state.update { it.copy(bassBoostPercent = percent.coerceIn(0, 100)) }
    fun setCompressor(settings: CompressorSettings) = _state.update { it.copy(compressor = settings) }
    fun setLimiter(settings: LimiterSettings) = _state.update { it.copy(limiter = settings) }
    fun setReverb(settings: ReverbSettings) = _state.update { it.copy(reverb = settings) }
    fun setStereoEnhance(settings: StereoEnhanceSettings) = _state.update { it.copy(stereoEnhance = settings) }
    fun setBassTreble(settings: BassTrebleSettings) = _state.update { it.copy(bassTreble = settings) }
    fun setMetering(data: MeteringData) = _state.update { it.copy(metering = data) }
}
