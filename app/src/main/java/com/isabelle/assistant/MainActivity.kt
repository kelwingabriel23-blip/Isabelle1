package com.isabelle.assistant

import android.Manifest
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.isabelle.assistant.pc.PcLink
import com.isabelle.assistant.ui.*

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissions.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CONTACTS
            )
        )
        PcLink.connect(this)

        setContent {
            IsabelleTheme {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                    vm.onSystemResult(it.resultCode == RESULT_OK)
                }
                val pending: IntentSender? = vm.pendingSender
                LaunchedEffect(pending) {
                    if (pending != null) {
                        launcher.launch(IntentSenderRequest.Builder(pending).build())
                        vm.pendingSender = null
                    }
                }
                Root(vm)
            }
        }
    }
}

@Composable
private fun Root(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF040C1A)) {
                listOf(
                    Icons.Filled.Home to "Início", Icons.Filled.BarChart to "Uso",
                    Icons.Filled.PhotoLibrary to "Fotos", Icons.Filled.Settings to "Ajustes"
                ).forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, label) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cyan, selectedTextColor = Cyan,
                            indicatorColor = Cyan.copy(alpha = .15f),
                            unselectedIconColor = CyanSoft, unselectedTextColor = CyanSoft
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeScreen(vm)
                1 -> UsageScreen(vm)
                2 -> PhotosScreen(vm)
                else -> SettingsScreen()
            }
        }
    }
}
