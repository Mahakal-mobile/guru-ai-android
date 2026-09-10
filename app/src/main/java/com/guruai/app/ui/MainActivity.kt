package com.guruai.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.guruai.app.R
import com.guruai.app.data.GeminiClient
import com.guruai.app.data.Prefs
import com.guruai.app.service.GuruAccessibilityService
import com.guruai.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: Prefs
    private lateinit var tvChat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnMic: Button
    private val history = mutableListOf<Pair<String, String>>()
    private var cameraImageUri: Uri? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var tts: TextToSpeech? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            Toast.makeText(this, "Mic permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraImageUri
        if (success && uri != null) {
            analyzeImage(uri, "captured")
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            analyzeImage(uri, "selected")
        }
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            append("user", "[File selected] $uri")
            append("assistant", "Got your file! (Reading file contents is coming soon.)")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        tvChat = findViewById(R.id.tvChat)
        tvStatus = findViewById(R.id.tvStatus)
        etInput = findViewById(R.id.etInput)
        btnMic = findViewById(R.id.btnMic)

        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSend).setOnClickListener { send() }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { showAttachMenu() }
        btnMic.setOnClickListener { toggleMic() }

        applyTheme()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        applyTheme()
    }

    private fun toggleMic() {
        if (isListening) {
            stopListening()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startListening()
            } else {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (isListening) restartListening()
            }

            override fun onError(error: Int) {
                if (isListening) restartListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    etInput.setText(spoken)
                    send()
                }
                if (isListening) restartListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        isListening = true
        btnMic.text = "⏹"
        launchRecognizerIntent()
    }

    private fun restartListening() {
        if (isListening) launchRecognizerIntent()
    }

    private fun launchRecognizerIntent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            btnMic.text = "🎤"
        }
    }

    private fun stopListening() {
        isListening = false
        btnMic.text = "🎤"
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun showAttachMenu() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_attach_menu, null)
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            launchCamera()
        }
        view.findViewById<LinearLayout>(R.id.optionPhotos).setOnClickListener {
            dialog.dismiss()
            pickImageLauncher.launch("image/*")
        }
        view.findViewById<LinearLayout>(R.id.optionFiles).setOnClickListener {
            dialog.dismiss()
            pickFileLauncher.launch("*/*")
        }
        dialog.show()
    }

    private fun launchCamera() {
        val imagesDir = File(getExternalFilesDir("images"), "")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "guru_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "com.guruai.app.fileprovider",
            imageFile
        )
        cameraImageUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun analyzeImage(uri: Uri, source: String) {
        append("user", "[Photo $source] Analyzing…")

        if (!prefs.aiOnlineMode) {
            append("assistant", "AI Online Mode is off. Turn it on in Settings to analyze photos.")
            return
        }
        if (prefs.geminiKey.isBlank()) {
            append("assistant", "Add your Gemini API key in Settings first.")
            return
        }

        lifecycleScope.launch {
            val reply = withContext(Dispatchers.IO) {
                GeminiClient(prefs.geminiKey).analyzeImage(
                    contentResolver,
                    uri,
                    "Describe what you see in this image and give useful, relevant information or help based on it."
                )
            }
            append("assistant", reply)
            speak(reply)
        }
    }

    private fun applyTheme() {
        val theme = Constants.THEMES[prefs.themeIndex]
        val bg = Color.parseColor(theme.background)
        val surface = Color.parseColor(theme.surface)
        val accent = Color.parseColor(theme.accent)
        val textPrimary = Color.parseColor(theme.textPrimary)
        val textSecondary = Color.parseColor(theme.textSecondary)

        findViewById<LinearLayout>(R.id.rootLayout).setBackgroundColor(bg)
        findViewById<LinearLayout>(R.id.topBar).setBackgroundColor(surface)
        findViewById<LinearLayout>(R.id.bottomBar).setBackgroundColor(surface)
        findViewById<TextView>(R.id.tvTitle).setTextColor(accent)
        tvStatus.setTextColor(textSecondary)
        tvChat.setTextColor(textPrimary)
        etInput.setTextColor(textPrimary)
        etInput.setBackgroundColor(surface)

        val btnSettings = findViewById<Button>(R.id.btnSettings)
        btnSettings.setBackgroundColor(accent)
        btnSettings.setTextColor(bg)

        val btnSend = findViewById<Button>(R.id.btnSend)
        btnSend.setBackgroundColor(accent)
        btnSend.setTextColor(bg)

        val btnPlus = findViewById<Button>(R.id.btnPlus)
        btnPlus.setBackgroundColor(surface)
        btnPlus.setTextColor(accent)

        btnMic.setBackgroundColor(surface)
        btnMic.setTextColor(accent)
    }

    private fun refreshStatus() {
        val a11y = if (GuruAccessibilityService.isEnabled()) "on" else "off"
        val key = if (prefs.geminiKey.isNotBlank()) "Gemini OK" else "add Gemini key"
        val mode = if (prefs.aiOnlineMode) "Online" else "Offline"
        tvStatus.text = "Accessibility: $a11y · $key · $mode · ${Constants.DEVICE_MODEL}"
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
            speak(reply)
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guru_reply")
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
