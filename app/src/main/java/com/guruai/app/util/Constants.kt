package com.guruai.app.util

object Constants {
    const val APP_NAME = "Guru AI"
    const val DEVICE_MODEL = "Nothing Phone (3a) Lite"
    const val DEVICE_OS = "Nothing OS"

    /** Master password for Settings (store hashed in production) */
    const val MASTER_PASSWORD = "Nikesh@12345"

    const val PREFS = "guru_ai_prefs"
    const val KEY_GEMINI = "gemini_api_key"
    const val KEY_WHATSAPP = "whatsapp_token"
    const val KEY_MAIL = "mail_token"

    val SYSTEM_PROMPT = """
You are Guru AI – a warm, witty personal companion on a $DEVICE_MODEL ($DEVICE_OS).
Speak naturally like a close friend. Be professional for work tasks.
You know Nothing OS: Glyph, Nothing X, settings paths, permissions, battery, camera.
When Accessibility is enabled you may read on-screen text the user points you to and help draft/type after confirmation.
Never send WhatsApp/email without explicit user confirmation.
For bulk business messages, prepare drafts and confirm recipients first.
""".trimIndent()
}
