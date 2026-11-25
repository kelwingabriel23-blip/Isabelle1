package com.isabelle.assistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.isabelle.assistant.core.Assistant
import kotlinx.coroutines.*

/**
 * Fica ouvindo e reage quando você diz "Isabelle ...".
 * Usa o reconhecedor de voz do Android em ciclo (simples, mas gasta bateria).
 * Para produção, troque por um detector de palavra de ativação offline (ex.: Picovoice Porcupine).
 */
class WakeWordService : Service() {

    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var voice: VoiceManager
    private var busy = false
    private var running = false
    private val wake = Regex("""\b(isabelle|isabele|isabel|izabel|izabele)\b[,.!]?\s*(.*)""", RegexOption.IGNORE_CASE)

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("wake", "Escuta da Isabelle", NotificationManager.IMPORTANCE_MIN))
        val n: Notification = NotificationCompat.Builder(this, "wake")
            .setContentTitle("Isabelle está ouvindo")
            .setContentText("Diga \"Isabelle\" para chamar")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)

        if (!running) {
            running = true
            voice = VoiceManager(this)
            listenLoop()
        }
        return START_STICKY
    }

    private fun listenLoop() {
        if (!running || busy) return
        voice.listen(
            onFinal = { text -> onHeard(text) },
            onErr = { main.postDelayed({ listenLoop() }, 600) }
        )
    }

    private fun onHeard(text: String) {
        val m = wake.find(text)
        if (m == null) { main.postDelayed({ listenLoop() }, 300); return }
        val command = m.groupValues[2].trim()
        busy = true
        if (command.isBlank()) {
            // só chamou o nome: pergunta e ouve o comando
            voice.speak("Pois não?") {
                main.post {
                    voice.listen(onFinal = { process(it) }, onErr = { resume() })
                }
            }
        } else process(command)
    }

    private fun process(command: String) {
        scope.launch {
            val reply = Assistant.handle(this@WakeWordService, command)
            voice.speak(reply) { main.post { resume() } }
        }
    }

    private fun resume() { busy = false; main.postDelayed({ listenLoop() }, 400) }

    override fun onDestroy() {
        running = false
        scope.cancel()
        if (::voice.isInitialized) voice.shutdown()
        super.onDestroy()
    }

    companion object {
        fun start(c: Context) = ContextCompat.startForegroundService(c, Intent(c, WakeWordService::class.java))
        fun stop(c: Context) { c.stopService(Intent(c, WakeWordService::class.java)) }
    }
}
