package app.sucareto.onepluskey.config

import android.content.SharedPreferences

class ConfigStore(private val prefs: SharedPreferences) {
    private val config = KeyConfig(prefs)

    val enabled get() = config.enabled
    val longPressMs get() = config.longPressMs.toInt()
    val multiTapMs get() = config.multiTapMs.toInt()

    fun binding(gesture: Gesture) = config.binding(gesture)

    fun setEnabled(value: Boolean) = edit { putBoolean(ConfigKeys.ENABLED, value) }

    fun setLongPressMs(value: Int) =
        edit { putInt(ConfigKeys.LONG_PRESS_MS, value.coerceIn(ConfigKeys.LONG_PRESS_RANGE)) }

    fun setMultiTapMs(value: Int) =
        edit { putInt(ConfigKeys.MULTI_TAP_MS, value.coerceIn(ConfigKeys.MULTI_TAP_RANGE)) }

    fun setBinding(gesture: Gesture, binding: Binding) =
        edit { putString(ConfigKeys.gesture(gesture), binding.encode()) }

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
    }
}
