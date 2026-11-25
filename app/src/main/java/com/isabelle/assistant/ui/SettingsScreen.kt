package com.isabelle.assistant.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isabelle.assistant.core.Prefs
import com.isabelle.assistant.pc.PcLink
import com.isabelle.assistant.voice.WakeWordService

@Composable
fun SettingsScreen() {
    val c = LocalContext.current
    @Composable fun field(key: String, label: String, def: String = "", secret: Boolean = false) {
        var v by remember { mutableStateOf(Prefs.str(c, key, def)) }
        OutlinedTextField(
            value = v, onValueChange = { v = it; Prefs.putStr(c, key, it) },
            label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
    var wake by remember { mutableStateOf(Prefs.bool(c, "svc_wake")) }
    var auto by remember { mutableStateOf(Prefs.bool(c, "auto_send")) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ajustes", fontSize = 22.sp)

        Text("Cérebro (API de IA compatível com chat/completions)", fontSize = 13.sp, color = CyanSoft)
        field("api_base", "URL base", "https://api.openai.com/v1")
        field("api_model", "Modelo", "gpt-4o-mini")
        field("api_key", "Chave da API", secret = true)
        Text("Para respostas com busca na internet, use um modelo com busca (ex.: Perplexity, base https://api.perplexity.ai, modelo sonar).", fontSize = 11.sp, color = CyanSoft)

        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Escutar \"Isabelle\" o tempo todo")
                Text("Usa o microfone em segundo plano; gasta bateria.", fontSize = 11.sp, color = CyanSoft)
            }
            Switch(wake, { wake = it; Prefs.putBool(c, "svc_wake", it); if (it) WakeWordService.start(c) else WakeWordService.stop(c) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Enviar WhatsApp automaticamente")
                Text("Exige ativar a Acessibilidade da Isabelle. Sem isso, você só toca em Enviar.", fontSize = 11.sp, color = CyanSoft)
            }
            Switch(auto, { auto = it; Prefs.putBool(c, "auto_send", it) })
        }
        OutlinedButton({ c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Abrir Acessibilidade") }

        HorizontalDivider()
        Text("Conexão com o PC (Windows)", fontSize = 13.sp, color = CyanSoft)
        field("pc_url", "Endereço (ex.: ws://100.101.102.103:8765)")
        field("pc_token", "Token (o mesmo do pc_agent)", secret = true)
        Button({ PcLink.disconnect(); PcLink.connect(c) }) { Text("Conectar agora") }
    }
}
