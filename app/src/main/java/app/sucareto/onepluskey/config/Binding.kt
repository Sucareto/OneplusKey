package app.sucareto.onepluskey.config

import org.json.JSONException
import org.json.JSONObject

enum class ScreenOffMode(val id: String) {
    IGNORE("ignore"),
    WAKE("wake"),
    SILENT("silent");

    companion object {
        fun fromId(id: String?): ScreenOffMode = entries.firstOrNull { it.id == id } ?: IGNORE
    }
}

data class Binding(
    val action: KeyAction = KeyAction.None,
    val vibrate: Boolean = true,
    val screenOff: ScreenOffMode = ScreenOffMode.IGNORE,
) {
    val isActive: Boolean get() = action != KeyAction.None

    fun encode(): String = JSONObject()
        .put(KEY_ACTION, action.toJson())
        .put(KEY_VIBRATE, vibrate)
        .put(KEY_SCREEN_OFF, screenOff.id)
        .toString()

    companion object {
        private const val KEY_ACTION = "action"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_SCREEN_OFF = "screenOff"

        fun defaultFor(gesture: Gesture): Binding =
            if (gesture.supportsNative) Binding(KeyAction.Native, vibrate = false) else Binding()

        fun decode(raw: String?, gesture: Gesture): Binding {
            if (raw.isNullOrEmpty()) return defaultFor(gesture)
            return try {
                val json = JSONObject(raw)
                val action = KeyAction.fromJson(json.optJSONObject(KEY_ACTION))
                Binding(
                    action = if (action == KeyAction.Native && !gesture.supportsNative) KeyAction.None else action,
                    vibrate = json.optBoolean(KEY_VIBRATE, true),
                    screenOff = ScreenOffMode.fromId(json.optString(KEY_SCREEN_OFF)),
                )
            } catch (_: JSONException) {
                defaultFor(gesture)
            }
        }
    }
}
