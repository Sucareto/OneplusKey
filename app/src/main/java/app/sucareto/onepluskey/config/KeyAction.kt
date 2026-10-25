package app.sucareto.onepluskey.config

import org.json.JSONObject

enum class IntentTarget(val id: String) {
    ACTIVITY("activity"),
    BROADCAST("broadcast"),
    SERVICE("service"),
    FOREGROUND_SERVICE("foreground_service");

    companion object {
        fun fromId(id: String?): IntentTarget = entries.firstOrNull { it.id == id } ?: ACTIVITY
    }
}

sealed interface KeyAction {
    val type: String

    data object None : KeyAction {
        override val type = "none"
    }

    data object Native : KeyAction {
        override val type = "native"
    }

    data object CircleToSearch : KeyAction {
        override val type = "circle_to_search"
    }

    data object Assistant : KeyAction {
        override val type = "assistant"
    }

    data object Gemini : KeyAction {
        override val type = "gemini"
    }

    data object GoogleLens : KeyAction {
        override val type = "google_lens"
    }

    data class ColorOsFunction(val function: String = "") : KeyAction {
        override val type = TYPE

        companion object {
            const val TYPE = "coloros"
        }
    }

    data class LaunchApp(val packageName: String = "") : KeyAction {
        override val type = TYPE

        companion object {
            const val TYPE = "launch_app"
        }
    }

    data class StartActivity(val packageName: String = "", val className: String = "") : KeyAction {
        override val type = TYPE

        companion object {
            const val TYPE = "activity"
        }
    }

    data class SendIntent(
        val target: IntentTarget = IntentTarget.ACTIVITY,
        val action: String = "",
        val data: String = "",
        val packageName: String = "",
        val className: String = "",
        val extras: String = "",
    ) : KeyAction {
        override val type = TYPE

        companion object {
            const val TYPE = "intent"
        }
    }

    data class Shell(val command: String = "", val root: Boolean = true) : KeyAction {
        override val type = TYPE

        companion object {
            const val TYPE = "shell"
        }
    }

    fun toJson(): JSONObject = JSONObject().put(KEY_TYPE, type).apply {
        when (val action = this@KeyAction) {
            is ColorOsFunction -> put(KEY_FUNCTION, action.function)
            is LaunchApp -> put(KEY_PACKAGE, action.packageName)
            is StartActivity -> put(KEY_PACKAGE, action.packageName).put(KEY_CLASS, action.className)
            is SendIntent -> put(KEY_TARGET, action.target.id)
                .put(KEY_ACTION, action.action)
                .put(KEY_DATA, action.data)
                .put(KEY_PACKAGE, action.packageName)
                .put(KEY_CLASS, action.className)
                .put(KEY_EXTRAS, action.extras)
            is Shell -> put(KEY_COMMAND, action.command).put(KEY_ROOT, action.root)
            else -> Unit
        }
    }

    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_PACKAGE = "package"
        private const val KEY_CLASS = "class"
        private const val KEY_TARGET = "target"
        private const val KEY_ACTION = "action"
        private const val KEY_DATA = "data"
        private const val KEY_EXTRAS = "extras"
        private const val KEY_COMMAND = "command"
        private const val KEY_ROOT = "root"
        private const val KEY_FUNCTION = "function"

        fun fromJson(json: JSONObject?): KeyAction = when (json?.optString(KEY_TYPE)) {
            Native.type -> Native
            CircleToSearch.type -> CircleToSearch
            Assistant.type -> Assistant
            Gemini.type -> Gemini
            GoogleLens.type -> GoogleLens
            ColorOsFunction.TYPE -> ColorOsFunction(json.optString(KEY_FUNCTION))
            LaunchApp.TYPE -> LaunchApp(json.optString(KEY_PACKAGE))
            StartActivity.TYPE -> StartActivity(json.optString(KEY_PACKAGE), json.optString(KEY_CLASS))
            SendIntent.TYPE -> SendIntent(
                target = IntentTarget.fromId(json.optString(KEY_TARGET)),
                action = json.optString(KEY_ACTION),
                data = json.optString(KEY_DATA),
                packageName = json.optString(KEY_PACKAGE),
                className = json.optString(KEY_CLASS),
                extras = json.optString(KEY_EXTRAS),
            )
            Shell.TYPE -> Shell(json.optString(KEY_COMMAND), json.optBoolean(KEY_ROOT, true))
            else -> None
        }
    }
}
