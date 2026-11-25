package com.isabelle.assistant.core

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * OPCIONAL. Só age no WhatsApp e só nos 15 s depois de você pedir
 * "Isabelle, manda mensagem para ..." com o envio automático ligado em Ajustes.
 */
class SendAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!SendFlag.isArmed()) return
        if (event?.packageName != "com.whatsapp") return
        val root = rootInActiveWindow ?: return
        val btn = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send").firstOrNull()
        if (btn != null && btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            SendFlag.disarm()
        }
    }

    override fun onInterrupt() {}
}
