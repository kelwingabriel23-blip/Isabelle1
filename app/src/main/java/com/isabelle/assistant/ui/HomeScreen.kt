package com.isabelle.assistant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isabelle.assistant.pc.PcLink

@Composable
fun HomeScreen(vm: AppViewModel) {
    val pcOnline by PcLink.online.collectAsState()
    var input by remember { mutableStateOf("") }
    val last = vm.messages.lastOrNull()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // topo: nome + status
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).border(2.dp, Cyan, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Isabelle", fontSize = 22.sp, color = Color.White)
                Text("Assistente de IA", fontSize = 12.sp, color = CyanSoft)
            }
            Row(Modifier.glass(20.dp).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Green, shape = CircleShape, modifier = Modifier.size(9.dp)) {}
                Spacer(Modifier.width(6.dp))
                Text("Online", fontSize = 12.sp)
                if (pcOnline) { Spacer(Modifier.width(8.dp)); Text("PC ✓", fontSize = 12.sp, color = Cyan) }
            }
        }

        // orbe
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Orb(vm.orb)
        }

        // estado da IA
        Column(Modifier.fillMaxWidth().glass().padding(14.dp)) {
            Text("Estado da IA", fontSize = 14.sp)
            Waveform(vm.orb != OrbState.IDLE, Modifier.fillMaxWidth().height(28.dp).padding(vertical = 6.dp))
            Text(vm.status, fontSize = 12.sp, color = CyanSoft)
        }
        Spacer(Modifier.height(10.dp))

        // recursos ativos
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(Icons.Filled.Search, "Busca na web", Modifier.weight(1f))
            Chip(Icons.Filled.Analytics, "Análise de dados", Modifier.weight(1f))
            Chip(Icons.Filled.AutoAwesome, "Automação", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        // balão
        Column(Modifier.fillMaxWidth().glass(24.dp).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (last == null) {
                Text("Olá! 👋", fontSize = 18.sp)
                Text("Como posso te ajudar hoje?", fontSize = 14.sp)
            } else {
                Text(if (last.fromUser) "Você: ${last.text}" else last.text, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 6)
            }
        }
        Spacer(Modifier.height(10.dp))

        // barra de mensagem
        Row(
            Modifier.fillMaxWidth().glass(32.dp).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.listen() }) { Icon(Icons.Filled.GraphicEq, "Falar", tint = Cyan) }
            TextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("Digite sua mensagem...", color = CyanSoft) },
                modifier = Modifier.weight(1f), singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { vm.send(input); input = "" })
            )
            IconButton(
                onClick = { vm.send(input); input = "" },
                modifier = Modifier.border(1.dp, Cyan, CircleShape)
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = Cyan) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Chip(icon: ImageVector, label: String, modifier: Modifier) {
    Row(modifier.glass(14.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, maxLines = 1)
    }
}
