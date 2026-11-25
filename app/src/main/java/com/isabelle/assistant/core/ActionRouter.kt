package com.isabelle.assistant.core

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.isabelle.assistant.pc.PcLink
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId

/** Executa as ações pedidas pela IA. Retorna um texto extra para falar (ou vazio). */
object ActionRouter {

    private const val YT_MUSIC = "com.google.android.apps.youtube.music"
    private const val WHATSAPP = "com.whatsapp"

    fun run(c: Context, a: JSONObject): String = try {
        when (a.optString("type")) {
            "alarm" -> alarm(c, a)
            "event" -> event(c, a)
            "whatsapp" -> whatsapp(c, a)
            "music" -> music(c, a.optString("query"))
            "music_control" -> mediaKey(c, a.optString("command"))
            "open_app" -> openApp(c, a.optString("name"))
            "pc" -> pc(a)
            "usage_report" -> Assistant.usageSummary(c)
            "organize_photos" -> "Abra a aba Fotos para eu organizar."
            else -> ""
        }
    } catch (e: Exception) {
        "Não consegui executar: ${e.message}"
    }

    private fun start(c: Context, i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        c.startActivity(i)
    }

    private fun alarm(c: Context, a: JSONObject): String {
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, a.getInt("hour"))
            .putExtra(AlarmClock.EXTRA_MINUTES, a.optInt("minute", 0))
            .putExtra(AlarmClock.EXTRA_MESSAGE, a.optString("label", "Isabelle"))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        start(c, i)
        return ""
    }

    private fun event(c: Context, a: JSONObject): String {
        val begin = LocalDateTime.parse(a.getString("start"))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = begin + a.optInt("durationMin", 60) * 60_000L
        val i = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, a.optString("title", "Compromisso"))
            .putExtra(CalendarContract.Events.DESCRIPTION, a.optString("notes"))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        start(c, i)
        return "Toque em Salvar na agenda para confirmar."
    }

    private fun whatsapp(c: Context, a: JSONObject): String {
        val text = a.optString("text")
        var phone = a.optString("phone").filter { it.isDigit() }
        if (phone.isBlank()) phone = findPhone(c, a.optString("contact")) ?: ""
        if (phone.isBlank()) return "Não achei o número de ${a.optString("contact")} nos contatos."
        if (phone.length in 10..11) phone = "55$phone"
        val uri = Uri.parse("https://wa.me/$phone?text=" + Uri.encode(text))
        if (Prefs.bool(c, "auto_send")) SendFlag.arm()
        start(c, Intent(Intent.ACTION_VIEW, uri).setPackage(WHATSAPP))
        return if (Prefs.bool(c, "auto_send")) "Enviando." else "Toque em enviar no WhatsApp."
    }

    private fun findPhone(c: Context, name: String): String? {
        if (name.isBlank()) return null
        if (c.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != 0) return null
        c.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"), null
        )?.use { cur -> if (cur.moveToFirst()) return cur.getString(0).filter { it.isDigit() } }
        return null
    }

    private fun music(c: Context, query: String): String {
        val i = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .setPackage(YT_MUSIC)
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            .putExtra(SearchManager.QUERY, query)
        start(c, i)
        return ""
    }

    fun mediaKey(c: Context, cmd: String): String {
        val code = when (cmd) {
            "pause", "play" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return ""
        }
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return ""
    }

    private fun openApp(c: Context, name: String): String {
        val pm = c.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val hit = pm.queryIntentActivities(main, 0).firstOrNull {
            it.loadLabel(pm).toString().contains(name, ignoreCase = true)
        } ?: return "Não encontrei o app $name."
        pm.getLaunchIntentForPackage(hit.activityInfo.packageName)?.let { start(c, it) }
        return ""
    }

    private fun pc(a: JSONObject): String {
        val ok = PcLink.send(a.optString("command"), a.optJSONObject("args") ?: JSONObject())
        return if (ok) "" else "O PC não está conectado."
    }
}

/** Marca temporária: "clique em Enviar assim que o WhatsApp abrir". */
object SendFlag {
    @Volatile var armedUntil = 0L
    fun arm() { armedUntil = System.currentTimeMillis() + 15_000 }
    fun isArmed() = System.currentTimeMillis() < armedUntil
    fun disarm() { armedUntil = 0L }
}
