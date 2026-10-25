package app.sucareto.onepluskey.ui

import app.sucareto.onepluskey.config.Binding
import app.sucareto.onepluskey.config.ConfigKeys
import app.sucareto.onepluskey.config.Gesture

data class UiState(
    val moduleActive: Boolean = false,
    val frameworkName: String? = null,
    val configurable: Boolean = false,
    val enabled: Boolean = true,
    val longPressMs: Int = ConfigKeys.DEFAULT_LONG_PRESS_MS,
    val multiTapMs: Int = ConfigKeys.DEFAULT_MULTI_TAP_MS,
    val bindings: Map<Gesture, Binding> = emptyMap(),
) {
    fun binding(gesture: Gesture): Binding = bindings[gesture] ?: Binding.defaultFor(gesture)

    fun timingMs(gesture: Gesture): Int? = when (gesture) {
        Gesture.LONG -> longPressMs
        Gesture.DOUBLE -> multiTapMs
        Gesture.SINGLE -> null
    }
}
