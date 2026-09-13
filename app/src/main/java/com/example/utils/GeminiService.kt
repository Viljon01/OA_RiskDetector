package com.example.utils

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

import kotlinx.coroutines.delay

object GeminiService {
    suspend fun getAiSummary(prompt: String): String = withContext(Dispatchers.IO) {
        var attempt = 0
        var lastException: Exception? = null
        var lastErrorString = ""
        
        while (attempt < 3) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY as? String ?: ""
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder")) {
                return@withContext "{\"error\": \"Gemini API Key is missing. Please add it to your project Secrets.\"}"
            }

            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 30000 // 30 seconds
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Build JSON payload
            val part = JSONObject().put("text", prompt)
            val partsArray = JSONArray().put(part)
            val content = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(content)
            
            // Limit output and ask for JSON
            val responseFormat = JSONObject()
                .put("type", "OBJECT")
                .put("properties", JSONObject()
                    .put("summary", JSONObject().put("type", "STRING").put("description", "A concise professional summary of the assessment."))
                    .put("recommendations", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")).put("description", "A list of actionable recommendations for the patient."))
                    .put("patientRecommendations", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")).put("description", "Simple, actionable advice to deliver directly to the patient."))
                    .put("prescription", JSONObject().put("type", "STRING").put("description", "Suggested medical prescription or therapy plan."))
                    .put("riskScore", JSONObject().put("type", "INTEGER").put("description", "A risk score from 0 to 100."))
                    .put("riskTier", JSONObject().put("type", "STRING").put("description", "Either LOW RISK, MODERATE RISK, or HIGH RISK"))
                    .put("riskReasoning", JSONObject().put("type", "STRING").put("description", "Clarification and detailed reasons for the assigned risk score and tier based on the assessment data."))
                )

            val generationConfig = JSONObject()
                .put("temperature", 0.2)
                .put("maxOutputTokens", 1500)
                .put("responseMimeType", "application/json")
                .put("responseSchema", responseFormat)

            val payload = JSONObject()
                .put("contents", contentsArray)
                .put("generationConfig", generationConfig)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "{}")
                    }
                }
                return@withContext "{\"error\": \"Response format was unexpected.\"}"
            } else {
                val errorString = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                lastErrorString = "Failed to generate summary. HTTP $responseCode"
                if (responseCode == 503 || responseCode == 429 || responseCode == 500) {
                    attempt++
                    delay(1500L * attempt)
                    continue
                } else {
                    return@withContext "{\"error\": \"Failed to generate summary. HTTP $responseCode\"}"
                }
            }
        } catch (e: Exception) {
            lastException = e
            e.printStackTrace()
            if (e.message?.contains("timeout", ignoreCase = true) == true || e.message?.contains("503") == true) {
                attempt++
                delay(1500L * attempt)
                continue
            } else {
                return@withContext "{\"error\": \"Error connecting to AI service: ${e.message}\"}"
            }
        }
        }
        return@withContext "{\"error\": \"${lastErrorString.ifBlank { lastException?.message ?: "Unknown Error" }}\"}"
    }
}
