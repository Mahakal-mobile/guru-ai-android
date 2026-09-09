package com.guruai.app.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guruai.app.R
import com.guruai.app.data.Prefs
import com.guruai.app.util.Constants

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        val lockPanel = findViewById<LinearLayout>(R.id.lockPanel)
        val contentPanel = findViewById<LinearLayout>(R.id.contentPanel)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvPassError = findViewById<TextView>(R.id.tvPassError)
        val etGemini = findViewById<EditText>(R.id.etGemini)
        val etWhatsapp = findViewById<EditText>(R.id.etWhatsapp)
        val etMail = findViewById<EditText>(R.id.etMail)
        val cbShowGemini = findViewById<CheckBox>(R.id.cbShowGemini)

        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            if (etPassword.text.toString() == Constants.MASTER_PASSWORD) {
                lockPanel.visibility = View.GONE
                contentPanel.visibility = View.VISIBLE
                etGemini.setText(prefs.geminiKey)
                etWhatsapp.setText(prefs.whatsappToken)
                etMail.setText(prefs.mailToken)
                tvPassError.visibility = View.GONE
            } else {
                tvPassError.visibility = View.VISIBLE
            }
        }

        cbShowGemini.setOnCheckedChangeListener { _, checked ->
            etGemini.inputType = if (checked)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etGemini.setSelection(etGemini.text.length)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.geminiKey = etGemini.text.toString().trim()
            prefs.whatsappToken = etWhatsapp.text.toString().trim()
            prefs.mailToken = etMail.text.toString().trim()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
