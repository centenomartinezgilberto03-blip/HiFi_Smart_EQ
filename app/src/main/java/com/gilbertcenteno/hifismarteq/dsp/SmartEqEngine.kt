package com.gilbertcenteno.hifismarteq.dsp

import com.gilbertcenteno.hifismarteq.model.EqBand
import kotlin.math.ln
import kotlin.math.sqrt

object SmartEqEngine {
    fun spectrumMagnitudeDb(fft: ByteArray): FloatArray {
        if (fft.size < 4) return FloatArray(0)
        val bins = fft.size / 2
        val result = FloatArray(bins)
        for (i in 0 until bins) {
            val re = fft[2 * i].toInt().toFloat()
            val im = fft[2 * i + 1].toInt().toFloat()
            val mag = sqrt(re * re + im * im).coerceAtLeast(1f)
            result[i] = (20.0 * kotlin.math.log10(mag / 128.0)).toFloat()
        }
        return result
    }

    fun suggest(bands: List<EqBand>, spectrumDb: FloatArray): List<EqBand> {
        if (bands.size != 10 || spectrumDb.isEmpty()) return bands
        val average = spectrumDb.average().toFloat()
        return bands.mapIndexed { index, band ->
            val sample = spectrumDb[(index * spectrumDb.size / bands.size).coerceIn(0, spectrumDb.lastIndex)]
            val correction = ((average - sample) * 0.25f).coerceIn(-6f, 6f)
            band.copy(gainDb = smooth(band.gainDb, correction, 0.15f))
        }
    }

    fun smooth(old: Float, target: Float, amount: Float): Float =
        old + (target - old) * amount.coerceIn(0f, 1f)

    fun validate(band: EqBand): EqBand =
        band.copy(
            frequencyHz = band.frequencyHz.coerceIn(20f, 20000f),
            gainDb = band.gainDb.coerceIn(-12f, 12f),
            q = band.q.coerceIn(0.1f, 10f)
        )

    fun xForFrequency(frequency: Float): Float {
        val min = ln(20.0)
        val max = ln(20000.0)
        return ((ln(frequency.coerceIn(20f, 20000f).toDouble()) - min) / (max - min)).toFloat()
    }
}
