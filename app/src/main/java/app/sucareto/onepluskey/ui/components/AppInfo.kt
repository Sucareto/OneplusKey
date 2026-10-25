package app.sucareto.onepluskey.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val packageName: String, val label: String, val icon: Drawable?)

suspend fun loadLaunchableApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0)
        .mapNotNull { resolve ->
            val app = resolve.activityInfo?.applicationInfo ?: return@mapNotNull null
            AppInfo(
                packageName = app.packageName,
                label = pm.getApplicationLabel(app).toString(),
                icon = runCatching { resolve.loadIcon(pm) }.getOrNull(),
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
