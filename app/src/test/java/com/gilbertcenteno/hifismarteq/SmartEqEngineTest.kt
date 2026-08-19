package com.gilbertcenteno.hifismarteq

import com.gilbertcenteno.hifismarteq.dsp.SmartEqEngine
import com.gilbertcenteno.hifismarteq.model.EqBand
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartEqEngineTest {
    @Test fun clampsValues() {
        val b = SmartEqEngine.validate(EqBand(99999f, 99f, 99f))
        assertEquals(20000f, b.frequencyHz)
        assertEquals(12f, b.gainDb)
        assertEquals(10f, b.q)
    }
    @Test fun smoothingWorks() {
        assertEquals(2f, SmartEqEngine.smooth(0f, 10f, .2f), .001f)
    }
}
