package com.isabelle.assistant.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.isabelle.assistant.usage.BlockerService
import com.isabelle.assistant.pc.PcLink

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs.bool(c, "svc_blocker")) BlockerService.start(c)
        // A escuta por voz (microfone) precisa ser religada abrindo o app uma vez, regra do Android 14+.
        PcLink.connect(c)
    }
}
