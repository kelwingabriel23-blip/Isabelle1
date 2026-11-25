package com.isabelle.assistant.usage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper

/** Tela mostrada por cima do app bloqueado; em 2,5 s volta para a tela inicial. */
class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra("reason") ?: "App bloqueado."
        setContent { BlockScreen(reason) }
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finishAndRemoveTask()
        }, 2500)
    }
}

@Composable
private fun BlockScreen(reason: String) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF020712)).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Isabelle", color = Color(0xFF18C7FF), fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        Text(reason, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Respira. Volte ao que importa.", color = Color(0xFF7FA8C9), fontSize = 14.sp)
    }
}
