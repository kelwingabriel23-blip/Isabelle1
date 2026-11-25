package com.isabelle.assistant.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente para qualquer API compatível com o formato "chat/completions"
 * (OpenAI, Groq, OpenRouter, Perplexity, Gemini compat., Ollama etc.).
 * Base URL, chave e modelo são configurados na tela Ajustes.
 */
object AiClient {
    private val http = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun chat(c: Context, messages: JSONArray): String = withContext(Dispatchers.IO) {
        val base = Prefs.str(c, "api_base", "https://api.openai.com/v1").trim().trimEnd('/')
        val key = Prefs.str(c, "api_key")
        val model = Prefs.str(c, "api_model", "gpt-4o-mini")
        if (key.isBlank()) error("Configure a chave da API em Ajustes.")

        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.3)

        val req = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Erro ${r.code}: ${s.take(200)}")
            JSONObject(s).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }
}
