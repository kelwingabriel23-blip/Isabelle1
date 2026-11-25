package com.isabelle.assistant.usage

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

/** Vigia o app em primeiro plano e abre a tela de bloqueio quando passa do limite. */
class BlockerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var current: String? = null
    private var used: Map<String, Long> = emptyMap()
    private var lastUsageRefresh = 0L
    private var lastBlockAt = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("blocker", "Proteção de tempo", NotificationManager.IMPORTANCE_MIN))
        val n: Notification = NotificationCompat.Builder(this, "blocker")
            .setContentTitle("Isabelle cuidando do seu tempo")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true).build()
        ServiceCompat.startForeground(this, 2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        scope.coroutineContext.cancelChildren()
        scope.launch {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                val now = System.currentTimeMillis()
                updateForeground(usm, now)
                if (now - lastUsageRefresh > 10_000) {
                    used = UsageMonitor.usedTodayMs(this@BlockerService); lastUsageRefresh = now
                }
                val pkg = current
                if (pkg != null && now - lastBlockAt > 3_000) {
                    // soma o tempo da sessão atual, que o sistema só fecha ao sair do app
                    val reason = UsageMonitor.blockReason(this@BlockerService, pkg, used)
                    if (reason != null) {
                        lastBlockAt = now
                        startActivity(
                            Intent(this@BlockerService, BlockActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra("reason", reason).putExtra("pkg", pkg)
                        )
                    }
                }
                delay(1000)
            }
        }
        return START_STICKY
    }

    private fun updateForeground(usm: UsageStatsManager, now: Long) {
        val ev = usm.queryEvents(now - 5_000, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) current = e.packageName
            else if (e.eventType == UsageEvents.Event.ACTIVITY_PAUSED && e.packageName == current) { /* mantém último */ }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        fun start(c: Context) = ContextCompat.startForegroundService(c, Intent(c, BlockerService::class.java))
        fun stop(c: Context) { c.stopService(Intent(c, BlockerService::class.java)) }
    }
}
