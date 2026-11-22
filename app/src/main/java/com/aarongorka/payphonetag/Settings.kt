package com.aarongorka.payphonetag

import android.content.Context

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var username: String
        get() = prefs.getString("username", "").orEmpty()
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String
        get() = prefs.getString("password", "").orEmpty()
        set(value) = prefs.edit().putString("password", value).apply()

    val hasCredentials get() = username.isNotBlank() && password.isNotBlank()
}
