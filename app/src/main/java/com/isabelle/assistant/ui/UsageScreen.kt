package com.isabelle.assistant.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isabelle.assistant.core.Prefs
import com.isabelle.assistant.usage.BlockerService
import com.isabelle.assistant.usage.UsageMonitor

@Composable
fun UsageScreen(vm: AppViewModel) {
    val c = LocalContext.current
    var focus by remember { mutableStateOf(Prefs.bool(c, "focus_mode")) }
    var guard by remember { mutableStateOf(Prefs.bool(c, "svc_blocker")) }
    LaunchedEffect(Unit) { vm.refreshUsage() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tempo de uso", fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))

        if (!UsageMonitor.hasAccess(c)) {
            Column(Modifier.fillMaxWidth().glass().padding(12.dp)) {
                Text("Preciso do \"Acesso de uso\" para ver o tempo dos apps.", fontSize = 13.sp)
                Button(onClick = { c.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Conceder") }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!Settings.canDrawOverlays(c)) {
            Column(Modifier.fillMaxWidth().glass().padding(12.dp)) {
                Text("Para bloquear apps, permita \"Exibir sobre outros apps\".", fontSize = 13.sp)
                Button(onClick = { c.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${c.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Conceder") }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Proteção ligada (bloqueia ao passar do limite)", Modifier.weight(1f), fontSize = 13.sp)
            Switch(guard, { guard = it; Prefs.putBool(c, "svc_blocker", it); if (it) BlockerService.start(c) else BlockerService.stop(c) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Modo foco: só apps essenciais", Modifier.weight(1f), fontSize = 13.sp)
            Switch(focus, { focus = it; Prefs.putBool(c, "focus_mode", it) })
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.usage.take(40), key = { it.pkg }) { a ->
                var limit by remember(a.pkg) { mutableIntStateOf(a.limitMin) }
                var ess by remember(a.pkg) { mutableStateOf(a.essential) }
                Column(Modifier.fillMaxWidth().glass(16.dp).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.label, fontSize = 15.sp)
                            Text("Hoje: ${a.minutes} min", fontSize = 12.sp, color = CyanSoft)
                        }
                        Text("Essencial", fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        Switch(ess, { ess = it; UsageMonitor.setEssential(c, a.pkg, it) })
                    }
                    if (!ess) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (limit == 0) "Sem limite" else "Limite: $limit min/dia", Modifier.weight(1f), fontSize = 13.sp)
                        TextButton({ limit = (limit - 15).coerceAtLeast(0); UsageMonitor.setLimit(c, a.pkg, limit) }) { Text("−15") }
                        TextButton({ limit += 15; UsageMonitor.setLimit(c, a.pkg, limit) }) { Text("+15") }
                    }
                }
            }
        }
    }
}
