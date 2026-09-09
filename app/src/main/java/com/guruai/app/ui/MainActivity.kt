package com.guruai.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guruai.app.R
import com.guruai.app.data.GeminiClient
import com.guruai.app.data.Prefs
import com.guruai.app.service.GuruAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var tvChat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private val history = mutableListOf<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        tvChat = findViewById(R.id.tvChat)
        tvStatus = findViewById(R.id.tvStatus)
        etInput = findViewById(R.id.etInput)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSend).setOnClickListener { send() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val a11y = if (GuruAccessibilityService.isEnabled()) "on" else "off"
        val key = if (prefs.geminiKey.isNotBlank()) "Gemini OK" else "add Gemini key"
        val mode = if (prefs.aiOnlineMode) "Online" else "Offline"
        tvStatus.text = "Accessibility: $a11y · $key · $mode · ${com.guruai.app.util.Constants.DEVICE_MODEL}"
    }

    private fun append(role: String, text: String) {
        history.add(role to text)
        val label = if (role == "user") "You" else "Guru"
        tvChat.append("\n\n$label:\n$text")
    }

    private fun send() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        etInput.setText("")
        append("user", text)

        if (!prefs.aiOnlineMode) {
            append("assistant", "AI Online Mode is off. Turn it on in Settings to chat with Gemini, or ask about something saved locally.")
            return
        }

        lifecycleScope.launch {
            val reply = withContext(Dispatchers.IO) {
                GeminiClient(prefs.geminiKey).chat(text, history.dropLast(1))
            }
            append("assistant", reply)
        }
    }
}
