package app.sucareto.onepluskey.xposed

import android.content.Context

internal object SystemContext {
    val context: Context?
        get() = runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val current = activityThread.getMethod("currentActivityThread").invoke(null)
            activityThread.getMethod("getSystemContext").invoke(current) as? Context
        }.getOrNull() ?: runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            activityThread.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull()
}
