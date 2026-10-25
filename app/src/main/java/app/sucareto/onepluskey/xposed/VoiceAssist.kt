package app.sucareto.onepluskey.xposed

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.service.voice.VoiceInteractionSession

internal object VoiceAssist {

    private const val SERVICE_MANAGER_CLASS = "android.os.ServiceManager"
    private const val INTERFACE_CLASS = "com.android.internal.app.IVoiceInteractionManagerService"
    private const val SERVICE_NAME = "voiceinteraction"

    private const val SETTING_VOICE_SERVICE = "voice_interaction_service"
    private const val GEMINI_PACKAGE = "com.google.android.apps.bard"

    fun showAssistant(context: Context): Boolean =
        showVoiceSession(context) ||
            startIfResolvable(context, Intent(Intent.ACTION_ASSIST)) ||
            startIfResolvable(context, Intent(Intent.ACTION_VOICE_COMMAND).setPackage(GOOGLE_PACKAGE))

    fun showGemini(context: Context): Boolean {
        val shown = (activeVoiceServicePackage(context) == GOOGLE_PACKAGE && showVoiceSession(context)) ||
            launchGeminiApp(context) ||
            startIfResolvable(context, Intent(Intent.ACTION_ASSIST).setPackage(GOOGLE_PACKAGE)) ||
            startIfResolvable(context, Intent(Intent.ACTION_VOICE_COMMAND).setPackage(GOOGLE_PACKAGE))
        if (!shown) Log.w("Neither Gemini nor Google App is available")
        return shown
    }

    private fun showVoiceSession(context: Context): Boolean = runCatching {
        val service = voiceService() ?: return false
        val iface = Class.forName(INTERFACE_CLASS)
        val locked = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        if (locked) {
            val supports = Reflect.method(iface, "activeServiceSupportsLaunchFromKeyguard")
                ?.invoke(service) as? Boolean ?: false
            val launch = Reflect.method(iface, "launchVoiceAssistFromKeyguard")
            if (supports && launch != null) {
                launch.invoke(service)
                return true
            }
        }
        val show = Reflect.methodMatching(iface, "showSessionForActiveService") {
            it.parameterCount == 5 && it.parameterTypes[0] == Bundle::class.java
        } ?: run {
            Log.w("showSessionForActiveService not found")
            return false
        }
        show.invoke(service, Bundle(), VoiceInteractionSession.SHOW_SOURCE_PUSH_TO_TALK, null, null, null) as? Boolean
            ?: false
    }.getOrElse {
        Log.w("Voice session failed", it)
        false
    }

    private fun voiceService(): Any? {
        val binder = Class.forName(SERVICE_MANAGER_CLASS)
            .getMethod("getService", String::class.java)
            .invoke(null, SERVICE_NAME) as? IBinder ?: return null
        return Class.forName("$INTERFACE_CLASS\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder)
    }

    private fun activeVoiceServicePackage(context: Context): String? = runCatching {
        Settings.Secure.getString(context.contentResolver, SETTING_VOICE_SERVICE)
            ?.let { ComponentName.unflattenFromString(it)?.packageName }
    }.getOrNull()

    private fun launchGeminiApp(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(GEMINI_PACKAGE) ?: return false
        return startIfResolvable(context, intent)
    }

    private fun startIfResolvable(context: Context, intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0)) ?: return false
        context.startActivity(intent)
        true
    }.getOrElse {
        Log.w("Start ${intent.action} failed", it)
        false
    }
}
