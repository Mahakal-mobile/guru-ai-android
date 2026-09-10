package com.yourpackage.aiassistant   // अपना पैकेज नाम डालो

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var etPrompt: EditText
    private lateinit var btnSend: Button
    private lateinit var tvResponse: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnSettings: Button

    private lateinit var prefs: Prefs
    private lateinit var grokClient: GrokClient
    // GeminiClient भी इसी तरह बना लो (अगर अभी नहीं है)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        grokClient = GrokClient()

        etPrompt = findViewById(R.id.etPrompt)
        btnSend = findViewById(R.id.btnSend)
        tvResponse = findViewById(R.id.tvResponse)
        scrollView = findViewById(R.id.scrollView)
        btnSettings = findViewById(R.id.btnSettings)

        btnSend.setOnClickListener { send() }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun send() {
        val prompt = etPrompt.text.toString().trim()
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Prompt लिखो", Toast.LENGTH_SHORT).show()
            return
        }

        btnSend.isEnabled = false
        tvResponse.append("\n\nYou: $prompt\nAI: सोच रहा हूँ...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    when (prefs.getProvider()) {
                        Constants.PROVIDER_GROK -> {
                            val key = prefs.getGrokKey()
                            if (key.isBlank()) throw Exception("Grok API Key नहीं मिला। Settings में डालो।")
                            grokClient.chat(prompt, key)
                        }
                        Constants.PROVIDER_GEMINI -> {
                            val key = prefs.getGeminiKey()
                            if (key.isBlank()) throw Exception("Gemini API Key नहीं मिला। Settings में डालो।")
                            // GeminiClient().chat(prompt, key)   // अपना Gemini क्लाइंट कॉल करो
                            "Gemini response placeholder" // अस्थायी
                        }
                        else -> throw Exception("कोई प्रोवाइडर चुना नहीं गया")
                    }
                }

                withContext(Dispatchers.Main) {
                    // "सोच रहा हूँ..." हटाकर असली रिस्पांस डालो
                    val current = tvResponse.text.toString()
                    tvResponse.text = current.replace("AI: सोच रहा हूँ...", "AI: $response")
                    scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvResponse.append("\nError: ${e.message}")
                    Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    btnSend.isEnabled = true
                    etPrompt.text.clear()
                }
            }
        }
    }
}
