package com.gilbertcenteno.hifismarteq.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EqState(
    val isEnabled: Boolean = true,
    val preampGainDb: Float = 0f,
    val bandGains: List<Float> = List(10) { 0f },
    val isSpatialAudioEnabled: Boolean = false,
    val spatialAudioSupported: Boolean = false,
    val spatialStrength: Short = 500,
    val bassBoostPercent: Int = 0
)

object EqRepository {
    val FREQUENCIES = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
    }

    fun setPreamp(gainDb: Float) {
        _state.update { it.copy(preampGainDb = gainDb) }
    }

    fun setBandGain(index: Int, gainDb: Float) {
        _state.update {
            val newGains = it.bandGains.toMutableList()
            if (index in newGains.indices) {
                newGains[index] = gainDb.coerceIn(-12f, 12f)
            }
            it.copy(bandGains = newGains)
        }
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        _state.update { it.copy(isSpatialAudioEnabled = enabled) }
    }

    fun setSpatialAudioSupported(supported: Boolean) {
        _state.update { it.copy(spatialAudioSupported = supported) }
    }

    fun setSpatialStrength(strength: Short) {
        _state.update { it.copy(spatialStrength = strength) }
    }

    fun setBassBoost(percent: Int) {
        _state.update { it.copy(bassBoostPercent = percent.coerceIn(0, 100)) }
    }

    fun applyPreset(gains: List<Float>, preamp: Float, spatial: Short, bass: Int) {
        _state.update {
            it.copy(
                preampGainDb = preamp,
                bandGains = gains.toList(),
                spatialStrength = spatial,
                bassBoostPercent = bass
            )
        }
    }
}
