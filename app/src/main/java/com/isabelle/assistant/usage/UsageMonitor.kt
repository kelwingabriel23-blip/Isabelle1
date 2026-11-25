package com.isabelle.assistant.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.isabelle.assistant.core.Prefs
import java.util.Calendar

data class AppUsage(
    val pkg: String,
    val label: String,
    val minutes: Int,
    val limitMin: Int,
    val essential: Boolean
)

object UsageMonitor {

    fun hasAccess(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Milissegundos em primeiro plano hoje, por pacote. */
    fun usedTodayMs(c: Context): Map<String, Long> {
        if (!hasAccess(c)) return emptyMap()
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usm.queryAndAggregateUsageStats(startOfDay(), System.currentTimeMillis())
            .mapValues { it.value.totalTimeInForeground }
    }

    fun launcherApps(c: Context): List<Pair<String, String>> {
        val pm = c.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
    }

    fun appsToday(c: Context): List<AppUsage> {
        val used = usedTodayMs(c)
        val ess = Prefs.essentials(c)
        return launcherApps(c).map { (pkg, label) ->
            AppUsage(
                pkg, label,
                ((used[pkg] ?: 0L) / 60_000).toInt(),
                Prefs.int(c, "limit_$pkg", 0),
                pkg in ess
            )
        }.sortedWith(compareByDescending<AppUsage> { it.minutes }.thenBy { it.label.lowercase() })
    }

    fun setLimit(c: Context, pkg: String, minutes: Int) =
        Prefs.putInt(c, "limit_$pkg", minutes.coerceAtLeast(0))

    fun setEssential(c: Context, pkg: String, on: Boolean) {
        val s = Prefs.essentials(c).toMutableSet()
        if (on) s += pkg else s -= pkg
        Prefs.putStrSet(c, "essentials", s)
    }

    private fun homePackage(c: Context): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return c.packageManager.resolveActivity(i, 0)?.activityInfo?.packageName
    }

    private fun alwaysAllowed(c: Context): Set<String> = setOfNotNull(
        c.packageName, homePackage(c),
        "com.android.systemui", "com.android.settings",
        "com.google.android.permissioncontroller", "com.android.permissioncontroller",
        "com.google.android.packageinstaller", "com.android.packageinstaller"
    )

    /** Retorna o motivo do bloqueio, ou null se o app pode abrir. */
    fun blockReason(c: Context, pkg: String, usedMs: Map<String, Long>): String? {
        if (pkg in alwaysAllowed(c)) return null
        if (pkg in Prefs.essentials(c)) return null
        if (Prefs.bool(c, "focus_mode")) return "Modo foco ligado: só apps essenciais."
        val lim = Prefs.int(c, "limit_$pkg", 0)
        if (lim > 0 && (usedMs[pkg] ?: 0L) / 60_000 >= lim)
            return "Você atingiu o limite de $lim min hoje."
        return null
    }
}
