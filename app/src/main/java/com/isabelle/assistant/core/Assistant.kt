package com.isabelle.assistant.core

import android.content.Context
import com.isabelle.assistant.usage.UsageMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/** Cérebro da Isabelle: manda o texto para a IA, recebe {reply, action} e executa a ação. */
object Assistant {

    private val history = ArrayDeque<Pair<String, String>>() // (role, content)

    private fun systemPrompt(): String = """
Você é a Isabelle, assistente pessoal de IA no celular Android do usuário. Responda sempre em português do Brasil, de forma curta e natural (será falado em voz alta).
Agora é ${LocalDateTime.now()} (fuso America/Sao_Paulo).
Responda SOMENTE com um JSON válido, sem markdown, neste formato:
{"reply":"texto para falar ao usuário","action":{"type":"none"}}
Tipos de action possíveis:
- {"type":"alarm","hour":7,"minute":30,"label":"Acordar"}
- {"type":"event","title":"Dentista","start":"2026-11-25T14:00","durationMin":60,"notes":""}
- {"type":"whatsapp","contact":"nome do contato","phone":"opcional, só dígitos com DDI","text":"mensagem"}
- {"type":"music","query":"nome da música/artista"}  (toca no YouTube Music)
- {"type":"music_control","command":"pause|play|next|previous"}
- {"type":"open_app","name":"nome do app"}
- {"type":"pc","command":"lock|sleep|shutdown|open|media","args":{"target":"url ou apelido","key":"playpause|next|prev|volup|voldown|mute"}}
- {"type":"usage_report"}  (resumo do tempo de uso dos apps hoje)
- {"type":"organize_photos"}  (pede para abrir a aba Fotos)
Para perguntas gerais, responda no "reply" com action "none". Se não souber algo atual, diga que não tem certeza.
Nunca invente número de telefone.
""".trimIndent()

    suspend fun handle(c: Context, text: String): String {
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", systemPrompt()))
        history.forEach { (r, t) -> msgs.put(JSONObject().put("role", r).put("content", t)) }
        msgs.put(JSONObject().put("role", "user").put("content", text))

        val raw = try {
            AiClient.chat(c, msgs)
        } catch (e: Exception) {
            return "Não consegui falar com a IA: ${e.message}"
        }

        var reply = raw
        var action: JSONObject? = null
        try {
            val s = raw.indexOf('{'); val e = raw.lastIndexOf('}')
            val j = JSONObject(raw.substring(s, e + 1))
            reply = j.optString("reply", raw)
            action = j.optJSONObject("action")
        } catch (_: Exception) { /* modelo respondeu texto puro */ }

        var extra = ""
        if (action != null && action.optString("type", "none") != "none") {
            extra = withContext(Dispatchers.Main) { ActionRouter.run(c, action) }
        }

        history.addLast("user" to text)
        history.addLast("assistant" to reply)
        while (history.size > 12) history.removeFirst()

        return if (extra.isBlank()) reply else "$reply $extra".trim()
    }

    fun usageSummary(c: Context): String {
        if (!UsageMonitor.hasAccess(c)) return "Ative o Acesso de uso em Ajustes para eu ver o tempo dos apps."
        val top = UsageMonitor.appsToday(c).filter { it.minutes > 0 }.take(5)
        if (top.isEmpty()) return "Ainda sem uso registrado hoje."
        return "Hoje: " + top.joinToString(", ") { "${it.label} ${it.minutes} min" } + "."
    }
}
