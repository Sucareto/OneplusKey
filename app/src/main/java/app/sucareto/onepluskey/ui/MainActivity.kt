package app.sucareto.onepluskey.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sucareto.onepluskey.config.Gesture
import app.sucareto.onepluskey.ui.screens.GestureEditorScreen
import app.sucareto.onepluskey.ui.screens.HomeScreen
import app.sucareto.onepluskey.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                var editing by rememberSaveable { mutableStateOf<Gesture?>(null) }

                val current = editing
                if (current == null) {
                    HomeScreen(
                        state = state,
                        onToggleEnabled = viewModel::setEnabled,
                        onEditGesture = { editing = it },
                    )
                } else {
                    GestureEditorScreen(
                        gesture = current,
                        saved = state.binding(current),
                        savedTimingMs = state.timingMs(current),
                        onSave = { draft, timingMs ->
                            viewModel.saveGesture(current, draft, timingMs)
                            editing = null
                        },
                        onBack = { editing = null },
                    )
                }
            }
        }
    }
}
