package com.isabelle.assistant.pc

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.isabelle.assistant.core.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Conexão com o agente da Isabelle no Windows (pasta pc_agent) por WebSocket.
 * Para funcionar pela internet com segurança, use Tailscale (ou VPN parecida):
 * o endereço fica algo como ws://100.x.y.z:8765 e nada é aberto no roteador.
 */
object PcLink {
    val online = MutableStateFlow(false)

    private val client = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private val main = Handler(Looper.getMainLooper())
    private var ws: WebSocket? = null
    private var connecting = false
    private lateinit var appCtx: Context

    fun connect(c: Context) {
        appCtx = c.applicationContext
        val url = Prefs.str(appCtx, "pc_url").trim()
        if (url.isBlank() || online.value || connecting) return
        connecting = true
        try {
            ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                override fun onOpen(w: WebSocket, r: Response) {
                    w.send(JSONObject().put("cmd", "auth").put("token", Prefs.str(appCtx, "pc_token")).toString())
                }
                override fun onMessage(w: WebSocket, text: String) {
                    if (text.contains("auth_ok")) { online.value = true; connecting = false }
                }
                override fun onClosed(w: WebSocket, code: Int, reason: String) = lost()
                override fun onFailure(w: WebSocket, t: Throwable, r: Response?) = lost()
            })
        } catch (_: Exception) { lost() }
    }

    private fun lost() {
        online.value = false; connecting = false
        main.postDelayed({ if (::appCtx.isInitialized) connect(appCtx) }, 10_000)
    }

    fun disconnect() { ws?.close(1000, null); online.value = false; connecting = false }

    fun send(cmd: String, args: JSONObject = JSONObject()): Boolean {
        if (!online.value) return false
        return ws?.send(JSONObject().put("cmd", cmd).put("args", args).toString()) ?: false
    }
}
