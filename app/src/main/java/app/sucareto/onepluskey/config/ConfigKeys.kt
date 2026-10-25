package app.sucareto.onepluskey.config

import android.content.SharedPreferences

object ConfigKeys {
    const val GROUP = "onepluskey"

    const val ENABLED = "enabled"
    const val LONG_PRESS_MS = "long_press_ms"
    const val MULTI_TAP_MS = "multi_tap_ms"

    const val DEFAULT_LONG_PRESS_MS = 450
    const val DEFAULT_MULTI_TAP_MS = 280
    val LONG_PRESS_RANGE = 250..1000
    val MULTI_TAP_RANGE = 150..600

    fun gesture(gesture: Gesture) = "gesture.${gesture.id}"
}

class KeyConfig(private val prefs: SharedPreferences) {
    val enabled: Boolean get() = prefs.getBoolean(ConfigKeys.ENABLED, true)

    val longPressMs: Long
        get() = prefs.getInt(ConfigKeys.LONG_PRESS_MS, ConfigKeys.DEFAULT_LONG_PRESS_MS)
            .coerceIn(ConfigKeys.LONG_PRESS_RANGE).toLong()

    val multiTapMs: Long
        get() = prefs.getInt(ConfigKeys.MULTI_TAP_MS, ConfigKeys.DEFAULT_MULTI_TAP_MS)
            .coerceIn(ConfigKeys.MULTI_TAP_RANGE).toLong()

    fun binding(gesture: Gesture): Binding =
        Binding.decode(prefs.getString(ConfigKeys.gesture(gesture), null), gesture)
}
