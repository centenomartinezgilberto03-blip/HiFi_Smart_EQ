package com.gilbertcenteno.hifismarteq.model

data class Preset(
    val name: String,
    val description: String = "",
    val preampDb: Float = 0f,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val bandGains: List<Float> = List(10) { 0f }
)

object PresetLibrary {
    val presets = listOf(
        Preset(
            name = "Plano",
            description = "Sin modificaciones",
            preampDb = 0f,
            bassBoostPercent = 0,
            virtualizerPercent = 0,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        ),
        Preset(
            name = "Rock",
            description = "Guitarras potentes y batería",
            preampDb = 2f,
            bassBoostPercent = 30,
            virtualizerPercent = 20,
            bandGains = listOf(4f, 3f, 2f, 1f, 0f, -1f, 0f, 2f, 3f, 4f)
        ),
        Preset(
            name = "Pop",
            description = "Voces claras y ritmo",
            preampDb = 1f,
            bassBoostPercent = 20,
            virtualizerPercent = 15,
            bandGains = listOf(0f, 1f, 2f, 3f, 2f, 0f, -1f, 0f, 1f, 0f)
        ),
        Preset(
            name = "Jazz",
            description = "Sonido cálido y suave",
            preampDb = 0f,
            bassBoostPercent = 15,
            virtualizerPercent = 10,
            bandGains = listOf(2f, 1f, 0f, 0f, 1f, 2f, 3f, 3f, 2f, 1f)
        ),
        Preset(
            name = "Clásica",
            description = "Orquestal y detallada",
            preampDb = 0f,
            bassBoostPercent = 0,
            virtualizerPercent = 30,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f, 4f)
        ),
        Preset(
            name = "Bass Boost",
            description = "Graves profundos",
            preampDb = 3f,
            bassBoostPercent = 80,
            virtualizerPercent = 0,
            bandGains = listOf(8f, 7f, 5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f)
        ),
        Preset(
            name = "Treble Boost",
            description = "Agudos brillantes",
            preampDb = 0f,
            bassBoostPercent = 0,
            virtualizerPercent = 10,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 5f, 7f, 8f)
        ),
        Preset(
            name = "Vocal Boost",
            description = "Voces protagonistas",
            preampDb = 1f,
            bassBoostPercent = 0,
            virtualizerPercent = 5,
            bandGains = listOf(-2f, -1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, -1f)
        ),
        Preset(
            name = "Electrónica",
            description = "Para EDM y dance",
            preampDb = 2f,
            bassBoostPercent = 50,
            virtualizerPercent = 40,
            bandGains = listOf(6f, 5f, 2f, 0f, -1f, 1f, 2f, 4f, 5f, 6f)
        ),
        Preset(
            name = "Hip Hop",
            description = "Bajos potentes y ritmo",
            preampDb = 3f,
            bassBoostPercent = 70,
            virtualizerPercent = 25,
            bandGains = listOf(7f, 6f, 4f, 2f, 0f, 1f, 2f, 1f, 2f, 3f)
        ),
        Preset(
            name = "Películas",
            description = "Sonido cinematográfico",
            preampDb = 1f,
            bassBoostPercent = 40,
            virtualizerPercent = 60,
            bandGains = listOf(3f, 2f, 1f, 0f, 1f, 2f, 3f, 2f, 1f, 0f)
        ),
        Preset(
            name = "Juegos",
            description = "Inmersión total",
            preampDb = 2f,
            bassBoostPercent = 45,
            virtualizerPercent = 70,
            bandGains = listOf(4f, 3f, 2f, 1f, 0f, 1f, 2f, 3f, 4f, 5f)
        )
    )
}
