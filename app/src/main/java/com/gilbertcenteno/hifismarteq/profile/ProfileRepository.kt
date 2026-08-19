package com.gilbertcenteno.hifismarteq.profile

import android.content.Context
import com.gilbertcenteno.hifismarteq.model.EqProfile
import org.json.JSONArray
import org.json.JSONObject

class ProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun save(profile: EqProfile) {
        prefs.edit().putString(profile.name, encode(profile).toString()).apply()
    }

    fun delete(name: String) { prefs.edit().remove(name).apply() }

    fun names(): List<String> = prefs.all.keys.sorted()

    private fun encode(p: EqProfile): JSONObject {
        val bands = JSONArray()
        p.bands.forEach {
            bands.put(JSONObject().apply {
                put("f", it.frequencyHz); put("g", it.gainDb); put("q", it.q); put("e", it.enabled)
            })
        }
        return JSONObject().apply {
            put("preamp", p.preampDb)
            put("bass", p.bassBoostPercent)
            put("virt", p.virtualizerPercent)
            put("lim", JSONObject().apply {
                put("enabled", p.limiter.enabled)
                put("threshold", p.limiter.thresholdDb)
                put("post", p.limiter.postGainDb)
                put("attack", p.limiter.attackMs)
                put("release", p.limiter.releaseMs)
            })
            put("bands", bands)
        }
    }
}
