package app.sucareto.onepluskey.xposed

import android.app.BroadcastOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.sucareto.onepluskey.config.IntentTarget
import app.sucareto.onepluskey.config.KeyAction
import app.sucareto.onepluskey.config.Protocol
import org.json.JSONObject

internal object ActionExecutor {
    private const val LENS_ACTIVITY = "com.google.android.apps.search.lens.LensActivity"
    private const val LENS_SHORTCUT_PACKAGE = "com.google.ar.lens"

    fun run(action: KeyAction): Boolean {
        val context = SystemContext.context ?: run {
            Log.w("No system context, cannot run ${action.type}")
            return false
        }
        return when (action) {
            KeyAction.None, KeyAction.Native, is KeyAction.ColorOsFunction -> false
            KeyAction.CircleToSearch -> ContextualSearch.trigger()
            KeyAction.Assistant -> VoiceAssist.showAssistant(context)
            KeyAction.Gemini -> VoiceAssist.showGemini(context)
            KeyAction.GoogleLens -> openLens(context)
            is KeyAction.LaunchApp -> launchApp(context, action.packageName)
            is KeyAction.StartActivity -> startComponent(context, action.packageName, action.className)
            is KeyAction.SendIntent -> sendIntent(context, action)
            is KeyAction.Shell -> { ShellBridge.dispatch(context, action.command, action.root); true }
        }
    }

    private fun launchApp(context: Context, pkg: String): Boolean {
        if (pkg.isEmpty()) return false
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context) { context.startActivity(intent) }
    }

    private fun openLens(context: Context): Boolean {
        val intent = Intent()
            .setClassName(GOOGLE_PACKAGE, LENS_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context) { context.startActivity(intent) } || launchApp(context, LENS_SHORTCUT_PACKAGE)
    }

    private fun startComponent(context: Context, pkg: String, cls: String): Boolean {
        if (pkg.isEmpty() || cls.isEmpty()) return false
        val intent = Intent()
            .setComponent(ComponentName(pkg, cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return start(context) { context.startActivity(intent) }
    }

    private fun sendIntent(context: Context, spec: KeyAction.SendIntent): Boolean {
        val intent = Intent()
        if (spec.action.isNotEmpty()) intent.action = spec.action
        if (spec.data.isNotEmpty()) intent.data = spec.data.toUri()
        if (spec.packageName.isNotEmpty() && spec.className.isNotEmpty()) {
            intent.component = ComponentName(spec.packageName, spec.className)
        } else if (spec.packageName.isNotEmpty()) {
            intent.setPackage(spec.packageName)
        }
        applyExtras(intent, spec.extras)
        return start(context) {
            when (spec.target) {
                IntentTarget.ACTIVITY -> {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                IntentTarget.BROADCAST -> context.sendBroadcast(intent)
                IntentTarget.SERVICE -> context.startService(intent)
                IntentTarget.FOREGROUND_SERVICE -> context.startForegroundService(intent)
            }
        }
    }

    private fun applyExtras(intent: Intent, extras: String) {
        if (extras.isBlank()) return
        runCatching {
            val json = JSONObject(extras)
            json.keys().forEach { key ->
                when (val value = json.get(key)) {
                    is Boolean -> intent.putExtra(key, value)
                    is Int -> intent.putExtra(key, value)
                    is Long -> intent.putExtra(key, value)
                    is Double -> intent.putExtra(key, value)
                    else -> intent.putExtra(key, value.toString())
                }
            }
        }.onFailure { Log.w("Invalid extras JSON", it) }
    }

    private inline fun start(context: Context, block: () -> Unit): Boolean = runCatching {
        block(); true
    }.getOrElse {
        Log.w("Action dispatch failed", it)
        false
    }
}

internal object ShellBridge {
    fun dispatch(context: Context, command: String, root: Boolean) {
        if (command.isBlank()) return
        val intent = Intent(Protocol.ACTION_SHELL)
            .setPackage(Protocol.PACKAGE)
            .putExtra(Protocol.EXTRA_COMMAND, command)
            .putExtra(Protocol.EXTRA_ROOT, root)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        val options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle()
        runCatching { context.sendBroadcast(intent, null, options) }
            .onFailure { Log.w("Shell dispatch failed", it) }
    }
}
