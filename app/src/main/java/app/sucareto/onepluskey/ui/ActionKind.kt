package app.sucareto.onepluskey.ui

import app.sucareto.onepluskey.R
import app.sucareto.onepluskey.config.ColorOsFunctions
import app.sucareto.onepluskey.config.IntentTarget
import app.sucareto.onepluskey.config.KeyAction

enum class ActionKind(val labelRes: Int) {
    NONE(R.string.action_none),
    NATIVE(R.string.action_native),
    COLOROS(R.string.action_coloros),
    CIRCLE_TO_SEARCH(R.string.action_circle_to_search),
    ASSISTANT(R.string.action_assistant),
    GEMINI(R.string.action_gemini),
    GOOGLE_LENS(R.string.action_google_lens),
    LAUNCH_APP(R.string.action_launch_app),
    ACTIVITY(R.string.action_activity),
    INTENT(R.string.action_intent),
    SHELL(R.string.action_shell);

    companion object {
        fun of(action: KeyAction): ActionKind = when (action) {
            KeyAction.None -> NONE
            KeyAction.Native -> NATIVE
            is KeyAction.ColorOsFunction -> COLOROS
            KeyAction.CircleToSearch -> CIRCLE_TO_SEARCH
            KeyAction.Assistant -> ASSISTANT
            KeyAction.Gemini -> GEMINI
            KeyAction.GoogleLens -> GOOGLE_LENS
            is KeyAction.LaunchApp -> LAUNCH_APP
            is KeyAction.StartActivity -> ACTIVITY
            is KeyAction.SendIntent -> INTENT
            is KeyAction.Shell -> SHELL
        }

        fun blank(kind: ActionKind): KeyAction = when (kind) {
            NONE -> KeyAction.None
            NATIVE -> KeyAction.Native
            COLOROS -> KeyAction.ColorOsFunction(ColorOsFunctions.known.first())
            CIRCLE_TO_SEARCH -> KeyAction.CircleToSearch
            ASSISTANT -> KeyAction.Assistant
            GEMINI -> KeyAction.Gemini
            GOOGLE_LENS -> KeyAction.GoogleLens
            LAUNCH_APP -> KeyAction.LaunchApp()
            ACTIVITY -> KeyAction.StartActivity()
            INTENT -> KeyAction.SendIntent(IntentTarget.ACTIVITY)
            SHELL -> KeyAction.Shell()
        }
    }
}

fun colorOsFunctionLabel(id: String): Int = when (id) {
    "flash_memory" -> R.string.coloros_flash_memory
    "ring_mode" -> R.string.coloros_ring_mode
    "no_disturb" -> R.string.coloros_no_disturb
    "camera" -> R.string.coloros_camera
    "ocrscanner" -> R.string.coloros_ocrscanner
    "flash_light" -> R.string.coloros_flash_light
    "recording" -> R.string.coloros_recording
    "translate" -> R.string.coloros_translate
    "screen_shot" -> R.string.coloros_screen_shot
    else -> R.string.action_coloros
}
