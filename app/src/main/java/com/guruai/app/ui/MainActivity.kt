package com.guruai.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guruai.app.agent.AgentLoop
import com.guruai.app.agent.GetTimeTool
import com.guruai.app.agent.ToolRegistry
import com.guruai.app.agent.WebSearchTool
import com.guruai.app.data.GeminiClient
import com.guruai.app.data.GrokClient
import com.guruai.app.memory.MemoryStore
import com.guruai.app.util.Constants
import com.guruai.app.util.Prefs
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
    private lateinit var geminiClient: GeminiClient
    private lateinit var memoryStore: MemoryStore
    private lateinit var agentLoop: AgentLoop

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        grokClient = GrokClient()
        geminiClient = GeminiClient()
        memoryStore = MemoryStore(this)

        // Tools setup
        val toolRegistry = ToolRegistry().apply {
            register(WebSearchTool())
            register(GetTimeTool())
        }

        // Agent setup (अभी Gemini से चलेगा, बाद में Grok भी)
        agentLoop = AgentLoop(
            llmClient = { prompt ->
                callAI(prompt)
            },
            toolRegistry = toolRegistry
        )

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

    // ये फंक्शन Settings में चुने हुए प्रोवाइडर के हिसाब से AI को कॉल करेगा
    private fun callAI(prompt: String): String {
        return when (prefs.getProvider()) {
            Constants.PROVIDER_GROK -> {
                val key = prefs.getGrokKey()
                if (key.isBlank()) throw Exception("Grok API Key नहीं मिला। Settings में डालो।")
                grokClient.chat(prompt, key)
            }
            Constants.PROVIDER_GEMINI -> {
                val key = prefs.getGeminiKey()
                if (key.isBlank()) throw Exception("Gemini API Key नहीं मिला। Settings में डालो।")
                geminiClient.chat(prompt, key)
            }
            else -> throw Exception("कोई प्रोवाइडर चुना नहीं गया")
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
                memoryStore.saveMessage("user", prompt)

                val response = withContext(Dispatchers.IO) {
                    agentLoop.run(prompt)
                }

                memoryStore.saveMessage("assistant", response)

                withContext(Dispatchers.Main) {
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
