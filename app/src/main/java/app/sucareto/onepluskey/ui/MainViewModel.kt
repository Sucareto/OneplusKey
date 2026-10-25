package app.sucareto.onepluskey.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.sucareto.onepluskey.config.Binding
import app.sucareto.onepluskey.config.ConfigStore
import app.sucareto.onepluskey.config.ConfigSync
import app.sucareto.onepluskey.config.Gesture
import app.sucareto.onepluskey.config.ModuleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var store: ConfigStore? = null

    init {
        viewModelScope.launch {
            ModuleState.service.collect { reload() }
        }
    }

    private fun reload() {
        val prefs = ModuleState.preferences()
        store = prefs?.let { ConfigStore(it) }
        if (prefs != null) ConfigSync.push(getApplication(), prefs)
        val current = store
        _state.value = if (current == null) {
            UiState(moduleActive = ModuleState.active, frameworkName = ModuleState.frameworkName)
        } else {
            UiState(
                moduleActive = true,
                frameworkName = ModuleState.frameworkName,
                configurable = true,
                enabled = current.enabled,
                longPressMs = current.longPressMs,
                multiTapMs = current.multiTapMs,
                bindings = Gesture.entries.associateWith { current.binding(it) },
            )
        }
    }

    fun setEnabled(value: Boolean) = mutate { setEnabled(value) }

    fun saveGesture(gesture: Gesture, binding: Binding, timingMs: Int?) = mutate {
        setBinding(gesture, binding)
        if (timingMs != null) {
            when (gesture) {
                Gesture.LONG -> setLongPressMs(timingMs)
                Gesture.DOUBLE -> setMultiTapMs(timingMs)
                Gesture.SINGLE -> Unit
            }
        }
    }

    private inline fun mutate(block: ConfigStore.() -> Unit) {
        val current = store ?: return
        current.block()
        reload()
    }
}
