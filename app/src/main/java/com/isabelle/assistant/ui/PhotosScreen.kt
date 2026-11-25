package com.isabelle.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PhotosScreen(vm: AppViewModel) {
    val s = vm.scan
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Fotos", fontSize = 22.sp)
        Text(
            "A análise acontece no seu celular. Mover e apagar sempre pedem a sua confirmação.",
            fontSize = 12.sp, color = CyanSoft
        )
        Button(onClick = { vm.scanPhotos() }, enabled = !vm.scanning) {
            Text(if (vm.scanning) "Analisando..." else "Analisar fotos recentes")
        }
        if (vm.photoMsg.isNotBlank()) Text(vm.photoMsg, fontSize = 13.sp)

        if (s != null) {
            Column(Modifier.fillMaxWidth().glass().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                s.byCategory.entries.sortedByDescending { it.value.size }.forEach { (cat, list) ->
                    Row { Text(cat, Modifier.weight(1f)); Text("${list.size}") }
                }
            }
            Button(onClick = { vm.organize() }) { Text("Organizar em pastas (Pictures/Isabelle)") }
            OutlinedButton(onClick = { vm.deleteSuggested() }) {
                Text("Apagar sugeridas (${s.deleteCandidates.size}): capturas > 30 dias e duplicadas")
            }
        }
    }
}
