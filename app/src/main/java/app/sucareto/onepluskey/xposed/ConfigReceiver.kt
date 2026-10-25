package app.sucareto.onepluskey.xposed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import app.sucareto.onepluskey.config.Protocol
import java.util.concurrent.atomic.AtomicBoolean

internal object ConfigReceiver {
    private val registered = AtomicBoolean(false)

    fun register(overlay: ConfigOverlay) {
        if (registered.get()) return
        val context = SystemContext.context ?: return
        if (!registered.compareAndSet(false, true)) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (!isFromModuleApp(ctx)) {
                    Log.w("Rejected config from uid=$sentFromUid")
                    return
                }
                runCatching {
                    val bundle = intent.getBundleExtra(Protocol.EXTRA_CONFIG) ?: return
                    overlay.replace(bundle.toMap())
                    Log.i("Config updated (${bundle.size()} keys)")
                }.onFailure { Log.w("Config update failed", it) }
            }
        }
        val thread = HandlerThread("OnePlusKeyConfig").apply { start() }
        runCatching {
            context.registerReceiver(
                receiver, IntentFilter(Protocol.ACTION_CONFIG), null, Handler(thread.looper),
                Context.RECEIVER_EXPORTED,
            )
            Log.i("Config receiver registered")
        }.onFailure {
            registered.set(false)
            thread.quitSafely()
            Log.w("Config receiver registration failed", it)
        }
    }

    private fun BroadcastReceiver.isFromModuleApp(context: Context): Boolean {
        val expected = runCatching { context.packageManager.getPackageUid(Protocol.PACKAGE, 0) }
            .getOrNull() ?: return false
        return sentFromUid == expected
    }

    @Suppress("DEPRECATION")
    private fun Bundle.toMap(): Map<String, Any?> = keySet().associateWith { get(it) }
}
