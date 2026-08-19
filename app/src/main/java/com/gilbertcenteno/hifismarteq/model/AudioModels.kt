package com.gilbertcenteno.hifismarteq.model

data class EqBand(
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val q: Float = 1f,
    val enabled: Boolean = true
)

data class LimiterSettings(
    val enabled: Boolean = true,
    val thresholdDb: Float = -1f,
    val postGainDb: Float = -0.5f,
    val attackMs: Float = 5f,
    val releaseMs: Float = 80f
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
