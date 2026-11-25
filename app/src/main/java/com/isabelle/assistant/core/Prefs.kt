package com.isabelle.assistant.core

import android.content.Context

/** Configurações simples guardadas no próprio celular. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("isabelle", Context.MODE_PRIVATE)

    fun str(c: Context, k: String, d: String = ""): String = sp(c).getString(k, d) ?: d
    fun putStr(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()

    fun bool(c: Context, k: String, d: Boolean = false): Boolean = sp(c).getBoolean(k, d)
    fun putBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()

    fun int(c: Context, k: String, d: Int = 0): Int = sp(c).getInt(k, d)
    fun putInt(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()

    fun strSet(c: Context, k: String, d: Set<String> = emptySet()): Set<String> =
        sp(c).getStringSet(k, d) ?: d

    fun putStrSet(c: Context, k: String, v: Set<String>) =
        sp(c).edit().putStringSet(k, v.toSet()).apply()

    val DEFAULT_ESSENTIALS = setOf(
        "com.whatsapp",
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.google.android.apps.maps",
        "com.google.android.deskclock",
        "com.google.android.apps.youtube.music",
        "com.sec.android.app.camera",
        "com.android.camera",
        "com.google.android.GoogleCamera"
    )

    fun essentials(c: Context): Set<String> = strSet(c, "essentials", DEFAULT_ESSENTIALS)
}
