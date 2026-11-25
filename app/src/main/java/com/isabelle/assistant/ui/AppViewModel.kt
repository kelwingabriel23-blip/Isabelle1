package com.isabelle.assistant.ui

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isabelle.assistant.core.Assistant
import com.isabelle.assistant.photos.PhotoOrganizer
import com.isabelle.assistant.usage.AppUsage
import com.isabelle.assistant.usage.UsageMonitor
import com.isabelle.assistant.voice.VoiceManager
import kotlinx.coroutines.launch

class AppViewModel(private val app: Application) : AndroidViewModel(app) {

    data class Msg(val fromUser: Boolean, val text: String)

    val messages = mutableStateListOf<Msg>()
    var status by mutableStateOf("Pronta para ajudar.")
    var orb by mutableStateOf(OrbState.IDLE)

    private val voice = VoiceManager(app)

    // ---- conversa ----
    fun send(text: String) {
        if (text.isBlank()) return
        messages += Msg(true, text)
        orb = OrbState.THINKING; status = "Pensando..."
        viewModelScope.launch {
            val reply = Assistant.handle(app, text)
            messages += Msg(false, reply)
            orb = OrbState.SPEAKING; status = "Falando..."
            voice.speak(reply) { orb = OrbState.IDLE; status = "Pronta para ajudar." }
        }
    }

    fun listen() {
        orb = OrbState.LISTENING; status = "Ouvindo..."
        voice.listen(
            onPartial = { status = it },
            onFinal = { send(it) },
            onErr = { orb = OrbState.IDLE; status = "Não entendi. Toque no microfone e fale de novo." }
        )
    }

    // ---- uso de apps ----
    var usage by mutableStateOf<List<AppUsage>>(emptyList())
    fun refreshUsage() { usage = UsageMonitor.appsToday(app) }

    // ---- fotos ----
    var scan by mutableStateOf<PhotoOrganizer.Scan?>(null)
    var scanning by mutableStateOf(false)
    var photoMsg by mutableStateOf("")
    var pendingSender by mutableStateOf<IntentSender?>(null)
    private var pendingKind = ""

    fun scanPhotos() {
        scanning = true; photoMsg = "Analisando fotos no seu celular..."
        viewModelScope.launch {
            try {
                scan = PhotoOrganizer.scan(app)
                photoMsg = "${scan?.photos?.size ?: 0} fotos analisadas."
            } catch (e: Exception) { photoMsg = "Erro: ${e.message}" }
            scanning = false
        }
    }

    fun organize() {
        val s = scan ?: return
        val list = s.photos.filter { it.category != "Outros" }
        if (list.isEmpty()) { photoMsg = "Nada para organizar."; return }
        pendingKind = "move"; pendingSender = PhotoOrganizer.moveRequest(app, list)
    }

    fun deleteSuggested() {
        val s = scan ?: return
        if (s.deleteCandidates.isEmpty()) { photoMsg = "Nenhuma sugestão de exclusão."; return }
        pendingKind = "delete"; pendingSender = PhotoOrganizer.deleteRequest(app, s.deleteCandidates)
    }

    fun onSystemResult(ok: Boolean) {
        val kind = pendingKind; pendingKind = ""
        if (!ok) { photoMsg = "Cancelado."; return }
        viewModelScope.launch {
            if (kind == "move") {
                PhotoOrganizer.applyMove(app, scan?.photos?.filter { it.category != "Outros" } ?: emptyList())
                photoMsg = "Fotos organizadas em Pictures/Isabelle."
            } else if (kind == "delete") photoMsg = "Fotos apagadas."
            scanPhotos()
        }
    }

    override fun onCleared() { voice.shutdown() }
}
