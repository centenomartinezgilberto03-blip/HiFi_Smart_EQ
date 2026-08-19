package com.gilbertcenteno.hifismarteq.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VisualizerAnalyzer {
    private var visualizer: Visualizer? = null
    private val _fft = MutableStateFlow(ByteArray(0))
    val fft: StateFlow<ByteArray> = _fft.asStateFlow()

    fun attach(sessionId: Int): Boolean {
        release()
        if (sessionId <= 0) return false
        return runCatching {
            val v = Visualizer(sessionId)
            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range[1].coerceAtMost(4096)
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) _fft.value = fft.copyOf()
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            v.enabled = true
            visualizer = v
            true
        }.getOrDefault(false)
    }

    fun release() {
        runCatching { visualizer?.enabled = false }
        runCatching { visualizer?.release() }
        visualizer = null
        _fft.value = ByteArray(0)
    }
}
