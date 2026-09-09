package com.guruai.app.data

import android.content.Context
import com.guruai.app.util.Constants

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)

    var geminiKey: String
        get() = sp.getString(Constants.KEY_GEMINI, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_GEMINI, v).apply()

    var whatsappToken: String
        get() = sp.getString(Constants.KEY_WHATSAPP, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_WHATSAPP, v).apply()

    var mailToken: String
        get() = sp.getString(Constants.KEY_MAIL, "") ?: ""
        set(v) = sp.edit().putString(Constants.KEY_MAIL, v).apply()

    var screenMonitorEnabled: Boolean
        get() = sp.getBoolean(Constants.KEY_SCREEN_MONITOR, false)
        set(v) = sp.edit().putBoolean(Constants.KEY_SCREEN_MONITOR, v).apply()

    var whatsappSyncEnabled: Boolean
        get() = sp.getBoolean(Constants.KEY_WHATSAPP_SYNC, false)
        set(v) = sp.edit().putBoolean(Constants.KEY_WHATSAPP_SYNC, v).apply()

    var emailSyncEnabled: Boolean
        get() = sp.getBoolean(Constants.KEY_EMAIL_SYNC, false)
        set(v) = sp.edit().putBoolean(Constants.KEY_EMAIL_SYNC, v).apply()

    var aiOnlineMode: Boolean
        get() = sp.getBoolean(Constants.KEY_AI_ONLINE, true)
        set(v) = sp.edit().putBoolean(Constants.KEY_AI_ONLINE, v).apply()
}
