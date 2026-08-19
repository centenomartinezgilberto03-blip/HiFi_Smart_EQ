package com.gilbertcenteno.hifismarteq.profile

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object ProfileImporter {
    fun importFromCsv(context: Context, uri: Uri): Pair<String, List<Float>>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            reader.close()
            
            if (lines.isEmpty()) return null
            
            val name = lines[0].trim()
            val gains = mutableListOf<Float>()
            
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isNotEmpty()) {
                    val values = line.split(",")
                    values.forEach { value ->
                        gains.add(value.trim().toFloatOrNull() ?: 0f)
                    }
                }
            }
            
            if (gains.isEmpty()) return null
            Pair(name, gains)
        } catch (e: Exception) {
            null
        }
    }

    fun importFromTxt(context: Context, uri: Uri): Pair<String, List<Float>>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            reader.close()
            
            if (lines.size < 2) return null
            
            val name = lines[0].replace("#", "").trim()
            val gains = mutableListOf<Float>()
            
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    line.split(" ", "\t", ",").forEach { value ->
                        gains.add(value.trim().toFloatOrNull() ?: 0f)
                    }
                }
            }
            
            if (gains.isEmpty()) return null
            Pair(name, gains)
        } catch (e: Exception) {
            null
        }
    }
}
