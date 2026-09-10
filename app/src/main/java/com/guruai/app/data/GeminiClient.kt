package com.guruai.app.data

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import com.guruai.app.util.Constants
import kotlinx.coroutines.delay
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

    suspend fun chat(userMessage: String, history: List<Pair<String, String>> = emptyList()): String {
        if (apiKey.isBlank()) {
            return "Gemini API key missing. Open Master Settings (password protected) and add your key."
        }
        return retryingCall {
            callTextOnly(userMessage, history)
        }
    }

    suspend fun analyzeImage(contentResolver: ContentResolver, uri: Uri, prompt: String): String {
        if (apiKey.isBlank()) {
            return "Gemini API key missing. Open Master Settings (password protected) and add your key."
        }
        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return "Could not read the image file."

        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return retryingCall {
            callWithImage(prompt, base64Image)
        }
    }

    private suspend fun retryingCall(block: () -> Result): String {
        var attempt = 0
        var delayMs = 1000L
        val maxAttempts = 4

        while (attempt < maxAttempts) {
            val result = block()
            if (result.code != 429) {
                return result.text
            }
            attempt++
            if (attempt >= maxAttempts) {
                return "Guru is getting a lot of requests right now (rate limit). Please try again in a minute."
            }
            delay(delayMs)
            delayMs *= 2
        }
        return "Something went wrong. Please try again."
    }

    private data class Result(val text: String, val code: Int)

    private fun callTextOnly(userMessage: String, history: List<Pair<String, String>>): Result {
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

        return executeRequest(url, body)
    }

    private fun callWithImage(prompt: String, base64Image: String): Result {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"

        val parts = JSONArray()
        parts.put(JSONObject().put("text", prompt))
        parts.put(
            JSONObject().put(
                "inline_data",
                JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", base64Image)
            )
        )

        val contents = JSONArray()
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", parts)
        )

        val body = JSONObject()
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.6)
                    .put("maxOutputTokens", 2048)
            )
            .toString()
            .toRequestBody(jsonMedia)

        return executeRequest(url, body)
    }

    private fun executeRequest(url: String, body: okhttp3.RequestBody): Result {
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return Result("Gemini error ${resp.code}: ${raw.take(200)}", resp.code)
            }
            val text = try {
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
            return Result(text, resp.code)
        }
    }
}
