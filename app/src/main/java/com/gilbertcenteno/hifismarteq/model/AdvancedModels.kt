package com.gilbertcenteno.hifismarteq.model

data class CompressorSettings(
    val enabled: Boolean = false,
    val thresholdDb: Float = -20f,
    val ratio: Float = 4f,
    val attackMs: Float = 10f,
    val releaseMs: Float = 100f,
    val kneeDb: Float = 6f,
    val makeupGainDb: Float = 0f
)

data class LimiterSettings(
    val enabled: Boolean = false,
    val thresholdDb: Float = -1f,
    val ceilingDb: Float = -0.5f,
    val attackMs: Float = 1f,
    val releaseMs: Float = 50f
)

data class StereoEnhanceSettings(
    val balance: Float = 0f,  // -1.0 (left) to 1.0 (right)
    val stereoWidth: Float = 100f,  // 0-200%
    val monoEnabled: Boolean = false
)

data class ReverbSettings(
    val enabled: Boolean = false,
    val roomSize: Int = 50,  // 0-100
    val wetDryMix: Float = 20f,  // 0-100%
    val decay: Float = 50f,  // 0-100%
    val preDelay: Float = 0f  // 0-100ms
)

data class BassTrebleSettings(
    val bassEnabled: Boolean = false,
    val bassGainDb: Float = 0f,
    val bassFrequency: Float = 100f,
    val trebleEnabled: Boolean = false,
    val trebleGainDb: Float = 0f,
    val trebleFrequency: Float = 5000f
)

data class MeteringData(
    val peakLeft: Float = 0f,
    val peakRight: Float = 0f,
    val rmsLeft: Float = 0f,
    val rmsRight: Float = 0f,
    val clipping: Boolean = false
)
