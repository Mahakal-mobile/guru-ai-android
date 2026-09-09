package com.guruai.app.data

import com.guruai.app.util.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun chat(userMessage: String, history: List<Pair<String, String>> = emptyList()): String {
        if (apiKey.isBlank()) {
            return "Gemini API key missing. Open Master Settings (password protected) and add your key."
        }
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"

        val contents = JSONArray()
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", Constants.SYSTEM_PROMPT)))
        )
        contents.put(
            JSONObject()
                .put("role", "model")
                .put("parts", JSONArray().put(JSONObject().put("text", "Understood. I'm Guru, ready.")))
        )
        history.takeLast(16).forEach { (role, text) ->
            val r = if (role == "assistant") "model" else "user"
            contents.put(
                JSONObject()
                    .put("role", r)
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        )

        val body = JSONObject()
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.85)
                    .put("maxOutputTokens", 2048)
            )
            .toString()
            .toRequestBody(jsonMedia)

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return "Gemini error ${resp.code}: ${raw.take(200)}"
            }
            return try {
                val root = JSONObject(raw)
                root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } catch (e: Exception) {
                "Could not parse reply: ${e.message}"
            }
        }
    }
}
