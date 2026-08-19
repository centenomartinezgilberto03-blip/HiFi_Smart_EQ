package com.gilbertcenteno.hifismarteq.profile

import android.content.Context
import com.gilbertcenteno.hifismarteq.model.EqProfile
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("custom_profiles", Context.MODE_PRIVATE)

    fun saveProfile(name: String, preampDb: Float, bandGains: List<Float>, spatialStrength: Int, bassBoost: Int): Boolean {
        return try {
            val json = JSONObject().apply {
                put("preamp", preampDb)
                put("spatial", spatialStrength)
                put("bass", bassBoost)
                put("bands", JSONArray().apply {
                    bandGains.forEach { put(it) }
                })
            }
            prefs.edit().putString(name, json.toString()).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadProfile(name: String): Pair<Float, List<Float>>? {
        return try {
            val jsonStr = prefs.getString(name, null) ?: return null
            val json = JSONObject(jsonStr)
            val preamp = json.getDouble("preamp").toFloat()
            val bands = mutableListOf<Float>()
            val arr = json.getJSONArray("bands")
            for (i in 0 until arr.length()) {
                bands.add(arr.getDouble(i).toFloat())
            }
            Pair(preamp, bands)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteProfile(name: String) {
        prefs.edit().remove(name).apply()
    }

    fun getAllProfiles(): List<String> {
        return prefs.all.keys.sorted()
    }

    fun getAllProfilesWithData(): List<Pair<String, Pair<Float, List<Float>>>> {
        return getAllProfiles().mapNotNull { name ->
            loadProfile(name)?.let { name to it }
        }
    }
}
