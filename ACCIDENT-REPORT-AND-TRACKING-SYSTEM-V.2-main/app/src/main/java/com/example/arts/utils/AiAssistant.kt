package com.example.arts.utils

import android.content.Context
import android.util.Log
import com.example.arts.BuildConfig
import com.example.arts.data.IncidentAiSuggestion
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI Assistant Helper. Handles communication with the configured AI service.
 * Supports a custom API key from Settings or local.properties.
 * Falls back to reliable local triage rules if no key is present or service is offline.
 */
object AiAssistant {

    private const val TAG = "ARTS-AI"
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent"
    
    private val VALID_PRIORITIES = setOf("High", "Medium", "Low")
    private val VALID_TYPES = setOf("Collision", "Pedestrian", "Breakdown")

    private fun getApiKey(context: Context): String {
        val customKey = PrefsHelper.getCustomAiKey(context)
        if (customKey.isNotBlank()) return customKey
        
        return try {
            BuildConfig.ARTS_AI_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    private fun getModel(): String {
        return try {
            val model = BuildConfig.ARTS_AI_MODEL
            if (model.isBlank()) "gemini-1.5-flash" else model
        } catch (e: Throwable) {
            "gemini-1.5-flash"
        }
    }

    /**
     * Analyzes user input to suggest incident type and priority.
     */
    suspend fun analyzeIncident(context: Context, userInput: String): IncidentAiSuggestion = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            Log.i(TAG, "No AI key found, using local rules.")
            return@withContext localTriage(userInput)
        }
        
        try {
            callAiService(apiKey, userInput)
        } catch (e: Exception) {
            Log.w(TAG, "AI Service failed: ${e.message}. Falling back.")
            localTriage(userInput)
        }
    }

    private fun callAiService(apiKey: String, description: String): IncidentAiSuggestion {
        val url = URL(ENDPOINT.format(getModel()))
        val body = mapOf(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to buildPrompt(description))))),
            "generationConfig" to mapOf("responseMimeType" to "application/json", "temperature" to 0.1)
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            connection.outputStream.use { it.write(Gson().toJson(body).toByteArray()) }
            if (connection.responseCode !in 200..299) {
                throw IOException("AI Service error: HTTP ${connection.responseCode}")
            }
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            return parseAiResponse(responseText, description)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildPrompt(text: String): String = """
        Analyze this road accident description and return a JSON object with:
        "priority": "High", "Medium", or "Low"
        "type": "Collision", "Pedestrian", or "Breakdown"
        "refinedDescription": A professional 1-2 sentence summary.

        Description: "$text"
    """.trimIndent()

    private fun parseAiResponse(json: String, fallbackText: String): IncidentAiSuggestion {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val candidates = root.getAsJsonArray("candidates")
            if (candidates == null || candidates.size() == 0) return localTriage(fallbackText)
            
            val content = candidates[0].asJsonObject
                .getAsJsonObject("content")
                .getAsJsonArray("parts")[0].asJsonObject
                .get("text").asString
                
            val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val result = JsonParser.parseString(cleaned).asJsonObject

            val priority = result.get("priority")?.asString?.trim() ?: "Medium"
            val type = result.get("type")?.asString?.trim() ?: "Collision"
            val refined = result.get("refinedDescription")?.asString?.trim() ?: fallbackText

            IncidentAiSuggestion(
                priority = if (priority in VALID_PRIORITIES) priority else "Medium",
                type = if (type in VALID_TYPES) type else "Collision",
                refinedDescription = refined.ifBlank { tidy(fallbackText) },
                source = "AI"
            )
        } catch (e: Exception) {
            localTriage(fallbackText)
        }
    }

    /** Reliable local rules used when API is unavailable. */
    fun localTriage(text: String): IncidentAiSuggestion {
        val lower = text.lowercase()
        val type = when {
            listOf("person", "walk", "child", "hit", "pedestrian", "crossing").any { lower.contains(it) } -> "Pedestrian"
            listOf("engine", "tire", "tyre", "stalled", "smoke", "breakdown", "broken").any { lower.contains(it) } -> "Breakdown"
            else -> "Collision"
        }
        
        val isEmergency = listOf("fire", "trapped", "blood", "die", "death", "hurt", "unconscious", "explosion", "bleeding").any { lower.contains(it) }
        val priority = if (isEmergency) "High" else if (type == "Breakdown") "Low" else "Medium"
        
        return IncidentAiSuggestion(priority, type, tidy(text), source = "Local System")
    }

    fun tidy(text: String): String {
        val t = text.trim().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return ""
        val capitalized = t.replaceFirstChar { it.uppercase() }
        return if (capitalized.last() in ".!?") capitalized else "$capitalized."
    }
}
