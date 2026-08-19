package com.gilbertcenteno.hifismarteq.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gilbertcenteno.hifismarteq.audio.AudioPlaybackMonitor
import com.gilbertcenteno.hifismarteq.dsp.SmartEqEngine
import com.gilbertcenteno.hifismarteq.model.DefaultBands
import com.gilbertcenteno.hifismarteq.model.EqBand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EqViewModel(app: Application) : AndroidViewModel(app) {
    private val monitor = AudioPlaybackMonitor(app)
    private val _bands = MutableStateFlow(DefaultBands.values)
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()
    private val _smart = MutableStateFlow(false)
    val smart: StateFlow<Boolean> = _smart.asStateFlow()
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    init { monitor.start() }

    fun sessions() = monitor.sessions

    fun updateBand(index: Int, value: EqBand) {
        if (index !in 0 until _bands.value.size) return
        val list = _bands.value.toMutableList()
        list[index] = SmartEqEngine.validate(value)
        _bands.value = list
    }

    fun setSmart(value: Boolean) { _smart.value = value }
    fun setEnabled(value: Boolean) { _enabled.value = value }

    override fun onCleared() {
        monitor.stop()
        super.onCleared()
    }
}
