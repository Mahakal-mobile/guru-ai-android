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
}
