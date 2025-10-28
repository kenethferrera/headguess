package com.example.headguess.ui

import android.content.Context
import android.content.SharedPreferences

object UsernameManager {
    private const val PREFS_NAME = "username_prefs"
    private const val USERNAME_KEY = "username"
    private const val DEFAULT_USERNAME = "Player"

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(USERNAME_KEY, DEFAULT_USERNAME) ?: DEFAULT_USERNAME
    }

    fun setUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(USERNAME_KEY, username).apply()
    }

    fun hasUsername(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(USERNAME_KEY)
    }
}


