package app.sucareto.onepluskey.xposed

import android.content.ContentResolver
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import app.sucareto.onepluskey.config.ColorOsFunctions

internal object ColorOsBridge {
    const val KEY_SINGLE_TAP = 781
    const val KEY_LONG_PRESS = 782

    private const val USER_CURRENT = -2
    private const val SWITCH_SETTLE_MS = 200L
    private const val TAP_RELEASE_MS = 80L
    private const val RESTORE_DELAY_MS = 800L

    private val thread = HandlerThread("OnePlusKeyColorOs").apply { start() }
    private val handler = Handler(thread.looper)

    private var savedState: String? = null
    private var pressing = false
    private var downInjected = false
    private var releaseRequested = false

    private var generation = 0
    private val restoreRunnable = Runnable { restore() }

    fun nativeTap() = handler.post {
        if (pressing) release()
        val delay = restoreNow()
        val gen = ++generation
        handler.postDelayed({ if (gen == generation) injectPress(KEY_SINGLE_TAP) }, delay)
    }

    fun nativeLongPressDown() = handler.post {
        if (pressing) release()
        beginPress(hold = true, delay = restoreNow())
    }

    fun longPressUp() = handler.post {
        if (!pressing) return@post
        if (downInjected) release() else releaseRequested = true
    }

    fun runFunction(function: String, hold: Boolean) = handler.post {
        if (function.isEmpty()) return@post
        val resolver = SystemContext.context?.contentResolver ?: return@post
        if (pressing) release()
        handler.removeCallbacks(restoreRunnable)
        val current = getState(resolver)
        if (savedState == null) savedState = current ?: ColorOsFunctions.NOTHING
        val delay = if (current == function) {
            0L
        } else {
            putState(resolver, function)
            SWITCH_SETTLE_MS
        }
        beginPress(hold, delay)
    }

    private fun beginPress(hold: Boolean, delay: Long) {
        val gen = ++generation
        pressing = true
        downInjected = false
        releaseRequested = !hold
        handler.postDelayed({
            if (gen != generation || !pressing) return@postDelayed
            inject(KEY_LONG_PRESS, KeyEvent.ACTION_DOWN)
            downInjected = true
            if (releaseRequested) {
                handler.postDelayed({ if (gen == generation && pressing) release() }, TAP_RELEASE_MS)
            }
        }, delay)
    }

    private fun release() {
        generation++
        pressing = false
        if (downInjected) inject(KEY_LONG_PRESS, KeyEvent.ACTION_UP)
        downInjected = false
        releaseRequested = false
        if (savedState != null) {
            handler.removeCallbacks(restoreRunnable)
            handler.postDelayed(restoreRunnable, RESTORE_DELAY_MS)
        }
    }

    private fun restoreNow(): Long {
        if (savedState == null) return 0L
        handler.removeCallbacks(restoreRunnable)
        return if (restore()) SWITCH_SETTLE_MS else 0L
    }

    private fun restore(): Boolean {
        val original = savedState ?: return false
        savedState = null
        val resolver = SystemContext.context?.contentResolver ?: return false
        if (getState(resolver) == original) return false
        putState(resolver, original)
        return true
    }

    private fun injectPress(keyCode: Int) {
        val downTime = SystemClock.uptimeMillis()
        inject(keyCode, KeyEvent.ACTION_DOWN, downTime)
        inject(keyCode, KeyEvent.ACTION_UP, downTime)
    }

    private fun inject(keyCode: Int, action: Int, downTime: Long = SystemClock.uptimeMillis()) {
        val event = KeyEvent(
            downTime, SystemClock.uptimeMillis(), action, keyCode, 0, 0,
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD,
        )
        runCatching {
            val global = Class.forName("android.hardware.input.InputManagerGlobal")
            val instance = global.getMethod("getInstance").invoke(null)
            global.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
                .invoke(instance, event, 0)
        }.onFailure { Log.w("Inject $keyCode failed", it) }
    }

    private fun getState(resolver: ContentResolver): String? = runCatching {
        android.provider.Settings.System::class.java
            .getMethod("getStringForUser", ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType)
            .invoke(null, resolver, ColorOsFunctions.SETTING_SWITCH_STATE, USER_CURRENT) as? String
    }.getOrNull()

    private fun putState(resolver: ContentResolver, value: String) {
        runCatching {
            android.provider.Settings.System::class.java
                .getMethod(
                    "putStringForUser",
                    ContentResolver::class.java, String::class.java, String::class.java, Int::class.javaPrimitiveType,
                )
                .invoke(null, resolver, ColorOsFunctions.SETTING_SWITCH_STATE, value, USER_CURRENT)
        }.onFailure { Log.w("Switch ColorOS function to $value failed", it) }
    }
}
