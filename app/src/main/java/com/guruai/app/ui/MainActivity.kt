package com.guruai.app.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var tvChat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private val history = mutableListOf<Pair<String, String>>()
    private var cameraImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            append("user", "[Photo captured] ${cameraImageUri}")
            append("assistant", "Got your photo! (Analyzing images is coming soon.)")
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            append("user", "[Photo selected] $uri")
            append("assistant", "Got your photo! (Analyzing images is coming soon.)")
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

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSend).setOnClickListener { send() }
        findViewById<Button>(R.id.btnPlus).setOnClickListener { showAttachMenu() }

        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        applyTheme()
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
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "com.guruai.app.fileprovider",
            imageFile
        )
        takePictureLauncher.launch(cameraImageUri)
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
        }
    }
}
