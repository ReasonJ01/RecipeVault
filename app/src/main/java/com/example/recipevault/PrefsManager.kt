package com.example.recipevault

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {
    private const val PREFS_NAME = "api_key_prefs"
    private const val KEY_API = "user_api_key"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_API, apiKey)
        editor.apply()
    }

    fun getApiKey(context: Context): String? {
        return getPreferences(context).getString(KEY_API, null)
    }

    fun clearApiKey(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(KEY_API)
        editor.apply()
    }
}