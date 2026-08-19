package com.gilbertcenteno.hifismarteq.model

data class EqBand(
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val q: Float = 1f,
    val enabled: Boolean = true
)

data class LimiterSettings(
    val enabled: Boolean = false,
    val thresholdDb: Float = -1f,
    val ceilingDb: Float = -0.5f,
    val postGainDb: Float = 0f,
    val attackMs: Float = 5f,
    val releaseMs: Float = 80f
)

data class CompressorSettings(
    val enabled: Boolean = false,
    val thresholdDb: Float = -20f,
    val ratio: Float = 4f,
    val attackMs: Float = 10f,
    val releaseMs: Float = 100f,
    val kneeDb: Float = 6f,
    val makeupGainDb: Float = 0f
)

data class StereoEnhanceSettings(
    val balance: Float = 0f,
    val stereoWidth: Float = 100f,
    val monoEnabled: Boolean = false
)

data class ReverbSettings(
    val enabled: Boolean = false,
    val roomSize: Int = 50,
    val wetDryMix: Float = 20f,
    val decay: Float = 50f,
    val preDelay: Float = 0f
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

data class EqProfile(
    val name: String,
    val preampDb: Float = 0f,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val limiter: LimiterSettings = LimiterSettings(),
    val bands: List<EqBand>
)

object DefaultBands {
    val values = listOf(
        EqBand(31f), EqBand(62f), EqBand(125f), EqBand(250f), EqBand(500f),
        EqBand(1000f), EqBand(2000f), EqBand(4000f), EqBand(8000f), EqBand(16000f)
    )
}
